package com.hbm_m.entity.projectile.rocketbehavior;

import com.hbm_m.entity.projectile.EntityArtilleryRocket;

/** 1:1 {@code IRocketSteeringBehavior}. */
public interface IRocketSteeringBehavior {

    /** Modifies the motion to steer towards the set target. */
    void adjustCourse(EntityArtilleryRocket rocket, double speed, double turnSpeed);
}
