package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ExposureChamberRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Exposure Chamber recipes - Partikel + Zutat -&gt; Ausgabe (siehe
 * {@link ExposureChamberRecipe}).
 *
 * <p>Экран 1:1 с оригинальным NEI ({@code ExposureChamberHandler} -> {@code NEIUniversalHandler}):
 * ключ рецепта оригинала {@code {array[0]=ingredient(×8), array[1]=particle}} -> {@code getInputCoords(2)}
 * = {(48,24), (30,24)} — частица левее, ингредиент у машины; выход {@code getOutputCoords(1)} = (102,24);
 * блок-машина в высокой рамке (74,14, 18×36) на (75,31) — рисует базовый класс. Оригинальный
 * {@code ExposureChamberRecipes.getRecipes()} показывал полный цикл одной капсулы — ингредиент ×8
 * и выход ×8 ({@code stack.stacksize = 8} / {@code output.stackSize = 8}, ExposureChamberRecipes.java:79-84);
 * 8 = {@code maxParticles} ({@link MachineExposureChamberBlockEntity#maxParticles}), машина при этом
 * тратит 1 ингредиент и 1 выход за выстрел — как в оригинале.</p>
 */
public class ExposureChamberJeiCategory extends JeiGenericRecipeCategory<ExposureChamberRecipe> {

    public static final RecipeType<ExposureChamberRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "exposure_chamber", ExposureChamberRecipe.class);

    /** Оригинал: {@code getRecipes()} масштабировал вход/выход на 8 — выстрелов в одной капсуле. */
    private static final int DISPLAY_PER_CAPSULE = MachineExposureChamberBlockEntity.maxParticles;

    public ExposureChamberJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get())
        });
    }

    @Override
    public RecipeType<ExposureChamberRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.exposure_chamber");
    }

    @Override
    protected int getInputCount(ExposureChamberRecipe recipe) {
        return 2;
    }

    @Override
    protected int getOutputCount(ExposureChamberRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(ExposureChamberRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ExposureChamberRecipe recipe, int inputXOffset) {
        // Оригинал: ключ {ingredient, particle} -> getInputCoords(2) = {(48,24), (30,24)}
        // (NEIUniversalHandler.getInputCoords): частица (30,24), ингредиент у машины (48,24).
        int[][] inPos = JeiNeiLayout.getUniversalInputCoords(2);
        addItemSlot(builder, RecipeIngredientRole.INPUT, inPos[1][0] + inputXOffset, inPos[1][1])
                .addItemStack(recipe.getParticle());
        var slot = addItemSlot(builder, RecipeIngredientRole.INPUT, inPos[0][0] + inputXOffset, inPos[0][1]);
        // Оригинал показывал ×8 (полный цикл капсулы): ExposureChamberRecipes.getRecipes().
        JeiIngredientSlots.addCountedIngredient(slot, recipe.getIngredient(), DISPLAY_PER_CAPSULE);
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ExposureChamberRecipe recipe, int outputXOffset) {
        // Оригинал: getOutputCoords(1) = (102,24), выход ×8 (полный цикл капсулы).
        int[] outPos = JeiNeiLayout.getUniversalOutputCoords(1)[0];
        ItemStack out = recipe.getOutput();
        out.setCount(out.getCount() * DISPLAY_PER_CAPSULE);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, outPos[0] + outputXOffset, outPos[1])
                .addItemStack(out);
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ExposureChamberRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Exposure-Chamber-Rezepte.
    }
}
