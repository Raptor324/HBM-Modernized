package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов экспозиционной камеры ({@code hbm_m:exposure_chamber}).
 *
 * <p>Порт рецептов 1:1 из статического {@code com.hbm_m.recipe.ExposureChamberRecipes}.
 * «Expensive mode»-альтернативы оригинала 1.7.10 намеренно не портированы
 * (нет конфиг-переключателя — как и в статической версии).</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class ExposureChamberRecipeGenerator {

    private ExposureChamberRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        expose(writer, "schraranium", ModItems.PARTICLE_HIGGS.get(),
                ModIngots.URANIUM, ModIngots.SCHRARANIUM);
        expose(writer, "schrabidium", ModItems.PARTICLE_HIGGS.get(),
                ModIngots.URANIUM238, ModIngots.SCHRABIDIUM);
        expose(writer, "euphemium", ModItems.PARTICLE_DARK.get(),
                ModIngots.PLUTONIUM, ModIngots.EUPHEMIUM);
        expose(writer, "dineutronium", ModItems.PARTICLE_SPARKTICLE.get(),
                ModIngots.SCHRABIDIUM, ModIngots.DINEUTRONIUM);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────────

    private static void expose(Consumer<FinishedRecipe> writer, String id,
                               net.minecraft.world.item.Item particle,
                               ModIngots ingredient, ModIngots output) {
        ExposureChamberRecipeBuilder.exposureChamberRecipe(
                        new ItemStack(particle),
                        Ingredient.of(ModItems.getIngot(ingredient).get()),
                        new ItemStack(ModItems.getIngot(output).get()))
                .save(writer, "exposure_chamber/" + id);
    }
}
//?}
