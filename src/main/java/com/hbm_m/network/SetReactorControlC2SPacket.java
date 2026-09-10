package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineReactorControlBlockEntity;
import com.hbm_m.blockentity.machines.MachineReactorControlBlockEntity.RodFunction;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Einstellungen des Reaktorsteuerpults. Entspricht {@code TileEntityReactorControl.receiveControl}:
 * entweder die Kennlinie ({@code function}) oder die vier Schwellen als Block.
 */
public class SetReactorControlC2SPacket implements C2SPacket {

    private final BlockPos blockPos;
    /** true = nur die Kennlinie setzen, false = die vier Schwellen setzen. */
    private final boolean functionOnly;
    private final int function;
    private final double levelLower;
    private final double levelUpper;
    private final double heatLower;
    private final double heatUpper;

    private SetReactorControlC2SPacket(BlockPos blockPos, boolean functionOnly, int function,
                                       double levelLower, double levelUpper,
                                       double heatLower, double heatUpper) {
        this.blockPos = blockPos;
        this.functionOnly = functionOnly;
        this.function = function;
        this.levelLower = levelLower;
        this.levelUpper = levelUpper;
        this.heatLower = heatLower;
        this.heatUpper = heatUpper;
    }

    public static SetReactorControlC2SPacket decode(FriendlyByteBuf buf) {
        return new SetReactorControlC2SPacket(buf.readBlockPos(), buf.readBoolean(), buf.readInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(functionOnly);
        buf.writeInt(function);
        buf.writeDouble(levelLower);
        buf.writeDouble(levelUpper);
        buf.writeDouble(heatLower);
        buf.writeDouble(heatUpper);
    }

    public static void handle(SetReactorControlC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            // Reichweitenpruefung wie bei den uebrigen Steuerpaketen dieses Ports.
            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (!(be instanceof MachineReactorControlBlockEntity control)) return;

            if (msg.functionOnly) {
                RodFunction[] values = RodFunction.values();
                if (msg.function >= 0 && msg.function < values.length) {
                    control.setFunction(values[msg.function]);
                }
            } else {
                control.applySettings(msg.levelLower, msg.levelUpper, msg.heatLower, msg.heatUpper);
            }
        });
    }

    /** Original: {@code receiveControl} mit dem Schluessel {@code function}. */
    public static void sendFunction(BlockPos pos, RodFunction function) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_REACTOR_CONTROL,
                new SetReactorControlC2SPacket(pos, true, function.ordinal(), 0, 0, 0, 0));
    }

    /** Original: {@code receiveControl} mit den vier Schwellenwerten. */
    public static void sendBounds(BlockPos pos, double levelLower, double levelUpper,
                                  double heatLower, double heatUpper) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_REACTOR_CONTROL,
                new SetReactorControlC2SPacket(pos, false, 0, levelLower, levelUpper, heatLower, heatUpper));
    }
}
