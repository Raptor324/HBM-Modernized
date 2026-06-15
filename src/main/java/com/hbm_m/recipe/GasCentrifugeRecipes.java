package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Static recipe table for the Gas Centrifuge.
 * <p>
 * Mirrors the legacy 1.7.10 "Centristick" enrichment mechanic: a hexafluoride gas
 * cell (UF6 / PUF6) is enriched into a usable nuclear material plus an empty cell,
 * consuming durability from a Centristick catalyst each completed cycle.
 * <p>
 * Uses a lazy static-initializer so no edits to {@code MainRegistry} are required.
 */
public final class GasCentrifugeRecipes {

    /** A single enrichment recipe entry. */
    public record Recipe(
            Ingredient input,
            ItemStack enrichedOutput,
            ItemStack emptyCellOutput,
            int centristickDamagePerCycle,
            int processDuration,
            long energyPerTick) {

        public boolean matches(ItemStack inputStack) {
            return !inputStack.isEmpty() && input.test(inputStack);
        }
    }

    private static final List<Recipe> RECIPES = new ArrayList<>();
    private static boolean initialized = false;

    private GasCentrifugeRecipes() {
    }

    /** Lazily populates the recipe list on first access. */
    private static void ensureRecipes() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Uranium hexafluoride -> enriched Uranium-235 nugget + empty cell.
        addRecipe(
                ModItems.CELL_UF6.get(),
                ModItems.NUGGET_U235.get(),
                ModItems.CELL_EMPTY.get(),
                50,   // centristick durability cost per cycle
                200,  // processing duration in ticks
                40L   // energy consumed per tick
        );

        // Plutonium hexafluoride -> enriched Plutonium-239 nugget + empty cell.
        addRecipe(
                ModItems.CELL_PUF6.get(),
                ModItems.NUGGET_PU239.get(),
                ModItems.CELL_EMPTY.get(),
                75,   // centristick durability cost per cycle
                260,  // processing duration in ticks
                50L   // energy consumed per tick
        );
    }

    private static void addRecipe(Item input, Item enrichedOutput, Item emptyCell,
                                   int centristickDamagePerCycle, int processDuration, long energyPerTick) {
        if (input == null || enrichedOutput == null || emptyCell == null) {
            return;
        }
        RECIPES.add(new Recipe(
                Ingredient.of(input),
                new ItemStack(enrichedOutput),
                new ItemStack(emptyCell),
                centristickDamagePerCycle,
                processDuration,
                energyPerTick));
    }

    /** Returns the matching recipe for the given input cell, or {@code null} if none matches. */
    public static Recipe getRecipe(ItemStack inputStack) {
        ensureRecipes();
        if (inputStack == null || inputStack.isEmpty()) {
            return null;
        }
        for (Recipe recipe : RECIPES) {
            if (recipe.matches(inputStack)) {
                return recipe;
            }
        }
        return null;
    }

    /** Returns true if the given stack is a valid centrifuge input cell. */
    public static boolean isValidInput(ItemStack stack) {
        return getRecipe(stack) != null;
    }

    /** Returns an immutable view of all registered recipes (for JEI etc). */
    public static List<Recipe> getRecipes() {
        ensureRecipes();
        return List.copyOf(RECIPES);
    }
}
