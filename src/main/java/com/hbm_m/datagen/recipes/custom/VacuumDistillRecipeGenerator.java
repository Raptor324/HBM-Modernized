package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов вакуумной дистилляции ({@code hbm_m:vacuum_distill}).
 *
 * <p>Порт 2 рецептов из удалённого статического {@code VacuumDistillRecipes} (static-блок,
 * Direktport 1.7.10 {@code VacuumRefineryRecipes}). Три фракции фиксированы (heavy/reformate/light),
 * четвёртая (sour) зависит от входа: сырая нефть → кислый газ, десульфурированная — реформат-газ.
 * Объёмы — константы оригинала 40/25/20/15 mB. Чистый ванильный 1.20.1 код внутри
 * {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class VacuumDistillRecipeGenerator {

    private VacuumDistillRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        VacuumDistillRecipeBuilder.vacuumDistillRecipe(
                fluid(ModFluids.OIL_BASE, 100),
                fluid(ModFluids.HEAVYOIL_VACUUM, 40),
                fluid(ModFluids.REFORMATE, 25),
                fluid(ModFluids.LIGHTOIL_VACUUM, 20),
                fluid(ModFluids.SOURGAS, 15)
        ).save(writer, "vacuum_distill/oil_base");

        VacuumDistillRecipeBuilder.vacuumDistillRecipe(
                fluid(ModFluids.OIL_DS, 100),
                fluid(ModFluids.HEAVYOIL_VACUUM, 40),
                fluid(ModFluids.REFORMATE, 25),
                fluid(ModFluids.LIGHTOIL_VACUUM, 20),
                fluid(ModFluids.REFORMGAS, 15)
        ).save(writer, "vacuum_distill/oil_ds");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
