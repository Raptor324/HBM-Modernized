package com.hbm_m.module.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.SuperComputerRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code ModuleMachineSuperComputer}: drei Item-Eingaenge und -Ausgaenge, je ein Fluid-Ein- und -Ausgang; das Rezept
 * wird per Rezeptwaehler gesetzt und durch den Blueprint-Pool im Ordnerslot begrenzt (gleiche Semantik wie das
 * Chemiewerk-Modul).
 */
public class MachineModuleSuperComputer extends MachineModuleBase<SuperComputerRecipe> {

    private final FluidTank[] inputTanks;
    private final FluidTank[] outputTanks;

    @Nullable private ResourceLocation lastTankSetupRecipeId;

    public MachineModuleSuperComputer(com.hbm_m.interfaces.IEnergyReceiver energy, com.hbm_m.platform.ModItemStackHandler inv,
                              int[] solidIn, int[] solidOut, FluidTank[] fluidIn, FluidTank[] fluidOut, Level level) {
        super(0, energy, inv, level);
        this.inputSlots = solidIn;
        this.outputSlots = solidOut;
        this.inputTanks = fluidIn;
        this.outputTanks = fluidOut;
    }

    public int[] getInputSlots() { return inputSlots; }

    /** @return true, wenn ein Sync noetig ist */
    public boolean updateAndGetDirty(double speed, double powerMul, boolean extraCondition, ItemStack blueprint) {
        SuperComputerRecipe r = getRecipeByIdCached(getRecipeType(), selectedRecipeId);
        if (r != null && !isRecipeAllowedByBlueprint(r, blueprint)) {
            selectedRecipeId = null;
            lastTankSetupRecipeId = null;
            resetProgress();
            return true;
        }

        boolean wasProcessing = this.didProcess;
        super.update(speed, powerMul, extraCondition, blueprint);
        if (wasProcessing && !this.didProcess) {
            this.needsSync = true;
        }
        return needsSync;
    }


    @Override
    protected void onRecipeChanged(@Nullable SuperComputerRecipe previous, @Nullable SuperComputerRecipe current) {
        if (selectedRecipeId == null || current == null) {
            lastTankSetupRecipeId = null;
            return;
        }
        if (selectedRecipeId.equals(lastTankSetupRecipeId)) return;
        setupTanks(current);
        lastTankSetupRecipeId = selectedRecipeId;
    }

    public void syncTankConfigurationToRecipe(Level level) {
        if (selectedRecipeId == null) {
            lastTankSetupRecipeId = null;
            return;
        }
        SuperComputerRecipe recipe = getRecipeByIdCached(getRecipeType(), selectedRecipeId);
        if (recipe != null) {
            setupTanks(recipe);
            lastTankSetupRecipeId = selectedRecipeId;
        }
    }

    @Override
    protected net.minecraft.world.item.crafting.RecipeType<SuperComputerRecipe> getRecipeType() {
        return SuperComputerRecipe.Type.INSTANCE;
    }

    @Override
    protected @Nullable SuperComputerRecipe findRecipeForInputs() {
        return getRecipeByIdCached(getRecipeType(), selectedRecipeId);
    }

    @Override
    protected boolean canProcess(@Nullable SuperComputerRecipe recipe) {
        return recipe != null && canProcessInternal(recipe);
    }

    @Override
    protected void processCraft(SuperComputerRecipe recipe) {
        finishRecipe(recipe);
    }

    @Override
    protected boolean matchesCurrentRecipe(SuperComputerRecipe recipe) {
        if (selectedRecipeId == null) return false;
        return recipe != null && selectedRecipeId.equals(RecipeHooks.recipeId(level.getRecipeManager(), getRecipeType(), recipe));
    }

    @Override
    protected int getRecipeDuration(SuperComputerRecipe recipe) {
        return recipe.getDuration();
    }

    @Override
    protected long getRecipeEnergyCost(SuperComputerRecipe recipe) {
        return recipe.getPowerConsumption();
    }

    @Override
    protected @Nullable SuperComputerRecipe findRecipeForItem(ItemStack stack) {
        SuperComputerRecipe r = getRecipeByIdCached(getRecipeType(), selectedRecipeId);
        if (r == null) return null;
        for (var in : r.getItemInputs()) {
            if (in.ingredient().test(stack)) return r;
        }
        return null;
    }

    @Override
    protected boolean isRecipeAllowedByBlueprint(SuperComputerRecipe recipe, @Nullable ItemStack blueprint) {
        return isBlueprintAllowedForPool(recipe.getBlueprintPool(), blueprint);
    }

    private static Fluid fluidOf(SuperComputerRecipe.FluidIngredient in) {
        return BuiltInRegistries.FLUID.get(in.fluidId());
    }

    public void setupTanks(@Nullable SuperComputerRecipe recipe) {
        if (recipe == null) return;
        List<SuperComputerRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < inputTanks.length; i++) {
            Fluid fluid = i < fluidInputs.size() ? fluidOf(fluidInputs.get(i)) : null;
            if (fluid != null && fluid != Fluids.EMPTY) inputTanks[i].conform(fluid);
            else inputTanks[i].resetTank();
            // Original: changeTankSize(max(fill, Rezeptmenge * 2, 4000))
            if (i < fluidInputs.size()) inputTanks[i].changeTankSize(Math.max(Math.max(inputTanks[i].getFluidAmountMb(), fluidInputs.get(i).amount() * 2), 4_000));
        }
        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < outputTanks.length; i++) {
            if (i < fluidOutputs.size() && !fluidOutputs.get(i).isEmpty()) outputTanks[i].conform(fluidOutputs.get(i).getFluid());
            else outputTanks[i].resetTank();
            if (i < fluidOutputs.size() && !fluidOutputs.get(i).isEmpty()) outputTanks[i].changeTankSize(Math.max(Math.max(outputTanks[i].getFluidAmountMb(), (int) fluidOutputs.get(i).getAmount() * 2), 4_000));
        }
    }

    private boolean canProcessInternal(SuperComputerRecipe recipe) {
        List<SuperComputerRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            if (i >= inputSlots.length) return false;
            ItemStack slotStack = itemHandler.getStackInSlot(inputSlots[i]);
            SuperComputerRecipe.CountedIngredient req = itemInputs.get(i);
            if (!req.ingredient().test(slotStack) || slotStack.getCount() < req.count()) return false;
        }

        List<SuperComputerRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            if (i >= inputTanks.length) return false;
            Fluid fluid = fluidOf(fluidInputs.get(i));
            if (fluid == null || fluid == Fluids.EMPTY) return false;
            FluidTank tank = inputTanks[i];
            if (tank.isEmpty()
                    || !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), fluid)
                    || tank.getFluidAmountMb() < fluidInputs.get(i).amount()) {
                return false;
            }
        }

        if (!canFitAllItemOutputs(recipe.getItemOutputs(), outputSlots)) return false;
        // Zufallsausgaben: jede moegliche Variante muss zusammen mit den festen Ausgaben Platz haben
        for (List<SuperComputerRecipe.WeightedStack> group : recipe.getChanceOutputs()) {
            for (SuperComputerRecipe.WeightedStack w : group) {
                List<ItemStack> worst = new java.util.ArrayList<>(recipe.getItemOutputs());
                worst.add(w.stack());
                if (!canFitAllItemOutputs(worst, outputSlots)) return false;
            }
        }

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            if (i >= outputTanks.length) return false;
            FluidTank tank = outputTanks[i];
            if (!tank.isEmpty() && !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), output.getFluid())) return false;
            if (tank.getFluidAmountMb() + (int) output.getAmount() > tank.getCapacityMb()) return false;
        }
        return true;
    }

    private void finishRecipe(SuperComputerRecipe recipe) {
        List<SuperComputerRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            itemHandler.getStackInSlot(inputSlots[i]).shrink(itemInputs.get(i).count());
        }

        List<SuperComputerRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drainMb(fluidInputs.get(i).amount());
        }

        List<ItemStack> outputs = new java.util.ArrayList<>(recipe.getItemOutputs());
        outputs.addAll(recipe.rollChanceOutputs(level != null ? level.random : net.minecraft.util.RandomSource.create()));
        placeAllItemOutputs(outputs, outputSlots);

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            outputTanks[i].fillMb(output.getFluid(), (int) output.getAmount());
        }
    }

    @Nullable
    public SuperComputerRecipe peekRecipe() {
        return getRecipeByIdCached(getRecipeType(), selectedRecipeId);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation id) {
        setSelectedRecipeId(id);
        this.lastTankSetupRecipeId = null;
    }

    @Override
    protected void writeExtraToNbt(CompoundTag nbt) {
        nbt.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) nbt.putString("SelectedRecipe", selectedRecipeId.toString());
    }

    @Override
    protected void readExtraFromNbt(CompoundTag nbt) {
        if (nbt.contains("HasRecipe") && nbt.getBoolean("HasRecipe")) {
            selectedRecipeId = ResourceLocation.tryParse(nbt.getString("SelectedRecipe"));
        } else {
            selectedRecipeId = null;
        }
    }

    @Override
    protected void writeExtraToBuf(FriendlyByteBuf buf) {
        buf.writeBoolean(selectedRecipeId != null);
        if (selectedRecipeId != null) buf.writeResourceLocation(selectedRecipeId);
    }

    @Override
    protected void readExtraFromBuf(FriendlyByteBuf buf) {
        selectedRecipeId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }
}
