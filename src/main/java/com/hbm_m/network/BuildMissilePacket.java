package com.hbm_m.network;

import com.hbm_m.block.entity.machines.MissileAssemblyBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BuildMissilePacket implements C2SPacket {

    private final BlockPos pos;

    public BuildMissilePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static BuildMissilePacket decode(FriendlyByteBuf buf) {
        return new BuildMissilePacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void handle(BuildMissilePacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);

            if (be instanceof MissileAssemblyBlockEntity assembly) {
                assembly.construct();
            }
        });
    }
}
