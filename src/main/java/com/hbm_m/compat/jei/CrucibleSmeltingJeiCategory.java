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
 * JEI category for crucible smelting recipes.
 *
 * Slot layout (identical to legacy NEI CrucibleSmeltingHandler):
 * <pre>
 *                         [o0][o1][o2]   y= 6
 *   [input]  [→]  [cruc]  [o3][o4][o5]  y=24  (input@48, crucible@75)
 *                         x=102 step=18
 * </pre>
 * Transfer rect (legacy): Rectangle(65, 23, 36, 18)
 */
public class CrucibleSmeltingJeiCategory implements IRecipeCategory<CrucibleSmeltingJeiRecipe> {

    public static final RecipeType<CrucibleSmeltingJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "crucible_smelting", CrucibleSmeltingJeiRecipe.class);

    static final int INPUT_X     = 48;
    static final int INPUT_Y     = 24;
    static final int CRUCIBLE_X  = 75;
    static final int CRUCIBLE_Y  = 42;
    static final int OUTPUT_X0   = 102;
    static final int OUTPUT_Y0   =   6;
    static final int GRID_STEP   =  18;
    static final int MAX_OUTPUTS =   6; // 3-wide × 2-tall

    private static final int BG_WIDTH  = 156;
    private static final int BG_HEIGHT =  60;

    private final IDrawable background;
    private final IDrawable icon;

    public CrucibleSmeltingJeiCategory(IGuiHelper guiHelper) {
        // Blank until gui_jei_crucible_smelting.png is supplied under textures/gui/jei/
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CRUCIBLE.get()));
    }

    @Override
    public RecipeType<CrucibleSmeltingJeiRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.hbm_m.crucible_smelting");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleSmeltingJeiRecipe recipe, IFocusGroup focuses) {
        // Single input
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addItemStack(recipe.getInput());

        // Crucible catalyst
        builder.addSlot(RecipeIngredientRole.CATALYST, CRUCIBLE_X, CRUCIBLE_Y)
                .addItemStack(new ItemStack(ModBlocks.CRUCIBLE.get()));

        // Outputs — 3×2 grid
        List<ItemStack> outputs = recipe.getOutputs();
        for (int i = 0; i < Math.min(outputs.size(), MAX_OUTPUTS); i++) {
            int col = i % 3;
            int row = i / 3;
            builder.addSlot(RecipeIngredientRole.OUTPUT,
                            OUTPUT_X0 + col * GRID_STEP,
                            OUTPUT_Y0 + row * GRID_STEP)
                    .addItemStack(outputs.get(i));
        }
    }
}
