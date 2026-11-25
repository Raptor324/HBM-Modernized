package com.hbm_m.entity.effect;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Joke UFO spawned by {@link com.hbm_m.explosion.FreakyAlienExplosion}:
 * flies in from the horizon, hovers over the detonation site firing a laser beam
 * into the ground, then accelerates away into the sky.
 */
public class EntityFreakyUfo extends Entity {

    public static final int ARRIVE_TICKS = 50;   // 2.5 s approach
    public static final int BEAM_END_TICK = 160; // 5.5 s laser beam
    public static final int LIFETIME = 240;      // 4.0 s departure (12 s total)

    private static final EntityDataAccessor<Boolean> BEAM =
            SynchedEntityData.defineId(EntityFreakyUfo.class, EntityDataSerializers.BOOLEAN);

    private double targetX;
    private double targetY;
    private double targetZ;
    private double hoverY;
    private double startX;
    private double startY;
    private double startZ;
    private double escapeDirX = 1.0;
    private double escapeDirZ = 0.0;

    public EntityFreakyUfo(EntityType<? extends EntityFreakyUfo> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntityFreakyUfo(Level level, double x, double y, double z) {
        this(com.hbm_m.entity.ModEntities.FREAKY_UFO.get(), level);
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.hoverY = Math.min(Math.max(y + 38.0, 95.0), level.getMaxBuildHeight() - 12.0);

        double angle = level.random.nextDouble() * Math.PI * 2.0;
        this.escapeDirX = Math.cos(angle);
        this.escapeDirZ = Math.sin(angle);

        this.startX = x - this.escapeDirX * 75.0;
        this.startY = Math.min(this.hoverY + 25.0, level.getMaxBuildHeight() - 4.0);
        this.startZ = z - this.escapeDirZ * 75.0;
        setPos(this.startX, this.startY, this.startZ);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        var defs = com.hbm_m.platform.EntityDataHooks.sink(this.entityData);
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        var defs = com.hbm_m.platform.EntityDataHooks.sink(builder);
    *///?}
        defs.define(BEAM, false);
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM);
    }

    public void setBeam(boolean beam) {
        this.entityData.set(BEAM, beam);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            if (getBeam()) {
                spawnImpactParticles();
            }
            return;
        }

        if (this.tickCount <= ARRIVE_TICKS) {
            // Smooth ease-out approach to hover point
            double t = (double) this.tickCount / ARRIVE_TICKS;
            double ease = 1.0 - Math.pow(1.0 - t, 3.0);
            double nx = Mth.lerp(ease, this.startX, this.targetX);
            double ny = Mth.lerp(ease, this.startY, this.hoverY);
            double nz = Mth.lerp(ease, this.startZ, this.targetZ);
            setPos(nx, ny, nz);
            setDeltaMovement(Vec3.ZERO);
        } else if (this.tickCount <= BEAM_END_TICK) {
            // Hover & fire laser beam
            if (!getBeam()) {
                setBeam(true);
                SoundEvent blast = HbmSoundsNT.get("hbm:entity.ufoBlast");
                if (blast != null) {
                    level().playSound(null, this.targetX, this.targetY, this.targetZ, blast, SoundSource.HOSTILE, 6.0F, 1.0F);
                }
            }
            if (this.tickCount % 25 == 0) {
                SoundEvent beamSound = HbmSoundsNT.get("hbm:entity.ufoBeamShort");
                if (beamSound != null) {
                    level().playSound(null, this.targetX, this.targetY, this.targetZ, beamSound, SoundSource.HOSTILE, 4.0F, 1.0F);
                }
            }
            double bob = Math.sin((this.tickCount - ARRIVE_TICKS) * 0.15) * 0.35;
            setPos(this.targetX, this.hoverY + bob, this.targetZ);
            setDeltaMovement(Vec3.ZERO);
        } else {
            // Turn off beam & fly away
            if (getBeam()) {
                setBeam(false);
            }
            double dt = this.tickCount - BEAM_END_TICK;
            double speed = 0.25 + dt * 0.04;
            setPos(getX() + this.escapeDirX * speed, getY() + 0.35 + dt * 0.015, getZ() + this.escapeDirZ * speed);
        }

        if (this.tickCount >= LIFETIME) {
            discard();
        }
    }

    private void spawnImpactParticles() {
        int ix = Mth.floor(getX());
        int iz = Mth.floor(getZ());
        int iy = level().getMinBuildHeight();
        for (int i = Mth.ceil(getY()); i >= level().getMinBuildHeight(); i--) {
            if (!level().getBlockState(new BlockPos(ix, i, iz)).isAir()) {
                iy = i + 1;
                break;
            }
        }
        for (int i = 0; i < 4; i++) {
            double ox = (random.nextDouble() - 0.5) * 2.5;
            double oz = (random.nextDouble() - 0.5) * 2.5;
            level().addParticle(ParticleTypes.FLAME, getX() + ox, iy + 0.2, getZ() + oz,
                    (random.nextDouble() - 0.5) * 0.2, 0.15 + random.nextDouble() * 0.25, (random.nextDouble() - 0.5) * 0.2);
        }
        if (this.tickCount % 4 == 0) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), iy + 0.3, getZ(), 0, 0.1, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.targetX = tag.getDouble("TargetX");
        this.targetY = tag.getDouble("TargetY");
        this.targetZ = tag.getDouble("TargetZ");
        this.hoverY = tag.getDouble("HoverY");
        this.startX = tag.getDouble("StartX");
        this.startY = tag.getDouble("StartY");
        this.startZ = tag.getDouble("StartZ");
        this.escapeDirX = tag.getDouble("EscapeX");
        this.escapeDirZ = tag.getDouble("EscapeZ");
        setBeam(tag.getBoolean("Beam"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("TargetX", this.targetX);
        tag.putDouble("TargetY", this.targetY);
        tag.putDouble("TargetZ", this.targetZ);
        tag.putDouble("HoverY", this.hoverY);
        tag.putDouble("StartX", this.startX);
        tag.putDouble("StartY", this.startY);
        tag.putDouble("StartZ", this.startZ);
        tag.putDouble("EscapeX", this.escapeDirX);
        tag.putDouble("EscapeZ", this.escapeDirZ);
        tag.putBoolean("Beam", getBeam());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
