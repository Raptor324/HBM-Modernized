package com.hbm_m.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hbm_m.lib.RefStrings;

public class AnvilRecipeManager {

    private static final List<AnvilRecipe> RECIPES = new ArrayList<>();
    private static int recipeCounter = 0;

    public static void registerRecipes() {
        RECIPES.add(new AnvilRecipe(
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "iron_pickaxe_" + recipeCounter++),
                new ItemStack(Items.IRON_INGOT, 3),
                new ItemStack(Items.STICK, 2),
                new ItemStack(Items.IRON_PICKAXE),
                List.of(new ItemStack(Items.IRON_INGOT, 3), new ItemStack(Items.STICK, 2))
        ));
        
        RECIPES.add(new AnvilRecipe(
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "diamond_sword_" + recipeCounter++),
                new ItemStack(Items.DIAMOND, 2),
                new ItemStack(Items.STICK, 1),
                new ItemStack(Items.DIAMOND_SWORD),
                List.of(new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.STICK, 1))
        ));
    }

    public static List<AnvilRecipe> getAllRecipes() {
        return new ArrayList<>(RECIPES);
    }

    // ДОБАВЛЕНО: метод поиска рецептов по имени
    public static List<AnvilRecipe> searchRecipes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllRecipes();
        }
        
        String lowerQuery = query.toLowerCase();
        return RECIPES.stream()
            .filter(recipe -> {
                ItemStack output = recipe.getResultItem(RegistryAccess.EMPTY);
                String itemName = output.getHoverName().getString().toLowerCase();
                return itemName.contains(lowerQuery);
            })
            .collect(Collectors.toList());
    }

    public static Optional<AnvilRecipe> findRecipe(Level level, ItemStack slotA, ItemStack slotB) {
        return level.getRecipeManager()
                .getAllRecipesFor(AnvilRecipe.Type.INSTANCE)
                .stream()
                .filter(recipe -> matchesRecipe(recipe, slotA, slotB))
                .findFirst();
    }

    private static boolean matchesRecipe(AnvilRecipe recipe, ItemStack slotA, ItemStack slotB) {
        return ItemStack.isSameItemSameTags(slotA, recipe.getInputA()) &&
               ItemStack.isSameItemSameTags(slotB, recipe.getInputB()) &&
               slotA.getCount() >= recipe.getInputA().getCount() &&
               slotB.getCount() >= recipe.getInputB().getCount();
    }
}
