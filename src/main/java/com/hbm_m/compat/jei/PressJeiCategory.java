package com.hbm_m.compat.jei;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PressRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI category for press recipes (stamp + material -> output).
 */
public class PressJeiCategory extends JeiGenericRecipeCategory<PressRecipe> {

    public static final RecipeType<PressRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "press", PressRecipe.class);

    public PressJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModItems.PRESS.get())
        });
    }

    @Override
    public RecipeType<PressRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.press");
    }

    @Override
    protected int getInputCount(PressRecipe recipe) {
        return recipe.getIngredients().size();
    }

    @Override
    protected int getOutputCount(PressRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(PressRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, PressRecipe recipe, int inputXOffset) {
        var ingredients = recipe.getIngredients();
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(ingredients.size());

        for (int i = 0; i < ingredients.size() && i < positions.length; i++) {
            Ingredient ingredient = ingredients.get(i);
            var slot = addItemSlot(builder, RecipeIngredientRole.INPUT, positions[i][0] + inputXOffset, positions[i][1]);
            JeiIngredientSlots.addCountedIngredient(slot, ingredient, 1);
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, PressRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.getResultItem(null));
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, PressRecipe recipe, int machineXOffset) {
        // No blueprint slot for press recipes.
    }
}
