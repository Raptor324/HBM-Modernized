package com.hbm_m.network;

import com.hbm_m.client.ChunkRadiationDebugRenderer;
import com.hbm_m.main.MainRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChunkRadiationDebugPacket {
    private final int chunkX;
    private final int chunkZ;
    private final float radiationValue;

    public ChunkRadiationDebugPacket(int chunkX, int chunkZ, float radiationValue) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.radiationValue = radiationValue;
    }

    public static void encode(ChunkRadiationDebugPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.chunkX);
        buf.writeInt(msg.chunkZ);
        buf.writeFloat(msg.radiationValue);
    }

    public static ChunkRadiationDebugPacket decode(FriendlyByteBuf buf) {
        return new ChunkRadiationDebugPacket(buf.readInt(), buf.readInt(), buf.readFloat());
    }

    public static void handle(ChunkRadiationDebugPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                MainRegistry.LOGGER.debug("CLIENT: Received ChunkRadiationDebugPacket for chunk ({}, {}). Updating radiation to {}", msg.chunkX, msg.chunkZ, msg.radiationValue);
                ChunkRadiationDebugRenderer.updateChunkRadiation(msg.chunkX, msg.chunkZ, msg.radiationValue);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}