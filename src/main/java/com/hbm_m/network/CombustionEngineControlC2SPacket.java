package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Zuendschalter und Drossel des Verbrennungsmotors. Im Original sind das die beiden Schluessel
 * {@code turnOn} und {@code setting} im {@code NBTControlPacket}.
 */
public class CombustionEngineControlC2SPacket implements C2SPacket {

    /** {@code action == 0} setzt die Drossel, {@code action == 1} schaltet die Zuendung um. */
    private final BlockPos blockPos;
    private final int throttle;
    private final int action;

    private CombustionEngineControlC2SPacket(BlockPos blockPos, int throttle, int action) {
        this.blockPos = blockPos;
        this.throttle = throttle;
        this.action = action;
    }

    public static CombustionEngineControlC2SPacket decode(FriendlyByteBuf buf) {
        return new CombustionEngineControlC2SPacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(throttle);
        buf.writeInt(action);
    }

    public static void handle(CombustionEngineControlC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (!(be instanceof MachineCombustionEngineBlockEntity engine)) return;

            if (msg.action == 0) {
                engine.setThrottle(msg.throttle);
            } else {
                engine.toggleIgnition();
            }
        });
    }

    public static void sendThrottle(BlockPos pos, int throttle) {
        ModPacketHandler.sendToServer(ModPacketHandler.COMBUSTION_ENGINE_CONTROL,
                new CombustionEngineControlC2SPacket(pos, throttle, 0));
    }

    public static void sendToggle(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.COMBUSTION_ENGINE_CONTROL,
                new CombustionEngineControlC2SPacket(pos, 0, 1));
    }
}
