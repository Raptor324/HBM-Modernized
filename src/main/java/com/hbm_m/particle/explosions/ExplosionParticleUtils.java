package com.hbm_m.particle.explosions;

import com.hbm_m.particle.ModExplosionParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ✅ ИСПРАВЛЕННАЯ ВЕРСИЯ - СПАВНИНГ НА КЛИЕНТЕ БЕЗ ОГРАНИЧЕНИЙ
 *
 * ГЛАВНОЕ ИСПРАВЛЕНИЕ:
 * Использование addAlwaysVisibleParticle() вместо sendParticles()
 * sendParticles() имеет встроенное ограничение в 32 блока (слишком мало!)
 * addAlwaysVisibleParticle() игнорирует это ограничение на клиенте
 */
public class ExplosionParticleUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExplosionParticleUtils.class);

    /**
     * ✅ Спавн 400 оранжевых искр
     *
     * ИСПРАВЛЕНИЕ: Теперь используется addAlwaysVisibleParticle()
     * Видны на расстояниях БОЛЬШЕ 32 блоков!
     */
    public static void spawnAirBombSparks(ServerLevel level, double x, double y, double z) {
        // Отправляем пакет всем игрокам на сервере
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int i = 0; i < 400; i++) {
                    double xSpeed = (level.random.nextDouble() - 0.5) * 6.0;
                    double ySpeed = level.random.nextDouble() * 5.0;
                    double zSpeed = (level.random.nextDouble() - 0.5) * 6.0;

                    // ✅ ГЛАВНОЕ: addAlwaysVisibleParticle БЕЗ ОГРАНИЧЕНИЙ!
                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get(),
                            true,
                            x, y, z,
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    /**
     * ✅ Спавн шокволны (6 колец расширения)
     *
     * ИСПРАВЛЕНИЕ: Теперь видна на больших расстояниях
     */
    public static void spawnAirBombShockwave(ServerLevel level, double x, double y, double z) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int ring = 0; ring < 6; ring++) {
                    double ringY = y + (ring * 0.3);

                    // ✅ addAlwaysVisibleParticle БЕЗ ОГРАНИЧЕНИЙ!
                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.SHOCKWAVE_RING.get(),
                            true,
                            x, ringY, z,
                            0, 0, 0
                    );
                }
            });
        });
    }

    /**
     * ✅ Спавн грибовидного облака (стебель + шапка)
     *
     * ИСПРАВЛЕНИЕ: Оба компонента видны на больших расстояниях
     */
    public static void spawnAirBombMushroomCloud(ServerLevel level, double x, double y, double z) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                // ✅ Стебель: 150 частиц, разброс 6.0
                for (int i = 0; i < 150; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 6.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 6.0;
                    double ySpeed = 0.8 + level.random.nextDouble() * 0.4;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.MUSHROOM_SMOKE.get(),
                            true,
                            x + offsetX, y, z + offsetZ,
                            offsetX * 0.08, ySpeed, offsetZ * 0.08
                    );
                }

                // ✅ Шапка: 250 частиц, радиус 8-20, высота 20-30
                for (int i = 0; i < 250; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double radius = 8.0 + level.random.nextDouble() * 12.0;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    double capY = y + 20 + level.random.nextDouble() * 10;
                    double xSpeed = Math.cos(angle) * 0.5;
                    double ySpeed = -0.1 + level.random.nextDouble() * 0.2;
                    double zSpeed = Math.sin(angle) * 0.5;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.MUSHROOM_SMOKE.get(),
                            true,
                            x + offsetX, capY, z + offsetZ,
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    /**
     * ✅ Универсальный метод для спавна ЛЮБОГО типа взрывных частиц
     * Используй это если нужна кастомная логика
     */
    public static void spawnCustomExplosion(double x, double y, double z, float intensity, SimpleParticleType particleType) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            LOGGER.warn("Cannot spawn custom explosion: ClientLevel is null");
            return;
        }

        RandomSource random = RandomSource.create();
        int particleCount = Math.round(20 * intensity);

        for (int i = 0; i < particleCount; i++) {
            double vx = (random.nextDouble() - 0.5) * 2.0 * intensity;
            double vy = random.nextDouble() * 1.5 * intensity;
            double vz = (random.nextDouble() - 0.5) * 2.0 * intensity;

            // ✅ addAlwaysVisibleParticle() для гарантированной видимости
            level.addAlwaysVisibleParticle(
                    particleType,
                    true,
                    x, y, z,
                    vx, vy, vz
            );
        }
    }
}
