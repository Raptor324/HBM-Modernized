package com.hbm_m.radiation;

import com.hbm_m.block.RadioactiveBlock;
import com.hbm_m.config.RadiationConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;

import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.util.Mth;

/**
 * Менеджер радиации, управляющий обработчиками радиации и обрабатывающий события
 */
public class ChunkRadiationManager {

    // Singleton instance
    public static final ChunkRadiationManager INSTANCE = new ChunkRadiationManager();

    public static final ChunkRadiationHandler proxy = RadiationConfig.usePrismSystem
            ? new ChunkRadiationHandlerPRISM()
            : new ChunkRadiationHandlerSimple();

    // Счетчик тиков для периодического обновления
    private int tickCounter = 0;

    /**
     * Обработчик события загрузки мира
     */
    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (RadiationConfig.enableChunkRads) {
            if (event.getLevel() instanceof net.minecraft.world.level.Level) {
                net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) event.getLevel();
                MainRegistry.LOGGER.debug("World load event received for {}", level.dimension().location());
            }
            proxy.receiveWorldLoad(event);
        }
    }

    /**
     * Обработчик события выгрузки мира
     */
    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (RadiationConfig.enableChunkRads) {
            if (event.getLevel() instanceof net.minecraft.world.level.Level) {
                net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) event.getLevel();
                MainRegistry.LOGGER.debug("World unload event received for {}", level.dimension().location());
            }
            proxy.receiveWorldUnload(event);
        }
    }

    /**
     * Обработчик события загрузки чанка
     */
    @SubscribeEvent
    public void onChunkLoad(ChunkDataEvent.Load event) {
        if (RadiationConfig.enableChunkRads) {
            proxy.receiveChunkLoad(event);
        }
    }

    /**
     * Обработчик события сохранения чанка
     */
    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save event) {
        if (RadiationConfig.enableChunkRads) {
            proxy.receiveChunkSave(event);
        }
    }

    /**
     * Обработчик события выгрузки чанка
     */
    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (RadiationConfig.enableChunkRads) {
            proxy.receiveChunkUnload(event);
        }
    }

    /**
     * Обработчик тика сервера, обновляет систему радиации
     */
    @SubscribeEvent
    public void updateSystem(TickEvent.ServerTickEvent event) {
        if (RadiationConfig.enableChunkRads && event.phase == TickEvent.Phase.END) {
            tickCounter++;

            // Обновляем систему радиации каждые 20 тиков (1 секунда), как в старом HBM
            if (tickCounter >= 20) {
                proxy.updateSystem();
                tickCounter = 0;

                // Если включены эффекты радиации на мир, обрабатываем их
                if (RadiationConfig.worldRadEffects) {
                    proxy.handleWorldDestruction();
                }
            }

            // Передаем тик в обработчик радиации
            proxy.receiveWorldTick(event);
        }
    }

    /**
     * Получить уровень радиации в указанной позиции
     */
    public static float getRadiation(net.minecraft.world.level.Level level, int x, int y, int z) {
        float rad = proxy.getRadiation(level, x, y, z);
        MainRegistry.LOGGER.debug("ChunkRadiationManager: getRadiation for pos ({}, {}, {}): {}", x, y, z, rad);
        return rad;
    }

    /**
     * Установить уровень радиации в указанной позиции
     */
    public static void setRadiation(net.minecraft.world.level.Level level, int x, int y, int z, float rad) {
        proxy.setRadiation(level, x, y, z, rad);
    }

    /**
     * Увеличить уровень радиации в указанной позиции
     */
    public static void incrementRad(net.minecraft.world.level.Level level, int x, int y, int z, float rad) {
        proxy.incrementRad(level, x, y, z, rad);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        MainRegistry.LOGGER.debug("onBlockPlace called: {} at {}", event.getPlacedBlock().getBlock().getClass().getName(), event.getPos());
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            ChunkPos pos = new ChunkPos(event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
            boolean isRadioactive = event.getPlacedBlock().getBlock() instanceof RadioactiveBlock;
            MainRegistry.LOGGER.debug("is RadioactiveBlock: {}", isRadioactive);
            if (proxy instanceof ChunkRadiationHandlerSimple) { // Проверяем, что используется SimpleHandler
                if (isRadioactive) {
                    ChunkRadiationHandlerSimple.getChunkRadiationCap((LevelChunk) level.getChunk(pos.x, pos.z)).ifPresent(cap -> {
                        float currentBlockRad = cap.getBlockRadiation();
                        float addedRad = ((RadioactiveBlock) event.getPlacedBlock().getBlock()).getRadiationLevel();
                        float newBlockRad = Mth.clamp(currentBlockRad + addedRad, 0, RadiationConfig.maxRad);
                        cap.setBlockRadiation(newBlockRad);
                        ((LevelChunk) level.getChunk(pos.x, pos.z)).setUnsaved(true);
                        MainRegistry.LOGGER.debug("RadioactiveBlock placed at {}. Chunk block radiation updated from {} to {}.", event.getPos(), currentBlockRad, newBlockRad);

                        // Отправляем отладочный пакет всем игрокам в режиме отладки
                        for (net.minecraft.server.level.ServerPlayer player : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                            if (player.level() == level && (player.isCreative() || player.isSpectator())) {
                                ModPacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.hbm_m.network.ChunkRadiationDebugPacket(pos.x, pos.z, cap.getRadiation()));
                            }
                        }
                    });
                }
            }
            // ...existing code...
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            ChunkPos pos = new ChunkPos(event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
            if (proxy instanceof ChunkRadiationHandlerSimple) { // Проверяем, что используется SimpleHandler
                if (event.getState().getBlock() instanceof RadioactiveBlock radioactiveBlock) {
                    ChunkRadiationHandlerSimple.getChunkRadiationCap((LevelChunk) level.getChunk(pos.x, pos.z)).ifPresent(cap -> {
                        float currentBlockRad = cap.getBlockRadiation();
                        float brokenBlockRad = radioactiveBlock.getRadiationLevel();
                        float newBlockRad = Math.max(0, currentBlockRad - brokenBlockRad);
                        cap.setBlockRadiation(newBlockRad);
                        // Удалено: if (newBlockRad < 1e-8F) { cap.setEnvRadiation(0.0F); }
                        ((LevelChunk) level.getChunk(pos.x, pos.z)).setUnsaved(true);
                        MainRegistry.LOGGER.debug("RadioactiveBlock broken at {}. Chunk block radiation updated from {} to {}.", event.getPos(), currentBlockRad, newBlockRad);

                        // Отправляем отладочный пакет всем игрокам в режиме отладки
                        for (net.minecraft.server.level.ServerPlayer player : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                            if (player.level() == level && (player.isCreative() || player.isSpectator())) {
                                ModPacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.hbm_m.network.ChunkRadiationDebugPacket(pos.x, pos.z, cap.getRadiation()));
                            }
                        }
                    });
                }
            }
            // ...existing code...
        }
    }

    // Аналогично добавьте обработчики для изменения контейнеров и выброшенных предметов
}