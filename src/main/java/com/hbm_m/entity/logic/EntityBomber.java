package com.hbm_m.entity.logic;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.EntityBombletZeta;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code EntityBomber} with {@code EntityPlaneBase} folded in - the base is only
 * health, a lifetime and chunk loading, and the original marks it as an afterthought anyway.
 *
 * <p>The plane spawns 100 blocks out from the target at Y+50, flies straight through, and drops
 * its payload between {@code bombStart} and {@code bombStop} at one bomb every {@code bombRate}
 * ticks. It can be shot down: 50 health, and it stops bombing the moment it is dead.</p>
 *
 * <p>Only the three bomblet-carrying loadouts are ported. The original also has boxcar rockets and
 * two poison-cloud variants that need {@code EntityBoxcar} and {@code ExplosionChaos}.</p>
 */
public class EntityBomber extends Entity {

    /** Synced so the renderer knows which silhouette to draw. */
    private static final EntityDataAccessor<Integer> PLANE_TYPE =
            SynchedEntityData.defineId(EntityBomber.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(EntityBomber.class, EntityDataSerializers.FLOAT);

    private static final float MAX_HEALTH = 50F;

    private int bombStart = 75;
    private int bombStop = 125;
    private int bombRate = 3;
    private int payload = EntityBombletZeta.TYPE_CARPET;
    private int timer = 200;

    public EntityBomber(EntityType<? extends EntityBomber> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(PLANE_TYPE, 1);
        this.entityData.define(HEALTH, MAX_HEALTH);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(PLANE_TYPE, 1);
        builder.define(HEALTH, MAX_HEALTH);
    }
    *///?}

    public int getPlaneType()  { return this.entityData.get(PLANE_TYPE); }
    public float getHealth()   { return this.entityData.get(HEALTH); }

    @Override public boolean isPickable() { return getHealth() > 0; }
    @Override public boolean isNoGravity() { return true; }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (this.isRemoved() || this.level().isClientSide || getHealth() <= 0) return true;

        this.entityData.set(HEALTH, getHealth() - amount);
        if (getHealth() <= 0) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    3.5F, Level.ExplosionInteraction.NONE);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 25.0F, 1.0F);
        }
        return true;
    }

    @Override
    public void tick() {
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (this.level().isClientSide) return;

        // A downed bomber falls out of the sky rather than vanishing.
        if (getHealth() <= 0) {
            this.setDeltaMovement(motion.x * 0.98D, motion.y - 0.1D, motion.z * 0.98D);
            if (this.onGround() || --this.timer <= 0) this.discard();
            return;
        }

        if (--this.timer <= 0) {
            this.discard();
            return;
        }

        if (this.tickCount > this.bombStart && this.tickCount < this.bombStop
                && this.tickCount % this.bombRate == 0) {
            dropBomb();
        }
    }

    private void dropBomb() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                com.hbm_m.sound.ModSounds.BOMBWHISTLE.get(), SoundSource.HOSTILE,
                10.0F, 0.9F + this.random.nextFloat() * 0.2F);

        Vec3 motion = this.getDeltaMovement();
        EntityBombletZeta zeta = EntityBombletZeta.create(this.level(),
                this.getX() + this.random.nextDouble() - 0.5,
                this.getY() - this.random.nextDouble(),
                this.getZ() + this.random.nextDouble() - 0.5,
                this.payload);

        // Carpet bombing scatters; everything else drops in a tight line.
        if (this.payload == EntityBombletZeta.TYPE_CARPET) {
            zeta.setDeltaMovement(
                    motion.x + this.random.nextGaussian() * 0.15, 0,
                    motion.z + this.random.nextGaussian() * 0.15);
        } else {
            zeta.setDeltaMovement(motion.x, 0, motion.z);
        }

        zeta.updateRotation();
        this.level().addFreshEntity(zeta);
    }

    /** {@code fac}: places the plane on an inbound course towards the given point. */
    private void setCourse(Level level, double x, double y, double z) {
        Vec3 heading = new Vec3(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5)
                .normalize().scale(2);

        this.setPos(x - heading.x * 100, y + 50, z - heading.z * 100);
        this.setDeltaMovement(heading.x, 0, heading.z);
        this.setYRot((float) (Math.atan2(heading.x, heading.z) * 180.0D / Math.PI));
        this.yRotO = this.getYRot();
    }

    // ─── Loadouts (the original's statFac* factories) ────────────────────────

    private static EntityBomber make(Level level, double x, double y, double z,
                                     int start, int stop, int rate, int payload, int planeType) {
        EntityBomber bomber = new EntityBomber(ModEntities.BOMBER.get(), level);
        bomber.timer = 200;
        bomber.bombStart = start;
        bomber.bombStop = stop;
        bomber.bombRate = rate;
        bomber.payload = payload;
        bomber.setCourse(level, x, y, z);
        bomber.entityData.set(PLANE_TYPE, planeType);
        return bomber;
    }

    public static EntityBomber carpet(Level level, double x, double y, double z) {
        return make(level, x, y, z, 50, 100, 2, EntityBombletZeta.TYPE_CARPET, 1);
    }

    public static EntityBomber napalm(Level level, double x, double y, double z) {
        return make(level, x, y, z, 50, 100, 5, EntityBombletZeta.TYPE_NAPALM, 2);
    }

    public static EntityBomber chlorine(Level level, double x, double y, double z) {
        return make(level, x, y, z, 50, 100, 4, EntityBombletZeta.TYPE_CHLORINE, 5);
    }

    /** A single nuke, dropped in a ten-tick window in the middle of the run. */
    public static EntityBomber aBomb(Level level, double x, double y, double z) {
        return make(level, x, y, z, 60, 70, 65, EntityBombletZeta.TYPE_NUKE, 8);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.bombStart = tag.getInt("bombStart");
        this.bombStop = tag.getInt("bombStop");
        this.bombRate = Math.max(1, tag.getInt("bombRate"));
        this.payload = tag.getInt("payload");
        this.timer = tag.getInt("timer");
        this.entityData.set(PLANE_TYPE, tag.getInt("planeType"));
        this.entityData.set(HEALTH, tag.getFloat("health"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("bombStart", this.bombStart);
        tag.putInt("bombStop", this.bombStop);
        tag.putInt("bombRate", this.bombRate);
        tag.putInt("payload", this.payload);
        tag.putInt("timer", this.timer);
        tag.putInt("planeType", getPlaneType());
        tag.putFloat("health", getHealth());
    }
}
