package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ArcWelderRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI-категория дуговой сварки ({@code hbm_m:arc_welder}) — порт 1:1
 * {@code ArcWelderHandler} (1.7.10 NEI, extends {@code NEIUniversalHandler}).
 *
 * <p>Работает напрямую с data-driven {@link ArcWelderRecipe} (JSON). Раскладка — универсальная
 * NEI ({@link JeiNeiLayout#getUniversalInputCoords}/{@link JeiNeiLayout#getUniversalOutputCoords}),
 * как в оригинале: входы слева (первый ингредиент в правом слоте, дальше влево), жидкостное
 * требование — иконкой последним элементом группы (ориг. {@code ItemFluidIcon} в
 * {@code ArcWelderRecipes.getRecipes()}), выход справа, машина — в высоком слоте (75,31).
 * Тексты {@code drawExtras} ({@code getShortNumber} + " ticks" / + "HE/t" без пробела,
 * выравнивание по x=164, y=45/57, цвет 0x404040) воспроизведены в {@link #draw}.</p>
 */
public class ArcWelderJeiCategory extends JeiUniversalRecipeCategory<ArcWelderRecipe> {

    public static final RecipeType<ArcWelderRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "arc_welder", ArcWelderRecipe.class);

    public ArcWelderJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{new ItemStack(ModBlocks.ARC_WELDER.get())});
    }

    @Override
    public RecipeType<ArcWelderRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.arc_welder");
    }

    /**
     * Число входных полей оригинальной группы: ингредиенты + иконка жидкости последним
     * (см. {@code ArcWelderRecipes.getRecipes()}: {@code array[size - 1] = ItemFluidIcon.make(...)}).
     */
    @Override
    protected int getInputCount(ArcWelderRecipe recipe) {
        return recipe.getInputs().length + (recipe.getFluid() != null ? 1 : 0);
    }

    @Override
    protected int getOutputCount(ArcWelderRecipe recipe) {
        return recipe.getOutput().isEmpty() ? 0 : 1;
    }

    /**
     * Одна группа = один NEI {@code PositionedStack}: варианты для цикла и размер стека =
     * требуемое количество (в оригинале {@code OreDictStack}/{@code ComparableStack} с count).
     */
    @Override
    protected List<List<ItemStack>> getInputStacks(ArcWelderRecipe recipe) {
        Ingredient[] inputs = recipe.getInputs();
        int[] counts = recipe.getCounts();
        List<List<ItemStack>> groups = new ArrayList<>(inputs.length);
        for (int i = 0; i < inputs.length; i++) {
            ItemStack[] items = inputs[i].getItems();
            List<ItemStack> stacks = new ArrayList<>(items.length);
            for (ItemStack item : items) {
                ItemStack copy = item.copy();
                copy.setCount(counts[i]);
                stacks.add(copy);
            }
            groups.add(stacks);
        }
        return groups;
    }

    @Override
    protected List<ItemStack> getOutputStacks(ArcWelderRecipe recipe) {
        return List.of(recipe.getOutput());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArcWelderRecipe recipe, IFocusGroup focuses) {
        // Базовая раскладка NEIUniversalHandler: входы слева по getInputCoords, выход справа,
        // машина в высоком слоте (75,31).
        super.setRecipe(builder, recipe, focuses);

        // Жидкость — последний вход оригинальной группы (иконка ItemFluidIcon в NEI).
        dev.architectury.fluid.FluidStack fluid = recipe.getFluid();
        if (fluid != null) {
            int count = getInputCount(recipe);
            int[] pos = JeiNeiLayout.getUniversalInputCoords(count)[count - 1];
            builder.addSlot(RecipeIngredientRole.INPUT, pos[0], pos[1])
                    .setBackground(itemSlotBackground, -1, -1)
                    .setCustomRenderer(JeiFluidTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addFluidStack(fluid.getFluid(), Math.max(fluid.getAmount(), 1));
        }
    }

    @Override
    public void draw(ArcWelderRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // Рамка машинного слота: drawTexturedModalRect(74, 14, 59, 87, 18, 36).
        super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);

        // Оригинальный drawExtras: BobMathUtil.getShortNumber(duration) + " ticks" и
        // getShortNumber(consumption) + "HE/t" (без пробела), правый край x=164, y=45/57, 0x404040.
        JeiNeiRendering.drawGenericRecipeExtras(guiGraphics, recipe.getDuration(), recipe.getConsumption());
    }
}
