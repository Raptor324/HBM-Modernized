package com.hbm_m.config;
// Обработчик привязки клавиш для открытия экрана конфигурации мода.
// Использует AutoConfig для получения экрана настроек и регистрирует сочетание клавиш

import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.powerarmor.ModPowerArmorItem;
import com.hbm_m.powerarmor.PowerArmorHandlers;
import com.mojang.blaze3d.platform.InputConstants;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

    public static final KeyMapping POWER_ARMOR_DASH = new KeyMapping(
            "key.hbm_m.power_armor_dash",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, // V key for dash
            CATEGORY
    );

    public static final KeyMapping POWER_ARMOR_VATS = new KeyMapping(
            "key.hbm_m.power_armor_vats",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C, // C key for VATS
            CATEGORY
    );

    public static final KeyMapping POWER_ARMOR_THERMAL = new KeyMapping(
            "key.hbm_m.power_armor_thermal",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X, // X key for thermal vision
            CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
        event.register(POWER_ARMOR_DASH);
        event.register(POWER_ARMOR_VATS);
        event.register(POWER_ARMOR_THERMAL);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Обработка открытия конфига
            if (OPEN_CONFIG.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) {
                    // Получаем экран настроек через AutoConfig
                    Screen configScreen = AutoConfig.getConfigScreen(ModClothConfig.class, mc.screen).get();
                    mc.setScreen(configScreen);
                }
            }

            // Обработка dash силовой брони
            if (POWER_ARMOR_DASH.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    // Отправляем пакет на сервер для выполнения dash
                    // TODO: Отправить пакет на сервер для выполнения dash
                    // Пока что просто вызываем локально для тестирования
                    PowerArmorHandlers.performDash(mc.player);
                    OverlayInfoToast.show(Component.translatable("hud.hbm_m.dash.perform"), 60, OverlayInfoToast.ID_DASH, 0x00FF00);
                }
            }

            // Обработка VATS
            if (POWER_ARMOR_VATS.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    if (com.hbm_m.powerarmor.ModEventHandlerClient.isVATSActive()) {
                        com.hbm_m.powerarmor.ModEventHandlerClient.deactivateVATS();
                        OverlayInfoToast.show(Component.translatable("hud.hbm_m.vats.off"), 60, OverlayInfoToast.ID_VATS, 0xFF0000);
                    } else {
                        com.hbm_m.powerarmor.ModEventHandlerClient.activateVATS();
                        OverlayInfoToast.show(Component.translatable("hud.hbm_m.vats.on"), 60, OverlayInfoToast.ID_VATS, 0x00FF00);
                    }                    
                }
            }

            // Обработка thermal vision
            if (POWER_ARMOR_THERMAL.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    if (com.hbm_m.powerarmor.ModEventHandlerClient.isThermalActive()) {
                        com.hbm_m.powerarmor.ModEventHandlerClient.deactivateThermal();
                        OverlayInfoToast.show(Component.translatable("hud.hbm_m.thermal.off"), 60, OverlayInfoToast.ID_THERMAL, 0xFF0000);
                    } else {
                        com.hbm_m.powerarmor.ModEventHandlerClient.activateThermal();
                        OverlayInfoToast.show(Component.translatable("hud.hbm_m.thermal.on"), 60, OverlayInfoToast.ID_THERMAL, 0x00FF00);
                    }                    
                }
            }
        }
    }
}
        
    

