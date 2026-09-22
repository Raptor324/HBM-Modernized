package com.hbm_m.network;

import com.hbm_m.blockentity.machines.dfc.DFCStabilizerBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Die Wattzahl des Feldstabilisators. Im Original ist das ein {@code AuxButtonPacket}, dessen
 * erster Wert die eingetippte Zahl traegt.
 */
public class DFCStabilizerWattsC2SPacket implements C2SPacket {

    private final BlockPos blockPos;
    private final int watts;

    private DFCStabilizerWattsC2SPacket(BlockPos blockPos, int watts) {
        this.blockPos = blockPos;
        this.watts = watts;
    }

    public static DFCStabilizerWattsC2SPacket decode(FriendlyByteBuf buf) {
        return new DFCStabilizerWattsC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(watts);
    }

    public static void handle(DFCStabilizerWattsC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof DFCStabilizerBlockEntity stabilizer) stabilizer.setWatts(msg.watts);
        });
    }

    public static void send(BlockPos pos, int watts) {
        ModPacketHandler.sendToServer(ModPacketHandler.DFC_STABILIZER_WATTS,
                new DFCStabilizerWattsC2SPacket(pos, watts));
    }
}
