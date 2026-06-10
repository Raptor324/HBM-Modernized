package com.hbm_m.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SetChemPlantRecipeC2SPacket implements C2SPacket {

    private final BlockPos blockPos;
    @Nullable
    private final ResourceLocation recipeId;

    public SetChemPlantRecipeC2SPacket(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        this.blockPos = blockPos;
        this.recipeId = recipeId;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static SetChemPlantRecipeC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos         blockPos  = buf.readBlockPos();
        boolean          hasRecipe = buf.readBoolean();
        ResourceLocation recipeId  = hasRecipe ? buf.readResourceLocation() : null;
        return new SetChemPlantRecipeC2SPacket(blockPos, recipeId);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(recipeId != null);
        if (recipeId != null) buf.writeResourceLocation(recipeId);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(SetChemPlantRecipeC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(
                    msg.blockPos.getX() + 0.5,
                    msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 64.0) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof MachineChemicalPlantBlockEntity chemPlant) {
                chemPlant.setSelectedRecipe(msg.recipeId);
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_CHEM_RECIPE,
                new SetChemPlantRecipeC2SPacket(blockPos, recipeId));
    }
}