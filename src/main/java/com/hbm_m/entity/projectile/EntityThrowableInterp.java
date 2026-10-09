package com.hbm_m.entity.projectile;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityThrowableInterp}: nur der Server simuliert, der Client interpoliert gleitend zur Serverposition
 * (statt selbst zu simulieren und dann zurueckzuspringen).
 */
public abstract class EntityThrowableInterp extends EntityThrowableNT {

    protected int turnProgress;
    protected double syncPosX;
    protected double syncPosY;
    protected double syncPosZ;
    protected double syncYaw;
    protected double syncPitch;
    protected double velocityX;
    protected double velocityY;
    protected double velocityZ;

    public EntityThrowableInterp(EntityType<? extends EntityThrowableInterp> type, Level world) {
        super(type, world);
    }

    public EntityThrowableInterp(EntityType<? extends EntityThrowableInterp> type, Level world, double x, double y, double z) {
        super(type, world, x, y, z);
    }

    @Override
    public void tick() {

        if (!level().isClientSide) {
            super.tick();
        } else {
            this.baseTick(); // setzt xo/yo/zo (lastTickPos)
            if (this.turnProgress > 0) {
                double interpX = this.getX() + (this.syncPosX - this.getX()) / (double) this.turnProgress;
                double interpY = this.getY() + (this.syncPosY - this.getY()) / (double) this.turnProgress;
                double interpZ = this.getZ() + (this.syncPosZ - this.getZ()) / (double) this.turnProgress;
                double d = Mth.wrapDegrees(this.syncYaw - (double) this.getYRot());
                this.setYRot((float) ((double) this.getYRot() + d / (double) this.turnProgress));
                this.setXRot((float) ((double) this.getXRot() + (this.syncPitch - (double) this.getXRot()) / (double) this.turnProgress));
                --this.turnProgress;
                this.setPos(interpX, interpY, interpZ);
            } else {
                this.setPos(this.getX(), this.getY(), this.getZ());
            }
        }
    }

    @Override
    public void lerpMotion(double velX, double velY, double velZ) {
        this.velocityX = velX;
        this.velocityY = velY;
        this.velocityZ = velZ;
        this.setDeltaMovement(velX, velY, velZ);
    }

    @Override
    //? if < 1.21.1 {
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int theNumberThree, boolean teleport) {
    //?} else {
    /*public void lerpTo(double x, double y, double z, float yaw, float pitch, int theNumberThree) {
    *///?}
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.syncYaw = yaw;
        this.syncPitch = pitch;
        this.turnProgress = theNumberThree + approachNum();
        this.setDeltaMovement(this.velocityX, this.velocityY, this.velocityZ);
    }

    /**
     * @return a number added to the basic "3" of the approach progress value. Larger numbers make the approach smoother, but lagging behind the true value more.
     */
    public int approachNum() {
        return 0;
    }
}
