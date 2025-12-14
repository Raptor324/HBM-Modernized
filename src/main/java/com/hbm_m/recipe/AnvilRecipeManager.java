package com.hbm_m.recipe;

import com.hbm_m.block.custom.machines.anvils.AnvilTier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AnvilRecipeManager {

    private AnvilRecipeManager() { }

    public static List<AnvilRecipe> getAllRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(AnvilRecipe.Type.INSTANCE);
    }

    @OnlyIn(Dist.CLIENT)
    public static List<AnvilRecipe> getClientRecipes() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        return level != null ? getAllRecipes(level) : Collections.emptyList();
    }

    public static List<AnvilRecipe> searchRecipes(Level level, String query) {
        List<AnvilRecipe> recipes = getAllRecipes(level);
        if (query == null || query.trim().isEmpty()) {
            return recipes;
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);
        RegistryAccess access = level.registryAccess();
        return recipes.stream()
                .filter(recipe -> {
                    ItemStack output = recipe.getResultItem(access);
                    String itemName = output.getHoverName().getString().toLowerCase(Locale.ROOT);
                    return itemName.contains(lowerQuery);
                })
                .collect(Collectors.toList());
    }

    public static Optional<AnvilRecipe> findRecipe(Level level, ItemStack slotA, ItemStack slotB, AnvilTier tier) {
        return level.getRecipeManager()
                .getAllRecipesFor(AnvilRecipe.Type.INSTANCE)
                .stream()
                .filter(recipe -> recipe.matches(slotA, slotB) && recipe.canCraftOn(tier))
                .findFirst();
    }

    public static Optional<AnvilRecipe> getRecipe(Level level, ResourceLocation id) {
        return level.getRecipeManager()
                .getAllRecipesFor(AnvilRecipe.Type.INSTANCE)
                .stream()
                .filter(recipe -> recipe.getId().equals(id))
                .findFirst();
    }
}
