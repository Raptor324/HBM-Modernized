package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов крекинговой башни ({@code hbm_m:cracking_tower}).
 *
 * <p>Порт 12 рецептов из удалённого статического {@code CrackingTowerRecipes} (static-блок,
 * Direktport 1.7.10 {@code CrackingRecipes}). Жидкостные стаки создаются через
 * {@link FluidStack#create} из {@link ModFluids} (mB). Чистый ванильный 1.20.1 код внутри
 * {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class CrackingTowerRecipeGenerator {

    private CrackingTowerRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // Kerosene не имеет второго выхода (оригинал: Fluids.NONE) — outputB = null.
        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.OIL_BASE, 100),
                fluid(ModFluids.CRACKOIL, 80),
                fluid(ModFluids.PETROLEUM, 20)
        ).save(writer, "cracking_tower/oil_base");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.BITUMEN, 100),
                fluid(ModFluids.OIL_BASE, 80),
                fluid(ModFluids.AROMATICS, 20)
        ).save(writer, "cracking_tower/bitumen");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.SMEAR, 100),
                fluid(ModFluids.NAPHTHA, 60),
                fluid(ModFluids.PETROLEUM, 40)
        ).save(writer, "cracking_tower/smear");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.GAS, 100),
                fluid(ModFluids.PETROLEUM, 30),
                fluid(ModFluids.UNSATURATEDS, 20)
        ).save(writer, "cracking_tower/gas");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.DIESEL, 100),
                fluid(ModFluids.KEROSENE, 40),
                fluid(ModFluids.PETROLEUM, 30)
        ).save(writer, "cracking_tower/diesel");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.DIESEL_CRACK, 100),
                fluid(ModFluids.KEROSENE, 40),
                fluid(ModFluids.PETROLEUM, 30)
        ).save(writer, "cracking_tower/diesel_crack");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.KEROSENE, 100),
                fluid(ModFluids.PETROLEUM, 60),
                null
        ).save(writer, "cracking_tower/kerosene");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.WOODOIL, 100),
                fluid(ModFluids.HEATINGOIL, 40),
                fluid(ModFluids.AROMATICS, 10)
        ).save(writer, "cracking_tower/woodoil");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.XYLENE, 100),
                fluid(ModFluids.AROMATICS, 80),
                fluid(ModFluids.PETROLEUM, 20)
        ).save(writer, "cracking_tower/xylene");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.HEATINGOIL_VACUUM, 100),
                fluid(ModFluids.HEATINGOIL, 80),
                fluid(ModFluids.REFORMGAS, 20)
        ).save(writer, "cracking_tower/heatingoil_vacuum");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.REFORMATE, 100),
                fluid(ModFluids.UNSATURATEDS, 40),
                fluid(ModFluids.REFORMGAS, 60)
        ).save(writer, "cracking_tower/reformate");

        CrackingTowerRecipeBuilder.crackingTowerRecipe(
                fluid(ModFluids.BIOGAS, 100),
                fluid(ModFluids.PETROLEUM, 20),
                fluid(ModFluids.AROMATICS, 20)
        ).save(writer, "cracking_tower/biogas");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
