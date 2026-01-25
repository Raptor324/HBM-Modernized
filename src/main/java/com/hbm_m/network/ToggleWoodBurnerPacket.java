package com.hbm_m.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import com.hbm_m.block.entity.custom.machines.MachineWoodBurnerBlockEntity;

public class ToggleWoodBurnerPacket {
    private final BlockPos pos;

    public ToggleWoodBurnerPacket(BlockPos pos) {
        this.pos = pos;
    }

    // Конструктор для десериализации
    public ToggleWoodBurnerPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    // Сериализация (запись в буфер)
    public static void encode(ToggleWoodBurnerPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    // Десериализация (чтение из буфера)
    public static ToggleWoodBurnerPacket decode(FriendlyByteBuf buf) {
        return new ToggleWoodBurnerPacket(buf);
    }

    // Обработка пакета на сервере
    public static void handle(ToggleWoodBurnerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // Выполняется на сервере
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(msg.pos);

            if (blockEntity instanceof MachineWoodBurnerBlockEntity woodBurner) {
                // Переключаем состояние
                boolean currentState = woodBurner.isEnabled();
                woodBurner.setEnabled(!currentState);
                woodBurner.setChanged();
            }
        });
        context.setPacketHandled(true);
    }
}