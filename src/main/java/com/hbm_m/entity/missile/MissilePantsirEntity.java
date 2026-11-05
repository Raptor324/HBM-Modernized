package com.hbm_m.entity.missile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 57E6, der Flugkoerper des Pantsir-S1: 3,2 Bloecke lang, zweistufig, Reichweite 12 km.
 *
 * <p>Radarverhalten und Zielsuche stammen von {@link MissileABMEntity}. Eigen sind drei Dinge: die
 * Rakete startet in Rohrrichtung, beschleunigt auf {@link #TOP_SPEED} Bloecke pro Tick und prueft den
 * Abstand zum Ziel ueber den ganzen Tick. Eine reine Abstandsmessung am Tickende wuerde bei 140
 * Bloecken Annaeherung pro Tick fast immer danebenliegen.
 */
public class MissilePantsirEntity extends MissileABMEntity {

    /** Hoechstgeschwindigkeit in Bloecken pro Tick, also 1.200 m/s. */
    public static final double TOP_SPEED = 60.0D;

    private static final double ACCELERATION = 4.0D;
    private static final double START_SPEED = 8.0D;

    /** Wirkradius des Splittergefechtskopfs. */
    private static final double KILL_RADIUS = 14.0D;

    /** Anteil der gewuenschten Richtungsaenderung pro Tick. */
    private static final double TURN_RATE = 0.35D;

    /** Genug fuer 12 km Flug plus Reserve fuer ein ausweichendes Ziel. */
    private static final int LIFETIME = 320;
    private static final float BLAST = 8.0F;

    private double speed = START_SPEED;

    public MissilePantsirEntity(EntityType<? extends MissilePantsirEntity> type, Level level) {
        super(type, level);
        this.activationTimer = 40;
    }

    /** Setzt die Rakete an die Muendung und richtet sie aus. */
    public void launch(Vec3 muzzle, Vec3 direction, Entity target) {
        this.setPos(muzzle.x, muzzle.y, muzzle.z);
        this.startX = (int) muzzle.x;
        this.startZ = (int) muzzle.z;
        this.targetX = (int) target.getX();
        this.targetZ = (int) target.getZ();
        this.tracking = target;
        this.setDeltaMovement(direction.normalize().scale(BASE_SPEED));
        this.velocity = this.speed / BASE_SPEED;
        this.updateRotationFromMotion();
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    @Override
    protected void serverTickLogic() {
        this.speed = Math.min(TOP_SPEED, this.speed + ACCELERATION);
        this.velocity = this.speed / BASE_SPEED;

        if (this.tracking == null || this.tracking.isRemoved() || !this.tracking.isAlive()) {
            this.targetMissile();
        }
        Entity target = this.tracking;
        if (target == null || this.tickCount > LIFETIME) {
            detonate();
            return;
        }

        Vec3 ownStep = this.getDeltaMovement().scale(this.velocity);
        Vec3 targetStep = new Vec3(target.getX() - target.xOld, target.getY() - target.yOld, target.getZ() - target.zOld);
        Vec3 delta = target.position().subtract(this.position());

        if (closestApproach(delta, targetStep.subtract(ownStep)) < KILL_RADIUS) {
            target.hurt(this.damageSources().generic(), 10_000.0F);
            detonate();
            return;
        }

        double ticksToTarget = delta.length() / Math.max(1.0D, this.speed);
        Vec3 aim = delta.add(targetStep.scale(ticksToTarget));
        Vec3 current = this.getDeltaMovement().normalize();
        Vec3 wanted = aim.lengthSqr() > 1.0E-6D ? aim.normalize() : current;
        Vec3 heading = current.lerp(wanted, TURN_RATE);
        if (heading.lengthSqr() < 1.0E-6D) {
            heading = wanted;
        }
        this.setDeltaMovement(heading.normalize().scale(BASE_SPEED));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("InterceptorSpeed", this.speed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("InterceptorSpeed")) {
            this.speed = tag.getDouble("InterceptorSpeed");
        }
    }

    /** Kleinster Abstand im abgelaufenen Tick. {@code relativeStep} ist der Weg des Ziels relativ zur Rakete. */
    private static double closestApproach(Vec3 delta, Vec3 relativeStep) {
        double stepSq = relativeStep.lengthSqr();
        if (stepSq < 1.0E-9D) {
            return delta.length();
        }
        double t = Math.max(-1.0D, Math.min(0.0D, -delta.dot(relativeStep) / stepSq));
        return delta.add(relativeStep.scale(t)).length();
    }

    private void detonate() {
        if (this.level() instanceof ServerLevel server) {
            server.explode(this, this.getX(), this.getY(), this.getZ(), BLAST, Level.ExplosionInteraction.NONE);
            com.hbm_m.particle.explosions.basic.ExplosionParticleUtils.spawnAirBombSparks(
                    server, this.getX(), this.getY(), this.getZ());
        }
        releaseChunkTicket();
        this.discard();
    }

    @Override
    protected void onMissileImpact(BlockPos pos) {
        if (this.level() instanceof ServerLevel server) {
            server.explode(this, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, BLAST,
                    Level.ExplosionInteraction.NONE);
        }
    }
}
