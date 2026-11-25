package com.hbm_m.network;

import com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm_m.inventory.menu.MenuReach;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S packet for the autoloader GUI's cycle-threshold buttons ({@code -} / {@code +}).
 *
 * <p>Mirrors the original's {@code GUIRBMKAutoloader.mouseClicked} → {@code NBTControlPacket}
 * ("minus"/"plus") → {@code TileEntityRBMKAutoloader.receiveControl} path. The original's
 * {@code hasPermission} is {@code isUseableByPlayer} - the standard menu reach - NOT the
 * 20-block radius the boiler column uses, so the check here is {@link MenuReach#canReach}.</p>
 */
public class RBMKAutoloaderPacket implements C2SPacket {

    public static final int ACTION_MINUS = 0;
    public static final int ACTION_PLUS  = 1;

    private final BlockPos pos;
    private final int      action;

    public RBMKAutoloaderPacket(BlockPos pos, int action) {
        this.pos    = pos;
        this.action = action;
    }

    public static RBMKAutoloaderPacket decode(FriendlyByteBuf buf) {
        return new RBMKAutoloaderPacket(buf.readBlockPos(), buf.readByte());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
    }

    public static void handle(RBMKAutoloaderPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (pkt.action != ACTION_MINUS && pkt.action != ACTION_PLUS) return;
            if (!(ModPacketHandler.blockEntityAt(player, pkt.pos) instanceof RBMKAutoloaderBlockEntity be)) return;
            // hasPermission: isUseableByPlayer - the normal menu reach, no fixed radius
            if (!MenuReach.canReach(player, pkt.pos, player.level().getBlockState(pkt.pos))) return;

            be.receiveCycleControl(pkt.action == ACTION_MINUS, pkt.action == ACTION_PLUS);
        });
    }

    public static void sendCycleMinus(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_AUTOLOADER_CONTROL,
                new RBMKAutoloaderPacket(pos, ACTION_MINUS));
    }

    public static void sendCyclePlus(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_AUTOLOADER_CONTROL,
                new RBMKAutoloaderPacket(pos, ACTION_PLUS));
    }
}
