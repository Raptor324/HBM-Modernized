package com.hbm_m.util;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * Минимальные утилиты мира для fallout / смены биомов (порт WorldUtil).
 */
public final class WorldUtil {

    private WorldUtil() {}

    /**
     * Подгружает чанки вокруг точки спавна и добавляет сущность в мир.
     * Порт {@code WorldUtil.loadAndSpawnEntityInWorld} из 1.7.10 — нужен для ядерных взрывов вдали от игрока.
     */
    public static void loadAndSpawnEntityInWorld(Entity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel server)) {
            entity.level().addFreshEntity(entity);
            return;
        }

        ChunkPos center = new ChunkPos(entity.blockPosition());
        int loadRadius = 2;
        for (int dx = -loadRadius; dx <= loadRadius; dx++) {
            for (int dz = -loadRadius; dz <= loadRadius; dz++) {
                server.getChunkSource().getChunk(center.x + dx, center.z + dz, true);
            }
        }

        server.addFreshEntity(entity);
    }

    public static void setBiomeColumn(ServerLevel level, int x, int z, Holder<Biome> biome) {
        LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
        int localX = (x & 15) >> 2;
        int localZ = (z & 15) >> 2;

        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null) continue;
            @SuppressWarnings("unchecked")
            PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) section.getBiomes();
            biomes.set(localX, 0, localZ, biome);
            biomes.set(localX, 1, localZ, biome);
            biomes.set(localX, 2, localZ, biome);
            biomes.set(localX, 3, localZ, biome);
        }

        chunk.setUnsaved(true);
    }

    public static void flushChunk(ServerLevel level, LevelChunk chunk) {
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                chunk,
                level.getLightEngine(),
                null,
                null
        );
        ChunkPos chunkPos = chunk.getPos();
        int viewDistance = level.getServer().getPlayerList().getViewDistance();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            ChunkPos playerChunk = player.chunkPosition();
            if (Math.abs(playerChunk.x - chunkPos.x) <= viewDistance + 1
                    && Math.abs(playerChunk.z - chunkPos.z) <= viewDistance + 1) {
                player.connection.send(packet);
            }
        }
    }
}
