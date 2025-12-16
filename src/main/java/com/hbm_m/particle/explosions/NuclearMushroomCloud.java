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
 * ✅ HBM STYLE + "ICEBERG" BASE
 * Основание уходит глубоко под землю, чтобы гриб не левитировал в кратерах.
 */
public class NuclearMushroomCloud {

    private static final Logger LOGGER = LoggerFactory.getLogger(NuclearMushroomCloud.class);

    public static void spawnNuclearMushroom(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {

        // 1. ОГНЕННЫЙ ШАР (Просто дым, без искр)
        spawnFireball(level, centerX, centerY, centerZ, random);

        // 2. НОЖКА
        spawnMushroomStem(level, centerX, centerY, centerZ, random);

        // 3. ⚓ ОСНОВАНИЕ "АЙСБЕРГ" (Уходит на -10 блоков вниз)
        spawnMushroomBase(level, centerX, centerY, centerZ, random);

        // 4. ВЗРЫВНАЯ ВОЛНА (Кольцо)
        spawnShockwaveRing(level, centerX, centerY, centerZ, random);

        // 5. КОЛЬЦО КОНДЕНСАЦИИ
        spawnCondensationRing(level, centerX, centerY + 12, centerZ, random);

        // 6. ШАПКА
        spawnMushroomCap(level, centerX, centerY, centerZ, random);
    }

    // --- МЕТОДЫ ---

    private static void spawnFireball(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {
        for (int i = 0; i < 40; i++) {
            double r = random.nextDouble() * 2.5;
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;

            double x = Math.sin(phi) * Math.cos(theta) * r;
            double y = Math.cos(phi) * r * 0.5;
            double z = Math.sin(phi) * Math.sin(theta) * r;

            // Используем ЖЕЛТЫЙ ДЫМ, не искры
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
     * ⚓ ОСНОВАНИЕ "АЙСБЕРГ"
     * Заполняет пространство от Y до Y-10
     */
    private static void spawnMushroomBase(ServerLevel level, double centerX, double centerY, double centerZ, RandomSource random) {
        // Увеличили количество частиц (150), чтобы заполнить объем
        for (int i = 0; i < 150; i++) {
            // Глубина от 0 до 10 блоков вниз
            double depth = random.nextDouble() * 10.0;
            double particleY = centerY - depth + 0.5; // +0.5 чтобы чуть выше дна блока

            double angle = random.nextDouble() * Math.PI * 2;

            // Радиус зависит от глубины (чем глубже, тем чуть уже, как корень/айсберг)
            // На поверхности (depth 0): 6.0 + rand(4.0) = 6-10 блоков (ШИРОКОЕ!)
            // На глубине (depth 10): 4.0 + rand(3.0) = 4-7 блоков
            double widthFactor = 1.0 - (depth / 20.0); // Небольшое сужение к низу
            double maxRadius = (6.0 + random.nextDouble() * 4.0) * widthFactor;

            double radius = random.nextDouble() * maxRadius;

            double offX = Math.cos(angle) * radius;
            double offZ = Math.sin(angle) * radius;

            // Желтый ядерный дым
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

            // Черный дым (DARK_SMOKE)
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

            // Белый дым
            spawnNuclearParticle(level, x + offX, y, z + offZ, offX*0.01, 0.0, offZ*0.01, false, true, false);
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
