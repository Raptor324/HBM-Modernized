package com.hbm_m.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * JEI port of {@code NEIGenericRecipeHandler}: shared {@code gui_nei.png} background and slot layout.
 */
public abstract class JeiGenericRecipeCategory<R> implements IRecipeCategory<R> {

    protected final IGuiHelper guiHelper;
    protected final IDrawable background;
    protected final IDrawable icon;
    protected final IDrawable itemSlotBackground;
    protected final ItemStack[] machines;

    protected JeiGenericRecipeCategory(IGuiHelper guiHelper, ItemStack[] machines) {
        this.guiHelper = guiHelper;
        this.background = JeiNeiTextures.createRecipeBackground(guiHelper);
        this.itemSlotBackground = JeiNeiTextures.createItemSlotBackground(guiHelper);
        this.machines = machines;
        this.icon = guiHelper.createDrawableItemStack(machines[0]);
    }

    protected abstract int getInputCount(R recipe);

    protected abstract int getOutputCount(R recipe);

    protected abstract boolean hasBlueprintTemplate(R recipe);

    protected abstract void addInputSlots(IRecipeLayoutBuilder builder, R recipe, int inputXOffset);

    protected abstract void addOutputSlots(IRecipeLayoutBuilder builder, R recipe, int outputXOffset);

    protected abstract void addBlueprintSlot(IRecipeLayoutBuilder builder, R recipe, int machineXOffset);

    protected int getInputXOffset(R recipe, int inputCount) {
        return 0;
    }

    protected int getOutputXOffset(R recipe, int outputCount) {
        return 0;
    }

    protected int getMachineXOffset(R recipe) {
        return 0;
    }

    protected ItemStack[] getMachines(R recipe) {
        return machines;
    }

    protected void drawRecipeExtras(R recipe, GuiGraphics graphics) {
    }

    /** NEI draws 18x18 item slot frames at (x - 1, y - 1). */
    protected IRecipeSlotBuilder addItemSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y)
                .setBackground(itemSlotBackground, -1, -1);
    }

    /** Machine / blueprint icons sit inside the tall NEI machine frame, not in item slot frames. */
    protected IRecipeSlotBuilder addUnframedSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y);
    }

    @Override
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
        int inputXOffset = getInputXOffset(recipe, inputCount);
        int outputXOffset = getOutputXOffset(recipe, outputCount);
        int machineXOffset = getMachineXOffset(recipe);
        boolean hasTemplate = hasBlueprintTemplate(recipe);

        addInputSlots(builder, recipe, inputXOffset);
        addOutputSlots(builder, recipe, outputXOffset);
        addBlueprintSlot(builder, recipe, machineXOffset);

        int machineY = hasTemplate ? 38 : 31;
        addUnframedSlot(builder, RecipeIngredientRole.CATALYST, 75 + machineXOffset, machineY)
                .addItemStacks(java.util.Arrays.asList(getMachines(recipe)));
    }

    @Override
    public void draw(R recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        JeiNeiRendering.drawMachineSlot(guiGraphics, getMachineXOffset(recipe), hasBlueprintTemplate(recipe));
        drawRecipeExtras(recipe, guiGraphics);
    }
}
