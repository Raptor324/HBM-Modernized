package com.hbm_m.network;

import com.hbm_m.inventory.menu.AnvilMenu;
import com.hbm_m.network.C2SPacket;
import com.hbm_m.recipe.AnvilRecipeManager;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

public class AnvilSelectRecipeC2SPacket implements C2SPacket {

    private final BlockPos pos;
    @Nullable
    private final ResourceLocation recipeId;

    public AnvilSelectRecipeC2SPacket(BlockPos pos, @Nullable ResourceLocation recipeId) {
        this.pos      = pos;
        this.recipeId = recipeId;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static AnvilSelectRecipeC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos          pos      = buf.readBlockPos();
        ResourceLocation  recipeId = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new AnvilSelectRecipeC2SPacket(pos, recipeId);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        if (recipeId != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(recipeId);
        } else {
            buf.writeBoolean(false);
        }
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(AnvilSelectRecipeC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.containerMenu instanceof AnvilMenu menu &&
                    menu.blockEntity.getBlockPos().equals(msg.pos)) {

                menu.blockEntity.setSelectedRecipeId(msg.recipeId);

                if (msg.recipeId != null) {
                    AnvilRecipeManager.getRecipe(player.level(), msg.recipeId)
                            .ifPresent(recipe -> menu.blockEntity.populateInputsFromPlayer(player, recipe));
                }
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos pos, @Nullable ResourceLocation recipeId) {
        ModPacketHandler.sendToServer(ModPacketHandler.ANVIL_SELECT_RECIPE,
                new AnvilSelectRecipeC2SPacket(pos, recipeId));
    }
}