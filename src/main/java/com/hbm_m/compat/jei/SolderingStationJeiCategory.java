package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.SolderingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI-категория паяльной станции ({@code hbm_m:soldering_station}) — порт 1:1
 * {@code SolderingStationHandler} (1.7.10 NEI, extends {@code NEIUniversalHandler}).
 *
 * <p>Работает напрямую с data-driven {@link SolderingRecipe} (JSON). Как и в оригинале
 * ({@code SolderingRecipes.getRecipes()}), входы — это одна плоская группа: toppings, затем pcb,
 * затем solder, затем иконка жидкости (если задана) — все позиции раскладываются универсальной
 * раскладкой NEI ({@link JeiNeiLayout#getUniversalInputCoords}), выход справа, машина —
 * в высоком слоте (75,31). Тексты {@code drawExtras} (длительность/потребление, выравнивание
 * по x=160, y=43/55, цвет 0x404040) воспроизведены в {@link #draw}.</p>
 */
public class SolderingStationJeiCategory extends JeiUniversalRecipeCategory<SolderingRecipe> {

    public static final RecipeType<SolderingRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "soldering_station", SolderingRecipe.class);

    public SolderingStationJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{new ItemStack(ModBlocks.SOLDERING_STATION.get())});
    }

    @Override
    public RecipeType<SolderingRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.soldering_station");
    }

    /**
     * Число входных слотов оригинала: toppings + pcb + solder (+ иконка жидкости последним
     * элементом, см. {@code SolderingRecipes.getRecipes()}).
     */
    @Override
    protected int getInputCount(SolderingRecipe recipe) {
        return recipe.getToppings().length + recipe.getPcb().length + recipe.getSolder().length
                + (recipe.getFluid() != null ? 1 : 0);
    }

    @Override
    protected int getOutputCount(SolderingRecipe recipe) {
        return recipe.getOutput().isEmpty() ? 0 : 1;
    }

    /**
     * Одна группа = один NEI {@code PositionedStack}: варианты для цика и размер стека = требуемое
     * количество ({@code ComparableStack}/{@code OreDictStack} с count в оригинале).
     */
    @Override
    protected List<List<ItemStack>> getInputStacks(SolderingRecipe recipe) {
        List<List<ItemStack>> groups = new ArrayList<>();
        addGroup(groups, recipe.getToppings(), recipe.getToppingCounts());
        addGroup(groups, recipe.getPcb(), recipe.getPcbCounts());
        addGroup(groups, recipe.getSolder(), recipe.getSolderCounts());
        return groups;
    }

    private static void addGroup(List<List<ItemStack>> groups, Ingredient[] ingredients, int[] counts) {
        for (int i = 0; i < ingredients.length; i++) {
            ItemStack[] items = ingredients[i].getItems();
            List<ItemStack> stacks = new ArrayList<>(items.length);
            for (ItemStack item : items) {
                ItemStack copy = item.copy();
                copy.setCount(counts[i]);
                stacks.add(copy);
            }
            groups.add(stacks);
        }
    }

    @Override
    protected List<ItemStack> getOutputStacks(SolderingRecipe recipe) {
        return List.of(recipe.getOutput());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SolderingRecipe recipe, IFocusGroup focuses) {
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
    public void draw(SolderingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // Рамка машинного слота: drawTexturedModalRect(74, 14, 59, 87, 18, 36).
        super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);

        // Оригинальный drawExtras: "%,d ticks" / "%,d HE/t", выравнивание по правому краю x=160,
        // y=43/55, цвет 0x404040 (Locale.US, полный формат числа — не getShortNumber, как у ArcWelder).
        Font font = Minecraft.getInstance().font;
        String duration = String.format(Locale.US, "%,d", recipe.getDuration()) + " ticks";
        String consumption = String.format(Locale.US, "%,d", recipe.getConsumption()) + " HE/t";
        int side = 160;
        int color = 0x404040;
        guiGraphics.drawString(font, duration, side - font.width(duration), 43, color, false);
        guiGraphics.drawString(font, consumption, side - font.width(consumption), 55, color, false);
    }
}
