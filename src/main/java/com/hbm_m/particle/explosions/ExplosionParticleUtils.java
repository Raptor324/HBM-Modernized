package com.hbm_m.particle.explosions;

import com.hbm_m.particle.ModExplosionParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ✅ ИСПРАВЛЕННАЯ ВЕРСИЯ: использует addAlwaysVisibleParticle()
 *
 * Утилита для спавна взрывных частиц на ЛЮБЫХ расстояниях (512+ блоков)
 *
 * Комбинирует ДВА подхода для гарантированной видимости:
 * 1. addAlwaysVisibleParticle() - обходит ограничение дистанции
 * 2. LongRangeParticleRenderType - кастомный рендер для коррректного отображения
 */
public class ExplosionParticleUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExplosionParticleUtils.class);

    /**
     * ✅ Спавнит быстрые искры взрыва на очень дальних расстояниях
     * Вызывай эту функцию вместо обычного createExplosion()
     *
     * @param x координата X центра взрыва
     * @param y координата Y центра взрыва
     * @param z координата Z центра взрыва
     * @param intensity интенсивность (0.1 - 5.0). Влияет на скорость и количество частиц
     */
    public static void spawnExplosion(double x, double y, double z, float intensity) {
        ClientLevel level = Minecraft.getInstance().level;

        // ✅ Проверяем, не NULL ли уровень
        if (level == null) {
            LOGGER.warn("Cannot spawn explosion particles: ClientLevel is null");
            return;
        }

        RandomSource random = RandomSource.create();

        // ✅ Количество частиц зависит от интенсивности
        int particleCount = Math.round(20 * intensity);

        for (int i = 0; i < particleCount; i++) {
            // ✅ Случайные векторы скорости во все стороны
            double vx = (random.nextDouble() - 0.5) * 2.0 * intensity;
            double vy = random.nextDouble() * 1.5 * intensity;
            double vz = (random.nextDouble() - 0.5) * 2.0 * intensity;

            // ✅ ГЛАВНОЕ ИЗМЕНЕНИЕ: addAlwaysVisibleParticle() вместо addParticle()
            // Это ПОЛНОСТЬЮ обходит ограничение в 32 блока
            // Частицы будут видны на расстояниях > 512 блоков
            // Третий параметр (boolean) = force rendering (игнорирует расстояние)
            level.addAlwaysVisibleParticle(
                    (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get(),
                    true, // force rendering - ОБЯЗАТЕЛЬНО true!
                    x, y, z,
                    vx, vy, vz
            );
        }
    }

    /**
     * ✅ Спавнит яркую вспышку в центре взрыва
     * Видна на максимально дальних расстояниях благодаря высокой яркости
     *
     * @param x координата X вспышки
     * @param y координата Y вспышки
     * @param z координата Z вспышки
     */
    public static void spawnFlash(double x, double y, double z) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            LOGGER.warn("Cannot spawn explosion flash: ClientLevel is null");
            return;
        }

        // ✅ ГЛАВНОЕ ИЗМЕНЕНИЕ: addAlwaysVisibleParticle() вместо addParticle()
        // Вспышка без движения - она тут же появляется в центре
        level.addAlwaysVisibleParticle(
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_FLASH.get(),
                true, // force rendering
                x, y, z,
                0.0, 0.0, 0.0
        );
    }

    /**
     * ✅ Спавнит шокволну (кольцо расширения)
     *
     * @param x координата X центра шокволны
     * @param y координата Y центра шокволны
     * @param z координата Z центра шокволны
     * @param radius радиус расширения кольца (в пикселях частицы)
     */
    public static void spawnShockwave(double x, double y, double z, float radius) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            LOGGER.warn("Cannot spawn shockwave: ClientLevel is null");
            return;
        }

        // ✅ Рисуем кольцо вокруг центра взрыва
        int points = Math.round(16 * (radius / 5.0f)); // Больше точек для больших радиусов

        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI * i) / points;

            double px = x + Math.cos(angle) * radius;
            double py = y;
            double pz = z + Math.sin(angle) * radius;

            level.addAlwaysVisibleParticle(
                    (SimpleParticleType) ModExplosionParticles.SHOCKWAVE_RING.get(),
                    true,
                    px, py, pz,
                    0.0, 0.0, 0.0
            );
        }
    }

    /**
     * ✅ Универсальный метод для спавна ЛЮБОГО типа взрывных частиц
     * Используй это если нужна кастомная логика
     *
     * @param x координата X
     * @param y координата Y
     * @param z координата Z
     * @param intensity интенсивность разброса
     * @param particleType тип частицы (из ModExplosionParticles)
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

            // ✅ Используем addAlwaysVisibleParticle() для гарантированной видимости
            level.addAlwaysVisibleParticle(
                    particleType,
                    true,
                    x, y, z,
                    vx, vy, vz
            );
        }
    }

    /**
     * ✅ КОМБО: Полный взрыв с вспышкой, искрами и шокволной
     * Это то, что нужно вызывать из основного кода при взрыве
     *
     * @param x координата X взрыва
     * @param y координата Y взрыва
     * @param z координата Z взрыва
     * @param power мощность взрыва (0.5 - 3.0 обычно)
     */
    public static void spawnFullExplosion(double x, double y, double z, float power) {
        // ✅ 1. Яркая вспышка в центре
        spawnFlash(x, y, z);

        // ✅ 2. Быстрые искры во все стороны
        spawnExplosion(x, y, z, power);

        // ✅ 3. Шокволна (кольцо)
        spawnShockwave(x, y, z, power * 2.0f);
    }
}