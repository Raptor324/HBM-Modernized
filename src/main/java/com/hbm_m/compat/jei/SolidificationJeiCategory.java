package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.SolidificationRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
/**
 * JEI-Kategorie der Verfestigung (siehe {@link SolidificationRecipe}).
 *
 * <p>Der umgekehrte Weg: Fluessigkeit hinein, fester Stoff heraus.</p>
 */
public class SolidificationJeiCategory extends JeiFluidRecipeCategory<SolidificationRecipe> {

    public static final RecipeType<SolidificationRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "solidifier", SolidificationRecipe.class);

    public SolidificationJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.SOLIDIFIER.get()) });
    }

    @Override
    public RecipeType<SolidificationRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.solidifier");
    }

    @Override
    protected List<FluidStack> getInputFluids(SolidificationRecipe recipe) {
        return List.of(recipe.getInput());
    }

    @Override
    protected List<FluidStack> getOutputFluids(SolidificationRecipe recipe) {
        return List.of();
    }

    @Override
    protected List<ItemStack> getOutputItems(SolidificationRecipe recipe) {
        return List.of(recipe.getOutput());
    }
}
//?} else {
/*public final class SolidificationJeiCategory {
    private SolidificationJeiCategory() {}
}*///?}
