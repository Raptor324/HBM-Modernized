package com.hbm_m.network.missile;

import com.hbm_m.client.missile.track.MissileTrackClient;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class S2CMissileTrackStopPacket implements S2CPacket {

    public final int entityId;

    public S2CMissileTrackStopPacket(int entityId) {
        this.entityId = entityId;
    }

    public static S2CMissileTrackStopPacket decode(FriendlyByteBuf buf) {
        return new S2CMissileTrackStopPacket(buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public static void handle(S2CMissileTrackStopPacket msg, PacketContext context) {
        context.queue(() -> MissileTrackClient.onStop(msg.entityId));
    }

    public static void sendTo(ServerPlayer player, int entityId) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.MISSILE_TRACK_STOP,
                new S2CMissileTrackStopPacket(entityId));
    }
}
