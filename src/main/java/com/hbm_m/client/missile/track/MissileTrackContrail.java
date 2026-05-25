package com.hbm_m.client.missile.track;

import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.custom.MissileContrailParticle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Contrail segments for network-tracked missiles.
 * Must use {@code pForce=true} spawn — vanilla {@code addAlwaysVisibleParticle} still caps at 32 blocks from camera.
 */
final class MissileTrackContrail {

    private static final double PARTICLE_SPACING_BLOCKS = 0.5D;

    private MissileTrackContrail() {}

    static void spawn(ClientLevel level,
                      double fromX, double fromY, double fromZ,
                      double toX, double toY, double toZ,
                      float yaw, float pitch, float scale) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 1.0E-6D) {
            return;
        }

        Vec3 thrust = new Vec3(0.0D, 1.0D, 0.0D);
        thrust = thrust.xRot(-pitch * ((float) Math.PI / 180.0F));
        thrust = thrust.yRot(-(yaw + 90.0F) * ((float) Math.PI / 180.0F));

        int particleCount = Math.max(1, (int) Math.ceil(distance / PARTICLE_SPACING_BLOCKS));

        MissileContrailParticle.currentSpawnScale = scale;
        try {
            for (int i = 0; i <= particleCount; i++) {
                double t = (double) i / particleCount;
                double px = Mth.lerp(t, fromX, toX);
                double py = Mth.lerp(t, fromY, toY);
                double pz = Mth.lerp(t, fromZ, toZ);
                level.addParticle(
                        ModParticleTypes.MISSILE_CONTRAIL.get(),
                        true,
                        px, py, pz,
                        -thrust.x * 0.1D,
                        -thrust.y * 0.1D,
                        -thrust.z * 0.1D);
            }
        } finally {
            MissileContrailParticle.currentSpawnScale = 1.0F;
        }
    }
}
