package com.hbm_m.event;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import com.hbm_m.network.ModNetwork;

/**
 * Обработчик FML событий для инициализации сетевых каналов
 * В Forge 1.20.1 каналы ДОЛЖНЫ регистрироваться на FMLCommonSetupEvent,
 * а не в статических блоках или на клиенте!
 */
@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventHandler {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Инициализируем сетевые каналы в правильное время
        ModNetwork.registerChannels();
    }
}