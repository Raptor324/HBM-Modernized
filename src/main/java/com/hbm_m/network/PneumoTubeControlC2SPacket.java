package com.hbm_m.network;

import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Die Schalter in der Oberflaeche des Druckluftrohrs. Im Original ist das
 * {@code IControlReceiverFilter.receiveControl} mit den Schluesseln {@code whitelist},
 * {@code redstone}, {@code pressure}, {@code send} und {@code receive}.
 */
public class PneumoTubeControlC2SPacket implements C2SPacket {

    public enum Control { WHITELIST, REDSTONE, PRESSURE, SEND_ORDER, RECEIVE_ORDER }

    private final BlockPos blockPos;
    private final int control;

    private PneumoTubeControlC2SPacket(BlockPos blockPos, int control) {
        this.blockPos = blockPos;
        this.control = control;
    }

    public static PneumoTubeControlC2SPacket decode(FriendlyByteBuf buf) {
        return new PneumoTubeControlC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(control);
    }

    public static void handle(PneumoTubeControlC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (!(be instanceof PneumoTubeBlockEntity tube)) return;

            Control[] values = Control.values();
            if (msg.control < 0 || msg.control >= values.length) return;

            switch (values[msg.control]) {
                case WHITELIST      -> tube.toggleWhitelist();
                case REDSTONE       -> tube.toggleRedstone();
                case PRESSURE       -> tube.nextPressure();
                case SEND_ORDER     -> tube.nextSendOrder();
                case RECEIVE_ORDER  -> tube.nextReceiveOrder();
            }
        });
    }

    public static void send(BlockPos pos, Control control) {
        ModPacketHandler.sendToServer(ModPacketHandler.PNEUMO_TUBE_CONTROL,
                new PneumoTubeControlC2SPacket(pos, control.ordinal()));
    }
}
