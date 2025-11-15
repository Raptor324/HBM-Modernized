package com.hbm_m.util;

import com.hbm_m.world.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class CraterBiomeApplier {

    // Должно совпадать с логикой CraterGenerator
    private static final int CRATER_RADIUS = 150;   // Inner Crater
    private static final int ZONE_4_RADIUS = 200;   // Outer Crater внешний радиус

    /**
     * Применяет биомы кратера в указанной области.
     * Вызывать с серверного треда (у тебя это уже так в CraterGenerator).
     */
    public static void applyCraterBiomes(ServerLevel level, BlockPos centerPos, int radius) {
        try {
            // Берём Holder<Biome> из реестра по ResourceKey
            Holder<Biome> innerCraterHolder = getBiomeHolder(level, ModBiomes.INNER_CRATER_KEY);
            Holder<Biome> outerCraterHolder = getBiomeHolder(level, ModBiomes.OUTER_CRATER_KEY);

            int extendedRadius = radius + 32;

            int minChunkX = (centerPos.getX() - extendedRadius) >> 4;
            int maxChunkX = (centerPos.getX() + extendedRadius) >> 4;
            int minChunkZ = (centerPos.getZ() - extendedRadius) >> 4;
            int maxChunkZ = (centerPos.getZ() + extendedRadius) >> 4;

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                    applyBiomesToChunk(chunk, centerPos, innerCraterHolder, outerCraterHolder);
                }
            }

            System.out.println("[CRATER_BIOME] Биомы успешно применены в радиусе " + radius + " блоков");
        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] Ошибка при применении биомов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Holder<Biome> getBiomeHolder(ServerLevel level, ResourceKey<Biome> key) {
        return level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(key);
    }

    /**
     * Применяет Inner / Outer Crater к одному чанку.
     * Биомы выставляются для всех секций чанка по горизонтальному расстоянию от центра кратера.
     */
    private static void applyBiomesToChunk(ChunkAccess chunk,
                                           BlockPos centerPos,
                                           Holder<Biome> innerCraterHolder,
                                           Holder<Biome> outerCraterHolder) {
        try {

            // Проходим по всем секциям чанка
            LevelChunkSection[] sections = chunk.getSections();
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;

// quart-координаты начала чанка по X/Z: (chunk << 4) >> 2 == (chunk << 2)
            int quartBaseXChunk = (chunkX << 2);
            int quartBaseZChunk = (chunkZ << 2);

            int minBuildY = chunk.getMinBuildHeight(); // есть у ChunkAccess

            for (int secIdx = 0; secIdx < sections.length; secIdx++) {
                LevelChunkSection section = sections[secIdx];
                if (section == null) continue;

                int sectionBottomY = minBuildY + (secIdx * 16);
                int quartBaseY = (sectionBottomY >> 2);
                int quartBaseX = quartBaseXChunk;
                int quartBaseZ = quartBaseZChunk;

                // BiomeResolver, который применяет твои Inner/Outer и сохраняет прочее
                section.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
                    int localX = qx - quartBaseX; // 0..3
                    int localY = qy - quartBaseY; // 0..3
                    int localZ = qz - quartBaseZ; // 0..3

                    // Переводим quart -> block (умножить на 4)
                    int bx = qx << 2;
                    int bz = qz << 2;

                    double dx = bx - centerPos.getX();
                    double dz = bz - centerPos.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    Holder<Biome> chosen =
                            dist <= CRATER_RADIUS ? innerCraterHolder :
                                    (dist <= ZONE_4_RADIUS ? outerCraterHolder : null);

                    if (chosen != null) {
                        return chosen;
                    } else {
                        // вернуть исходный биом секции по локальным координатам 0..3
                        int lx = Math.max(0, Math.min(3, localX));
                        int ly = Math.max(0, Math.min(3, localY));
                        int lz = Math.max(0, Math.min(3, localZ));
                        return section.getNoiseBiome(lx, ly, lz);
                    }
                }, null, quartBaseX, quartBaseY, quartBaseZ);
            }

        } catch (Exception e) {
            System.err.println("[CRATER_BIOME] Ошибка при применении биомов к чанку: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Выбор биома по расстоянию: Inner до CRATER_RADIUS, Outer до ZONE_4_RADIUS, дальше не трогаем.
     */
    private static Holder<Biome> selectBiomeForDistance(double distance,
                                                        Holder<Biome> innerCraterHolder,
                                                        Holder<Biome> outerCraterHolder) {
        if (distance <= CRATER_RADIUS) {
            return innerCraterHolder;
        } else if (distance <= ZONE_4_RADIUS) {
            return outerCraterHolder;
        }
        return null;
    }

    /**
     * Оставил для совместимости – сейчас просто прокси на основной метод.
     */
    public static void applyCraterBiomesWithGradient(ServerLevel level, BlockPos centerPos, int radius) {
        applyCraterBiomes(level, centerPos, radius);
    }
}
