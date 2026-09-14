package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.HydrotreaterRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

//? if forge {
/**
 * JEI-Kategorie des Hydrotreaters (siehe {@link HydrotreaterRecipe}).
 *
 * <p>Eingang plus Wasserstoff ergibt die behandelte Fluessigkeit und Sauergas - der
 * Wasserstoff steht darum als zweiter Eingangsplatz.</p>
 */
public class HydrotreaterJeiCategory extends JeiFluidRecipeCategory<HydrotreaterRecipe> {

    public static final RecipeType<HydrotreaterRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "hydrotreater", HydrotreaterRecipe.class);

    public HydrotreaterJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModItems.HYDROTREATER.get()) });
    }

    @Override
    public RecipeType<HydrotreaterRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.hydrotreater");
    }

    @Override
    protected List<FluidStack> getInputFluids(HydrotreaterRecipe recipe) {
        return List.of(
                FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()),
                FluidStack.create(recipe.getHydrogen().getFluid(), recipe.getHydrogenMb()));
    }

    @Override
    protected List<FluidStack> getOutputFluids(HydrotreaterRecipe recipe) {
        return List.of(
                FluidStack.create(recipe.getOutput(), recipe.getOutputMb()),
                FluidStack.create(recipe.getSourGas(), recipe.getSourGasMb()));
    }
}
//?} else {
/*public final class HydrotreaterJeiCategory {
    private HydrotreaterJeiCategory() {}
}*///?}
