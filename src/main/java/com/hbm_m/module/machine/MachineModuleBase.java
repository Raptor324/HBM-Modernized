package com.hbm_m.module.machine;

import com.hbm_m.platform.PlatformHooks;

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
 * Р‘Р°Р·РѕРІС‹Р№ РјРѕРґСѓР»СЊ РјР°С€РёРЅС‹, РёРЅРєР°РїСЃСѓР»РёСЂСѓСЋС‰РёР№ Р»РѕРіРёРєСѓ РєСЂР°С„С‚Р°.
 * Р’РґРѕС…РЅРѕРІР»С‘РЅ РѕСЂРёРіРёРЅР°Р»СЊРЅС‹Рј ModuleMachineBase РёР· HBM 1.7.10.
 *
 * РђРґР°РїС‚РёСЂРѕРІР°РЅ РґР»СЏ 1.20.1:
 * - РСЃРїРѕР»СЊР·СѓРµС‚ RecipeManager РІРјРµСЃС‚Рѕ GenericRecipes
 * - Р Р°Р±РѕС‚Р°РµС‚ СЃ IItemHandler РІРјРµСЃС‚Рѕ РјР°СЃСЃРёРІР° СЃР»РѕС‚РѕРІ
 * - РћР‘РќРћР’Р›Р•РќРћ: РСЃРїРѕР»СЊР·СѓРµС‚ ILongEnergyStorage РґР»СЏ РїРѕРґРґРµСЂР¶РєРё Р±РѕР»СЊС€РёС… Р·РЅР°С‡РµРЅРёР№ СЌРЅРµСЂРіРёРё
 */
public abstract class MachineModuleBase<T extends Recipe<?>> {
    // === CONFIGURATION ===
    protected final int moduleIndex;
    // РР—РњР•РќР•РќРР•: РўРµРїРµСЂСЊ РёСЃРїРѕР»СЊР·СѓРµРј ILongEnergyStorage РІРјРµСЃС‚Рѕ IEnergyStorage
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

    // РР—РњР•РќР•РќРР•: РљРѕРЅСЃС‚СЂСѓРєС‚РѕСЂ С‚РµРїРµСЂСЊ РїСЂРёРЅРёРјР°РµС‚ ILongEnergyStorage
    public MachineModuleBase(int moduleIndex, IEnergyReceiver energyStorage, ModItemStackHandler itemHandler, Level level) {
        this.moduleIndex = moduleIndex;
        this.energyStorage = energyStorage;
        this.itemHandler = itemHandler;
        this.level = level;
    }

    /** BlockEntity РјРѕР¶РµС‚ СЃРѕР·РґР°С‚СЊ РјРѕРґСѓР»СЊ РґРѕ СѓСЃС‚Р°РЅРѕРІРєРё level вЂ” РѕР±РЅРѕРІР»СЏРµРј СЃСЃС‹Р»РєСѓ РїСЂРё РєР°Р¶РґРѕРј С‚РёРєРµ. */
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

    // РР—РњР•РќР•РќРР•: Р’РѕР·РІСЂР°С‰Р°РµРј long РІРјРµСЃС‚Рѕ int
    protected abstract long getRecipeEnergyCost(T recipe);

    @Nullable
    protected abstract T findRecipeForItem(ItemStack stack);

    /**
     * Optional hook: РІС‹Р·С‹РІР°РµС‚СЃСЏ РїСЂРё СЃРјРµРЅРµ СЂРµС†РµРїС‚Р° (ID РёР»Рё auto-РІС‹Р±РѕСЂ).
     * РќР°РїСЂРёРјРµСЂ, РјР°С€РёРЅС‹ СЃ Р¶РёРґРєРѕСЃС‚РЅС‹РјРё Р±Р°РєР°РјРё РјРѕРіСѓС‚ СЃРєРѕРЅС„РёРіСѓСЂРёСЂРѕРІР°С‚СЊ С‚РёРїС‹ Р±Р°РєРѕРІ.
     */
    protected void onRecipeChanged(@Nullable T previous, @Nullable T current) {
        // no-op by default
    }

    /**
     * РҐСѓРє СЃРµРјР°РЅС‚РёРєРё СЌРЅРµСЂРіРѕРіРµР№С‚Р°:
     * - true (default): РєР°Рє Сѓ Р°СЃСЃРµРјР±Р»РµСЂР° вЂ” РїСЂРё СЃС‚Р°СЂС‚Рµ Р¶РґС‘Рј СЌРЅРµСЂРіРёСЋ РЅР° РІРµСЃСЊ С†РёРєР».
     * - false: РєР°Рє Сѓ С…РёРјРјР°С€РёРЅС‹ РІ 1.7.10 вЂ” РґРѕСЃС‚Р°С‚РѕС‡РЅРѕ СЌРЅРµСЂРіРёРё С‚РѕР»СЊРєРѕ РЅР° С‚РµРєСѓС‰РёР№ С‚РёРє.
     */
    protected boolean requiresFullEnergyBufferToStart() {
        return true;
    }

    /**
     * Р¦РµРЅС‚СЂР°Р»СЊРЅР°СЏ РїСЂРѕРІРµСЂРєР° blueprint pool.
     */
    protected static boolean isBlueprintPoolAllowed(@Nullable String recipePool, ItemStack blueprint) {
        if (recipePool == null || recipePool.isEmpty()) return true;
        String installed = ItemBlueprintFolder.getBlueprintPool(blueprint);
        return installed != null && !installed.isEmpty() && installed.equals(recipePool);
    }

    /** РЈС‚РёР»РёС‚Р° РґР»СЏ СЂРµС†РµРїС‚РѕРІ, РєРѕС‚РѕСЂС‹Рµ РёРјРµСЋС‚ РїРѕР»Рµ blueprintPool (РєР°Рє РІ 1.7.10 pooled recipes). */
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
     * Р­РЅРµСЂРіРµС‚РёС‡РµСЃРєРёР№ РіРµР№С‚ "РєР°Рє Сѓ Р°СЃСЃРµРјР±Р»РµСЂР°":
     * - РµСЃР»Рё РєСЂР°С„С‚ С‚РѕР»СЊРєРѕ РЅР°С‡РёРЅР°РµС‚СЃСЏ, РјР°С€РёРЅР° Р¶РґС‘С‚ РїРѕРєР° РЅР°РєРѕРїРёС‚СЃСЏ СЌРЅРµСЂРіРёСЏ РЅР° Р’Р•РЎР¬ С†РёРєР»;
     * - РµСЃР»Рё РєСЂР°С„С‚ СѓР¶Рµ РёРґС‘С‚, Рё РЅР° С‚РµРєСѓС‰РёР№ С‚РёРє СЌРЅРµСЂРіРёРё РЅРµ С…РІР°С‚Р°РµС‚ вЂ” РїСЂРѕРіСЂРµСЃСЃ РЅРµ СЃР±СЂР°СЃС‹РІР°РµРј, РїСЂРѕСЃС‚Рѕ Р¶РґС‘Рј.
     *
     * Р’С‹РЅРµСЃРµРЅРѕ РІ Р±Р°Р·РѕРІС‹Р№ РјРѕРґСѓР»СЊ, С‡С‚РѕР±С‹ СЂР°Р·РЅС‹Рµ РјР°С€РёРЅС‹ РјРѕРіР»Рё РїРµСЂРµРёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ РѕРґРЅСѓ Р»РѕРіРёРєСѓ.
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
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ СЃРїРёСЃРѕРє РїСЂРёР·СЂР°С‡РЅС‹С… РїСЂРµРґРјРµС‚РѕРІ РґР»СЏ GUI
     * РџРµСЂРµРѕРїСЂРµРґРµР»РёС‚Рµ РІ РґРѕС‡РµСЂРЅРёС… РєР»Р°СЃСЃР°С… РґР»СЏ СЃРїРµС†РёС„РёС‡РµСЃРєРѕР№ Р»РѕРіРёРєРё
     */
    public NonNullList<ItemStack> getGhostItems() {
        return NonNullList.create();
    }

    /**
     * РЎР±СЂР°СЃС‹РІР°РµС‚ РїСЂРѕРіСЂРµСЃСЃ РєСЂР°С„С‚Р° Рё С‚РµРєСѓС‰РёР№ СЂРµС†РµРїС‚.
     * РСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ РїСЂРё РїСЂРёРЅСѓРґРёС‚РµР»СЊРЅРѕР№ СЃРјРµРЅРµ СЂРµС†РµРїС‚Р° С‡РµСЂРµР· GUI.
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
    /** РЎРѕРІРјРµСЃС‚РёРјРѕСЃС‚СЊ СЃРѕ СЃС‚Р°СЂС‹Рј API РјРѕРґСѓР»РµР№/BE. */
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
     * РћР±РЅРѕРІР»РµРЅРёРµ СЃ РїРѕРґРґРµСЂР¶РєРѕР№ blueprint
     */
    public void update(double speedMultiplier, double powerMultiplier, boolean extraCondition, @Nullable ItemStack blueprint) {
        this.didProcess = false;
        this.needsSync = false;

        // РџРѕРёСЃРє РёР»Рё РІР°Р»РёРґР°С†РёСЏ СЂРµС†РµРїС‚Р°
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

        if (currentRecipe != null && !isRecipeAllowedByBlueprint(currentRecipe, blueprint)) {
            this.didProcess = false;
            this.progress = 0.0;
            this.currentRecipe = null;
            this.needsSync = true;
            return;
        }

        if (extraCondition && currentRecipe != null && canProcess(currentRecipe)) {
            long energyPerTick = (long) (getRecipeEnergyCost(currentRecipe) * powerMultiplier);

            long storedEnergy = energyStorage.getEnergyStored();

            if (requiresFullEnergyBufferToStart()
                    && !hasEnoughEnergyToStartCraft(progress, storedEnergy, energyPerTick, maxProgress)) {
                return;
            }
            if (!hasEnoughEnergyForTick(storedEnergy, energyPerTick)) {
                return;
            }

            energyStorage.setEnergyStored(storedEnergy - energyPerTick);

            double step = Math.max(0.0, speedMultiplier);
            if (step <= 0.0) return;

            this.progress += step;
            this.didProcess = true;

            // Р’ 1.7.10 overdrive РјРѕРі "РїРµСЂРµСЃРєРѕС‡РёС‚СЊ" РЅРµСЃРєРѕР»СЊРєРѕ С†РёРєР»РѕРІ Р·Р° С‚РёРє.
            // Р‘РµР·РѕРїР°СЃРЅС‹Р№ РєР°Рї РёС‚РµСЂР°С†РёР№ вЂ” Р·Р°С‰РёС‚Р° РѕС‚ Р±РµСЃРєРѕРЅРµС‡РЅРѕРіРѕ while РїСЂРё РЅРµРєРѕСЂСЂРµРєС‚РЅРѕРј maxProgress.
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
            if (!PlatformHooks.isSameItemSameTags(slotStack, incoming)) return false;
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
