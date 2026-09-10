package com.hbm_m.network;

import com.hbm_m.blockentity.machines.ForceFieldBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Der Schalter des Kraftfeldgenerators. Im Original ist das ein {@code AuxButtonPacket} mit dem
 * Schluessel 0 - der Knopf rechts in der Oberflaeche.
 */
public class ToggleForceFieldC2SPacket implements C2SPacket {

    private final BlockPos blockPos;

    private ToggleForceFieldC2SPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static ToggleForceFieldC2SPacket decode(FriendlyByteBuf buf) {
        return new ToggleForceFieldC2SPacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }

    public static void handle(ToggleForceFieldC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof ForceFieldBlockEntity field) field.toggle();
        });
    }

    public static void send(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.TOGGLE_FORCE_FIELD,
                new ToggleForceFieldC2SPacket(pos));
    }
}
