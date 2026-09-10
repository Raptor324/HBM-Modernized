package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.VacuumDistillRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

//? if forge {
/**
 * JEI-Kategorie der Vakuumdestillation (siehe {@link VacuumDistillRecipe}).
 *
 * <p>Eine Eingangsfluessigkeit, vier Schnitte: schwer, Reformat, leicht und sauer.</p>
 */
public class VacuumDistillJeiCategory extends JeiFluidRecipeCategory<VacuumDistillRecipe> {

    public static final RecipeType<VacuumDistillRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "vacuum_distill", VacuumDistillRecipe.class);

    public VacuumDistillJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModItems.VACUUM_DISTILL.get()) });
    }

    @Override
    public RecipeType<VacuumDistillRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.vacuum_distill");
    }

    @Override
    protected List<FluidStack> getInputFluids(VacuumDistillRecipe recipe) {
        return List.of(FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()));
    }

    @Override
    protected List<FluidStack> getOutputFluids(VacuumDistillRecipe recipe) {
        return List.of(
                FluidStack.create(recipe.getHeavy(), recipe.getHeavyMb()),
                FluidStack.create(recipe.getReformate(), recipe.getReformateMb()),
                FluidStack.create(recipe.getLight(), recipe.getLightMb()),
                FluidStack.create(recipe.getSour(), recipe.getSourMb()));
    }
}
//?} else {
/*public final class VacuumDistillJeiCategory {
    private VacuumDistillJeiCategory() {}
}*///?}
