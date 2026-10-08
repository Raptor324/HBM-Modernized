package com.hbm_m.entity.mob;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.item.ModItems;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code EntityMaskMan} - a 1000 HP boss that shrugs off most of what you throw at it
 * and can be killed instantly by a thrown egg.
 *
 * <p>The damage table is the interesting part and is reproduced exactly: fire and magic do
 * nothing, projectiles and explosions are halved, and everything above 50 in a single hit is
 * halved past that threshold, so burst damage is heavily punished. Half health triggers a one-off
 * explosion above its head, and its face changes to a skull - see the renderer.</p>
 *
 * <p>Die Laserkanone feuert wie im Original {@code EntityBulletBaseNT} (BulletConfigSyncingUtil),
 * das Minigun 7.62 FMJ aus {@code XFactory762mm} (SEDNA).</p>
 */
public class EntityMaskMan extends Monster implements IRadiationImmune {

    /** Above this in a single hit, further damage is halved. */
    private static final float SOFT_CAP = 50F;
    /** The original's one-in-ten instant kill when hit by a thrown egg. */
    private static final int EGG_KILL_CHANCE = 10;

    private float lastHealth;
    private boolean halfHealthBlown;

    // ─── Laser gun state ─────────────────────────────────────────────────────

    /** {@code EnumLaserAttack}: delay between shots and how many before switching. */
    private enum LaserAttack {
        ORB(60, 5),
        MISSILE(10, 10),
        SPLASH(40, 3);

        final int delay;
        final int amount;

        LaserAttack(int delay, int amount) {
            this.delay = delay;
            this.amount = amount;
        }
    }

    public EntityMaskMan(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.lastHealth = this.getMaxHealth();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // 1:1 Konstruktor EntityMaskMan (Prioritaeten und Mutex wie im Original)
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new CasualApproachGoal(this, Player.class, 1.0D, false));
        this.goalSelector.addGoal(2, new MinigunGoal(this, 3));
        this.goalSelector.addGoal(3, new LasergunGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true): Chance 0 = jeden Tick
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    // ─── KI 1:1 aus com.hbm.entity.mob.ai ───────────────────────────────────

    /** {@code EntityAIMaskmanCasualApproach}: haelt ~10 Bloecke Abstand, greift nie im Nahkampf an. */
    static class CasualApproachGoal extends Goal {
        private final PathfinderMob attacker;
        private final double speedTowardsTarget;
        private final boolean longMemory;
        private final Class<?> classTarget;
        private net.minecraft.world.level.pathfinder.Path entityPathEntity;
        private int attackTick;
        private int pathTimer;
        private double lastX, lastY, lastZ;
        private int failedPathFindingPenalty;

        CasualApproachGoal(PathfinderMob owner, Class<?> target, double speed, boolean longMemory) {
            this.attacker = owner;
            this.classTarget = target;
            this.speedTowardsTarget = speed;
            this.longMemory = longMemory;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); // setMutexBits(3)
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.attacker.getTarget();
            if (target == null) return false;
            if (!target.isAlive()) return false;
            if (this.classTarget != null && !this.classTarget.isAssignableFrom(target.getClass())) return false;

            if (--this.pathTimer <= 0) {
                double[] pos = getApproachPos();
                this.entityPathEntity = this.attacker.getNavigation().createPath(pos[0], pos[1], pos[2], 0);
                this.pathTimer = 4 + this.attacker.getRandom().nextInt(7);
                return this.entityPathEntity != null;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.attacker.getTarget();
            if (target == null || !target.isAlive()) return false;
            return !this.longMemory ? !this.attacker.getNavigation().isDone()
                    : this.attacker.isWithinRestriction(target.blockPosition());
        }

        @Override
        public void start() {
            this.attacker.getNavigation().moveTo(this.entityPathEntity, this.speedTowardsTarget);
            this.pathTimer = 0;
        }

        @Override
        public void stop() {
            this.attacker.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.attacker.getTarget();
            if (target == null) return;
            this.attacker.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double d0 = this.attacker.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());

            this.pathTimer--;

            if ((this.longMemory || this.attacker.getSensing().hasLineOfSight(target)) && this.pathTimer <= 0
                    && (this.lastX == 0.0D && this.lastY == 0.0D && this.lastZ == 0.0D
                    || target.distanceToSqr(this.lastX, this.lastY, this.lastZ) >= 1.0D
                    || this.attacker.getRandom().nextFloat() < 0.05F)) {

                this.lastX = target.getX();
                this.lastY = target.getBoundingBox().minY;
                this.lastZ = target.getZ();
                this.pathTimer = this.failedPathFindingPenalty + 4 + this.attacker.getRandom().nextInt(7);

                net.minecraft.world.level.pathfinder.Path path = this.attacker.getNavigation().getPath();
                if (path != null) {
                    net.minecraft.world.level.pathfinder.Node finalPathPoint = path.getEndNode();
                    if (finalPathPoint != null && target.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1) {
                        this.failedPathFindingPenalty = 0;
                    } else {
                        this.failedPathFindingPenalty += 10;
                    }
                } else {
                    this.failedPathFindingPenalty += 10;
                }

                if (d0 > 1024.0D) {
                    this.pathTimer += 10;
                } else if (d0 > 256.0D) {
                    this.pathTimer += 5;
                }

                double[] pos = getApproachPos();
                if (!this.attacker.getNavigation().moveTo(pos[0], pos[1], pos[2], this.speedTowardsTarget)) {
                    this.pathTimer += 15;
                }
            }

            this.attackTick = Math.max(this.attackTick - 1, 0);
            // Nahkampf ist im Original auskommentiert.
        }

        private double[] getApproachPos() {
            LivingEntity target = this.attacker.getTarget();
            Vec3 vec = new Vec3(this.attacker.getX() - target.getX(), this.attacker.getY() - target.getY(), this.attacker.getZ() - target.getZ());
            double range = Math.min(vec.length(), 20) - 10;
            vec = vec.normalize();
            double x = this.attacker.getX() + vec.x * range + this.attacker.getRandom().nextGaussian() * 2;
            double y = this.attacker.getY() + vec.y - 5 + this.attacker.getRandom().nextInt(11);
            double z = this.attacker.getZ() + vec.z * range + this.attacker.getRandom().nextGaussian() * 2;
            return new double[] {x, y, z};
        }
    }

    /** {@code EntityAIMaskmanMinigun}: 7.62 FMJ alle {@code delay} Ticks im 5-10-Bloecke-Band. */
    static class MinigunGoal extends Goal {
        private final PathfinderMob owner;
        private LivingEntity target;
        private final int delay;
        private int timer;

        MinigunGoal(PathfinderMob owner, int delay) {
            this.owner = owner;
            this.delay = delay;
            this.timer = delay;
        }

        @Override
        public boolean canUse() {
            LivingEntity entity = this.owner.getTarget();
            if (entity == null || !entity.isAlive()) return false;
            this.target = entity;
            double dist = new Vec3(target.getX() - owner.getX(), target.getY() - owner.getY(), target.getZ() - owner.getZ()).length();
            return dist > 5 && dist < 10;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() || !this.owner.getNavigation().isDone();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            timer--;

            if (target != null) this.owner.getLookControl().setLookAt(this.target, 15F, 15F);

            if (timer <= 0) {
                timer = delay;
                // 1:1: SEDNA-Geschoss 7.62 FMJ aus XFactory762mm
                com.hbm_m.entity.projectile.EntityBulletBaseMK4 bullet = new com.hbm_m.entity.projectile.EntityBulletBaseMK4(this.owner,
                        com.hbm_m.item.weapon.sedna.factory.XFactory762mm.r762_fmj, 5F, 0.075F, -1.5, -1.5, 0);
                owner.level().addFreshEntity(bullet);
                owner.playSound(com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.calShoot"), 1.0F, 1.0F);
            }
        }
    }

    /** {@code EntityAIMaskmanLasergun}: ab 10 Bloecken Kugel-, Raketen- und Leuchtspursalven. */
    static class LasergunGoal extends Goal {
        private final PathfinderMob owner;
        private LivingEntity target;
        private LaserAttack attack;
        private int timer;
        private int attackCount;

        LasergunGoal(PathfinderMob owner) {
            this.owner = owner;
            this.attack = LaserAttack.values()[owner.getRandom().nextInt(3)];
        }

        @Override
        public boolean canUse() {
            LivingEntity entity = this.owner.getTarget();
            if (entity == null) return false;
            this.target = entity;
            double dist = new Vec3(target.getX() - owner.getX(), target.getY() - owner.getY(), target.getZ() - owner.getZ()).length();
            return dist > 10;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() || !this.owner.getNavigation().isDone();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            timer--;

            if (timer <= 0) {
                timer = attack.delay;

                // 1:1 EntityAIMaskmanLasergun mit EntityBulletBaseNT (MASKMAN_ORB / _ROCKET / _TRACER)
                switch (attack) {
                    case ORB -> {
                        com.hbm_m.entity.projectile.EntityBulletBaseNT orb = com.hbm_m.entity.projectile.EntityBulletBaseNT.create(owner.level(),
                                com.hbm_m.handler.BulletConfigSyncingUtil.MASKMAN_ORB, owner, target, 2.0F, 0);
                        orb.setDeltaMovement(orb.getDeltaMovement().add(0, 0.5D, 0));
                        owner.level().addFreshEntity(orb);
                        owner.playSound(com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.teslaShoot"), 1.0F, 1.0F);
                    }
                    case MISSILE -> {
                        com.hbm_m.entity.projectile.EntityBulletBaseNT missile = com.hbm_m.entity.projectile.EntityBulletBaseNT.create(owner.level(),
                                com.hbm_m.handler.BulletConfigSyncingUtil.MASKMAN_ROCKET, owner, target, 1.0F, 0);
                        Vec3 vec = new Vec3(target.getX() - owner.getX(), 0, target.getZ() - owner.getZ());
                        missile.setDeltaMovement(vec.x * 0.05D, 0.5D + owner.getRandom().nextDouble() * 0.5D, vec.z * 0.05D);
                        owner.level().addFreshEntity(missile);
                        owner.playSound(com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.hkShoot"), 1.0F, 1.0F);
                    }
                    case SPLASH -> {
                        for (int i = 0; i < 5; i++) {
                            com.hbm_m.entity.projectile.EntityBulletBaseNT tracer = com.hbm_m.entity.projectile.EntityBulletBaseNT.create(owner.level(),
                                    com.hbm_m.handler.BulletConfigSyncingUtil.MASKMAN_TRACER, owner, target, 1.0F, 0.05F);
                            owner.level().addFreshEntity(tracer);
                        }
                    }
                }

                attackCount++;

                if (attackCount >= attack.amount) {
                    attackCount = 0;
                    int newAtk = attack.ordinal() + owner.getRandom().nextInt(LaserAttack.values().length - 1);
                    attack = LaserAttack.values()[newAtk % LaserAttack.values().length];
                }
            }

            this.owner.setYRot(this.owner.getYHeadRot());
        }
    }

    @Override public boolean fireImmune()                { return true; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canBeAffected(@NotNull net.minecraft.world.effect.MobEffectInstance effect) { return false; }

    @Override protected SoundEvent getHurtSound(@NotNull DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }

    /** Half health is what flips the face to a skull; the renderer reads this. */
    public boolean isUnmasked() {
        return this.getHealth() < this.getMaxHealth() / 2;
    }

    // ─── Damage table ────────────────────────────────────────────────────────

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // A thrown egg has a one-in-ten chance to end it outright, worth no XP at all.
        if (source.getDirectEntity() instanceof ThrownEgg && this.random.nextInt(EGG_KILL_CHANCE) == 0) {
            this.xpReward = 0;
            // 1.7.10 runs die() out of its own tick once health hits zero, so setHealth(0) was
            // enough there. 1.20 only calls die() from hurt(), so setting health directly left a
            // corpse that never dropped its mask and never awarded the advancement. Route the
            // kill through super.hurt instead, which skips the damage table below by design.
            return super.hurt(source, Float.MAX_VALUE);
        }

        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA) || source.is(DamageTypes.HOT_FLOOR)) {
            amount = 0;
        }
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            amount = 0;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) amount *= 0.5F;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;

        if (amount > SOFT_CAP) {
            amount = SOFT_CAP + (amount - SOFT_CAP) * 0.5F;
        }

        return super.hurt(source, amount);
    }

    // ─── Behaviour ───────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Crossing half health once blows a hole above its head.
        if (!halfHealthBlown && this.lastHealth >= this.getMaxHealth() / 2
                && this.getHealth() < this.getMaxHealth() / 2 && this.isAlive()) {
            halfHealthBlown = true;
            if (!this.level().isClientSide) {
                this.level().explode(this, this.getX(), this.getY() + 4, this.getZ(),
                        2.5F, Level.ExplosionInteraction.TNT); // createExplosion(..., true): zerstoert immer Bloecke
            }
        }
        this.lastHealth = this.getHealth();

        if (!this.level().isClientSide) {
            updateBossBar();
        }
        // Minigun/Laserkanone/Annaeherung laufen wie im Original als KI-Aufgaben (registerGoals).
    }

    // ─── Death ───────────────────────────────────────────────────────────────

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        // The original credits everyone within 100 blocks, not 50 like the other bosses.
        ModAdvancements.grantNearby(this, 100D, ModAdvancements.BOSS_MASKMAN);
    }

    @Override
    //? if < 1.21.1 {
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
    //?} else {
    /*protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, @NotNull DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
    *///?}
        if (this.level().isClientSide) return;

        // Original: Gasmaske mit bereits eingesetztem Kombifilter (ArmorUtil.installGasMaskFilter)
        ItemStack mask = new ItemStack(ModItems.GAS_MASK_M65.get());
        com.hbm_m.item.gasmask.IGasMask.installFilter(mask, ModItems.GAS_MASK_FILTER_COMBO.get());
        this.spawnAtLocation(mask);
        this.spawnAtLocation(new ItemStack(ModItems.COIN_MASKMAN.get()));
        this.spawnAtLocation(new ItemStack(ModItems.BOTTLED_CLOUD.get()));
        this.spawnAtLocation(new ItemStack(Items.SKELETON_SKULL));
    }

    /**
     * Without persisting these, a reloaded MaskMan below half health starts with lastHealth at
     * full and the flag cleared, so he detonates over his own head again on the first tick after
     * every world load.
     */
    @Override
    public void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("halfHealthBlown", this.halfHealthBlown);
    }

    @Override
    public void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.halfHealthBlown = tag.getBoolean("halfHealthBlown");
        this.lastHealth = this.getHealth();
    }

    // ─── Boss bar ────────────────────────────────────────────────────────────
    // The original calls BossStatus.setBossStatus from its renderer every frame, which is how
    // 1.7.10 did boss bars. 1.20 has a real server-side ServerBossEvent instead, so the bar is
    // driven from the entity and correctly disappears when it dies or unloads.

    private final net.minecraft.server.level.ServerBossEvent bossEvent =
            new net.minecraft.server.level.ServerBossEvent(this.getDisplayName(),
                    net.minecraft.world.BossEvent.BossBarColor.RED,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);

    @Override
    public void startSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@org.jetbrains.annotations.Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    /** Keeps the bar in step with the health bar; call once per server tick. */
    protected void updateBossBar() {
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

}
