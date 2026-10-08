package com.hbm_m.module.machine;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.index.ModRecipeIndex;

public class MachineModuleAdvancedAssembler extends MachineModuleBase<AssemblerRecipe> {

    public MachineModuleAdvancedAssembler(int moduleIndex, IEnergyReceiver energyStorage,
                                          ModItemStackHandler itemHandler, Level level) {
        super(moduleIndex, energyStorage, itemHandler, level);

        this.inputSlots = new int[12];
        for (int i = 0; i < 12; i++) {
            this.inputSlots[i] = 4 + i;
        }
        this.outputSlots = new int[] { 16 };
    }

    // ========== BUILDER METHODS ==========

    /** Original ModuleMachineAssembler: ein Eingangs- und ein Ausgangstank fuer inputFluid/outputFluid. */
    @Nullable private com.hbm_m.inventory.fluid.tank.FluidTank inputTank;
    @Nullable private com.hbm_m.inventory.fluid.tank.FluidTank outputTank;

    public MachineModuleAdvancedAssembler setFluidTanks(com.hbm_m.inventory.fluid.tank.FluidTank in,
                                                         com.hbm_m.inventory.fluid.tank.FluidTank out) {
        this.inputTank = in;
        this.outputTank = out;
        return this;
    }

    /** setupTanks + Mengenpruefung des Originals (nur fuer Rezepte mit Fluessigkeiten). */
    private boolean fluidsAllow(AssemblerRecipe recipe) {
        if (!recipe.getFluidInputs().isEmpty()) {
            if (inputTank == null) return false;
            var fs = recipe.getFluidInputs().get(0);
            if (inputTank.getTankType() != fs.getFluid()) inputTank.conform(fs.getFluid());
            if (inputTank.getFill() < fs.getAmount()) return false;
        }
        if (!recipe.getFluidOutputs().isEmpty()) {
            if (outputTank == null) return false;
            var fs = recipe.getFluidOutputs().get(0);
            if (outputTank.getTankType() != fs.getFluid()) outputTank.conform(fs.getFluid());
            if (outputTank.getFill() + fs.getAmount() > outputTank.getMaxFill()) return false;
        }
        return true;
    }

    public MachineModuleAdvancedAssembler setInputSlots(int startSlot, int count) {
        this.inputSlots = new int[count];
        for (int i = 0; i < count; i++) {
            this.inputSlots[i] = startSlot + i;
        }
        return this;
    }

    public MachineModuleAdvancedAssembler setOutputSlot(int slot) {
        this.outputSlots = new int[] { slot };
        return this;
    }

    @Override
    protected AssemblerRecipe.Type getRecipeType() {
        return AssemblerRecipe.Type.INSTANCE;
    }

    @Override
    @Nullable
    public AssemblerRecipe findRecipeForInputs() {
        if (level == null) return null;

        ItemStack blueprint = itemHandler.getStackInSlot(1);
        for (AssemblerRecipe recipe : ModRecipeIndex.of(level.getRecipeManager()).getAll(getRecipeType())) {
            if (matchesRecipe(recipe) && isRecipeAllowedByBlueprint(recipe, blueprint)) return recipe;
        }
        return null;
    }

    private boolean matchesRecipe(AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        ItemStack[] inputCopy = new ItemStack[inputSlots.length];
        for (int i = 0; i < inputSlots.length; i++) {
            inputCopy[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
        }

        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < inputCopy.length; i++) {
                if (!inputCopy[i].isEmpty() && ingredient.test(inputCopy[i])) {
                    inputCopy[i].shrink(1);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    /**
     * API used by BE/GUI: prefer a specific recipe (or clear preference).
     * Stored as ID, resolved via {@link ModRecipeIndex} when needed.
     */
    public void setPreferredRecipe(@Nullable AssemblerRecipe recipe) {
        setPreferredRecipeId(recipe != null ? RecipeHooks.recipeId(level.getRecipeManager(), getRecipeType(), recipe) : null);
    }

    @Nullable
    public AssemblerRecipe getPreferredRecipe() {
        return preferredRecipeId != null ? getRecipeByIdCached(getRecipeType(), preferredRecipeId) : null;
    }

    @Override
    protected boolean matchesCurrentRecipe(AssemblerRecipe recipe) {
        return matchesRecipe(recipe);
    }

    @Override
    public boolean canProcess(AssemblerRecipe recipe) {
        if (recipe == null) return false;

        if (!matchesRecipe(recipe)) return false;
        if (!fluidsAllow(recipe)) return false;

        ItemStack outputSlot = itemHandler.getStackInSlot(outputSlots[0]);
        ItemStack result = recipe.getResultItem(level.registryAccess());

        if (outputSlot.isEmpty()) return true;

        //? if < 1.21.1 {
        if (!PlatformHooks.isSameItemSameTags(outputSlot, result)) return false;
        //?} else {
        /*if (!ItemStack.isSameItemSameComponents(outputSlot, result)) return false;
        *///?}

        return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize();
    }

    @Override
    protected void processCraft(AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        for (Ingredient ingredient : ingredients) {
            for (int slot : inputSlots) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    itemHandler.extractItem(slot, 1, false);
                    break;
                }
            }
        }

        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();

        itemHandler.insertItem(outputSlots[0], result, false);

        if (!recipe.getFluidInputs().isEmpty() && inputTank != null) {
            inputTank.setFill(inputTank.getFill() - (int) recipe.getFluidInputs().get(0).getAmount());
        }
        if (!recipe.getFluidOutputs().isEmpty() && outputTank != null) {
            var fs = recipe.getFluidOutputs().get(0);
            outputTank.conform(fs.getFluid());
            outputTank.setFill(outputTank.getFill() + (int) fs.getAmount());
        }
    }

    @Override
    protected int getRecipeDuration(AssemblerRecipe recipe) {
        return recipe.getDuration();
    }

    @Override
    protected long getRecipeEnergyCost(AssemblerRecipe recipe) {
        return recipe.getPowerConsumption();
    }

    @Override
    @Nullable
    protected AssemblerRecipe findRecipeForItem(ItemStack stack) {
        if (level == null) return null;

        for (AssemblerRecipe recipe : ModRecipeIndex.of(level.getRecipeManager()).getAll(getRecipeType())) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.test(stack)) {
                    return recipe;
                }
            }
        }

        return null;
    }

    @Override
    protected boolean isRecipeAllowedByBlueprint(AssemblerRecipe recipe, @Nullable ItemStack blueprint) {
        return isBlueprintAllowedForPool(recipe.getBlueprintPool(), blueprint);
    }
}
