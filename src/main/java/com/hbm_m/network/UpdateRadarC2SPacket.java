package com.hbm_m.network;

import java.util.function.Supplier;

import com.hbm_m.block.entity.machines.MachineRadarBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class UpdateRadarC2SPacket {

    private final BlockPos pos;
    private final int buttonId;

    public UpdateRadarC2SPacket(BlockPos pos, int buttonId) {
        this.pos = pos;
        this.buttonId = buttonId;
    }

    public UpdateRadarC2SPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.buttonId = buf.readInt();
    }

    public static void encode(UpdateRadarC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeInt(packet.buttonId);
    }

    public static UpdateRadarC2SPacket decode(FriendlyByteBuf buf) {
        return new UpdateRadarC2SPacket(buf);
    }

    public static void handle(UpdateRadarC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            var blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof MachineRadarBlockEntity radar) {
                radar.handleButtonPress(packet.buttonId);
            }
        });
        context.setPacketHandled(true);
    }
}
