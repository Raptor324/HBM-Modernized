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
 * ✅ УТИЛИТЫ ДЛЯ ВЗРЫВНЫХ ЭФФЕКТОВ
 *
 * Группирует все эффекты для разных типов взрывов:
 * - spawnAirBombExplosion() - фугасный взрыв
 * - spawnNukeExplosion() - ядерный взрыв
 */
public class ExplosionParticleUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExplosionParticleUtils.class);

    // ════════════════════════════════════════════════════════════════════════
    // 💣 ФУГАСНЫЙ ВЗРЫВ (AIR BOMB)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * ✅ ПОЛНЫЙ ЭФФЕКТ ФУГАСНОГО ВЗРЫВА
     *
     * Включает: искры, огненный шар, шапку, ударную волну
     */
    public static void spawnAirBombExplosion(ServerLevel level, double x, double y, double z) {
        spawnAirBombSparks(level, x, y, z);
        spawnAirBombMushroomCloud(level, x, y, z);
        spawnAirBombShockwave(level, x, y, z);
    }

    /**
     * ✅ Спавн 400 оранжевых искр
     */
    public static void spawnAirBombSparks(ServerLevel level, double x, double y, double z) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int i = 0; i < 400; i++) {
                    double theta = level.random.nextDouble() * 2 * Math.PI;
                    double phi = level.random.nextDouble() * Math.PI;

                    double dirX = Math.sin(phi) * Math.cos(theta);
                    double dirY = Math.cos(phi);
                    double dirZ = Math.sin(phi) * Math.sin(theta);

                    double speed = 0.8 + level.random.nextDouble() * 0.5;

                    double xSpeed = dirX * speed;
                    double ySpeed = dirY * speed;
                    double zSpeed = dirZ * speed;

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
     * ✅ Спавн 400 оранжевых искр
     */
    public static void spawnAirBombFireSparks(ServerLevel level, double x, double y, double z) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int i = 0; i < 400; i++) {
                    double theta = level.random.nextDouble() * 2 * Math.PI;
                    double phi = level.random.nextDouble() * Math.PI;

                    double dirX = Math.sin(phi) * Math.cos(theta);
                    double dirY = Math.cos(phi);
                    double dirZ = Math.sin(phi) * Math.sin(theta);

                    double speed = 0.8 + level.random.nextDouble() * 0.5;

                    double xSpeed = dirX * speed;
                    double ySpeed = dirY * speed;
                    double zSpeed = dirZ * speed;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.FIRE_SPARK.get(),
                            true,
                            x, y, z,
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    /**
     * ✅ КОЛЬЦО УДАРНОЙ ВОЛНЫ
     */
    public static void spawnAirBombShockwave(ServerLevel level, double x, double y, double z) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                int particleCount = 400;

                for (int i = 0; i < particleCount; i++) {
                    double angle = (i / (double) particleCount) * 2 * Math.PI;

                    double startRadius = 9.0 + level.random.nextDouble() * 2.0;

                    double offsetX = Math.cos(angle) * startRadius;
                    double offsetZ = Math.sin(angle) * startRadius;
                    double offsetY = (level.random.nextDouble() - 0.5) * 2.0;

                    double expansionSpeed = 0.6 + level.random.nextDouble() * 0.2;

                    double xSpeed = Math.cos(angle) * expansionSpeed;
                    double zSpeed = Math.sin(angle) * expansionSpeed;
                    double ySpeed = -0.05 + level.random.nextDouble() * 0.1;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.WAVE_SMOKE.get(),
                            true,
                            x + offsetX, y + offsetY, z + offsetZ,
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    /**
     * ✅ ГРИБОВИДНОЕ ОБЛАКО (огненный шар + шапка)
     */
    public static void spawnAirBombMushroomCloud(ServerLevel level, double x, double y, double z) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                // ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
                // ┃ ЧАСТЬ 1: СФЕРИЧЕСКИЙ ОГНЕННЫЙ ШАР                   ┃
                // ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

                for (int i = 0; i < 550; i++) {
                    double theta = level.random.nextDouble() * 2 * Math.PI;
                    double phi = level.random.nextDouble() * Math.PI;
                    double radius = 0.0 + level.random.nextDouble() * 4.0;

                    double offsetX = radius * Math.sin(phi) * Math.cos(theta);
                    double offsetY = radius * Math.sin(phi) * Math.sin(theta);
                    double offsetZ = radius * Math.cos(phi);

                    double expansionSpeed = 0.5 + level.random.nextDouble() * 0.1;
                    double xSpeed = (offsetX / Math.max(radius, 0.1)) * expansionSpeed;
                    double ySpeed = (offsetY / Math.max(radius, 0.1)) * expansionSpeed;
                    double zSpeed = (offsetZ / Math.max(radius, 0.1)) * expansionSpeed;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.DARK_SMOKE.get(),
                            true,
                            x + offsetX, y + offsetY, z + offsetZ,
                            xSpeed, ySpeed, zSpeed
                    );
                }

                // ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
                // ┃ ЧАСТЬ 2: ШАПКА ГРИБОВИДНОГО ОБЛАКА                  ┃
                // ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

                for (int i = 0; i < 150; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double radius = 0.0 + level.random.nextDouble() * 4.0;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    double capY = y + 1 + level.random.nextDouble() * 2;

                    double xSpeed = Math.cos(angle) * 0.5;
                    double ySpeed = -0.01 + level.random.nextDouble() * 0.02;
                    double zSpeed = Math.sin(angle) * 0.5;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.DARK_SMOKE.get(),
                            true,
                            x + offsetX, capY, z + offsetZ,
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    /**
     * ☠️ СПАВН AGENT ORANGE
     */
    public static void spawnAgentOrange(ServerLevel level, double x, double y, double z, int particleCount) {

        // ✅ ТОЛЬКО СПАВН ЧАСТИЦ (логика внутри самой частицы!)
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;

                    double xSpeed = offsetX * 0.1;
                    double ySpeed = -0.1 - level.random.nextDouble() * 0.2;
                    double zSpeed = offsetZ * 0.1;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.AGENT_ORANGE.get(),
                            true,
                            x + offsetX, y, z + offsetZ,
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    /**
     * ☠️ СПАВН AGENT ORANGE ДЛЯ ГЕЙЗЕРА (вертикальная струя)
     *
     * @param verticalSpeed Скорость вверх (0.1 - 0.3)
     */
    public static void spawnAgentOrangeGeyser(ServerLevel level, double x, double y, double z,
                                              int particleCount, double verticalSpeed) {
        spawnAgentOrangeGeyser(level, x, y, z, particleCount, verticalSpeed, 0.5);
    }

    /**
     * ☠️ СПАВН AGENT ORANGE ДЛЯ ГЕЙЗЕРА (с горизонтальным разбросом)
     *
     * @param verticalSpeed Скорость вверх
     * @param horizontalSpread Радиус горизонтального разброса
     */
    public static void spawnAgentOrangeGeyser(ServerLevel level, double x, double y, double z,
                                              int particleCount, double verticalSpeed,
                                              double horizontalSpread) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int i = 0; i < particleCount; i++) {
                    // ✅ ГОРИЗОНТАЛЬНОЕ СМЕЩЕНИЕ
                    double offsetX = (level.random.nextDouble() - 0.5) * horizontalSpread;
                    double offsetZ = (level.random.nextDouble() - 0.5) * horizontalSpread;

                    // ✅ СКОРОСТИ
                    double xSpeed = offsetX * 0.08; // Медленный разлёт в стороны
                    double ySpeed = verticalSpeed + (level.random.nextDouble() - 0.5) * 0.05; // В основном вверх
                    double zSpeed = offsetZ * 0.08;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.AGENT_ORANGE.get(),
                            true,
                            x + offsetX * 0.2, y, z + offsetZ * 0.2, // Небольшой начальный разброс
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }


    /**
     * ✅ Универсальный метод для спавна ЛЮБОГО типа взрывных частиц
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

            level.addAlwaysVisibleParticle(
                    particleType,
                    true,
                    x, y, z,
                    vx, vy, vz
            );
        }
    }
}
