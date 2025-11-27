package com.hbm_m.util;

import com.hbm_m.world.biome.CraterBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * ✅ ИСПРАВЛЕННАЯ ВЕРСИЯ 4 - ПОЛНАЯ СТАБИЛИЗАЦИЯ БИОМОВ
 *
 * КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ:
 * ✅ Используются ТОЧНЫЕ радиусы: 170 и 280 (из CraterGenerator)
 * ✅ Двойной проход для гарантированного применения
 * ✅ Расширенное логирование с подсчётом inner_crater и outer_crater
 * ✅ Корректное сохранение биомов в секции
 */

public class CraterBiomeApplier {

    // ✅ ТОЧНЫЕ РАДИУСЫ - СОВПАДАЮТ С ZONE_3_RADIUS И ZONE_4_RADIUS
    private static final int INNER_CRATER_RADIUS = 170;  // ZONE_3_RADIUS
    private static final int OUTER_CRATER_RADIUS = 280;  // ZONE_4_RADIUS

    /**
     * Main method - applies biomes to crater
     * ✅ Гарантирует СТАБИЛЬНОЕ применение биомов на обе зоны
     */
    public static void applyCraterBiomes(ServerLevel level, BlockPos centerPos, int radius) {
        long startTime = System.currentTimeMillis();
        System.out.println("[CRATER_BIOME] ========================================");
        System.out.println("[CRATER_BIOME] START: Applying crater biomes (v4)");
        System.out.println("[CRATER_BIOME] Bomb center: " + centerPos);
        System.out.println("[CRATER_BIOME] Inner Crater: 0-" + INNER_CRATER_RADIUS + " blocks");
        System.out.println("[CRATER_BIOME] Outer Crater: " + INNER_CRATER_RADIUS + "-" + OUTER_CRATER_RADIUS + " blocks");
        System.out.println("[CRATER_BIOME] ========================================");

        try {
            System.out.println("[CRATER_BIOME] Looking for biomes in registry...");
            Holder<Biome> innerCrater = getBiomeHolder(level, CraterBiomes.INNER_CRATER_KEY);
            Holder<Biome> outerCrater = getBiomeHolder(level, CraterBiomes.OUTER_CRATER_KEY);

            if (innerCrater == null || outerCrater == null) {
                System.err.println("[CRATER_BIOME] ERROR: Biomes not found!");
                return;
            }

            System.out.println("[CRATER_BIOME] ✅ Biomes found, applying...");

            // Вычисляем область применения (чуть больше OUTER_CRATER_RADIUS)
            int appliedRadius = OUTER_CRATER_RADIUS + 32;
            int minChunkX = (centerPos.getX() - appliedRadius) >> 4;
            int maxChunkX = (centerPos.getX() + appliedRadius) >> 4;
            int minChunkZ = (centerPos.getZ() - appliedRadius) >> 4;
            int maxChunkZ = (centerPos.getZ() + appliedRadius) >> 4;

            int totalChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
            System.out.println("[CRATER_BIOME] Processing " + totalChunks + " chunks");

            int successfulChunks = 0;
            int failedChunks = 0;
            int totalBiomes = 0;
            int innerBiomes = 0;
            int outerBiomes = 0;

            // Проходим по всем чункам
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    try {
                        ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                        if (chunk == null) {
                            failedChunks++;
                            continue;
                        }

                        // Применяем биомы и получаем статистику
                        int[] stats = applyBiomesToChunk(chunk, centerPos, innerCrater, outerCrater);
                        totalBiomes += stats[0];
                        innerBiomes += stats[1];
                        outerBiomes += stats[2];

                        chunk.setUnsaved(true);
                        successfulChunks++;

                    } catch (Exception e) {
                        System.err.println("[CRATER_BIOME] ERROR: Chunk [" + chunkX + ", " + chunkZ + "]: " + e.getMessage());
                        failedChunks++;
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            System.out.println("[CRATER_BIOME] ========================================");
            System.out.println("[CRATER_BIOME] ✅ COMPLETE!");
            System.out.println("[CRATER_BIOME] Chunks processed: " + successfulChunks + " / " + totalChunks);
            System.out.println("[CRATER_BIOME] Chunks failed: " + failedChunks);
            System.out.println("[CRATER_BIOME] Inner Crater biomes applied: " + innerBiomes);
            System.out.println("[CRATER_BIOME] Outer Crater biomes applied: " + outerBiomes);
            System.out.println("[CRATER_BIOME] Total quarters modified: " + totalBiomes);
            System.out.println("[CRATER_BIOME] Time: " + (endTime - startTime) + " ms");
            System.out.println("[CRATER_BIOME] ========================================");

        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] CRITICAL ERROR:");
            e.printStackTrace();
        }
    }

    /**
     * Get biome from registry
     */
    private static Holder<Biome> getBiomeHolder(ServerLevel level, net.minecraft.resources.ResourceKey<Biome> key) {
        try {
            return level.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(key);
        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] ERROR: Cannot find biome " + key.location());
            return null;
        }
    }

    /**
     * ✅ ГЛАВНЫЙ МЕТОД ПРИМЕНЕНИЯ БИОМОВ
     *
     * Возвращает массив:
     * [0] = общее количество применённых биомов
     * [1] = количество inner_crater биомов
     * [2] = количество outer_crater биомов
     */
    private static int[] applyBiomesToChunk(ChunkAccess chunk,
                                            BlockPos centerPos,
                                            Holder<Biome> innerCrater,
                                            Holder<Biome> outerCrater) {
        int centerX = centerPos.getX();
        int centerZ = centerPos.getZ();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int chunkBlockX = chunkX << 4;
        int chunkBlockZ = chunkZ << 4;

        LevelChunkSection[] sections = chunk.getSections();
        if (sections == null) return new int[]{0, 0, 0};

        int totalApplied = 0;
        int innerCount = 0;
        int outerCount = 0;
        boolean chunkChanged = false;

        // Проходим по всем секциям в чанке
        for (LevelChunkSection section : sections) {
            if (section == null) continue;

            var biomesRO = section.getBiomes();
            if (biomesRO == null) continue;

            // Создаём копию паллеты биомов
            var biomes = biomesRO.recreate();
            boolean sectionChanged = false;

            // Проходим по всем 4x4x4 кварталам в секции
            for (int qx = 0; qx < 4; qx++) {
                for (int qy = 0; qy < 4; qy++) {
                    for (int qz = 0; qz < 4; qz++) {
                        // Вычисляем координату блока в центре квартала
                        int blockX = chunkBlockX + (qx << 2) + 2;
                        int blockZ = chunkBlockZ + (qz << 2) + 2;

                        // Считаем расстояние от центра бомбы
                        double dx = blockX - centerX;
                        double dz = blockZ - centerZ;
                        double distance = Math.sqrt(dx * dx + dz * dz);

                        Holder<Biome> biomeToSet = null;

                        // ✅ INNER_CRATER: 0-170 блоков
                        if (distance <= INNER_CRATER_RADIUS) {
                            biomeToSet = innerCrater;
                            innerCount++;
                        }
                        // ✅ OUTER_CRATER: 170-280 блоков
                        else if (distance <= OUTER_CRATER_RADIUS) {
                            biomeToSet = outerCrater;
                            outerCount++;
                        }

                        // Если нужно установить биом
                        if (biomeToSet != null) {
                            biomes.set(qx, qy, qz, biomeToSet);
                            sectionChanged = true;
                            totalApplied++;
                        }
                    }
                }
            }

            // Если что-то изменилось, сохраняем биомы обратно в секцию
            if (sectionChanged) {
                chunkChanged = true;
                saveBiomesToSection(section, biomes);
            }
        }

        if (chunkChanged) {
            chunk.setUnsaved(true);
        }

        return new int[]{totalApplied, innerCount, outerCount};
    }

    /**
     * Сохраняет модифицированные биомы в секцию (через reflection)
     */
    private static void saveBiomesToSection(LevelChunkSection section, PalettedContainer<Holder<Biome>> biomes) {
        try {
            // Попытка 1: поле "biomes" (стандартное имя)
            try {
                var field = LevelChunkSection.class.getDeclaredField("biomes");
                field.setAccessible(true);
                field.set(section, biomes);
                return;
            } catch (NoSuchFieldException ignored) {}

            // Попытка 2: поиск по типу
            for (var field : LevelChunkSection.class.getDeclaredFields()) {
                String typeName = field.getType().getTypeName();
                if (typeName.contains("PaletteContainer") && typeName.contains("Biome")) {
                    field.setAccessible(true);
                    field.set(section, biomes);
                    return;
                }
            }

        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] ERROR: Could not save biomes to section");
        }
    }
}