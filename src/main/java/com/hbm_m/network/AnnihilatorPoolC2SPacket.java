package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Port of {@code TileEntityMachineAnnihilator.receiveControl} (1.7.10 Original) - pool-name text field. */
public class AnnihilatorPoolC2SPacket implements C2SPacket {

    private final BlockPos pos;
    private final String poolName;

    public AnnihilatorPoolC2SPacket(BlockPos pos, String poolName) {
        this.pos = pos;
        this.poolName = poolName;
    }

    public static AnnihilatorPoolC2SPacket decode(FriendlyByteBuf buf) {
        return new AnnihilatorPoolC2SPacket(buf.readBlockPos(), buf.readUtf(64));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(poolName, 64);
    }

    public static void handle(AnnihilatorPoolC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            BlockEntity be = player.serverLevel().getBlockEntity(msg.pos);
            if (be instanceof MachineAnnihilatorBlockEntity annihilator) {
                annihilator.setPoolName(msg.poolName);
            }
        });
    }

    public static void sendToServer(BlockPos pos, String poolName) {
        ModPacketHandler.sendToServer(ModPacketHandler.ANNIHILATOR_POOL,
                new AnnihilatorPoolC2SPacket(pos, poolName));
    }
}
