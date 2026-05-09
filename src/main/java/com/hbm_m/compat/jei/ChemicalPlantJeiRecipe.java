package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.recipe.ChemicalPlantRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * Wrapper class for displaying Chemical Plant recipes in JEI.
 * Does not implement Recipe<Container>, it's just a display object.
 */
public class ChemicalPlantJeiRecipe {
    private final ResourceLocation recipeId;
    private final List<List<ItemStack>> itemInputStacks;
    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidStack> fluidOutputs;
    private final int duration;

    public ChemicalPlantJeiRecipe(ChemicalPlantRecipe recipe) {
        this.recipeId = recipe.getId();
        this.itemInputStacks = new ArrayList<>();
        for (var in : recipe.getItemInputs()) {
            ItemStack[] variants = in.ingredient().getItems();
            List<ItemStack> stacks = new ArrayList<>(Math.max(1, variants.length));
            if (variants.length == 0) {
                stacks.add(ItemStack.EMPTY);
            } else {
                for (ItemStack v : variants) {
                    if (v.isEmpty()) continue;
                    ItemStack c = v.copy();
                    c.setCount(Math.max(1, in.count()));
                    stacks.add(c);
                }
                if (stacks.isEmpty()) {
                    stacks.add(ItemStack.EMPTY);
                }
            }
            itemInputStacks.add(stacks);
        }
        this.fluidInputs = new ArrayList<>();
        for (var fin : recipe.getFluidInputs()) {
            ResourceLocation fid = fin.fluidId();
            var fluid = BuiltInRegistries.FLUID.get(fid);
            if (fluid == null || fluid == Fluids.EMPTY) continue;
            this.fluidInputs.add(FluidStack.create(fluid, fin.amount()));
        }
        this.itemOutputs = recipe.getItemOutputs();
        this.fluidOutputs = recipe.getFluidOutputs();
        this.duration = recipe.getDuration();
    }

    public ResourceLocation getRecipeId() {
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
