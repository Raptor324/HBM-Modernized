package com.hbm_m.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AnvilRecipeManager {
    private static final List<AnvilRecipe> RECIPES = new ArrayList<>();

    public static void registerRecipes() {
        // Пример рецептов
        RECIPES.add(new AnvilRecipe(
                new ItemStack(Items.IRON_INGOT, 3),
                new ItemStack(Items.STICK, 2),
                new ItemStack(Items.IRON_PICKAXE),
                List.of(new ItemStack(Items.IRON_INGOT, 3), new ItemStack(Items.STICK, 2)),
                "Iron Pickaxe"
        ));

        RECIPES.add(new AnvilRecipe(
                new ItemStack(Items.DIAMOND, 2),
                new ItemStack(Items.STICK, 1),
                new ItemStack(Items.DIAMOND_SWORD),
                List.of(new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.STICK, 1)),
                "Diamond Sword"
        ));

        // Добавь свои рецепты здесь
    }

    public static List<AnvilRecipe> getAllRecipes() {
        return new ArrayList<>(RECIPES);
    }

    public static List<AnvilRecipe> searchRecipes(String query) {
        if (query == null || query.isEmpty()) {
            return getAllRecipes();
        }

        String lowerQuery = query.toLowerCase();
        return RECIPES.stream()
                .filter(recipe -> recipe.getRecipeName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    public static AnvilRecipe findRecipe(ItemStack slotA, ItemStack slotB) {
        for (AnvilRecipe recipe : RECIPES) {
            if (recipe.matches(slotA, slotB)) {
                return recipe;
            }
        }
        return null;
    }
}