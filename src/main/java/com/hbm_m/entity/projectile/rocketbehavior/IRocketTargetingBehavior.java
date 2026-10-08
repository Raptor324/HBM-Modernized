package com.hbm_m.entity.projectile.rocketbehavior;

import com.hbm_m.entity.projectile.EntityArtilleryRocket;

import net.minecraft.world.entity.Entity;

/** 1:1 {@code IRocketTargetingBehavior}. */
public interface IRocketTargetingBehavior {

    /** Recalculates the position that should be steered towards. */
    void recalculateTargetPosition(EntityArtilleryRocket rocket, Entity target);
}
