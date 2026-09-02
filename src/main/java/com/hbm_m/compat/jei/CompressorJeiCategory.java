package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CompressorRecipe;

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
 * JEI category for Compressor special-case recipes (siehe {@link CompressorRecipe} - der
 * generische "immer +1 Druck"-Fallback ist selbsterklaerend und wird nicht als JEI-Rezept gezeigt).
 *
 * <p>Data-driven: рецепты читаются напрямую из {@code RecipeManager} (JSON {@code hbm_m:compressor}),
 * ранее — статический {@code CompressorRecipes}.</p>
 */
//? if forge {
public class CompressorJeiCategory extends JeiGenericRecipeCategory<CompressorRecipe> {

    public static final RecipeType<CompressorRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "compressor", CompressorRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 16_000;

    public CompressorJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.COMPRESSOR.get())
        });
    }

    @Override
    public RecipeType<CompressorRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.compressor");
    }

    @Override
    protected int getInputCount(CompressorRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(CompressorRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(CompressorRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, CompressorRecipe recipe, int inputXOffset) {
        addFluidSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22,
                FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, CompressorRecipe recipe, int outputXOffset) {
        addFluidSlot(builder, RecipeIngredientRole.OUTPUT, outputXOffset + 22, 22,
                FluidStack.create(recipe.getOutputFluid(), recipe.getOutputMb()));
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
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, CompressorRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Compressor-Rezepte.
    }
}
//?} else {
/*public final class CompressorJeiCategory {
    private CompressorJeiCategory() {}
}*///?}
