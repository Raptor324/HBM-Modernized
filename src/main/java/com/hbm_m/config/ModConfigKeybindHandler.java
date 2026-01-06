package com.hbm_m.config;
// Обработчик привязки клавиш для открытия экрана конфигурации мода.
// Использует AutoConfig для получения экрана настроек и регистрирует сочетание клавиш

import com.hbm_m.client.overlay.OverlayVATS;
import com.hbm_m.client.overlay.OverlayThermal;
import com.hbm_m.event.PowerArmorMovementHandler;
import com.hbm_m.item.armor.ModPowerArmorItem;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import me.shedaniel.autoconfig.AutoConfig;

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
                    PowerArmorMovementHandler.performDash(mc.player);
                }
            }

            // Обработка VATS
            if (POWER_ARMOR_VATS.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    if (OverlayVATS.isVATSActive()) {
                        OverlayVATS.deactivateVATS();
                    } else {
                        OverlayVATS.activateVATS();
                    }
                }
            }

            // Обработка thermal vision
            if (POWER_ARMOR_THERMAL.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    if (OverlayThermal.isThermalActive()) {
                        OverlayThermal.deactivateThermal();
                    } else {
                        OverlayThermal.activateThermal();
                    }
                }
            }
        }
    }
}
        
    

