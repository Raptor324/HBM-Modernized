package com.hbm_m.network;

import com.hbm_m.blockentity.machines.TurretBaseBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Button-Klicks im Turret-GUI: On/Off + die vier Ziel-Kategorie-Toggles (siehe {@link TurretBaseBlockEntity}). */
public class TurretControlPacket implements C2SPacket {

    public static final int ACTION_TOGGLE_ON = 0;
    public static final int ACTION_TOGGLE_PLAYERS = 1;
    public static final int ACTION_TOGGLE_ANIMALS = 2;
    public static final int ACTION_TOGGLE_MOBS = 3;
    public static final int ACTION_TOGGLE_MACHINES = 4;
    public static final int ACTION_CYCLE_FIRE_MODE = 5;
    /** Original {@code receiveControl}: KI-Chip-Name hinzufuegen ({@code "name"}) bzw. Eintrag loeschen ({@code "del"}). */
    public static final int ACTION_ADD_NAME = 6;
    public static final int ACTION_DEL_NAME = 7;

    private final BlockPos pos;
    private final int action;
    private final String name;
    private final int value;

    public TurretControlPacket(BlockPos pos, int action) {
        this(pos, action, "", 0);
    }

    public TurretControlPacket(BlockPos pos, int action, String name, int value) {
        this.pos = pos;
        this.action = action;
        this.name = name == null ? "" : name;
        this.value = value;
    }

    public static TurretControlPacket decode(FriendlyByteBuf buf) {
        return new TurretControlPacket(buf.readBlockPos(), buf.readInt(), buf.readUtf(64), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(action);
        buf.writeUtf(name, 64);
        buf.writeInt(value);
    }

    public static void handle(TurretControlPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            var blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof TurretBaseBlockEntity turret) {
                // Original IControlReceiver#hasPermission: nur wer das Inventar benutzen darf
                if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5, packet.pos.getZ() + 0.5) > 64D) return;
                if (packet.action == ACTION_ADD_NAME || packet.action == ACTION_DEL_NAME) {
                    turret.handleNameControl(packet.action, packet.name, packet.value);
                } else {
                    turret.handleButtonPress(packet.action);
                }
            }
        });
    }

    public static void sendToServer(BlockPos pos, int action) {
        ModPacketHandler.sendToServer(ModPacketHandler.TURRET_CONTROL,
                new TurretControlPacket(pos, action));
    }

    public static void sendToServer(BlockPos pos, int action, String name, int value) {
        ModPacketHandler.sendToServer(ModPacketHandler.TURRET_CONTROL,
                new TurretControlPacket(pos, action, name, value));
    }
}
