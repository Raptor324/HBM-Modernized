package com.hbm_m.compat.jei;

//? if forge || neoforge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.MoltenAlloyRecipe;

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

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
/**
 * Сплавление в тигле: расплав на входе, расплав на выходе. Предметов в рецепте нет вообще,
 * поэтому вместо слотов рисуются цветные свотчи материалов с подписями.
 */
public class MoltenAlloyJeiCategory implements IRecipeCategory<MoltenAlloyRecipe> {

    public static final RecipeType<MoltenAlloyRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "molten_alloy", MoltenAlloyRecipe.class);

    private static final int BG_WIDTH = 156;
    private static final int BG_HEIGHT = 62;
    private static final int ROW_HEIGHT = 20;
    private static final int LEFT_X = 4;
    private static final int RIGHT_X = 86;

    private final IDrawable background;
    private final IDrawable icon;

    public MoltenAlloyJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CRUCIBLE.get()));
    }

    @Override public RecipeType<MoltenAlloyRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override public Component getTitle() { return Component.translatable("block.hbm_m.crucible"); }

    @Override @SuppressWarnings("removal") public IDrawable getBackground() { return background; }

    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MoltenAlloyRecipe recipe, IFocusGroup focuses) {
        // Единственный предмет на экране — сам тигель.
        builder.addSlot(RecipeIngredientRole.CATALYST, 70, BG_HEIGHT - 20)
                .addItemStack(new ItemStack(ModBlocks.CRUCIBLE.get()));
    }

    @Override
    public void draw(MoltenAlloyRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        drawColumn(graphics, LEFT_X, recipe.getInputs());
        drawColumn(graphics, RIGHT_X, recipe.getOutputs());
    }

    private static void drawColumn(GuiGraphics graphics, int x, MaterialStack[] stacks) {
        int y = 2;
        for (MaterialStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            JeiMoltenRendering.drawSwatch(graphics, x + 1, y + 1, stack);
            JeiMoltenRendering.drawLabel(graphics, x + 20, y + 5, stack);
            y += ROW_HEIGHT;
        }
    }
}
//?}
