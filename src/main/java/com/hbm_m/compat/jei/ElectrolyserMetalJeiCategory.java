package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ElectrolyserRecipes;
import com.hbm_m.recipe.ElectrolyserRecipes.MetalRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Electrolyser metal-mode recipes - 1 Kristall-Eingang -&gt; 2 Metall-Ausgaenge +
 * Byprodukt-Items (siehe {@link ElectrolyserRecipes}).
 */
//? if forge {
public class ElectrolyserMetalJeiCategory extends JeiGenericRecipeCategory<Map.Entry<Item, MetalRecipe>> {

    public static final RecipeType<Map.Entry<Item, MetalRecipe>> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "electrolyser_metal", castEntry());

    public ElectrolyserMetalJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ELECTROLYSER.get())
        });
    }

    @SuppressWarnings("unchecked")
    private static Class<Map.Entry<Item, MetalRecipe>> castEntry() {
        return (Class<Map.Entry<Item, MetalRecipe>>) (Class<?>) Map.Entry.class;
    }

    public static List<Map.Entry<Item, MetalRecipe>> fromRecipes() {
        return new ArrayList<>(ElectrolyserRecipes.getAllMetalRecipes().entrySet());
    }

    @Override
    public RecipeType<Map.Entry<Item, MetalRecipe>> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.electrolyser");
    }

    @Override
    protected int getInputCount(Map.Entry<Item, MetalRecipe> recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(Map.Entry<Item, MetalRecipe> recipe) {
        MetalRecipe r = recipe.getValue();
        int count = 0;
        if (!r.outA().isEmpty()) count++;
        if (!r.outB().isEmpty()) count++;
        count += r.byproducts().length;
        return count;
    }

    @Override
    protected boolean hasBlueprintTemplate(Map.Entry<Item, MetalRecipe> recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, Map.Entry<Item, MetalRecipe> recipe, int inputXOffset) {
        addItemSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22)
                .addItemStack(new ItemStack(recipe.getKey()));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, Map.Entry<Item, MetalRecipe> recipe, int outputXOffset) {
        MetalRecipe r = recipe.getValue();
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        if (!r.outA().isEmpty()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(r.outA());
            slotIndex++;
        }
        if (!r.outB().isEmpty()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(r.outB());
            slotIndex++;
        }
        for (ItemStack byproduct : r.byproducts()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(byproduct);
            slotIndex++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, Map.Entry<Item, MetalRecipe> recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Electrolyser-Metal-Rezepte.
    }
}
//?} else {
/*public final class ElectrolyserMetalJeiCategory {
    private ElectrolyserMetalJeiCategory() {}
}*///?}
