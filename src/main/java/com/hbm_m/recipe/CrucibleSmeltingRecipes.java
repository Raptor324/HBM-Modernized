package com.hbm_m.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for crucible smelting recipes.
 * Each entry maps a single input ItemStack to a list of output ItemStacks.
 *
 * Once AStack / RecipesCommon is ported, replace ItemStack key with AStack
 * so wildcard/ore-dict matching is supported.
 *
 * Legacy equivalent: CrucibleRecipes.getSmeltingRecipes()
 */
public class CrucibleSmeltingRecipes {

    // LinkedHashMap preserves insertion order (matches legacy recipeOrderedList behaviour)
    private static final Map<ItemStack, List<ItemStack>> RECIPES = new LinkedHashMap<>();

    /**
     * Register a smelting recipe.
     *
     * @param input   the item to smelt
     * @param outputs one or more output ItemStacks
     */
    public static void register(ItemStack input, List<ItemStack> outputs) {
        RECIPES.put(input.copy(), new ArrayList<>(outputs));
    }

    /** Convenience overload for a single output. */
    public static void register(ItemStack input, ItemStack output) {
        register(input, List.of(output));
    }

    /**
     * Returns an unmodifiable view of all smelting recipes as input → outputs pairs.
     */
    public static Map<ItemStack, List<ItemStack>> getRecipes() {
        return Collections.unmodifiableMap(RECIPES);
    }
}
