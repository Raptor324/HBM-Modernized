package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PyroOvenRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
/**
 * JEI-Kategorie des Pyrolyseofens (siehe {@link PyroOvenRecipe}).
 *
 * <p>Der Ofen nimmt wahlweise einen Gegenstand, eine Fluessigkeit oder beides und gibt
 * ebenso beides zurueck - darum sind hier alle vier Seiten wahlfrei.</p>
 */
public class PyroOvenJeiCategory extends JeiFluidRecipeCategory<PyroOvenRecipe> {

    public static final RecipeType<PyroOvenRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "pyrooven", PyroOvenRecipe.class);

    public PyroOvenJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.PYROOVEN.get()) });
    }

    @Override
    public RecipeType<PyroOvenRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.pyrooven");
    }

    @Override
    protected List<FluidStack> getInputFluids(PyroOvenRecipe recipe) {
        List<FluidStack> list = new ArrayList<>();
        if (recipe.getInputFluidStack() != null) list.add(recipe.getInputFluidStack());
        return list;
    }

    @Override
    protected List<Ingredient> getInputItems(PyroOvenRecipe recipe) {
        List<Ingredient> list = new ArrayList<>();
        if (recipe.getInputItem() != null) list.add(recipe.getInputItem());
        return list;
    }

    @Override
    protected List<FluidStack> getOutputFluids(PyroOvenRecipe recipe) {
        List<FluidStack> list = new ArrayList<>();
        if (recipe.getOutputFluidStack() != null) list.add(recipe.getOutputFluidStack());
        return list;
    }

    @Override
    protected List<ItemStack> getOutputItems(PyroOvenRecipe recipe) {
        List<ItemStack> list = new ArrayList<>();
        if (recipe.getOutputItem() != null) list.add(recipe.getOutputItem());
        return list;
    }
}
//?} else {
/*public final class PyroOvenJeiCategory {
    private PyroOvenJeiCategory() {}
}*///?}
