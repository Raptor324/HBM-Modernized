package com.hbm_m.compat.jei;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PressRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI port of {@code PressRecipeHandler}.
 */
public class PressJeiCategory implements IRecipeCategory<PressRecipe> {

    public static final RecipeType<PressRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "pressing", PressRecipe.class);

    /** Stamp slot — {@code PositionedStack(stamp, 83 - 35, 6)} */
    private static final int STAMP_X = 48;
    private static final int STAMP_Y = 6;

    /** Material slot — {@code PositionedStack(input, 83 - 35, 5 + 36 + 1)} */
    private static final int MATERIAL_X = 48;
    private static final int MATERIAL_Y = 42;

    /** Output slot — {@code PositionedStack(result, 83 + 28, 5 + 18 + 1)} */
    private static final int OUTPUT_X = 111;
    private static final int OUTPUT_Y = 24;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlotBackground;

    public PressJeiCategory(IGuiHelper guiHelper) {
        this.background = JeiPressTextures.createRecipeBackground(guiHelper);
        this.itemSlotBackground = JeiPressTextures.createItemSlotBackground(guiHelper);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.PRESS.get()));
    }

    @Override
    public RecipeType<PressRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.press");
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
        return JeiPressTextures.RECIPE_WIDTH;
    }

    @Override
    public int getHeight() {
        return JeiPressTextures.RECIPE_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PressRecipe recipe, IFocusGroup focuses) {
        Ingredient stamp = recipe.getIngredients().get(0);
        Ingredient material = recipe.getIngredients().get(1);

        addItemSlot(builder, RecipeIngredientRole.INPUT, STAMP_X, STAMP_Y)
                .addIngredients(stamp);
        addItemSlot(builder, RecipeIngredientRole.INPUT, MATERIAL_X, MATERIAL_Y)
                .addIngredients(material);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.getResultItemSafe());
    }

    @Override
    public void draw(PressRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        JeiPressRendering.drawProgressBar(guiGraphics);
    }

    private IRecipeSlotBuilder addItemSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y).setBackground(itemSlotBackground, -1, -1);
    }
}
