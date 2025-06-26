package com.hbm_m.radiation;

import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.capability.IChunkRadiation;
import com.hbm_m.config.RadiationConfig;
import com.hbm_m.block.RadioactiveBlock;
import com.hbm_m.main.MainRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.server.ServerLifecycleHooks;


import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.ChunkRadiationDebugPacket;

public class ChunkRadiationHandlerSimple extends ChunkRadiationHandler {

    private static final float MAX_RAD = RadiationConfig.maxRad;
    private final java.util.Set<ChunkPos> activeChunks = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Публичный статический метод для доступа к capability радиации чанка
    public static java.util.Optional<IChunkRadiation> getChunkRadiationCap(LevelChunk chunk) {
        java.util.Optional<IChunkRadiation> opt = chunk.getCapability(ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY).resolve();
        if (opt.isPresent()) {
        } else {
            MainRegistry.LOGGER.warn("getChunkRadiationCap: capability NOT PRESENT for chunkPos={}, chunkHash={}", chunk.getPos(), System.identityHashCode(chunk));
        }
        return opt;
    }

    @Override
    public float getRadiation(Level level, int x, int y, int z) {
        if (level == null || level.isClientSide()) return 0F;
        LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
        final float[] radiation = {0F};
        getChunkRadiationCap(chunk).ifPresent(cap -> {
            radiation[0] = cap.getRadiation();
            if (radiation[0] > 1e-8F) {
                MainRegistry.LOGGER.debug("getRadiation: chunkPos={}, chunkHash={}, capHash={}, blockRad={}, totalRad={}", chunk.getPos(), System.identityHashCode(chunk), System.identityHashCode(cap), cap.getBlockRadiation(), radiation[0]);
            }
        });
        return radiation[0];
    }

    @Override
    public void setRadiation(Level level, int x, int y, int z, float rad) {
        if (level == null || level.isClientSide()) return;
        LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
        float value = Mth.clamp(rad, 0, MAX_RAD);
        getChunkRadiationCap(chunk).ifPresent(cap -> {
            cap.setBlockRadiation(value); // Теперь устанавливаем blockRadiation
            chunk.setUnsaved(true); // Помечаем чанк как измененный для сохранения
            if (value > 1e-8F) {
                MainRegistry.LOGGER.debug("setRadiation (block): chunkPos={}, chunkHash={}, capHash={}, setBlockValue={}", chunk.getPos(), System.identityHashCode(chunk), System.identityHashCode(cap), value);
            }
        });
    }

    public void incrementRad(Level level, int x, int y, int z, float rad) {
        setRadiation(level, x, y, z, getRadiation(level, x, y, z) + rad);
    }

    public void decrementRad(Level level, int x, int y, int z, float rad) {
        setRadiation(level, x, y, z, Math.max(getRadiation(level, x, y, z) - rad, 0));
    }

    @Override
    public void updateSystem() {
        for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            if (level.isClientSide()) continue;

            // --- Распространение и распад радиации через activeChunks ---
            java.util.Map<ChunkPos, Float> oldRadiation = new java.util.HashMap<>();
            java.util.Map<ChunkPos, Float> newRadiation = new java.util.HashMap<>();

            // Сохраняем текущие значения радиации только для активных чанков
            for (ChunkPos pos : activeChunks) {
                LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
                if (chunk != null) {
                    float blockRad = recalculateChunkRadiation(chunk);
                    oldRadiation.put(pos, blockRad);
                }
            }

            // --- Новый подход: сначала формируем карту "притока" от блоков с учётом radSourceInfluenceFactor ---
            for (ChunkPos pos : activeChunks) {
                LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
                if (chunk != null) {
                    float blockRad = recalculateChunkRadiation(chunk) * RadiationConfig.radSourceInfluenceFactor; // только приток от блоков с коэффициентом
                    newRadiation.put(pos, blockRad);
                }
            }

            // Далее распространяем радиацию (аналогично оригинальному HBM)
            java.util.Map<ChunkPos, Float> spreadRadiation = new java.util.HashMap<>();
            for (java.util.Map.Entry<ChunkPos, Float> entry : newRadiation.entrySet()) {
                ChunkPos pos = entry.getKey();
                float value = entry.getValue();
                if (value < 1e-6f) continue;

                // Ядро (сам чанк)
                spreadRadiation.put(pos, spreadRadiation.getOrDefault(pos, 0f) + value * 0.6f);
                // Стороны и углы
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int type = Math.abs(dx) + Math.abs(dz);
                        if ((type == 1 || type == 2) && (dx != 0 || dz != 0)) {
                            float percent = (type == 1) ? 0.075f : 0.025f;
                            ChunkPos neighbor = new ChunkPos(pos.x + dx, pos.z + dz);
                            spreadRadiation.put(neighbor, spreadRadiation.getOrDefault(neighbor, 0f) + value * percent);
                        }
                    }
                }
            }

            
            // Применяем распад с усиленной рандомизацией (±60%)
            for (java.util.Map.Entry<ChunkPos, Float> entry : spreadRadiation.entrySet()) {
                ChunkPos pos = entry.getKey();
                float radValue = entry.getValue();
                float randomFactor = 1.0f + (level.random.nextFloat() - 0.5f) * 0.8f; // ±40%
                radValue = radValue * 0.99f * randomFactor - 0.05f;
                radValue = Math.max(0f, Math.min(radValue, MAX_RAD));
                LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
                if (chunk != null) {
                    final float finalRad = radValue;
                    java.util.Optional<IChunkRadiation> capOpt = getChunkRadiationCap(chunk);
                    capOpt.ifPresent(cap -> {
                        cap.setBlockRadiation(finalRad);
                        chunk.setUnsaved(true);
                    });
                }
            }
            // --- Конец блока распространения и распада ---

            // Отправляем отладочные пакеты радиации чанков игрокам в режиме отладки
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (player.level() != level) continue;
                if (player.isCreative() || player.isSpectator()) {
                    int playerChunkX = player.chunkPosition().x;
                    int playerChunkZ = player.chunkPosition().z;
                    int radius = 3;
                    for (int dx_player = -radius; dx_player <= radius; dx_player++) {
                        for (int dz_player = -radius; dz_player <= radius; dz_player++) {
                            int chunkX = playerChunkX + dx_player;
                            int chunkZ = playerChunkZ + dz_player;
                            LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                            if (chunk != null) {
                                java.util.Optional<IChunkRadiation> capOpt = getChunkRadiationCap(chunk);
                                capOpt.ifPresent(cap -> {
                                    float value = cap.getRadiation();
                                    // Теперь всегда отправляем пакет, даже если value == 0
                                    ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ChunkRadiationDebugPacket(chunkX, chunkZ, value));
                                    MainRegistry.LOGGER.debug("SERVER: Sending ChunkRadiationDebugPacket for chunk ({}, {}) with value {} to player {}", chunkX, chunkZ, value, player.getName().getString());
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void clearSystem(Level level) {
        // Capability автоматически очищается при выгрузке чанка
    }

    @Override
    public void receiveWorldLoad(LevelEvent.Load event) {
        // Не требуется, так как данные хранятся в Capability чанка
    }

    @Override
    public void receiveWorldUnload(LevelEvent.Unload event) {
        // Не требуется, так как данные хранятся в Capability чанка
    }

    @Override
    public void receiveChunkLoad(ChunkDataEvent.Load event) {
        if (event.getLevel() != null && !event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            activeChunks.add(chunk.getPos());
            getChunkRadiationCap(chunk).ifPresent(cap -> {
                float recalculatedRad = recalculateChunkRadiation(chunk);
                // Только обновляем blockRadiation, envRadiation не трогаем!
                // cap.setBlockRadiation(recalculatedRad); // уже вызывается внутри recalculateChunkRadiation
                chunk.setUnsaved(true);
                if (recalculatedRad > 1e-8F) {
                    MainRegistry.LOGGER.debug("Chunk {}: blockRadiation recalculated from blocks and set to capability: {}", chunk.getPos(), recalculatedRad);
                }
            });
        }
    }

    @Override
    public void receiveChunkSave(ChunkDataEvent.Save event) {
        if (event.getLevel() != null && !event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            getChunkRadiationCap(chunk).ifPresent(cap -> {
                if (cap.getRadiation() > 1e-8F) {
                    MainRegistry.LOGGER.debug("Saved chunk {} with radiation from capability: {}", chunk.getPos(), cap.getRadiation());
                }
            });
        }
    }

    /**
     * Пересчитывает радиацию в чанке на основе радиоактивных блоков.
     * Этот метод сканирует все блоки в чанке и суммирует их радиационное излучение.
     * @param chunk Чанк для пересчета.
     * @return Общий уровень радиации от блоков в чанке.
     */

    public float recalculateChunkRadiation(LevelChunk chunk) {
        long startTime = System.nanoTime();
        final float[] totalBlockRadiation = {0F};
        ChunkPos chunkPos = chunk.getPos();

        LevelChunkSection[] sections = chunk.getSections();

        // Итерируем по всем блокам в чанке
        // Итерируем по всем секциях чанка
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (!section.hasOnlyAir()) { // Проверяем, содержит ли секция блоки (не только воздух)
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockState blockState = section.getBlockState(x, y, z);
                            if (blockState.getBlock() instanceof RadioactiveBlock radioactiveBlock) {
                                if (radioactiveBlock.getRadiationLevel() > 1e-8F) {
                                    MainRegistry.LOGGER.debug("recalculateChunkRadiation: Found RadioactiveBlock at chunk relative pos ({}, {}, {}) in section {}. Radiation level: {}", x, y, z, sectionIndex, radioactiveBlock.getRadiationLevel());
                                }
                                totalBlockRadiation[0] += radioactiveBlock.getRadiationLevel();
                            }
                        }
                    }
                }
            }
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // миллисекунды
        if (totalBlockRadiation[0] > 1e-8F) {
            MainRegistry.LOGGER.debug("recalculateChunkRadiation: Finished recalculation for chunk {}. Took {} ms. Total block radiation: {}", chunkPos, duration, totalBlockRadiation[0]);
            MainRegistry.LOGGER.debug("recalculateChunkRadiation: Final totalBlockRadiation for chunk {}: {}", chunkPos, totalBlockRadiation[0]);
        }
        // Сохраняем только blockRadiation в capability
        getChunkRadiationCap(chunk).ifPresent(cap -> {
            cap.setBlockRadiation(totalBlockRadiation[0]);
            chunk.setUnsaved(true);
        });
        return totalBlockRadiation[0];
    }

    @Override
    public void receiveChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() != null && !event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            activeChunks.remove(chunk.getPos());
        }
    }
}