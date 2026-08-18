package com.hbm_m.api.energy;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

//? if forge {
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "hbm_m")
//?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = "hbm_m")
*///?}
public class ServerLifecycleHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        runRebuild(event.getServer());
    }

    public static void runRebuild(MinecraftServer server) {
        LOGGER.info("[HBM-NETWORK] Server has started, rebuilding energy networks for all dimensions...");
        for (ServerLevel level : server.getAllLevels()) {
            EnergyNetworkManager.get(level).rebuildAllNetworks();
        }
        LOGGER.info("[HBM-NETWORK] Energy network rebuild complete.");
    }
}