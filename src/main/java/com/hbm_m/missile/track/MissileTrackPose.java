package com.hbm_m.missile.track;



import net.minecraft.core.Direction;

import net.minecraft.resources.ResourceLocation;



/**

 * Authoritative missile pose from the server (network track snapshot).

 */

public record MissileTrackPose(

        double x,

        double y,

        double z,

        double vx,

        double vy,

        double vz,

        float yaw,

        float pitch,

        Direction launchFacing,

        ResourceLocation entityTypeId,

        ResourceLocation launchItemId,

        float contrailScale,

        long worldTick,

        long receiveNanos

) {

    public MissileTrackPose withReceiveNanos(long nanos) {

        return new MissileTrackPose(

                x, y, z, vx, vy, vz, yaw, pitch, launchFacing,

                entityTypeId, launchItemId, contrailScale, worldTick, nanos);

    }

}

