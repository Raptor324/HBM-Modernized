package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineFunnelBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Port of {@code TileEntityMachineFunnel.receiveControl} (1.7.10 Original) - mode-cycle button. */
public class FunnelModeC2SPacket implements C2SPacket {

    private final BlockPos pos;

    public FunnelModeC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static FunnelModeC2SPacket decode(FriendlyByteBuf buf) {
        return new FunnelModeC2SPacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void handle(FunnelModeC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            BlockEntity be = player.serverLevel().getBlockEntity(msg.pos);
            if (be instanceof MachineFunnelBlockEntity funnel) {
                funnel.cycleMode();
            }
        });
    }

    public static void sendToServer(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.FUNNEL_MODE, new FunnelModeC2SPacket(pos));
    }
}
