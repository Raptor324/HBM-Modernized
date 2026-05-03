package com.hbm_m.particle.explosions;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;

/**
 * Spawns always-visible explosion particles from the dedicated server without
 * touching client-only classes ({@code Minecraft}, {@code ClientLevel}).
 */
public final class ServerExplosionParticles {

    private ServerExplosionParticles() {}

    public static void sendAlwaysVisible(
            ServerLevel level,
            SimpleParticleType type,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz) {
        level.sendParticles(type, x, y, z, 1, vx, vy, vz, 0.0);
    }
}
