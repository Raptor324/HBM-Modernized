package com.hbm_m.network;

import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FluidTankModePacket implements C2SPacket {

    private final BlockPos pos;

    public FluidTankModePacket(BlockPos pos) {
        this.pos = pos;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static FluidTankModePacket decode(FriendlyByteBuf buf) {
        return new FluidTankModePacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(FluidTankModePacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be    = level.getBlockEntity(msg.pos);

            if (be instanceof MachineFluidTankBlockEntity fluidTank) {
                fluidTank.handleModeButton();
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.FLUID_TANK_MODE,
                new FluidTankModePacket(pos));
    }
}