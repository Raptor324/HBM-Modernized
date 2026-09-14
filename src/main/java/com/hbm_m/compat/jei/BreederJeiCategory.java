package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.BreederRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
/**
 * JEI-Kategorie des Brutreaktors (siehe {@link BreederRecipe}).
 *
 * <p>Ein Gegenstand hinein, ein Gegenstand heraus - der Neutronenfluss macht den Rest.</p>
 */
public class BreederJeiCategory extends JeiFluidRecipeCategory<BreederRecipe> {

    public static final RecipeType<BreederRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "breeder", BreederRecipe.class);

    public BreederJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModItems.BREEDER.get()) });
    }

    @Override
    public RecipeType<BreederRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.breeder");
    }

    @Override
    protected List<FluidStack> getInputFluids(BreederRecipe recipe) {
        return List.of();
    }

    @Override
    protected List<Ingredient> getInputItems(BreederRecipe recipe) {
        return List.of(recipe.getInput());
    }

    @Override
    protected List<FluidStack> getOutputFluids(BreederRecipe recipe) {
        return List.of();
    }

    @Override
    protected List<ItemStack> getOutputItems(BreederRecipe recipe) {
        return List.of(recipe.getOutput());
    }
}
//?} else {
/*public final class BreederJeiCategory {
    private BreederJeiCategory() {}
}*///?}
