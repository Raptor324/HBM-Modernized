package com.hbm_m.module.machine;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.recipe.AssemblerRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.index.ModRecipeIndex;

/**
 * РњРѕРґСѓР»СЊ РєСЂР°С„С‚Р° РґР»СЏ РїСЂРѕРґРІРёРЅСѓС‚РѕР№ СЃР±РѕСЂРѕС‡РЅРѕР№ РјР°С€РёРЅС‹.
 * Р РµР°Р»РёР·СѓРµС‚ Р»РѕРіРёРєСѓ РѕР±СЂР°Р±РѕС‚РєРё AssemblerRecipe.
 *
 * РћР‘РќРћР’Р›Р•РќРћ: РўРµРїРµСЂСЊ РёСЃРїРѕР»СЊР·СѓРµС‚ ILongEnergyStorage РґР»СЏ РїРѕРґРґРµСЂР¶РєРё Р±РѕР»СЊС€РёС… Р·РЅР°С‡РµРЅРёР№ СЌРЅРµСЂРіРёРё
 */
public class MachineModuleAdvancedAssembler extends MachineModuleBase<AssemblerRecipe> {

    // РР—РњР•РќР•РќРР•: РљРѕРЅСЃС‚СЂСѓРєС‚РѕСЂ С‚РµРїРµСЂСЊ РїСЂРёРЅРёРјР°РµС‚ ILongEnergyStorage
    public MachineModuleAdvancedAssembler(int moduleIndex, IEnergyReceiver energyStorage,
                                          ModItemStackHandler itemHandler, Level level) {
        super(moduleIndex, energyStorage, itemHandler, level);

        // РќР°СЃС‚СЂРѕР№РєР° РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ: 12 РІС…РѕРґРЅС‹С… (4-15), 1 РІС‹С…РѕРґРЅРѕР№ (16)
        this.inputSlots = new int[12];
        for (int i = 0; i < 12; i++) {
            this.inputSlots[i] = 4 + i;
        }
        this.outputSlots = new int[] { 16 };
    }

    // ========== BUILDER METHODS ==========

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

    // ========== Р Р•РђР›РР—РђР¦РРЇ РђР‘РЎРўР РђРљРўРќР«РҐ РњР•РўРћР”РћР’ ==========

    @Override
    protected AssemblerRecipe.Type getRecipeType() {
        return AssemblerRecipe.Type.INSTANCE;
    }

    @Override
    @Nullable
    public AssemblerRecipe findRecipeForInputs() {
        if (level == null) return null;

        // Р’ РѕС‚Р»РёС‡РёРµ РѕС‚ С…РёРјРјР°С€РёРЅС‹: Р·РґРµСЃСЊ Р°РІС‚Рѕ-РІС‹Р±РѕСЂ. Blueprint РїСЂРёРјРµРЅСЏРµС‚СЃСЏ РєР°Рє С„РёР»СЊС‚СЂ.
        ItemStack blueprint = itemHandler.getStackInSlot(1);
        for (AssemblerRecipe recipe : ModRecipeIndex.of(level.getRecipeManager()).getAll(getRecipeType())) {
            if (matchesRecipe(recipe) && isRecipeAllowedByBlueprint(recipe, blueprint)) return recipe;
        }
        return null;
    }

    /**
     * РџСЂРѕРІРµСЂСЏРµС‚, СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓРµС‚ Р»Рё РёРЅРІРµРЅС‚Р°СЂСЊ РґР°РЅРЅРѕРјСѓ СЂРµС†РµРїС‚Сѓ.
     */
    private boolean matchesRecipe(AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        // РЎРѕР·РґР°С‘Рј РєРѕРїРёСЋ РІС…РѕРґРЅС‹С… РїСЂРµРґРјРµС‚РѕРІ
        ItemStack[] inputCopy = new ItemStack[inputSlots.length];
        for (int i = 0; i < inputSlots.length; i++) {
            inputCopy[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
        }

        // РџСЂРѕРІРµСЂСЏРµРј, С‡С‚Рѕ РІСЃРµ РёРЅРіСЂРµРґРёРµРЅС‚С‹ РїСЂРёСЃСѓС‚СЃС‚РІСѓСЋС‚
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < inputCopy.length; i++) {
                if (!inputCopy[i].isEmpty() && ingredient.test(inputCopy[i])) {
                    inputCopy[i].shrink(1); // РЈР±РёСЂР°РµРј РѕРґРёРЅ РїСЂРµРґРјРµС‚
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
        setPreferredRecipeId(recipe != null ? recipe.getId() : null);
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

        // РџСЂРѕРІРµСЂСЏРµРј РІС…РѕРґРЅС‹Рµ РїСЂРµРґРјРµС‚С‹
        if (!matchesRecipe(recipe)) return false;

        // РџСЂРѕРІРµСЂСЏРµРј РІС‹С…РѕРґРЅРѕР№ СЃР»РѕС‚
        ItemStack outputSlot = itemHandler.getStackInSlot(outputSlots[0]);
        ItemStack result = recipe.getResultItem(level.registryAccess());

        if (outputSlot.isEmpty()) return true; // РЎР»РѕС‚ РїСѓСЃС‚ - РћРљ

        // РџСЂРѕРІРµСЂСЏРµРј СЃРѕРІРјРµСЃС‚РёРјРѕСЃС‚СЊ
        //? if < 1.21.1 {
        if (!PlatformHooks.isSameItemSameTags(outputSlot, result)) return false;
        //?} else {
        /*if (!ItemStack.isSameItemSameComponents(outputSlot, result)) return false;
        *///?}

        // РџСЂРѕРІРµСЂСЏРµРј, РїРѕРјРµСЃС‚РёС‚СЃСЏ Р»Рё СЂРµР·СѓР»СЊС‚Р°С‚
        return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize();
    }

    @Override
    protected void processCraft(AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        // Р—Р°Р±РёСЂР°РµРј РІС…РѕРґРЅС‹Рµ РїСЂРµРґРјРµС‚С‹
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

        // insertItem Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РѕР±СЉРµРґРёРЅРёС‚ СЃС‚Р°РєРё, РµСЃР»Рё РІРѕР·РјРѕР¶РЅРѕ
        itemHandler.insertItem(outputSlots[0], result, false);
    }

    @Override
    protected int getRecipeDuration(AssemblerRecipe recipe) {
        return recipe.getDuration();
    }

    // РР—РњР•РќР•РќРР•: Р’РѕР·РІСЂР°С‰Р°РµРј long РІРјРµСЃС‚Рѕ int
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

    /**
     * Р РµР°Р»РёР·Р°С†РёСЏ РїСЂРѕРІРµСЂРєРё blueprint pool
     * РСЃРїРѕР»СЊР·СѓРµС‚ AssemblerRecipeConfig РґР»СЏ РІР°Р»РёРґР°С†РёРё
     */
    @Override
    protected boolean isRecipeAllowedByBlueprint(AssemblerRecipe recipe, @Nullable ItemStack blueprint) {
        return isBlueprintAllowedForPool(recipe.getBlueprintPool(), blueprint);
    }
}
