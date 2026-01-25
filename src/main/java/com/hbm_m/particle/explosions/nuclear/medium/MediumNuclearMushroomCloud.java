package com.hbm_m.particle.explosions.nuclear.medium;

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
 * ✅ УТИЛИТА СПАВНА ЧАСТЕЙ ЯДЕРНОГО ВЗРЫВА
 * Методы сделаны public static для вызова из NuclearChargeBlock.
 */
public class MediumNuclearMushroomCloud {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediumNuclearMushroomCloud.class);

    /**
     * ⚫ ЧЕРНАЯ СФЕРА (Вспышка)
     */
    public static void spawnBlackSphere(ServerLevel level, double x, double y, double z, RandomSource random) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                for (int i = 0; i < 750; i++) {
                    double theta = random.nextDouble() * 2 * Math.PI;
                    double phi = random.nextDouble() * Math.PI;
                    double radius = 0.0 + random.nextDouble() * 4.0;

                    double offsetX = radius * Math.sin(phi) * Math.cos(theta);
                    double offsetY = radius * Math.sin(phi) * Math.sin(theta);
                    double offsetZ = radius * Math.cos(phi);

                    double expansionSpeed = 0.3 + random.nextDouble() * 0.06;
                    double xSpeed = (offsetX / Math.max(radius, 0.1)) * expansionSpeed;
                    double ySpeed = (offsetY / Math.max(radius, 0.1)) * expansionSpeed - 0.05;
                    double zSpeed = (offsetZ / Math.max(radius, 0.1)) * expansionSpeed;

                    clientLevel.addAlwaysVisibleParticle(
                            (SimpleParticleType) ModExplosionParticles.DARK_SMOKE.get(),
                            true,
                            x + offsetX, y + offsetY, z + offsetZ,
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
    }

    /**
     * 💥 УДАРНАЯ ВОЛНА (Черное кольцо)
     */
    public static void spawnShockwaveRing(ServerLevel level, double x, double y, double z, RandomSource random) {
        for (int i = 0; i < 350; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 0.0 + random.nextDouble() * 4.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double capY = y + 1 + random.nextDouble() * 2;

            double xSpeed = Math.cos(angle) * 0.25;
            double ySpeed = -0.01 + random.nextDouble() * 0.02;
            double zSpeed = Math.sin(angle) * 0.25;

            spawnNuclearParticle(level, x + offsetX, capY, z + offsetZ, xSpeed, ySpeed, zSpeed, false, false, true);
        }
    }

    /**
     * 🌵 СЕГМЕНТ НОЖКИ (Для анимированного роста)
     * Спавнит небольшой кусочек столба на заданной высоте Y.
     */
    public static void spawnStemSegment(ServerLevel level, double x, double y, double z, RandomSource random) {
        // Спавним 2 слоя частиц
        for (int h = 0; h < 2; h++) {
            int count = 8;
            double radius = 1.0;
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double offX = Math.cos(angle) * radius;
                double offZ = Math.sin(angle) * radius;

                // Небольшой рандом по высоте, чтобы слои смешивались
                double actualY = y + h + random.nextDouble() * 0.5;

                spawnNuclearParticle(level, x + offX, actualY, z + offZ, 0.0, 0.0, 0.0, false, false, false);
            }
        }
    }

    /**
     * ⚓ ОСНОВАНИЕ "АЙСБЕРГ" (Широкое)
     */
    public static void spawnMushroomBase(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {
        for (int i = 0; i < 200; i++) {
            double depth = random.nextDouble() * 10.0;
            double particleY = centerY - depth + 0.5;
            double angle = random.nextDouble() * 2 * Math.PI;

            // Конус: чем глубже, тем уже. На поверхности широко.
            double widthFactor = 1.0 - (depth / 20.0);
            double maxRadius = (12.0 + random.nextDouble() * 6.0) * widthFactor;

            double radius = random.nextDouble() * maxRadius;
            double offX = Math.cos(angle) * radius;
            double offZ = Math.sin(angle) * radius;

            spawnNuclearParticle(level, centerX + offX, particleY, centerZ + offZ, 0.0, 0.0, 0.0, false, false, false);
        }
    }

    /**
     * 🍄 ШАПКА ГРИБА
     * Спавнится на высоте (centerY + 20)
     */
    public static void spawnMushroomCap(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {
        double startY = centerY + 20; // Высота начала шапки

        for (int i = 0; i < 250; i++) { // 250 частиц
            double r = random.nextDouble() * 9.0;
            double theta = random.nextDouble() * 2 * Math.PI;
            double phi = random.nextDouble() * (Math.PI / 2.0); // Полусфера

            double x = Math.sin(phi) * Math.cos(theta) * r;
            double y = Math.cos(phi) * r * 0.7; // Сплюснутая
            double z = Math.sin(phi) * Math.sin(theta) * r;

            double finalY = startY + y;

            spawnNuclearParticle(level, centerX + x, finalY, centerZ + z, 0.0, 0.0, 0.0, false, false, false);
        }
    }

    /**
     * ☁️ КОЛЬЦО КОНДЕНСАЦИИ
     */
    public static void spawnCondensationRing(ServerLevel level, double x, double y, double z, RandomSource random) {
        int particles = 50;
        double radius = 7.0;
        for (int i = 0; i < particles; i++) {
            double angle = (i / (double)particles) * 2 * Math.PI;
            double offX = Math.cos(angle) * radius;
            double offZ = Math.sin(angle) * radius;

            // Белый дым, чуть расширяется
            spawnNuclearParticle(level, x + offX, y, z + offZ, offX*0.01, 0.0, offZ*0.01, false, true, false);
        }
    }

    /**
     * 🛠️ ВНУТРЕННИЙ СПАВНЕР (Клиентский)
     */
    private static void spawnNuclearParticle(ServerLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, boolean isSpark, boolean isMushroomSmoke, boolean isDarkSmoke) {
        level.getServer().execute(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel clientLevel = Minecraft.getInstance().level;
                if (clientLevel == null) return;

                SimpleParticleType particleType;

                if (isDarkSmoke) {
                    particleType = (SimpleParticleType) ModExplosionParticles.DARK_SMOKE.get();
                } else if (isMushroomSmoke) {
                    particleType = (SimpleParticleType) ModExplosionParticles.MUSHROOM_SMOKE.get();
                } else {
                    particleType = (SimpleParticleType) ModExplosionParticles.LARGE_DARK_SMOKE.get();
                }

                clientLevel.addAlwaysVisibleParticle(particleType, true, x, y, z, xSpeed, ySpeed, zSpeed);
            });
        });
    }
}
