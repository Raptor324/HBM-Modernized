package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Schaltet einen der fuenf Bohrer-Modi (drill/crusher/walling/veinminer/silktouch) um. */
public class MiningDrillToggleC2SPacket implements C2SPacket {

    private final BlockPos pos;
    private final String toggle;

    public MiningDrillToggleC2SPacket(BlockPos pos, String toggle) {
        this.pos = pos;
        this.toggle = toggle;
    }

    public static MiningDrillToggleC2SPacket decode(FriendlyByteBuf buf) {
        return new MiningDrillToggleC2SPacket(buf.readBlockPos(), buf.readUtf());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(toggle);
    }

    public static void handle(MiningDrillToggleC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);

            if (be instanceof MachineMiningDrillBlockEntity drill) {
                drill.receiveToggle(msg.toggle);
            }
        });
    }

    public static void sendToServer(BlockPos pos, String toggle) {
        ModPacketHandler.sendToServer(ModPacketHandler.MINING_DRILL_TOGGLE,
                new MiningDrillToggleC2SPacket(pos, toggle));
    }
}
