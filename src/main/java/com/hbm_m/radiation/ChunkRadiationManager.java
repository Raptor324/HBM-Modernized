package com.hbm_m.radiation;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

//? if forge {
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

//? if neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?}

//? if fabric {
/*import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
*///?}

/**
 * ЕДИНЫЙ менеджер радиации, управляющий обработчиками и ВСЕМИ связанными событиями.
 * Использует Architectury API для логики тиков/миров и загрузчики для событий чанков.
 */
//? if forge {
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID)
//?}
//? if neoforge {
/*@EventBusSubscriber(modid = MainRegistry.MOD_ID)
*///?}
public class ChunkRadiationManager {

    public static final ChunkRadiationManager INSTANCE = new ChunkRadiationManager();
    private ChunkRadiationManager() {}

    private static ChunkRadiationHandler proxyInstance;

    public static ChunkRadiationHandler getProxy() {
        if (proxyInstance == null) {
            proxyInstance = new ChunkRadiationHandlerSimple();
        }
        return proxyInstance;
    }

    private static int tickCounter = 0;
    private static boolean registered = false;

    /**
     * Кроссплатформенная инициализация событий через Architectury API + события чанков.
     */
    public static void init() {
        if (registered) return;
        registered = true;

        // 1. Старт тика сервера (updateSystem) — заменяет ServerTickEvent.Phase.START
        TickEvent.SERVER_PRE.register(server -> {
            if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) return;
            tickCounter++;
            if (tickCounter >= 20) {
                getProxy().updateSystem();
                tickCounter = 0;
            }
        });

        // 2. Конец тика сервера (handleWorldDestruction) — заменяет ServerTickEvent.Phase.END
        TickEvent.SERVER_POST.register(server -> {
            if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) return;
            if (ModClothConfig.get().worldRadEffects) {
                getProxy().handleWorldDestruction();
            }
        });

        // 3. Выход игрока с сервера (очистка кэша отладки)
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player.level().isClientSide()) return;
            if (getProxy() instanceof ChunkRadiationHandlerSimple handler) {
                handler.clearPlayerDebugCache(player.getUUID());
            }
        });

        // 4. Жизненный цикл миров
        LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> {
            if (ModClothConfig.get().enableChunkRads) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("World load event received for {}", level.dimension().location());
                }
            }
        });

        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level -> {
            if (ModClothConfig.get().enableChunkRads) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("World unload event received for {}", level.dimension().location());
                }
                getProxy().clearSystem(level);
            }
        });

        // 5. События чанков для Fabric
        //? if fabric {
        /*ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            if (ModClothConfig.get().enableChunkRads && !level.isClientSide()) {
                getProxy().receiveChunkLoad(chunk);
            }
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            if (ModClothConfig.get().enableChunkRads && !level.isClientSide()) {
                if (getProxy() instanceof ChunkRadiationHandlerSimple handler) {
                    handler.receiveChunkUnloadFabric(chunk);
                }
            }
        });
        *///?}
    }

    // События чанков для Forge и NeoForge
    //? if forge || neoforge {
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (ModClothConfig.get().enableChunkRads && !event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            getProxy().receiveChunkLoad(chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (ModClothConfig.get().enableChunkRads && !event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            //? if forge {
            getProxy().receiveChunkUnload(event);
            //?} else if neoforge {
            /*if (getProxy() instanceof ChunkRadiationHandlerSimple handler) {
                handler.receiveChunkUnloadFabric(chunk);
            }
            *///?}
        }
    }
    //?}

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