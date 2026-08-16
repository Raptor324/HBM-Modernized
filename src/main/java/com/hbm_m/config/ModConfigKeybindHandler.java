package com.hbm_m.config;
// Обработчик привязки клавиш для открытия экрана конфигурации мода.
// Использует AutoConfig для получения экрана настроек и регистрирует сочетание клавиш

import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.inventory.gui.GUIMultiDetonator;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.powerarmor.ModPowerArmorItem;
import com.hbm_m.powerarmor.PowerArmorClientState;
import com.hbm_m.powerarmor.PowerArmorHandlers;
import com.mojang.blaze3d.platform.InputConstants;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if forge || neoforge {
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
//?}

public class ModConfigKeybindHandler {
    public static final String CATEGORY = "key.categories.hbm_m";
    private static boolean INITIALIZED = false;

    public static final KeyMapping OPEN_CONFIG =
    //? if forge || neoforge {
            new KeyMapping(
                    "key.hbm_m.open_config",
                    KeyConflictContext.UNIVERSAL,
                    KeyModifier.ALT,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_0,
                    CATEGORY
            );
    //?} else {
    /*        new KeyMapping(
                    "key.hbm_m.open_config",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_0,
                    CATEGORY
            );*///?}

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

    // RBMK crane console controls (1:1 with the original's EnumKeybind.CRANE_UP/DOWN/LEFT/RIGHT/LOAD) -
    // held continuously while standing in a crane's detection zone, reported to the server every
    // tick the combined state changes (see onClientPostTick below).
    public static final KeyMapping RBMK_CRANE_UP = new KeyMapping(
            "key.hbm_m.rbmk_crane_up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, CATEGORY);
    public static final KeyMapping RBMK_CRANE_DOWN = new KeyMapping(
            "key.hbm_m.rbmk_crane_down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, CATEGORY);
    public static final KeyMapping RBMK_CRANE_LEFT = new KeyMapping(
            "key.hbm_m.rbmk_crane_left", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, CATEGORY);
    public static final KeyMapping RBMK_CRANE_RIGHT = new KeyMapping(
            "key.hbm_m.rbmk_crane_right", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, CATEGORY);
    public static final KeyMapping RBMK_CRANE_LOAD = new KeyMapping(
            "key.hbm_m.rbmk_crane_load", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY);

    private static boolean lastCraneUp, lastCraneDown, lastCraneLeft, lastCraneRight, lastCraneLoad;

    public static void init() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        KeyMappingRegistry.register(OPEN_CONFIG);
        KeyMappingRegistry.register(POWER_ARMOR_DASH);
        KeyMappingRegistry.register(POWER_ARMOR_VATS);
        KeyMappingRegistry.register(POWER_ARMOR_THERMAL);
        KeyMappingRegistry.register(OPEN_MULTI_DETONATOR);
        KeyMappingRegistry.register(RBMK_CRANE_UP);
        KeyMappingRegistry.register(RBMK_CRANE_DOWN);
        KeyMappingRegistry.register(RBMK_CRANE_LEFT);
        KeyMappingRegistry.register(RBMK_CRANE_RIGHT);
        KeyMappingRegistry.register(RBMK_CRANE_LOAD);

        // Аналог END-фазы ClientTickEvent на Forge: выполняем после стандартного тика клиента.
        ClientTickEvent.CLIENT_POST.register(client -> onClientPostTick());
    }

    private static void onClientPostTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;

        // Обработка открытия конфига
        if (OPEN_CONFIG.consumeClick()
        //? if fabric {
                /*&& Screen.hasAltDown()
        *///?}
        ) {
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
                        if (PowerArmorClientState.isVATSActive()) {
                            PowerArmorClientState.deactivateVATS();
                            OverlayInfoToast.show(Component.translatable("hud.hbm_m.vats.off"), 60, OverlayInfoToast.ID_VATS, 0xFF0000);
                        } else {
                            PowerArmorClientState.activateVATS();
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
                        if (PowerArmorClientState.isThermalActive()) {
                            PowerArmorClientState.deactivateThermal();
                            OverlayInfoToast.show(Component.translatable("hud.hbm_m.thermal.off"), 60, OverlayInfoToast.ID_THERMAL, 0xFF0000);
                        } else {
                            PowerArmorClientState.activateThermal();
                            // If activation was blocked by first-use warning, do not show "ON" toast.
                            if (PowerArmorClientState.isThermalActive()) {
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

        // RBMK crane control: report held state to the server only when it changes.
        boolean up    = RBMK_CRANE_UP.isDown();
        boolean down  = RBMK_CRANE_DOWN.isDown();
        boolean left  = RBMK_CRANE_LEFT.isDown();
        boolean right = RBMK_CRANE_RIGHT.isDown();
        boolean load  = RBMK_CRANE_LOAD.isDown();
        if (mc.player != null && (up != lastCraneUp || down != lastCraneDown || left != lastCraneLeft
                || right != lastCraneRight || load != lastCraneLoad)) {
            lastCraneUp = up; lastCraneDown = down; lastCraneLeft = left; lastCraneRight = right; lastCraneLoad = load;
            com.hbm_m.network.RBMKCraneControlPacket.send(up, down, left, right, load);
        }
    }
}
        
    

