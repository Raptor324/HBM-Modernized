package com.hbm_m.satellite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code SpaceAssemblerRecipes}: was die 0G-Fabrik des Weltraumlabors aus einer per Rakete nachgelieferten
 * Orbital-Baugruppe baut. Eine Zutat, bis zu neun Ausgaben, kein Strom.
 */
public final class SpaceAssemblerRecipes {

    private SpaceAssemblerRecipes() {}

    public record Recipe(String name, int duration, Supplier<? extends Item> input, Supplier<List<ItemStack>> outputs) { }

    private static final int MINUTE = 60 * 20;

    private static final Map<String, Recipe> RECIPES = new LinkedHashMap<>();

    static {
        register(new Recipe("space.crystalcircuit", 3 * MINUTE, ModItems.ORBITAL_ASSEMBLY_CRYSTAL_CIRCUIT,
                () -> List.of(new ItemStack(ModItems.CIRCUIT_CRYSTAL.get()))));
    }

    private static void register(Recipe recipe) {
        RECIPES.put(recipe.name(), recipe);
    }

    public static java.util.Collection<Recipe> all() {
        return RECIPES.values();
    }

    @Nullable
    public static Recipe byName(String name) {
        return RECIPES.get(name);
    }

    @Nullable
    public static Recipe getRecipe(ItemStack stack) {
        for (Recipe r : RECIPES.values()) if (stack.is(r.input().get())) return r;
        return null;
    }
}
