package com.hbm_m.network;

import com.hbm_m.handler.ImpactWorldHandler;
import com.hbm_m.saveddata.TomSaveData;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Original {@code PermaSyncHandler}, Abschnitt "TOM IMPACT DATA": Feuer, Staub und Einschlag der Welt des Spielers.
 * Clientseitig landen die Werte in {@link ImpactWorldHandler} und gelten nur fuer die Welt, in der sie ankamen.
 */
public class ImpactSyncPacket implements S2CPacket {

    private final float fire;
    private final float dust;
    private final boolean impact;

    public ImpactSyncPacket(float fire, float dust, boolean impact) {
        this.fire = fire;
        this.dust = dust;
        this.impact = impact;
    }

    public static ImpactSyncPacket decode(FriendlyByteBuf buf) {
        return new ImpactSyncPacket(buf.readFloat(), buf.readFloat(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(fire);
        buf.writeFloat(dust);
        buf.writeBoolean(impact);
    }

    public static void handle(ImpactSyncPacket msg, PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() == null) return;
            ImpactWorldHandler.lastSyncWorld = context.getPlayer().level();
            ImpactWorldHandler.fire = msg.fire;
            ImpactWorldHandler.dust = msg.dust;
            ImpactWorldHandler.impact = msg.impact;
        });
    }

    public static void sendTo(ServerPlayer player) {
        TomSaveData data = TomSaveData.forWorld(player.serverLevel());
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.IMPACT_SYNC, new ImpactSyncPacket(data.fire, data.dust, data.impact));
    }
}
