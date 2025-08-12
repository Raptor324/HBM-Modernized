package com.hbm_m.radiation;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;
import com.hbm_m.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


/**
 * ЕДИНЫЙ менеджер радиации, управляющий обработчиками и ВСЕМИ связанными событиями.
 * Этот класс является единственным источником истины для системы радиации.
 */
public class ChunkRadiationManager {

    // Singleton instance. Этот объект должен быть зарегистрирован в FORGE Event Bus.
    public static final ChunkRadiationManager INSTANCE = new ChunkRadiationManager();
    private ChunkRadiationManager() {}

    // Единственный экземпляр обработчика симуляции.
    private static ChunkRadiationHandler proxyInstance;

    /**
     * Глобальная точка доступа к единственному экземпляру обработчика симуляции.
     */
    public static ChunkRadiationHandler getProxy() {
        if (proxyInstance == null) {
            proxyInstance = ModClothConfig.get().usePrismSystem
                    ? new ChunkRadiationHandlerPRISM()
                    : new ChunkRadiationHandlerSimple();
        }
        return proxyInstance;
    }

    private int tickCounter = 0;

    // ОБРАБОТЧИКИ СОБЫТИЙ ЖИЗНЕННОГО ЦИКЛА МИРА 

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (ModClothConfig.get().enableChunkRads) {
            if (event.getLevel() instanceof Level level) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("World load event received for {}", level.dimension().location());
                }
            }
            getProxy().receiveWorldLoad(event);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (ModClothConfig.get().enableChunkRads) {
            if (event.getLevel() instanceof Level level) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("World unload event received for {}", level.dimension().location());
                }
            }
            getProxy().receiveWorldUnload(event);
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        // Мы работаем только на сервере и только с полноценными чанками
        if (ModClothConfig.get().enableChunkRads && !event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            // Передаем чанк в наш обработчик, чтобы он "проснулся"
            getProxy().receiveChunkLoad(chunk);
        }
    }

    // @SubscribeEvent
    // public void onChunkSave(ChunkDataEvent.Save event) {
    //     if (ModClothConfig.get().enableChunkRads) {
    //         getProxy().receiveChunkSave(event);
    //     }
    // }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (ModClothConfig.get().enableChunkRads) {
            getProxy().receiveChunkUnload(event);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads || event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter >= 20) {
            getProxy().updateSystem();
            tickCounter = 0;
            if (ModClothConfig.get().worldRadEffects) {
                getProxy().handleWorldDestruction();
            }
        }
        getProxy().receiveWorldTick(event);
    }

    // ОБРАБОТЧИКИ СОБЫТИЙ ИЗМЕНЕНИЯ БЛОКОВ 

    private float getRadFromState(BlockState state) {
    // Простая проверка для оптимизации: воздушные блоки не могут быть радиоактивными.
    if (state.isAir()) {
        return 0f;
    }

    // 1. Создаем временный ItemStack, представляющий этот блок.
    // Это ключевой шаг для связи мира блоков с нашей предметно-ориентированной HazardSystem.
    ItemStack blockAsStack = new ItemStack(state.getBlock().asItem());

    // 2. Запрашиваем уровень радиации у нашей центральной системы, передавая ей созданный ItemStack.
    // Вся старая логика с `instanceof` заменяется этой одной строкой.
    return HazardSystem.getHazardLevelFromStack(blockAsStack, HazardType.RADIATION);
    }

    private void handleBlockChange(BlockState oldState, BlockState newState, LevelAccessor level, BlockPos pos) {
        if (level.isClientSide() || !(level instanceof Level world)) {
            return;
        }

        float oldRad = getRadFromState(oldState);
        float newRad = getRadFromState(newState);
        float diff = newRad - oldRad;

        // Если произошло реальное изменение радиации, сообщаем симулятору.
        // Это единственный вызов, который должен быть здесь.
        if (Math.abs(diff) > 1e-6f) {
            getProxy().incrementBlockRadiation(world, pos, diff);
        }
    }


    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockSnapshot snapshot = event.getBlockSnapshot();
        BlockState oldState = snapshot.getReplacedBlock();
        BlockState newState = event.getState();
        handleBlockChange(oldState, newState, event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        handleBlockChange(event.getState(), Blocks.AIR.defaultBlockState(), event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        for (BlockPos pos : event.getAffectedBlocks()) {
            // Передаем BlockState до взрыва
            BlockState oldState = level.getBlockState(pos);
            handleBlockChange(oldState, Blocks.AIR.defaultBlockState(), level, pos);
        }
    }

    @SubscribeEvent
    public void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Проверяем, что уровень серверный
        if (event.getEntity().level().isClientSide()) return;
        
        // Используем instanceof для безопасного приведения типов
        if (getProxy() instanceof ChunkRadiationHandlerSimple handler) {
            handler.clearPlayerDebugCache(event.getEntity().getUUID());
        }
    }

    // СТАТИЧЕСКИЕ МЕТОДЫ-ОБЕРТКИ 

    public static float getRadiation(Level level, int x, int y, int z) {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) return 0F;
        return getProxy().getRadiation(level, x, y, z);
    }

    public static void setRadiation(Level level, int x, int y, int z, float rad) {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) return;
        getProxy().setRadiation(level, x, y, z, rad);
    }

    public static void incrementRad(Level level, int x, int y, int z, float rad) {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) return;
        getProxy().incrementRad(level, x, y, z, rad);
    }
}