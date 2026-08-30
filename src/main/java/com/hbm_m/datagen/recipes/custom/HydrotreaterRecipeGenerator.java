package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов гидроочистки ({@code hbm_m:hydrotreater}).
 *
 * <p>Порт 6 рецептов из удалённого статического {@code HydrotreaterRecipes} (static-блок,
 * Direktport 1.7.10 {@code HydrotreatingRecipes}). Жидкостные стаки создаются через
 * {@link FluidStack#create} из {@link ModFluids} (mB). Чистый ванильный 1.20.1 код внутри
 * {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class HydrotreaterRecipeGenerator {

    private HydrotreaterRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        HydrotreaterRecipeBuilder.hydrotreaterRecipe(
                fluid(ModFluids.OIL_BASE, 100),
                fluid(ModFluids.HYDROGEN, 5),
                fluid(ModFluids.OIL_DS, 90),
                fluid(ModFluids.SOURGAS, 15)
        ).save(writer, "hydrotreater/oil_base");

        HydrotreaterRecipeBuilder.hydrotreaterRecipe(
                fluid(ModFluids.CRACKOIL, 100),
                fluid(ModFluids.HYDROGEN, 5),
                fluid(ModFluids.CRACKOIL_DS, 90),
                fluid(ModFluids.SOURGAS, 15)
        ).save(writer, "hydrotreater/crackoil");

        HydrotreaterRecipeBuilder.hydrotreaterRecipe(
                fluid(ModFluids.GAS, 100),
                fluid(ModFluids.HYDROGEN, 5),
                fluid(ModFluids.PETROLEUM, 80),
                fluid(ModFluids.SOURGAS, 15)
        ).save(writer, "hydrotreater/gas");

        HydrotreaterRecipeBuilder.hydrotreaterRecipe(
                fluid(ModFluids.DIESEL_CRACK, 100),
                fluid(ModFluids.HYDROGEN, 10),
                fluid(ModFluids.DIESEL, 80),
                fluid(ModFluids.SOURGAS, 30)
        ).save(writer, "hydrotreater/diesel_crack");

        HydrotreaterRecipeBuilder.hydrotreaterRecipe(
                fluid(ModFluids.DIESEL_CRACK_REFORM, 100),
                fluid(ModFluids.HYDROGEN, 10),
                fluid(ModFluids.DIESEL_REFORM, 80),
                fluid(ModFluids.SOURGAS, 30)
        ).save(writer, "hydrotreater/diesel_crack_reform");

        HydrotreaterRecipeBuilder.hydrotreaterRecipe(
                fluid(ModFluids.COALOIL, 100),
                fluid(ModFluids.HYDROGEN, 10),
                fluid(ModFluids.COALGAS, 80),
                fluid(ModFluids.SOURGAS, 15)
        ).save(writer, "hydrotreater/coaloil");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
