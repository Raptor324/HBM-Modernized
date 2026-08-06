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
 * SCOPE-Vereinfachung: Das Original nutzt clientseitiges {@code turnProgress}-Lerp-Smoothing fuer
 * die Netzwerk-Positions-Interpolation. Hier: direkte Positionsuebernahme wie bei
 * {@link com.hbm_m.entity.conveyor.MovingConveyorItemEntity} - bei den hier verwendeten
 * Geschwindigkeiten (max. 1.125 Bloecke/Tick) optisch kaum wahrnehmbar, spart die Lerp-Infrastruktur.
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

    @Override
    protected void defineSynchedData() {
        this.entityData.define(HAS_TARGET, false);
        this.entityData.define(APPEARANCE, APPEARANCE_EMPTY);
    }

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

    @Override
    public void tick() {
        super.tick();

        this.setDeltaMovement(Vec3.ZERO);

        if (hasTarget()) {
            double dx = targetX - getX();
            double dy = targetY - getY();
            double dz = targetZ - getZ();
            Vec3 toTarget = new Vec3(dx, dy, dz);
            double dist = toTarget.length();

            if (dist < 0.05) {
                clearTarget();
                onTargetReached();
            } else {
                double speed = Math.min(getSpeed(), dist);
                Vec3 motion = toTarget.scale(speed / dist);
                this.setDeltaMovement(motion);
            }
        }

        move(MoverType.SELF, getDeltaMovement());

        if (horizontalCollision) {
            // Original's crude escape hatch: nudge upward to try to clear the obstruction.
            this.setDeltaMovement(getDeltaMovement().add(0, 1, 0));
        }

        loadNeighboringChunks();

        if (level().isClientSide) {
            spawnTrailParticles();
        }
    }

    /** Called once when the drone reaches its target (dist < 0.05) and clears it. Hook for subclasses. */
    protected void onTargetReached() {
    }

    /** Hook for chunk-loading drones (see {@link com.hbm_m.entity.drone.EntityDeliveryDrone}). No-op by default. */
    protected void loadNeighboringChunks() {
    }

    private void spawnTrailParticles() {
        for (int i = 0; i < 4; i++) {
            double ox = (random.nextDouble() - 0.5) * getBbWidth();
            double oz = (random.nextDouble() - 0.5) * getBbWidth();
            level().addParticle(ParticleTypes.CLOUD, getX() + ox, getY() - 0.1, getZ() + oz, 0, 0, 0);
        }
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
