package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов электролизёра, Fluid-режим ({@code hbm_m:electrolyser_fluid}).
 *
 * <p>Порт 6 рецептов из удалённого статического {@code ElectrolyserRecipes} (Fluid-часть).
 * Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class ElectrolyserFluidRecipeGenerator {

    private ElectrolyserFluidRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // Вода -> Водород + Кислород.
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.WATER, 2_000),
                fluid(ModFluids.HYDROGEN, 200),
                fluid(ModFluids.OXYGEN, 200)
        ).save(writer, "electrolyser_fluid/water");

        // Тяжёлая вода -> Дейтерий + Кислород.
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.HEAVYWATER, 2_000),
                fluid(ModFluids.DEUTERIUM, 200),
                fluid(ModFluids.OXYGEN, 200)
        ).save(writer, "electrolyser_fluid/heavy_water");

        // Купорос -> Серная кислота + Хлор (+ железный порошок и ртутный самородок).
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.VITRIOL, 1_000),
                fluid(ModFluids.SULFURIC_ACID, 500),
                fluid(ModFluids.CHLORINE, 500),
                new ItemStack(ModItems.getPowders(ModPowders.IRON).get()),
                new ItemStack(ModItems.NUGGET_MERCURY.get())
        ).save(writer, "electrolyser_fluid/vitriol");

        // Красный шлам -> Ртуть + Щёлок (+ порошки титана/железа/алюминия).
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.REDMUD, 450),
                fluid(ModFluids.MERCURY, 150),
                fluid(ModFluids.LYE, 50),
                new ItemStack(ModItems.getPowder(ModIngots.TITANIUM).get(), 3),
                new ItemStack(ModItems.getPowders(ModPowders.IRON).get(), 3),
                new ItemStack(ModItems.getPowder(ModIngots.ALUMINUM).get(), 2)
        ).save(writer, "electrolyser_fluid/redmud");

        // Хлорид калия -> Хлор (без второго выхода; оригинал использовал NONE/0).
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.POTASSIUM_CHLORIDE, 250),
                fluid(ModFluids.CHLORINE, 125),
                null
        ).save(writer, "electrolyser_fluid/potassium_chloride");

        // Хлорид кальция -> Хлор + Раствор кальция.
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.CALCIUM_CHLORIDE, 250),
                fluid(ModFluids.CHLORINE, 125),
                fluid(ModFluids.CALCIUM_SOLUTION, 125)
        ).save(writer, "electrolyser_fluid/calcium_chloride");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
