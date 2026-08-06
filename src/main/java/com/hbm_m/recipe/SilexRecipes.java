package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Vereinfachter Java-Port von {@code SILEXRecipes} (1.7.10 Original, {@code com.hbm.inventory.recipes.SILEXRecipes}).
 * <p>
 * Vereinfachung gegenueber dem Original (siehe Aufgabenstellung / Report): die Laser-Wellenlaengen-Gating-Mechanik
 * ({@code EnumWavelengths}/{@code hasLaser}) und die separate Peroxid-Fluid-"Ladeleiste" (die im Original als
 * zweite, von Item-Input entkoppelte Fuellstandsanzeige diente) wurden nicht uebernommen. Stattdessen: fester
 * Fluidverbrauch aus dem Peroxid-Tank pro Item + feste Prozesszeit + gewichtete Zufallsausgabe, direkt am Item-Slot.
 */
public final class SilexRecipes {

    private SilexRecipes() {}

    public record WeightedOutput(ItemStack stack, int weight) {}

    public record SilexRecipe(Item input, int fluidConsumedMb, int processTicks, List<WeightedOutput> outputs) {
        public int totalWeight() {
            int total = 0;
            for (WeightedOutput o : outputs) total += o.weight();
            return total;
        }
    }

    private static final Map<Item, SilexRecipe> RECIPES = new HashMap<>();

    private static void register(Item input, int fluidConsumedMb, int processTicks, Object... weightedOutputs) {
        List<WeightedOutput> outputs = new ArrayList<>();
        for (int i = 0; i < weightedOutputs.length; i += 2) {
            outputs.add(new WeightedOutput((ItemStack) weightedOutputs[i], (Integer) weightedOutputs[i + 1]));
        }
        RECIPES.put(input, new SilexRecipe(input, fluidConsumedMb, processTicks, outputs));
    }

    static {
        register(ModItems.getIngot(ModIngots.URANIUM).get(), 100, 100,
                new ItemStack(ModItems.NUGGET_U235.get()), 1,
                new ItemStack(ModItems.NUGGET_U238.get()), 11);

        register(ModItems.getIngot(ModIngots.PU_MIX).get(), 100, 100,
                new ItemStack(ModItems.NUGGET_PU239.get()), 6,
                new ItemStack(ModItems.NUGGET_PU240.get()), 3);

        register(ModItems.getIngot(ModIngots.AM_MIX).get(), 100, 100,
                new ItemStack(ModItems.NUGGET_AM241.get()), 3,
                new ItemStack(ModItems.NUGGET_AM242.get()), 6);
    }

    public static SilexRecipe get(Item item) {
        return RECIPES.get(item);
    }

    public static boolean has(Item item) {
        return RECIPES.containsKey(item);
    }
}
