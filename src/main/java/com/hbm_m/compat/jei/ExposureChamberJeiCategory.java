package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ExposureChamberRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Exposure Chamber recipes - Partikel + Zutat -&gt; Ausgabe (siehe
 * {@link ExposureChamberRecipe}).
 */
//? if forge {
public class ExposureChamberJeiCategory extends JeiGenericRecipeCategory<ExposureChamberRecipe> {

    public static final RecipeType<ExposureChamberRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "exposure_chamber", ExposureChamberRecipe.class);

    public ExposureChamberJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get())
        });
    }

    @Override
    public RecipeType<ExposureChamberRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.exposure_chamber");
    }

    @Override
    protected int getInputCount(ExposureChamberRecipe recipe) {
        return 2;
    }

    @Override
    protected int getOutputCount(ExposureChamberRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(ExposureChamberRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ExposureChamberRecipe recipe, int inputXOffset) {
        addItemSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 13)
                .addItemStack(recipe.getParticle());
        var slot = addItemSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 31);
        JeiIngredientSlots.addCountedIngredient(slot, recipe.getIngredient(), 1);
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ExposureChamberRecipe recipe, int outputXOffset) {
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, outputXOffset + 22, 22)
                .addItemStack(recipe.getOutput());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ExposureChamberRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Exposure-Chamber-Rezepte.
    }
}
//?} else {
/*public final class ExposureChamberJeiCategory {
    private ExposureChamberJeiCategory() {}
}*///?}
