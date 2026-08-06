package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ExposureChamberRecipes;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI category for Exposure Chamber recipes - Partikel + Zutat -&gt; Ausgabe (siehe
 * {@link ExposureChamberRecipes}).
 */
//? if forge {
public class ExposureChamberJeiCategory extends JeiGenericRecipeCategory<ExposureChamberRecipes.Recipe> {

    public static final RecipeType<ExposureChamberRecipes.Recipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "exposure_chamber", ExposureChamberRecipes.Recipe.class);

    public ExposureChamberJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get())
        });
    }

    @Override
    public RecipeType<ExposureChamberRecipes.Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.exposure_chamber");
    }

    @Override
    protected int getInputCount(ExposureChamberRecipes.Recipe recipe) {
        return 2;
    }

    @Override
    protected int getOutputCount(ExposureChamberRecipes.Recipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(ExposureChamberRecipes.Recipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ExposureChamberRecipes.Recipe recipe, int inputXOffset) {
        addItemSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 13)
                .addItemStack(new ItemStack(recipe.particle()));
        var slot = addItemSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 31);
        JeiIngredientSlots.addCountedIngredient(slot, recipe.ingredient(), 1);
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ExposureChamberRecipes.Recipe recipe, int outputXOffset) {
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, outputXOffset + 22, 22)
                .addItemStack(recipe.output());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ExposureChamberRecipes.Recipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Exposure-Chamber-Rezepte.
    }
}
//?} else {
/*public final class ExposureChamberJeiCategory {
    private ExposureChamberJeiCategory() {}
}*///?}
