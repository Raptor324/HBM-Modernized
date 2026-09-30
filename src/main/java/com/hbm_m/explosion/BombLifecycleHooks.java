package com.hbm_m.explosion;

import net.minecraft.server.level.ServerLevel;

/**
 * Тонкий глюк шины событий: остановка сервера и выгрузка измерения освобождают
 * пул бомбовых расчётов и зеркало чанков; тик сервера обслуживает очередь
 * потоковой перепечатки чанков кратера. Регистрируется лениво, при первом взрыве.
 */
public final class BombLifecycleHooks {

    private static volatile boolean registered;

    private BombLifecycleHooks() {}

    public static void ensureRegistered() {
        if (registered) return;
        registered = true;
        //? if forge {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(BombLifecycleHooks.class);
        //?} else {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(BombLifecycleHooks.class);
         *///?}
    }

    //? if forge {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            CraterResendScheduler.serverTick();
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        CraterResendScheduler.clear();
        BombForkJoinPool.onServerStopped();
        BlastChunkUtil.onServerStopped();
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel server) {
            BombForkJoinPool.onLevelUnload(server);
        }
    }
    //?} else {
    /*@net.neoforged.bus.api.SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        CraterResendScheduler.serverTick();
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        CraterResendScheduler.clear();
        BombForkJoinPool.onServerStopped();
        BlastChunkUtil.onServerStopped();
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel server) {
            BombForkJoinPool.onLevelUnload(server);
        }
    }
    *///?}
}
