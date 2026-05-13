package com.hbm_m.network;

import com.hbm_m.block.entity.machines.MachineRadarBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class UpdateRadarC2SPacket implements C2SPacket {

    private final BlockPos pos;
    private final int buttonId;

    public UpdateRadarC2SPacket(BlockPos pos, int buttonId) {
        this.pos = pos;
        this.buttonId = buttonId;
    }

    public static UpdateRadarC2SPacket decode(FriendlyByteBuf buf) {
        return new UpdateRadarC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(buttonId);
    }

    public static void handle(UpdateRadarC2SPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            var blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof MachineRadarBlockEntity radar) {
                radar.handleButtonPress(packet.buttonId);
            }
        });
    }
}
