package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineZirnoxBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class ZirnoxControlPacket implements C2SPacket {

    public static final int ACTION_CONTROL = 0;
    public static final int ACTION_VENT = 1;

    private final BlockPos pos;
    private final int action;

    public ZirnoxControlPacket(BlockPos pos, int action) {
        this.pos = pos;
        this.action = action;
    }

    public static ZirnoxControlPacket decode(FriendlyByteBuf buf) {
        return new ZirnoxControlPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(action);
    }

    public static void handle(ZirnoxControlPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            var blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof MachineZirnoxBlockEntity zirnox) {
                zirnox.handleButtonPress(packet.action);
            }
        });
    }

    public static void sendToServer(BlockPos pos, int action) {
        ModPacketHandler.sendToServer(ModPacketHandler.ZIRNOX_CONTROL,
                new ZirnoxControlPacket(pos, action));
    }
}