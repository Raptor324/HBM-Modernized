package com.hbm_m.api.energy;

import com.hbm_m.api.energy.EnergyNetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "hbm_m") // Замени на свой modid
public class EnergyNetworkTickHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Тикаем сети во всех измерениях
            for (ServerLevel level : event.getServer().getAllLevels()) {
                EnergyNetworkManager.get(level).tick();
            }
        }
    }
}