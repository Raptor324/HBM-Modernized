package com.hbm_m.network;

// Пакет для отправки от сервера к клиенту данных о радиации в чанках для отладки.
// Используется только в режиме отладки для отображения уровней радиации в чанках на клиенте.
// Содержит карту позиций чанков и их уровней радиации, а также идентификатор измерения.

import com.hbm_m.client.ClientRadiationData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ChunkRadiationDebugBatchPacket {

    private final Map<ChunkPos, Float> radiationData;
    // Добавлено поле для идентификатора измерения.
    private final ResourceLocation dimension;

    public ChunkRadiationDebugBatchPacket(Map<ChunkPos, Float> radiationData, ResourceLocation dimension) {
        this.radiationData = radiationData;
        this.dimension = dimension;
    }

    public static void encode(ChunkRadiationDebugBatchPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.dimension);
        buf.writeVarInt(msg.radiationData.size());
        for (Map.Entry<ChunkPos, Float> entry : msg.radiationData.entrySet()) {
            buf.writeChunkPos(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
    }

    public static ChunkRadiationDebugBatchPacket decode(FriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readResourceLocation();
        int size = buf.readVarInt();
        Map<ChunkPos, Float> data = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            data.put(buf.readChunkPos(), buf.readFloat());
        }
        return new ChunkRadiationDebugBatchPacket(data, dimension);
    }

    public static void handle(ChunkRadiationDebugBatchPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Использование DistExecutor - самый безопасный способ вызова клиентского кода.
            // Он гарантирует, что ClientHandler.handlePacket() никогда не будет вызван на сервере.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handlePacket(msg));
        });
        ctx.get().setPacketHandled(true);
    }


    // Внутренний класс для обработки пакета, который существует только на клиенте.

    private static class ClientHandler {
        public static void handlePacket(ChunkRadiationDebugBatchPacket msg) {
            ClientRadiationData.updateRadiationData(msg.dimension, msg.radiationData);
        }
    }
}