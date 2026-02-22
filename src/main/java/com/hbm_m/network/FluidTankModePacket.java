package com.hbm_m.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import com.hbm_m.block.entity.custom.machines.MachineFluidTankBlockEntity;

public class FluidTankModePacket {
    private final BlockPos pos;

    public FluidTankModePacket(BlockPos pos) {
        this.pos = pos;
    }

    public FluidTankModePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public static void encode(FluidTankModePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static FluidTankModePacket decode(FriendlyByteBuf buf) {
        return new FluidTankModePacket(buf);
    }

    public static void handle(FluidTankModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(msg.pos);

            if (blockEntity instanceof MachineFluidTankBlockEntity fluidTank) {
                fluidTank.handleModeButton();
            }
        });
        context.setPacketHandled(true);
    }
}
