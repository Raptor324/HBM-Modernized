package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S packet for all RBMK console control actions:
 *   - Control rod level change
 *   - AZ-5 emergency shutdown
 *   - Color group assignment
 *   - Screen type cycling
 *   - Set reactor origin
 */
public class RBMKConsoleControlPacket implements C2SPacket {

    public static final int ACTION_SET_LEVEL      = 0;
    public static final int ACTION_AZ5            = 1;
    public static final int ACTION_ASSIGN_COLOR   = 2;
    public static final int ACTION_CYCLE_SCREEN   = 3;
    public static final int ACTION_SET_ORIGIN     = 4;
    public static final int ACTION_ASSIGN_SCREEN  = 5;

    private final BlockPos consolePos;
    private final int      action;
    /** Payload meaning varies by action (see handle). */
    private final double   doubleVal;
    private final int      intVal;
    private final int[]    selectedIndices;

    public RBMKConsoleControlPacket(BlockPos consolePos, int action, double doubleVal, int intVal, int[] selectedIndices) {
        this.consolePos      = consolePos;
        this.action          = action;
        this.doubleVal       = doubleVal;
        this.intVal          = intVal;
        this.selectedIndices = selectedIndices != null ? selectedIndices : new int[0];
    }

    public static RBMKConsoleControlPacket decode(FriendlyByteBuf buf) {
        BlockPos pos    = buf.readBlockPos();
        int     action  = buf.readByte();
        double  dVal    = buf.readDouble();
        int     iVal    = buf.readInt();
        int     len     = buf.readInt();
        int[]   sel     = new int[len];
        for (int i = 0; i < len; i++) sel[i] = buf.readInt();
        return new RBMKConsoleControlPacket(pos, action, dVal, iVal, sel);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(consolePos);
        buf.writeByte(action);
        buf.writeDouble(doubleVal);
        buf.writeInt(intVal);
        buf.writeInt(selectedIndices.length);
        for (int i : selectedIndices) buf.writeInt(i);
    }

    public static void handle(RBMKConsoleControlPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (player.level().getBlockEntity(pkt.consolePos) instanceof MachineRbmkConsoleBlockEntity console) {
                console.handleControl(player.serverLevel(), pkt.action, pkt.doubleVal, pkt.intVal, pkt.selectedIndices);
            }
        });
    }

    // ─── Static send helpers ──────────────────────────────────────────────────

    public static void sendSetLevel(BlockPos pos, double level, int[] selected) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONSOLE_CONTROL,
            new RBMKConsoleControlPacket(pos, ACTION_SET_LEVEL, level, 0, selected));
    }

    public static void sendAZ5(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONSOLE_CONTROL,
            new RBMKConsoleControlPacket(pos, ACTION_AZ5, 0, 0, null));
    }

    public static void sendAssignColor(BlockPos pos, int color, int[] selected) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONSOLE_CONTROL,
            new RBMKConsoleControlPacket(pos, ACTION_ASSIGN_COLOR, 0, color, selected));
    }

    public static void sendCycleScreen(BlockPos pos, int screenId) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONSOLE_CONTROL,
            new RBMKConsoleControlPacket(pos, ACTION_CYCLE_SCREEN, 0, screenId, null));
    }

    public static void sendAssignScreen(BlockPos pos, int screenId, int[] selected) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONSOLE_CONTROL,
            new RBMKConsoleControlPacket(pos, ACTION_ASSIGN_SCREEN, 0, screenId, selected));
    }

    public static void sendSetOrigin(BlockPos consolePos, BlockPos origin) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CONSOLE_CONTROL,
            new RBMKConsoleControlPacket(consolePos, ACTION_SET_ORIGIN, 0,
                0, new int[]{ origin.getX(), origin.getY(), origin.getZ() }));
    }
}
