package com.hbm_m.radiation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.hbm_m.block.ModBlocks;
// Этот класс реализует простую и эффективную систему симуляции радиации в чанках. Ядро всей радиационной механики мода.

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.IChunkRadiation;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ChunkRadiationDebugBatchPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.particle.ModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
//? if forge {
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
//?}

// Моя конфетка, сколько же сил и нервов я на тебя потратил!
public class ChunkRadiationHandlerSimple extends ChunkRadiationHandler {

    /** {@link com.hbm.handler.radiation.ChunkRadiationHandlerSimple#handleWorldDestruction()} (1.7.10), hardcoded. */
    private static final float WORLD_RAD_EFFECTS_THRESHOLD = 10F;
    private static final int WORLD_RAD_BLOCK_OPS_PER_CHUNK = 10;
    private static final int WORLD_RAD_CHUNKS_PER_TICK = 5;
    /** {@link com.hbm.config.RadiationConfig#fogRad} */
    private static final float CHUNK_FOG_RAD_THRESHOLD = 100F;
    /** {@link com.hbm.config.RadiationConfig#fogCh} — 1:n раз в секунду (раз в 20 тиков). */
    private static final int CHUNK_FOG_SPAWN_CHANCE = 20;

    private static final float MAX_RAD = ModClothConfig.get().maxRad;
    private final Map<ResourceLocation, Set<ChunkPos>> activeChunksByDimension = new ConcurrentHashMap<>();
    private final Map<UUID, Map<ChunkPos, Float>> lastSentDebugValues = new ConcurrentHashMap<>();

    public static Optional<IChunkRadiation> getChunkRadiationCap(LevelChunk chunk) {
        return ChunkRadiationAccess.get(chunk);
    }

    @Override
    public void onBlockUpdated(Level level, BlockPos pos) {
        // if (level.isClientSide()) return;
        // dirtyChunksByDimension.computeIfAbsent(level.dimension().location(), k -> ConcurrentHashMap.newKeySet()).add(new ChunkPos(pos));
    }

    @Override
    public void updateSystem() {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) {
            return;
        }

        var server = dev.architectury.utils.GameInstance.getServer();
        if (server == null) {
            return;
        }

            // Итерация по всем измерениям, где есть активные чанки
        for (Map.Entry<ResourceLocation, Set<ChunkPos>> dimensionEntry : activeChunksByDimension.entrySet()) {
            ResourceLocation dimId = dimensionEntry.getKey();
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimId);

            ServerLevel level = server.getLevel(levelKey);
            if (level == null || level.isClientSide()) {
                continue;
            }

            Set<ChunkPos> currentActiveChunks = dimensionEntry.getValue();
            if (currentActiveChunks == null || currentActiveChunks.isEmpty()) {
                continue;
            }

            // 1:1 порт ChunkRadiationHandlerSimple#updateSystem (GIT): один Float на чанк,
            // snapshot+clear+rebuild, spread 60/7.5/2.5, decay ×0.99 − 0.05 для чанков уже имевших радиацию.
            // Источники (BlockHazard и т.д.) накачивают ambient собственным scheduled-tick — здесь только spread+decay.
            Map<ChunkPos, Float> buff = new HashMap<>();
            for (ChunkPos pos : new HashSet<>(currentActiveChunks)) {
                LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
                if (chunk == null) {
                    continue;
                }
                getChunkRadiationCap(chunk).ifPresent(cap -> {
                    float ambient = cap.getAmbientRadiation();
                    if (ambient > 1e-6f) {
                        buff.put(pos, ambient);
                    }
                });
            }

            if (buff.isEmpty()) {
                activeChunksByDimension.put(dimId, ConcurrentHashMap.newKeySet());
                continue;
            }

            // Накопитель новых значений (после spread+decay). Не пишётся напрямую в cap, чтобы
            // распределение считалось от стартового состояния цикла (как 1.7.10).
            Map<ChunkPos, Float> radiation = new HashMap<>();
            Set<ChunkPos> nextActiveChunks = ConcurrentHashMap.newKeySet();

            for (Map.Entry<ChunkPos, Float> chunkEntry : buff.entrySet()) {
                if (chunkEntry.getValue() == 0f) {
                    continue;
                }

                ChunkPos coord = chunkEntry.getKey();
                float sourceValue = chunkEntry.getValue();

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int type = Math.abs(dx) + Math.abs(dz);
                        float percent = type == 0 ? 0.6F : type == 1 ? 0.075F : 0.025F;
                        ChunkPos newCoord = new ChunkPos(coord.x + dx, coord.z + dz);

                        if (buff.containsKey(newCoord)) {
                            // Чанк уже имел радиацию → spread contribution + decay ×0.99 − 0.05.
                            float rad = radiation.getOrDefault(newCoord, 0f);
                            float newRad = rad + sourceValue * percent;
                            newRad = Mth.clamp(newRad * 0.99f - 0.05f, 0f, MAX_RAD);
                            radiation.put(newCoord, newRad);
                        } else {
                            // Свежий чанк, впервые получает spread — без декея в этот цикл (как 1.7.10).
                            radiation.put(newCoord, sourceValue * percent);
                        }

                        float rad = radiation.get(newCoord);
                        if (ModClothConfig.get().enableRadFogEffect
                                && rad > CHUNK_FOG_RAD_THRESHOLD
                                && level.random.nextInt(CHUNK_FOG_SPAWN_CHANCE) == 0
                                && level.hasChunk(newCoord.x, newCoord.z)) {
                            int x = newCoord.getMinBlockX() + level.random.nextInt(16);
                            int z = newCoord.getMinBlockZ() + level.random.nextInt(16);
                            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z)
                                    + level.random.nextInt(5);

                            level.sendParticles(
                                    ModParticleTypes.RAD_FOG_PARTICLE.get(),
                                    x + 0.5, y + 1.0, z + 0.5,
                                    7,
                                    1.5, 0.5, 1.5,
                                    0.01
                            );
                        }
                    }
                }
            }

            // Запись новых значений в cap и обновление nextActiveChunks.
            Set<ChunkPos> chunksToProcess = new HashSet<>(currentActiveChunks);
            chunksToProcess.addAll(radiation.keySet());

            for (ChunkPos pos : chunksToProcess) {
                LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
                if (chunk == null) {
                    continue;
                }

                float newAmbient = radiation.getOrDefault(pos, 0f);

                float fluctuationFactor = ModClothConfig.get().radRandomizationFactor;
                if (fluctuationFactor > 0 && newAmbient > 0.1f) {
                    newAmbient *= (1.0f + (level.random.nextFloat() - 0.5f) * fluctuationFactor);
                }

                if (newAmbient < 0.01f) {
                    newAmbient = 0f;
                }

                newAmbient = Mth.clamp(newAmbient, 0f, MAX_RAD);
                final float finalAmbientRad = newAmbient;

                getChunkRadiationCap(chunk).ifPresent(cap -> {
                    if (Math.abs(cap.getAmbientRadiation() - finalAmbientRad) > 1e-6f) {
                        cap.setAmbientRadiation(finalAmbientRad);
                        chunk.setUnsaved(true);

                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("[RadSim] Tick update for chunk [{}, {}]: NewAmb: {}",
                                    pos.x, pos.z, finalAmbientRad);
                        }
                    }

                    if (finalAmbientRad > 1e-6f) {
                        nextActiveChunks.add(pos);
                    } else if (ModClothConfig.get().enableDebugLogging && currentActiveChunks.contains(pos)) {
                        MainRegistry.LOGGER.debug("[RadSim] Chunk {} REMOVED from active list (all radiation gone)", pos);
                    }
                });
            }

            activeChunksByDimension.put(dimId, nextActiveChunks);
        }

        if (ModClothConfig.get().enableDebugRender) {
            for (ServerLevel level : server.getAllLevels()) {
                sendDebugPackets(level);
            }
        }
    }

    @Override
    public void recalculateChunkRadiation(LevelChunk chunk) {
        // No-op: 1.7.10 ChunkRadiationHandlerSimple не суммирует радиацию от блоков.
        // Источники (BlockHazard и т.д.) переэмиттят радиацию в ambient собственным scheduled-tick.
        // Метод оставлен как no-op, т.к. он вызывается из EntityFalloutRain/ExplosionVNT.
    }

    @Override
    public void receiveChunkLoad(LevelChunk chunk) {
        getChunkRadiationCap(chunk).ifPresent(cap -> {
            if (cap.getAmbientRadiation() > 1e-6f) {
                activeChunksByDimension.computeIfAbsent(chunk.getLevel().dimension().location(),
                        k -> ConcurrentHashMap.newKeySet()).add(chunk.getPos());
            }
        });
    }

    //? if forge {
    @Override
    public void receiveChunkUnload(ChunkEvent.Unload event) {
        if (event.getChunk() instanceof LevelChunk chunk && !chunk.getLevel().isClientSide()) {
            Optional.ofNullable(activeChunksByDimension.get(chunk.getLevel().dimension()
            .location())).ifPresent(set -> set.remove(chunk.getPos()));
        }
    }
    //?} else {
    /*// ВЫЗЫВАЕТСЯ ИЗ МЕНЕДЖЕРА ДЛЯ NEOFORGE / FABRIC
    public void receiveChunkUnload(LevelChunk chunk) {
        if (!chunk.getLevel().isClientSide()) {
            Optional.ofNullable(activeChunksByDimension.get(chunk.getLevel().dimension()
                    .location())).ifPresent(set -> set.remove(chunk.getPos()));
        }
    }
    *///?}

    @Override
    public float getRadiation(Level level, int x, int y, int z) {
        if (level == null || level.isClientSide()) return 0F;
        ChunkAccess chunkAccess = level.getChunk(x >> 4, z >> 4);
        if (chunkAccess instanceof LevelChunk chunk) {
            AtomicReference<Float> radiation = new AtomicReference<>(0f);
            getChunkRadiationCap(chunk).ifPresent(cap -> radiation.set(cap.getAmbientRadiation()));
            return radiation.get();
        }
        return 0F;
    }

    @Override
    public void setRadiation(Level level, int x, int y, int z, float rad) {
        if (level == null || level.isClientSide()) return;
        ChunkAccess chunkAccess = level.getChunk(x >> 4, z >> 4);
        if (chunkAccess instanceof LevelChunk chunk) {
            getChunkRadiationCap(chunk).ifPresent(cap -> {
                cap.setAmbientRadiation(rad);
                chunk.setUnsaved(true);
                if (rad > 1e-6f) {
                    activeChunksByDimension.computeIfAbsent(level.dimension()
                    .location(), k -> ConcurrentHashMap.newKeySet()).add(chunk.getPos());
                }
            });
        }
    }

    @Override
    public void incrementRad(Level level, int x, int y, int z, float rad) {
        setRadiation(level, x, y, z, getRadiation(level, x, y, z) + rad);
    }

    @Override
    public void decrementRad(Level level, int x, int y, int z, float rad) {
        setRadiation(level, x, y, z, Math.max(0, getRadiation(level, x, y, z) - rad));
    }

    private void sendDebugPackets(ServerLevel level) {
        if (!ModClothConfig.get().enableDebugRender) return;

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) continue;
            
            boolean isCreativeOrSpectator = player.isCreative() || player.isSpectator();
            if (!ModClothConfig.get().debugRenderInSurvival && !isCreativeOrSpectator) {
                // Если игрок не в креативе/спектаторе и рендер в выживании выключен, очищаем его кеш
                // на случай, если он только что вышел из креатива, и пропускаем его.
                lastSentDebugValues.remove(player.getUUID());
                continue;
            }

            Map<ChunkPos, Float> updatesForPlayer = new HashMap<>();
            Map<ChunkPos, Float> playerLastValues = lastSentDebugValues.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
            
            ChunkPos playerChunkPos = player.chunkPosition();
            int radius = 4;
            
            Set<ChunkPos> visibleChunks = new HashSet<>();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    ChunkPos chunkPos = new ChunkPos(playerChunkPos.x + dx, playerChunkPos.z + dz);
                    visibleChunks.add(chunkPos);

                    float currentValue = 0f;
                    LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                    if (chunk != null) {
                        currentValue = getChunkRadiationCap(chunk).map(IChunkRadiation::getAmbientRadiation).orElse(0f);
                    }
                    
                    float lastSentValue = playerLastValues.getOrDefault(chunkPos, -1f); // -1f гарантирует отправку при первом появлении

                    // Отправляем, если значение изменилось (с погрешностью для float)
                    if (Math.abs(currentValue - lastSentValue) > 1e-6f) {
                        updatesForPlayer.put(chunkPos, currentValue);
                        playerLastValues.put(chunkPos, currentValue);
                    }
                }
            }
            
            // Очистка старых, вышедших из поля зрения чанков из кеша игрока, чтобы избежать утечек памяти
            // и отправить им 0, если они стали невидимы.
            playerLastValues.entrySet().removeIf(entry -> {
                if (!visibleChunks.contains(entry.getKey())) {
                    // Если чанк больше не виден и его последнее значение не было 0, добавляем 0 в апдейт
                    if (entry.getValue() > 0f) {
                        updatesForPlayer.put(entry.getKey(), 0f);
                    }
                    return true; // Удаляем из кеша
                }
                return false;
            });

            if (!updatesForPlayer.isEmpty()) {
                ModPacketHandler.sendToPlayer(player, ModPacketHandler.CHUNK_RAD_DEBUG_BATCH,
                    new ChunkRadiationDebugBatchPacket(updatesForPlayer, level.dimension().location()));
            }
        }
    }

    // Для очистки кеша при выходе игрока (вызывается из RadiationEvents)
    public void clearPlayerDebugCache(UUID playerUUID) {
        lastSentDebugValues.remove(playerUUID);
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("Cleared debug radiation cache for player {}", playerUUID);
        }
    }

    /**
     * Эффекты радиации на мир (трава → мёртвая трава, листья → waste leaves и т.д.).
     * Порт 1:1 с {@code ChunkRadiationHandlerSimple#handleWorldDestruction()} (1.7.10):
     * каждый серверный тик, 5 случайных чанков, 10 проходов, порог 10 RAD (hardcoded, как в 1.7.10).
     */
    @Override
    public void handleWorldDestruction() {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) {
            return;
        }

        int count = WORLD_RAD_BLOCK_OPS_PER_CHUNK;
        float threshold = WORLD_RAD_EFFECTS_THRESHOLD;
        int chunksPerTick = WORLD_RAD_CHUNKS_PER_TICK;

        var server = dev.architectury.utils.GameInstance.getServer();
        if (server == null) {
            return;
        }

        for (Map.Entry<ResourceLocation, Set<ChunkPos>> dimensionEntry : activeChunksByDimension.entrySet()) {
            ResourceLocation dimId = dimensionEntry.getKey();
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimId);
            ServerLevel level = server.getLevel(levelKey);
            if (level == null || level.isClientSide()) {
                continue;
            }

            Set<ChunkPos> chunkSet = dimensionEntry.getValue();
            if (chunkSet == null || chunkSet.isEmpty()) {
                continue;
            }

            ChunkPos[] chunkArray = chunkSet.toArray(ChunkPos[]::new);

            for (int c = 0; c < chunksPerTick; c++) {
                ChunkPos coords = chunkArray[level.random.nextInt(chunkArray.length)];

                if (!level.hasChunk(coords.x, coords.z)) {
                    continue;
                }

                LevelChunk chunk = level.getChunkSource().getChunk(coords.x, coords.z, false);
                if (chunk == null) {
                    continue;
                }

                float rad = getChunkRadiationCap(chunk).map(IChunkRadiation::getAmbientRadiation).orElse(0f);
                if (rad < threshold) {
                    continue;
                }

                int minX = coords.getMinBlockX();
                int minZ = coords.getMinBlockZ();

                for (int i = 0; i < count; i++) {
                    int x = minX + level.random.nextInt(16);
                    int z = minZ + level.random.nextInt(16);
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z)
                            - 1 - level.random.nextInt(2);

                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(blockPos);

                    if (state.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(blockPos, ModBlocks.WASTE_GRASS.get().defaultBlockState(), 2);
                    //? if < 1.21.1 {
                    } else if (state.is(Blocks.GRASS)) {
                    //?} else {
                    /*} else if (state.is(Blocks.SHORT_GRASS)) {
                    *///?}
                        level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    } else if (state.is(BlockTags.LEAVES) && !state.is(ModBlocks.WASTE_LEAVES.get())) {
                        if (level.random.nextInt(7) <= 5) {
                            level.setBlock(blockPos, ModBlocks.WASTE_LEAVES.get().defaultBlockState(), 2);
                        } else {
                            level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }


    @Override
    public void clearSystem(Level level) {
        if (level == null || level.isClientSide()) {
            return;
        }
        activeChunksByDimension.remove(level.dimension().location());
    }

    //? if forge {
    @Override
    public void receiveWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            activeChunksByDimension.remove(level.dimension().location());
        }
    }

    @Override
    public void receiveWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            activeChunksByDimension.remove(level.dimension().location());
        }
    }
    //?}
}