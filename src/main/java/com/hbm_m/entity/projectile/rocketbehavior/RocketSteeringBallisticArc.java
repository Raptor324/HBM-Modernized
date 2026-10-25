package com.hbm_m.entity.projectile.rocketbehavior;

import com.hbm_m.entity.projectile.EntityArtilleryRocket;
import com.hbm_m.util.Vec3NT;

import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RocketSteeringBallisticArc}. */
public class RocketSteeringBallisticArc implements IRocketSteeringBehavior {

    @Override
    public void adjustCourse(EntityArtilleryRocket rocket, double speed, double maxTurn) {

        double turnSpeed = 45;

        Vec3 m = rocket.getDeltaMovement();
        Vec3NT direction = Vec3NT.createVectorHelper(m.x, m.y, m.z).normalize();
        double horizontalMomentum = Math.sqrt(m.x * m.x + m.z * m.z);
        Vec3 targetPos = rocket.getLastTarget();
        double deltaX = targetPos.x - rocket.getX();
        double deltaZ = targetPos.z - rocket.getZ();
        double horizontalDelta = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double stepsRequired = horizontalDelta / horizontalMomentum;
        Vec3NT target = Vec3NT.createVectorHelper(targetPos.x - rocket.getX(), targetPos.y - rocket.getY(), targetPos.z - rocket.getZ()).normalize();

        /* the entity's angles lack precision and i lack the nerve to figure out how they're oriented */
        double rocketYaw = yaw(direction);
        double rocketPitch = pitch(direction);
        double targetYaw = yaw(target);
        double targetPitch = pitch(target);

        turnSpeed = Math.min(maxTurn, turnSpeed / stepsRequired);

        /* ...and then we just cheat */
        if (stepsRequired <= 1) {
            turnSpeed = 180D;
        }

        /* shortest delta of α < 180° */
        double deltaYaw = ((targetYaw - rocketYaw) + 180D) % 360D - 180D;
        double deltaPitch = ((targetPitch - rocketPitch) + 180D) % 360D - 180D;

        double turnYaw = Math.min(Math.abs(deltaYaw), turnSpeed) * Math.signum(deltaYaw);
        double turnPitch = Math.min(Math.abs(deltaPitch), turnSpeed) * Math.signum(deltaPitch);

        Vec3NT velocity = Vec3NT.createVectorHelper(speed, 0, 0);
        velocity.rotateAroundZ((float) -Math.toRadians(rocketPitch + turnPitch));
        velocity.rotateAroundY((float) Math.toRadians(rocketYaw + turnYaw + 90));

        rocket.setDeltaMovement(velocity.xCoord, velocity.yCoord, velocity.zCoord);
    }

    private static double yaw(Vec3NT vec) {
        boolean pos = vec.zCoord >= 0;
        return Math.toDegrees(Math.atan(vec.xCoord / vec.zCoord)) + (pos ? 180 : 0);
    }

    private static double pitch(Vec3NT vec) {
        return Math.toDegrees(Math.atan(vec.yCoord / Math.sqrt(vec.xCoord * vec.xCoord + vec.zCoord * vec.zCoord)));
    }
}
