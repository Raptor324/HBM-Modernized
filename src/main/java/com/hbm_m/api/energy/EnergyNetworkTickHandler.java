//? if forge {
package com.hbm_m.api.energy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "hbm_m")
public class EnergyNetworkTickHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide) return;

        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).tick();
        }
    }
}
//?}
//? if fabric {
/*package com.hbm_m.api.energy;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;

public class EnergyNetworkTickHandler {

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register((ServerLevel level) -> EnergyNetworkManager.get(level).tick());
    }
}
*///?}