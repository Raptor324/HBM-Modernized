package com.hbm_m.network;

import com.hbm_m.menu.AnvilMenu;
import com.hbm_m.recipe.AnvilRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class AnvilSelectRecipeC2SPacket {
    private final BlockPos pos;
    @Nullable
    private final ResourceLocation recipeId;

    public AnvilSelectRecipeC2SPacket(BlockPos pos, @Nullable ResourceLocation recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public static void encode(AnvilSelectRecipeC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        if (packet.recipeId != null) {
            buffer.writeBoolean(true);
            buffer.writeResourceLocation(packet.recipeId);
        } else {
            buffer.writeBoolean(false);
        }
    }

    public static AnvilSelectRecipeC2SPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        ResourceLocation recipeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        return new AnvilSelectRecipeC2SPacket(pos, recipeId);
    }

    public static void handle(AnvilSelectRecipeC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (player.containerMenu instanceof AnvilMenu menu &&
                    menu.blockEntity.getBlockPos().equals(packet.pos)) {
                menu.blockEntity.setSelectedRecipeId(packet.recipeId);
                if (packet.recipeId != null) {
                    AnvilRecipeManager.getRecipe(player.level(), packet.recipeId)
                            .ifPresent(recipe -> menu.blockEntity.populateInputsFromPlayer(player, recipe));
                }
            }
        });
        context.setPacketHandled(true);
    }
}

