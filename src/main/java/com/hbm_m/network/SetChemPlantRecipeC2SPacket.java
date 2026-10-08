package com.hbm_m.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineChemicalPlantBlockEntity;
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
    /** Modulindex (Chemiefabrik: 0-3, sonst 0) - Original receiveControl "index". */
    private final int index;

    public SetChemPlantRecipeC2SPacket(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        this(blockPos, recipeId, 0);
    }

    public SetChemPlantRecipeC2SPacket(BlockPos blockPos, @Nullable ResourceLocation recipeId, int index) {
        this.blockPos = blockPos;
        this.recipeId = recipeId;
        this.index = index;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static SetChemPlantRecipeC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos         blockPos  = buf.readBlockPos();
        boolean          hasRecipe = buf.readBoolean();
        ResourceLocation recipeId  = hasRecipe ? buf.readResourceLocation() : null;
        int              index     = buf.readVarInt();
        return new SetChemPlantRecipeC2SPacket(blockPos, recipeId, index);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(recipeId != null);
        if (recipeId != null) buf.writeResourceLocation(recipeId);
        buf.writeVarInt(index);
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
            } else if (be instanceof com.hbm_m.blockentity.machines.MachinePUREXBlockEntity purex) {
                purex.setSelectedRecipe(msg.recipeId);
            } else if (be instanceof com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity precass) {
                precass.setSelectedRecipe(msg.recipeId);
            } else if (be instanceof com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity factory) {
                factory.setSelectedRecipe(msg.index, msg.recipeId);
            } else if (be instanceof com.hbm_m.blockentity.machines.MachineAssemblyFactoryBlockEntity assemFac) {
                assemFac.setSelectedRecipe(msg.index, msg.recipeId);
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_CHEM_RECIPE,
                new SetChemPlantRecipeC2SPacket(blockPos, recipeId));
    }
}