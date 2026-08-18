package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Port of {@code TileEntityMicrowave.handleButtonPacket} (1.7.10 Original) - speed +/- buttons. */
public class MicrowaveSpeedC2SPacket implements C2SPacket {

    private final BlockPos pos;
    private final int delta;

    public MicrowaveSpeedC2SPacket(BlockPos pos, int delta) {
        this.pos = pos;
        this.delta = delta;
    }

    public static MicrowaveSpeedC2SPacket decode(FriendlyByteBuf buf) {
        return new MicrowaveSpeedC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(delta);
    }

    public static void handle(MicrowaveSpeedC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);

            if (be instanceof MachineMicrowaveBlockEntity microwave) {
                microwave.adjustSpeed(msg.delta);
            }
        });
    }

    public static void sendToServer(BlockPos pos, int delta) {
        ModPacketHandler.sendToServer(ModPacketHandler.MICROWAVE_SPEED,
                new MicrowaveSpeedC2SPacket(pos, delta));
    }
}
