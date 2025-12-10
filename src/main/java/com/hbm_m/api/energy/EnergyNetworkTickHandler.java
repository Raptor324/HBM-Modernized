package com.hbm_m.api.energy;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "hbm_m")
public class EnergyNetworkTickHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // Логируем ВСЕ вызовы, даже неправильные фазы, чтобы понять масштаб бедствия
        // Но чтобы не убить лог, выводим только если время делится на 40 (раз в 2 сек)
        // ИЛИ если это критический момент

        if (event.level.isClientSide) return;

        // Фильтр, чтобы спамило только раз в секунду (20 тиков), иначе консоль разорвет
        boolean isDebugTick = event.level.getGameTime() % 20 == 0;

        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {

            if (isDebugTick) {
                LOGGER.info("[DEBUG-TICK] Ticking Network for Level: {} | Time: {}",
                        serverLevel.dimension().location(),
                        event.level.getGameTime());
            }

            EnergyNetworkManager.get(serverLevel).tick();
        }
    }
}