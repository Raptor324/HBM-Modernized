package com.hbm_m.radiation;

// Этот класс реализует простую и эффективную систему симуляции радиации в чанках. Ядро всей радиационной механики мода.

import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.capability.IChunkRadiation;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;
import com.hbm_m.block.custom.nature.RadioactiveBlock;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.block.ModBlocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.network.PacketDistributor;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.network.ChunkRadiationDebugBatchPacket;

// Моя конфетка, сколько же сил и нервов я на тебя потратил!
public class ChunkRadiationHandlerSimple extends ChunkRadiationHandler {

    private static final float MAX_RAD = ModClothConfig.get().maxRad;
    private final Map<ResourceLocation, Set<ChunkPos>> activeChunksByDimension = new ConcurrentHashMap<>();
    private final Map<UUID, Map<ChunkPos, Float>> lastSentDebugValues = new ConcurrentHashMap<>();

    public static Optional<IChunkRadiation> getChunkRadiationCap(LevelChunk chunk) {
        return chunk.getCapability(ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY).resolve();
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

    // Итерация по всем измерениям, где есть активные чанки
    for (Map.Entry<ResourceLocation, Set<ChunkPos>> dimensionEntry : activeChunksByDimension.entrySet()) {
        
        ResourceLocation dimId = dimensionEntry.getKey();
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimId);
        
        ServerLevel level = ServerLifecycleHooks.getCurrentServer().getLevel(levelKey);
        if (level == null || level.isClientSide()) continue;

        Set<ChunkPos> currentActiveChunks = dimensionEntry.getValue();
        if (currentActiveChunks == null || currentActiveChunks.isEmpty()) {
            continue;
        }

        // Буферы для данных
        Map<ChunkPos, Float> ambientReadBuffer = new HashMap<>();
        Map<ChunkPos, Float> blockReadBuffer = new HashMap<>();

        // ФАЗА 1: ЧТЕНИЕ 
        // Считываем состояние всех активных чанков, чтобы избежать гонки потоков
        for (ChunkPos pos : new HashSet<>(currentActiveChunks)) {
            LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
            if (chunk != null) {
                getChunkRadiationCap(chunk).ifPresent(cap -> {
                    ambientReadBuffer.put(pos, cap.getAmbientRadiation());
                    blockReadBuffer.put(pos, cap.getBlockRadiation());
                });
            }
        }

        // Буфер для записи результатов распространения
        Map<ChunkPos, Float> writeBuffer = new HashMap<>();

        // ФАЗА 2: РАСЧЕТ РАСПРОСТРАНЕНИЯ 
        // Корректное распространение радиации: только чтение из ambientReadBuffer, запись в writeBuffer с merge
        for (Map.Entry<ChunkPos, Float> entry : ambientReadBuffer.entrySet()) {
                ChunkPos pos = entry.getKey();
                float ambientToSpread = entry.getValue();
                if (ambientToSpread > 1e-6f) {
                    // Распределяем 95% радиации, 5% теряется при распространении
                    float spreadFactor = 0.95f; 
                    float totalToSpread = ambientToSpread * spreadFactor;
                    
                    float centerShare = totalToSpread * 0.60f;
                    float cardinalShare = totalToSpread * 0.075f; // 4 * 0.075 = 0.3
                    float diagonalShare = totalToSpread * 0.025f; // 4 * 0.025 = 0.1

                    if (centerShare > 0) {
                        writeBuffer.merge(pos, centerShare, Float::sum);
                    }

                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            
                            float share = (Math.abs(dx) + Math.abs(dz) == 1) ? cardinalShare : diagonalShare;

                            // Добавляем порог для распространения
                            // Это предотвратит распространение незначительных значений
                            if (share > 0.1f) {
                                ChunkPos neighbor = new ChunkPos(pos.x + dx, pos.z + dz);
                                writeBuffer.merge(neighbor, share, Float::sum);
                            }
                        }
                    }
                }
            }

        // ФАЗА 3: ПРИМЕНЕНИЕ И ОБНОВЛЕНИЕ 
        Set<ChunkPos> chunksToProcess = new HashSet<>(currentActiveChunks);
            chunksToProcess.addAll(writeBuffer.keySet());
            Set<ChunkPos> nextActiveChunks = ConcurrentHashMap.newKeySet();

            for (ChunkPos pos : chunksToProcess) {
                LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
                if (chunk == null) continue;

                float oldAmbient = ambientReadBuffer.getOrDefault(pos, 0f);
                
                // 1. Новое значение фона = то, что пришло от соседей
                float newAmbient = writeBuffer.getOrDefault(pos, 0f);

                // 2. Добавляем генерацию от блоков в этом чанке
                float generation = blockReadBuffer.getOrDefault(pos, 0f);
                if (generation > 1e-6f) {
                    newAmbient += generation * ModClothConfig.get().radSourceInfluenceFactor;
                }
                // Распад состоит из процентной и фиксированной части, чтобы добивать малые значения.
                float decayPercent = 0.05f; // 5% от текущего значения
                float decayFlat = 0.1f;    // 0.1 рад в секунду (в 20 тиков)
                newAmbient -= (newAmbient * decayPercent + decayFlat);

                float fluctuationFactor = ModClothConfig.get().radRandomizationFactor;
                // Применяем только если включено и есть чему флуктуировать
                if (fluctuationFactor > 0 && newAmbient > 0.1f) { 
                    newAmbient *= (1.0f + (level.random.nextFloat() - 0.5f) * fluctuationFactor);
                }

                // 4. Ограничения и очистка
                float clearThreshold = 0.01f;
                if (newAmbient < clearThreshold) {
                    newAmbient = 0f;
                }

                newAmbient = Mth.clamp(newAmbient, 0f, MAX_RAD);

                final float finalAmbientRad = newAmbient;

                // СПАВНИМ РАДИОАКТИВНЫЙ ТУМАН
                if (ModClothConfig.get().enableRadFogEffect && finalAmbientRad > ModClothConfig.get().radFogThreshold && level.random.nextInt(ModClothConfig.get().radFogChance) == 0) {
                    int x = pos.getMinBlockX() + level.random.nextInt(16);
                    int z = pos.getMinBlockZ() + level.random.nextInt(16);
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

                    // Спавним несколько частиц, чтобы создать эффект облака
                    level.sendParticles(
                        ModParticleTypes.RAD_FOG_PARTICLE.get(),
                        x + 0.5, y + 1.0, z + 0.5,
                        7, // Количество частиц
                        1.5, 0.5, 1.5, // Разброс по осям
                        0.01 // Скорость/дельта
                    );
                }

                getChunkRadiationCap(chunk).ifPresent(cap -> {
                    if (Math.abs(cap.getAmbientRadiation() - finalAmbientRad) > 1e-6f) {
                        cap.setAmbientRadiation(finalAmbientRad);
                        chunk.setUnsaved(true);
                        
                        if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.debug("[RadSim] Tick update for chunk [{}, {}]: OldAmb: {}, SpreadIn: {}, Gen: {}, NewAmb: {}",
                            pos.x, pos.z, oldAmbient, writeBuffer.getOrDefault(pos, 0f), generation, finalAmbientRad);
                        }
                    }

                    // Чанк остается активным, если в нем есть хоть какая-то радиация (фоновая или от блоков)
                    if (finalAmbientRad > 1e-6f || cap.getBlockRadiation() > 1e-6f) {
                        nextActiveChunks.add(pos);
                    } else if (currentActiveChunks.contains(pos)) {
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("[RadSim] Chunk {} REMOVED from active list (all radiation gone)", pos);
                        }
                    }
                });
                // Проверяем, включены ли эффекты и достаточно ли радиации
                if (ModClothConfig.get().worldRadEffects && finalAmbientRad > ModClothConfig.get().worldRadEffectsThreshold) {
                    handleWorldDestruction(level, pos, finalAmbientRad);
                }
            }
            activeChunksByDimension.put(dimId, nextActiveChunks);
            sendDebugPackets(level);
        }
    }

    @Override
    public void recalculateChunkRadiation(LevelChunk chunk) {
        float totalBlockRadiation = 0F;
        for (LevelChunkSection section : chunk.getSections()) {
            // hasOnlyAir() немного эффективнее, чем section != null && !section.isEmpty()
            if (section != null && !section.hasOnlyAir()) { 
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockState blockState = section.getBlockState(x, y, z);
                            if (blockState.isAir()) {
                                continue;
                            }
                            float blockRad = HazardSystem.getHazardLevelFromState(blockState, HazardType.RADIATION);

                            // Если радиация есть, добавляем ее к общей сумме в чанке
                            if (blockRad > 0) {
                                totalBlockRadiation += blockRad;
                            }
                        }
                    }
                }
            }
        }
        
        final float finalTotalBlockRadiation = totalBlockRadiation;
        getChunkRadiationCap(chunk).ifPresent(cap -> {
            // Устанавливаем значение только если оно изменилось
            if (Math.abs(cap.getBlockRadiation() - finalTotalBlockRadiation) > 1e-6f) {
                cap.setBlockRadiation(finalTotalBlockRadiation);
                chunk.setUnsaved(true);
            }
            // Активируем чанк если в нем есть любой вид радиации
            if (cap.getAmbientRadiation() > 1e-6f || finalTotalBlockRadiation > 1e-6f) {
                activeChunksByDimension.computeIfAbsent(chunk.getLevel().dimension()
                    .location(), k -> ConcurrentHashMap.newKeySet()).add(chunk.getPos());
            }
        });
    }

    @Override
    public void receiveChunkLoad(LevelChunk chunk) {
        // Пересчет нужен только при загрузке, чтобы инициализировать состояние
        recalculateChunkRadiation(chunk); 
    }

    @Override
    public void receiveChunkUnload(ChunkEvent.Unload event) {
        if (event.getChunk() instanceof LevelChunk chunk && !chunk.getLevel().isClientSide()) {
            Optional.ofNullable(activeChunksByDimension.get(chunk.getLevel().dimension()
            .location())).ifPresent(set -> set.remove(chunk.getPos()));
        }
    }

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
    
    @Override
    public void incrementBlockRadiation(Level level, BlockPos pos, float diff) {
        if (level.isClientSide()) return;

        ChunkPos chunkPos = new ChunkPos(pos);
        LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
        if (chunk == null) return;

        getChunkRadiationCap(chunk).ifPresent(cap -> {
            float oldBlockRad = cap.getBlockRadiation();
            float newBlockRad = Math.max(0, oldBlockRad + diff);
            
            cap.setBlockRadiation(newBlockRad);
            chunk.setUnsaved(true);
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("[RadSim] Updated block radiation for chunk {}: {} -> {} (diff: {})", chunkPos, oldBlockRad, newBlockRad, diff);
            }
            // "Пробуждаем" симуляцию для этого чанка, если он еще не активен
            if (newBlockRad > 1e-6f || cap.getAmbientRadiation() > 1e-6f) {
                activeChunksByDimension.computeIfAbsent(level.dimension().location(), k -> ConcurrentHashMap.newKeySet())
                                       .add(chunkPos);
            }
        });
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
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
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
     * Обрабатывает эффекты разрушения мира в чанке под воздействием сильной радиации.
     * Логика спроектирована для максимальной производительности и естественного вида эффекта.
     *
     * @param level            Серверный мир, в котором происходят изменения.
     * @param pos              Позиция чанка.
     * @param currentRadiation Текущий уровень фоновой радиации в чанке.
     */
    private void handleWorldDestruction(ServerLevel level, ChunkPos pos, float currentRadiation) {
        ModClothConfig config = ModClothConfig.get();
        int baseChecks = config.worldRadEffectsBlockChecks;
        if (baseChecks <= 0) return;

        // Шаг 1: Рассчитать количество проверок в этом тике в зависимости от радиации.
        // Чем выше радиация, тем интенсивнее эффект.
        float normalizedRad = Mth.inverseLerp(currentRadiation, config.worldRadEffectsThreshold, config.maxRad);
        float scalingFactor = Mth.lerp(normalizedRad, 1.0F, config.worldRadEffectsMaxScaling);
        int actualChecks = Math.max(baseChecks, (int)(baseChecks * scalingFactor));

        // Создаем один изменяемый объект BlockPos, чтобы переиспользовать его во всех циклах.
        // Это ключевая оптимизация, предотвращающая создание тысяч объектов.
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // Шаг 2: Выполнить рассчитанное количество случайных проверок в чанке.
        for (int i = 0; i < actualChecks; i++) {
            int x = pos.getMinBlockX() + level.random.nextInt(16);
            int z = pos.getMinBlockZ() + level.random.nextInt(16);

            // Находим высоту самого верхнего блока, на который может влиять движение (трава, земля, но не листья).
            int startY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);

            // Шаг 3: Двигаемся вниз от поверхности ("бурим"), ища первый подходящий блок для мутации.
            for (int y = startY; y >= level.getMinBuildHeight(); y--) {
                mutablePos.set(x, y, z);
                BlockState currentState = level.getBlockState(mutablePos);

                // 3.1: Пропускаем воздух, продолжая "бурение" вглубь.
                if (currentState.isAir()) {
                    continue;
                }

                // 3.2: Если мы наткнулись на неразрушимый или уже измененный блок, прекращаем обработку этой колонки.
                if (currentState.is(Blocks.BEDROCK) || currentState.getBlock() instanceof RadioactiveBlock) {
                    break; // Выходим из внутреннего цикла (for y), переходим к следующей итерации (for i).
                }

                // 3.3: Основная логика замены блоков. Используем if-else if для однозначности.
                BlockState newState = null;

                if (currentState.is(Blocks.GRASS_BLOCK)) {
                    newState = ModBlocks.WASTE_GRASS.get().defaultBlockState();
                } else if (currentState.is(BlockTags.LEAVES)) {
                    // Листья могут либо превратиться в мертвые, либо просто исчезнуть.
                    if (level.random.nextInt(7) <= 5) {
                        newState = ModBlocks.WASTE_LEAVES.get().defaultBlockState();
                    } else {
                        newState = Blocks.AIR.defaultBlockState();
                    }
                } else if (currentState.is(Blocks.DIRT)) {
                    // Раскомментируйте, когда добавите блок Waste Dirt
                    // newState = ModBlocks.WASTE_DIRT.get().defaultBlockState();
                }
                // Сюда можно легко добавить другие правила:
                // else if (currentState.is(BlockTags.SAND)) { ... }
                // else if (currentState.is(Blocks.WATER)) { ... }


                // 3.4: Применяем изменения, если было найдено правило для замены.
                if (newState != null) {
                    // Используем флаг '2' для установки блока:
                    // - Уведомляет клиентов об изменении (блок меняется визуально).
                    // - НЕ вызывает обновление соседних блоков (предотвращает каскадные лаги).
                    level.setBlock(mutablePos, newState, 2);

                    // Дополнительный эффект: если мы заменили блок, убираем хрупкие блоки (траву, цветы) над ним.
                    BlockPos posAbove = mutablePos.above();
                    BlockState stateAbove = level.getBlockState(posAbove);
                    if (stateAbove.is(Blocks.TALL_GRASS) || stateAbove.is(Blocks.GRASS) || stateAbove.is(BlockTags.FLOWERS) || stateAbove.is(Blocks.SNOW)) {
                        level.setBlock(posAbove, Blocks.AIR.defaultBlockState(), 2);
                    }
                }

                // 3.5: Вне зависимости от того, заменили мы блок или нет (например, если это был камень, для которого нет правила),
                // мы прекращаем "бурение" в этой колонке. Это создает эффект поверхностного разложения.
                break;
            }
        }
    }


    // Пустые реализации
    @Override public void clearSystem(Level level) {}
}