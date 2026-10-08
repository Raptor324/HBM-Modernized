package com.hbm_m.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.hbm_m.inventory.menu.AnvilMenu;
import com.hbm_m.recipe.AnvilRecipeManager;

import dev.architectury.networking.NetworkManager.PacketContext;

/** 1:1 {@code AnvilCraftPacket}: Konstruktionsrezept (hier per ID statt Listenindex) und Modus (1 = Shift, so oft wie moeglich). */
public class AnvilCraftC2SPacket implements C2SPacket {
    private final ResourceLocation recipe;
    private final int mode;

    public AnvilCraftC2SPacket(ResourceLocation recipe, int mode) {
        this.recipe = recipe;
        this.mode = mode;
    }

    public static AnvilCraftC2SPacket decode(FriendlyByteBuf buffer) {
        return new AnvilCraftC2SPacket(buffer.readResourceLocation(), buffer.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.recipe);
        buffer.writeInt(this.mode);
    }

    public static void handle(AnvilCraftC2SPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof AnvilMenu anvil)) return; //player isn't even using an anvil -> bad
            AnvilRecipeManager.getRecipe(player.level(), packet.recipe) //recipe is out of range -> bad
                    .ifPresent(recipe -> anvil.craftConstruction(player, recipe, packet.mode));
        });
    }
}
