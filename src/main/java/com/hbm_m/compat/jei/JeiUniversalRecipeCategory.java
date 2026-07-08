package com.hbm_m.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * JEI port of {@code NEIUniversalHandler}: shared {@code gui_nei.png} background and universal slot layout.
 */
public abstract class JeiUniversalRecipeCategory<R> implements IRecipeCategory<R> {

    protected final IGuiHelper guiHelper;
    protected final IDrawable background;
    protected final IDrawable icon;
    protected final IDrawable itemSlotBackground;
    protected final ItemStack[] machines;

    protected JeiUniversalRecipeCategory(IGuiHelper guiHelper, ItemStack[] machines) {
        this.guiHelper = guiHelper;
        this.background = JeiNeiTextures.createRecipeBackground(guiHelper);
        this.itemSlotBackground = JeiNeiTextures.createItemSlotBackground(guiHelper);
        this.machines = machines;
        this.icon = guiHelper.createDrawableItemStack(machines[0]);
    }

    protected abstract int getInputCount(R recipe);

    protected abstract int getOutputCount(R recipe);

    protected abstract List<List<ItemStack>> getInputStacks(R recipe);

    protected abstract List<ItemStack> getOutputStacks(R recipe);

    protected ItemStack[] getMachines(R recipe) {
        return machines;
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return JeiNeiTextures.RECIPE_WIDTH;
    }

    @Override
    public int getHeight() {
        return JeiNeiTextures.RECIPE_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, R recipe, IFocusGroup focuses) {
        int inputCount = getInputCount(recipe);
        int outputCount = getOutputCount(recipe);
        int[][] inPos = JeiNeiLayout.getUniversalInputCoords(inputCount);
        int[][] outPos = JeiNeiLayout.getUniversalOutputCoords(outputCount);

        List<List<ItemStack>> inputs = getInputStacks(recipe);
        for (int i = 0; i < inputCount && i < inputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, inPos[i][0], inPos[i][1])
                    .setBackground(itemSlotBackground, -1, -1)
                    .addItemStacks(inputs.get(i));
        }

        List<ItemStack> outputs = getOutputStacks(recipe);
        for (int i = 0; i < outputCount && i < outputs.size(); i++) {
            if (outputs.get(i).isEmpty()) {
                continue;
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, outPos[i][0], outPos[i][1])
                    .setBackground(itemSlotBackground, -1, -1)
                    .addItemStack(outputs.get(i));
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStacks(java.util.Arrays.asList(getMachines(recipe)));
    }

    @Override
    public void draw(R recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        JeiNeiRendering.drawMachineSlot(guiGraphics, 0, false);
    }
}
