package com.hbm_m.recipe;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.platform.recipe.RecipeHooks;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * {@code AnvilRecipes.getSmithing()} / {@code getConstruction()}: beide Listen in Original-Reihenfolge ({@code sort}).
 */
public final class AnvilRecipeManager {

    private AnvilRecipeManager() { }

    //? if < 1.21.1 {
    private static final Comparator<AnvilRecipe> ORDER = Comparator.comparingInt(AnvilRecipe::getSort)
            .thenComparing(r -> r.getId().toString());
    //?} else {
    /*// 1.21.1: die Rezept-ID steckt nur im RecipeHolder - gleiche Ordnung (sort, dann ID)
    private static List<AnvilRecipe> ordered(Level level, java.util.function.Predicate<AnvilRecipe> filter) {
        return RecipeHooks.getAllRecipesById(level, AnvilRecipe.Type.INSTANCE).entrySet().stream()
                .filter(e -> filter.test(e.getValue()))
                .sorted(Comparator.<java.util.Map.Entry<ResourceLocation, AnvilRecipe>>comparingInt(e -> e.getValue().getSort())
                        .thenComparing(e -> e.getKey().toString()))
                .map(java.util.Map.Entry::getValue)
                .toList();
    }
    *///?}

    public static List<AnvilRecipe> getAllRecipes(Level level) {
        return RecipeHooks.getAllRecipes(level, AnvilRecipe.Type.INSTANCE);
    }

    public static List<AnvilRecipe> getSmithing(Level level) {
        //? if < 1.21.1 {
        return getAllRecipes(level).stream().filter(AnvilRecipe::isSmithing).sorted(ORDER).toList();
        //?} else {
        /*return ordered(level, AnvilRecipe::isSmithing);
        *///?}
    }

    public static List<AnvilRecipe> getConstruction(Level level) {
        //? if < 1.21.1 {
        return getAllRecipes(level).stream().filter(AnvilRecipe::isConstruction).sorted(ORDER).toList();
        //?} else {
        /*return ordered(level, AnvilRecipe::isConstruction);
        *///?}
    }

    //? if fabric {
    /*@Environment(EnvType.CLIENT)
    *///?}
    //? if forge {
    @OnlyIn(Dist.CLIENT)
    //?}
    public static List<AnvilRecipe> getClientRecipes() {
        Level level = com.hbm_m.client.ClientAccess.level();
        return level != null ? getAllRecipes(level) : Collections.emptyList();
    }

    /** {@code ContainerAnvil.updateSmithing}: erste passende Schmiede-Regel, deren Stufe reicht. */
    public static Optional<AnvilRecipe> findSmithing(Level level, ItemStack left, ItemStack right, AnvilTier tier) {
        for (AnvilRecipe rec : getSmithing(level)) {
            if (rec.matchesSmithing(left, right) && rec.canCraftOn(tier)) return Optional.of(rec);
        }
        return Optional.empty();
    }

    /** {@code ContainerAnvil}-Verbrauch: erste passende Schmiede-Regel ohne Stufenpruefung ({@code matchesInt}). */
    public static Optional<AnvilRecipe> findSmithingAnyTier(Level level, ItemStack left, ItemStack right) {
        for (AnvilRecipe rec : getSmithing(level)) {
            if (rec.matchesSmithing(left, right)) return Optional.of(rec);
        }
        return Optional.empty();
    }

    public static Optional<AnvilRecipe> getRecipe(Level level, ResourceLocation id) {
        return Optional.ofNullable(RecipeHooks.getAllRecipesById(level, AnvilRecipe.Type.INSTANCE).get(id));
    }
}
