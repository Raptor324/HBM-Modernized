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
    /** Original {@code ServerProxy.ID_DUCK / ID_JETPACK / ID_MAGNET / ID_HUD}. */
    public static final int ID_DUCK = 0;
    public static final int ID_JETPACK = 5;
    public static final int ID_MAGNET = 6;
    public static final int ID_HUD = 7;
    /** Original {@code ServerProxy.ID_TOOLABILITY}. */
    public static final int ID_TOOLABILITY = 11;

    private final Component text;
    private final int ticks;
    private final int id;
    private final int rgb;

    public InfoToastPacket(String translationKey, int ticks, int id, int rgb) {
        this(Component.translatable(translationKey), ticks, id, rgb);
    }

    /** Wie {@code PlayerInformPacket(IChatComponent, id)}: fertig formatierte Nachricht. */
    public InfoToastPacket(Component text, int ticks, int id, int rgb) {
        this.text = text;
        this.ticks = ticks;
        this.id = id;
        this.rgb = rgb;
    }

    public static InfoToastPacket decode(FriendlyByteBuf buf) {
        return new InfoToastPacket(buf.readComponent(), buf.readVarInt(), buf.readVarInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeComponent(text);
        buf.writeVarInt(ticks);
        buf.writeVarInt(id);
        buf.writeInt(rgb);
    }

    public static void handle(InfoToastPacket msg, PacketContext context) {
        context.queue(() -> {
            if (Minecraft.getInstance().level != null) {
                OverlayInfoToast.show(msg.text, msg.ticks, msg.id, msg.rgb);
            }
        });
    }

    public static void sendTo(ServerPlayer player, String translationKey, int ticks, int id, int rgb) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.INFO_TOAST,
                new InfoToastPacket(translationKey, ticks, id, rgb));
    }

    public static void sendTo(ServerPlayer player, Component text, int ticks, int id, int rgb) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.INFO_TOAST, new InfoToastPacket(text, ticks, id, rgb));
    }
}
