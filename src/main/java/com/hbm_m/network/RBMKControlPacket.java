package com.hbm_m.network;

import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlManualBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * C2S packet for the direct per-block control rod menus ({@code RBMKControlMenu} /
 * {@code RBMKControlAutoMenu}). The GUIs previously mutated the {@link RBMKControlBlockEntity}
 * fields directly on click - but that block entity instance is the client-side copy, so nothing
 * ever reached the server and the next tick's sync snapped the level back to whatever the server
 * still had (stuck at 0%, unable to select 50/75/100). This packet routes those edits through the
 * server the same way {@code RBMKConsoleControlPacket} already does for the console.
 */
public class RBMKControlPacket implements C2SPacket {

    public static final int ACTION_SET_TARGET   = 0; // manual: doubleVal = target level 0-1
    public static final int ACTION_SET_COLOR    = 1; // manual: intVal = color group -1..4
    public static final int ACTION_SET_FUNCTION = 2; // auto: intVal = RBMKFunction ordinal
    public static final int ACTION_SET_PARAMS   = 3; // auto: doubleVal4 = [levelUpper, levelLower, heatUpper, heatLower]

    private final BlockPos pos;
    private final int      action;
    private final double   doubleVal;
    private final int      intVal;
    private final double[] doubleVal4;

    public RBMKControlPacket(BlockPos pos, int action, double doubleVal, int intVal, double[] doubleVal4) {
        this.pos        = pos;
        this.action      = action;
        this.doubleVal   = doubleVal;
        this.intVal      = intVal;
        this.doubleVal4  = doubleVal4 != null ? doubleVal4 : new double[4];
    }

    public static RBMKControlPacket decode(FriendlyByteBuf buf) {
        BlockPos pos   = buf.readBlockPos();
        int action     = buf.readByte();
        double dVal    = buf.readDouble();
        int iVal       = buf.readInt();
        double[] d4    = new double[4];
        for (int i = 0; i < 4; i++) d4[i] = buf.readDouble();
        return new RBMKControlPacket(pos, action, dVal, iVal, d4);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
        buf.writeDouble(doubleVal);
        buf.writeInt(intVal);
        for (double v : doubleVal4) buf.writeDouble(v);
    }

    public static void handle(RBMKControlPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (!(player.level().getBlockEntity(pkt.pos) instanceof RBMKControlBlockEntity be)) return;

            switch (pkt.action) {
                case ACTION_SET_TARGET -> be.setTarget(pkt.doubleVal);
                case ACTION_SET_COLOR -> be.color = (short) Mth.clamp(pkt.intVal, -1, 4);
                case ACTION_SET_FUNCTION -> {
                    if (be instanceof RBMKControlAutoBlockEntity auto) {
                        int ord = Mth.clamp(pkt.intVal, 0,
                                RBMKControlAutoBlockEntity.RBMKFunction.values().length - 1);
                        auto.function = RBMKControlAutoBlockEntity.RBMKFunction.values()[ord];
                    }
                }
                case ACTION_SET_PARAMS -> {
                    if (be instanceof RBMKControlAutoBlockEntity auto) {
                        auto.levelUpper = pkt.doubleVal4[0];
                        auto.levelLower = pkt.doubleVal4[1];
                        auto.heatUpper  = pkt.doubleVal4[2];
                        auto.heatLower  = pkt.doubleVal4[3];
                    }
                }
            }
            be.setChanged();
        });
    }

    // ─── Static send helpers ──────────────────────────────────────────────────

    public static void sendSetTarget(BlockPos pos, double target) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONTROL_CONTROL,
            new RBMKControlPacket(pos, ACTION_SET_TARGET, target, 0, null));
    }

    public static void sendSetColor(BlockPos pos, int color) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONTROL_CONTROL,
            new RBMKControlPacket(pos, ACTION_SET_COLOR, 0, color, null));
    }

    public static void sendSetFunction(BlockPos pos, int functionOrdinal) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONTROL_CONTROL,
            new RBMKControlPacket(pos, ACTION_SET_FUNCTION, 0, functionOrdinal, null));
    }

    public static void sendSetParams(BlockPos pos, double levelUpper, double levelLower, double heatUpper, double heatLower) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONTROL_CONTROL,
            new RBMKControlPacket(pos, ACTION_SET_PARAMS, 0, 0,
                new double[]{ levelUpper, levelLower, heatUpper, heatLower }));
    }
}
