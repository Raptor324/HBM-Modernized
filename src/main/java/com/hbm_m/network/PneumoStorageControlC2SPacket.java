package com.hbm_m.network;

import com.hbm_m.blockentity.network.pneumatic.PneumaticStorageBlockEntity;
import com.hbm_m.blockentity.network.pneumatic.PneumoStorageExporterBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Die Schalter der Lagernetzgeraete. Im Original sind das {@code IControlReceiver.receiveControl}
 * mit den Schluesseln {@code pressure} (Lager), {@code continuous}, {@code request} und
 * {@code ror} (Ausgabe).
 */
public class PneumoStorageControlC2SPacket implements C2SPacket {

    public enum Control { PRESSURE, EXPORT_CONTINUOUS, EXPORT_MODE, EXPORT_ROR }

    private final BlockPos blockPos;
    private final int control;

    private PneumoStorageControlC2SPacket(BlockPos blockPos, int control) {
        this.blockPos = blockPos;
        this.control = control;
    }

    public static PneumoStorageControlC2SPacket decode(FriendlyByteBuf buf) {
        return new PneumoStorageControlC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(control);
    }

    public static void handle(PneumoStorageControlC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            Control[] values = Control.values();
            if (msg.control < 0 || msg.control >= values.length) return;

            switch (values[msg.control]) {
                case PRESSURE -> {
                    if (be instanceof PneumaticStorageBlockEntity storage) storage.nextPressure();
                }
                case EXPORT_CONTINUOUS -> {
                    if (be instanceof PneumoStorageExporterBlockEntity exporter) exporter.toggleContinuous();
                }
                case EXPORT_MODE -> {
                    if (be instanceof PneumoStorageExporterBlockEntity exporter) exporter.nextRequestMode();
                }
                case EXPORT_ROR -> {
                    if (be instanceof PneumoStorageExporterBlockEntity exporter) exporter.toggleRorMode();
                }
            }
        });
    }

    public static void send(BlockPos pos, Control control) {
        ModPacketHandler.sendToServer(ModPacketHandler.PNEUMO_STORAGE_CONTROL,
                new PneumoStorageControlC2SPacket(pos, control.ordinal()));
    }
}
