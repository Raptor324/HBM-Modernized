package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code NBTControlPacket("toggle")} из {@code GUIOilburner} (1.7.10):
 * кнопка On/Off в GUI переключает {@code isOn} у TileEntityHeaterOilburner.
 */
public class ToggleOilburnerC2SPacket implements C2SPacket {

    private final BlockPos pos;

    public ToggleOilburnerC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ToggleOilburnerC2SPacket decode(FriendlyByteBuf buf) {
        return new ToggleOilburnerC2SPacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(ToggleOilburnerC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be    = level.getBlockEntity(msg.pos);

            if (be instanceof MachineOilburnerBlockEntity oilburner) {
                oilburner.toggleOn();
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.OILBURNER_TOGGLE,
                new ToggleOilburnerC2SPacket(pos));
    }
}
