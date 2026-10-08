package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.nuclear.BreedingRodType;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

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
        breed(writer, "cobalt_to_co60",            ModMaterials.COBALT,       ModMaterials.CO60,          100);
        breed(writer, "radium_to_actinium",        ModMaterials.RA226,        ModMaterials.ACTINIUM,      300);
        breed(writer, "thorium232_to_thorium_fuel", ModMaterials.THORIUM232,  ModMaterials.THORIUM_FUEL,  500); // Original TH232 -> THF
        breed(writer, "uranium235_to_neptunium",   ModMaterials.URANIUM235,   ModMaterials.NEPTUNIUM,     300);
        breed(writer, "neptunium_to_plutonium238", ModMaterials.NEPTUNIUM,    ModMaterials.PLUTONIUM238,  200);
        breed(writer, "plutonium238_to_239",       ModMaterials.PLUTONIUM238, ModMaterials.PLUTONIUM239, 1000);
        breed(writer, "uranium238_to_pu_mix",      ModMaterials.URANIUM238,   ModMaterials.PU_MIX,        300);
        breed(writer, "uranium_to_pu_mix",         ModMaterials.URANIUM,      ModMaterials.PU_MIX,        200);

        // PU_MIX -> NUCLEAR_WASTE (выход — обычный предмет, не слиток).
        BreederRecipeBuilder.breederRecipe(
                        Ingredient.of(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.INGOT)),
                        new ItemStack(ModItems.NUCLEAR_WASTE.get()),
                        200)
                .save(writer, "breeder/pu_mix_to_nuclear_waste");

        // Original BreederRecipes.registerDefaults: Brutstaebe (Einzel = flux, Doppel = flux * 2, Vierfach = flux * 3)
        rods(writer, BreedingRodType.LITHIUM, BreedingRodType.TRITIUM, 200);
        rods(writer, BreedingRodType.CO, BreedingRodType.CO60, 100);
        rods(writer, BreedingRodType.RA226, BreedingRodType.AC227, 300);
        rods(writer, BreedingRodType.TH232, BreedingRodType.THF, 500);
        rods(writer, BreedingRodType.U235, BreedingRodType.NP237, 300);
        rods(writer, BreedingRodType.NP237, BreedingRodType.PU238, 200);
        rods(writer, BreedingRodType.PU238, BreedingRodType.PU239, 1000);
        rods(writer, BreedingRodType.U238, BreedingRodType.RGP, 300);
        rods(writer, BreedingRodType.URANIUM, BreedingRodType.RGP, 200);
        rods(writer, BreedingRodType.RGP, BreedingRodType.WASTE, 200);
    }

    /** Original {@code BreederRecipes.setRecipe}. */
    private static void rods(Consumer<FinishedRecipe> writer, BreedingRodType in, BreedingRodType out, int flux) {
        BreederRecipeBuilder.breederRecipe(Ingredient.of(ModItems.ROD.get(in).get()), new ItemStack(ModItems.ROD.get(out).get()), flux)
                .save(writer, "breeder/rod_" + in.id());
        BreederRecipeBuilder.breederRecipe(Ingredient.of(ModItems.ROD_DUAL.get(in).get()), new ItemStack(ModItems.ROD_DUAL.get(out).get()), flux * 2)
                .save(writer, "breeder/rod_dual_" + in.id());
        BreederRecipeBuilder.breederRecipe(Ingredient.of(ModItems.ROD_QUAD.get(in).get()), new ItemStack(ModItems.ROD_QUAD.get(out).get()), flux * 3)
                .save(writer, "breeder/rod_quad_" + in.id());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────────

    private static void breed(Consumer<FinishedRecipe> writer, String id,
                              ModMaterials input, ModMaterials output, int energyPerTick) {
        BreederRecipeBuilder.breederRecipe(
                        Ingredient.of(ModMaterialItems.item(input, MaterialShape.INGOT)),
                        new ItemStack(ModMaterialItems.item(output, MaterialShape.INGOT)),
                        energyPerTick)
                .save(writer, "breeder/" + id);
    }
}
//?}
