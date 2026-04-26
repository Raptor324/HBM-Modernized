package com.hbm_m.api.energy;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/*
 * Этот обработчик запускает перестройку энергосетей
 * ОДИН РАЗ, когда сервер полностью загрузился.
 * Это предотвращает дедлок при загрузке мира.
 */

//? if forge {
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

//? if fabric {
/*import dev.architectury.event.events.common.LifecycleEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = "hbm_m")
 //?}
public class ServerLifecycleHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    //? if fabric {
    /*public static void init() {
        LifecycleEvent.SERVER_STARTED.register(ServerLifecycleHandler::runRebuild);
    }
    *///?}

    //? if forge {
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        runRebuild(event.getServer());
    }
    //?}

    public static void runRebuild(MinecraftServer server) {
        LOGGER.info("[HBM-NETWORK] Server has started, rebuilding energy networks for all dimensions...");
        for (ServerLevel level : server.getAllLevels()) {
            EnergyNetworkManager.get(level).rebuildAllNetworks();
        }
        LOGGER.info("[HBM-NETWORK] Energy network rebuild complete.");
    }
}