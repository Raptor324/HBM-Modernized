package com.hbm_m.module.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.recipe.index.ModRecipeIndex;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Одна из 4 параллельных "линий" Chemical Factory (1.7.10 {@code TileEntityMachineChemicalFactory}
 * + {@code ModuleMachineChemplant} x4).
 *
 * <p>Отличие от {@link MachineModuleChemplant} (одиночный Chemical Plant): в оригинале рецепт для
 * каждой линии выбирался вручную через GUI (blueprint-driven selector). Мы сознательно упрощаем это —
 * рецепт подбирается автоматически по содержимому входных слотов/баков линии, как это уже делает
 * {@link MachineModuleAdvancedAssembler} для сборочной машины. Ручной выбор рецепта — nice-to-have,
 * не являющийся частью базовой функциональности машины, поэтому он пропущен в этом порту.</p>
 */
public class MachineModuleChemFactoryLane extends MachineModuleBase<ChemicalPlantRecipe> {

    private final FluidTank[] inputTanks;
    private final FluidTank[] outputTanks;

    @Nullable private ChemicalPlantRecipe lastTankSetupRecipe;

    public MachineModuleChemFactoryLane(int index, IEnergyReceiver energy, ModItemStackHandler inv,
                                         int[] solidIn, int[] solidOut,
                                         FluidTank[] fluidIn, FluidTank[] fluidOut,
                                         Level level) {
        super(index, energy, inv, level);
        this.inputSlots = solidIn;
        this.outputSlots = solidOut;
        this.inputTanks = fluidIn;
        this.outputTanks = fluidOut;
    }

    /**
     * Обновление линии за тик. Возвращает true, если состояние изменилось и требуется sync
     * (совпадает по семантике с {@code needsSync} базового класса, добавлено для симметрии с
     * {@link MachineModuleChemplant#updateAndGetDirty}).
     */
    public boolean updateAndGetDirty(double speed, double powerMul, boolean extraCondition) {
        super.update(speed, powerMul, extraCondition, null);
        return needsSync;
    }

    @Override
    protected boolean requiresFullEnergyBufferToStart() {
        // Как оригинальная химмашина 1.7.10: достаточно энергии на текущий тик, без ожидания полного буфера.
        return false;
    }

    @Override
    protected RecipeType<ChemicalPlantRecipe> getRecipeType() {
        return ChemicalPlantRecipe.Type.INSTANCE;
    }

    @Override
    @Nullable
    protected ChemicalPlantRecipe findRecipeForInputs() {
        if (level == null) return null;
        for (ChemicalPlantRecipe recipe : ModRecipeIndex.of(level.getRecipeManager()).getAll(getRecipeType())) {
            if (matchesInputs(recipe)) return recipe;
        }
        return null;
    }

    /**
     * Проверка "похож ли рецепт на текущее содержимое линии" — используется и для авто-подбора,
     * и для валидации того, что уже выбранный рецепт всё ещё уместен (иначе прогресс сбрасывается).
     */
    private boolean matchesInputs(ChemicalPlantRecipe recipe) {
        List<ChemicalPlantRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        if (itemInputs.size() > inputSlots.length) return false;
        for (int i = 0; i < itemInputs.size(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(inputSlots[i]);
            if (stack.isEmpty() || !itemInputs.get(i).ingredient().test(stack)) return false;
        }

        List<ChemicalPlantRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        if (fluidInputs.size() > inputTanks.length) return false;
        for (int i = 0; i < fluidInputs.size(); i++) {
            Fluid fluid = BuiltInRegistries.FLUID.get(fluidInputs.get(i).fluidId());
            if (fluid == null || fluid == Fluids.EMPTY) return false;
            FluidTank tank = inputTanks[i];
            if (tank.isEmpty()) {
                // Без предметного якоря нельзя достоверно опознать рецепт по пустому баку.
                if (itemInputs.isEmpty()) return false;
                continue;
            }
            if (!VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), fluid)) return false;
        }
        // Рецепты вообще без входов не матчим — иначе линия "залипнет" на первом таком рецепте.
        return !itemInputs.isEmpty() || !fluidInputs.isEmpty();
    }

    @Override
    protected boolean matchesCurrentRecipe(ChemicalPlantRecipe recipe) {
        return recipe != null && matchesInputs(recipe);
    }

    @Override
    protected boolean canProcess(@Nullable ChemicalPlantRecipe recipe) {
        if (recipe == null) return false;
        return canProcessInternal(recipe);
    }

    @Override
    protected void processCraft(ChemicalPlantRecipe recipe) {
        finishRecipe(recipe);
    }

    @Override
    protected int getRecipeDuration(ChemicalPlantRecipe recipe) {
        return recipe.getDuration();
    }

    @Override
    protected long getRecipeEnergyCost(ChemicalPlantRecipe recipe) {
        return recipe.getPowerConsumption();
    }

    @Override
    @Nullable
    protected ChemicalPlantRecipe findRecipeForItem(ItemStack stack) {
        if (level == null) return null;
        for (ChemicalPlantRecipe recipe : ModRecipeIndex.of(level.getRecipeManager()).getAll(getRecipeType())) {
            for (ChemicalPlantRecipe.CountedIngredient in : recipe.getItemInputs()) {
                if (in.ingredient().test(stack)) return recipe;
            }
        }
        return null;
    }

    @Override
    protected void onRecipeChanged(@Nullable ChemicalPlantRecipe previous, @Nullable ChemicalPlantRecipe current) {
        setupTanks(current);
    }

    private void setupTanks(@Nullable ChemicalPlantRecipe recipe) {
        if (recipe == null || recipe == lastTankSetupRecipe) return;

        List<ChemicalPlantRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
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
        lastTankSetupRecipe = recipe;
    }

    private boolean canProcessInternal(ChemicalPlantRecipe recipe) {
        List<ChemicalPlantRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            if (i >= inputSlots.length) return false;
            ItemStack slotStack = itemHandler.getStackInSlot(inputSlots[i]);
            ChemicalPlantRecipe.CountedIngredient req = itemInputs.get(i);
            if (!req.ingredient().test(slotStack) || slotStack.getCount() < req.count()) return false;
        }

        List<ChemicalPlantRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            if (i >= inputTanks.length) return false;
            ChemicalPlantRecipe.FluidIngredient req = fluidInputs.get(i);
            Fluid fluid = BuiltInRegistries.FLUID.get(req.fluidId());
            if (fluid == null) return false;
            FluidTank tank = inputTanks[i];
            if (tank.isEmpty()
                    || !VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), fluid)
                    || tank.getFluidAmountMb() < req.amount()) {
                return false;
            }
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        if (!canFitAllItemOutputs(itemOutputs, outputSlots)) {
            return false;
        }

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            if (i >= outputTanks.length) return false;
            FluidTank tank = outputTanks[i];
            if (!tank.isEmpty() && !VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), output.getFluid())) return false;
            if (tank.getFluidAmountMb() + (int) output.getAmount() > tank.getCapacityMb()) return false;
        }
        return true;
    }

    private void finishRecipe(ChemicalPlantRecipe recipe) {
        List<ChemicalPlantRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            itemHandler.getStackInSlot(inputSlots[i]).shrink(itemInputs.get(i).count());
        }

        List<ChemicalPlantRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drainMb(fluidInputs.get(i).amount());
        }

        placeAllItemOutputs(recipe.getItemOutputs(), outputSlots);

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            outputTanks[i].fillMb(output.getFluid(), (int) output.getAmount());
        }
    }

    /** Текущий (уже подобранный/проверенный на этом тике) рецепт — для GUI/рендера. */
    @Nullable
    public ChemicalPlantRecipe peekRecipe() {
        return currentRecipe;
    }
}
