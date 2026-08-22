package com.hbm_m.entity.mob;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.entity.projectile.TurretBulletEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1:1 port of {@code EntityUFO} - a 15x4 block flying fortress with 20000 HP.
 *
 * <p>It picks the nearest non-creative, non-invisible player as its primary and keeps a list of
 * secondaries it can see; it then circles to a point 35 blocks past the primary rather than
 * hovering over it, so it is always strafing. Its armament runs on a 300-tick cycle: lasers for
 * the first 200 ticks, homing rockets for the last 100. Anything caught under it while the beam
 * is on takes 1000 damage, catches fire and is irradiated.</p>
 *
 * <p><b>Substitutions:</b> the projectiles are {@link TurretBulletEntity} rather than the
 * unported {@code EntityBulletBaseNT}, so the rockets fly straight instead of homing. The death
 * blast is a vanilla explosion where the original also calls {@code ExplosionNukeSmall}, which
 * this port does not have.</p>
 */
public class EntityUFO extends Mob implements Enemy, IRadiationImmune {

    private static final EntityDataAccessor<Boolean> BEAM =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> WAYPOINT =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.BLOCK_POS);

    /** How far past the target it aims when picking a new waypoint. */
    private static final double OVERSHOOT = 35D;

    public int courseChangeCooldown;
    public int scanCooldown;
    public int hurtCooldown;
    public int beamTimer;

    @Nullable private Entity primary;
    private final List<Entity> secondaries = new ArrayList<>();

    public EntityUFO(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.noCulling = true;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        // deathTime starts at -30 so the wreck tips over and falls for a while before exploding.
        this.deathTime = -30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20000.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.FLYING_SPEED, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEAM, false);
        this.entityData.define(WAYPOINT, BlockPos.ZERO);
    }

    public boolean getBeam()             { return this.entityData.get(BEAM); }
    public void setBeam(boolean beam)    { this.entityData.set(BEAM, beam); }
    public BlockPos getWaypoint()        { return this.entityData.get(WAYPOINT); }
    public void setWaypoint(BlockPos p)  { this.entityData.set(WAYPOINT, p); }

    @Override public boolean fireImmune()                { return true; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isNoGravity()               { return true; }
    @Override protected float getSoundVolume()           { return 10.0F; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource source) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound()       { return null; }
    @Override public boolean causeFallDamage(float d, float m, @NotNull DamageSource s) { return false; }

    /** A five-tick window after every hit, so a fast weapon cannot melt it. */
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.hurtCooldown > 0) return false;
        boolean hit = super.hurt(source, amount);
        if (hit) this.hurtCooldown = 5;
        return hit;
    }

    private boolean canAttack(Entity entity) {
        return !(entity instanceof EntityUFO) && !(entity instanceof TurretBulletEntity);
    }

    // ─── Behaviour ───────────────────────────────────────────────────────────

    @Override
    protected void customServerAiStep() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }

        if (this.hurtCooldown > 0) this.hurtCooldown--;
        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;

        if (this.primary != null && !this.primary.isAlive()) this.primary = null;

        if (this.scanCooldown <= 0) scanForTargets();
        if (this.primary != null && this.courseChangeCooldown <= 0) pickWaypoint();

        updateBeam();
        updateWeapons();
        updateFlight();
    }

    /** Nearest valid player wins; everything else it can see becomes a secondary. */
    private void scanForTargets() {
        List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().inflate(100, 50, 100));
        this.secondaries.clear();
        this.primary = null;

        for (Entity entity : entities) {
            if (!entity.isAlive() || !canAttack(entity)) continue;

            if (entity instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) continue;
                if (player.hasEffect(MobEffects.INVISIBILITY)) continue;

                if (this.primary == null
                        || this.distanceToSqr(entity) < this.distanceToSqr(this.primary)) {
                    this.primary = entity;
                }
            }

            if (entity instanceof LivingEntity && this.distanceToSqr(entity) < 100 * 100
                    && this.hasLineOfSight(entity) && entity != this.primary) {
                this.secondaries.add(entity);
            }
        }

        // With no player around it will still harass whatever else is nearby.
        if (this.primary == null && !this.secondaries.isEmpty()) {
            this.primary = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
        }

        this.scanCooldown = 50;
    }

    /**
     * Aims for a point {@link #OVERSHOOT} blocks beyond the target, usually on a random bearing,
     * so it sweeps past rather than parking overhead.
     */
    private void pickWaypoint() {
        if (this.primary == null) return;

        Vec3 away = new Vec3(this.getX() - this.primary.getX(), 0, this.getZ() - this.primary.getZ());
        if (this.random.nextInt(3) > 0) {
            away = away.yRot((float) (Math.PI * 2 * this.random.nextFloat()));
        }

        double length = away.length();
        if (length < 1.0E-4D) return;

        int wx = (int) Math.floor(this.primary.getX() - away.x / length * OVERSHOOT);
        int wz = (int) Math.floor(this.primary.getZ() - away.z / length * OVERSHOOT);
        int ground = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
        int wy = Math.max(ground + 20 + this.random.nextInt(15), (int) this.primary.getY() + 15);

        setWaypoint(new BlockPos(wx, wy, wz));
        this.courseChangeCooldown = 40 + this.random.nextInt(20);
    }

    /** The abduction beam: everything in the column beneath it is deleted. */
    private void updateBeam() {
        if (this.beamTimer <= 0 && getBeam()) setBeam(false);

        if (this.primary != null) {
            double flatDistance = Math.abs(this.primary.getX() - this.getX())
                    + Math.abs(this.primary.getZ() - this.getZ());
            if (flatDistance < 25) this.beamTimer = 30;
        }

        if (this.beamTimer <= 0) return;
        this.beamTimer--;

        if (!getBeam()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 10.0F, 1.0F);
            setBeam(true);
        }

        int groundY = groundBelow();
        if (groundY >= this.getY()) return;

        AABB column = new AABB(this.getX(), groundY, this.getZ(), this.getX(), this.getY(), this.getZ())
                .inflate(5, 0, 5);
        for (Entity entity : this.level().getEntities(this, column)) {
            if (!canAttack(entity)) continue;
            entity.hurt(this.damageSources().mobAttack(this), 1000F);
            entity.setSecondsOnFire(5);
            if (entity instanceof LivingEntity living) {
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            }
        }

        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), groundY + 0.5, this.getZ(), 20, 2.0D, 0.5D, 2.0D, 0.05D);
        }
    }

    private int groundBelow() {
        int ix = this.getBlockX();
        int iz = this.getBlockZ();
        for (int y = (int) Math.ceil(this.getY()); y >= this.level().getMinBuildHeight(); y--) {
            if (!this.level().getBlockState(new BlockPos(ix, y, iz)).isAir()) return y;
        }
        return this.level().getMinBuildHeight();
    }

    /** 300-tick cycle: lasers for 200 ticks, then rockets for 100. */
    private void updateWeapons() {
        int phase = this.tickCount % 300;

        if (phase < 200) {
            if (this.tickCount % 4 == 0) {
                fireAtSomeone(this::laserAttack);
            } else if (this.tickCount % 4 == 2 && this.primary != null) {
                laserAttack(this.primary);
            }
        } else {
            if (this.tickCount % 20 == 0) {
                fireAtSomeone(this::rocketAttack);
            } else if (this.tickCount % 20 == 10 && this.primary != null) {
                rocketAttack(this.primary);
            }
        }
    }

    private void fireAtSomeone(java.util.function.Consumer<Entity> attack) {
        if (!this.secondaries.isEmpty()) {
            Entity victim = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
            if (!victim.isAlive()) {
                this.secondaries.remove(victim);
            } else {
                attack.accept(victim);
            }
        } else if (this.primary != null) {
            attack.accept(this.primary);
        }
    }

    /**
     * The lasers do not come from the hull centre - the original picks a pivot ten blocks out on a
     * random bearing, so the shots rake in from the rim of the disc.
     */
    private void laserAttack(Entity target) {
        Vec3 away = new Vec3(this.getX() - target.getX(), 0, this.getZ() - target.getZ())
                .yRot((float) Math.toRadians(-80 + this.random.nextInt(160)))
                .normalize();

        double pivotX = this.getX() - away.x * 10;
        double pivotY = this.getY() + 0.5;
        double pivotZ = this.getZ() - away.z * 10;

        Vec3 heading = new Vec3(
                target.getX() - pivotX,
                target.getY() + target.getBbHeight() / 2 - pivotY,
                target.getZ() - pivotZ).normalize();

        TurretBulletEntity bullet = TurretBulletEntity.create(this.level(), pivotX, pivotY, pivotZ,
                heading.x * 2, heading.y * 2, heading.z * 2, 8F, ModItems.PARTICLE_DIGAMMA.get());
        bullet.setOwner(this);
        this.level().addFreshEntity(bullet);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 5.0F, 1.0F);
    }

    /** The original's rockets home; {@link TurretBulletEntity} has no homing, so these fly true. */
    private void rocketAttack(Entity target) {
        Vec3 heading = new Vec3(
                target.getX() - this.getX(),
                target.getY() + target.getBbHeight() / 2 - this.getY() - 0.5D,
                target.getZ() - this.getZ()).normalize();

        TurretBulletEntity rocket = TurretBulletEntity.create(this.level(),
                this.getX(), this.getY() - 0.5D, this.getZ(),
                heading.x * 2, heading.y * 2, heading.z * 2, 20F, ModItems.MISSILE_NUCLEAR.get());
        rocket.setOwner(this);
        this.level().addFreshEntity(rocket);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 5.0F, 1.0F);
    }

    /** Flies straight at the waypoint, faster when chasing a player, and stops if the way is blocked. */
    private void updateFlight() {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.courseChangeCooldown <= 0) return;

        BlockPos waypoint = getWaypoint();
        Vec3 delta = new Vec3(
                waypoint.getX() - this.getX(),
                waypoint.getY() - this.getY(),
                waypoint.getZ() - this.getZ());
        double length = delta.length();
        if (length <= 5) return;

        if (!isCourseTraversable(delta, length)) {
            this.courseChangeCooldown = 0;
            return;
        }

        double speed = this.primary instanceof Player ? 5D : 2D;
        this.setDeltaMovement(delta.scale(speed / length));
    }

    private boolean isCourseTraversable(Vec3 delta, double length) {
        Vec3 step = delta.scale(1 / length);
        AABB box = this.getBoundingBox();
        for (int i = 1; i < length; i++) {
            box = box.move(step);
            if (!this.level().noCollision(this, box)) return false;
        }
        return true;
    }

    // ─── Death ───────────────────────────────────────────────────────────────

    @Override
    protected void tickDeath() {
        if (getBeam()) setBeam(false);
        this.setDeltaMovement(this.getDeltaMovement().subtract(0, 0.05D, 0));

        if (this.deathTime == -10) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 10.0F, 0.5F);
        }

        if (this.deathTime == 19 && !this.level().isClientSide) {
            // The original follows this with ExplosionNukeSmall.PARAMS_MEDIUM, which is not ported.
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    10F, Level.ExplosionInteraction.MOB);

            ModAdvancements.grantNearby(this, 200D, ModAdvancements.BOSS_UFO);
            for (Player player : this.level().getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(200D))) {
                ItemStack coin = new ItemStack(ModItems.COIN_UFO.get());
                if (!player.getInventory().add(coin)) player.drop(coin, false);
            }
        }

        super.tickDeath();
    }
}
