package com.hbm_m.entity.projectile;

import com.hbm_m.platform.PlatformHooks;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.bomb.BlockDetonatable;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code EntityBullet} (altes Pfeil-Geschoss): wird nur noch vom Kampfhubschrauber ({@code "chopper"}, Schaden
 * "chopperBullet", bleibt in Bloecken stecken) und der Cyberkrabbe ({@code critical + tau}: fliegt durch Bloecke, roter
 * Partikelschweif) benutzt. Lebt 250 Ticks.
 */
public class EntityBullet extends Entity {

    private static final EntityDataAccessor<Byte> CRITICAL = SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TAU = SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> CHOPPER = SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.BYTE);

    private int tileX = -1;
    private int tileY = -1;
    private int tileZ = -1;
    public double gravity = 0.0D;
    private BlockState inTile;
    private boolean inGround;
    public int canBePickedUp;
    public int arrowShake;
    @Nullable public Entity shootingEntity;
    private int ticksInGround;
    private int ticksInAir;
    public double damage;
    private int knockbackStrength;
    private boolean instakill = false;
    private boolean rad = false;
    public boolean antidote = false;
    public boolean pip = false;
    public boolean fire = false;

    public EntityBullet(EntityType<? extends EntityBullet> type, Level world) {
        super(type, world);
    }

    public EntityBullet(Level world) {
        this(ModEntities.BULLET.get(), world);
    }

    /** Original {@code (world, shooter, target, velocity, inaccuracy)}: auf ein Ziel gerichtet (Cyberkrabbe). */
    public EntityBullet(Level world, LivingEntity shooter, LivingEntity target, float velocity, float inaccuracy) {
        this(world);
        this.shootingEntity = shooter;

        if (shooter instanceof Player) this.canBePickedUp = 1;

        double posY = shooter.getY() + shooter.getEyeHeight() - 0.10000000149011612D;
        double d0 = target.getX() - shooter.getX();
        double d1 = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - posY;
        double d2 = target.getZ() - shooter.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        this.setPos(getX(), posY, getZ());

        if (d3 >= 1.0E-7D) {
            float f2 = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
            float f3 = (float) (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
            double d4 = d0 / d3;
            double d5 = d2 / d3;
            this.moveTo(shooter.getX() + d4, posY, shooter.getZ() + d5, f2, f3);
            this.setThrowableHeading(d0, d1, d2, velocity, inaccuracy);
        }
    }

    /** Original {@code (world, shooter, velocity, dmgMin, dmgMax, instakill, String type)}: Blickrichtung des Schuetzen. */
    public EntityBullet(Level world, LivingEntity shooter, float velocity, int dmgMin, int dmgMax, boolean instakill, String type) {
        this(world);
        this.shootingEntity = shooter;

        if (shooter instanceof Player) this.canBePickedUp = 1;

        if (shooter != null) this.moveTo(shooter.getX(), shooter.getY() + shooter.getEyeHeight(), shooter.getZ(), shooter.getYRot(), shooter.getXRot());
        double x = getX() - Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
        double y = getY() - 0.10000000149011612D;
        double z = getZ() - Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F;
        this.setPos(x, y, z);
        double mx = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double mz = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double my = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.setThrowableHeading(mx, my, mz, velocity * 1.5F, 1.0F);
        this.instakill = instakill;
        this.setTau("tauDay".equals(type));
        this.setChopper("chopper".equals(type));
        this.setIsCritical(!"chopper".equals(type));
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(CRITICAL, (byte) 0);
        this.entityData.define(TAU, (byte) 0);
        this.entityData.define(CHOPPER, (byte) 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(CRITICAL, (byte) 0);
        builder.define(TAU, (byte) 0);
        builder.define(CHOPPER, (byte) 0);
    }
    *///?}

    /** Original {@code setThrowableHeading}: Streuung 0.0075 je Achse mit Zufallsvorzeichen. */
    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
        double f2 = Math.sqrt(x * x + y * y + z * z);
        x /= f2;
        y /= f2;
        z /= f2;
        x += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        y += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        z += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        this.setDeltaMovement(x, y, z);
        double f3 = Math.sqrt(x * x + z * z);
        this.setYRot((float) (Math.atan2(x, z) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(y, f3) * 180.0D / Math.PI));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        this.ticksInGround = 0;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.setDeltaMovement(x, y, z);

        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double f = Math.sqrt(x * x + z * z);
            this.setYRot((float) (Math.atan2(x, z) * 180.0D / Math.PI));
            this.setXRot((float) (Math.atan2(y, f) * 180.0D / Math.PI));
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
            this.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            this.ticksInGround = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = getDeltaMovement();

        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
            this.yRotO = getYRot();
        }

        BlockPos tile = new BlockPos(tileX, tileY, tileZ);
        BlockState state = level().getBlockState(tile);

        if (!state.isAir()) {
            VoxelShape shape = state.getCollisionShape(level(), tile);

            if (!shape.isEmpty() && shape.bounds().move(tile).contains(position()) && !this.getIsCritical()) {
                this.inGround = true;
            }

            if (state.getBlock() instanceof BlockDetonatable det) {
                det.onShot(level(), tile);
            }

            if (isGlass(state)) {
                level().setBlockAndUpdate(tile, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level().playLocalSound(this.tileX, this.tileY, this.tileZ, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F, true);
            }
        }

        if (this.arrowShake > 0) {
            --this.arrowShake;
        }

        if (this.inGround && !this.getIsCritical()) {
            this.discard();

        } else {
            ++this.ticksInAir;
            Vec3 vec31 = position();
            Vec3 vec3 = position().add(motion);
            HitResult hit = level().clip(new ClipContext(vec31, vec3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.MISS) hit = null;
            vec31 = position();
            vec3 = position().add(motion);

            if (hit != null) {
                vec3 = hit.getLocation();
            }

            Entity entity = null;
            List<Entity> list = level().getEntities(this, this.getBoundingBox().expandTowards(motion).inflate(1.0D, 1.0D, 1.0D));
            double d0 = 0.0D;

            for (Entity entity1 : list) {

                if (entity1.isPickable() && (entity1 != this.shootingEntity || this.ticksInAir >= 5)) {
                    float f1 = 0.3F;
                    AABB axisalignedbb1 = entity1.getBoundingBox().inflate(f1, f1, f1);
                    var intercept = axisalignedbb1.clip(vec31, vec3);

                    if (intercept.isPresent()) {
                        double d1 = vec31.distanceTo(intercept.get());

                        if (d1 < d0 || d0 == 0.0D) {
                            entity = entity1;
                            d0 = d1;
                        }
                    }
                }
            }

            if (entity != null) {
                hit = new EntityHitResult(entity);
            }

            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player entityplayer) {

                if (entityplayer.getAbilities().invulnerable || this.shootingEntity instanceof Player shooter && !shooter.canHarmPlayer(entityplayer)) {
                    hit = null;
                }
            }

            if (hit != null) {
                if (hit instanceof EntityHitResult ehr) {
                    Entity entityHit = ehr.getEntity();

                    if (!(entityHit instanceof ItemFrame frame) || frame.getItem().isEmpty() || !frame.getItem().is(ModItems.FLAME_PONY.get())) {

                        DamageSource damagesource = null;

                        //L: Crit
                        //R: Chop
                        //X: NOT
                        //O: Direct

                        //   X X   Bullet
                        //    \|
                        //   O-X   Tau
                        //   |/
                        //   X-O   Displacer

                        Entity owner = this.shootingEntity == null ? this : this.shootingEntity;
                        if (!this.getIsCritical() && !this.getIsChopper()) {
                            damagesource = ModDamageSources.revolverBullet(this, owner);
                        } else if (!this.getIsChopper()) {
                            damagesource = ModDamageSources.tau(this, owner);
                        } else if (!this.getIsCritical()) {
                            damagesource = ModDamageSources.chopperBullet(this, owner);
                        }

                        if (damagesource == null) damagesource = ModDamageSources.revolverBullet(this, owner);

                        if (fire || this.isOnFire() && !(entityHit instanceof EnderMan)) {
                            PlatformHooks.setSecondsOnFire(entityHit, 5);
                        }

                        if (entityHit.hurt(damagesource, (float) damage)) {
                            if (entityHit instanceof LivingEntity entitylivingbase) {

                                if (rad) applyRad(entitylivingbase);

                                if (antidote)
                                    entitylivingbase.removeAllEffects();

                                if (this.knockbackStrength > 0) {
                                    double f4 = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

                                    if (f4 > 0.0F) {
                                        entityHit.push(motion.x * this.knockbackStrength * 0.6000000238418579D / f4, 0.1D,
                                                motion.z * this.knockbackStrength * 0.6000000238418579D / f4);
                                    }
                                }

                                if (this.shootingEntity instanceof LivingEntity shooterLiving) {
                                    //? if < 1.21.1 {
                                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostHurtEffects(entitylivingbase, this.shootingEntity);
                                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(shooterLiving, entitylivingbase);
                                    //?} else {
                                    /*// 1.21.1: Dornen- und Angreifer-Effekte in einem Aufruf
                                    if (this.level() instanceof net.minecraft.server.level.ServerLevel hbmSl)
                                        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(hbmSl, entitylivingbase, damagesource);
                                    *///?}
                                }

                                if (this.shootingEntity instanceof net.minecraft.server.level.ServerPlayer sp && entityHit != this.shootingEntity && entityHit instanceof Player) {
                                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
                                }
                            }

                            if (!(entityHit instanceof EnderMan)) {
                                if (!level().isClientSide) {
                                    if (instakill && !(entityHit instanceof Player) && entityHit instanceof LivingEntity living) {
                                        living.setHealth(0.0F);
                                    }
                                }
                            }
                        } else {

                            // Original: Treffer im Unverwundbarkeitsfenster mit damage + lastDamage nachholen (Reflexion)
                            if (entityHit instanceof LivingEntity living) {
                                try {
                                    float dmg = (float) damage + getLastHurt(living);
                                    entityHit.hurt(damagesource, dmg);
                                } catch (Exception x) { }
                            }
                        }
                    } else {
                        this.discard();
                    }
                } else if (!this.getIsCritical() && hit instanceof BlockHitResult bhr) {
                    BlockPos bp = bhr.getBlockPos();
                    this.tileX = bp.getX();
                    this.tileY = bp.getY();
                    this.tileZ = bp.getZ();
                    this.inTile = level().getBlockState(bp);
                    Vec3 m = bhr.getLocation().subtract(position());
                    this.setDeltaMovement(m);
                    double f2 = m.length();
                    this.setPos(getX() - m.x / f2 * 0.05000000074505806D, getY() - m.y / f2 * 0.05000000074505806D, getZ() - m.z / f2 * 0.05000000074505806D);
                    this.inGround = true;
                    this.arrowShake = 7;

                    if (!this.inTile.isAir()) {
                        this.inTile.entityInside(level(), bp, this);
                    }
                }
            }

            motion = getDeltaMovement();

            if (this.getIsCritical()) {
                for (int i = 0; i < 8; ++i) {
                    double px = getX() + motion.x * i / 8.0D, py = getY() + motion.y * i / 8.0D, pz = getZ() + motion.z * i / 8.0D;
                    if (!this.getIsTau())
                        level().addParticle(ParticleTypes.FIREWORK, px, py, pz, 0, 0, 0);
                    else
                        level().addParticle(DustParticleOptions.REDSTONE, px, py, pz, 0, 0, 0);
                }
            }

            this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
            this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

            float f3 = 0.99F;

            if (this.isInWater()) {
                for (int l = 0; l < 4; ++l) {
                    float f4 = 0.25F;
                    level().addParticle(ParticleTypes.BUBBLE, getX() - motion.x * f4, getY() - motion.y * f4, getZ() - motion.z * f4, motion.x, motion.y, motion.z);
                }

                f3 = 0.8F;
            }

            if (this.isInWaterRainOrBubble()) {
                this.clearFire();
            }

            this.setDeltaMovement(motion.x * f3, motion.y * f3 - gravity, motion.z * f3);
            this.checkInsideBlocks();
        }

        if (this.tickCount > 250)
            this.discard();
    }

    private static boolean isGlass(BlockState state) {
        Block b = state.getBlock();
        return b == net.minecraft.world.level.block.Blocks.GLASS || b instanceof net.minecraft.world.level.block.StainedGlassBlock
                || b == net.minecraft.world.level.block.Blocks.GLASS_PANE || b instanceof net.minecraft.world.level.block.StainedGlassPaneBlock;
    }

    /** Original {@code rad}: Creeper werden nuklear, Dorfbewohner Zombies, sonst Gift/Verdorren/Langsamkeit. */
    private void applyRad(LivingEntity entitylivingbase) {
        if (entitylivingbase instanceof Player p && com.hbm_m.util.ArmorUtil.checkForHazmat(p)) {
        } else if (entitylivingbase instanceof Creeper) {
            com.hbm_m.entity.mob.EntityCreeperNuclear creep = new com.hbm_m.entity.mob.EntityCreeperNuclear(com.hbm_m.entity.ModEntities.ENTITY_MOB_NUCLEAR_CREEPER.get(), level());
            creep.moveTo(entitylivingbase.getX(), entitylivingbase.getY(), entitylivingbase.getZ(), entitylivingbase.getYRot(), entitylivingbase.getXRot());
            if (entitylivingbase.isAlive())
                if (!level().isClientSide)
                    level().addFreshEntity(creep);
            entitylivingbase.discard();
        } else if (entitylivingbase instanceof Villager) {
            Zombie creep = new Zombie(level());
            creep.moveTo(entitylivingbase.getX(), entitylivingbase.getY(), entitylivingbase.getZ(), entitylivingbase.getYRot(), entitylivingbase.getXRot());
            entitylivingbase.discard();
            if (!level().isClientSide)
                level().addFreshEntity(creep);
        } else if (!(entitylivingbase instanceof com.hbm_m.entity.mob.EntityCreeperNuclear)
                && !(entitylivingbase instanceof net.minecraft.world.entity.animal.MushroomCow)
                && !(entitylivingbase instanceof Zombie)) {
            entitylivingbase.addEffect(new MobEffectInstance(MobEffects.POISON, 2 * 60 * 20, 2));
            entitylivingbase.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 4));
            entitylivingbase.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1 * 60 * 20, 1));
        }
    }

    private static java.lang.reflect.Field lastHurtField;

    private static float getLastHurt(LivingEntity living) throws Exception {
        if (lastHurtField == null) {
            java.lang.reflect.Field f;
            try {
                f = LivingEntity.class.getDeclaredField("lastHurt");
            } catch (NoSuchFieldException e) {
                f = LivingEntity.class.getDeclaredField("f_20898_");
            }
            f.setAccessible(true);
            lastHurtField = f;
        }
        return lastHurtField.getFloat(living);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putShort("xTile", (short) this.tileX);
        nbt.putShort("yTile", (short) this.tileY);
        nbt.putShort("zTile", (short) this.tileZ);
        nbt.putShort("life", (short) this.ticksInGround);
        nbt.putByte("shake", (byte) this.arrowShake);
        nbt.putByte("inGround", (byte) (this.inGround ? 1 : 0));
        nbt.putByte("pickup", (byte) this.canBePickedUp);
        nbt.putDouble("damage", this.damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.tileX = nbt.getShort("xTile");
        this.tileY = nbt.getShort("yTile");
        this.tileZ = nbt.getShort("zTile");
        this.ticksInGround = nbt.getShort("life");
        this.arrowShake = nbt.getByte("shake") & 255;
        this.inGround = nbt.getByte("inGround") == 1;

        if (nbt.contains("damage", 99)) {
            this.damage = nbt.getDouble("damage");
        }

        if (nbt.contains("pickup", 99)) {
            this.canBePickedUp = nbt.getByte("pickup");
        } else if (nbt.contains("player", 99)) {
            this.canBePickedUp = nbt.getBoolean("player") ? 1 : 0;
        }
    }

    /** Original {@code onCollideWithPlayer}: steckende Geschosse eines Spielers aufheben (ohne Gegenstand). */
    @Override
    public void playerTouch(Player player) {
        if (!level().isClientSide && this.inGround && this.arrowShake <= 0) {
            boolean flag = this.canBePickedUp == 1 || this.canBePickedUp == 2 && player.getAbilities().instabuild;

            if (flag) {
                player.take(this, 1);
                this.discard();
            }
        }
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return this.damage;
    }

    public void setKnockbackStrength(int strength) {
        this.knockbackStrength = strength;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    public void setIsCritical(boolean b) {
        byte b0 = this.entityData.get(CRITICAL);
        this.entityData.set(CRITICAL, (byte) (b ? (b0 | 1) : (b0 & -2)));
    }

    public void setTau(boolean b) {
        byte b0 = this.entityData.get(TAU);
        this.entityData.set(TAU, (byte) (b ? (b0 | 1) : (b0 & -2)));
    }

    public void setChopper(boolean b) {
        byte b0 = this.entityData.get(CHOPPER);
        this.entityData.set(CHOPPER, (byte) (b ? (b0 | 1) : (b0 & -2)));
    }

    public boolean getIsCritical() {
        return (this.entityData.get(CRITICAL) & 1) != 0;
    }

    public boolean getIsTau() {
        return (this.entityData.get(TAU) & 1) != 0;
    }

    public boolean getIsChopper() {
        return (this.entityData.get(CHOPPER) & 1) != 0;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 10.0D * 64.0D * getViewScale();
        return distance < d * d;
    }

    /** {@code getBrightnessForRender}: Tau- und Hubschraubergeschosse leuchten voll. */
    public boolean isFullbright() {
        return this.getIsCritical() || this.getIsChopper();
    }
}
