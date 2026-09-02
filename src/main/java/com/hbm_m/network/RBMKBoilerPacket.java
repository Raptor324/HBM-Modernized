package com.hbm_m.network;

import com.hbm_m.blockentity.machines.rbmk.RBMKBoilerBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S packet for the steam channel's compressor button.
 *
 * <p>{@code GUIRBMKBoiler} used to call {@code cycleCompressor()} straight off the click, but the
 * block entity a screen holds is the client-side copy: the server never heard about it, and the
 * next sync tick snapped the grade back. This mirrors the original's
 * {@code receiveControl(data.hasKey("compression"))} path, and it keeps the original's
 * {@code hasPermission} check - the player has to be within 20 blocks of the column.</p>
 */
public class RBMKBoilerPacket implements C2SPacket {

    public static final int ACTION_CYCLE_COMPRESSOR = 0;

    private final BlockPos pos;
    private final int      action;

    public RBMKBoilerPacket(BlockPos pos, int action) {
        this.pos    = pos;
        this.action = action;
    }

    public static RBMKBoilerPacket decode(FriendlyByteBuf buf) {
        return new RBMKBoilerPacket(buf.readBlockPos(), buf.readByte());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
    }

    public static void handle(RBMKBoilerPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (!(player.level().getBlockEntity(pkt.pos) instanceof RBMKBoilerBlockEntity be)) return;
            // hasPermission: lengthVector() < 20
            if (player.distanceToSqr(pkt.pos.getX(), pkt.pos.getY(), pkt.pos.getZ()) > 400.0D) return;

            if (pkt.action == ACTION_CYCLE_COMPRESSOR) {
                be.cycleCompressor();
            }
        });
    }

    public static void sendCycleCompressor(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_BOILER_CONTROL,
                new RBMKBoilerPacket(pos, ACTION_CYCLE_COMPRESSOR));
    }
}
