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

import java.util.List;

/**
 * JEI category for crucible alloying recipes.
 *
 * Slot layout (identical to legacy NEI CrucibleAlloyingHandler):
 * <pre>
 *  [i0][i1][i2]          [o0][o1][o2]    y= 6
 *  [i3][i4][i5]  [cruc]  [o3][o4][o5]   y=24  (crucible at x=75)
 *  x=12 step=18          x=102 step=18
 * </pre>
 * Transfer rect (legacy): Rectangle(65, 23, 36, 18)
 */
public class CrucibleAlloyingJeiCategory implements IRecipeCategory<CrucibleAlloyingJeiRecipe> {

    public static final RecipeType<CrucibleAlloyingJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "crucible_alloying", CrucibleAlloyingJeiRecipe.class);

    // Input grid: 3 columns, 2 rows
    static final int INPUT_X0   = 12;
    static final int INPUT_Y0   =  6;
    static final int GRID_STEP  = 18;

    // Crucible catalyst position
    static final int CRUCIBLE_X = 75;
    static final int CRUCIBLE_Y = 24;

    // Output grid: same Y offsets, shifted right
    static final int OUTPUT_X0  = 102;
    static final int OUTPUT_Y0  =  6;

    static final int MAX_STACKS = 6; // 3-wide × 2-tall grid

    private static final int BG_WIDTH  = 156;
    private static final int BG_HEIGHT =  60;

    private final IDrawable background;
    private final IDrawable icon;

    public CrucibleAlloyingJeiCategory(IGuiHelper guiHelper) {
        // Blank until gui_jei_crucible.png is supplied under textures/gui/jei/
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CRUCIBLE.get()));
    }

    @Override
    public RecipeType<CrucibleAlloyingJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.hbm_m.crucible_alloying");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleAlloyingJeiRecipe recipe, IFocusGroup focuses) {
        // Inputs — 3×2 grid
        List<ItemStack> inputs = recipe.getInputs();
        for (int i = 0; i < Math.min(inputs.size(), MAX_STACKS); i++) {
            int col = i % 3;
            int row = i / 3;
            builder.addSlot(RecipeIngredientRole.INPUT,
                            INPUT_X0 + col * GRID_STEP,
                            INPUT_Y0 + row * GRID_STEP)
                    .addItemStack(inputs.get(i));
        }

        // Crucible — catalyst
        builder.addSlot(RecipeIngredientRole.CATALYST, CRUCIBLE_X, CRUCIBLE_Y)
                .addItemStack(new ItemStack(ModBlocks.CRUCIBLE.get()));

        // Outputs — 3×2 grid
        List<ItemStack> outputs = recipe.getOutputs();
        for (int i = 0; i < Math.min(outputs.size(), MAX_STACKS); i++) {
            int col = i % 3;
            int row = i / 3;
            builder.addSlot(RecipeIngredientRole.OUTPUT,
                            OUTPUT_X0 + col * GRID_STEP,
                            OUTPUT_Y0 + row * GRID_STEP)
                    .addItemStack(outputs.get(i));
        }
    }
}
