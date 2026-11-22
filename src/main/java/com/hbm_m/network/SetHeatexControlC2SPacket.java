package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineHeatexBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port of the per-keystroke {@code NBTControlPacket} from {@code GUIHeaterHeatex} (1.7.10 Original):
 * the original sent {@code toCool} / {@code delay} in an NBT compound on every textbox change.
 * Here the same fields are sent as ints; {@code -1} means "field not set" (equivalent to the
 * missing NBT key in {@code receiveControl}).
 */
public class SetHeatexControlC2SPacket implements C2SPacket {

    public static final int NOT_SET = -1;

    private final BlockPos pos;
    private final int toCool;
    private final int tickDelay;

    public SetHeatexControlC2SPacket(BlockPos pos, int toCool, int tickDelay) {
        this.pos = pos;
        this.toCool = toCool;
        this.tickDelay = tickDelay;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static SetHeatexControlC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int toCool = buf.readInt();
        int tickDelay = buf.readInt();
        return new SetHeatexControlC2SPacket(pos, toCool, tickDelay);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(toCool);
        buf.writeInt(tickDelay);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(SetHeatexControlC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);

            if (be instanceof MachineHeatexBlockEntity heatex) {
                heatex.receiveControl(msg.toCool, msg.tickDelay);
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos pos, Integer toCool, Integer tickDelay) {
        ModPacketHandler.sendToServer(ModPacketHandler.HEATEX_CONTROL,
                new SetHeatexControlC2SPacket(pos,
                        toCool != null ? toCool : NOT_SET,
                        tickDelay != null ? tickDelay : NOT_SET));
    }
}
