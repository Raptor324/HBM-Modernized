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
import net.minecraft.world.level.material.Fluid;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов газовой центрифуги ({@code hbm_m:gas_centrifuge}) — JEI-only.
 *
 * <p>Порт четырёх canonical-cascade рецептов из {@code GasCentrifugeJeiCategory.getDefaultRecipes()}
 * (legacy 1.7.10 NEI {@code GasCentrifugeHandler}, показывавший полностью обогащённый результат, а не
 * per-tick логику). Runtime Gas Centrifuge работает через cascade-enrichment ({@code PseudoFluidType});
 * эти рецепты используются только для статичного JEI-показа.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class GasCentrifugeRecipeGenerator {

    private GasCentrifugeRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // ── UF6 high-enriched (12 high-speed centrifuges worth of cascade) → U238×11 + U235×1 + fluorite×4
        GasCentrifugeRecipeBuilder.gasCentrifugeRecipe(
                        fluid(ModFluids.UF6.getSource(), 1200),
                        new ItemStack[]{
                                ModMaterialItems.stack(ModMaterials.URANIUM238, MaterialShape.NUGGET, 11),
                                ModMaterialItems.stack(ModMaterials.URANIUM235, MaterialShape.NUGGET, 1),
                                new ItemStack(ModItems.FLUORITE.get(), 4)
                        }, true, 4)
                .save(writer, "gas_centrifuge/uf6_high_enriched");

        // ── UF6 low-enriched → U238×6 + uranium_fuel×6 + fluorite×4 (needs 2 centrifuges)
        GasCentrifugeRecipeBuilder.gasCentrifugeRecipe(
                        fluid(ModFluids.UF6.getSource(), 1200),
                        new ItemStack[]{
                                ModMaterialItems.stack(ModMaterials.URANIUM238, MaterialShape.NUGGET, 6),
                                ModMaterialItems.stack(ModMaterials.URANIUM_FUEL, MaterialShape.NUGGET, 6),
                                new ItemStack(ModItems.FLUORITE.get(), 4)
                        }, false, 2)
                .save(writer, "gas_centrifuge/uf6_low_enriched");

        // ── PUF6 → Pu238×3 + Pu-mix×6 + fluorite×3 (1 centrifuge)
        GasCentrifugeRecipeBuilder.gasCentrifugeRecipe(
                        fluid(ModFluids.PUF6.getSource(), 900),
                        new ItemStack[]{
                                ModMaterialItems.stack(ModMaterials.PLUTONIUM238, MaterialShape.NUGGET, 3),
                                ModMaterialItems.stack(ModMaterials.PU_MIX, MaterialShape.NUGGET, 6),
                                new ItemStack(ModItems.FLUORITE.get(), 3)
                        }, false, 1)
                .save(writer, "gas_centrifuge/puf6_enriched");

        // ── WATZ sludge → iron powder×1 + lead powder×1 + nuclear_waste_tiny×1 + dust×2 (2 centrifuges)
        // ModMaterialItems API: MaterialShape.POWDER — порошок (был getPowders/getPowder).
        GasCentrifugeRecipeBuilder.gasCentrifugeRecipe(
                        fluid(ModFluids.WATZ.getSource(), 1000),
                        new ItemStack[]{
                                ModMaterialItems.stack(ModMaterials.IRON, MaterialShape.POWDER, 1),
                                ModMaterialItems.stack(ModMaterials.LEAD, MaterialShape.POWDER, 1),
                                new ItemStack(ModItems.NUCLEAR_WASTE_TINY.get(), 1),
                                new ItemStack(ModItems.DUST.get(), 2)
                        }, false, 2)
                .save(writer, "gas_centrifuge/watz_sludge");
    }

    private static FluidStack fluid(Fluid fluid, int mb) {
        return FluidStack.create(fluid, mb);
    }
}
//?}