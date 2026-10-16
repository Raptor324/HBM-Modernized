package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineWatzPowerplantBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class WatzControlPacket implements C2SPacket {

    public static final int ACTION_TOGGLE = 0;

    private final BlockPos pos;
    private final int action;

    public WatzControlPacket(BlockPos pos, int action) {
        this.pos = pos;
        this.action = action;
    }

    public static WatzControlPacket decode(FriendlyByteBuf buf) {
        return new WatzControlPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(action);
    }

    public static void handle(WatzControlPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            var blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof MachineWatzPowerplantBlockEntity watz) {
                watz.handleButtonPress(packet.action);
            }
        });
    }

    public static void sendToServer(BlockPos pos, int action) {
        ModPacketHandler.sendToServer(ModPacketHandler.WATZ_CONTROL,
                new WatzControlPacket(pos, action));
    }
}
