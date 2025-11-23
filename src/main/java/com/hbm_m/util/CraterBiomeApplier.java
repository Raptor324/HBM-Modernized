package com.hbm_m.util;

import com.hbm_m.world.biome.ModBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * ✅ ИСПРАВЛЕННАЯ ВЕРСИЯ 2 - ЦЕНТР РАВЕН ЦЕНТРУ БОМБЫ
 *
 * ИСПРАВЛЕНИЯ:
 * ✅ Использует переданный centerPos напрямую - это позиция блока бомбы
 * ✅ Правильное вычисление координат кварталов относительно переданного центра
 * ✅ Корректная сохранение изменённых биомов в секции
 * ✅ Полная диагностика с логированием центра
 */

public class CraterBiomeApplier {

    // Радиусы из CraterGenerator для соответствия
    private static final int INNER_CRATER_RADIUS = 190;
    private static final int OUTER_CRATER_RADIUS = 260;

    /**
     * Main method - applies biomes to crater
     * centerPos - это позиция БЛОКА БОМБЫ, используем её как центр
     */
    public static void applyCraterBiomes(ServerLevel level, BlockPos centerPos, int radius) {
        long startTime = System.currentTimeMillis();
        System.out.println("[CRATER_BIOME] ========================================");
        System.out.println("[CRATER_BIOME] START: Applying crater biomes...");
        System.out.println("[CRATER_BIOME] Bomb block center: " + centerPos);
        System.out.println("[CRATER_BIOME] Center X: " + centerPos.getX());
        System.out.println("[CRATER_BIOME] Center Z: " + centerPos.getZ());
        System.out.println("[CRATER_BIOME] Radius: " + radius + " blocks");
        System.out.println("[CRATER_BIOME] ========================================");

        try {
            // Get biomes from registry
            System.out.println("[CRATER_BIOME] Searching for biomes in registry...");
            Holder innerCrater = getBiomeHolder(level, ModBiomes.INNER_CRATER_KEY);
            Holder outerCrater = getBiomeHolder(level, ModBiomes.OUTER_CRATER_KEY);

            if (innerCrater == null || outerCrater == null) {
                System.err.println("[CRATER_BIOME] ERROR: One or both biomes NOT found in registry!");
                System.err.println("[CRATER_BIOME] Inner Crater: " + (innerCrater != null ? "OK" : "NULL"));
                System.err.println("[CRATER_BIOME] Outer Crater: " + (outerCrater != null ? "OK" : "NULL"));
                return;
            }

            System.out.println("[CRATER_BIOME] SUCCESS: Both biomes found!");

            // Calculate area of biome application
            // ✅ Используем именно переданный centerPos
            int appliedRadius = Math.max(OUTER_CRATER_RADIUS, radius) + 32;
            int minChunkX = (centerPos.getX() - appliedRadius) >> 4;
            int maxChunkX = (centerPos.getX() + appliedRadius) >> 4;
            int minChunkZ = (centerPos.getZ() - appliedRadius) >> 4;
            int maxChunkZ = (centerPos.getZ() + appliedRadius) >> 4;

            int totalChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
            System.out.println("[CRATER_BIOME] Processing chunks: " + totalChunks);
            System.out.println("[CRATER_BIOME] Chunk range X: " + minChunkX + " to " + maxChunkX);
            System.out.println("[CRATER_BIOME] Chunk range Z: " + minChunkZ + " to " + maxChunkZ);

            int successfulChunks = 0;
            int failedChunks = 0;
            int appliedBiomes = 0;

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    try {
                        ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                        if (chunk == null) {
                            failedChunks++;
                            continue;
                        }

                        int resultBiomes = applyBiomesToChunk(chunk, centerPos, innerCrater, outerCrater);
                        appliedBiomes += resultBiomes;
                        chunk.setUnsaved(true);
                        successfulChunks++;

                    } catch (Exception e) {
                        System.err.println("[CRATER_BIOME] ERROR: Chunk [" + chunkX + ", " + chunkZ + "] failed: " + e.getMessage());
                        e.printStackTrace();
                        failedChunks++;
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            System.out.println("[CRATER_BIOME] ========================================");
            System.out.println("[CRATER_BIOME] COMPLETE!");
            System.out.println("[CRATER_BIOME] Processed: " + successfulChunks + " / " + totalChunks);
            System.out.println("[CRATER_BIOME] Failed: " + failedChunks);
            System.out.println("[CRATER_BIOME] Biomes applied to: " + appliedBiomes + " quarters");
            System.out.println("[CRATER_BIOME] Time: " + (endTime - startTime) + " ms");
            System.out.println("[CRATER_BIOME] ========================================");

        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] CRITICAL ERROR:");
            e.printStackTrace();
        }
    }

    /**
     * Get biome holder from registry
     */
    private static Holder getBiomeHolder(ServerLevel level, net.minecraft.resources.ResourceKey key) {
        try {
            var result = level.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(key);
            System.out.println("[CRATER_BIOME] Found: " + key.location());
            return result;
        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] WARNING: Cannot find biome: " + key.location());
            System.err.println("[CRATER_BIOME] " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ ИСПРАВЛЕННЫЙ МЕТОД V2 - ЦЕНТР РАВЕН ПЕРЕДАННОМУ CENTERPOS
     *
     * КЛЮЧЕВЫЕ ИСПРАВЛЕНИЯ:
     * 1. Вычисляем реальные координаты каждого квартала ОТНОСИТЕЛЬНО ПЕРЕДАННОГО centerPos
     * 2. centerPos - это позиция блока бомбы, используем его напрямую
     * 3. Правильно считаем расстояние от центра бомбы
     * 4. Сохраняем изменённые биомы обратно в секцию
     */
    private static int applyBiomesToChunk(ChunkAccess chunk,
                                          BlockPos centerPos,
                                          Holder innerCrater,
                                          Holder outerCrater) {
        try {
            // ✅ centerPos - это координаты блока бомбы, используем их как центр кратера
            int centerX = centerPos.getX();
            int centerZ = centerPos.getZ();
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            int chunkBlockX = chunkX << 4;  // чанк * 16
            int chunkBlockZ = chunkZ << 4;

            LevelChunkSection[] sections = chunk.getSections();
            if (sections == null) return 0;

            int appliedBiomesCount = 0;
            boolean chunkHasChanges = false;

            // Process each section
            for (LevelChunkSection section : sections) {
                if (section == null) continue;

                // Get read-only container
                var biomesRO = section.getBiomes();
                if (biomesRO == null) continue;

                // Create writeable copy - это работает на 1.20.1
                var biomes = biomesRO.recreate();
                boolean sectionChanged = false;

                // ✅ Правильный расчёт координат кварталов
                // В одном чанке 16x16 блоков, т.е. 4x4 квартала по X и Z
                // Каждый квартал занимает 4x4 блока

                for (int qx = 0; qx < 4; qx++) {
                    for (int qy = 0; qy < 4; qy++) {
                        for (int qz = 0; qz < 4; qz++) {
                            // ✅ Вычисляем центр квартала правильно
                            // qx,qz - номер квартала (0-3)
                            // Каждый квартал покрывает 4 блока
                            // Центр квартала находится в середине его 4x4 блоков
                            int blockX = chunkBlockX + (qx << 2) + 2;  // +2 = центр 4 блоков
                            int blockZ = chunkBlockZ + (qz << 2) + 2;

                            // Считаем расстояние от ЦЕНТРА БОМБЫ (переданный centerPos)
                            double dx = blockX - centerX;
                            double dz = blockZ - centerZ;
                            double distance = Math.sqrt(dx * dx + dz * dz);

                            // Выбираем биом в зависимости от расстояния
                            Holder biomeToSet = null;

                            if (distance <= INNER_CRATER_RADIUS) {
                                biomeToSet = innerCrater;
                                sectionChanged = true;
                            } else if (distance <= OUTER_CRATER_RADIUS) {
                                biomeToSet = outerCrater;
                                sectionChanged = true;
                            }

                            // Применяем биом
                            if (biomeToSet != null) {
                                biomes.set(qx, qy, qz, biomeToSet);
                                appliedBiomesCount++;
                            }
                        }
                    }
                }

                // ✅ Сохраняем изменённые биомы обратно в секцию
                if (sectionChanged) {
                    chunkHasChanges = true;
                    saveBiomesToSection(section, biomes);
                }
            }

            return appliedBiomesCount;

        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] ERROR in applyBiomesToChunk: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * ✅ Сохраняет изменённые биомы в секцию
     * Использует reflection для обхода приватного поля
     */
    private static void saveBiomesToSection(LevelChunkSection section, PalettedContainer<Holder<Biome>> biomes) {
        try {
            // Попытка 1: Найти и установить поле "biomes"
            try {
                var field = LevelChunkSection.class.getDeclaredField("biomes");
                field.setAccessible(true);
                field.set(section, biomes);
                System.out.println("[CRATER_BIOME] ✅ Biomes saved via 'biomes' field");
                return;
            } catch (NoSuchFieldException ex1) {
                // Попробуем другие имена
            }

            // Попытка 2: Поиск по типу поля
            var fields = LevelChunkSection.class.getDeclaredFields();
            for (var f : fields) {
                String fullTypeName = f.getType().getTypeName();

                // Ищем PaletteContainer<Biome> или похожее
                if (fullTypeName.contains("PaletteContainer") &&
                        fullTypeName.contains("Biome")) {
                    f.setAccessible(true);
                    f.set(section, biomes);
                    System.out.println("[CRATER_BIOME] ✅ Biomes saved via field: " + f.getName());
                    return;
                }
            }

            System.err.println("[CRATER_BIOME] ⚠️ WARNING: Could not find biome field in LevelChunkSection");
            System.err.println("[CRATER_BIOME] Available fields:");
            for (var f : fields) {
                System.err.println("[CRATER_BIOME]   - " + f.getName() + " (" + f.getType().getSimpleName() + ")");
            }

        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] ERROR: Could not save biomes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}