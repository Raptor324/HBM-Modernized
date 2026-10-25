package com.hbm_m.handler;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.config.ModConfigKeybindHandler;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.interfaces.IKeybindReceiver;
import com.hbm_m.network.KeybindPacket;
import com.hbm_m.network.ModPacketHandler;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port von {@code com.hbm.handler.HbmKeybinds} (1.7.10, Kategorie {@code hbm.key}): alle
 * Tasten des Originals mit ihren Standardbelegungen. Wie im Original meldet der Client jede
 * Zustandsaenderung einer {@link EnumKeybind} ueber das {@link KeybindPacket} an den Server und
 * spiegelt sie in seine eigenen {@link HbmPlayerProps}.
 *
 * <p>Die Kran-Tasten sind die bereits vorhandenen {@link ModConfigKeybindHandler#RBMK_CRANE_UP}
 * usw. (gleiche Standardbelegung), damit es keine doppelten Eintraege gibt. Maus-Tasten des
 * Originals ({@code -100/-99/-98}) sind die linke/rechte/mittlere Maustaste.</p>
 */
public final class HbmKeybinds {

    private HbmKeybinds() {}

    public static final String CATEGORY = "hbm.key";

    public static final KeyMapping calculatorKey = key("calculator", GLFW.GLFW_KEY_N);
    public static final KeyMapping jetpackKey = key("toggleBack", GLFW.GLFW_KEY_C);
    public static final KeyMapping magnetKey = key("toggleMagnet", GLFW.GLFW_KEY_Z);
    public static final KeyMapping hudKey = key("toggleHUD", GLFW.GLFW_KEY_V);
    public static final KeyMapping dashKey = key("dash", GLFW.GLFW_KEY_LEFT_SHIFT);
    public static final KeyMapping trainKey = key("trainInv", GLFW.GLFW_KEY_R);
    public static final KeyMapping qmaw = key("qmaw", GLFW.GLFW_KEY_F1);
    public static final KeyMapping abilityCycle = mouse("ability", GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    public static final KeyMapping abilityAlt = key("abilityAlt", GLFW.GLFW_KEY_LEFT_ALT);
    public static final KeyMapping copyToolAlt = key("copyToolAlt", GLFW.GLFW_KEY_LEFT_ALT);
    public static final KeyMapping copyToolCtrl = key("copyToolCtrl", GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyMapping reloadKey = key("reload", GLFW.GLFW_KEY_R);
    public static final KeyMapping gunPrimaryKey = mouse("gunPrimary", GLFW.GLFW_MOUSE_BUTTON_LEFT);
    public static final KeyMapping gunSecondaryKey = mouse("gunSecondary", GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    public static final KeyMapping gunTertiaryKey = mouse("gunTertitary", GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(CATEGORY + "." + name, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    private static KeyMapping mouse(String name, int button) {
        return new KeyMapping(CATEGORY + "." + name, InputConstants.Type.MOUSE, button, CATEGORY);
    }

    public static void registerAll(java.util.function.Consumer<KeyMapping> registrar) {
        registrar.accept(calculatorKey);
        registrar.accept(jetpackKey);
        registrar.accept(magnetKey);
        registrar.accept(hudKey);
        registrar.accept(dashKey);
        registrar.accept(trainKey);
        registrar.accept(qmaw);
        registrar.accept(reloadKey);
        registrar.accept(gunPrimaryKey);
        registrar.accept(gunSecondaryKey);
        registrar.accept(gunTertiaryKey);
        registrar.accept(abilityCycle);
        registrar.accept(abilityAlt);
        registrar.accept(copyToolAlt);
        registrar.accept(copyToolCtrl);
    }

    /** Original {@code ClientProxy.getIsKeyPressed}. */
    public static boolean getIsKeyPressed(EnumKeybind key) {
        return switch (key) {
            case JETPACK -> Minecraft.getInstance().options.keyJump.isDown();
            case TOGGLE_JETPACK -> jetpackKey.isDown();
            case TOGGLE_MAGNET -> magnetKey.isDown();
            case TOGGLE_HEAD -> hudKey.isDown();
            case RELOAD -> reloadKey.isDown();
            case DASH -> dashKey.isDown();
            case TRAIN -> trainKey.isDown();
            case CRANE_UP -> ModConfigKeybindHandler.RBMK_CRANE_UP.isDown();
            case CRANE_DOWN -> ModConfigKeybindHandler.RBMK_CRANE_DOWN.isDown();
            case CRANE_LEFT -> ModConfigKeybindHandler.RBMK_CRANE_LEFT.isDown();
            case CRANE_RIGHT -> ModConfigKeybindHandler.RBMK_CRANE_RIGHT.isDown();
            case CRANE_LOAD -> ModConfigKeybindHandler.RBMK_CRANE_LOAD.isDown();
            case ABILITY_CYCLE -> abilityCycle.isDown();
            case ABILITY_ALT -> abilityAlt.isDown();
            case TOOL_ALT -> copyToolAlt.isDown();
            case TOOL_CTRL -> copyToolCtrl.isDown();
            case GUN_PRIMARY -> gunPrimaryKey.isDown();
            case GUN_SECONDARY -> gunSecondaryKey.isDown();
            case GUN_TERTIARY -> gunTertiaryKey.isDown();
        };
    }

    /**
     * Original {@code handleProps} + {@code postClientTick}: jede Aenderung wird gesendet. 1.20
     * liefert Tasten nicht mehr als Ereignisstrom mit Zustand, darum wird pro Clienttick verglichen.
     */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        HbmPlayerProps props = HbmPlayerProps.get(player);

        for (EnumKeybind key : EnumKeybind.values()) {
            boolean last = props.getKeyPressed(key);
            boolean current = mc.screen == null && getIsKeyPressed(key);
            if (last != current) {
                props.setKeyPressed(key, current);
                ModPacketHandler.sendToServer(ModPacketHandler.KEYBIND, new KeybindPacket(key, current));
                onPressedClient(player, key, current);
            }
        }

        if (calculatorKey.consumeClick() && mc.screen == null) {
            com.hbm_m.inventory.gui.GUICalculator.open();
        }
    }

    public static void onPressedClient(Player player, EnumKeybind key, boolean state) {
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof IKeybindReceiver rec) {
            if (rec.canHandleKeybind(player, held, key)) rec.handleKeybindClient(player, held, key, state);
        }
    }
}
