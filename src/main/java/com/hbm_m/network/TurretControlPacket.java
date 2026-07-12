package com.hbm_m.network;

import com.hbm_m.block.entity.machines.TurretBaseBlockEntity;

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

    private final BlockPos pos;
    private final int action;

    public TurretControlPacket(BlockPos pos, int action) {
        this.pos = pos;
        this.action = action;
    }

    public static TurretControlPacket decode(FriendlyByteBuf buf) {
        return new TurretControlPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(action);
    }

    public static void handle(TurretControlPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            var blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof TurretBaseBlockEntity turret) {
                turret.handleButtonPress(packet.action);
            }
        });
    }

    public static void sendToServer(BlockPos pos, int action) {
        ModPacketHandler.sendToServer(ModPacketHandler.TURRET_CONTROL,
                new TurretControlPacket(pos, action));
    }
}
