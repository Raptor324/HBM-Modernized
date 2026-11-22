package com.hbm_m.client.missile.track;

import com.hbm_m.particle.nt.MissileContrailNT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Contrail segments for network-tracked missiles.
 * Сегменты спавнятся в NT-движок (ParticleEngineNT) — рендер идёт в нашем
 * пайплайне AFTER_WEATHER и не клипается far plane'ом при активном DH.
 */
public final class MissileTrackContrail {

    private MissileTrackContrail() {}

    public static void spawn(ClientLevel level,
                      double fromX, double fromY, double fromZ,
                      double toX, double toY, double toZ,
                      float scale) {
        // Вектор НАЗАД (от текущей позиции к предыдущей) — как в entity-пути
        // (MissileBaseEntity.spawnContrailWithOffset: xOld - getX()) и в оригинале
        // 1.7.10 (lastTickPos - pos). spawnSegments ставит сегменты вдоль этого
        // вектора: при векторе вперёд они ложились ПО КУРСУ ракеты, а не позади.
        Vec3 motion = new Vec3(fromX - toX, fromY - toY, fromZ - toZ);
        double len = motion.length();
        if (len <= 1.0E-6D) {
            return;
        }
        Vec3 motionNorm = motion.normalize();
        // Выхлоп дует назад — противоположно полёту, т.е. вдоль motionNorm.
        Vec3 exhaust = motionNorm;
        spawnSegments(level, toX, toY, toZ, motionNorm, len, exhaust, scale, 0.0D, 0.0D, 0.0D);
    }

        /** Пар привязан к настройке частиц: на «минимум» не спавнится вовсе, на «меньше» — вдвое реже. */
        public static boolean allowVapor() {
            return particleStatus() != net.minecraft.client.ParticleStatus.MINIMAL;
        }

        public static boolean isVaporHalved() {
            return particleStatus() == net.minecraft.client.ParticleStatus.DECREASED;
        }

        /** Горячий contrail на «минимум» и «меньше» — вдвое реже. */
        private static boolean halveContrail() {
            return particleStatus() != net.minecraft.client.ParticleStatus.ALL;
        }

        private static net.minecraft.client.ParticleStatus particleStatus() {
            return net.minecraft.client.Minecraft.getInstance().options.particles().get();
        }

        public static void spawnSegments(ClientLevel level,
                                  double anchorX, double anchorY, double anchorZ,
                                  Vec3 motionNorm, double len,
                                  Vec3 exhaustVelocity, float scale,
                                  double offsetX, double offsetY, double offsetZ) {
        int segmentCount = Math.max(1, Math.min((int) len, 10));
        boolean halve = halveContrail();

        if (MissileContrailNT.sprites == null) {
            return; // Провайдеры ещё не зарегистрированы (ранний кадр)
        }
        for (int i = 0; i < segmentCount; i++) {
            if (halve && (i & 1) == 1) {
                continue;
            }
            double j = i - len;
            double px = anchorX - motionNorm.x * j + offsetX;
            double py = anchorY - motionNorm.y * j + offsetY;
            double pz = anchorZ - motionNorm.z * j + offsetZ;

            com.hbm_m.particle.nt.ParticleEngineNT.INSTANCE.add(new MissileContrailNT(
                    level, px, py, pz,
                    exhaustVelocity.x, exhaustVelocity.y, exhaustVelocity.z,
                    scale));
        }
    }
}
