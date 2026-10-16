package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ElectrolyserMetalRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Electrolyser metal-mode recipes - 1 Kristall-Eingang -> 2 Metall-Ausgaenge +
 * Byprodukt-Items (siehe {@link ElectrolyserMetalRecipe}).
 *
 * <p>Data-driven: рецепты читаются напрямую aus {@code RecipeManager} (JSON {@code hbm_m:electrolyser_metal}),
 * ранее — статический {@code ElectrolyserRecipes} (metal-mode).</p>
 */
//? if forge {
public class ElectrolyserMetalJeiCategory extends JeiGenericRecipeCategory<ElectrolyserMetalRecipe> {

    public static final RecipeType<ElectrolyserMetalRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "electrolyser_metal", ElectrolyserMetalRecipe.class);

    public ElectrolyserMetalJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ELECTROLYSER.get())
        });
    }

    @Override
    public RecipeType<ElectrolyserMetalRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.electrolyser");
    }

    @Override
    protected int getInputCount(ElectrolyserMetalRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(ElectrolyserMetalRecipe recipe) {
        int count = 0;
        if (!recipe.getOutputA().isEmpty()) count++;
        if (!recipe.getOutputB().isEmpty()) count++;
        count += recipe.getByproducts().length;
        return count;
    }

    @Override
    protected boolean hasBlueprintTemplate(ElectrolyserMetalRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ElectrolyserMetalRecipe recipe, int inputXOffset) {
        // Ingredient-вход (кристалл): показываем первый стек ингредиента как представитель.
        ItemStack[] matching = recipe.getInput().getItems();
        ItemStack display = matching != null && matching.length > 0 ? matching[0] : ItemStack.EMPTY;
        addItemSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22)
                .addItemStack(display);
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ElectrolyserMetalRecipe recipe, int outputXOffset) {
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        if (!recipe.getOutputA().isEmpty()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(recipe.getOutputA());
            slotIndex++;
        }
        if (!recipe.getOutputB().isEmpty()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(recipe.getOutputB());
            slotIndex++;
        }
        for (ItemStack byproduct : recipe.getByproducts()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(byproduct);
            slotIndex++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ElectrolyserMetalRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Electrolyser-Metal-Rezepte.
    }
}
//?} else {
/*public final class ElectrolyserMetalJeiCategory {
    private ElectrolyserMetalJeiCategory() {}
}*///?}
