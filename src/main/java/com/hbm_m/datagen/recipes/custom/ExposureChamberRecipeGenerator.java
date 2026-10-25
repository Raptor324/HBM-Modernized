package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов экспозиционной камеры ({@code hbm_m:exposure_chamber}).
 *
 * <p>Порт рецептов 1:1 из статического {@code com.hbm_m.recipe.ExposureChamberRecipes}.
 * Expensive-Modus des Originals (Degenerierte Materie statt Schrabidat) ueber {@link ConfigRecipes}.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class ExposureChamberRecipeGenerator {

    private ExposureChamberRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        expose(writer, "schraranium", ModItems.PARTICLE_HIGGS.get(),
                ModMaterials.URANIUM, ModMaterials.SCHRARANIUM);
        expose(writer, "schrabidium", ModItems.PARTICLE_HIGGS.get(),
                ModMaterials.URANIUM238, ModMaterials.SCHRABIDIUM);
        expose(writer, "euphemium", ModItems.PARTICLE_DARK.get(),
                ModMaterials.PLUTONIUM, ModMaterials.EUPHEMIUM);
        // Original: enableExpensiveMode ? item_expensive DEGENERATE_MATTER : SBD.ingot() (Schrabidat)
        ConfigRecipes.variants(writer)
                .base(w -> expose(w, "dineutronium", ModItems.PARTICLE_SPARKTICLE.get(),
                        Ingredient.of(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.INGOT)), ModMaterials.DINEUTRONIUM))
                .variant(v -> expose(v, "dineutronium", ModItems.PARTICLE_SPARKTICLE.get(),
                        Ingredient.of(OreDictIngredients.item("hbm_m:item_expensive_degenerate_matter")), ModMaterials.DINEUTRONIUM), "expensive")
                .save();
        OreDictIngredients.checkMissing("ExposureChamberRecipeGenerator");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────────

    private static void expose(Consumer<FinishedRecipe> writer, String id,
                               net.minecraft.world.item.Item particle,
                               ModMaterials ingredient, ModMaterials output) {
        expose(writer, id, particle, Ingredient.of(ModMaterialItems.item(ingredient, MaterialShape.INGOT)), output);
    }

    private static void expose(Consumer<FinishedRecipe> writer, String id,
                               net.minecraft.world.item.Item particle,
                               Ingredient ingredient, ModMaterials output) {
        ExposureChamberRecipeBuilder.exposureChamberRecipe(
                        new ItemStack(particle),
                        ingredient,
                        new ItemStack(ModMaterialItems.item(output, MaterialShape.INGOT)))
                .save(writer, "exposure_chamber/" + id);
    }
}
//?}
