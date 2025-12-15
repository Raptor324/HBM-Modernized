package com.hbm_m.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.hbm_m.block.entity.custom.machines.MachineAdvancedAssemblerBlockEntity;

/**
 * Пакет от клиента к серверу для установки рецепта в продвинутой сборочной машине.
 * Поддерживает null для сброса рецепта.
 */
public class SetAssemblerRecipeC2SPacket {
    private final BlockPos blockPos;
    @Nullable
    private final ResourceLocation recipeId;

    /**
     * Конструктор для создания пакета ПЕРЕД отправкой.
     * @param blockPos Позиция блока машины
     * @param recipeId ID выбранного рецепта (может быть null для сброса)
     */
    public SetAssemblerRecipeC2SPacket(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        this.blockPos = blockPos;
        this.recipeId = recipeId;
    }

    /**
     * Конструктор для декодирования на принимающей стороне.
     */
    public SetAssemblerRecipeC2SPacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        // Читаем флаг наличия рецепта
        boolean hasRecipe = buf.readBoolean();
        this.recipeId = hasRecipe ? buf.readResourceLocation() : null;
    }

    /**
     * Кодировщик с поддержкой null.
     */
    public static void encode(SetAssemblerRecipeC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.blockPos);
        // Записываем флаг наличия рецепта
        buf.writeBoolean(packet.recipeId != null);
        if (packet.recipeId != null) {
            buf.writeResourceLocation(packet.recipeId);
        }
    }

    /**
     * Декодер.
     */
    public static SetAssemblerRecipeC2SPacket decode(FriendlyByteBuf buf) {
        return new SetAssemblerRecipeC2SPacket(buf);
    }

    /**
     * Обработчик на сервере.
     */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Проверяем, что игрок достаточно близко к машине
            if (player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > 64.0) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
            if (blockEntity instanceof MachineAdvancedAssemblerBlockEntity assembler) {
                // recipeId может быть null для сброса рецепта
                assembler.setSelectedRecipe(recipeId);
            }
        });
        return true;
    }
}