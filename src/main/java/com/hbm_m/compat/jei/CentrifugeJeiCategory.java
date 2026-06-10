package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CentrifugeRecipes.RecipeInput;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI port of {@code CentrifugeRecipeHandler} (extends {@code NEIUniversalHandler}).
 */
//? if forge {
public class CentrifugeJeiCategory extends JeiUniversalRecipeCategory<CentrifugeJeiCategory.Recipe> {

    public static final RecipeType<Recipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "centrifuge", Recipe.class);

    public CentrifugeJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{new ItemStack(ModBlocks.CENTRIFUGE.get())});
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.centrifuge");
    }

    @Override
    protected int getInputCount(Recipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(Recipe recipe) {
        return (int) Arrays.stream(recipe.outputs).filter(stack -> !stack.isEmpty()).count();
    }

    @Override
    protected List<List<ItemStack>> getInputStacks(Recipe recipe) {
        return List.of(recipe.input.getDisplayStacks());
    }

    @Override
    protected List<ItemStack> getOutputStacks(Recipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack stack : recipe.outputs) {
            if (!stack.isEmpty()) {
                outputs.add(stack);
            }
        }
        return outputs;
    }

    public record Recipe(RecipeInput input, ItemStack[] outputs) {
    }
}
//?} else {
/*public final class CentrifugeJeiCategory {
    private CentrifugeJeiCategory() {}
}*///?}
