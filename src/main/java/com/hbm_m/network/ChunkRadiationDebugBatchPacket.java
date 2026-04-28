package com.hbm_m.network;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.client.ClientRadiationData;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class ChunkRadiationDebugBatchPacket implements S2CPacket {

    private final Map<ChunkPos, Float> radiationData;
    private final ResourceLocation     dimension;

    public ChunkRadiationDebugBatchPacket(Map<ChunkPos, Float> radiationData,
                                          ResourceLocation dimension) {
        this.radiationData = radiationData;
        this.dimension     = dimension;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ChunkRadiationDebugBatchPacket decode(FriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readResourceLocation();
        int              size      = buf.readVarInt();
        Map<ChunkPos, Float> data  = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            data.put(buf.readChunkPos(), buf.readFloat());
        }
        return new ChunkRadiationDebugBatchPacket(data, dimension);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension);
        buf.writeVarInt(radiationData.size());
        for (Map.Entry<ChunkPos, Float> entry : radiationData.entrySet()) {
            buf.writeChunkPos(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(ChunkRadiationDebugBatchPacket msg, PacketContext context) {
        // context.queue() уже на клиентском главном потоке — DistExecutor не нужен
        context.queue(() -> ClientRadiationData.updateRadiationData(msg.dimension, msg.radiationData));
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendTo(ServerPlayer player,
                              Map<ChunkPos, Float> data, ResourceLocation dimension) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.CHUNK_RAD_DEBUG_BATCH,
                new ChunkRadiationDebugBatchPacket(data, dimension));
    }
}