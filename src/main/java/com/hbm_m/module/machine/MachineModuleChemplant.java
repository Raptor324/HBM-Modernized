package com.hbm_m.module.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.platform.ModFluidTank;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe.CountedIngredient;
import com.hbm_m.recipe.ChemicalPlantRecipe.FluidIngredient;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Модуль крафта хим. установки — порт ModuleMachineChemplant из 1.7.10.
 *
 * В отличие от {@link MachineModuleAdvancedAssembler}, рецепт выбирается
 * явно через GUI (по {@link ResourceLocation}), а не ищется автоматически.
 * Модуль управляет:
 *   - конформированием баков ({@link #setupTanks})
 *   - проверкой входов/выходов ({@link #canProcess})
 *   - потреблением энергии и прогрессом
 *   - завершением крафта ({@link #finishRecipe})
 */
public class MachineModuleChemplant {

    private final IEnergyReceiver energyStorage;
    private final ModItemStackHandler inventory;

    private final int[] solidInputSlots;
    private final int[] solidOutputSlots;
    private final ModFluidTank[] inputTanks;
    private final ModFluidTank[] outputTanks;

    @Nullable private ResourceLocation selectedRecipeId;
    @Nullable private ChemicalPlantRecipe cachedRecipe;
    private boolean recipeCacheDirty;

    private int progress;
    private int maxProgress = 100;
    private double progressAccum;

    public boolean didProcess;
    public boolean needsSync;

    public MachineModuleChemplant(IEnergyReceiver energy, ModItemStackHandler inv,
                                  int[] solidIn, int[] solidOut,
                                  ModFluidTank[] fluidIn, ModFluidTank[] fluidOut) {
        this.energyStorage = energy;
        this.inventory = inv;
        this.solidInputSlots = solidIn;
        this.solidOutputSlots = solidOut;
        this.inputTanks = fluidIn;
        this.outputTanks = fluidOut;
    }

    /**
     * Основной метод обновления — вызывается из BE каждый серверный тик.
     *
     * @param level мир (для поиска рецепта)
     * @param blueprint стак папки чертежей (может быть пуст)
     * @param speed множитель скорости (1.0 = базовая, модифицируется апгрейдами)
     * @param powerMul множитель потребления энергии (1.0 = базовое)
     * @return true, если состояние изменилось и нужен sync
     */
    public boolean update(Level level, ItemStack blueprint, double speed, double powerMul) {
        didProcess = false;
        needsSync = false;

        ChemicalPlantRecipe recipe = getCachedRecipe(level);

        if (recipe != null && !isRecipeValidForBlueprint(recipe, blueprint)) {
            selectedRecipeId = null;
            cachedRecipe = null;
            recipeCacheDirty = false;
            recipe = null;
            progress = 0;
            needsSync = true;
        }

        setupTanks(recipe);

        if (recipe != null) {
            maxProgress = recipe.getDuration();
            long powerPerTick = (long) (recipe.getPowerConsumption() * powerMul);

            if (canProcess(recipe) && energyStorage.getEnergyStored() >= powerPerTick) {
                energyStorage.setEnergyStored(energyStorage.getEnergyStored() - powerPerTick);
                progressAccum += speed;
                int steps = (int) progressAccum;
                if (steps > 0) {
                    progressAccum -= steps;
                    progress += steps;
                }
                didProcess = true;
                needsSync = true;

                if (progress >= maxProgress) {
                    progress = 0;
                    progressAccum = 0;
                    finishRecipe(recipe);
                }
            } else {
                if (progress != 0 || didProcess) {
                    progress = 0;
                    didProcess = false;
                    needsSync = true;
                }
            }
        } else {
            if (progress != 0 || didProcess) {
                progress = 0;
                didProcess = false;
                needsSync = true;
            }
        }
        return needsSync;
    }

    public void setupTanks(@Nullable ChemicalPlantRecipe recipe) {
        if (recipe == null) return;
        List<FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < inputTanks.length; i++) {
            if (i < fluidInputs.size()) {
                Fluid fluid = BuiltInRegistries.FLUID.get(fluidInputs.get(i).fluidId());
                if (fluid != null && fluid != Fluids.EMPTY) {
                    inputTanks[i].conform(fluid);
                } else {
                    inputTanks[i].resetTank();
                }
            } else {
                inputTanks[i].resetTank();
            }
        }
        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < outputTanks.length; i++) {
            if (i < fluidOutputs.size() && !fluidOutputs.get(i).isEmpty()) {
                outputTanks[i].conform(fluidOutputs.get(i).getFluid());
            } else {
                outputTanks[i].resetTank();
            }
        }
    }

    public boolean canProcess(ChemicalPlantRecipe recipe) {
        List<CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            if (i >= solidInputSlots.length) return false;
            int slot = solidInputSlots[i];
            ItemStack slotStack = inventory.getStackInSlot(slot);
            CountedIngredient req = itemInputs.get(i);
            if (!req.ingredient().test(slotStack) || slotStack.getCount() < req.count()) return false;
        }

        List<FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            if (i >= inputTanks.length) return false;
            FluidIngredient req = fluidInputs.get(i);
            Fluid fluid = BuiltInRegistries.FLUID.get(req.fluidId());
            if (fluid == null) return false;
            ModFluidTank tank = inputTanks[i];
            if (tank.isEmpty()
                || tank.getStoredFluid() != fluid
                || tank.getFluidAmountMb() < req.amount()) {
                return false;
            }
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            if (i >= solidOutputSlots.length) return false;
            int slot = solidOutputSlots[i];
            ItemStack slotStack = inventory.getStackInSlot(slot);
            if (!slotStack.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(slotStack, output)) return false;
                if (slotStack.getCount() + output.getCount() > slotStack.getMaxStackSize()) return false;
            }
        }

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            if (i >= outputTanks.length) return false;
            ModFluidTank tank = outputTanks[i];
            if (!tank.isEmpty() && tank.getStoredFluid() != output.getFluid()) return false;
            if (tank.getFluidAmountMb() + (int) output.getAmount() > tank.getCapacityMb()) return false;
        }
        return true;
    }

    private void finishRecipe(ChemicalPlantRecipe recipe) {
        List<CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            int slot = solidInputSlots[i];
            inventory.getStackInSlot(slot).shrink(itemInputs.get(i).count());
        }

        List<FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drainMb(fluidInputs.get(i).amount());
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            int slot = solidOutputSlots[i];
            ItemStack slotStack = inventory.getStackInSlot(slot);
            if (slotStack.isEmpty()) {
                inventory.setStackInSlot(slot, output.copy());
            } else {
                slotStack.grow(output.getCount());
            }
        }

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            outputTanks[i].fillMb(output.getFluid(), (int) output.getAmount());
        }
    }

    private boolean isRecipeValidForBlueprint(ChemicalPlantRecipe recipe, ItemStack folder) {
        String pool = recipe.getBlueprintPool();
        if (pool == null || pool.isEmpty()) return true;
        String installed = com.hbm_m.item.industrial.ItemBlueprintFolder.getBlueprintPool(folder);
        return installed != null && !installed.isEmpty() && installed.equals(pool);
    }

    @Nullable
    private ChemicalPlantRecipe getCachedRecipe(Level level) {
        if (selectedRecipeId == null) {
            cachedRecipe = null;
            return null;
        }
        if (cachedRecipe == null || recipeCacheDirty) {
            cachedRecipe = level.getRecipeManager()
                .byKey(selectedRecipeId)
                .filter(r -> r instanceof ChemicalPlantRecipe)
                .map(r -> (ChemicalPlantRecipe) r)
                .orElse(null);
            recipeCacheDirty = false;
        }
        return cachedRecipe;
    }

    // === Getters / Setters ===

    public void setSelectedRecipe(@Nullable ResourceLocation id) {
        this.selectedRecipeId = id;
        this.cachedRecipe = null;
        this.recipeCacheDirty = true;
        this.progress = 0;
        this.didProcess = false;
        this.needsSync = true;
    }

    @Nullable public ResourceLocation getSelectedRecipeId() { return selectedRecipeId; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public boolean getDidProcess() { return didProcess; }

    // === NBT ===

    public void writeNBT(CompoundTag tag) {
        tag.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
    }

    public void readNBT(CompoundTag tag) {
        if (tag.contains("HasRecipe") && tag.getBoolean("HasRecipe")) {
            selectedRecipeId = ResourceLocation.tryParse(tag.getString("SelectedRecipe"));
            recipeCacheDirty = true;
        } else {
            selectedRecipeId = null;
            cachedRecipe = null;
            recipeCacheDirty = false;
        }
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        if (maxProgress <= 0) maxProgress = 100;
    }
}
