package com.hbm_m.util.explosions.nuclear;

import com.hbm_m.world.biome.ModBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

/**
 * Crater Biome Helper v7.0 - AIR CHECK FIX
 * - FIXED: Removed hasOnlyAir() check - now biomes apply to empty crater space.
 * - FIXED: "Only at border" bug resolved.
 * - RESTORED: Gradient zones for smooth transitions.
 * - KEPT: Instant application logic (no batching).
 */
public class CraterBiomeHelper {

    // Wave parameters - must match CraterGenerator
    private static final double ZONE_NOISE_SCALE = 0.15;
    private static final double ZONE_NOISE_STRENGTH = 0.25;

    // Gradient parameters
    private static final double GRADIENT_MULTIPLIER = 1.15;  // 15% soft transition

    public static void applyBiomesAsync(ServerLevel level, BlockPos center, double zone3Radius, double zone4Radius) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            System.err.println("[CraterBiomeHelper] ERROR: No MinecraftServer!");
            return;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("[CraterBiomeHelper] START: Applying crater biomes v7 (full coverage)");

        // Biomes from registry
        var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);

        // FIX: Change type to Holder.Reference
        Optional<Holder.Reference<Biome>> innerOpt = biomeRegistry.getHolder(ModBiomes.INNER_CRATER_KEY);
        Optional<Holder.Reference<Biome>> outerOpt = biomeRegistry.getHolder(ModBiomes.OUTER_CRATER_KEY);

        if (innerOpt.isEmpty() || outerOpt.isEmpty()) {
            System.err.println("[CraterBiomeHelper] ERROR: Biomes not registered!");
            return;
        }

        Holder<Biome> innerBiome = innerOpt.get();
        Holder<Biome> outerBiome = outerOpt.get();

        int centerX = center.getX();
        int centerZ = center.getZ();

        // Chunk bounds around crater
        int radiusBlocks = (int) Math.ceil(zone4Radius) + 40;
        int minChunkX = (centerX - radiusBlocks) >> 4;
        int maxChunkX = (centerX + radiusBlocks) >> 4;
        int minChunkZ = (centerZ - radiusBlocks) >> 4;
        int maxChunkZ = (centerZ + radiusBlocks) >> 4;

        Set<LevelChunk> modifiedChunks = Collections.synchronizedSet(new HashSet<>());
        int totalChunks = 0;
        int modifiedCount = 0;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                totalChunks++;
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk != null && !chunk.isEmpty()) {
                    if (applyBiomesToChunkFixed(chunk, center, zone3Radius, zone4Radius, innerBiome, outerBiome, level)) {
                        modifiedCount++;
                        modifiedChunks.add(chunk);
                    }
                }
            }
        }

        sendAllChunkUpdatesImmediate(level, modifiedChunks);

        long endTime = System.currentTimeMillis();
        System.out.println("[CraterBiomeHelper] COMPLETE: Modified " + modifiedCount + "/" + totalChunks +
                " chunks in " + (endTime - startTime) + " ms");
    }


    // === Noise, must match CraterGenerator ===

    private static double getZoneWaveNoise(double x, double z) {
        double wave1 = Math.sin(x * 0.1) * Math.cos(z * 0.1) * 0.5;
        double wave2 = Math.sin(x * 0.05 + z * 0.08) * 0.3;
        double wave3 = Math.cos(x * 0.15 - z * 0.12) * 0.2;
        return (wave1 + wave2 + wave3) / 2.0;
    }

    private static double getZoneRadiusWithNoise(double baseRadius, double centerX, double centerZ, int x, int z) {
        double relX = (x - centerX) * ZONE_NOISE_SCALE;
        double relZ = (z - centerZ) * ZONE_NOISE_SCALE;
        double noise = getZoneWaveNoise(relX, relZ);
        double noiseInfluence = 1.0 + (noise * ZONE_NOISE_STRENGTH);
        return baseRadius * noiseInfluence;
    }

    @SuppressWarnings("unchecked")
    private static boolean applyBiomesToChunkFixed(LevelChunk chunk, BlockPos center,
                                                   double zone3Radius, double baseZone4Radius, // baseZone4Radius - это физическая зона разрушения
                                                   Holder<Biome> innerBiome, Holder<Biome> outerBiome,
                                                   ServerLevel level) {
        boolean modified = false;
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int chunkBlockStartX = chunkX << 4;
        int chunkBlockStartZ = chunkZ << 4;
        int centerX = center.getX();
        int centerZ = center.getZ();

        // 🔥 РАСШИРЕНИЕ БИОМА: Зона биома будет больше зоны разрушения на 40 блоков
        double zone4BiomeRadius = baseZone4Radius + 40.0;

        double gradientR3 = zone3Radius * GRADIENT_MULTIPLIER;
        double gradientR4 = zone4BiomeRadius * GRADIENT_MULTIPLIER; // Градиент тоже сдвигается

        int minSection = level.getMinSection();
        int maxSection = level.getMaxSection();

        for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
            int sectionIndex = sectionY - minSection;
            if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) continue;
            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (section == null) continue;

            try {
                PalettedContainer<Holder<Biome>> biomeContainer = (PalettedContainer<Holder<Biome>>) section.getBiomes();

                for (int qx = 0; qx < 4; qx++) {
                    for (int qz = 0; qz < 4; qz++) {
                        int blockX = chunkBlockStartX + (qx << 2) + 2;
                        int blockZ = chunkBlockStartZ + (qz << 2) + 2;

                        double dx = blockX - centerX;
                        double dz = blockZ - centerZ;
                        double dist2D = Math.sqrt(dx * dx + dz * dz);

                        // Wavy radii (используем расширенный радиус для зоны 4)
                        double currentR3 = getZoneRadiusWithNoise(zone3Radius, centerX, centerZ, blockX, blockZ);
                        double gradientR3Wavy = getZoneRadiusWithNoise(gradientR3, centerX, centerZ, blockX, blockZ);

                        double currentR4 = getZoneRadiusWithNoise(zone4BiomeRadius, centerX, centerZ, blockX, blockZ);
                        double gradientR4Wavy = getZoneRadiusWithNoise(gradientR4, centerX, centerZ, blockX, blockZ);

                        Holder<Biome> targetBiome = null;

                        // Dithering noise
                        double dither = ((blockX & 3) * 0.25 + (blockZ & 3) * 0.25) * 0.2 - 0.1;

                        if (dist2D <= currentR3) {
                            targetBiome = innerBiome;
                        } else if (dist2D <= gradientR3Wavy) {
                            double factor = (dist2D - currentR3) / (gradientR3Wavy - currentR3);
                            targetBiome = (factor + dither) < 0.5 ? innerBiome : outerBiome;
                        } else if (dist2D <= currentR4) {
                            targetBiome = outerBiome;
                        } else if (dist2D <= gradientR4Wavy) {
                            double factor = (dist2D - currentR4) / (gradientR4Wavy - currentR4);
                            targetBiome = (factor + dither) < 0.5 ? outerBiome : null;
                        }

                        if (targetBiome != null) {
                            for (int qy = 0; qy < 4; qy++) {
                                biomeContainer.set(qx, qy, qz, targetBiome);
                                modified = true;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        if (modified) chunk.setUnsaved(true);
        return modified;
    }



    // === Sending updates ===

    private static void sendAllChunkUpdatesImmediate(ServerLevel level, Set<LevelChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }

        for (LevelChunk chunk : chunks) {
            try {
                ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                        chunk,
                        level.getLightEngine(),
                        null,
                        null
                );

                var players = level.getServer().getPlayerList().getPlayers();
                for (var player : players) {
                    player.connection.send(packet);
                }
            } catch (Exception e) {
                System.err.println("[CraterBiomeHelper] Failed to send chunk update for " + chunk.getPos());
                e.printStackTrace();
            }
        }
    }
}
