package com.hbm_m.client;

import com.hbm_m.client.overlay.GeigerOverlay;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    private static final Logger LOGGER = LogManager.getLogger(RefStrings.MODID);

    // ИСПРАВЛЕНО: Лямбда теперь вызывает новый метод GeigerOverlay.onRenderOverlay
    public static final IGuiOverlay GEIGER_HUD_OVERLAY = (gui, graphics, partialTick, screenWidth, screenHeight) -> {
        // Просто вызываем статический метод GeigerOverlay с нужными параметрами
        GeigerOverlay.onRenderOverlay(graphics, partialTick, screenWidth, screenHeight);
    };


    public ClientSetup() {
        
        // Регистрация на FORGE Event Bus (для RegisterGuiOverlaysEvent)
        MinecraftForge.EVENT_BUS.addListener(this::registerGuiOverlays);
        MainRegistry.LOGGER.info("!!! ClientSetup constructor called, Forge listeners registered !!!");
    }

    public static void onCreativeModeTabBuild(BuildCreativeModeTabContentsEvent event) {
        // ...existing code...
    }
    
    public static void onClientSetupEvent(final FMLClientSetupEvent event) {
        // Здесь можно добавить любую другую клиентскую инициализацию
        MainRegistry.LOGGER.info("!!! FMLClientSetupEvent fired in ClientSetup !!!");
        // Регистрируем Overlay для Geiger Counter
        // Overlay registration is handled in registerGuiOverlays via RegisterGuiOverlaysEvent.
        // No need to register overlays here.
        MinecraftForge.EVENT_BUS.register(com.hbm_m.client.ChunkRadiationDebugRenderer.class);
        // Регистрируем обработчик клавиш для открытия меню конфигурации
        MinecraftForge.EVENT_BUS.register(ModConfigKeybindHandler.class);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ModConfigKeybindHandler.onRegisterKeyMappings(event);
    }

    static {
        LOGGER.info("[GeigerOverlayDebug] ClientSetup class loaded");
    }

    @SubscribeEvent
    public void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        LOGGER.info("[GeigerOverlayDebug] RegisterGuiOverlaysEvent triggered");
        LOGGER.info("[GeigerOverlay] Registering Geiger Counter Overlay...");
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "geiger_counter_hud", GEIGER_HUD_OVERLAY);
        LOGGER.info("[GeigerOverlay] Geiger Counter Overlay registered.");
        LOGGER.info("[GeigerOverlayDebug] GeigerCounterOverlay registered via registerBelowAll");
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("geiger_counter", new GeigerCounterOverlay());
    }
}