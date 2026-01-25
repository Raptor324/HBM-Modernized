package com.hbm_m.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import java.util.function.Supplier;

public class PacketSyncEnergy {
    private final int containerId;
    private final long energy;
    private final long maxEnergy;
    private final long delta; // <-- Новое поле

    public PacketSyncEnergy(int containerId, long energy, long maxEnergy, long delta) {
        this.containerId = containerId;
        this.energy = energy;
        this.maxEnergy = maxEnergy;
        this.delta = delta;
    }

    public static void encode(PacketSyncEnergy msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.containerId);
        buffer.writeLong(msg.energy);
        buffer.writeLong(msg.maxEnergy);
        buffer.writeLong(msg.delta); // <-- Пишем long
    }

    public static PacketSyncEnergy decode(FriendlyByteBuf buffer) {
        return new PacketSyncEnergy(
                buffer.readInt(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readLong() // <-- Читаем long
        );
    }

    public static void handle(PacketSyncEnergy msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Вызываем обновленный хендлер
                com.hbm_m.client.ClientEnergySyncHandler.handle(
                        msg.containerId,
                        msg.energy,
                        msg.maxEnergy,
                        msg.delta
                );
            });
        });
        ctx.get().setPacketHandled(true);
    }
}