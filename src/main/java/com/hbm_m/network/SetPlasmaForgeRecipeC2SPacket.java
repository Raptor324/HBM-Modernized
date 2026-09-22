package com.hbm_m.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.fusion.FusionPlasmaForgeBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Rezeptauswahl der Plasmaschmiede. Entspricht dem {@code NBTControlPacket} mit
 * {@code index = 0} aus {@code TileEntityFusionPlasmaForge.receiveControl}.
 */
public class SetPlasmaForgeRecipeC2SPacket implements C2SPacket {

    private final BlockPos blockPos;
    @Nullable
    private final ResourceLocation recipeId;

    public SetPlasmaForgeRecipeC2SPacket(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        this.blockPos = blockPos;
        this.recipeId = recipeId;
    }

    public static SetPlasmaForgeRecipeC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos blockPos = buf.readBlockPos();
        boolean hasRecipe = buf.readBoolean();
        ResourceLocation recipeId = hasRecipe ? buf.readResourceLocation() : null;
        return new SetPlasmaForgeRecipeC2SPacket(blockPos, recipeId);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(recipeId != null);
        if (recipeId != null) buf.writeResourceLocation(recipeId);
    }

    public static void handle(SetPlasmaForgeRecipeC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            // Original: hasPermission -> isUseableByPlayer, 8 Bloecke Radius (Vanilla-Container).
            if (player.distanceToSqr(
                    msg.blockPos.getX() + 0.5,
                    msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 64.0) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof FusionPlasmaForgeBlockEntity forge) {
                forge.setSelectedRecipeId(msg.recipeId);
            }
        });
    }

    public static void sendToServer(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_PLASMA_FORGE_RECIPE,
                new SetPlasmaForgeRecipeC2SPacket(blockPos, recipeId));
    }
}
