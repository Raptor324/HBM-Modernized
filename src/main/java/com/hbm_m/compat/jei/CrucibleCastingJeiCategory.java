package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for crucible mold-casting recipes.
 *
 * Slot layout (relative to background, identical to legacy NEI CrucibleCastingHandler):
 * <pre>
 *           [mold]              y= 6
 *   [input]         [output]   y=24
 *           [basin]             y=42
 *   x=48    x=75    x=102
 * </pre>
 */
public class CrucibleCastingJeiCategory implements IRecipeCategory<CrucibleCastingJeiRecipe> {

    public static final RecipeType<CrucibleCastingJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "crucible_casting", CrucibleCastingJeiRecipe.class);

    // Slot positions — kept identical to the legacy NEI handler for easy future texture alignment
    static final int SLOT_INPUT_X  = 48;
    static final int SLOT_INPUT_Y  = 24;
    static final int SLOT_MOLD_X   = 75;
    static final int SLOT_MOLD_Y   =  6;
    static final int SLOT_BASIN_X  = 75;
    static final int SLOT_BASIN_Y  = 42;
    static final int SLOT_OUTPUT_X = 102;
    static final int SLOT_OUTPUT_Y = 24;

    private static final int BG_WIDTH  = 140;
    private static final int BG_HEIGHT =  68;

    private final IDrawable background;
    private final IDrawable icon;

    public CrucibleCastingJeiCategory(IGuiHelper guiHelper) {
        // Blank until gui_jei_foundry.png is supplied under textures/gui/jei/
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.FOUNDRY_BASIN.get()));
    }

    @Override
    public RecipeType<CrucibleCastingJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.hbm_m.crucible_casting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleCastingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_INPUT_X, SLOT_INPUT_Y)
                .addItemStack(recipe.getInput());

        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_MOLD_X, SLOT_MOLD_Y)
                .addItemStack(recipe.getMold());

        // Foundry basin is always present as a machine catalyst
        builder.addSlot(RecipeIngredientRole.CATALYST, SLOT_BASIN_X, SLOT_BASIN_Y)
                .addItemStack(new ItemStack(ModBlocks.FOUNDRY_BASIN.get()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUT_Y)
                .addItemStack(recipe.getOutput());
    }
}
