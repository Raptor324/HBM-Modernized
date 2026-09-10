package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Der An/Aus-Knopf des Dieselgenerators. Im Original der Schluessel {@code turnOn} im
 * {@code NBTControlPacket}.
 */
public class DieselGeneratorToggleC2SPacket implements C2SPacket {

    private final BlockPos blockPos;

    private DieselGeneratorToggleC2SPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static DieselGeneratorToggleC2SPacket decode(FriendlyByteBuf buf) {
        return new DieselGeneratorToggleC2SPacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }

    public static void handle(DieselGeneratorToggleC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof MachineDieselGeneratorBlockEntity diesel) diesel.toggleIgnition();
        });
    }

    public static void send(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.DIESEL_GENERATOR_TOGGLE,
                new DieselGeneratorToggleC2SPacket(pos));
    }
}
