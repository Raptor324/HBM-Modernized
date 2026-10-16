package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов промышленного миксера ({@code hbm_m:mixer}).
 *
 * <p>Порт 4 рецептов из удалённого статического {@code MixerRecipes} (static-блок).
 * Жидкостные стаки создаются через {@link FluidStack#create} из {@link ModFluids} (mB).
 * Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class MixerRecipeGenerator {

    private MixerRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // Sulfuric Acid: Vitriol + Water -> Sulfuric Acid (прямой порт vitriol-процесса).
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.VITRIOL,        1000),
                fluid(ModFluids.WATER,          1000),
                fluid(ModFluids.SULFURIC_ACID, 2000),
                100, 50L
        ).save(writer, "mixer/vitriol_water_to_sulfuric_acid");

        // Biofuel: Ethanol + Sunflower Oil -> Biofuel (трансэтерификация растительного масла).
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.ETHANOL,     1000),
                fluid(ModFluids.SUNFLOWEROIL, 1000),
                fluid(ModFluids.BIOFUEL,    2000),
                100, 50L
        ).save(writer, "mixer/ethanol_oil_to_biofuel");

        // Nitroglycerin: Nitric Acid + Peroxide -> Nitroglycerin (упрощённый нитрационный шаг).
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.NITRIC_ACID,  1000),
                fluid(ModFluids.PEROXIDE,     1000),
                fluid(ModFluids.NITROGLYCERIN, 1000),
                200, 100L
        ).save(writer, "mixer/nitric_acid_peroxide_to_nitroglycerin");

        // Chlorocalcite Mix: Calcium Solution + Chlorocalcite Solution -> Chlorocalcite Mix
        // (часть цепочки хлорида кальция / извлечения лития).
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.CALCIUM_SOLUTION,        1000),
                fluid(ModFluids.CHLOROCALCITE_SOLUTION, 1000),
                fluid(ModFluids.CHLOROCALCITE_MIX,     2000),
                150, 75L
        ).save(writer, "mixer/calcium_chlorocalcite_to_mix");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
