package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CatalyticReformerRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

//? if forge {
/**
 * JEI-Kategorie des katalytischen Reformers (siehe {@link CatalyticReformerRecipe}).
 *
 * <p>Eine Eingangsfluessigkeit, drei Ergebnisse.</p>
 */
public class CatalyticReformerJeiCategory extends JeiFluidRecipeCategory<CatalyticReformerRecipe> {

    public static final RecipeType<CatalyticReformerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "catalytic_reformer", CatalyticReformerRecipe.class);

    public CatalyticReformerJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModItems.CATALYTIC_REFORMER.get()) });
    }

    @Override
    public RecipeType<CatalyticReformerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.catalytic_reformer");
    }

    @Override
    protected List<FluidStack> getInputFluids(CatalyticReformerRecipe recipe) {
        return List.of(FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()));
    }

    @Override
    protected List<FluidStack> getOutputFluids(CatalyticReformerRecipe recipe) {
        return List.of(
                FluidStack.create(recipe.getOutputA(), recipe.getOutputAMb()),
                FluidStack.create(recipe.getOutputB(), recipe.getOutputBMb()),
                FluidStack.create(recipe.getOutputC(), recipe.getOutputCMb()));
    }
}
//?} else {
/*public final class CatalyticReformerJeiCategory {
    private CatalyticReformerJeiCategory() {}
}*///?}
