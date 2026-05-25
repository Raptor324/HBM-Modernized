package com.hbm_m.client.missile.track;

import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.custom.MissileContrailParticle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Contrail segments for network-tracked missiles.
 * Must use {@code pForce=true} spawn — vanilla {@code addAlwaysVisibleParticle} still caps at 32 blocks from camera.
 * Gray vapor is spawned later by {@link MissileContrailParticle} when each hot particle expires.
 */
public final class MissileTrackContrail {

    private MissileTrackContrail() {}

    public static void spawn(ClientLevel level,
                      double fromX, double fromY, double fromZ,
                      double toX, double toY, double toZ,
                      float scale) {
        Vec3 motion = new Vec3(toX - fromX, toY - fromY, toZ - fromZ);
        double len = motion.length();
        if (len <= 1.0E-6D) {
            return;
        }
        Vec3 motionNorm = motion.normalize();
        Vec3 exhaust = motionNorm.scale(-1.0D);
        spawnSegments(level, toX, toY, toZ, motionNorm, len, exhaust, scale, 0.0D, 0.0D, 0.0D);
    }

    public static void spawnSegments(ClientLevel level,
                              double anchorX, double anchorY, double anchorZ,
                              Vec3 motionNorm, double len,
                              Vec3 exhaustVelocity, float scale,
                              double offsetX, double offsetY, double offsetZ) {
        int segmentCount = Math.max(1, Math.min((int) len, 10));

        MissileContrailParticle.currentSpawnScale = scale;
        try {
            for (int i = 0; i < segmentCount; i++) {
                double j = i - len;
                double px = anchorX - motionNorm.x * j + offsetX;
                double py = anchorY - motionNorm.y * j + offsetY;
                double pz = anchorZ - motionNorm.z * j + offsetZ;

                level.addParticle(
                        ModParticleTypes.MISSILE_CONTRAIL.get(),
                        true,
                        px, py, pz,
                        exhaustVelocity.x,
                        exhaustVelocity.y,
                        exhaustVelocity.z);
            }
        } finally {
            MissileContrailParticle.currentSpawnScale = 1.0F;
        }
    }
}
