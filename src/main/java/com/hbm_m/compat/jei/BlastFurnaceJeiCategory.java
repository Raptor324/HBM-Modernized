package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.BlastFurnaceRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI category for blast furnace recipes (two items -> output).
 */
public class BlastFurnaceJeiCategory extends JeiGenericRecipeCategory<BlastFurnaceRecipe> {

    public static final RecipeType<BlastFurnaceRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "blast_furnace", BlastFurnaceRecipe.class);

    public BlastFurnaceJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.BLAST_FURNACE.get())
        });
    }

    @Override
    public RecipeType<BlastFurnaceRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.blast_furnace");
    }

    @Override
    protected int getInputCount(BlastFurnaceRecipe recipe) {
        return recipe.getIngredients().size();
    }

    @Override
    protected int getOutputCount(BlastFurnaceRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(BlastFurnaceRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, BlastFurnaceRecipe recipe, int inputXOffset) {
        var ingredients = recipe.getIngredients();
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(ingredients.size());

        for (int i = 0; i < ingredients.size() && i < positions.length; i++) {
            Ingredient ingredient = ingredients.get(i);
            var slot = addItemSlot(builder, RecipeIngredientRole.INPUT, positions[i][0] + inputXOffset, positions[i][1]);
            JeiIngredientSlots.addCountedIngredient(slot, ingredient, 1);
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, BlastFurnaceRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.getResultItem(null));
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, BlastFurnaceRecipe recipe, int machineXOffset) {
        // No blueprint slot for blast furnace recipes.
    }
}
