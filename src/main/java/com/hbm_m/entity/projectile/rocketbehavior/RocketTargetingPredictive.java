package com.hbm_m.entity.projectile.rocketbehavior;

import com.hbm_m.entity.projectile.EntityArtilleryRocket;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RocketTargetingPredictive}: Puffer der Zielbewegung der letzten Sekunde, daraus ein vorhergesagtes Ziel.
 * Das Verschieben des Puffers kopiert wie im Original nur die Zeilen-Referenzen.
 */
public class RocketTargetingPredictive implements IRocketTargetingBehavior {

    private double[][] targetMotion = new double[20][3];

    @Override
    public void recalculateTargetPosition(EntityArtilleryRocket rocket, Entity target) {

        Vec3 rm = rocket.getDeltaMovement();
        Vec3 tm = target.getDeltaMovement();
        double speed = rm.length();
        double eta = new Vec3(target.getX() - rocket.getX(), target.getY() - rocket.getY(), target.getZ() - rocket.getZ()).length() - speed;

        /* initialize with the values we already know */
        double motionX = tm.x;
        double motionY = tm.y;
        double motionZ = tm.z;

        /* shift the buffer and add the older values */
        for (int i = 1; i < 20; i++) {
            targetMotion[i - 1] = targetMotion[i];
            motionX += targetMotion[i][0];
            motionY += targetMotion[i][1];
            motionZ += targetMotion[i][2];
        }

        /* push the new values to the buffer for future use */
        targetMotion[19][0] = tm.x;
        targetMotion[19][1] = tm.y;
        targetMotion[19][2] = tm.z;

        if (eta <= 1) {
            rocket.setTarget(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            return;
        }

        /* generate averages and predict a new position */
        double predX = target.getX() + (motionX / 20D) * eta;
        double predY = target.getY() + target.getBbHeight() * 0.5D + (motionY / 20D) * eta;
        double predZ = target.getZ() + (motionZ / 20D) * eta;

        rocket.setTarget(predX, predY, predZ);
    }
}
