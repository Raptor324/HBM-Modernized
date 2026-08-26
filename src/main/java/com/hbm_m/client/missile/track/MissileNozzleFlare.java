package com.hbm_m.client.missile.track;

import com.hbm_m.particle.nt.MissileNozzleFlareNT;
import com.hbm_m.particle.nt.ParticleEngineNT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Nozzle engine glare (flash + flare) — locked to the exhaust, not the contrail trail.
 * Слои спавнятся в NT-движок (ParticleEngineNT) — рендер не клипается far plane'ом.
 */
public final class MissileNozzleFlare {

    private static final double NOZZLE_BACK_BLOCKS = 0.3D;
    /** Quad size multiplier on top of missile contrail scale. */
    private static final float FLARE_SIZE_MUL = 7.5F;

    private MissileNozzleFlare() {}

    public static void spawn(ClientLevel level,
                             double anchorX, double anchorY, double anchorZ,
                             float pitch, float yaw,
                             Vec3 flightStep, float scale) {
        if (MissileNozzleFlareNT.sprites == null) {
            return; // Провайдеры ещё не зарегистрированы (ранний кадр)
        }
        Vec3 nozzle;
        Vec3 carryVel;
        if (flightStep.lengthSqr() > 1.0E-8D) {
            Vec3 flight = flightStep.normalize();
            double back = NOZZLE_BACK_BLOCKS * scale;
            nozzle = new Vec3(anchorX, anchorY, anchorZ).subtract(flight.scale(back));
            carryVel = flightStep;
        } else {
            Vec3 thrust = thrustFromRotation(pitch, yaw);
            double back = NOZZLE_BACK_BLOCKS * scale;
            nozzle = new Vec3(anchorX, anchorY, anchorZ).add(thrust.scale(back));
            carryVel = Vec3.ZERO;
        }

        spawnLayer(level, nozzle, carryVel, scale, 0);
        spawnLayer(level, nozzle, carryVel, scale, 1);
    }

    private static void spawnLayer(ClientLevel level, Vec3 pos, Vec3 carryVel, float scale, int layer) {
        ParticleEngineNT.INSTANCE.add(new MissileNozzleFlareNT(
                level, pos.x, pos.y, pos.z,
                carryVel.x, carryVel.y, carryVel.z,
                scale * FLARE_SIZE_MUL, layer));
    }

    private static Vec3 thrustFromRotation(float pitch, float yaw) {
        Vec3 thrust = new Vec3(0.0D, 1.0D, 0.0D);
        thrust = thrust.xRot(-pitch * ((float) Math.PI / 180.0F));
        thrust = thrust.yRot(-(yaw + 90.0F) * ((float) Math.PI / 180.0F));
        return thrust;
    }
}
