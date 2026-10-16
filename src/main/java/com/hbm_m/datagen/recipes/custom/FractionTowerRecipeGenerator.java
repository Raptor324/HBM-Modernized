package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов фракционной башни ({@code hbm_m:fraction_tower}).
 *
 * <p>Порт 19 рецептов из удалённого статического {@code FractionTowerRecipes} (static-блок,
 * Direktport 1.7.10 {@code FractionRecipes}). Жидкостные стаки создаются через
 * {@link FluidStack#create} из {@link ModFluids} (mB). Чистый ванильный 1.20.1 код внутри
 * {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class FractionTowerRecipeGenerator {

    private FractionTowerRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.HEAVYOIL, 100),
                fluid(ModFluids.BITUMEN, 30),
                fluid(ModFluids.SMEAR, 70)
        ).save(writer, "fraction_tower/heavyoil");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.HEAVYOIL_VACUUM, 100),
                fluid(ModFluids.SMEAR, 40),
                fluid(ModFluids.HEATINGOIL_VACUUM, 60)
        ).save(writer, "fraction_tower/heavyoil_vacuum");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.SMEAR, 100),
                fluid(ModFluids.HEATINGOIL, 60),
                fluid(ModFluids.LUBRICANT, 40)
        ).save(writer, "fraction_tower/smear");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.NAPHTHA, 100),
                fluid(ModFluids.HEATINGOIL, 40),
                fluid(ModFluids.DIESEL, 60)
        ).save(writer, "fraction_tower/naphtha");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.NAPHTHA_DS, 100),
                fluid(ModFluids.XYLENE, 60),
                fluid(ModFluids.DIESEL_REFORM, 40)
        ).save(writer, "fraction_tower/naphtha_ds");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.NAPHTHA_CRACK, 100),
                fluid(ModFluids.HEATINGOIL, 30),
                fluid(ModFluids.DIESEL_CRACK, 70)
        ).save(writer, "fraction_tower/naphtha_crack");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.LIGHTOIL, 100),
                fluid(ModFluids.DIESEL, 40),
                fluid(ModFluids.KEROSENE, 60)
        ).save(writer, "fraction_tower/lightoil");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.LIGHTOIL_DS, 100),
                fluid(ModFluids.DIESEL_REFORM, 60),
                fluid(ModFluids.KEROSENE_REFORM, 40)
        ).save(writer, "fraction_tower/lightoil_ds");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.LIGHTOIL_CRACK, 100),
                fluid(ModFluids.KEROSENE, 70),
                fluid(ModFluids.PETROLEUM, 30)
        ).save(writer, "fraction_tower/lightoil_crack");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.COALOIL, 100),
                fluid(ModFluids.COALGAS, 30),
                fluid(ModFluids.OIL_BASE, 70)
        ).save(writer, "fraction_tower/coaloil");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.COALCREOSOTE, 100),
                fluid(ModFluids.COALOIL, 10),
                fluid(ModFluids.BITUMEN, 90)
        ).save(writer, "fraction_tower/coalcreosote");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.REFORMATE, 100),
                fluid(ModFluids.AROMATICS, 40),
                fluid(ModFluids.XYLENE, 60)
        ).save(writer, "fraction_tower/reformate");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.LIGHTOIL_VACUUM, 100),
                fluid(ModFluids.KEROSENE, 70),
                fluid(ModFluids.REFORMGAS, 30)
        ).save(writer, "fraction_tower/lightoil_vacuum");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.EGG, 100),
                fluid(ModFluids.CHOLESTEROL, 50),
                fluid(ModFluids.RADIOSOLVENT, 50)
        ).save(writer, "fraction_tower/egg");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.OIL_COKER, 100),
                fluid(ModFluids.CRACKOIL, 30),
                fluid(ModFluids.HEATINGOIL, 70)
        ).save(writer, "fraction_tower/oil_coker");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.NAPHTHA_COKER, 100),
                fluid(ModFluids.NAPHTHA_CRACK, 75),
                fluid(ModFluids.LIGHTOIL_CRACK, 25)
        ).save(writer, "fraction_tower/naphtha_coker");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.GAS_COKER, 100),
                fluid(ModFluids.AROMATICS, 25),
                fluid(ModFluids.CARBONDIOXIDE, 75)
        ).save(writer, "fraction_tower/gas_coker");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.CHLOROCALCITE_MIX, 100),
                fluid(ModFluids.CHLOROCALCITE_CLEANED, 50),
                fluid(ModFluids.COLLOID, 50)
        ).save(writer, "fraction_tower/chlorocalcite_mix");

        FractionTowerRecipeBuilder.fractionTowerRecipe(
                fluid(ModFluids.BAUXITE_SOLUTION, 100),
                fluid(ModFluids.REDMUD, 50),
                fluid(ModFluids.SODIUM_ALUMINATE, 50)
        ).save(writer, "fraction_tower/bauxite_solution");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
