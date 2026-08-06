package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CompressorRecipes;
import com.hbm_m.recipe.CompressorRecipes.Key;
import com.hbm_m.recipe.CompressorRecipes.Recipe;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * JEI category for Compressor special-case recipes (siehe {@link CompressorRecipes} - der
 * generische "immer +1 Druck"-Fallback ist selbsterklaerend und wird nicht als JEI-Rezept gezeigt).
 */
//? if forge {
public class CompressorJeiCategory extends JeiGenericRecipeCategory<CompressorJeiCategory.JeiRecipe> {

    public record JeiRecipe(Fluid inFluid, int inputAmount, Fluid outFluid, int outAmount) {}

    public static final RecipeType<JeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "compressor", JeiRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 16_000;

    public CompressorJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.COMPRESSOR.get())
        });
    }

    public static List<JeiRecipe> fromRecipes() {
        List<JeiRecipe> result = new ArrayList<>();
        for (Map.Entry<Key, Recipe> entry : CompressorRecipes.getAll().entrySet()) {
            result.add(new JeiRecipe(entry.getKey().fluid(), entry.getValue().inputAmount(),
                    entry.getValue().outFluid(), entry.getValue().outAmount()));
        }
        return result;
    }

    @Override
    public RecipeType<JeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.compressor");
    }

    @Override
    protected int getInputCount(JeiRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(JeiRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(JeiRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, JeiRecipe recipe, int inputXOffset) {
        addFluidSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22,
                FluidStack.create(recipe.inFluid(), recipe.inputAmount()));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, JeiRecipe recipe, int outputXOffset) {
        addFluidSlot(builder, RecipeIngredientRole.OUTPUT, outputXOffset + 22, 22,
                FluidStack.create(recipe.outFluid(), recipe.outAmount()));
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid) {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, JeiRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Compressor-Rezepte.
    }
}
//?} else {
/*public final class CompressorJeiCategory {
    private CompressorJeiCategory() {}
}*///?}
