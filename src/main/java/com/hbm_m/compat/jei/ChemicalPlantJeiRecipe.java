package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.recipe.ChemicalPlantRecipes.ChemicalRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipes.RecipeInput;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.ItemStack;

/**
 * Wrapper class for displaying Chemical Plant recipes in JEI.
 * Does not implement Recipe<Container>, it's just a display object.
 */
public class ChemicalPlantJeiRecipe {
    private final String recipeId;
    private final List<List<ItemStack>> itemInputStacks;
    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidStack> fluidOutputs;
    private final int duration;

    public ChemicalPlantJeiRecipe(ChemicalRecipe recipe) {
        this.recipeId = recipe.getId();
        this.itemInputStacks = new ArrayList<>();
        for (RecipeInput input : recipe.getItemInputs()) {
            itemInputStacks.add(input.getDisplayStacks());
        }
        this.fluidInputs = recipe.getFluidInputs();
        this.itemOutputs = recipe.getItemOutputs();
        this.fluidOutputs = recipe.getFluidOutputs();
        this.duration = recipe.getDuration();
    }

    public String getRecipeId() {
        return recipeId;
    }

    /**
     * Get item inputs as lists (for tag support - multiple items can match).
     */
    public List<List<ItemStack>> getItemInputStacks() {
        return itemInputStacks;
    }

    public List<FluidStack> getFluidInputs() {
        return fluidInputs;
    }

    public List<ItemStack> getItemOutputs() {
        return itemOutputs;
    }

    public List<FluidStack> getFluidOutputs() {
        return fluidOutputs;
    }

    public int getDuration() {
        return duration;
    }
}
