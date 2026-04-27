package com.hbm_m.config;
// Обработчик привязки клавиш для открытия экрана конфигурации мода.
// Использует AutoConfig для получения экрана настроек и регистрирует сочетание клавиш

import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.inventory.gui.GUIMultiDetonator;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.powerarmor.ModPowerArmorItem;
import com.hbm_m.powerarmor.PowerArmorHandlers;
import com.mojang.blaze3d.platform.InputConstants;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigKeybindHandler {
    public static final String CATEGORY = "key.categories.hbm_m";
    private static boolean INITIALIZED = false;

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.hbm_m.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_0,
            CATEGORY
    );

    public static final KeyMapping POWER_ARMOR_DASH = new KeyMapping(
            "key.hbm_m.power_armor_dash",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, // V key for dash
            CATEGORY
    );

    public static final KeyMapping POWER_ARMOR_VATS = new KeyMapping(
            "key.hbm_m.power_armor_vats",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C, // C key for VATS
            CATEGORY
    );

    public static final KeyMapping POWER_ARMOR_THERMAL = new KeyMapping(
            "key.hbm_m.power_armor_thermal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X, // X key for thermal vision
            CATEGORY
    );

    public static final KeyMapping OPEN_MULTI_DETONATOR = new KeyMapping(
            "key.hbm_m.multi_detonator_open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static void init() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        KeyMappingRegistry.register(OPEN_CONFIG);
        KeyMappingRegistry.register(POWER_ARMOR_DASH);
        KeyMappingRegistry.register(POWER_ARMOR_VATS);
        KeyMappingRegistry.register(POWER_ARMOR_THERMAL);
        KeyMappingRegistry.register(OPEN_MULTI_DETONATOR);

        // Аналог END-фазы ClientTickEvent на Forge: выполняем после стандартного тика клиента.
        ClientTickEvent.CLIENT_POST.register(client -> onClientPostTick());
    }

    private static void onClientPostTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;

        // Обработка открытия конфига
        if (OPEN_CONFIG.consumeClick() && Screen.hasAltDown()) {
            if (mc.screen == null) {
                Screen configScreen = AutoConfig.getConfigScreen(ModClothConfig.class, mc.screen).get();
                mc.setScreen(configScreen);
            }
        }

        // Обработка dash силовой брони
        if (POWER_ARMOR_DASH.consumeClick()) {
            if (mc.player != null && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                var chestplate = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                if (chestplate.getItem() instanceof ModPowerArmorItem armorItem) {
                    var specs = armorItem.getSpecs();
                    if (specs.dashCount > 0) {
                        // TODO: Отправить пакет на сервер для выполнения dash
                        PowerArmorHandlers.performDash(mc.player);
                        OverlayInfoToast.show(Component.translatable("hud.hbm_m.dash.perform"), 60, OverlayInfoToast.ID_DASH, 0x00FF00);
                    }
                }
            }
        }

        // Обработка VATS
        if (POWER_ARMOR_VATS.consumeClick()) {
            if (mc.player != null && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                var chestplate = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                if (chestplate.getItem() instanceof ModPowerArmorItem armorItem) {
                    var specs = armorItem.getSpecs();
                    if (specs.hasVats) {
                        if (com.hbm_m.powerarmor.ModEventHandlerClient.isVATSActive()) {
                            com.hbm_m.powerarmor.ModEventHandlerClient.deactivateVATS();
                            OverlayInfoToast.show(Component.translatable("hud.hbm_m.vats.off"), 60, OverlayInfoToast.ID_VATS, 0xFF0000);
                        } else {
                            com.hbm_m.powerarmor.ModEventHandlerClient.activateVATS();
                            OverlayInfoToast.show(Component.translatable("hud.hbm_m.vats.on"), 60, OverlayInfoToast.ID_VATS, 0x00FF00);
                        }
                    }
                }
            }
        }

        // Обработка thermal vision
        if (POWER_ARMOR_THERMAL.consumeClick()) {
            if (mc.player != null && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                var chestplate = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                if (chestplate.getItem() instanceof ModPowerArmorItem armorItem) {
                    var specs = armorItem.getSpecs();
                    if (specs.hasThermal) {
                        if (com.hbm_m.powerarmor.ModEventHandlerClient.isThermalActive()) {
                            com.hbm_m.powerarmor.ModEventHandlerClient.deactivateThermal();
                            OverlayInfoToast.show(Component.translatable("hud.hbm_m.thermal.off"), 60, OverlayInfoToast.ID_THERMAL, 0xFF0000);
                        } else {
                            com.hbm_m.powerarmor.ModEventHandlerClient.activateThermal();
                            // If activation was blocked by first-use warning, do not show "ON" toast.
                            if (com.hbm_m.powerarmor.ModEventHandlerClient.isThermalActive()) {
                                OverlayInfoToast.show(Component.translatable("hud.hbm_m.thermal.on"), 60, OverlayInfoToast.ID_THERMAL, 0x00FF00);
                            }
                        }
                    }
                }
            }
        }

        // Multi-detonator GUI (R)
        if (OPEN_MULTI_DETONATOR.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                var player = mc.player;
                var main = player.getMainHandItem();
                var off = player.getOffhandItem();
                if (main.getItem() instanceof MultiDetonatorItem) {
                    mc.setScreen(new GUIMultiDetonator(main));
                } else if (off.getItem() instanceof MultiDetonatorItem) {
                    mc.setScreen(new GUIMultiDetonator(off));
                }
            }
        }
    }
}
        
    

