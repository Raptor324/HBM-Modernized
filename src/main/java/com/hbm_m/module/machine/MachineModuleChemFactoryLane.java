package com.hbm_m.module.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;

import dev.architectury.fluid.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Одна из 4 параллельных "линий" Chemical Factory (1.7.10 {@code TileEntityMachineChemicalFactory}
 * + {@code ModuleMachineChemplant} x4).
 *
 * <p>Как и в оригинале, рецепт каждой линии выбирается вручную через GUI
 * ({@code GUIScreenRecipeSelector}): слот шаблона линии принимает папку чертежей
 * ({@code ItemBlueprintFolder}), а выбор рецепта ограничен пулом этой папки.
 * Логика повторяет {@link MachineModuleChemplant}, но с собственными слотами/баками линии.</p>
 */
public class MachineModuleChemFactoryLane extends MachineModuleBase<ChemicalPlantRecipe> {

    private final FluidTank[] inputTanks;
    private final FluidTank[] outputTanks;

    @Nullable private ResourceLocation lastTankSetupRecipeId;

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
    public boolean updateAndGetDirty(double speed, double powerMul, boolean extraCondition, ItemStack blueprint) {
        // Нужно сохранить семантику химмашины: при несоответствии blueprint pool сбрасываем выбранный рецепт целиком.
        ChemicalPlantRecipe r = getRecipeByIdCached(getRecipeType(), selectedRecipeId);
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
        // Рецепт выбирается явно через GUI (как в оригинале), поэтому "по инпутам" — это поиск по selectedRecipeId.
        return getRecipeByIdCached(getRecipeType(), selectedRecipeId);
    }

    @Override
    protected boolean matchesCurrentRecipe(ChemicalPlantRecipe recipe) {
        if (selectedRecipeId == null || recipe == null || level == null) return false;
        return selectedRecipeId.equals(RecipeHooks.recipeId(level.getRecipeManager(), getRecipeType(), recipe));
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
        // Валидация мусора в слотах: предмет подходит, только если он входит во входы выбранного рецепта.
        ChemicalPlantRecipe r = getRecipeByIdCached(getRecipeType(), selectedRecipeId);
        if (r == null) return null;
        for (ChemicalPlantRecipe.CountedIngredient in : r.getItemInputs()) {
            if (in.ingredient().test(stack)) return r;
        }
        return null;
    }

    @Override
    protected boolean isRecipeAllowedByBlueprint(ChemicalPlantRecipe recipe, @Nullable ItemStack blueprint) {
        return isBlueprintAllowedForPool(recipe.getBlueprintPool(), blueprint);
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

    public void setupTanks(@Nullable ChemicalPlantRecipe recipe) {
        if (recipe == null) return;

        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < inputTanks.length; i++) {
            if (i < fluidInputs.size() && !fluidInputs.get(i).isEmpty()) {
                inputTanks[i].conform(fluidInputs.get(i).getFluid());
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
            ItemStack slotStack = itemHandler.getStackInSlot(inputSlots[i]);
            ChemicalPlantRecipe.CountedIngredient req = itemInputs.get(i);
            if (!req.ingredient().test(slotStack) || slotStack.getCount() < req.count()) return false;
        }

        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            if (i >= inputTanks.length) return false;
            FluidStack req = fluidInputs.get(i);
            if (req.isEmpty()) return false;
            FluidTank tank = inputTanks[i];
            if (tank.isEmpty()
                    || !VanillaFluidEquivalence.sameSubstance(tank.getStoredFluid(), req.getFluid())
                    || tank.getFluidAmountMb() < (int) req.getAmount()) {
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

        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drainMb((int) fluidInputs.get(i).getAmount());
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

    /** Рецепт из менеджера по выбранному id — независимо от того, крутился ли тик. */
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
