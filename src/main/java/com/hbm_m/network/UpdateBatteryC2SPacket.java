package com.hbm_m.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import com.hbm_m.block.entity.custom.machines.MachineBatteryBlockEntity;

public class UpdateBatteryC2SPacket {
    private final BlockPos pos;
    private final int buttonId;

    public UpdateBatteryC2SPacket(BlockPos pos, int buttonId) {
        this.pos = pos;
        this.buttonId = buttonId;
    }

    public UpdateBatteryC2SPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.buttonId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(buttonId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player.level().getBlockEntity(pos) instanceof MachineBatteryBlockEntity battery) {
                battery.handleButtonPress(buttonId);
            }
        });
        return true;
    }
}