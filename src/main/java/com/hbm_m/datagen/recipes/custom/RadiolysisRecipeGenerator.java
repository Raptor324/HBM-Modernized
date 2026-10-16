package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов радиолиза ({@code hbm_m:radiolysis}).
 *
 * <p>Порт единственного собственного рецепта из удалённого статического {@code RadiolysisRecipes}
 * (вода → пероксид + водород). Делегирование крэкинг-таблице оригинала сохранено на уровне машины
 * ({@code MachineRadiolysisBlockEntity} падает в {@code CrackingTowerRecipe}), здесь переносится
 * только «собственная» запись. Чистый ванильный 1.20.1 код внутри {@code //? if forge} —
 * датаген только для 1.20.1-forge.</p>
 */
public final class RadiolysisRecipeGenerator {

    private RadiolysisRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        RadiolysisRecipeBuilder.radiolysisRecipe(
                fluid(ModFluids.WATER, 100),
                fluid(ModFluids.PEROXIDE, 80),
                fluid(ModFluids.HYDROGEN, 20)
        ).save(writer, "radiolysis/water");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
