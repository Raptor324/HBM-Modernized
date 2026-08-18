package com.hbm_m.api.energy;

import net.minecraft.server.level.ServerLevel;

//? if forge {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "hbm_m")
public class EnergyNetworkTickHandler {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide) return;

        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).tick();
        }
    }
}
//?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = "hbm_m")
public class EnergyNetworkTickHandler {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).tick();
        }
    }
}
*///?}