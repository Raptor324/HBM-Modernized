package com.hbm_m.entity.mob;

/** 1:1 {@code IFlyingCreature}: Wesen mit Lauf-/Flugzustand. */
public interface IFlyingCreature {

    int STATE_WALKING = 0;
    int STATE_FLYING = 1;

    int getFlyingState();
    void setFlyingState(int state);
}
