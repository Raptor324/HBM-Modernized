package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Der An/Aus-Knopf des Bergbaulasers. Im Original der {@code AuxButtonPacket} mit {@code (0, 0)}. */
public class MiningLaserToggleC2SPacket implements C2SPacket {

    private final BlockPos blockPos;

    private MiningLaserToggleC2SPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static MiningLaserToggleC2SPacket decode(FriendlyByteBuf buf) {
        return new MiningLaserToggleC2SPacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }

    public static void handle(MiningLaserToggleC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof MachineMiningLaserBlockEntity laser) laser.toggleOn();
        });
    }

    public static void send(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.MINING_LASER_TOGGLE,
                new MiningLaserToggleC2SPacket(pos));
    }
}
