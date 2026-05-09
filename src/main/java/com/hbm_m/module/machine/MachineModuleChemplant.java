package com.hbm_m.module.machine;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.platform.ModItemStackHandler;
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
public class MachineModuleChemplant {

    private final IEnergyReceiver energyStorage;
    private final ModItemStackHandler inventory;

    private final int[] solidInputSlots;
    private final int[] solidOutputSlots;
    private final FluidTank[] inputTanks;
    private final FluidTank[] outputTanks;

    @Nullable private ResourceLocation selectedRecipeId;
    @Nullable private ChemicalPlantRecipe cachedRecipe;
    private boolean recipeCacheDirty;
    @Nullable private ResourceLocation lastTankSetupRecipeId;

    private int progress;
    private int maxProgress = 100;
    private double progressAccum;

    public boolean didProcess;
    public boolean needsSync;

    public MachineModuleChemplant(IEnergyReceiver energy, ModItemStackHandler inv,
                                  int[] solidIn, int[] solidOut,
                                  FluidTank[] fluidIn, FluidTank[] fluidOut) {
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
            progressAccum = 0;
            needsSync = true;
        }

        setupTanksIfNeeded(recipe);

        if (recipe != null) {
            maxProgress = recipe.getDuration();
            long powerPerTick = (long) (recipe.getPowerConsumption() * powerMul);

            if (canProcess(recipe) && energyStorage.getEnergyStored() >= powerPerTick) {
                // Потребляем энергию для текущего тика
                energyStorage.setEnergyStored(energyStorage.getEnergyStored() - powerPerTick);

                // ИЗМЕНЕНО: Кап скорости и перенос остатков прогресса как в 1.7.10
                double step = Math.min(speed, maxProgress);
                progressAccum += step;
                progress = (int) progressAccum;

                didProcess = true;
                needsSync = true;

                if (progressAccum >= maxProgress) {
                    finishRecipe(recipe);

                    if (canProcess(recipe)) {
                        progressAccum -= maxProgress;
                        progress = (int) progressAccum;
                    } else {
                        progressAccum = 0;
                        progress = 0;
                    }
                }
            } else {
                if (progressAccum != 0 || didProcess) {
                    progressAccum = 0;
                    progress = 0;
                    didProcess = false;
                    needsSync = true;
                }
            }
        } else {
            if (progressAccum != 0 || didProcess) {
                progressAccum = 0;
                progress = 0;
                didProcess = false;
                needsSync = true;
            }
        }
        return needsSync;
    }

    /**
     * Настройка типов баков под выбранный рецепт.
     *
     * Важно: не делаем reset/дренаж каждый тик — иначе любые внешние трубы будут
     * бесконечно пытаться заливать "лишние" баки, которые тут же обнуляются.
     */
    private void setupTanksIfNeeded(@Nullable ChemicalPlantRecipe recipe) {
        if (selectedRecipeId == null || recipe == null) {
            lastTankSetupRecipeId = null;
            return;
        }
        if (selectedRecipeId.equals(lastTankSetupRecipeId)) {
            return;
        }
        setupTanks(recipe);
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
        ChemicalPlantRecipe recipe = getCachedRecipe(level);
        if (recipe != null) {
            setupTanks(recipe);
            lastTankSetupRecipeId = selectedRecipeId;
        }
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

    public boolean canProcess(ChemicalPlantRecipe recipe) {
        List<ChemicalPlantRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            if (i >= solidInputSlots.length) return false;
            int slot = solidInputSlots[i];
            ItemStack slotStack = inventory.getStackInSlot(slot);
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
        if (!canFitAllItemOutputs(itemOutputs)) {
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
            int slot = solidInputSlots[i];
            inventory.getStackInSlot(slot).shrink(itemInputs.get(i).count());
        }

        List<ChemicalPlantRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drainMb(fluidInputs.get(i).amount());
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        placeAllItemOutputs(itemOutputs);

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            outputTanks[i].fillMb(output.getFluid(), (int) output.getAmount());
        }
    }

    /** Непустые предметные выходы рецепта — порядок важен для DFS. */
    private List<ItemStack> nonEmptyItemOutputs(List<ItemStack> itemOutputs) {
        List<ItemStack> list = new ArrayList<>(3);
        for (ItemStack o : itemOutputs) {
            if (o.isEmpty()) continue;
            list.add(o);
        }
        return list;
    }

    private ItemStack[] copyOutputSlotStacks() {
        ItemStack[] sim = new ItemStack[solidOutputSlots.length];
        for (int j = 0; j < solidOutputSlots.length; j++) {
            ItemStack cur = inventory.getStackInSlot(solidOutputSlots[j]);
            sim[j] = cur.isEmpty() ? ItemStack.EMPTY : cur.copy();
        }
        return sim;
    }

    private static boolean canMergeItemInto(ItemStack slotStack, ItemStack incoming) {
        if (incoming.isEmpty()) return true;
        if (slotStack.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(slotStack, incoming)) return false;
        return (long) slotStack.getCount() + incoming.getCount() <= slotStack.getMaxStackSize();
    }

    /**
     * Подбираем для каждой порции выхода один из трёх выходных слотов (слияние с уже лежащими стаками).
     */
    private boolean dfsPlaceItemOutputs(List<ItemStack> outs, int idx, ItemStack[] sim, int[] chosenSlotPerOutput) {
        if (idx >= outs.size()) return true;
        ItemStack inc = outs.get(idx);
        for (int j = 0; j < solidOutputSlots.length; j++) {
            ItemStack slotStack = sim[j];
            if (!canMergeItemInto(slotStack, inc)) continue;

            ItemStack prev = slotStack.isEmpty() ? ItemStack.EMPTY : slotStack.copy();
            if (slotStack.isEmpty()) {
                sim[j] = inc.copy();
            } else {
                ItemStack merged = slotStack.copy();
                merged.grow(inc.getCount());
                sim[j] = merged;
            }
            chosenSlotPerOutput[idx] = j;
            if (dfsPlaceItemOutputs(outs, idx + 1, sim, chosenSlotPerOutput)) return true;
            sim[j] = prev;
        }
        return false;
    }

    private boolean canFitAllItemOutputs(List<ItemStack> itemOutputs) {
        List<ItemStack> outs = nonEmptyItemOutputs(itemOutputs);
        if (outs.isEmpty()) return true;

        ItemStack[] sim = copyOutputSlotStacks();
        int[] pick = new int[outs.size()];
        return dfsPlaceItemOutputs(outs, 0, sim, pick);
    }

    private void placeAllItemOutputs(List<ItemStack> itemOutputs) {
        List<ItemStack> outs = nonEmptyItemOutputs(itemOutputs);
        if (outs.isEmpty()) return;
        ItemStack[] sim = copyOutputSlotStacks();
        int[] pick = new int[outs.size()];
        if (dfsPlaceItemOutputs(outs, 0, sim, pick)) {
            for (int i = 0; i < outs.size(); i++) {
                int j = pick[i];
                int slot = solidOutputSlots[j];
                ItemStack out = outs.get(i);
                ItemStack cur = inventory.getStackInSlot(slot);
                if (cur.isEmpty()) {
                    inventory.setStackInSlot(slot, out.copy());
                } else {
                    cur.grow(out.getCount());
                }
            }
            return;
        }
        // Fallback: порядная привязка выход → слот как в старом порте (DFS не нашёл решение из-за гонки/особого рецепта).
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            if (i >= solidOutputSlots.length) break;
            int slot = solidOutputSlots[i];
            ItemStack cur = inventory.getStackInSlot(slot);
            if (cur.isEmpty()) {
                inventory.setStackInSlot(slot, output.copy());
            } else {
                cur.grow(output.getCount());
            }
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

    /** Рецепт из кэша / менеджера без привязки к тику {@link #update}; для сети и капабилити. */
    @Nullable
    public ChemicalPlantRecipe peekRecipe(Level level) {
        return getCachedRecipe(level);
    }

    /**
     * Полезные хелперы из 1.7.10 для труб и воронок.
     * Проверяет, подходит ли предмет в слот для текущего рецепта.
     */
    public boolean isItemValid(int slot, ItemStack stack) {
        if (cachedRecipe == null) return false;
        List<ChemicalPlantRecipe.CountedIngredient> inputs = cachedRecipe.getItemInputs();
        for (int i = 0; i < solidInputSlots.length; i++) {
            if (solidInputSlots[i] == slot) {
                if (i < inputs.size()) {
                    return inputs.get(i).ingredient().test(stack);
                }
            }
        }
        return false;
    }

    /**
     * Возвращает true, если в слоте лежит мусор, не подходящий текущему рецепту.
     */
    public boolean isSlotClogged(int slot) {
        boolean isInput = false;
        for (int s : solidInputSlots) {
            if (s == slot) {
                isInput = true;
                break;
            }
        }
        if (!isInput) return false;

        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty()) return false;
        return !isItemValid(slot, stack);
    }

    // === Getters / Setters ===

    public void setSelectedRecipe(@Nullable ResourceLocation id) {
        this.selectedRecipeId = id;
        this.cachedRecipe = null;
        this.recipeCacheDirty = true;
        this.progressAccum = 0;
        this.progress = 0;
        this.didProcess = false;
        this.lastTankSetupRecipeId = null;
        this.needsSync = true;
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() { return selectedRecipeId; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public boolean getDidProcess() { return didProcess; }

    // === Сериализация (NBT и Sync) ===

    public void writeNBT(CompoundTag tag) {
        tag.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        tag.putDouble("progressAccum", progressAccum);
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
        progressAccum = tag.getDouble("progressAccum");
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        if (maxProgress <= 0) maxProgress = 100;
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeDouble(progressAccum);
        buf.writeInt(maxProgress);
        buf.writeBoolean(selectedRecipeId != null);
        if (selectedRecipeId != null) {
            buf.writeResourceLocation(selectedRecipeId);
        }
    }

    public void deserialize(FriendlyByteBuf buf) {
        this.progressAccum = buf.readDouble();
        this.progress = (int) this.progressAccum;
        this.maxProgress = buf.readInt();
        if (buf.readBoolean()) {
            this.selectedRecipeId = buf.readResourceLocation();
            this.recipeCacheDirty = true;
        } else {
            this.selectedRecipeId = null;
            this.cachedRecipe = null;
            this.recipeCacheDirty = false;
        }
    }
}
