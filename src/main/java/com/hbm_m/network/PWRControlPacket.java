package com.hbm_m.network;

import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** C2S packet setting the PWR reactor's control-rod target level (0-100). */
public class PWRControlPacket implements C2SPacket {

    private final BlockPos pos;
    private final double rodTarget;

    public PWRControlPacket(BlockPos pos, double rodTarget) {
        this.pos = pos;
        this.rodTarget = rodTarget;
    }

    public static PWRControlPacket decode(FriendlyByteBuf buf) {
        return new PWRControlPacket(buf.readBlockPos(), buf.readDouble());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeDouble(rodTarget);
    }

    public static void handle(PWRControlPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            var blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof PWRControllerBlockEntity pwr) {
                pwr.setRodTarget(packet.rodTarget);
            }
        });
    }

    public static void sendToServer(BlockPos pos, double rodTarget) {
        ModPacketHandler.sendToServer(ModPacketHandler.PWR_CONTROL,
                new PWRControlPacket(pos, rodTarget));
    }
}
