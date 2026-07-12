package com.hbm_m.network;

import com.hbm_m.block.entity.machines.SoyuzLauncherBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S packet for the Soyuz Launcher's two GUI buttons (mode switch, launch start).
 * Patterned on {@link RBMKConsoleControlPacket}.
 */
public class SoyuzLauncherControlPacket implements C2SPacket {

    public static final int ACTION_SET_MODE = 0;
    public static final int ACTION_START = 1;

    private final BlockPos pos;
    private final int action;
    private final int value;

    public SoyuzLauncherControlPacket(BlockPos pos, int action, int value) {
        this.pos = pos;
        this.action = action;
        this.value = value;
    }

    public static SoyuzLauncherControlPacket decode(FriendlyByteBuf buf) {
        return new SoyuzLauncherControlPacket(buf.readBlockPos(), buf.readByte(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
        buf.writeInt(value);
    }

    public static void handle(SoyuzLauncherControlPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (player.level().getBlockEntity(pkt.pos) instanceof SoyuzLauncherBlockEntity launcher) {
                if (pkt.action == ACTION_SET_MODE) {
                    launcher.setMode(pkt.value);
                } else if (pkt.action == ACTION_START) {
                    launcher.startCountdown();
                }
            }
        });
    }

    public static SoyuzLauncherControlPacket setMode(BlockPos pos, int mode) {
        return new SoyuzLauncherControlPacket(pos, ACTION_SET_MODE, mode);
    }

    public static SoyuzLauncherControlPacket start(BlockPos pos) {
        return new SoyuzLauncherControlPacket(pos, ACTION_START, 0);
    }
}
