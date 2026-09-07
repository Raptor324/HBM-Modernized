package com.hbm_m.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.fusion.FusionTorusBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Rezeptauswahl des Fusionstorus. Entspricht dem {@code NBTControlPacket} mit
 * {@code index = 0} und {@code selection = <name>} aus {@code TileEntityFusionTorus.receiveControl}.
 */
public class SetFusionRecipeC2SPacket implements C2SPacket {

    private final BlockPos blockPos;
    @Nullable
    private final ResourceLocation recipeId;

    public SetFusionRecipeC2SPacket(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        this.blockPos = blockPos;
        this.recipeId = recipeId;
    }

    public static SetFusionRecipeC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos blockPos = buf.readBlockPos();
        boolean hasRecipe = buf.readBoolean();
        ResourceLocation recipeId = hasRecipe ? buf.readResourceLocation() : null;
        return new SetFusionRecipeC2SPacket(blockPos, recipeId);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(recipeId != null);
        if (recipeId != null) buf.writeResourceLocation(recipeId);
    }

    public static void handle(SetFusionRecipeC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            // Original: hasPermission -> isUseableByPlayer, 32 Bloecke Radius.
            if (player.distanceToSqr(
                    msg.blockPos.getX() + 0.5,
                    msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 32 * 32) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof FusionTorusBlockEntity torus) {
                torus.setSelectedRecipeId(msg.recipeId);
            }
        });
    }

    public static void sendToServer(BlockPos blockPos, @Nullable ResourceLocation recipeId) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_FUSION_RECIPE,
                new SetFusionRecipeC2SPacket(blockPos, recipeId));
    }
}
