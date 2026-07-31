package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ShredderRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for shredder recipes (single item -> output).
 */
public class ShredderJeiCategory extends JeiGenericRecipeCategory<ShredderRecipe> {

    public static final RecipeType<ShredderRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "shredding", ShredderRecipe.class);

    public ShredderJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.SHREDDER.get())
        });
    }

    @Override
    public RecipeType<ShredderRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.shredder");
    }

    @Override
    protected int getInputCount(ShredderRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(ShredderRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(ShredderRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ShredderRecipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(1);
        var slot = addItemSlot(builder, RecipeIngredientRole.INPUT, positions[0][0] + inputXOffset, positions[0][1]);
        JeiIngredientSlots.addCountedIngredient(slot, recipe.getInput(), 1);
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ShredderRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.getOutput());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ShredderRecipe recipe, int machineXOffset) {
        // No blueprint slot for shredder recipes.
    }
}
