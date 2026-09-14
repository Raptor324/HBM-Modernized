package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.LiquefactorRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
/**
 * JEI-Kategorie des Verfluessigers (siehe {@link LiquefactorRecipe}).
 *
 * <p>Ein fester Stoff wird geschmolzen: Gegenstand hinein, Fluessigkeit heraus.</p>
 */
public class LiquefactorJeiCategory extends JeiFluidRecipeCategory<LiquefactorRecipe> {

    public static final RecipeType<LiquefactorRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "liquefactor", LiquefactorRecipe.class);

    public LiquefactorJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModItems.LIQUEFACTOR.get()) });
    }

    @Override
    public RecipeType<LiquefactorRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.liquefactor");
    }

    @Override
    protected List<FluidStack> getInputFluids(LiquefactorRecipe recipe) {
        return List.of();
    }

    @Override
    protected List<Ingredient> getInputItems(LiquefactorRecipe recipe) {
        return List.of(recipe.getInput());
    }

    @Override
    protected List<FluidStack> getOutputFluids(LiquefactorRecipe recipe) {
        return List.of(recipe.getOutput());
    }
}
//?} else {
/*public final class LiquefactorJeiCategory {
    private LiquefactorJeiCategory() {}
}*///?}
