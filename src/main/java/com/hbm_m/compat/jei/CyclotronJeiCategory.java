package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CyclotronRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-категория циклотрона ({@code hbm_m:cyclotron}) — порт 1:1
 * {@code com.hbm.handler.nei.CyclotronRecipeHandler}.
 *
 * <p>Работает напрямую с data-driven {@link CyclotronRecipe} (JSON) — без промежуточной
 * {@code *JeiRecipe}-обёртки. Target/input — {@link CyclotronRecipe#getTarget()} / {@link CyclotronRecipe#getInput()},
 * выход — {@link CyclotronRecipe#getOutput()}.</p>
 *
 * <p>Раскладка слотов повторяет NEI-класс {@code SmeltingSet}: таргет-слот (частица)
 * {@code input1 = (66 - 45, 6 + 18) = (21, 24)}, реактив {@code input2 = (66 + 9, 42 - 18) = (75, 24)},
 * выход {@code result = (129, 24)}. Фон — как в {@code TemplateRecipeHandler.drawBackground}:
 * текстура {@code gui_nei_cyclotron.png} с оффсетом {@code (5, 11)} размером {@code 166x65}
 * (прорези слотов уже нарисованы в текстуре). Прогресс — как в {@code drawExtras}:
 * {@code drawProgressBar(83 - 3 + 16 - 52, 5 + 18 + 1, 100, 118 + 1, 24, 16, 48, 0)} —
 * стрелка {@code 24x16} с оффсетом текстуры {@code (100, 119)} в позиции {@code (44, 24)},
 * 48 тиков, слева направо.</p>
 */
public class CyclotronJeiCategory implements IRecipeCategory<CyclotronRecipe> {

    public static final RecipeType<CyclotronRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "cyclotron", CyclotronRecipe.class);

    private static final ResourceLocation TEXTURE =
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_cyclotron.png");


    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public CyclotronJeiCategory(IGuiHelper guiHelper) {
        // TemplateRecipeHandler.drawBackground: drawTexturedModalRect(0, 0, 5, 11, 166, 65)
        this.background = guiHelper.drawableBuilder(TEXTURE, 5, 11, 166, 65).setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.CYCLOTRON.get()));
        // drawExtras: drawProgressBar(83 - 3 + 16 - 52, 5 + 18 + 1, 100, 118 + 1, 24, 16, 48, 0)
        this.arrow = guiHelper.drawableBuilder(TEXTURE, 100, 119, 24, 16).setTextureSize(256, 256)
                .buildAnimated(48, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<CyclotronRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        // Ориг. getRecipeName() возвращает просто «Cyclotron» — без «(WIP)» из block.hbm_m.cyclotron.
        return Component.translatable("jei.hbm_m.cyclotron");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CyclotronRecipe recipe, IFocusGroup focuses) {
        // Таргет-слот (частица) — ориг. input1 (66 - 45, 6 + 18) = (21, 24).
        builder.addSlot(RecipeIngredientRole.INPUT, 21, 24)
                .addIngredients(recipe.getTarget());

        // Реактив-слот (чем бомбардируем) — ориг. input2 (66 + 9, 42 - 18) = (75, 24).
        builder.addSlot(RecipeIngredientRole.INPUT, 75, 24)
                .addIngredients(recipe.getInput());

        // Выход — ориг. result (129, 24). Пустой выход не показываем (фильтр по источнику уже
        // отсеивает рецепты без result, но на всякий случай — guard).
        ItemStack output = recipe.getOutput();
        if (!output.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 129, 24)
                    .addItemStack(output);
        }
    }

    @Override
    public void draw(CyclotronRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 44, 24);
    }
}
