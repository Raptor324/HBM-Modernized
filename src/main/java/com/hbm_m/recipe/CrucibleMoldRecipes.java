package com.hbm_m.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple registry for crucible mold-casting recipes.
 * Each recipe is stored as a four-element array:
 *   [0] = input material (ingot / ore with material-id NBT once Mats is ported)
 *   [1] = mold item
 *   [2] = reserved / unused placeholder (was "basin" stack in legacy code)
 *   [3] = cast output
 *
 * Populate via {@link #register} once the Mats / ItemMold system is ported.
 */
public class CrucibleMoldRecipes {

    private static final List<ItemStack[]> RECIPES = new ArrayList<>();

    /**
     * Register a mold casting recipe.
     *
     * @param material the input material stack
     * @param mold     the mold item stack
     * @param output   the resulting cast item
     */
    public static void register(ItemStack material, ItemStack mold, ItemStack output) {
        RECIPES.add(new ItemStack[]{material.copy(), mold.copy(), ItemStack.EMPTY, output.copy()});
    }

    /**
     * Returns all registered mold recipes as [material, mold, placeholder, output] arrays.
     */
    public static List<ItemStack[]> getMoldRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }
}
