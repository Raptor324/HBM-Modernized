package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов электролизёра, Fluid-режим ({@code hbm_m:electrolyser_fluid}).
 *
 * <p>1:1-порт всех 8 рецептов статической таблицы {@code ElectrolyserFluidRecipes.registerDefaults}
 * (Fluid-часть): вода, тяжёлая вода, купорос, слоп, красный шлам, глинозём, хлорид калия, хлорид кальция —
 * те же жидкости, количества, побочные продукты и длительности (стандарт 20 тиков).
 * Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class ElectrolyserFluidRecipeGenerator {

    private ElectrolyserFluidRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // Вода -> Водород + Кислород (10 тиков).
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.WATER, 2_000),
                fluid(ModFluids.HYDROGEN, 200),
                fluid(ModFluids.OXYGEN, 200),
                10
        ).save(writer, "electrolyser_fluid/water");

        // Тяжёлая вода -> Дейтерий + Кислород (10 тиков).
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.HEAVYWATER, 2_000),
                fluid(ModFluids.DEUTERIUM, 200),
                fluid(ModFluids.OXYGEN, 200),
                10
        ).save(writer, "electrolyser_fluid/heavy_water");

        // Купорос -> Серная кислота + Хлор (+ железный порошок и ртутный самородок).
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.VITRIOL, 1_000),
                fluid(ModFluids.SULFURIC_ACID, 500),
                fluid(ModFluids.CHLORINE, 500),
                20,
                new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER)),
                new ItemStack(ModItems.NUGGET_MERCURY.get())
        ).save(writer, "electrolyser_fluid/vitriol");

        // Слоп -> Ртуть (без второго выхода; оригинал использовал NONE/0) + селитра, известняк, сера.
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.SLOP, 1_000),
                fluid(ModFluids.MERCURY, 250),
                null,
                20,
                new ItemStack(ModItems.NITER.get(), 2),
                new ItemStack(ModMaterialItems.item(ModMaterials.LIMESTONE, MaterialShape.POWDER), 2),
                new ItemStack(ModItems.SULFUR.get())
        ).save(writer, "electrolyser_fluid/slop");

        // Красный шлам -> Ртуть + Щёлок (+ порошки титана/железа/алюминия).
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.REDMUD, 450),
                fluid(ModFluids.MERCURY, 150),
                fluid(ModFluids.LYE, 50),
                20,
                new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.POWDER), 3),
                new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER), 3),
                new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER), 2)
        ).save(writer, "electrolyser_fluid/redmud");

        // Глинозём -> Диоксид углерода (без второго выхода), 40 тиков (+ алюминиевый порошок, флюорит).
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.ALUMINA, 200),
                fluid(ModFluids.CARBONDIOXIDE, 100),
                null,
                40,
                new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER), 7),
                new ItemStack(ModItems.FLUORITE.get(), 2)
        ).save(writer, "electrolyser_fluid/alumina");

        // Хлорид калия -> Хлор (без второго выхода; оригинал использовал NONE/0) + пыль.
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.POTASSIUM_CHLORIDE, 250),
                fluid(ModFluids.CHLORINE, 125),
                null,
                20,
                new ItemStack(ModItems.DUST.get())
        ).save(writer, "electrolyser_fluid/potassium_chloride");

        // Хлорид кальция -> Хлор + Раствор кальция.
        ElectrolyserFluidRecipeBuilder.electrolyserFluidRecipe(
                fluid(ModFluids.CALCIUM_CHLORIDE, 250),
                fluid(ModFluids.CHLORINE, 125),
                fluid(ModFluids.CALCIUM_SOLUTION, 125),
                20
        ).save(writer, "electrolyser_fluid/calcium_chloride");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
