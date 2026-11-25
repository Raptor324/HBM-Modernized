package com.hbm_m.network;

import com.hbm_m.blockentity.machines.LaunchpadSoyuzBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S packet for the Soyuz launch pad's GUI buttons (cargo-mode toggle, launch).
 * Original: {@code NBTControlPacket} {cargo: bool} / {launch: true} via
 * {@code TileEntityLaunchpadSoyuz.receiveControl} - ported as a typed packet
 * ({@link SoyuzLauncherControlPacket} pattern).
 */
public class LaunchpadSoyuzControlPacket implements C2SPacket {

    public static final int ACTION_SET_CARGO = 0;
    public static final int ACTION_LAUNCH = 1;

    private final BlockPos pos;
    private final int action;
    private final int value;

    public LaunchpadSoyuzControlPacket(BlockPos pos, int action, int value) {
        this.pos = pos;
        this.action = action;
        this.value = value;
    }

    public static LaunchpadSoyuzControlPacket decode(FriendlyByteBuf buf) {
        return new LaunchpadSoyuzControlPacket(buf.readBlockPos(), buf.readByte(), buf.readByte());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
        buf.writeByte(value);
    }

    public static void handle(LaunchpadSoyuzControlPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (ModPacketHandler.blockEntityAt(player, pkt.pos) instanceof LaunchpadSoyuzBlockEntity pad) {
                if (!(player.containerMenu instanceof com.hbm_m.inventory.menu.LaunchpadSoyuzMenu menu)
                        || menu.getBlockEntity() != pad
                        || !menu.stillValid(player)) return;
                if (pkt.action == ACTION_SET_CARGO) {
                    // Original receiveControl: data.setBoolean("cargo", ...)
                    pad.setCargoMode(pkt.value != 0);
                } else if (pkt.action == ACTION_LAUNCH) {
                    // Original receiveControl: launch() - Gatter READY && canLaunch
                    pad.launch();
                }
            }
        });
    }

    public static LaunchpadSoyuzControlPacket setCargo(BlockPos pos, boolean cargo) {
        return new LaunchpadSoyuzControlPacket(pos, ACTION_SET_CARGO, cargo ? 1 : 0);
    }

    public static LaunchpadSoyuzControlPacket launch(BlockPos pos) {
        return new LaunchpadSoyuzControlPacket(pos, ACTION_LAUNCH, 0);
    }
}
