package com.hbm_m.particle.explosions.nuclear.small;

import com.hbm_m.particle.ModExplosionParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ✅ РАСШИРЕНИЯ ДЛЯ ЯДЕРНЫХ ЭФФЕКТОВ
 * 
 * Новые методы для спавна УВЕЛИЧЕННЫХ частиц и полного грибного облака
 * 
 * ДОБАВИТЬ В ExplosionParticleUtils класс!
 */
public class NuclearExplosionExtensions {

    private static final Logger LOGGER = LoggerFactory.getLogger(NuclearExplosionExtensions.class);

    // ════════════════════════════════════════════════════════════════════════
    // 🔴 УВЕЛИЧЕННЫЕ ИСКРЫ (LargeExplosionSpark)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * ✅ Спавн УВЕЛИЧЕННЫХ оранжевых искр (0.6-1.2 размер)
     * 
     * @param level ServerLevel
     * @param x координата X
     * @param y координата Y
     * @param z координата Z
     */
    public static void spawnCustomNuclearSpark(ServerLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed) {
        
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                clientLevel.addAlwaysVisibleParticle(
                    (SimpleParticleType) ModExplosionParticles.LARGE_EXPLOSION_SPARK.get(),
                    true,
                    x, y, z,
                    xSpeed, ySpeed, zSpeed
                );
            });
        });
    }

    /**
     * ✅ Спавн множества УВЕЛИЧЕННЫХ искр со сферическим распределением
     */
    public static void spawnLargeExplosionSparks(ServerLevel level, double x, double y, double z, int particleCount) {
        
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int i = 0; i < particleCount; i++) {
                    double theta = level.random.nextDouble() * 2 * Math.PI;
                    double phi = level.random.nextDouble() * Math.PI;
                    double dirX = Math.sin(phi) * Math.cos(theta);
                    double dirY = Math.cos(phi);
                    double dirZ = Math.sin(phi) * Math.sin(theta);
                    
                    double speed = 0.6 + level.random.nextDouble() * 0.6; // Мощнее обычных
                    double xSpeed = dirX * speed;
                    double ySpeed = dirY * speed;
                    double zSpeed = dirZ * speed;

                    clientLevel.addAlwaysVisibleParticle(
                        (SimpleParticleType) ModExplosionParticles.LARGE_EXPLOSION_SPARK.get(),
                        true,
                        x, y, z,
                        xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // ⚫ УВЕЛИЧЕННЫЙ ДЫМ (LargeDarkSmoke)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * ✅ Спавн УВЕЛИЧЕННОГО дыма (1.0-2.5 размер)
     */
    public static void spawnCustomNuclearSmoke(ServerLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed) {
        
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                clientLevel.addAlwaysVisibleParticle(
                    (SimpleParticleType) ModExplosionParticles.LARGE_DARK_SMOKE.get(),
                    true,
                    x, y, z,
                    xSpeed, ySpeed, zSpeed
                );
            });
        });
    }

    /**
     * ✅ Спавн множества УВЕЛИЧЕННОГО дыма со сферическим распределением
     */
    public static void spawnLargeDarkSmokes(ServerLevel level, double x, double y, double z, int particleCount) {
        
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int i = 0; i < particleCount; i++) {
                    double theta = level.random.nextDouble() * 2 * Math.PI;
                    double phi = level.random.nextDouble() * Math.PI;
                    double radius = level.random.nextDouble() * 3.0;
                    
                    double offsetX = radius * Math.sin(phi) * Math.cos(theta);
                    double offsetY = radius * Math.sin(phi) * Math.sin(theta);
                    double offsetZ = radius * Math.cos(phi);
                    
                    double expansionSpeed = 0.3 + level.random.nextDouble() * 0.2;
                    double xSpeed = (offsetX / Math.max(radius, 0.1)) * expansionSpeed;
                    double ySpeed = (offsetY / Math.max(radius, 0.1)) * expansionSpeed;
                    double zSpeed = (offsetZ / Math.max(radius, 0.1)) * expansionSpeed;

                    clientLevel.addAlwaysVisibleParticle(
                        (SimpleParticleType) ModExplosionParticles.LARGE_DARK_SMOKE.get(),
                        true,
                        x + offsetX, y + offsetY, z + offsetZ,
                        xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // 🍄 ПОЛНОЕ ЯДЕРНОЕ ГРИБНОЕ ОБЛАКО
    // ════════════════════════════════════════════════════════════════════════

    /**
     * ✅ ГЛАВНЫЙ МЕТОД: Спавн полного ядерного грибного облака
     * 
     * Это ЕДИНСТВЕННЫЙ метод, который нужно вызвать для создания эффекта!
     * 
     * Внутри использует:
     * - NuclearMushroomCloud.spawnNuclearMushroom() для многоуровневого облака
     * 
     * @param level ServerLevel
     * @param x центр X
     * @param y центр Y (база облака)
     * @param z центр Z
     */
    public static void spawnNuclearMushroomCloud(ServerLevel level, double x, double y, double z) {
        
        LOGGER.info("[NUCLEAR] Spawning mushroom cloud at ({}, {}, {})", x, y, z);
        
        // ✅ Запускаем спавн грибного облака
        NuclearMushroomCloud.spawnNuclearMushroom(level, x, y, z, level.random);
        
        // 🔊 Звуковой эффект (опционально)
        // level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 1.0F);
    }

    /**
     * ✅ ДОПОЛНИТЕЛЬНЫЙ ЭФФЕКТ: Кольцо мощной ударной волны
     * 
     * Используется после грибного облака для усиления эффекта
     */
    public static void spawnEnhancedShockwave(ServerLevel level, double x, double y, double z) {
        
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                int particleCount = 800; // Больше частиц для мощной волны
                
                for (int i = 0; i < particleCount; i++) {
                    double angle = (i / (double) particleCount) * 2 * Math.PI;
                    double startRadius = 8.0 + level.random.nextDouble() * 3.0;
                    
                    double offsetX = Math.cos(angle) * startRadius;
                    double offsetZ = Math.sin(angle) * startRadius;
                    double offsetY = (level.random.nextDouble() - 0.5) * 3.0;
                    
                    double expansionSpeed = 0.8 + level.random.nextDouble() * 0.3;
                    double xSpeed = Math.cos(angle) * expansionSpeed;
                    double zSpeed = Math.sin(angle) * expansionSpeed;
                    double ySpeed = -0.05 + level.random.nextDouble() * 0.15;

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
     * ✅ КОМБО: Полный ядерный взрыв (гриб + ударная волна + искры)
     * 
     * Это最полный эффект - используй это в блоке!
     */
    public static void spawnFullNuclearExplosion(ServerLevel level, double x, double y, double z) {
        
        LOGGER.info("[NUCLEAR] Spawning FULL nuclear explosion at ({}, {}, {})", x, y, z);
        
        // ✅ Фаза 1: Мощные начальные искры (сразу)
        spawnLargeExplosionSparks(level, x, y, z, 600);
        
        // ✅ Фаза 2: Ударная волна (через 2 тика)
        level.getServer().tell(new net.minecraft.server.TickTask(2, () ->
            spawnEnhancedShockwave(level, x, y, z)
        ));
        
        // ✅ Фаза 3: Грибное облако (через 5 тиков)
        level.getServer().tell(new net.minecraft.server.TickTask(5, () ->
            spawnNuclearMushroomCloud(level, x, y, z)
        ));
    }
}
