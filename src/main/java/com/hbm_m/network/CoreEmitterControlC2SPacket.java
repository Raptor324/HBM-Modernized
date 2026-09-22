package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineCoreEmitterBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Die beiden Schalter des DFC-Strahlers. Im Original ist das {@code AuxButtonPacket} mit
 * {@code (watts, 0)} fuer den Regler und {@code (0, 1)} fuer den An/Aus-Knopf.
 */
public class CoreEmitterControlC2SPacket implements C2SPacket {

    /** Original: {@code meta == 0} setzt die Wattzahl, {@code meta == 1} schaltet um. */
    private final BlockPos blockPos;
    private final int watts;
    private final int action;

    private CoreEmitterControlC2SPacket(BlockPos blockPos, int watts, int action) {
        this.blockPos = blockPos;
        this.watts = watts;
        this.action = action;
    }

    public static CoreEmitterControlC2SPacket decode(FriendlyByteBuf buf) {
        return new CoreEmitterControlC2SPacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(watts);
        buf.writeInt(action);
    }

    public static void handle(CoreEmitterControlC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (!(be instanceof MachineCoreEmitterBlockEntity emitter)) return;

            if (msg.action == 0) {
                emitter.setWatts(msg.watts);
            } else {
                emitter.toggle();
            }
        });
    }

    public static void sendWatts(BlockPos pos, int watts) {
        ModPacketHandler.sendToServer(ModPacketHandler.CORE_EMITTER_CONTROL,
                new CoreEmitterControlC2SPacket(pos, watts, 0));
    }

    public static void sendToggle(BlockPos pos) {
        ModPacketHandler.sendToServer(ModPacketHandler.CORE_EMITTER_CONTROL,
                new CoreEmitterControlC2SPacket(pos, 0, 1));
    }
}
