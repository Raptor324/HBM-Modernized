package com.hbm_m.entity.projectile.rocketbehavior;

import com.hbm_m.entity.projectile.EntityArtilleryRocket;

import net.minecraft.world.entity.Entity;

/**
 * 1:1 {@code RocketTargetingSimple}: "Stupid targeting for rockets, they move straight towards the target's current position"
 * (Original {@code posY - yOffset} ist hier die Fusshoehe {@code getY()}).
 */
public class RocketTargetingSimple implements IRocketTargetingBehavior {

    @Override
    public void recalculateTargetPosition(EntityArtilleryRocket rocket, Entity target) {
        rocket.setTarget(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }
}
