package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AnvilRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-Port von {@code SmithingRecipeHandler}: {@code gui_nei_smithing.png}, links/rechts/Ergebnis bei
 * (39|24), (75|24), (111|24) und "Tier n" darunter.
 */
public class AnvilSmithingJeiCategory implements IRecipeCategory<AnvilRecipe> {

    public static final RecipeType<AnvilRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "anvil_smithing", AnvilRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_smithing.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AnvilSmithingJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ANVIL_IRON.get()));
    }

    @Override
    public RecipeType<AnvilRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Anvil");
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
        return 166;
    }

    @Override
    public int getHeight() {
        return 65;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilRecipe recipe, IFocusGroup focuses) {
        var left = recipe.getLeftDisplay();
        var right = recipe.getRightDisplay();
        builder.addSlot(RecipeIngredientRole.INPUT, 39, 24).addItemStacks(left);
        builder.addSlot(RecipeIngredientRole.INPUT, 75, 24).addItemStacks(right);
        ItemStack l = left.isEmpty() ? ItemStack.EMPTY : left.get(0);
        ItemStack r = right.isEmpty() ? ItemStack.EMPTY : right.get(0);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 111, 24).addItemStack(recipe.getSmithingOutput(l, r));
    }

    @Override
    public void draw(AnvilRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics g, double mouseX, double mouseY) {
        g.drawString(Minecraft.getInstance().font, "Tier " + recipe.getRequiredTier().getLegacyId(), 52, 43, 0x404040, false);
    }
}
