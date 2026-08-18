package com.hbm_m.config;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.inventory.gui.GUIMultiDetonator;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.powerarmor.ModPowerArmorItem;
import com.hbm_m.powerarmor.PowerArmorClientState;
import com.hbm_m.powerarmor.PowerArmorHandlers;
import com.mojang.blaze3d.platform.InputConstants;

import dev.architectury.event.events.client.ClientTickEvent;
import com.hbm_m.client.gui.ConfigScreen;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class ModConfigKeybindHandler {
    public static final String CATEGORY = "key.categories.hbm_m";
    private static boolean INITIALIZED = false;

    public static final KeyMapping OPEN_CONFIG =
    //? if forge {
            new KeyMapping(
                    "key.hbm_m.open_config",
                    net.minecraftforge.client.settings.KeyConflictContext.UNIVERSAL,
                    net.minecraftforge.client.settings.KeyModifier.ALT,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_0,
                    CATEGORY
            );
    //?} elif neoforge {
            /*new KeyMapping(
                    "key.hbm_m.open_config",
                    net.neoforged.neoforge.client.settings.KeyConflictContext.UNIVERSAL,
                    net.neoforged.neoforge.client.settings.KeyModifier.ALT,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_0,
                    CATEGORY
            );
    *///?} else {
    /*        new KeyMapping(
                    "key.hbm_m.open_config",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_0,
                    CATEGORY
            );*///?}

    public static final KeyMapping POWER_ARMOR_DASH = new KeyMapping(
            "key.hbm_m.power_armor_dash",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static final KeyMapping POWER_ARMOR_VATS = new KeyMapping(
            "key.hbm_m.power_armor_vats",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    public static final KeyMapping POWER_ARMOR_THERMAL = new KeyMapping(
            "key.hbm_m.power_armor_thermal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
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

        //? if fabric {
        /*// Fabric: KeyBindingHelper-обвязка Architectury, регистрация в любой момент валидна.
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
        *///?}

        // Аналог END-фазы ClientTickEvent на Forge: выполняем после стандартного тика клиента.
        ClientTickEvent.CLIENT_POST.register(client -> onClientPostTick());
    }

    /**
     * Регистрация биндов на Forge/NeoForge — ТОЛЬКО из RegisterKeyMappingsEvent
     * (см. ClientSetup.onRegisterKeyMappings). Регистрация из FMLClientSetupEvent
     * слишком поздна: на Forge 1.20.1 событие уже прошло, а на NeoForge 1.21.1
     * стреляет раньше client setup. Architectury KeyMappingRegistry в обоих случаях
     * лишь добавляет бинд в options.keyMappings в обход события — таблица ввода
     * (KeyMapping.MAP) не пересобирается, нажатия не приходят в consumeClick,
     * а сам бинд не появляется в настройках управления.
     */
    public static void registerAll(java.util.function.Consumer<KeyMapping> registrar) {
        registrar.accept(OPEN_CONFIG);
        registrar.accept(POWER_ARMOR_DASH);
        registrar.accept(POWER_ARMOR_VATS);
        registrar.accept(POWER_ARMOR_THERMAL);
        registrar.accept(OPEN_MULTI_DETONATOR);
    }

    private static void onClientPostTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;

        // Обработка открытия конфига
        if (OPEN_CONFIG.consumeClick()
        //? if fabric {
                /*&& net.minecraft.client.gui.screens.Screen.hasAltDown()
        *///?}
        ) {
            if (mc.screen == null) {
                mc.setScreen(new ConfigScreen());
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