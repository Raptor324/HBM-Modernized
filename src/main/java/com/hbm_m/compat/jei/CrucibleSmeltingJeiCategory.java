package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CrucibleSmeltingRecipe;

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
 * JEI-категория тигель-плавки ({@code hbm_m:crucible_smelting}).
 *
 * <p>Работает напрямую с data-driven {@link CrucibleSmeltingRecipe} (JSON) — без промежуточной
 * {@code *JeiRecipe}-обёртки. Тигель плавит предметный вход ({@link CrucibleSmeltingRecipe#getInput()})
 * в расплавленный материал — рецептов с предметным {@code output} нет, поэтому единственный показываемый
 * слот входа — input ingredient; затем изображается тигель-катализатор.</p>
 *
 * <p>Slot layout (как у legacy NEI CrucibleSmeltingHandler):</p>
 * <pre>
 *                         [o0][o1][o2]   y= 6
 *   [input]  [→]  [cruc]  [o3][o4][o5]  y=24  (input@48, crucible@75)
 *                         x=102 step=18
 * </pre>
 * Transfer rect (legacy): Rectangle(65, 23, 36, 18)
 */
public class CrucibleSmeltingJeiCategory implements IRecipeCategory<CrucibleSmeltingRecipe> {

    public static final RecipeType<CrucibleSmeltingRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "crucible_smelting", CrucibleSmeltingRecipe.class);

    static final int INPUT_X     = 48;
    static final int INPUT_Y     = 24;
    static final int CRUCIBLE_X  = 75;
    static final int CRUCIBLE_Y  = 42;

    private static final int BG_WIDTH  = 156;
    private static final int BG_HEIGHT =  60;

    private final IDrawable background;
    private final IDrawable icon;

    public CrucibleSmeltingJeiCategory(IGuiHelper guiHelper) {
        // Без текстурного фона — blank (как в оригинальной категории).
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CRUCIBLE.get()));
    }

    @Override
    public RecipeType<CrucibleSmeltingRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.hbm_m.crucible_smelting");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleSmeltingRecipe recipe, IFocusGroup focuses) {
        // Вход — ingredient (предмет, который плавится). Берётся напрямую из data-driven рецепта.
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addIngredients(recipe.getInput());

        // Тигель — катализатор.
        builder.addSlot(RecipeIngredientRole.CATALYST, CRUCIBLE_X, CRUCIBLE_Y)
                .addItemStack(new ItemStack(ModBlocks.CRUCIBLE.get()));

        // Предметных выходов нет — плавка даёт расплавленный материал (in-memory, не data-driven).
        // Оригинальный layout预留ал 3×2 outputs-сетку, но для JSON-recipe без item-выхода слотов не добавляем.
    }
}
