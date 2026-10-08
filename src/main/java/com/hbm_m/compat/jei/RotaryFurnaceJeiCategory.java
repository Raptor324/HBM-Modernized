package com.hbm_m.compat.jei;
//? if forge {

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm_m.inventory.recipes.RotaryFurnaceRecipes.RecipeInput;
import com.hbm_m.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Original {@code RotaryFurnaceRecipes.getRecipes()}: Zutaten plus Fluid-Symbol, Ausgabe als fluessiger Schrott.
 */
public class RotaryFurnaceJeiCategory extends JeiUniversalRecipeCategory<RotaryFurnaceRecipe> {

    public static final RecipeType<RotaryFurnaceRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rotary_furnace", RotaryFurnaceRecipe.class);

    public RotaryFurnaceJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[] { new ItemStack(ModBlocks.ROTARY_FURNACE.get()) });
    }

    public static List<RotaryFurnaceRecipe> recipes() {
        return RotaryFurnaceRecipes.all();
    }

    @Override public RecipeType<RotaryFurnaceRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("container.machineRotaryFurnace"); }

    @Override
    protected int getInputCount(RotaryFurnaceRecipe recipe) {
        return recipe.ingredients.length + (recipe.fluid != null ? 1 : 0);
    }

    @Override protected int getOutputCount(RotaryFurnaceRecipe recipe) { return 1; }

    @Override
    protected List<List<ItemStack>> getInputStacks(RotaryFurnaceRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        for (RecipeInput in : recipe.ingredients) list.add(in.display());
        return list;
    }

    @Override
    protected List<ItemStack> getOutputStacks(RotaryFurnaceRecipe recipe) {
        return List.of(ItemScraps.create(recipe.output, true));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, IFocusGroup focuses) {
        int inputCount = getInputCount(recipe);
        int[][] inPos = JeiNeiLayout.getUniversalInputCoords(inputCount);
        int[][] outPos = JeiNeiLayout.getUniversalOutputCoords(1);

        List<List<ItemStack>> inputs = getInputStacks(recipe);
        for (int i = 0; i < inputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, inPos[i][0], inPos[i][1])
                    .setBackground(itemSlotBackground, -1, -1)
                    .addItemStacks(inputs.get(i));
        }

        // Original: ItemFluidIcon.make(recipe.fluid) als letzte Eingabe
        if (recipe.fluid != null) {
            int i = inputs.size();
            builder.addSlot(RecipeIngredientRole.INPUT, inPos[i][0], inPos[i][1])
                    .setBackground(itemSlotBackground, -1, -1)
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addFluidStack(recipe.fluid.type(), recipe.fluid.fill());
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, outPos[0][0], outPos[0][1])
                .setBackground(itemSlotBackground, -1, -1)
                .addItemStack(getOutputStacks(recipe).get(0));

        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStacks(java.util.Arrays.asList(getMachines(recipe)));
    }
}
//?} else {
/*public final class RotaryFurnaceJeiCategory {
    private RotaryFurnaceJeiCategory() {}
}*///?}
