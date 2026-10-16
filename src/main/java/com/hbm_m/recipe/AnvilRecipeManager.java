package com.hbm_m.recipe;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.platform.recipe.RecipeHooks;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public final class AnvilRecipeManager {

    private AnvilRecipeManager() { }

    public static List<AnvilRecipe> getAllRecipes(Level level) {
        return RecipeHooks.getAllRecipes(level, AnvilRecipe.Type.INSTANCE);
    }

    //? if fabric {
    /*@Environment(EnvType.CLIENT)
    *///?}
    //? if forge {
    @OnlyIn(Dist.CLIENT)
    //?}
    public static List<AnvilRecipe> getClientRecipes() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        return level != null ? getAllRecipes(level) : Collections.emptyList();
    }

    public static Optional<AnvilRecipe> findRecipe(Level level, ItemStack slotA, ItemStack slotB, AnvilTier tier) {
        return RecipeHooks.getAllRecipes(level, AnvilRecipe.Type.INSTANCE).stream()
                .filter(recipe -> recipe.matches(slotA, slotB) && recipe.canCraftOn(tier))
                .findFirst();
    }

    public static Optional<AnvilRecipe> getRecipe(Level level, ResourceLocation id) {
        return Optional.ofNullable(RecipeHooks.getAllRecipesById(level, AnvilRecipe.Type.INSTANCE).get(id));
    }
}
