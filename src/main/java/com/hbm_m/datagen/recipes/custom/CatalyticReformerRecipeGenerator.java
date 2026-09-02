package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов каталитического риформера ({@code hbm_m:catalytic_reformer}).
 *
 * <p>Порт 9 рецептов из удалённого статического {@code CatalyticReformerRecipes} (static-блок,
 * Direktport 1.7.10 {@code ReformingRecipes}). Жидкостные стаки создаются через
 * {@link FluidStack#create} из {@link ModFluids} (mB). Чистый ванильный 1.20.1 код внутри
 * {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class CatalyticReformerRecipeGenerator {

    private CatalyticReformerRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.HEATINGOIL, 100),
                fluid(ModFluids.NAPHTHA, 50),
                fluid(ModFluids.PETROLEUM, 15),
                fluid(ModFluids.HYDROGEN, 10)
        ).save(writer, "catalytic_reformer/heatingoil");

        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.NAPHTHA, 100),
                fluid(ModFluids.REFORMATE, 50),
                fluid(ModFluids.PETROLEUM, 15),
                fluid(ModFluids.HYDROGEN, 10)
        ).save(writer, "catalytic_reformer/naphtha");

        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.NAPHTHA_CRACK, 100),
                fluid(ModFluids.REFORMATE, 50),
                fluid(ModFluids.AROMATICS, 10),
                fluid(ModFluids.HYDROGEN, 5)
        ).save(writer, "catalytic_reformer/naphtha_crack");

        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.NAPHTHA_COKER, 100),
                fluid(ModFluids.REFORMATE, 50),
                fluid(ModFluids.REFORMGAS, 10),
                fluid(ModFluids.HYDROGEN, 5)
        ).save(writer, "catalytic_reformer/naphtha_coker");

        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.LIGHTOIL, 100),
                fluid(ModFluids.AROMATICS, 50),
                fluid(ModFluids.REFORMGAS, 10),
                fluid(ModFluids.HYDROGEN, 15)
        ).save(writer, "catalytic_reformer/lightoil");

        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.LIGHTOIL_CRACK, 100),
                fluid(ModFluids.AROMATICS, 50),
                fluid(ModFluids.REFORMGAS, 5),
                fluid(ModFluids.HYDROGEN, 20)
        ).save(writer, "catalytic_reformer/lightoil_crack");

        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.PETROLEUM, 100),
                fluid(ModFluids.UNSATURATEDS, 85),
                fluid(ModFluids.REFORMGAS, 10),
                fluid(ModFluids.HYDROGEN, 5)
        ).save(writer, "catalytic_reformer/petroleum");

        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.SOURGAS, 100),
                fluid(ModFluids.SULFURIC_ACID, 75),
                fluid(ModFluids.PETROLEUM, 10),
                fluid(ModFluids.HYDROGEN, 15)
        ).save(writer, "catalytic_reformer/sourgas");

        CatalyticReformerRecipeBuilder.catalyticReformerRecipe(
                fluid(ModFluids.CHOLESTEROL, 100),
                fluid(ModFluids.ESTRADIOL, 50),
                fluid(ModFluids.REFORMGAS, 35),
                fluid(ModFluids.HYDROGEN, 15)
        ).save(writer, "catalytic_reformer/cholesterol");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
