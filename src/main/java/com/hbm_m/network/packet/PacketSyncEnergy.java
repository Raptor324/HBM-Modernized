package com.hbm_m.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.function.Supplier;

// ВАЖНО: Здесь НЕТ импортов net.minecraft.client.* !

public class PacketSyncEnergy {
    private final int containerId;
    private final long energy;
    private final long maxEnergy;

    public PacketSyncEnergy(int containerId, long energy, long maxEnergy) {
        this.containerId = containerId;
        this.energy = energy;
        this.maxEnergy = maxEnergy;
    }

    public static void encode(PacketSyncEnergy msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.containerId);
        buffer.writeLong(msg.energy);
        buffer.writeLong(msg.maxEnergy);
    }

    public static PacketSyncEnergy decode(FriendlyByteBuf buffer) {
        return new PacketSyncEnergy(buffer.readInt(), buffer.readLong(), buffer.readLong());
    }

    public static void handle(PacketSyncEnergy msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Магия DistExecutor:
            // Сервер проигнорирует этот блок кода и не будет пытаться загрузить ClientEnergySyncHandler.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.hbm_m.client.ClientEnergySyncHandler.handle(msg.containerId, msg.energy, msg.maxEnergy);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
