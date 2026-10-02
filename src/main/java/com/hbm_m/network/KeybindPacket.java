package com.hbm_m.network;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.interfaces.IKeybindReceiver;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code KeybindPacket} + {@code HbmKeybindsServer.onPressedServer}: der Client meldet jede
 * Zustandsaenderung einer HBM-Taste; der Server setzt sie in {@link HbmPlayerProps} und reicht sie an
 * einen {@link IKeybindReceiver} in der Hand weiter.
 */
public class KeybindPacket implements C2SPacket {

    private final int key;
    private final boolean pressed;

    public KeybindPacket(EnumKeybind key, boolean pressed) {
        this(key.ordinal(), pressed);
    }

    private KeybindPacket(int key, boolean pressed) {
        this.key = key;
        this.pressed = pressed;
    }

    public static KeybindPacket decode(FriendlyByteBuf buf) {
        return new KeybindPacket(buf.readInt(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(key);
        buf.writeBoolean(pressed);
    }

    public static void handle(KeybindPacket msg, PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player == null) return;
            if (msg.key < 0 || msg.key >= EnumKeybind.values().length) return;
            onPressedServer(player, EnumKeybind.values()[msg.key], msg.pressed);
        });
    }

    public static void onPressedServer(Player player, EnumKeybind key, boolean state) {
        HbmPlayerProps props = HbmPlayerProps.get(player);
        props.setKeyPressed(key, state);

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof IKeybindReceiver rec) {
            if (rec.canHandleKeybind(player, held, key)) rec.handleKeybind(player, held, key, state);
        }
    }
}
