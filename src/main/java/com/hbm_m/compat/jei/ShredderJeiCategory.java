package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ShredderRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/**
 * JEI port of {@code ShredderRecipeHandler}: dedicated {@code gui_nei_shredder.png} background
 * with input left (39, 24), output right (129, 24) and the cycling blade pair in the middle.
 */
@OnlyIn(Dist.CLIENT)
public class ShredderJeiCategory implements IRecipeCategory<ShredderRecipe> {

    public static final RecipeType<ShredderRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "shredding", ShredderRecipe.class);

    /** Input slot — {@code PositionedStack(input, 83 - 27 - 18 + 1, 5 + 18 + 1)} */
    private static final int INPUT_X = 39;
    private static final int INPUT_Y = 24;

    /** Output slot — {@code PositionedStack(result, 83 + 27 + 18 + 1, 5 + 18 + 1)} */
    private static final int OUTPUT_X = 129;
    private static final int OUTPUT_Y = 24;

    /** Blade slots — {@code PositionedStack(ingred, 83 + 1, 5 + 1)} / {@code (83 + 1, 5 + 36 + 1)} */
    private static final int BLADE_TOP_X = 84;
    private static final int BLADE_TOP_Y = 6;
    private static final int BLADE_BOTTOM_X = 84;
    private static final int BLADE_BOTTOM_Y = 42;

    private final IDrawable background;
    private final IDrawable icon;

    public ShredderJeiCategory(IGuiHelper guiHelper) {
        this.background = JeiShredderTextures.createRecipeBackground(guiHelper);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.SHREDDER.get()));
    }

    @Override
    public RecipeType<ShredderRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.shredder");
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
        return JeiShredderTextures.RECIPE_WIDTH;
    }

    @Override
    public int getHeight() {
        return JeiShredderTextures.RECIPE_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ShredderRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addIngredients(recipe.getInput());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.getOutput());

        // NEI getOtherStacks: the blade pair, cycling steel -> titanium -> desh
        List<ItemStack> blades = getBlades();
        builder.addSlot(RecipeIngredientRole.CATALYST, BLADE_TOP_X, BLADE_TOP_Y)
                .addItemStacks(blades);
        builder.addSlot(RecipeIngredientRole.CATALYST, BLADE_BOTTOM_X, BLADE_BOTTOM_Y)
                .addItemStacks(blades);
    }

    @Override
    public void draw(ShredderRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        JeiShredderRendering.drawPowerBar(guiGraphics);
        JeiShredderRendering.drawProgressArrow(guiGraphics);
    }

    /** Port of {@code ShredderRecipeHandler.getBlades()}. */
    public static List<ItemStack> getBlades() {
        List<ItemStack> blades = new ArrayList<ItemStack>();
        blades.add(new ItemStack(ModItems.BLADES_STEEL.get()));
        blades.add(new ItemStack(ModItems.BLADES_TITANIUM.get()));
        blades.add(new ItemStack(ModItems.BLADES_DESH.get()));
        return blades;
    }
}
