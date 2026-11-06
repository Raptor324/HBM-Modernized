package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.HydrotreaterRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-Kategorie des Hydrotreaters (siehe {@link HydrotreaterRecipe}).
 *
 * <p>Eingang plus Wasserstoff ergibt die behandelte Fluessigkeit und Sauergas. Das Slot-Layout
 * folgt dem Original-{@code NEIUniversalHandler} ({@code HydrotreatingHandler}): das Hauptfluid
 * steht rechts im Eingangsfeld (48,24), der Wasserstoff links daneben (30,24) - genau wie die
 * beiden Ausgaenge (102,24) und (120,24).</p>
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

    /**
     * Original-Layout ({@code NEIUniversalHandler.getInputCoords(2)}): Hauptfluid in (48,24),
     * Wasserstoff in (30,24). Die generische Basis wuerde beide Plätze tauschen, darum hier
     * die universellen Koordinaten.
     */
    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, HydrotreaterRecipe recipe, int inputXOffset) {
        List<FluidStack> fluids = getInputFluids(recipe);
        int[][] positions = JeiNeiLayout.getUniversalInputCoords(fluids.size());
        for (int i = 0; i < fluids.size(); i++) {
            addFluidSlot(builder, RecipeIngredientRole.INPUT,
                    positions[i][0] + inputXOffset, positions[i][1], fluids.get(i));
        }
    }

    @Override
    protected List<FluidStack> getOutputFluids(HydrotreaterRecipe recipe) {
        return List.of(
                FluidStack.create(recipe.getOutput(), recipe.getOutputMb()),
                FluidStack.create(recipe.getSourGas(), recipe.getSourGasMb()));
    }
}

