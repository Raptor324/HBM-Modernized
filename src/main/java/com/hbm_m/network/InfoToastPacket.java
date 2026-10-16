package com.hbm_m.network;

import com.hbm_m.client.overlay.OverlayInfoToast;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C: показать сообщение в OverlayInfoToast (аналог PlayerInformPacket из 1.7.10,
 * который сервером пугал игрока "info.asbestos" / "info.coaldust").
 */
public class InfoToastPacket implements S2CPacket {

    /** ID строки-тоста для газовых опасностей (асбестоз, угольная пыль). */
    public static final int ID_GAS_HAZARD = 2010;

    private final String translationKey;
    private final int ticks;
    private final int id;
    private final int rgb;

    public InfoToastPacket(String translationKey, int ticks, int id, int rgb) {
        this.translationKey = translationKey;
        this.ticks = ticks;
        this.id = id;
        this.rgb = rgb;
    }

    public static InfoToastPacket decode(FriendlyByteBuf buf) {
        return new InfoToastPacket(buf.readUtf(256), buf.readVarInt(), buf.readVarInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(translationKey, 256);
        buf.writeVarInt(ticks);
        buf.writeVarInt(id);
        buf.writeInt(rgb);
    }

    public static void handle(InfoToastPacket msg, PacketContext context) {
        context.queue(() -> {
            if (Minecraft.getInstance().level != null) {
                OverlayInfoToast.show(Component.translatable(msg.translationKey), msg.ticks, msg.id, msg.rgb);
            }
        });
    }

    public static void sendTo(ServerPlayer player, String translationKey, int ticks, int id, int rgb) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.INFO_TOAST,
                new InfoToastPacket(translationKey, ticks, id, rgb));
    }
}
