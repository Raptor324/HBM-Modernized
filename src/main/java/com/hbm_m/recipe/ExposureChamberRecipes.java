package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Port of {@code com.hbm.inventory.recipes.ExposureChamberRecipes} (1.7.10 Original).
 * <p>
 * The "expensive mode" alternate recipe (degenerate-matter item instead of raw Schrabidium for
 * Dineutronium) is skipped - this port has no {@code GeneralConfig.enableExpensiveMode} toggle
 * or {@code item_expensive} equivalent, so only the always-available Schrabidium-ingot path is kept.
 */
public final class ExposureChamberRecipes {

    private ExposureChamberRecipes() {}

    public record Recipe(Item particle, Ingredient ingredient, ItemStack output) {}

    private static final List<Recipe> RECIPES = new ArrayList<>();

    private static void add(Item particle, Ingredient ingredient, ItemStack output) {
        RECIPES.add(new Recipe(particle, ingredient, output));
    }

    static {
        add(ModItems.PARTICLE_HIGGS.get(), Ingredient.of(ModItems.getIngot(ModIngots.URANIUM).get()),
                new ItemStack(ModItems.getIngot(ModIngots.SCHRARANIUM).get()));
        add(ModItems.PARTICLE_HIGGS.get(), Ingredient.of(ModItems.getIngot(ModIngots.URANIUM238).get()),
                new ItemStack(ModItems.getIngot(ModIngots.SCHRABIDIUM).get()));
        add(ModItems.PARTICLE_DARK.get(), Ingredient.of(ModItems.getIngot(ModIngots.PLUTONIUM).get()),
                new ItemStack(ModItems.getIngot(ModIngots.EUPHEMIUM).get()));
        add(ModItems.PARTICLE_SPARKTICLE.get(), Ingredient.of(ModItems.getIngot(ModIngots.SCHRABIDIUM).get()),
                new ItemStack(ModItems.getIngot(ModIngots.DINEUTRONIUM).get()));
    }

    @Nullable
    public static Recipe getRecipe(ItemStack particle, ItemStack ingredient) {
        if (particle.isEmpty() || ingredient.isEmpty()) return null;
        for (Recipe r : RECIPES) {
            if (particle.is(r.particle()) && r.ingredient().test(ingredient)) return r;
        }
        return null;
    }

    public static List<Recipe> getAll() {
        return List.copyOf(RECIPES);
    }
}
