package com.hbm_m.item.industrial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.recipe.PlasmaForgeRecipe;
import com.hbm_m.recipe.PurexRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * {@code GenericRecipes.blueprintPools} des Originals: welche Rezepte (Montagemaschine, Chemiefabrik, Plasmaschmiede,
 * PUREX) zu welchem Blaupausen-Pool gehoeren. Im Port aus den geladenen Rezepten gesammelt.
 */
public final class BlueprintPools {

    public static final String POOL_PREFIX_ALT = "alt.";
    public static final String POOL_PREFIX_DISCOVER = "discover.";
    public static final String POOL_PREFIX_SECRET = "secret.";
    public static final String POOL_PREFIX_528 = "528.";

    private BlueprintPools() {}

    /** Pool -> Anzeigenamen der Rezepte (Ergebnisgegenstand). */
    public static Map<String, List<Component>> getPools(Level level) {
        Map<String, List<Component>> pools = new LinkedHashMap<>();
        var access = level.registryAccess();
        for (AssemblerRecipe r : RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE))
            add(pools, r.getBlueprintPool(), r.getResultItem(access).getHoverName());
        for (ChemicalPlantRecipe r : RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE))
            add(pools, r.getBlueprintPool(), r.getResultItem(access).getHoverName());
        for (PlasmaForgeRecipe r : RecipeHooks.getAllRecipes(level, PlasmaForgeRecipe.Type.INSTANCE))
            add(pools, r.getBlueprintPool(), r.getResultItem(access).getHoverName());
        for (PurexRecipe r : RecipeHooks.getAllRecipes(level, PurexRecipe.Type.INSTANCE))
            add(pools, r.getBlueprintPool(), r.getResultItem(access).getHoverName());
        return pools;
    }

    private static void add(Map<String, List<Component>> pools, String pool, Component name) {
        if (pool == null || pool.isEmpty()) return;
        pools.computeIfAbsent(pool, k -> new ArrayList<>()).add(name);
    }
}
