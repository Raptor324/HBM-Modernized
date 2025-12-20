package com.hbm_m.particle.explosions.nuclear.small;

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
 * ✅ ЯДЕРНЫЙ ВЗРЫВ (БЕЗ ЗАДЕРЖЕК, МГНОВЕННЫЙ)
 */
public class NuclearMushroomCloud {

    private static final Logger LOGGER = LoggerFactory.getLogger(NuclearMushroomCloud.class);

    public static void spawnNuclearMushroom(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {

        // 1. ЧЕРНАЯ СФЕРА + УДАРНАЯ ВОЛНА
        spawnBlackSphere(level, centerX, centerY, centerZ, random);
        spawnShockwaveRing(level, centerX, centerY, centerZ, random);

        // 2. ВСЕ ЧАСТИ ГРИБА (Сразу)
        spawnFireball(level, centerX, centerY, centerZ, random);
        spawnMushroomStem(level, centerX, centerY, centerZ, random);
        spawnMushroomBase(level, centerX, centerY, centerZ, random);
        spawnCondensationRing(level, centerX, centerY + 12, centerZ, random);
        spawnMushroomCap(level, centerX, centerY, centerZ, random);
    }

    private static void spawnBlackSphere(ServerLevel level, double x, double y, double z, RandomSource random) {
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
                    clientLevel.addAlwaysVisibleParticle((SimpleParticleType) ModExplosionParticles.DARK_SMOKE.get(), true, x + offsetX, y + offsetY, z + offsetZ, xSpeed, ySpeed, zSpeed);
                }
            });
        });
    }

    private static void spawnFireball(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {
        for (int i = 0; i < 40; i++) {
            double r = random.nextDouble() * 2.5;
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;
            double x = Math.sin(phi) * Math.cos(theta) * r;
            double y = Math.cos(phi) * r * 0.5;
            double z = Math.sin(phi) * Math.sin(theta) * r;
            spawnNuclearParticle(level, centerX + x, centerY + y + 1, centerZ + z, 0.0, 0.0, 0.0, false, false, false);
        }
    }

    private static void spawnMushroomStem(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {
        for (int height = 3; height < 18; height++) {
            double y = centerY + height;
            int count = 6;
            double radius = 0.8;
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double offX = Math.cos(angle) * radius;
                double offZ = Math.sin(angle) * radius;
                spawnNuclearParticle(level, centerX + offX, y, centerZ + offZ, 0.0, 0.0, 0.0, false, false, false);
            }
        }
    }

    /**
     * ⚓ ОСНОВАНИЕ "АЙСБЕРГ" (ШИРОКОЕ X2)
     */
    private static void spawnMushroomBase(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {
        for (int i = 0; i < 200; i++) {
            double depth = random.nextDouble() * 10.0;
            double particleY = centerY - depth + 0.5;
            double angle = random.nextDouble() * Math.PI * 2;
            double widthFactor = 1.0 - (depth / 20.0);

            // ↔️ РАДИУС X2 (12-18 блоков)
            double maxRadius = (12.0 + random.nextDouble() * 6.0) * widthFactor;

            double radius = random.nextDouble() * maxRadius;
            double offX = Math.cos(angle) * radius;
            double offZ = Math.sin(angle) * radius;
            spawnNuclearParticle(level, centerX + offX, particleY, centerZ + offZ, 0.0, 0.0, 0.0, false, false, false);
        }
    }

    private static void spawnShockwaveRing(ServerLevel level, double x, double y, double z, RandomSource random) {
        for (int i = 0; i < 350; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
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

    private static void spawnCondensationRing(ServerLevel level, double x, double y, double z, RandomSource random) {
        int particles = 40;
        double radius = 7.0;

        for (int i = 0; i < particles; i++) {
            double angle = (i / (double)particles) * Math.PI * 2;
            double offX = Math.cos(angle) * radius;
            double offZ = Math.sin(angle) * radius;

            // +0.5 блока вверх
            double spawnY = y + 0.5;

            // скорость по Y = 0, гравитация отключается в самом классе частицы
            spawnNuclearParticle(
                    level,
                    x + offX,
                    spawnY,
                    z + offZ,
                    offX * 0.01,
                    0.0,          // было 0.0, оставляем
                    offZ * 0.01,
                    false,
                    true,          // белое кольцо (MUSHROOM_SMOKE)
                    false
            );
        }
    }


    private static void spawnMushroomCap(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {
        double startY = centerY + 18;
        for (int i = 0; i < 200; i++) {
            double r = random.nextDouble() * 9.0;
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * (Math.PI / 2.0);
            double x = Math.sin(phi) * Math.cos(theta) * r;
            double y = Math.cos(phi) * r * 0.7;
            double z = Math.sin(phi) * Math.sin(theta) * r;
            double finalY = startY + y;
            spawnNuclearParticle(level, centerX + x, finalY, centerZ + z, 0.0, 0.0, 0.0, false, false, false);
        }
    }

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
