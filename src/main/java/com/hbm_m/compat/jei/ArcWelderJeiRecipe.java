package com.hbm_m.compat.jei;

import com.hbm_m.inventory.recipes.ArcWelderRecipes;
import com.hbm_m.inventory.recipes.ArcWelderRecipes.ArcIngredient;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

//? if forge {
public class ArcWelderJeiRecipe {

    private final List<List<ItemStack>> inputs;
    private final ItemStack output;
    private final @org.jetbrains.annotations.Nullable ArcWelderRecipes.FluidRequirement fluid;

    public ArcWelderJeiRecipe(ArcWelderRecipes.ArcWelderRecipe recipe) {
        this.inputs = Arrays.stream(recipe.ingredients)
                .map(ing -> List.of(ing.ingredient().getItems()))
                .toList();
        this.output = recipe.output.copy();
        this.fluid  = recipe.fluid;
    }

    public List<List<ItemStack>> getInputs() { return inputs; }
    public ItemStack getOutput()             { return output;  }
    public @org.jetbrains.annotations.Nullable ArcWelderRecipes.FluidRequirement getFluid() { return fluid; }

    public static List<ArcWelderJeiRecipe> fromRecipes() {
        return ArcWelderRecipes.recipes.stream()
                .map(ArcWelderJeiRecipe::new)
                .toList();
    }
}
//?} else {
/*public final class ArcWelderJeiRecipe { private ArcWelderJeiRecipe() {} }
*///?}
