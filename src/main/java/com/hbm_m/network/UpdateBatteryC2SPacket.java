package com.hbm_m.network;

import com.hbm_m.block.entity.machines.BatterySocketBlockEntity;
import com.hbm_m.block.entity.machines.MachineBatteryBlockEntity;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class UpdateBatteryC2SPacket implements C2SPacket {

    private final BlockPos pos;
    private final int      buttonId;

    public UpdateBatteryC2SPacket(BlockPos pos, int buttonId) {
        this.pos      = pos;
        this.buttonId = buttonId;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static UpdateBatteryC2SPacket decode(FriendlyByteBuf buf) {
        return new UpdateBatteryC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(buttonId);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(UpdateBatteryC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (be instanceof MachineBatteryBlockEntity battery) {
                battery.handleButtonPress(msg.buttonId);
            } else if (be instanceof BatterySocketBlockEntity socket) {
                socket.handleButtonPress(msg.buttonId);
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos pos, int buttonId) {
        ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_BATTERY,
                new UpdateBatteryC2SPacket(pos, buttonId));
    }
}