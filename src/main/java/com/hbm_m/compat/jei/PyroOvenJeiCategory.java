package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PyroOvenRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI-Kategorie des Pyrolyseofens (siehe {@link PyroOvenRecipe}).
 *
 * <p>Der Ofen nimmt wahlweise einen Gegenstand, eine Fluessigkeit oder beides und gibt
 * ebenso beides zurueck - darum sind hier alle vier Seiten wahlfrei.</p>
 *
 * <p>1:1 zum 1.7.10 {@code PyroHandler} ({@code NEIUniversalHandler}): dort steht das
 * Eingangsfeld als {@code [Fluessigkeitsicon, Item]} auf {@code getInputCoords} - die
 * Fluessigkeit landet auf (48, 24), das Item auf (30, 24); das Ausgangsfeld als
 * {@code [Item, Fluessigkeitsicon]} auf {@code getOutputCoords} - das Item auf (102, 24),
 * die Fluessigkeit auf (120, 24). Die Basis-Kategorie ({@link JeiFluidRecipeCategory})
 * nutzt stattdessen die {@code NEIGenericRecipeHandler}-Anordnung und zaehlt Fluessigkeiten
 * zuerst, darum wird hier beides ueberschrieben.</p>
 */
public class PyroOvenJeiCategory extends JeiFluidRecipeCategory<PyroOvenRecipe> {

    public static final RecipeType<PyroOvenRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "pyrooven", PyroOvenRecipe.class);

    public PyroOvenJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.PYROOVEN.get()) });
    }

    @Override
    public RecipeType<PyroOvenRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.pyrooven");
    }

    /** Original: {@code [ItemFluidIcon, AStack]} auf {@code NEIUniversalHandler.getInputCoords}. */
    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, PyroOvenRecipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getUniversalInputCoords(getInputCount(recipe));
        int slot = 0;

        for (FluidStack fluid : getInputFluids(recipe)) {
            addFluidSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slot][0] + inputXOffset, positions[slot][1], fluid);
            slot++;
        }

        for (Ingredient ingredient : getInputItems(recipe)) {
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slot][0] + inputXOffset, positions[slot][1])
                    .addItemStacks(countedItems(recipe, ingredient));
            slot++;
        }
    }

    /** Original: {@code [ItemStack, ItemFluidIcon]} auf {@code NEIUniversalHandler.getOutputCoords}. */
    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, PyroOvenRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getUniversalOutputCoords(getOutputCount(recipe));
        int slot = 0;

        for (ItemStack stack : getOutputItems(recipe)) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slot][0] + outputXOffset, positions[slot][1])
                    .addItemStack(stack);
            slot++;
        }

        for (FluidStack fluid : getOutputFluids(recipe)) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slot][0] + outputXOffset, positions[slot][1], fluid);
            slot++;
        }
    }

    /** Das Original zeigt die AStack-Stackgroesse ({@code extractForNEI}) - Biomasse/Teer x4. */
    private static List<ItemStack> countedItems(PyroOvenRecipe recipe, Ingredient ingredient) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack base : ingredient.getItems()) {
            ItemStack stack = base.copy();
            stack.setCount(recipe.getInputItemCount());
            stacks.add(stack);
        }
        return stacks;
    }

    @Override
    protected List<FluidStack> getInputFluids(PyroOvenRecipe recipe) {
        List<FluidStack> list = new ArrayList<>();
        if (recipe.getInputFluidStack() != null) list.add(recipe.getInputFluidStack());
        return list;
    }

    @Override
    protected List<Ingredient> getInputItems(PyroOvenRecipe recipe) {
        List<Ingredient> list = new ArrayList<>();
        if (recipe.getInputItem() != null) list.add(recipe.getInputItem());
        return list;
    }

    @Override
    protected List<FluidStack> getOutputFluids(PyroOvenRecipe recipe) {
        List<FluidStack> list = new ArrayList<>();
        if (recipe.getOutputFluidStack() != null) list.add(recipe.getOutputFluidStack());
        return list;
    }

    @Override
    protected List<ItemStack> getOutputItems(PyroOvenRecipe recipe) {
        List<ItemStack> list = new ArrayList<>();
        if (recipe.getOutputItem() != null) list.add(recipe.getOutputItem());
        return list;
    }
}
