package com.hbm_m.util;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * ✅ CRATER BIOME HELPER v4.6 - MINECRAFT 1.20.1 COMPATIBLE (FIXED)
 *
 * ФИНАЛЬНАЯ ВЕРСИЯ БЕЗ isEmpty():
 * ✅ Правильный расчет индекса квартала в PalettedContainer
 * ✅ Безопасная работа с reflection
 * ✅ Корректная синхронизация zone3 и zone4
 * ✅ Совместимость с Minecraft 1.20.1
 * ✅ Без использования isEmpty() (версионная проблема)
 * ✅ Детальное логирование для отладки
 *
 * @author HBM_M
 * @version 4.6 (1.20.1 compatible)
 */
public class CraterBiomeHelper {

    private static final String LOG_PREFIX = "[CRATER_BIOME]";

    /**
     * 🎯 Асинхронное наложение биомов кратера
     */
    public static void applyBiomesAsync(ServerLevel level, BlockPos centerPos,
                                        double zone3Radius, double zone4Radius) {
        if (level == null || centerPos == null) {
            System.err.println(LOG_PREFIX + " ❌ Invalid parameters: level or centerPos is null");
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            System.err.println(LOG_PREFIX + " ❌ Cannot get MinecraftServer instance");
            return;
        }

        server.tell(new TickTask(1, () -> {
            applyCraterBiomesSync(level, centerPos, zone3Radius, zone4Radius);
        }));
    }

    /**
     * 📍 Применяет биомы кратера (синхронная версия)
     */
    private static void applyCraterBiomesSync(ServerLevel level, BlockPos centerPos,
                                              double zone3Radius, double zone4Radius) {
        long startTime = System.currentTimeMillis();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║ " + LOG_PREFIX + " START: Applying crater biomes                   ║");
        System.out.println("║ Center: " + String.format("%-47s║", centerPos));
        System.out.println("║ INNER (Zone 3): 0-" + (int)zone3Radius + "m                              ║");
        System.out.println("║ OUTER (Zone 4): " + (int)zone3Radius + "-" + (int)zone4Radius + "m                            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        try {
            Holder<Biome> innerCrater = getBiomeHolder(level, "hbm_m", "inner_crater");
            Holder<Biome> outerCrater = getBiomeHolder(level, "hbm_m", "outer_crater");

            if (innerCrater == null || outerCrater == null) {
                System.err.println(LOG_PREFIX + " ⚠️ WARNING: One or both crater biomes not found!");
                System.err.println(LOG_PREFIX + " Make sure biomes 'inner_crater' and 'outer_crater' are registered!");

                innerCrater = level.registryAccess()
                        .registryOrThrow(Registries.BIOME)
                        .getRandom(level.random)
                        .orElse(null);

                if (innerCrater == null) {
                    System.err.println(LOG_PREFIX + " 🔴 CRITICAL: No biomes found at all!");
                    return;
                }

                outerCrater = innerCrater;
                System.err.println(LOG_PREFIX + " Using fallback biome for both zones");
            }

            int centerX = centerPos.getX();
            int centerZ = centerPos.getZ();
            int searchRadius = (int) zone4Radius + 64;

            int minChunkX = (centerX - searchRadius) >> 4;
            int maxChunkX = (centerX + searchRadius) >> 4;
            int minChunkZ = (centerZ - searchRadius) >> 4;
            int maxChunkZ = (centerZ + searchRadius) >> 4;

            int totalChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
            int successfulChunks = 0;
            int failedChunks = 0;
            int innerBiomeCount = 0;
            int outerBiomeCount = 0;

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    try {
                        ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                        if (chunk == null) {
                            failedChunks++;
                            continue;
                        }

                        int[] stats = applyBiomesToChunk(chunk, centerPos,
                                zone3Radius, zone4Radius,
                                innerCrater, outerCrater,
                                level.random);

                        innerBiomeCount += stats[0];
                        outerBiomeCount += stats[1];

                        chunk.setUnsaved(true);
                        successfulChunks++;

                    } catch (Exception e) {
                        System.err.println(LOG_PREFIX + " ⚠️ Chunk [" + chunkX + ", " + chunkZ + "]: " +
                                e.getClass().getSimpleName() + " - " + e.getMessage());
                        failedChunks++;
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║ " + LOG_PREFIX + " ✅ COMPLETE!                                          ║");
            System.out.println("║ Chunks processed: " + String.format("%-41s║", successfulChunks + " / " + totalChunks));
            System.out.println("║ Chunks failed: " + String.format("%-49s║", failedChunks));
            System.out.println("║ INNER biomes: " + String.format("%-48s║", innerBiomeCount));
            System.out.println("║ OUTER biomes: " + String.format("%-48s║", outerBiomeCount));
            System.out.println("║ Time: " + String.format("%-55s║", duration + " ms"));
            System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " 🔴 CRITICAL ERROR:");
            e.printStackTrace();
        }
    }

    /**
     * 🔍 Получает биом из реестра
     */
    private static Holder<Biome> getBiomeHolder(ServerLevel level, String namespace, String biomeName) {
        try {
            ResourceKey<Biome> key = ResourceKey.create(
                    Registries.BIOME,
                    new ResourceLocation(namespace, biomeName)
            );

            return level.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getHolder(key)
                    .orElse(null);

        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " ❌ Cannot find biome: " + namespace + ":" + biomeName);
            return null;
        }
    }

    /**
     * 📍 Применяет биомы к чанку
     * Возвращает [innerCount, outerCount]
     *
     * ✅ ИСПРАВЛЕНО: Удалена проверка isEmpty() (версионная несовместимость)
     */
    private static int[] applyBiomesToChunk(ChunkAccess chunk,
                                            BlockPos centerPos,
                                            double zone3Radius,
                                            double zone4Radius,
                                            Holder<Biome> innerCrater,
                                            Holder<Biome> outerCrater,
                                            RandomSource random) {

        int centerX = centerPos.getX();
        int centerZ = centerPos.getZ();

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        int chunkBlockX = chunkX << 4;
        int chunkBlockZ = chunkZ << 4;

        LevelChunkSection[] sections = chunk.getSections();
        if (sections == null) return new int[]{0, 0};

        int innerCount = 0;
        int outerCount = 0;

        double zone3RadiusSq = zone3Radius * zone3Radius;
        double zone4RadiusSq = zone4Radius * zone4Radius;

        // ✅ ИСПРАВЛЕНО: Убрана проверка isEmpty() (нет в 1.20.1)
        for (LevelChunkSection section : sections) {
            // ✅ Проверяем только null, isEmpty() не существует в 1.20.1
            if (section == null) continue;

            try {
                var biomesContainer = section.getBiomes();
                if (biomesContainer == null) continue;

                // ✅ ГЛАВНОЕ: Правильный индекс квартала
                for (int qx = 0; qx < 4; qx++) {
                    for (int qy = 0; qy < 4; qy++) {
                        for (int qz = 0; qz < 4; qz++) {

                            int blockX = chunkBlockX + (qx << 2) + 2;
                            int blockZ = chunkBlockZ + (qz << 2) + 2;

                            double dx = blockX - centerX;
                            double dz = blockZ - centerZ;
                            double distanceSq = dx * dx + dz * dz;

                            Holder<Biome> biomeToSet = null;

                            if (distanceSq <= zone3RadiusSq) {
                                biomeToSet = innerCrater;
                                innerCount++;
                            } else if (distanceSq <= zone4RadiusSq) {
                                biomeToSet = outerCrater;
                                outerCount++;
                            }

                            if (biomeToSet != null) {
                                // ✅ ПРАВИЛЬНЫЙ ИНДЕКС: (qy << 4) | (qz << 2) | qx
                                setBiomeViaReflection(biomesContainer, qx, qy, qz, biomeToSet);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println(LOG_PREFIX + " ⚠️ Error in section: " + e.getMessage());
            }
        }

        return new int[]{innerCount, outerCount};
    }

    /**
     * 🔧 Устанавливает биом через reflection
     * ✅ ИСПРАВЛЕНО: Правильный расчет индекса (qy, qz, qx)
     */
    private static void setBiomeViaReflection(Object paletteContainer,
                                              int x, int y, int z,
                                              Holder<Biome> biome) {
        try {
            // ✅ ПРАВИЛЬНЫЙ ИНДЕКС для PalettedContainer биомов
            // Формула: index = (y << 4) | (z << 2) | x
            // Эквивалентно: index = y * 16 + z * 4 + x
            int index = (y << 4) | (z << 2) | x;

            Method setMethod = paletteContainer.getClass()
                    .getDeclaredMethod("set", int.class, Object.class);

            setMethod.setAccessible(true);
            setMethod.invoke(paletteContainer, index, biome);

        } catch (NoSuchMethodException e) {
            System.err.println(LOG_PREFIX + " Warning: set() method not found (version difference?)");

        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " ⚠️ Error during reflection: " + e.getClass().getSimpleName());
        }
    }
}