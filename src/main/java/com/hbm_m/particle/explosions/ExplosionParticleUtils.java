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
                    // ✅ СФЕРИЧЕСКОЕ РАСПРЕДЕЛЕНИЕ ИСКР
                    double theta = level.random.nextDouble() * 2 * Math.PI;
                    double phi = level.random.nextDouble() * Math.PI;

                    double dirX = Math.sin(phi) * Math.cos(theta);
                    double dirY = Math.cos(phi);
                    double dirZ = Math.sin(phi) * Math.sin(theta);

                    // ✅ МИНИМАЛЬНАЯ СКОРОСТЬ: 0.3-0.8 блоков/сек (практически незаметное движение)
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
     * ════════════════════════════════════════════════════════════════════════
     * 🌊 МЕТОД 2: КОЛЬЦО ВОЛНОВОГО ДЫМА (РАСШИРЯЕТСЯ ПО ЗЕМЛЕ)
     * ════════════════════════════════════════════════════════════════════════
     *
     * Создаёт расширяющееся кольцо светло-серого дыма, которое:
     * - Облетает препятствия
     * - Сохраняет толщину (±1 блок от высоты y)
     * - Расширяется радиально от центра
     */
    public static void spawnAirBombShockwave(ServerLevel level, double x, double y, double z) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                int particleCount = 400;

                for (int i = 0; i < particleCount; i++) {
                    double angle = (i / (double) particleCount) * 2 * Math.PI;

                    // ✅ УВЕЛИЧЕН НАЧАЛЬНЫЙ РАДИУС: 5-7 блоков
                    double startRadius = 9.0 + level.random.nextDouble() * 2.0;

                    double offsetX = Math.cos(angle) * startRadius;
                    double offsetZ = Math.sin(angle) * startRadius;
                    double offsetY = (level.random.nextDouble() - 0.5) * 2.0;

                    // ✅ ПРЕЖНЯЯ СКОРОСТЬ РАСШИРЕНИЯ: 0.4-0.6 блоков/тик (заметное расширение)
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
     * ✅ Спавн грибовидного облака (сфера + шапка)
     *
     * ИСПРАВЛЕНИЕ: Оба компонента видны на больших расстояниях
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

                    // ✅ МИНИМАЛЬНАЯ СКОРОСТЬ РАСШИРЕНИЯ: 0.03-0.08 блоков/тик (почти статичная сфера)
                    double expansionSpeed = 0.5 + level.random.nextDouble() * 0.1;
                    double xSpeed = (offsetX / radius) * expansionSpeed;
                    double ySpeed = (offsetY / radius) * expansionSpeed;
                    double zSpeed = (offsetZ / radius) * expansionSpeed;

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

                    // ✅ МИНИМАЛЬНАЯ ВЕРТИКАЛЬНАЯ СКОРОСТЬ: -0.01 до +0.01
                    double ySpeed = -0.01 + level.random.nextDouble() * 0.02;

                    // ✅ МИНИМАЛЬНАЯ СКОРОСТЬ: 0.05 блоков/тик (почти статичная шапка)
                    double xSpeed = Math.cos(angle) * 0.5;
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

            level.addAlwaysVisibleParticle(
                    particleType,
                    true,
                    x, y, z,
                    vx, vy, vz
            );
        }
    }
}
