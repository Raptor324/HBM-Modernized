package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов реактора-размножителя ({@code hbm_m:breeder}).
 *
 * <p>Порт рецептов 1:1 из статического {@code com.hbm_m.recipe.BreederRecipes#registerRecipes()}
 * (включая материальные подстановки breeding-rod → слитки, описанные в его javadoc).
 * Непортируемые TODO-рецепты оригинала (литий → тритий, meteorite sword easter-egg) также
 * намеренно пропущены — как и в статической версии.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class BreederRecipeGenerator {

    private BreederRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        breed(writer, "cobalt_to_co60",            ModIngots.COBALT,       ModIngots.CO60,          100);
        breed(writer, "radium_to_actinium",        ModIngots.RA226,        ModIngots.ACTINIUM,      300);
        breed(writer, "thorium232_to_thorium",     ModIngots.THORIUM232,   ModIngots.THORIUM,       500);
        breed(writer, "uranium235_to_neptunium",   ModIngots.URANIUM235,   ModIngots.NEPTUNIUM,     300);
        breed(writer, "neptunium_to_plutonium238", ModIngots.NEPTUNIUM,    ModIngots.PLUTONIUM238,  200);
        breed(writer, "plutonium238_to_239",       ModIngots.PLUTONIUM238, ModIngots.PLUTONIUM239, 1000);
        breed(writer, "uranium238_to_pu_mix",      ModIngots.URANIUM238,   ModIngots.PU_MIX,        300);
        breed(writer, "uranium_to_pu_mix",         ModIngots.URANIUM,      ModIngots.PU_MIX,        200);

        // PU_MIX -> NUCLEAR_WASTE (выход — обычный предмет, не слиток).
        BreederRecipeBuilder.breederRecipe(
                        Ingredient.of(ModItems.getIngot(ModIngots.PU_MIX).get()),
                        new ItemStack(ModItems.NUCLEAR_WASTE.get()),
                        200)
                .save(writer, "breeder/pu_mix_to_nuclear_waste");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────────

    private static void breed(Consumer<FinishedRecipe> writer, String id,
                              ModIngots input, ModIngots output, int energyPerTick) {
        BreederRecipeBuilder.breederRecipe(
                        Ingredient.of(ModItems.getIngot(input).get()),
                        new ItemStack(ModItems.getIngot(output).get()),
                        energyPerTick)
                .save(writer, "breeder/" + id);
    }
}
//?}
