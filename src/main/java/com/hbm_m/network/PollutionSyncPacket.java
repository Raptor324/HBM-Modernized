package com.hbm_m.network;

import com.hbm_m.handler.pollution.PollutionData;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Original {@code PermaSyncHandler}, Abschnitt "POLLUTION": die Verschmutzung der Zelle, in der der Spieler steht,
 * jeden Tick an ihn. Clientseitig in {@link #pollution} (Original {@code PermaSyncHandler.pollution}), daraus der
 * Smog-Nebel ({@code RadiationConfig.enableSootFog}).
 */
public class PollutionSyncPacket implements S2CPacket {

    /** Original {@code PermaSyncHandler.pollution} (nur Client). */
    public static float[] pollution = new float[PollutionType.values().length];

    private final float[] values;

    public PollutionSyncPacket(float[] values) {
        this.values = values;
    }

    public static PollutionSyncPacket decode(FriendlyByteBuf buf) {
        float[] v = new float[PollutionType.values().length];
        for (int i = 0; i < v.length; i++) v[i] = buf.readFloat();
        return new PollutionSyncPacket(v);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        for (int i = 0; i < PollutionType.values().length; i++) buf.writeFloat(values[i]);
    }

    public static void handle(PollutionSyncPacket msg, PacketContext context) {
        context.queue(() -> {
            for (int i = 0; i < pollution.length; i++) pollution[i] = msg.values[i];
        });
    }

    public static void sendTo(ServerPlayer player) {
        PollutionData data = PollutionHandler.getPollutionData(player.level(), (int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));
        if (data == null) data = new PollutionData();
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.POLLUTION_SYNC, new PollutionSyncPacket(data.pollution.clone()));
    }
}
