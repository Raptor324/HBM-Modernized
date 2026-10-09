package com.hbm_m.entity.drone;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port von {@code com.hbm.entity.item.EntityDroneBase} (1.7.10 Original). Fliegende Basis-Entity
 * mit trivialer Ziel-Verfolgung: keine echte Pfadfindungs-KI, nur konstante Geschwindigkeit direkt
 * auf {@link #targetPos} zu, mit einem Ausweich-Hack (nach oben ausweichen) bei horizontaler
 * Kollision - 1:1 aus dem Original uebernommen (siehe Klassenkommentar dort: "no real AI").
 * <p>
 * Wie im Original bewegt sich die Drohne nur auf dem Server; der Client interpoliert die Netzwerkposition ueber
 * {@code turnProgress} (Original {@code setPositionAndRotation2}) und laesst an den vier Rotoren Rauch aufsteigen.
 */
public abstract class EntityDroneBase extends Entity {

    private static final EntityDataAccessor<Boolean> HAS_TARGET =
            SynchedEntityData.defineId(EntityDroneBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> APPEARANCE =
            SynchedEntityData.defineId(EntityDroneBase.class, EntityDataSerializers.INT);

    public static final int APPEARANCE_EMPTY = 0;
    public static final int APPEARANCE_CRATE = 1;
    public static final int APPEARANCE_BARREL = 2;

    private double targetX, targetY, targetZ;

    protected EntityDroneBase(EntityType<? extends EntityDroneBase> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(HAS_TARGET, false);
        this.entityData.define(APPEARANCE, APPEARANCE_EMPTY);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(HAS_TARGET, false);
        builder.define(APPEARANCE, APPEARANCE_EMPTY);
    }
    *///?}

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.entityData.set(HAS_TARGET, true);
    }

    public void clearTarget() {
        this.entityData.set(HAS_TARGET, false);
    }

    public boolean hasTarget() {
        return this.entityData.get(HAS_TARGET);
    }

    public boolean isIdle() {
        return !hasTarget() || getDeltaMovement().lengthSqr() < 1.0E-6;
    }

    public int getAppearance() {
        return this.entityData.get(APPEARANCE);
    }

    public void setAppearance(int appearance) {
        this.entityData.set(APPEARANCE, appearance);
    }

    /** Blocks/tick. Overridden per subtype/variant (patrol vs. express vs. request). */
    public double getSpeed() {
        return 0.125D;
    }

    protected int turnProgress;
    protected double syncPosX;
    protected double syncPosY;
    protected double syncPosZ;

    /** Original {@code setPositionAndRotation2}: Zielposition merken, ueber {@code steps} Ticks hinterherziehen. */
    @Override
    //? if < 1.21.1 {
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
    //?} else {
    /*public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    *///?}
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.turnProgress = steps;
    }

    @Override
    public void tick() {

        if (level().isClientSide) {
            if (this.turnProgress > 0) {
                double interpX = getX() + (this.syncPosX - getX()) / (double) this.turnProgress;
                double interpY = getY() + (this.syncPosY - getY()) / (double) this.turnProgress;
                double interpZ = getZ() + (this.syncPosZ - getZ()) / (double) this.turnProgress;
                --this.turnProgress;
                this.setPos(interpX, interpY, interpZ);
            } else {
                this.setPos(getX(), getY(), getZ());
            }

            level().addParticle(ParticleTypes.SMOKE, getX() + 1.125, getY() + 0.75, getZ(), 0, -0.2, 0);
            level().addParticle(ParticleTypes.SMOKE, getX() - 1.125, getY() + 0.75, getZ(), 0, -0.2, 0);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.75, getZ() + 1.125, 0, -0.2, 0);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.75, getZ() - 1.125, 0, -0.2, 0);
        } else {

            boolean collided = this.horizontalCollision;
            Vec3 motion = Vec3.ZERO;

            if (hasTarget()) {
                Vec3 dist = new Vec3(targetX - getX(), targetY - getY(), targetZ - getZ());
                double len = dist.length();
                double speed = Math.min(getSpeed(), len);

                if (len < 0.05) {
                    clearTarget();
                    onTargetReached();
                } else {
                    motion = dist.normalize().scale(speed);
                }
            }
            if (collided) {
                motion = motion.add(0, 1, 0);
            }
            this.setDeltaMovement(motion);
            this.loadNeighboringChunks();
            move(MoverType.SELF, getDeltaMovement());
        }

        super.tick();
    }

    /** Called once when the drone reaches its target (dist < 0.05) and clears it. Hook for subclasses. */
    protected void onTargetReached() {
    }

    /** Hook for chunk-loading drones (see {@link com.hbm_m.entity.drone.EntityDeliveryDrone}). No-op by default. */
    protected void loadNeighboringChunks() {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.getBoolean("hasTarget")) {
            setTarget(tag.getDouble("targetX"), tag.getDouble("targetY"), tag.getDouble("targetZ"));
        }
        setAppearance(tag.getInt("appearance"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("hasTarget", hasTarget());
        tag.putDouble("targetX", targetX);
        tag.putDouble("targetY", targetY);
        tag.putDouble("targetZ", targetZ);
        tag.putInt("appearance", getAppearance());
    }
}
