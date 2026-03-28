package com.hbm_m.network;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

public class SetChemPlantRecipeC2SPacket {
    private final BlockPos blockPos;
    @Nullable
    private final String recipeId;

    public SetChemPlantRecipeC2SPacket(BlockPos blockPos, @Nullable String recipeId) {
        this.blockPos = blockPos;
        this.recipeId = recipeId;
    }

    public SetChemPlantRecipeC2SPacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        boolean hasRecipe = buf.readBoolean();
        this.recipeId = hasRecipe ? buf.readUtf(256) : null;
    }

    public static void encode(SetChemPlantRecipeC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.blockPos);
        buf.writeBoolean(packet.recipeId != null);
        if (packet.recipeId != null) {
            buf.writeUtf(packet.recipeId, 256);
        }
    }

    public static SetChemPlantRecipeC2SPacket decode(FriendlyByteBuf buf) {
        return new SetChemPlantRecipeC2SPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > 64.0) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
            if (blockEntity instanceof MachineChemicalPlantBlockEntity chemPlant) {
                chemPlant.setRecipe(recipeId);
            }
        });
        return true;
    }
}
