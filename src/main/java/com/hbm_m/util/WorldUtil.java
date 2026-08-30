package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Ставит биомы сразу на несколько кварт 4x4 по всей высоте чанка за один проход
     * по секциям. Массив индексируется как [bx * 4 + bz] (bx/bz в 0..3),
     * null = кварту не трогать.
     */
    @SuppressWarnings("unchecked")
    public static void setBiomeQuarts(LevelChunk chunk, Holder<Biome>[] quarts) {
        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null) continue;
            PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) section.getBiomes();
            for (int i = 0; i < quarts.length; i++) {
                Holder<Biome> h = quarts[i];
                if (h == null) continue;
                int lx = i >> 2;
                int lz = i & 3;
                biomes.set(lx, 0, lz, h);
                biomes.set(lx, 1, lz, h);
                biomes.set(lx, 2, lz, h);
                biomes.set(lx, 3, lz, h);
            }
        }
        chunk.setUnsaved(true);
    }

    /**
     * Быстрая запись блока напрямую в чанк в обход {@code Level.setBlock}:
     * не вызывает markAndNotifyBlock (neighbor shape updates, хоппер-проверка Lithium,
     * блокирующая догрузка соседних чанков), onRemove/onPlace и sendBlockUpdated.
     * Heightmaps, свет и unsaved-флаг обрабатываются самим LevelChunk.
     * Клиенту изменения уходят одним полным пакетом чанка через {@link #flushChunk}.
     *
     * @return true, если состояние реально изменилось
     */
    public static boolean setBlockFast(LevelChunk chunk, BlockPos.MutableBlockPos pos, BlockState state) {
        if (chunk.getMinBuildHeight() > pos.getY() || pos.getY() >= chunk.getMaxBuildHeight()) {
            return false;
        }
        return chunk.setBlockState(pos, state, false) != null;
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
        ChunkPos chunkPos = chunk.getPos();
        int viewDistance = level.getServer().getPlayerList().getViewDistance();

        // Сначала собираем получателей — полный пакет чанка (сериализация + свет)
        // дорогой, строить его только чтобы выбросить, если игроков рядом нет, нельзя
        List<ServerPlayer> receivers = null;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            ChunkPos playerChunk = player.chunkPosition();
            if (Math.abs(playerChunk.x - chunkPos.x) <= viewDistance + 1
                    && Math.abs(playerChunk.z - chunkPos.z) <= viewDistance + 1) {
                if (receivers == null) {
                    receivers = new ArrayList<>();
                }
                receivers.add(player);
            }
        }
        if (receivers == null) {
            return;
        }

        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                chunk,
                level.getLightEngine(),
                null,
                null
        );
        for (ServerPlayer player : receivers) {
            player.connection.send(packet);
        }
    }
}
