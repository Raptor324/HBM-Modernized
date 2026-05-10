package com.hbm_m.module.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.recipe.ChemicalPlantRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
public class MachineModuleChemplant extends MachineModuleBase<ChemicalPlantRecipe> {

    private final FluidTank[] inputTanks;
    private final FluidTank[] outputTanks;

    @Nullable private ResourceLocation lastTankSetupRecipeId;

    public MachineModuleChemplant(com.hbm_m.interfaces.IEnergyReceiver energy, com.hbm_m.platform.ModItemStackHandler inv,
                                  int[] solidIn, int[] solidOut,
                                  FluidTank[] fluidIn, FluidTank[] fluidOut,
                                  Level level) {
        super(0, energy, inv, level);
        this.inputSlots = solidIn;
        this.outputSlots = solidOut;
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
    public boolean updateAndGetDirty(double speed, double powerMul, boolean extraCondition, ItemStack blueprint) {
        // Нужно сохранить семантику химмашины: при несоответствии blueprint pool сбрасываем выбранный рецепт целиком.
        ChemicalPlantRecipe r = getRecipeByIdCached(getRecipeType(), selectedRecipeId);
        if (r != null && !isRecipeAllowedByBlueprint(r, blueprint)) {
            selectedRecipeId = null;
            lastTankSetupRecipeId = null;
            resetProgress();
            return true;
        }

        super.update(speed, powerMul, extraCondition, blueprint);
        return needsSync;
    }

    @Override
    protected boolean requiresFullEnergyBufferToStart() {
        return true;
    }

    @Override
    protected void onRecipeChanged(@Nullable ChemicalPlantRecipe previous, @Nullable ChemicalPlantRecipe current) {
        // Важно: не делаем reset/дренаж каждый тик — конфигурируем только при смене id.
        if (selectedRecipeId == null || current == null) {
            lastTankSetupRecipeId = null;
            return;
        }
        if (selectedRecipeId.equals(lastTankSetupRecipeId)) return;
        setupTanks(current);
        lastTankSetupRecipeId = selectedRecipeId;
    }

    /**
     * После выбора рецепта на сервере: немедленно конфигурирует входные/выходные баки,
     * чтобы блок-синх не уходил клиенту со старыми типами до следующего {@link #update}.
     */
    public void syncTankConfigurationToRecipe(Level level) {
        if (selectedRecipeId == null) {
            lastTankSetupRecipeId = null;
            return;
        }
        ChemicalPlantRecipe recipe = getRecipeByIdCached(getRecipeType(), selectedRecipeId);
        if (recipe != null) {
            setupTanks(recipe);
            lastTankSetupRecipeId = selectedRecipeId;
        }
    }

    @Override
    protected net.minecraft.world.item.crafting.RecipeType<ChemicalPlantRecipe> getRecipeType() {
        return ChemicalPlantRecipe.Type.INSTANCE;
    }

    @Override
    protected @Nullable ChemicalPlantRecipe findRecipeForInputs() {
        // Рецепт выбирается явно через GUI, поэтому "по инпутам" — это поиск по selectedRecipeId.
        return getRecipeByIdCached(getRecipeType(), selectedRecipeId);
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
    protected boolean matchesCurrentRecipe(ChemicalPlantRecipe recipe) {
        if (selectedRecipeId == null) return false;
        return recipe != null && selectedRecipeId.equals(recipe.getId());
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
    protected @Nullable ChemicalPlantRecipe findRecipeForItem(ItemStack stack) {
        // Для химмашины это используется только как "валидация" мусора в слотах — проверяем, подходит ли предмет в любой вход текущего рецепта.
        ChemicalPlantRecipe r = getRecipeByIdCached(getRecipeType(), selectedRecipeId);
        if (r == null) return null;
        for (var in : r.getItemInputs()) {
            if (in.ingredient().test(stack)) return r;
        }
        return null;
    }

    @Override
    protected boolean isRecipeAllowedByBlueprint(ChemicalPlantRecipe recipe, @Nullable ItemStack blueprint) {
        return isBlueprintAllowedForPool(recipe.getBlueprintPool(), blueprint);
    }

    public void setupTanks(@Nullable ChemicalPlantRecipe recipe) {
        if (recipe == null) return;
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
    }

    private boolean canProcessInternal(ChemicalPlantRecipe recipe) {
        List<ChemicalPlantRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            if (i >= inputSlots.length) return false;
            int slot = inputSlots[i];
            ItemStack slotStack = itemHandler.getStackInSlot(slot);
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
                    || !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), fluid)
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
            if (!tank.isEmpty() && !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), output.getFluid())) return false;
            if (tank.getFluidAmountMb() + (int) output.getAmount() > tank.getCapacityMb()) return false;
        }
        return true;
    }

    private void finishRecipe(ChemicalPlantRecipe recipe) {
        List<ChemicalPlantRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            int slot = inputSlots[i];
            itemHandler.getStackInSlot(slot).shrink(itemInputs.get(i).count());
        }

        List<ChemicalPlantRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drainMb(fluidInputs.get(i).amount());
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        placeAllItemOutputs(itemOutputs, outputSlots);

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            outputTanks[i].fillMb(output.getFluid(), (int) output.getAmount());
        }
    }

    /** Рецепт из менеджера по id — для сети и капабилити. */
    @Nullable
    public ChemicalPlantRecipe peekRecipe(Level level) {
        return getRecipeByIdCached(getRecipeType(), selectedRecipeId);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation id) {
        setSelectedRecipeId(id);
        this.lastTankSetupRecipeId = null;
    }

    @Override
    protected void writeExtraToNbt(CompoundTag nbt) {
        nbt.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            nbt.putString("SelectedRecipe", selectedRecipeId.toString());
        }
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
        if (selectedRecipeId != null) {
            buf.writeResourceLocation(selectedRecipeId);
        }
    }

    @Override
    protected void readExtraFromBuf(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            selectedRecipeId = buf.readResourceLocation();
        } else {
            selectedRecipeId = null;
        }
    }
}
