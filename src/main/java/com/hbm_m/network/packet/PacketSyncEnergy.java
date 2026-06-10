package com.hbm_m.network.packet;

import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C пакет: синхронизация энергии между сервером и GUI клиента.
 */
public class PacketSyncEnergy implements S2CPacket {

    private final int  containerId;
    private final long energy;
    private final long maxEnergy;
    private final long delta;

    public PacketSyncEnergy(int containerId, long energy, long maxEnergy, long delta) {
        this.containerId = containerId;
        this.energy      = energy;
        this.maxEnergy   = maxEnergy;
        this.delta       = delta;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static PacketSyncEnergy decode(FriendlyByteBuf buf) {
        return new PacketSyncEnergy(
                buf.readInt(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong()
        );
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(containerId);
        buf.writeLong(energy);
        buf.writeLong(maxEnergy);
        buf.writeLong(delta);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(PacketSyncEnergy msg, PacketContext context) {
        // context.queue() — главный поток клиента, доступ к Minecraft безопасен
        context.queue(() ->
                com.hbm_m.client.ClientEnergySyncHandler.handle(
                        msg.containerId,
                        msg.energy,
                        msg.maxEnergy,
                        msg.delta
                )
        );
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendTo(ServerPlayer player,
                              int containerId, long energy, long maxEnergy, long delta) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.SYNC_ENERGY,
                new PacketSyncEnergy(containerId, energy, maxEnergy, delta));
    }
}