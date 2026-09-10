package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.FractionTowerRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

//? if forge {
/**
 * JEI-Kategorie des Fraktionierturms (siehe {@link FractionTowerRecipe}).
 *
 * <p>Eine Eingangsfluessigkeit, zwei Fraktionen.</p>
 */
public class FractionTowerJeiCategory extends JeiFluidRecipeCategory<FractionTowerRecipe> {

    public static final RecipeType<FractionTowerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "fraction_tower", FractionTowerRecipe.class);

    public FractionTowerJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModItems.FRACTION_TOWER.get()) });
    }

    @Override
    public RecipeType<FractionTowerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.fraction_tower");
    }

    @Override
    protected List<FluidStack> getInputFluids(FractionTowerRecipe recipe) {
        return List.of(FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()));
    }

    @Override
    protected List<FluidStack> getOutputFluids(FractionTowerRecipe recipe) {
        return List.of(
                FluidStack.create(recipe.getOutputA(), recipe.getOutputAMb()),
                FluidStack.create(recipe.getOutputB(), recipe.getOutputBMb()));
    }
}
//?} else {
/*public final class FractionTowerJeiCategory {
    private FractionTowerJeiCategory() {}
}*///?}
