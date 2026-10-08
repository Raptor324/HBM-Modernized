package com.hbm_m.entity.mob.glyphid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.MobConfig;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntityWaypoint;
import com.hbm_m.entity.mob.EntityParasiteMaggot;
import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm_m.entity.pathfinder.PathFinderUtils;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorGlyphidDig;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IResistanceProvider;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.item.ModItems;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityGlyphid}: Grundglyphide mit Panzerplatten, Aufgabensystem (Wegpunkte, Rueckzug, Verstaerkung,
 * Graben) und Untertypen (normal, befallen, radioaktiv).
 *
 * <p>Das Original nutzt die alte {@code EntityCreature}-KI (ohne Aufgabenliste): Ziel suchen
 * ({@link #findPlayerToAttack()}), Pfad dorthin, Nahkampf bei unter zwei Bloecken alle 20 Ticks, sonst
 * gelegentliches Umherstreifen bevorzugt ins Dunkle. Diese Schleife ist hier in {@link #customServerAiStep()}
 * nachgebaut; Bewegung laeuft wie bei der alten KI mit dem Grundfaktor 0,1, die Geschwindigkeitswerte gehen als
 * Vorwaertsanteil (gedeckelt auf 1) ein.</p>
 */
public class EntityGlyphid extends Monster implements IResistanceProvider {

    //I might have overdone it a little bit

    public boolean hasHome = false;
    public int homeX;
    public int homeY;
    public int homeZ;
    protected int currentTask = 0;

    //both of those below are used for digging, so the glyphid remembers what it was doing
    protected int previousTask;
    protected EntityWaypoint previousWaypoint;
    public int taskX;
    public int taskY;
    public int taskZ;

    //used for digging, bigger glyphids have a longer reach
    public int blastSize = Math.min((int) (3 * (getGlyphidScale())) / 2, 5);
    public int blastResToDig = Math.min((int) (50 * (getGlyphidScale() * 2)), 150);
    public boolean shouldDig;

    // Tasks

    /** Idle state, only makes glpyhids wander around randomly */
    public static final int TASK_IDLE = 0;
    /** Causes the glyphid to walk to the waypoint, then communicate the FOLLOW task to nearby glyphids */
    public static final int TASK_RETREAT_FOR_REINFORCEMENTS = 1;
    /** Task used by scouts, if the waypoint is reached it will construct a new hive */
    public static final int TASK_BUILD_HIVE = 2;
    /** Creates a waypoint at the home position and then immediately initiates the RETREAT_FOR_REINFORCEMENTS task */
    public static final int TASK_INITIATE_RETREAT = 3;
    /** Will simply walk to the waypoint and enter IDLE once it is reached */
    public static final int TASK_FOLLOW = 4;
    /** Causes nuclear glyphids to immediately self-destruct, also signaling nearby scouts to retreat */
    public static final int TASK_TERRAFORM = 5;
    /** If any task other than IDLE is interrupted by an obstacle, initiates digging behavior which is also communicated to nearby glyohids */
    public static final int TASK_DIG = 6;

    protected boolean hasWaypoint = false;
    /** Yeah, fuck, whatever, anything goes now */
    protected EntityWaypoint taskWaypoint = null;

    //subtypes
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_INFECTED = 1;
    public static final int TYPE_RADIOACTIVE = 2;

    //data watcher keys
    private static final EntityDataAccessor<Byte> DW_WALL = SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DW_ARMOR = SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DW_SUBTYPE = SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BYTE);

    /** Entity-Ereignis fuer {@code swingItem()} (Original: Animationspaket an die Beobachter). */
    private static final byte EVENT_SWING = 101;

    /** Original {@code entityToAttack}: Spieler oder ein vorrangiger Wegpunkt. */
    @Nullable protected Entity entityToAttack;
    /** Original {@code attackTime}. */
    protected int attackTime;
    /** Original {@code fleeingTick}. */
    protected int fleeingTick;
    @Nullable protected Path pathToEntity;

    // Original swingProgress/swingProgressInt mit eigener Dauer
    public boolean isSwingInProgress;
    public int swingProgressInt;
    public float swingProgress;
    public float prevSwingProgress;

    public EntityGlyphid(EntityType<? extends EntityGlyphid> type, Level world) {
        super(type, world);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder attributes(StatBundle stats) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, stats.health)
                .add(Attributes.MOVEMENT_SPEED, stats.speed)
                .add(Attributes.ATTACK_DAMAGE, stats.damage)
                .add(Attributes.FOLLOW_RANGE, 64D);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getGrunt());
    }

    public String getSkinName() {
        return "glyphid";
    }

    public double getGlyphidScale() {
        return 1.0D;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DW_WALL, (byte) 0);           //wall climbing
        this.entityData.define(DW_ARMOR, (byte) 0b11111);    //armor
        this.entityData.define(DW_SUBTYPE, (byte) 0);        //subtype (i.e. normal, infected, etc)
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DW_WALL, (byte) 0);
        builder.define(DW_ARMOR, (byte) 0b11111);
        builder.define(DW_SUBTYPE, (byte) 0);
    }
    *///?}

    @Override
    protected void registerGoals() {
        // Original: keine Aufgabenliste; nur das Schwimmen der alten KI (isJumping im Wasser)
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    public byte getArmorBits() { return this.entityData.get(DW_ARMOR); }
    public int getSubtype() { return this.entityData.get(DW_SUBTYPE); }
    public void setSubtype(int type) { this.entityData.set(DW_SUBTYPE, (byte) type); }

    public StatBundle getStats() {
        return GlyphidStats.getStats().statsGrunt;
    }

    @Override
    public float[] getCurrentDTDR(DamageSource damage, float amount, float pierceDT, float pierce) {
        if (DamageResistanceHandler.isDamageAbsolute(damage) || damage.is(DamageTypeTags.BYPASSES_ARMOR)) return new float[] { 0F, 0F };
        StatBundle stats = this.getStats();
        float threshold = stats.thresholdMultForArmor * getGlyphidArmor() / 5F;

        String type = damage.getMsgId();
        if (damage.is(ModDamageTypes.NUCLEAR_BLAST)) return new float[] { threshold * 0.25F, 0F }; // nukes shred shrough glyphids
        if (type.equals(DamageClass.LASER.name().toLowerCase(Locale.US))) return new float[] { threshold * 0.5F, stats.resistanceMult * 0.5F }; //lasers are quite powerful too
        if (type.equals(DamageClass.ELECTRIC.name().toLowerCase(Locale.US))) return new float[] { threshold * 0.25F, stats.resistanceMult * 0.25F }; //electricity even more so
        if (type.equals(DamageClass.SUBATOMIC.name().toLowerCase(Locale.US))) return new float[] { 0F, stats.resistanceMult * 0.1F }; //and particles are almsot commpletely unaffected

        if (damage.is(DamageTypeTags.IS_FIRE)) return new float[] { 0F, stats.resistanceMult * 0.2F }; //fire ignores DT and most DR
        if (damage.is(DamageTypeTags.IS_EXPLOSION)) return new float[] { threshold * 0.5F, stats.resistanceMult * 0.35F }; //explosions  are still subject to DT and reduce DR by a fair amount

        return new float[] { threshold, stats.resistanceMult };
    }

    @Override
    public void onDamageDealt(DamageSource damage, float amount) {
        if (this.isArmorBroken(amount)) this.breakOffArmor();
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (!hasHome) {
                homeX = (int) getX();
                homeY = (int) getY();
                homeZ = (int) getZ();
                hasHome = true;
            }

            if (this.hasEffect(MobEffects.BLINDNESS)) {
                onBlinded();
            }

            if (getCurrentTask() == TASK_FOLLOW) {

                //incase the waypoint somehow doesn't exist and it got this task anyway
                if (isAtDestination() && !hasWaypoint) {
                    setCurrentTask(TASK_IDLE, null);
                }
            //the task cannot be 6 outside of rampant, so this is a non issue p much
            } else if (getCurrentTask() == TASK_DIG && tickCount % 20 == 0 && isAtDestination()) {
                swingItem();

                ExplosionVNT vnt = new ExplosionVNT(level(), taskX, taskY + 2, taskZ, blastSize, this);
                vnt.setBlockAllocator(new BlockAllocatorGlyphidDig(blastResToDig));
                vnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                vnt.setEntityProcessor(null);
                vnt.setPlayerProcessor(null);
                vnt.explode();

                this.setCurrentTask(previousTask, previousWaypoint);
            }

            this.setBesideClimbableBlock(this.horizontalCollision);

            if (tickCount % 100 == 0) {
                this.swingItem();
            }
        }

        updateArmSwingProgress();
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        Item drop = isOnFire() ? ModItems.GLYPHID_MEAT_GRILLED.get() : ModItems.GLYPHID_MEAT.get();
        if (random.nextInt(2) == 0) this.spawnAtLocation(new ItemStack(drop, ((int) getGlyphidScale() * 2) + looting), 0F);
    }

    /** Original {@code findPlayerToAttack}: naechster angreifbarer Spieler in 16 (erweitert 128) Bloecken. */
    @Nullable
    protected Entity findPlayerToAttack() {
        if (this.hasEffect(MobEffects.BLINDNESS)) return null;
        return closestVulnerablePlayer(useExtendedTargeting() ? 128D : 16D);
    }

    @Nullable
    protected Player closestVulnerablePlayer(double range) {
        return level().getNearestPlayer(TargetingConditions.forCombat().range(range).ignoreLineOfSight(), this);
    }

    @Nullable
    public Entity getEntityToAttack() {
        return entityToAttack;
    }

    public void setEntityToAttack(@Nullable Entity e) {
        this.entityToAttack = e;
        this.setTarget(e instanceof LivingEntity l ? l : null);
    }

    // ═══════════════════════════ alte EntityCreature-KI ═══════════════════════════

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.attackTime > 0) --this.attackTime;
        if (this.fleeingTick > 0) --this.fleeingTick;

        float f4 = 16.0F;

        if (this.entityToAttack == null) {
            Entity found = this.findPlayerToAttack();
            if (found != null) {
                this.setEntityToAttack(found);
                this.pathToEntity = this.getNavigation().createPath(found, 0);
            }
        } else if (this.entityToAttack.isAlive()) {
            float f = this.entityToAttack.distanceTo(this);
            if (this.hasLineOfSight(this.entityToAttack)) this.attackEntity(this.entityToAttack, f);
        } else {
            this.setEntityToAttack(null);
        }

        if (this.entityToAttack instanceof Player p && (p.isCreative() || p.isSpectator())) {
            this.setEntityToAttack(null);
        }

        if (this.entityToAttack != null && (this.pathToEntity == null || this.random.nextInt(20) == 0)) {
            this.pathToEntity = this.getNavigation().createPath(this.entityToAttack, 0);
        } else if ((this.pathToEntity == null && this.random.nextInt(180) == 0 || this.random.nextInt(120) == 0 || this.fleeingTick > 0) && this.noActionTime < 100) {
            this.updateWanderPath();
        }

        this.updateEntityActionState();

        if (this.pathToEntity != null && this.getNavigation().getPath() != this.pathToEntity) {
            this.getNavigation().moveTo(this.pathToEntity, legacySpeedMod());
        }
        if (this.getNavigation().isDone()) this.pathToEntity = null;

        if (this.entityToAttack != null) {
            this.getLookControl().setLookAt(this.entityToAttack, 30.0F, 30.0F);
        }
    }

    /** Alte KI: Vorwaerts = Geschwindigkeitswert (normiert auf hoechstens 1), Grundfaktor 0,1. */
    protected double legacySpeedMod() {
        double attr = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (attr <= 0) return 0;
        // MoveControl setzt speed = attr * mod; gewuenscht ist 0,1 * min(1, attr) (moveFlying normiert den Vorwaertsanteil)
        return 0.1D * Math.min(1D, attr) / attr;
    }

    /** Original {@code EntityMob.attackEntity}: Nahkampf unter 2 Bloecken bei Hoehenueberschneidung, alle 20 Ticks. */
    protected void attackEntity(Entity target, float dist) {
        if (this.attackTime <= 0 && dist < 2.0F && target.getBoundingBox().maxY > this.getBoundingBox().minY && target.getBoundingBox().minY < this.getBoundingBox().maxY) {
            this.attackTime = 20;
            this.doHurtTarget(target);
        }
    }

    /** Original {@code EntityCreature.updateWanderPath}: bester von zehn Zufallspunkten (Monster: dunkler ist besser). */
    protected void updateWanderPath() {
        if (getCurrentTask() != TASK_IDLE) return;

        boolean found = false;
        int x = -1, y = -1, z = -1;
        float best = -99999.0F;

        for (int l = 0; l < 10; ++l) {
            int i1 = Mth.floor(this.getX() + this.random.nextInt(13) - 6.0D);
            int j1 = Mth.floor(this.getY() + this.random.nextInt(7) - 3.0D);
            int k1 = Mth.floor(this.getZ() + this.random.nextInt(13) - 6.0D);
            float f1 = this.getWalkTargetValue(new BlockPos(i1, j1, k1));

            if (f1 > best) {
                best = f1;
                x = i1;
                y = j1;
                z = k1;
                found = true;
            }
        }

        if (found) {
            this.pathToEntity = this.getNavigation().createPath(new BlockPos(x, y, z), 0);
        }
    }

    /** Original {@code updateEntityActionState} der Glyphide: Neuzielsuche, erweiterte Verfolgung, Wegpunkte, Graben. */
    protected void updateEntityActionState() {

        // re-scan for new targets every so often
        // every third glyphid does not do this, so you cannot "juggle" hordes on purpose
        if (this.getId() % 3 > 0 && (this.getId() + this.tickCount) % 100 == 0) {
            Entity newTarget = this.findPlayerToAttack();
            if (newTarget != null) this.setEntityToAttack(newTarget);
        }

        if (!this.hasEffect(MobEffects.BLINDNESS)) {
            if (this.pathToEntity == null) {

                // hell yeah!!
                if (useExtendedTargeting() && this.entityToAttack != null) {
                    this.pathToEntity = PathFinderUtils.getPathEntityToEntityPartial(level(), this, this.entityToAttack, 16F);
                } else if (getCurrentTask() != TASK_IDLE) {

                    if (!isAtDestination()) {

                        if (taskWaypoint != null) {

                            taskX = (int) taskWaypoint.getX();
                            taskY = (int) taskWaypoint.getY();
                            taskZ = (int) taskWaypoint.getZ();

                            if (taskWaypoint.highPriority) {
                                setEntityToAttack(taskWaypoint);
                            }

                        }

                        if (hasWaypoint) {

                            if (canDig()) {

                                BlockHitResult obstacle = findWaypointObstruction();
                                if (getGlyphidScale() >= 1 && getCurrentTask() != TASK_DIG && obstacle != null) {
                                    digToWaypoint(obstacle);
                                } else {
                                    int maxDist = (int) (Math.sqrt(this.distanceToSqr(taskX, taskY, taskZ)) * 1.2);
                                    this.pathToEntity = PathFinderUtils.getPathEntityToCoordPartial(level(), this, taskX, taskY, taskZ, maxDist);
                                }

                            } else {
                                int maxDist = (int) (Math.sqrt(this.distanceToSqr(taskX, taskY, taskZ)) * 1.2);
                                this.pathToEntity = PathFinderUtils.getPathEntityToCoordPartial(level(), this, taskX, taskY, taskZ, maxDist);
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean canDig() {
        return MobConfig.rampantDig();
    }

    public void onBlinded() {
        this.setEntityToAttack(null);
        this.pathToEntity = null;
        this.getNavigation().stop();
        this.fleeingTick = 80;

        if (getGlyphidScale() >= 1.25) {
            if (tickCount % 20 == 0) {
                for (int i = 0; i < 16; i++) {
                    float angle = (float) Math.toRadians(360D / 16 * i);
                    Vec3 rot = new Vec3(0, 0, 4).yRot(angle);
                    Vec3 pos = new Vec3(this.getX(), this.getY() + 1, this.getZ());
                    Vec3 nextPos = new Vec3(this.getX() + rot.x, this.getY() + 1, this.getZ() + rot.z);
                    BlockHitResult mop = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

                    if (mop.getType() == HitResult.Type.BLOCK) {

                        BlockState block = level().getBlockState(mop.getBlockPos());

                        if (block.is(ModBlocks.LANTERN.get())) {
                            setYRot(360F / 16 * i);
                            swingItem();
                            level().destroyBlock(mop.getBlockPos(), false);
                        }
                    }
                }
            }
        }
    }

    public boolean useExtendedTargeting() {
        return MobConfig.rampantExtendedTargetting() || PollutionHandler.getPollution(level(), Mth.floor(getX()), Mth.floor(getY()), Mth.floor(getZ()), PollutionType.SOOT) >= MobConfig.targetingThreshold();
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return entityToAttack == null && getCurrentTask() == TASK_IDLE && this.tickCount > 100;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!level().isClientSide && doesInfectedSpawnMaggots() && this.getSubtype() == TYPE_INFECTED) {

            int j = 2 + this.random.nextInt(3);

            for (int k = 0; k < j; ++k) {
                float f = ((float) (k % 2) - 0.5F) * 0.5F;
                float f1 = ((float) (k / 2) - 0.5F) * 0.5F;
                EntityParasiteMaggot maggot = new EntityParasiteMaggot(ModEntities.PARASITE_MAGGOT.get(), level());
                maggot.moveTo(this.getX() + f, this.getY() + 0.5D, this.getZ() + f1, this.random.nextFloat() * 360.0F, 0.0F);
                maggot.setDeltaMovement(f, 0, f1);
                maggot.hurtMarked = true;
                level().addFreshEntity(maggot);
            }

            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.95F + level().random.nextFloat() * 0.2F);

            CompoundTag vdat = new CompoundTag();
            vdat.putString("type", "giblets");
            vdat.putInt("ent", this.getId());
            com.hbm_m.particle.helper.IParticleCreator.sendPacket((ServerLevel) level(), getX(), getY() + getBbHeight() * 0.5, getZ(), 150, vdat);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof EntityGlyphid) return false;
        return GlyphidStats.getStats().handleAttack(this, source, amount);
    }

    /** Provides a direct entrypoint from outside to access the superclass' implementation because otherwise we end up with infinite recursion */
    public boolean attackSuperclass(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    public boolean doesInfectedSpawnMaggots() {
        return true;
    }

    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.6, 2), 100);
    }

    public void breakOffArmor() {
        byte armor = this.entityData.get(DW_ARMOR);
        List<Integer> indices = Arrays.asList(0, 1, 2, 3, 4);
        Collections.shuffle(indices);

        for (Integer i : indices) {
            byte bit = (byte) (1 << i);
            if ((armor & bit) > 0) {
                armor &= ~bit;
                armor = (byte) (armor & 0b11111);
                this.entityData.set(DW_ARMOR, armor);
                level().playSound(null, this, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0F, 1.25F);
                break;
            }
        }
    }

    public int getGlyphidArmor() {
        int total = 0;
        byte armor = this.entityData.get(DW_ARMOR);
        for (int i = 0; i < 5; i++) {
            total += (armor & (1 << i)) != 0 ? 1 : 0;
        }
        return total;
    }

    // ═══════════════════════════ Schwung ═══════════════════════════

    /** Original {@code swingItem()}: startet den Biss, der Server meldet ihn den Beobachtern. */
    public void swingItem() {
        if (!this.isSwingInProgress || this.swingProgressInt >= this.swingDuration() / 2 || this.swingProgressInt < 0) {
            this.swingProgressInt = -1;
            this.isSwingInProgress = true;
            if (!level().isClientSide) level().broadcastEntityEvent(this, EVENT_SWING);
        }
        this.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SWING) {
            this.swingProgressInt = -1;
            this.isSwingInProgress = true;
            return;
        }
        super.handleEntityEvent(id);
    }

    /** Original {@code updateArmSwingProgress} mit eigener Dauer. */
    protected void updateArmSwingProgress() {
        this.prevSwingProgress = this.swingProgress;
        int i = this.swingDuration();

        if (this.isSwingInProgress) {
            ++this.swingProgressInt;

            if (this.swingProgressInt >= i) {
                this.swingProgressInt = 0;
                this.isSwingInProgress = false;
            }
        } else {
            this.swingProgressInt = 0;
        }

        this.swingProgress = (float) this.swingProgressInt / (float) i;
    }

    public float getGlyphidSwingProgress(float interp) {
        float f = this.swingProgress - this.prevSwingProgress;
        if (f < 0.0F) ++f;
        return this.prevSwingProgress + f * interp;
    }

    public int swingDuration() {
        return 15;
    }

    /** Original {@code setInWeb()}: Spinnweben bremsen nicht. */
    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (state.is(net.minecraft.world.level.block.Blocks.COBWEB)) return;
        super.makeStuckInBlock(state, motionMultiplier);
    }

    @Override
    public boolean onClimbable() {
        return this.isBesideClimbableBlock();
    }

    public boolean isBesideClimbableBlock() {
        return (this.entityData.get(DW_WALL) & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbable) {
        byte watchable = this.entityData.get(DW_WALL);

        if (climbable) {
            watchable = (byte) (watchable | 1);
        } else {
            watchable &= -2;
        }

        this.entityData.set(DW_WALL, watchable);
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        if (this.isSwingInProgress) return false;
        this.swingItem();

        if (this.getSubtype() == TYPE_INFECTED && victim instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }

        return super.doHurtTarget(victim);
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    /// TASK SYSTEM START ///
    public int getCurrentTask() {
        return currentTask;
    }

    public EntityWaypoint getWaypoint() {
        return taskWaypoint;
    }

    /**
     * Sets a new task for the glyphid to do, a waypoint alongside with that task, and refreshes their waypoint coordinates
     * @param task The task the glyphid is to do, refer to carryOutTask()
     * @param waypoint The waypoint for the task, can be null
     */
    public void setCurrentTask(int task, @Nullable EntityWaypoint waypoint) {
        this.currentTask = task;
        this.taskWaypoint = waypoint;
        this.hasWaypoint = waypoint != null;
        if (taskWaypoint != null) {

            taskX = (int) taskWaypoint.getX();
            taskY = (int) taskWaypoint.getY();
            taskZ = (int) taskWaypoint.getZ();

            if (taskWaypoint.highPriority) {
                this.setEntityToAttack(null);
                this.pathToEntity = null;
                this.getNavigation().stop();
            }

        }
        carryOutTask();
    }

    /**
     * Handles the task system, used mainly for things that only need to be done once, such as setting targets
     */
    public void carryOutTask() {
        int task = getCurrentTask();

        switch (task) {

            case TASK_RETREAT_FOR_REINFORCEMENTS:
                if (taskWaypoint != null) {
                    communicate(TASK_FOLLOW, taskWaypoint);
                    setCurrentTask(TASK_FOLLOW, taskWaypoint);
                }
                break;

            case TASK_INITIATE_RETREAT:

                if (!level().isClientSide && taskWaypoint == null) {

                    // Then, Come back later
                    EntityWaypoint additional = new EntityWaypoint(level());
                    additional.moveTo(getX(), getY(), getZ(), 0, 0);

                    // First, go home and get reinforcements
                    EntityWaypoint home = new EntityWaypoint(level());
                    home.setWaypointType(TASK_RETREAT_FOR_REINFORCEMENTS);
                    home.setAdditionalWaypoint(additional);
                    home.setHighPriority();
                    home.moveTo(homeX, homeY, homeZ, 0, 0);
                    level().addFreshEntity(home);

                    this.taskWaypoint = home;
                    communicate(TASK_FOLLOW, home);
                    setCurrentTask(TASK_FOLLOW, taskWaypoint);

                    break;
                }

                break;

            case TASK_DIG:
                shouldDig = true;
                break;

            default:
                break;

        }

    }

    /** Copies tasks and waypoint to nearby glyphids. Does not work on glyphid scouts */
    public void communicate(int task, @Nullable EntityWaypoint waypoint) {
        int radius = waypoint != null ? waypoint.radius : 4;
        AABB bb = new AABB(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY(), this.getZ()).inflate(radius);

        List<Entity> bugs = level().getEntities(this, bb);
        for (Entity e : bugs) {
            if (e instanceof EntityGlyphid glyphid && !(e instanceof EntityGlyphidScout)) {
                if (glyphid.getCurrentTask() != task) {
                    glyphid.setCurrentTask(task, waypoint);
                }
            }
        }
    }

    /** What each type of glyphid does when it is time to expand the hive.
     * @return Whether it has expanded successfully or not
     * **/
    public boolean expandHive() {
        return false;
    }

    public boolean isAtDestination() {
        int destinationRadius = taskWaypoint != null ? (int) Math.pow(taskWaypoint.radius, 2) : 25;
        return this.distanceToSqr(taskX, taskY, taskZ) <= destinationRadius;
    }
    ///TASK SYSTEM END

    ///DIGGING SYSTEM START

    /** Handles the special digging system, used in Rampant mode due to high potential for destroyed bases**/
    @Nullable
    public BlockHitResult findWaypointObstruction() {
        Vec3 bugVec = new Vec3(getX(), getY() + getEyeHeight(), getZ());
        Vec3 waypointVec = new Vec3(taskX, taskY, taskZ);
        BlockHitResult obstruction = level().clip(new ClipContext(bugVec, waypointVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (obstruction.getType() == HitResult.Type.BLOCK) {
            BlockState blockHit = level().getBlockState(obstruction.getBlockPos());
            if (blockHit.getBlock().getExplosionResistance() <= blastResToDig) {
                return obstruction;
            }
        }
        return null;
    }

    public void digToWaypoint(BlockHitResult obstacle) {

        EntityWaypoint target = new EntityWaypoint(level());
        BlockPos b = obstacle.getBlockPos();
        target.moveTo(b.getX(), b.getY(), b.getZ(), 0, 0);
        target.radius = 5;
        level().addFreshEntity(target);

        previousTask = getCurrentTask();
        previousWaypoint = getWaypoint();

        setCurrentTask(TASK_DIG, target);

        int maxDist = (int) (Math.sqrt(this.distanceToSqr(taskX, taskY, taskZ)) * 1.2);
        this.pathToEntity = PathFinderUtils.getPathEntityToCoordPartial(level(), this, taskX, taskY, taskZ, maxDist);

        communicate(TASK_DIG, target);

    }
    ///DIGGING END

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putByte("armor", this.entityData.get(DW_ARMOR));
        nbt.putByte("subtype", this.entityData.get(DW_SUBTYPE));

        nbt.putBoolean("hasHome", hasHome);
        nbt.putInt("homeX", homeX);
        nbt.putInt("homeY", homeY);
        nbt.putInt("homeZ", homeZ);

        nbt.putBoolean("hasWaypoint", hasWaypoint);
        nbt.putInt("taskX", taskX);
        nbt.putInt("taskY", taskY);
        nbt.putInt("taskZ", taskZ);

        nbt.putInt("task", currentTask);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(DW_ARMOR, nbt.getByte("armor"));
        this.entityData.set(DW_SUBTYPE, nbt.getByte("subtype"));

        this.hasHome = nbt.getBoolean("hasHome");
        this.homeX = nbt.getInt("homeX");
        this.homeY = nbt.getInt("homeY");
        this.homeZ = nbt.getInt("homeZ");

        this.hasWaypoint = nbt.getBoolean("hasWaypoint");
        this.taskX = nbt.getInt("taskX");
        this.taskY = nbt.getInt("taskY");
        this.taskZ = nbt.getInt("taskZ");

        this.currentTask = nbt.getInt("task");
    }

    /** Original {@code getCanSpawnHere}. */
    public boolean getCanSpawnHere() {
        return level().getDifficulty() != Difficulty.PEACEFUL && level().isUnobstructed(this) && level().noCollision(this) && !level().containsAnyLiquid(this.getBoundingBox());
    }
}
