package com.hbm_m.util;

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
 * Crater Biome Helper v4.0 - COMPLETE REWRITE
 * ✅ Instant application without batching
 * ✅ Seamless wavy borders synchronized with damage zones
 * ✅ No visual artifacts or holes in biome coverage
 */
public class CraterBiomeHelper {

    // Wave parameters - SYNCHRONIZED with CraterGenerator
    private static final double ZONE_NOISE_SCALE = 0.15;
    private static final double ZONE_NOISE_STRENGTH = 0.25;

    public static void applyBiomesAsync(ServerLevel level, BlockPos center, double zone3Radius, double zone4Radius) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            System.err.println("[CraterBiomeHelper] ERROR: No MinecraftServer!");
            return;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("[CraterBiomeHelper] START: Applying biomes instantly");
        System.out.println("[CraterBiomeHelper] Zone 3 radius: " + (int)zone3Radius + "m");
        System.out.println("[CraterBiomeHelper] Zone 4 radius: " + (int)zone4Radius + "m");

        // Get biomes from registry
        var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);

        Optional<Holder.Reference<Biome>> innerOpt = biomeRegistry.getHolder(ModBiomes.INNER_CRATER_KEY);
        Optional<Holder.Reference<Biome>> outerOpt = biomeRegistry.getHolder(ModBiomes.OUTER_CRATER_KEY);

        if (innerOpt.isEmpty() || outerOpt.isEmpty()) {
            System.err.println("[CraterBiomeHelper] ERROR: Biomes not registered!");
            return;
        }

        Holder<Biome> innerBiome = innerOpt.get();
        Holder<Biome> outerBiome = outerOpt.get();

        // Calculate affected area with padding
        int radiusBlocks = (int) Math.ceil(zone4Radius) + 30;
        int minChunkX = (center.getX() - radiusBlocks) >> 4;
        int maxChunkX = (center.getX() + radiusBlocks) >> 4;
        int minChunkZ = (center.getZ() - radiusBlocks) >> 4;
        int maxChunkZ = (center.getZ() + radiusBlocks) >> 4;

        // Collect all affected chunks
        Set<LevelChunk> affectedChunks = new HashSet<>();
        int totalChunks = 0;
        int modifiedChunks = 0;

        // Process all chunks + neighbors for seamless edges
        Set<ChunkPos> chunksToProcess = new HashSet<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunksToProcess.add(new ChunkPos(cx, cz));
                // Add neighbors for seamless transitions
                chunksToProcess.add(new ChunkPos(cx - 1, cz));
                chunksToProcess.add(new ChunkPos(cx + 1, cz));
                chunksToProcess.add(new ChunkPos(cx, cz - 1));
                chunksToProcess.add(new ChunkPos(cx, cz + 1));
            }
        }

        // MAIN PROCESSING LOOP - All at once, no batching
        for (ChunkPos chunkPos : chunksToProcess) {
            totalChunks++;
            LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);

            if (chunk != null && !chunk.isEmpty()) {
                if (applyBiomesToChunk(chunk, center, zone3Radius, zone4Radius, innerBiome, outerBiome, level)) {
                    modifiedChunks++;
                    affectedChunks.add(chunk);
                }
            }
        }

        // Send ALL updates in one batch
        sendAllChunkUpdates(level, affectedChunks);

        long endTime = System.currentTimeMillis();
        System.out.println("[CraterBiomeHelper] COMPLETE! Modified " + modifiedChunks + "/" + totalChunks + " chunks in " + (endTime - startTime) + "ms");
    }

    /**
     * Wave noise function - SYNCHRONIZED with CraterGenerator
     */
    private static double getZoneWaveNoise(double x, double z) {
        double wave1 = Math.sin(x * 0.1) * Math.cos(z * 0.1) * 0.5;
        double wave2 = Math.sin(x * 0.05 + z * 0.08) * 0.3;
        double wave3 = Math.cos(x * 0.15 - z * 0.12) * 0.2;
        return (wave1 + wave2 + wave3) / 2.0;
    }

    /**
     * Calculate wavy radius at specific point - SYNCHRONIZED with CraterGenerator
     */
    private static double getZoneRadiusWithNoise(double baseRadius, double centerX, double centerZ, int x, int z) {
        double relX = (x - centerX) * ZONE_NOISE_SCALE;
        double relZ = (z - centerZ) * ZONE_NOISE_SCALE;

        double noise = getZoneWaveNoise(relX, relZ);
        double noiseInfluence = 1.0 + (noise * ZONE_NOISE_STRENGTH);
        return baseRadius * noiseInfluence;
    }

    /**
     * Apply biomes to a single chunk - NO BATCHING
     */
    @SuppressWarnings("unchecked")
    private static boolean applyBiomesToChunk(LevelChunk chunk, BlockPos center,
                                              double zone3Radius, double zone4Radius,
                                              Holder<Biome> innerBiome, Holder<Biome> outerBiome,
                                              ServerLevel level) {
        boolean modified = false;

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int chunkBlockStartX = chunkX << 4;
        int chunkBlockStartZ = chunkZ << 4;

        int centerX = center.getX();
        int centerZ = center.getZ();
        int centerY = center.getY();

        double baseR3 = zone3Radius;
        double baseR4 = zone4Radius;

        // Iterate through all sections
        int minSection = level.getMinSection();
        int maxSection = level.getMaxSection();

        for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
            int sectionIndex = sectionY - minSection;
            if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) continue;

            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (section == null || section.hasOnlyAir()) continue;

            int sectionBlockStartY = sectionY << 4;

            try {
                // Get biome container with proper casting
                PalettedContainer<Holder<Biome>> biomeContainer =
                        (PalettedContainer<Holder<Biome>>) section.getBiomes();

                // Iterate through all biome quarts (4x4x4 blocks)
                for (int qx = 0; qx < 4; qx++) {
                    for (int qz = 0; qz < 4; qz++) {
                        for (int qy = 0; qy < 4; qy++) {
                            // Calculate block coordinates at quart center
                            int blockX = chunkBlockStartX + (qx << 2) + 2;
                            int blockY = sectionBlockStartY + (qy << 2) + 2;
                            int blockZ = chunkBlockStartZ + (qz << 2) + 2;

                            // Calculate wavy radii for this exact point
                            double currentR3 = getZoneRadiusWithNoise(baseR3, centerX, centerZ, blockX, blockZ);
                            double currentR4 = getZoneRadiusWithNoise(baseR4, centerX, centerZ, blockX, blockZ);

                            // Calculate distance
                            double dx = blockX - centerX;
                            double dy = blockY - centerY;
                            double dz = blockZ - centerZ;
                            double distSq = dx * dx + dy * dy + dz * dz;
                            double dist = Math.sqrt(distSq);

                            // Determine target biome
                            Holder<Biome> targetBiome = null;

                            if (dist <= currentR3) {
                                targetBiome = innerBiome;
                            } else if (dist <= currentR4) {
                                targetBiome = outerBiome;
                            }

                            // Apply biome if in zone
                            if (targetBiome != null) {
                                try {
                                    biomeContainer.set(qx, qy, qz, targetBiome);
                                    modified = true;
                                } catch (Exception e) {
                                    System.err.println("[CraterBiomeHelper] Failed to set biome at " + blockX + "," + blockY + "," + blockZ + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (ClassCastException e) {
                System.err.println("[CraterBiomeHelper] ClassCastException in section " + sectionY + ": " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[CraterBiomeHelper] Exception in section " + sectionY + ": " + e.getMessage());
            }
        }

        if (modified) {
            chunk.setUnsaved(true);
        }

        return modified;
    }

    /**
     * Send all chunk updates to clients at once
     */
    private static void sendAllChunkUpdates(ServerLevel level, Set<LevelChunk> chunks) {
        if (chunks.isEmpty()) {
            System.out.println("[CraterBiomeHelper] No chunks to update");
            return;
        }

        System.out.println("[CraterBiomeHelper] Sending updates for " + chunks.size() + " chunks to all players...");

        try {
            for (LevelChunk chunk : chunks) {
                try {
                    // Create update packet
                    ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                            chunk,
                            level.getLightEngine(),
                            null,
                            null
                    );

                    // Send to ALL players on server
                    var players = level.getServer().getPlayerList().getPlayers();
                    for (var player : players) {
                        try {
                            player.connection.send(packet);
                        } catch (Exception e) {
                            System.err.println("[CraterBiomeHelper] Failed to send to player " + player.getName().getString());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[CraterBiomeHelper] Failed to create packet for chunk " + chunk.getPos());
                }
            }
        } catch (Exception e) {
            System.err.println("[CraterBiomeHelper] Critical error during broadcast!");
            e.printStackTrace();
        }
    }
}
