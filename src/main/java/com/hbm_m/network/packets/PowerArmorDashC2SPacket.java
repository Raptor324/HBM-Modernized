package com.hbm_m.network.packets;

import com.hbm_m.network.C2SPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.powerarmor.PowerArmorHandlers;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S: dash request from the power armour keybind.
 *
 * <p>{@link PowerArmorHandlers#performDash} is server-only — it spends energy, sets the cooldown and
 * broadcasts {@link PowerArmorDashPacket} to nearby clients. Calling it with a client player did
 * nothing, so the keybind needs this request packet to reach the server.
 */
public class PowerArmorDashC2SPacket implements C2SPacket {

    public static PowerArmorDashC2SPacket decode(FriendlyByteBuf buf) {
        return new PowerArmorDashC2SPacket();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(PowerArmorDashC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            // Every precondition (armour, energy, cooldown) is re-checked server-side.
            if (context.getPlayer() instanceof ServerPlayer player) {
                PowerArmorHandlers.performDash(player);
            }
        });
    }

    public static void sendToServer() {
        ModPacketHandler.sendToServer(ModPacketHandler.POWER_ARMOR_DASH_REQUEST, new PowerArmorDashC2SPacket());
    }
}
