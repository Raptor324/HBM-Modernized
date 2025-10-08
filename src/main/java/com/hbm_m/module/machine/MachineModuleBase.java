package com.hbm_m.module.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Базовый модуль машины, инкапсулирующий логику крафта.
 * Вдохновлён оригинальным ModuleMachineBase из HBM 1.7.10.
 * 
 * Адаптирован для 1.20.1:
 * - Использует RecipeManager вместо GenericRecipes
 * - Работает с IItemHandler вместо массива слотов
 * - Использует Forge Energy API (IEnergyStorage)
 */
public abstract class MachineModuleBase<T extends Recipe<?>> {
    // === CONFIGURATION ===
    protected final int moduleIndex;
    protected final IEnergyStorage energyStorage;
    protected final IItemHandler itemHandler;
    protected final Level level;
    protected int[] inputSlots;
    protected int[] outputSlots;

    // === RUNTIME STATE ===
    protected double progress = 0.0;
    protected int maxProgress = 100;
    @Nullable
    protected T currentRecipe = null;
    
    // === SIGNALS ===
    public boolean didProcess = false;
    public boolean needsSync = false;

    public MachineModuleBase(int moduleIndex, IEnergyStorage energyStorage, IItemHandler itemHandler, Level level) {
        this.moduleIndex = moduleIndex;
        this.energyStorage = energyStorage;
        this.itemHandler = itemHandler;
        this.level = level;
    }

    protected abstract RecipeType<T> getRecipeType();
    
    @Nullable
    protected abstract T findRecipeForInputs();
    
    protected abstract boolean canProcess(@Nullable T recipe);
    
    protected abstract void processCraft(T recipe);
    
    protected abstract boolean matchesCurrentRecipe(T recipe);
    
    protected abstract int getRecipeDuration(T recipe);
    
    protected abstract int getRecipeEnergyCost(T recipe);
    
    @Nullable
    protected abstract T findRecipeForItem(ItemStack stack);

    /**
     * ИСПРАВЛЕНО: Добавлен параметр extraCondition
     */
    public void update(double speedMultiplier, double powerMultiplier, boolean extraCondition) {
        this.didProcess = false;
        this.needsSync = false;

        // Поиск или валидация рецепта
        if (currentRecipe == null || !matchesCurrentRecipe(currentRecipe)) {
            currentRecipe = findRecipeForInputs();
            if (currentRecipe != null) {
                maxProgress = getRecipeDuration(currentRecipe);
                progress = 0.0;
                needsSync = true;
            }
        }

        // ИСПРАВЛЕНО: Учитываем extraCondition и потребляем энергию правильно
        if (extraCondition && currentRecipe != null && canProcess(currentRecipe)) {
            // ИСПРАВЛЕНО: Энергия потребляется ТОЛЬКО при завершении крафта, а не каждый тик
            double step = Math.min(speedMultiplier, 1.0);
            this.progress += step;
            this.didProcess = true;

            // Завершаем крафт
            if (progress >= maxProgress) {
                // ИСПРАВЛЕНО: Потребляем всю энергию за рецепт ЗДЕСЬ
                int totalEnergyCost = (int) (getRecipeEnergyCost(currentRecipe) * powerMultiplier);
                energyStorage.extractEnergy(totalEnergyCost, false);
                
                processCraft(currentRecipe);
                this.needsSync = true;

                // Проверяем, можем ли продолжить с тем же рецептом
                if (canProcess(currentRecipe)) {
                    progress -= maxProgress; // Продолжаем с остатком
                } else {
                    progress = 0.0;
                    currentRecipe = null;
                }
            }
        } else {
            // ИСПРАВЛЕНО: Сброс прогресса при отсутствии условий (как в 1.7.10)
            if (progress > 0.0) {
                progress = 0.0;
                needsSync = true;
            }
        }
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        for (int inputSlot : inputSlots) {
            if (inputSlot == slot) {
                T recipe = findRecipeForItem(stack);
                return recipe != null;
            }
        }
        return false;
    }

    public boolean isSlotClogged(int slot) {
        for (int inputSlot : inputSlots) {
            if (inputSlot == slot) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    return !isItemValidForSlot(slot, stack);
                }
            }
        }
        return false;
    }

    // === GETTERS ===
    public double getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getProgressInt() { return (int) progress; }
    public double getProgressPercent() { return maxProgress > 0 ? (progress / maxProgress) : 0.0; }
    
    @Nullable
    public T getCurrentRecipe() { return currentRecipe; }
    public boolean isProcessing() { return didProcess; }

    // === SERIALIZATION ===
    public void writeToNBT(CompoundTag nbt) {
        nbt.putDouble("Progress_" + moduleIndex, progress);
        nbt.putInt("MaxProgress_" + moduleIndex, maxProgress);
    }

    public void readFromNBT(CompoundTag nbt) {
        this.progress = nbt.getDouble("Progress_" + moduleIndex);
        this.maxProgress = nbt.getInt("MaxProgress_" + moduleIndex);
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeDouble(progress);
        buf.writeInt(maxProgress);
    }

    public void deserialize(FriendlyByteBuf buf) {
        this.progress = buf.readDouble();
        this.maxProgress = buf.readInt();
    }

    /**
     * Абстрактный метод для проверки blueprint pool
     * Должен быть реализован в подклассах, которые используют blueprint систему
     */
    protected abstract boolean isRecipeValidForBlueprint(T recipe, ItemStack blueprint);
    
    /**
     * ИСПРАВЛЕНО: Добавлена проверка blueprint pool
     */
    public void update(double speedMultiplier, double powerMultiplier, boolean extraCondition, ItemStack blueprint) {
        this.didProcess = false;
        this.needsSync = false;

        // Поиск или валидация рецепта
        if (currentRecipe == null || !matchesCurrentRecipe(currentRecipe)) {
            currentRecipe = findRecipeForInputs();
            if (currentRecipe != null) {
                maxProgress = getRecipeDuration(currentRecipe);
                progress = 0.0;
                needsSync = true;
            }
        }

        // НОВОЕ: Проверка blueprint pool (как в 1.7.10)
        if (currentRecipe != null && !isRecipeValidForBlueprint(currentRecipe, blueprint)) {
            this.didProcess = false;
            this.progress = 0.0;
            this.currentRecipe = null;
            this.needsSync = true;
            return;
        }

        // Обработка крафта
        if (extraCondition && currentRecipe != null && canProcess(currentRecipe)) {
            double step = Math.min(speedMultiplier, 1.0);
            this.progress += step;
            this.didProcess = true;

            if (progress >= maxProgress) {
                int totalEnergyCost = (int) (getRecipeEnergyCost(currentRecipe) * powerMultiplier);
                energyStorage.extractEnergy(totalEnergyCost, false);
                
                processCraft(currentRecipe);
                this.needsSync = true;

                if (canProcess(currentRecipe)) {
                    progress -= maxProgress;
                } else {
                    progress = 0.0;
                    currentRecipe = null;
                }
            }
        } else {
            if (progress > 0.0) {
                progress = 0.0;
                needsSync = true;
            }
        }
    }
}