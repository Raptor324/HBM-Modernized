package com.hbm_m.module.machine;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.index.ModRecipeIndex;

/**
 * Базовый модуль машины, инкапсулирующий логику крафта.
 * Вдохновлён оригинальным ModuleMachineBase из HBM 1.7.10.
 *
 * Адаптирован для 1.20.1:
 * - Использует RecipeManager вместо GenericRecipes
 * - Работает с IItemHandler вместо массива слотов
 * - ОБНОВЛЕНО: Использует ILongEnergyStorage для поддержки больших значений энергии
 */
public abstract class MachineModuleBase<T extends Recipe<?>> {
    // === CONFIGURATION ===
    protected final int moduleIndex;
    // ИЗМЕНЕНИЕ: Теперь используем ILongEnergyStorage вместо IEnergyStorage
    protected final IEnergyReceiver energyStorage;
    protected final ModItemStackHandler itemHandler;
    protected Level level;
    protected int[] inputSlots;
    protected int[] outputSlots;

    // === RUNTIME STATE ===
    protected double progress = 0.0;
    protected int maxProgress = 100;
    @Nullable
    protected T currentRecipe = null;

    // === RECIPE SELECTION / CACHE (ID based) ===
    @Nullable
    protected ResourceLocation selectedRecipeId = null;
    @Nullable
    protected ResourceLocation preferredRecipeId = null;
    protected boolean autoSelectRecipe = true;

    @Nullable
    private ResourceLocation cachedRecipeId = null;
    @Nullable
    private T cachedRecipeById = null;

    // === SIGNALS ===
    public boolean didProcess = false;
    public boolean needsSync = false;

    // ИЗМЕНЕНИЕ: Конструктор теперь принимает ILongEnergyStorage
    public MachineModuleBase(int moduleIndex, IEnergyReceiver energyStorage, ModItemStackHandler itemHandler, Level level) {
        this.moduleIndex = moduleIndex;
        this.energyStorage = energyStorage;
        this.itemHandler = itemHandler;
        this.level = level;
    }

    /** BlockEntity может создать модуль до установки level — обновляем ссылку при каждом тике. */
    public void setLevel(Level level) {
        this.level = level;
    }

    protected abstract RecipeType<T> getRecipeType();

    @Nullable
    protected abstract T findRecipeForInputs();

    protected abstract boolean canProcess(@Nullable T recipe);

    protected abstract void processCraft(T recipe);

    protected abstract boolean matchesCurrentRecipe(T recipe);

    protected abstract int getRecipeDuration(T recipe);

    // ИЗМЕНЕНИЕ: Возвращаем long вместо int
    protected abstract long getRecipeEnergyCost(T recipe);

    @Nullable
    protected abstract T findRecipeForItem(ItemStack stack);

    /**
     * Optional hook: вызывается при смене рецепта (ID или auto-выбор).
     * Например, машины с жидкостными баками могут сконфигурировать типы баков.
     */
    protected void onRecipeChanged(@Nullable T previous, @Nullable T current) {
        // no-op by default
    }

    /**
     * Хук семантики энергогейта:
     * - true (default): как у ассемблера — при старте ждём энергию на весь цикл.
     * - false: как у химмашины в 1.7.10 — достаточно энергии только на текущий тик.
     */
    protected boolean requiresFullEnergyBufferToStart() {
        return true;
    }

    /**
     * Центральная проверка blueprint pool.
     */
    protected static boolean isBlueprintPoolAllowed(@Nullable String recipePool, ItemStack blueprint) {
        if (recipePool == null || recipePool.isEmpty()) return true;
        String installed = ItemBlueprintFolder.getBlueprintPool(blueprint);
        return installed != null && !installed.isEmpty() && installed.equals(recipePool);
    }

    /** Утилита для рецептов, которые имеют поле blueprintPool (как в 1.7.10 pooled recipes). */
    protected static boolean isBlueprintAllowedForPool(@Nullable String recipePool, @Nullable ItemStack blueprint) {
        if (blueprint == null || blueprint.isEmpty()) {
            return recipePool == null || recipePool.isEmpty();
        }
        return isBlueprintPoolAllowed(recipePool, blueprint);
    }

    /**
     * Optional hook: recipe is allowed for current blueprint.
     * Default: always allowed (machines without blueprint system).
     */
    protected boolean isRecipeAllowedByBlueprint(T recipe, @Nullable ItemStack blueprint) {
        return true;
    }

    /**
     * Fast recipe lookup by id using {@link ModRecipeIndex}.
     */
    @Nullable
    protected final T getRecipeByIdCached(RecipeType<T> type, @Nullable ResourceLocation id) {
        if (level == null || id == null) {
            cachedRecipeId = null;
            cachedRecipeById = null;
            return null;
        }
        if (id.equals(cachedRecipeId) && cachedRecipeById != null) {
            return cachedRecipeById;
        }
        cachedRecipeId = id;
        cachedRecipeById = ModRecipeIndex.of(level.getRecipeManager())
                .getById(type, id)
                .orElse(null);
        return cachedRecipeById;
    }

    /**
     * Default selection: preferred -> selected -> auto (findRecipeForInputs).
     */
    @Nullable
    protected T pickRecipeForTick() {
        RecipeType<T> type = getRecipeType();
        if (preferredRecipeId != null) {
            return getRecipeByIdCached(type, preferredRecipeId);
        }
        if (selectedRecipeId != null) {
            return getRecipeByIdCached(type, selectedRecipeId);
        }
        if (autoSelectRecipe) {
            return findRecipeForInputs();
        }
        return null;
    }

    /**
     * Энергетический гейт "как у ассемблера":
     * - если крафт только начинается, машина ждёт пока накопится энергия на ВЕСЬ цикл;
     * - если крафт уже идёт, и на текущий тик энергии не хватает — прогресс не сбрасываем, просто ждём.
     *
     * Вынесено в базовый модуль, чтобы разные машины могли переиспользовать одну логику.
     */
    public static boolean hasEnoughEnergyToStartCraft(double progress, long storedEnergy, long energyPerTick, int maxProgress) {
        if (energyPerTick <= 0) return true;
        if (maxProgress <= 0) return true;
        if (progress > 0.0) return true;
        long totalEnergyRequired = energyPerTick * (long) maxProgress;
        return storedEnergy >= totalEnergyRequired;
    }

    public static boolean hasEnoughEnergyForTick(long storedEnergy, long energyPerTick) {
        return energyPerTick <= 0 || storedEnergy >= energyPerTick;
    }

    public final void update(double speedMultiplier, double powerMultiplier, boolean extraCondition) {
        update(speedMultiplier, powerMultiplier, extraCondition, null);
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

    /**
     * Возвращает список призрачных предметов для GUI
     * Переопределите в дочерних классах для специфической логики
     */
    public NonNullList<ItemStack> getGhostItems() {
        return NonNullList.create();
    }

    /**
     * Сбрасывает прогресс крафта и текущий рецепт.
     * Используется при принудительной смене рецепта через GUI.
     */
    public void resetProgress() {
        this.progress = 0.0;
        this.currentRecipe = null;
        this.didProcess = false;
        this.needsSync = true;
    }

    // === GETTERS ===
    public double getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getProgressInt() { return (int) progress; }
    public double getProgressPercent() { return maxProgress > 0 ? (progress / maxProgress) : 0.0; }

    @Nullable
    public T getCurrentRecipe() { return currentRecipe; }
    public boolean isProcessing() { return didProcess; }
    /** Совместимость со старым API модулей/BE. */
    public boolean getDidProcess() { return didProcess; }

    // === SERIALIZATION ===
    public void writeToNBT(CompoundTag nbt) {
        nbt.putDouble("Progress_" + moduleIndex, progress);
        nbt.putInt("MaxProgress_" + moduleIndex, maxProgress);
        writeExtraToNbt(nbt);
    }

    public void readFromNBT(CompoundTag nbt) {
        this.progress = nbt.getDouble("Progress_" + moduleIndex);
        this.maxProgress = nbt.getInt("MaxProgress_" + moduleIndex);
        readExtraFromNbt(nbt);
    }

    /** Совместимость: старое имя NBT-сериализации (химмашина и др.). */
    public final void writeNBT(CompoundTag tag) { writeToNBT(tag); }
    public final void readNBT(CompoundTag tag) { readFromNBT(tag); }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeDouble(progress);
        buf.writeInt(maxProgress);
        writeExtraToBuf(buf);
    }

    public void deserialize(FriendlyByteBuf buf) {
        this.progress = buf.readDouble();
        this.maxProgress = buf.readInt();
        readExtraFromBuf(buf);
    }

    /** Доп. состояние модуля, которое не относится к базовому прогрессу (например выбранный рецепт). */
    protected void writeExtraToNbt(CompoundTag nbt) {}
    protected void readExtraFromNbt(CompoundTag nbt) {}
    protected void writeExtraToBuf(FriendlyByteBuf buf) {}
    protected void readExtraFromBuf(FriendlyByteBuf buf) {}

    // === OUTPUT FIT / PLACEMENT HELPERS (multi-slot) ===

    protected final boolean canFitAllItemOutputs(java.util.List<ItemStack> itemOutputs, int[] outputSlots) {
        return OutputPlacement.canFitAll(itemHandler, itemOutputs, outputSlots);
    }

    protected final void placeAllItemOutputs(java.util.List<ItemStack> itemOutputs, int[] outputSlots) {
        OutputPlacement.placeAll(itemHandler, itemOutputs, outputSlots);
    }

    // === Selection setters (used by thin modules / GUIs) ===

    public void setSelectedRecipeId(@Nullable ResourceLocation id) {
        this.selectedRecipeId = id;
        this.cachedRecipeId = null;
        this.cachedRecipeById = null;
        resetProgress();
    }

    public void setPreferredRecipeId(@Nullable ResourceLocation id) {
        this.preferredRecipeId = id;
        this.cachedRecipeId = null;
        this.cachedRecipeById = null;
        resetProgress();
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    /**
     * Обновление с поддержкой blueprint
     */
    public void update(double speedMultiplier, double powerMultiplier, boolean extraCondition, @Nullable ItemStack blueprint) {
        this.didProcess = false;
        this.needsSync = false;

        // Поиск или валидация рецепта
        if (currentRecipe == null || !matchesCurrentRecipe(currentRecipe)) {
            T prev = currentRecipe;
            currentRecipe = pickRecipeForTick();
            if (currentRecipe != null) {
                maxProgress = getRecipeDuration(currentRecipe);
                progress = 0.0;
                needsSync = true;
                onRecipeChanged(prev, currentRecipe);
            }
        }

        // Проверка blueprint pool
        if (currentRecipe != null && !isRecipeAllowedByBlueprint(currentRecipe, blueprint)) {
            this.didProcess = false;
            this.progress = 0.0;
            this.currentRecipe = null;
            this.needsSync = true;
            return;
        }

        // Обработка крафта
        if (extraCondition && currentRecipe != null && canProcess(currentRecipe)) {
            // ИЗМЕНЕНИЕ: Используем long для энергии
            long energyPerTick = (long) (getRecipeEnergyCost(currentRecipe) * powerMultiplier);

            long storedEnergy = energyStorage.getEnergyStored();

            // Ассемблер-логика ожидания энергии (общая для всех машин)
            if (requiresFullEnergyBufferToStart()
                    && !hasEnoughEnergyToStartCraft(progress, storedEnergy, energyPerTick, maxProgress)) {
                return;
            }
            if (!hasEnoughEnergyForTick(storedEnergy, energyPerTick)) {
                return;
            }

            // Потребляем энергию ПЕРЕД увеличением прогресса (как в оригинале)
            energyStorage.setEnergyStored(storedEnergy - energyPerTick);

            double step = Math.max(0.0, speedMultiplier);
            if (step <= 0.0) return;

            this.progress += step;
            this.didProcess = true;

            // В 1.7.10 overdrive мог "перескочить" несколько циклов за тик.
            // Безопасный кап итераций — защита от бесконечного while при некорректном maxProgress.
            if (maxProgress > 0 && progress >= maxProgress) {
                final int maxIterations = 64;
                int it = 0;
                while (progress >= maxProgress && it++ < maxIterations) {
                    processCraft(currentRecipe);
                    this.needsSync = true;

                    if (canProcess(currentRecipe)) {
                        progress -= maxProgress;
                    } else {
                        progress = 0.0;
                        currentRecipe = null;
                        break;
                    }
                }

                if (it >= maxIterations) {
                    progress = Math.min(progress, (double) maxProgress - 1.0);
                }
            }
        } else {
            if (progress > 0.0) {
                progress = 0.0;
                needsSync = true;
            }
        }
    }

    private static final class OutputPlacement {
        private OutputPlacement() {}

        private static java.util.List<ItemStack> nonEmpty(java.util.List<ItemStack> outs) {
            if (outs == null || outs.isEmpty()) return java.util.List.of();
            java.util.List<ItemStack> list = new java.util.ArrayList<>(outs.size());
            for (ItemStack o : outs) {
                if (o == null || o.isEmpty()) continue;
                list.add(o);
            }
            return list;
        }

        private static ItemStack[] snapshot(ModItemStackHandler handler, int[] slots) {
            ItemStack[] sim = new ItemStack[slots.length];
            for (int j = 0; j < slots.length; j++) {
                ItemStack cur = handler.getStackInSlot(slots[j]);
                sim[j] = cur.isEmpty() ? ItemStack.EMPTY : cur.copy();
            }
            return sim;
        }

        private static boolean canMerge(ItemStack slotStack, ItemStack incoming) {
            if (incoming == null || incoming.isEmpty()) return true;
            if (slotStack == null || slotStack.isEmpty()) return true;
            //? if < 1.21.1 {
            if (!ItemStack.isSameItemSameTags(slotStack, incoming)) return false;
            //?} else {
            /*if (!ItemStack.isSameItemSameComponents(slotStack, incoming)) return false;
            *///?}
            return (long) slotStack.getCount() + incoming.getCount() <= slotStack.getMaxStackSize();
        }

        private static boolean dfs(java.util.List<ItemStack> outs, int idx, ItemStack[] sim, int[] chosenSlotPerOutput) {
            if (idx >= outs.size()) return true;
            ItemStack inc = outs.get(idx);
            for (int j = 0; j < sim.length; j++) {
                ItemStack slotStack = sim[j];
                if (!canMerge(slotStack, inc)) continue;

                ItemStack prev = slotStack.isEmpty() ? ItemStack.EMPTY : slotStack.copy();
                if (slotStack.isEmpty()) {
                    sim[j] = inc.copy();
                } else {
                    ItemStack merged = slotStack.copy();
                    merged.grow(inc.getCount());
                    sim[j] = merged;
                }
                chosenSlotPerOutput[idx] = j;
                if (dfs(outs, idx + 1, sim, chosenSlotPerOutput)) return true;
                sim[j] = prev;
            }
            return false;
        }

        static boolean canFitAll(ModItemStackHandler handler, java.util.List<ItemStack> itemOutputs, int[] outputSlots) {
            java.util.List<ItemStack> outs = nonEmpty(itemOutputs);
            if (outs.isEmpty()) return true;
            ItemStack[] sim = snapshot(handler, outputSlots);
            int[] pick = new int[outs.size()];
            return dfs(outs, 0, sim, pick);
        }

        static void placeAll(ModItemStackHandler handler, java.util.List<ItemStack> itemOutputs, int[] outputSlots) {
            java.util.List<ItemStack> outs = nonEmpty(itemOutputs);
            if (outs.isEmpty()) return;

            ItemStack[] sim = snapshot(handler, outputSlots);
            int[] pick = new int[outs.size()];
            if (dfs(outs, 0, sim, pick)) {
                for (int i = 0; i < outs.size(); i++) {
                    int j = pick[i];
                    int slot = outputSlots[j];
                    ItemStack out = outs.get(i);
                    ItemStack cur = handler.getStackInSlot(slot);
                    if (cur.isEmpty()) {
                        handler.setStackInSlot(slot, out.copy());
                    } else {
                        cur.grow(out.getCount());
                    }
                }
                return;
            }

            // Fallback: deterministic positional placement if DFS can't solve.
            for (int i = 0; i < itemOutputs.size(); i++) {
                ItemStack output = itemOutputs.get(i);
                if (output == null || output.isEmpty()) continue;
                if (i >= outputSlots.length) break;
                int slot = outputSlots[i];
                ItemStack cur = handler.getStackInSlot(slot);
                if (cur.isEmpty()) {
                    handler.setStackInSlot(slot, output.copy());
                } else {
                    cur.grow(output.getCount());
                }
            }
        }
    }
}