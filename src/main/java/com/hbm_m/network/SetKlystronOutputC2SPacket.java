package com.hbm_m.network;

import com.hbm_m.blockentity.machines.fusion.FusionKlystronBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Zielleistung des Klystrons. Entspricht dem {@code NBTControlPacket} mit dem Schluessel
 * {@code amount} aus {@code TileEntityFusionKlystron.receiveControl}; die Klemmung auf
 * {@code 0..MAX_OUTPUT} passiert wie im Original serverseitig.
 */
public class SetKlystronOutputC2SPacket implements C2SPacket {

    private final BlockPos blockPos;
    private final long amount;

    public SetKlystronOutputC2SPacket(BlockPos blockPos, long amount) {
        this.blockPos = blockPos;
        this.amount = amount;
    }

    public static SetKlystronOutputC2SPacket decode(FriendlyByteBuf buf) {
        return new SetKlystronOutputC2SPacket(buf.readBlockPos(), buf.readLong());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeLong(amount);
    }

    public static void handle(SetKlystronOutputC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            // Original: hasPermission -> 20 Bloecke um die Mitte des Klystrons.
            if (player.distanceToSqr(
                    msg.blockPos.getX() + 0.5,
                    msg.blockPos.getY() + 2.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof FusionKlystronBlockEntity klystron) {
                klystron.setOutputTarget(msg.amount);
            }
        });
    }

    public static void sendToServer(BlockPos blockPos, long amount) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_KLYSTRON_OUTPUT,
                new SetKlystronOutputC2SPacket(blockPos, amount));
    }
}
