package com.hbm_m.radiation;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
//? if forge {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
*///?}


/**
 * ЕДИНЫЙ менеджер радиации, управляющий обработчиками и ВСЕМИ связанными событиями.
 * Этот класс является единственным источником истины для системы радиации.
 */
@SuppressWarnings("UnstableApiUsage")
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
//            proxyInstance = ModClothConfig.get().usePrismSystem
//                    ? new ChunkRadiationHandlerPRISM()
//                    : new ChunkRadiationHandlerSimple();
            proxyInstance = new ChunkRadiationHandlerSimple();
        }
        return proxyInstance;
    }

    private int tickCounter = 0;

    //? if fabric {
    /*public static void initFabric() {
        ServerWorldEvents.LOAD.register((server, level) -> {
            if (ModClothConfig.get().enableChunkRads) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("World load event received for {}", level.dimension().location());
                }
            }
        });

        ServerWorldEvents.UNLOAD.register((server, level) -> {
            if (ModClothConfig.get().enableChunkRads) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("World unload event received for {}", level.dimension().location());
                }
            }
        });

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            if (ModClothConfig.get().enableChunkRads && !level.isClientSide()) {
                getProxy().receiveChunkLoad(chunk);
            }
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            if (ModClothConfig.get().enableChunkRads) {
                if (getProxy() instanceof ChunkRadiationHandlerSimple simple) {
                    simple.receiveChunkUnloadFabric(chunk);
                }
            }
        });

        // updateSystem на START_SERVER_TICK (до мирового тика), handleWorldDestruction на END.
        // См. подробный комментарий в onServerTickStart/onServerTickEnd (Forge-ветка) — тот же
        // фикс для Fabric: при END-фазе получался порядок «emit → decay» и chunk_rad стабильно
        // находился в LOW-фазе (≈146 для polonium вместо ≈221).
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) return;
            INSTANCE.tickCounter++;
            if (INSTANCE.tickCounter >= 20) {
                getProxy().updateSystem();
                INSTANCE.tickCounter = 0;
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads) return;
            if (ModClothConfig.get().worldRadEffects) {
                getProxy().handleWorldDestruction();
            }
        });

        // В 1.7.10 ChunkRadiationManager не отслеживает place/break/explosion — источники переэмиттят сами.

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (getProxy() instanceof ChunkRadiationHandlerSimple handlerSimple) {
                handlerSimple.clearPlayerDebugCache(handler.player.getUUID());
            }
        });
    }
    *///?}

    // ОБРАБОТЧИКИ СОБЫТИЙ ЖИЗНЕННОГО ЦИКЛА МИРА 

    //? if forge {
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

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (ModClothConfig.get().enableChunkRads) {
            getProxy().receiveChunkUnload(event);
        }
    }

    // updateSystem ДОЛЖЕН срабатывать на Phase.START (до мирового тика), а не на Phase.END.
    // Причина: BlockHazard.tick эмиттит радиацию во время мирового тика. Если updateSystem бежит
    // на END (после эмиттера), получается порядок «emit → decay» в одном тике — chunk_rad стабильно
    // находится в LOW-фазе (≈146 для polonium-210 вместо правильных ≈221), и игрок 19/20 тиков
    // видит LOW, получая заниженную дозу. На START декей применяется к ПРЕДЫДУЩЕМУ циклу,
    // а свежая эмиссия остаётся HIGH на весь следующий цикл — как в 1.7.10.
    // handleWorldDestruction и receiveWorldTick остаются на END (видят уже обновлённый чанк).
    @SubscribeEvent
    public void onServerTickStart(TickEvent.ServerTickEvent event) {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads || event.phase != TickEvent.Phase.START) return;
        tickCounter++;
        if (tickCounter >= 20) {
            getProxy().updateSystem();
            tickCounter = 0;
        }
    }

    @SubscribeEvent
    public void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if (!ModClothConfig.get().enableRadiation || !ModClothConfig.get().enableChunkRads || event.phase != TickEvent.Phase.END) return;
        if (ModClothConfig.get().worldRadEffects) {
            getProxy().handleWorldDestruction();
        }
        getProxy().receiveWorldTick(event);
    }
    //?}

    // ОБРАБОТЧИКИ СОБЫТИЙ ИЗМЕНЕНИЯ БЛОКОВ 
    // В 1.7.10 ChunkRadiationManager не отслеживает place/break/explosion для радиации:
    // радиоактивный блок сам переэмиттит радиацию каждый scheduled-tick (20t) через свой updateTick.
    // При ломании блока эмиттер останавливается, а накопленный ambient естественно затухает в updateSystem.

    //? if forge {
    @SubscribeEvent
    public void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Проверяем, что уровень серверный
        if (event.getEntity().level().isClientSide()) return;

        // Используем instanceof для безопасного приведения типов
        if (getProxy() instanceof ChunkRadiationHandlerSimple handler) {
            handler.clearPlayerDebugCache(event.getEntity().getUUID());
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