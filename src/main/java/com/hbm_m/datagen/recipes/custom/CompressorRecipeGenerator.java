package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов компрессора ({@code hbm_m:compressor}).
 *
 * <p>Порт 5 спец-рецептов из удалённого статического {@code CompressorRecipes} (static-блок,
 * Direktport 1.7.10 {@code CompressorRecipes}). Ключ оригинальной Map — пара (жидкость,
 * давление входного бака) — переносится в JSON полями {@code input_pressure}/{@code output_pressure}.
 * Генерический «+1 к давлению»-fallback машины остаётся захардкоженным в BE и здесь не
 * описывается. Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для
 * 1.20.1-forge.</p>
 */
public final class CompressorRecipeGenerator {

    private CompressorRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        CompressorRecipeBuilder.compressorRecipe(
                fluid(ModFluids.PETROLEUM, 2_000),
                0,
                fluid(ModFluids.PETROLEUM, 2_000),
                1,
                20
        ).save(writer, "compressor/petroleum_p0");

        CompressorRecipeBuilder.compressorRecipe(
                fluid(ModFluids.PETROLEUM, 2_000),
                1,
                fluid(ModFluids.LPG, 1_000),
                0,
                20
        ).save(writer, "compressor/petroleum_p1_to_lpg");

        CompressorRecipeBuilder.compressorRecipe(
                fluid(ModFluids.BLOOD, 1_000),
                3,
                fluid(ModFluids.HEAVYOIL, 250),
                0,
                200
        ).save(writer, "compressor/blood_p3_to_heavyoil");

        CompressorRecipeBuilder.compressorRecipe(
                fluid(ModFluids.PERFLUOROMETHYL, 1_000),
                0,
                fluid(ModFluids.PERFLUOROMETHYL, 1_000),
                1,
                50
        ).save(writer, "compressor/perfluoromethyl_p0");

        CompressorRecipeBuilder.compressorRecipe(
                fluid(ModFluids.PERFLUOROMETHYL, 1_000),
                1,
                fluid(ModFluids.PERFLUOROMETHYL_COLD, 1_000),
                0,
                50
        ).save(writer, "compressor/perfluoromethyl_p1_to_cold");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
