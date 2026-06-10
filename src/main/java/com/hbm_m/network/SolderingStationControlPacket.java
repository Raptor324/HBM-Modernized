package com.hbm_m.network;

import com.hbm_m.block.entity.machines.MachineSolderingStationBlockEntity;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** C2S packet to toggle the Collision Prevention flag on the Soldering Station. */
public class SolderingStationControlPacket implements C2SPacket {

    private final BlockPos pos;

    public SolderingStationControlPacket(BlockPos pos) { this.pos = pos; }

    public static SolderingStationControlPacket decode(FriendlyByteBuf buf) {
        return new SolderingStationControlPacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) { buf.writeBlockPos(pos); }

    public static void handle(SolderingStationControlPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (player.level().getBlockEntity(pkt.pos) instanceof MachineSolderingStationBlockEntity be)
                be.toggleCollisionPrevention();
        });
    }

    public static void sendToServer(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.SOLDERING_STATION_CONTROL,
                new SolderingStationControlPacket(pos));
    }
}
