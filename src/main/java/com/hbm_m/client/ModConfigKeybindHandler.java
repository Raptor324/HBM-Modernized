package com.hbm_m.client;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.lib.RefStrings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

//import me.shedaniel.clothconfig2.api.ConfigBuilder;
//import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.autoconfig.AutoConfig;
//import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
public class ModConfigKeybindHandler {
    public static final String CATEGORY = "key.categories.hbm_m";
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.hbm_m.open_config",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_0,
            CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && OPEN_CONFIG.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                // Получаем экран настроек через AutoConfig
                Screen configScreen = AutoConfig.getConfigScreen(ModClothConfig.class, mc.screen).get();
                mc.setScreen(configScreen);
            }
        }
    }
}
        
    

