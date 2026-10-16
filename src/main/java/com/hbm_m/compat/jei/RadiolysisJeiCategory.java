package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.RadiolysisRecipe;

import dev.architectury.fluid.FluidStack;
//? if forge {
//? if forge {
import mezz.jei.api.forge.ForgeTypes;
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
//? if forge {
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
//?}
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Radiolysis-Kammer recipes (siehe {@link RadiolysisRecipe}): 100mB Eingangsfluid
 * -> outA + optional outB (kein Dampf-Co-Input, anders als {@link CrackingTowerJeiCategory}).
 *
 * <p>Data-driven: рецепты читаются напрямую aus {@code RecipeManager} (JSON {@code hbm_m:radiolysis}),
 * ранее — статический {@code RadiolysisRecipes} (делегат в {@code CrackingTowerRecipes}).</p>
 */
//? if forge {
public class RadiolysisJeiCategory extends JeiGenericRecipeCategory<RadiolysisRecipe> {

    public static final RecipeType<RadiolysisRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "radiolysis", RadiolysisRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    public RadiolysisJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.RADIOLYSIS.get())
        });
    }

    @Override
    public RecipeType<RadiolysisRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.radiolysis");
    }

    @Override
    protected int getInputCount(RadiolysisRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(RadiolysisRecipe recipe) {
        return recipe.hasOutputB() ? 2 : 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(RadiolysisRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, RadiolysisRecipe recipe, int inputXOffset) {
        addFluidSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22,
                FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, RadiolysisRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                positions[0][0] + outputXOffset, positions[0][1],
                FluidStack.create(recipe.getOutputA(), recipe.getOutputAMb()));

        if (recipe.hasOutputB()) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[1][0] + outputXOffset, positions[1][1],
                    FluidStack.create(recipe.getOutputB(), recipe.getOutputBMb()));
        }
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid) {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                //? if forge {
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                //?} elif neoforge {
                /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                *///?}
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, RadiolysisRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Radiolysis-Rezepte.
    }
}
//?} else {
/*public final class RadiolysisJeiCategory {
    private RadiolysisJeiCategory() {}
}*///?}
