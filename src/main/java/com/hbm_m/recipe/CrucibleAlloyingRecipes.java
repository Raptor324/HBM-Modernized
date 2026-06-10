package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry for crucible alloying recipes.
 * Populate via {@link #register} once the MaterialStack / Mats system is ported.
 *
 * Legacy equivalent: CrucibleRecipes.INSTANCE.recipeOrderedList
 */
public class CrucibleAlloyingRecipes {

    private static final List<CrucibleAlloyingRecipe> RECIPES = new ArrayList<>();

    public static void register(CrucibleAlloyingRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static List<CrucibleAlloyingRecipe> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }
}
