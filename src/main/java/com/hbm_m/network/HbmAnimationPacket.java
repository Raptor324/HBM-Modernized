package com.hbm_m.network;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 1:1 {@code HbmAnimationPacket} (Server -> Client): startet eine Waffenanimation fuer den gehaltenen Gegenstand.
 * {@code type} = {@code GunAnimation}-Ordinalwert, {@code receiverIndex} fuer den Rueckstoss, {@code itemIndex} =
 * Waffen-Konfiguration (Animationsschiene).
 */
public class HbmAnimationPacket implements S2CPacket {

    public final short type;
    public final int receiverIndex;
    public final int itemIndex;

    public HbmAnimationPacket(int type, int rec, int gun) {
        this.type = (short) type;
        this.receiverIndex = rec;
        this.itemIndex = gun;
    }

    public static HbmAnimationPacket decode(FriendlyByteBuf buf) {
        return new HbmAnimationPacket(buf.readShort(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeShort(type);
        buf.writeInt(receiverIndex);
        buf.writeInt(itemIndex);
    }

    public static void handle(HbmAnimationPacket msg, PacketContext context) {
        context.queue(() -> com.hbm_m.client.weapon.GunClientHooks.handleAnimationPacket(msg.type, msg.receiverIndex, msg.itemIndex));
    }
}
