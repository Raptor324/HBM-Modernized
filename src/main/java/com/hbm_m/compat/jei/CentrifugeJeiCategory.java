package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CentrifugeRecipe;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

//? if forge {
public class CentrifugeJeiCategory extends JeiUniversalRecipeCategory<CentrifugeRecipe> {

    public static final RecipeType<CentrifugeRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "centrifuge", CentrifugeRecipe.class);

    public CentrifugeJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{new ItemStack(ModBlocks.CENTRIFUGE.get())});
    }

    @Override
    public RecipeType<CentrifugeRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.centrifuge");
    }

    @Override
    protected int getInputCount(CentrifugeRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(CentrifugeRecipe recipe) {
        return (int) Arrays.stream(recipe.getOutputs())
                .filter(stack -> stack != null && !stack.isEmpty())
                .count();
    }

    @Override
    protected List<List<ItemStack>> getInputStacks(CentrifugeRecipe recipe) {
        return List.of(Arrays.asList(recipe.getInput().getItems()));
    }

    @Override
    protected List<ItemStack> getOutputStacks(CentrifugeRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack stack : recipe.getOutputs()) {
            if (stack != null && !stack.isEmpty()) {
                outputs.add(stack);
            }
        }
        return outputs;
    }

}
//?} else {
/*public final class CentrifugeJeiCategory {
    private CentrifugeJeiCategory() {}
}*///?}