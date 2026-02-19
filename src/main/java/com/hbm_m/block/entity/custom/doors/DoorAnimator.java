package com.hbm_m.block.entity.custom.doors;

/**
 * Server-safe abstraction for door transform animation.
 * Implemented on the client by renderer-specific animators.
 */
public interface DoorAnimator {

    void translate(double x, double y, double z);

    void rotate(float degrees, float x, float y, float z);
}
