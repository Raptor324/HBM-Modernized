package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Direkter Java-Port von {@code CokerRecipes} (1.7.10 Original, {@code
 * com.hbm.inventory.recipes.CokerRecipes}). Feste Java-Map statt JSON-Rezeptsystem, analog zu
 * {@link VacuumDistillRecipes}. Die meisten Rezepte werden - wie im Original - automatisch aus der
 * {@code FT_Flammable}/{@code FT_Combustible}-Verbrennungsenergie des Eingangsfluids berechnet
 * ({@code registerSFAuto}: 820.000 TU Brennwert-Aequivalent = 1 Kohle-Bonusstack, ergibt die
 * benoetigte mB-Menge pro Zyklus).
 * <p>
 * SCOPE-Entscheidung: Das Original nutzt einen einzigen Metadata-Multi-Item {@code
 * ItemEnumMulti(EnumCokeType)} mit COAL/LIGNITE/PETROLEUM-Sub-Items - hier existiert nur die fuer
 * den Coker benoetigte PETROLEUM-Variante als eigenstaendiges {@link ModItems#COKE_PETROLEUM}
 * (COAL/LIGNITE gehoeren zu anderen, noch nicht portierten Maschinen). Das
 * CALCIUM_SOLUTION-Rezept entfaellt ersatzlos (kein Calcium-Pulver-Item in diesem Port vorhanden).
 */
public final class CokerRecipes {

    private CokerRecipes() {}

    public record Recipe(int inputMb, ItemStack output, Fluid byproduct, int byproductMb) {}

    private static final Map<Fluid, Recipe> RECIPES = new HashMap<>();

    private static void registerAuto(FluidEntry fluid, FluidEntry byproductType) {
        // 3200 burntime * 1.25 burntime bonus * 200 TU/t + 20000TU pro Zyklus - 1:1 aus dem Original.
        registerSFAuto(fluid, 820_000L, new ItemStack(ModItems.COKE_PETROLEUM.get()), byproductType);
    }

    private static void registerSFAuto(FluidEntry fluid, long tuPerSF, ItemStack output, FluidEntry byproductType) {
        Fluid f = fluid.getSource();

        FT_Flammable flammable = FluidType.getTrait(f, FT_Flammable.class);
        FT_Combustible combustible = FluidType.getTrait(f, FT_Combustible.class);
        long tuFlammable = flammable != null ? flammable.getHeatEnergy() : 0L;
        long tuCombustible = combustible != null ? combustible.getCombustionEnergy() : 0L;
        long tuPerBucket = Math.max(tuFlammable, tuCombustible);

        if (tuPerBucket <= 0) return; // keine Brennwert-Traits hinterlegt - Rezept entfaellt (dokumentierte Luecke).

        int mB = (int) (tuPerSF * 1000L / tuPerBucket);
        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);
        if (mB <= 0) return;

        Fluid byproduct = byproductType != null ? byproductType.getSource() : null;
        int byproductMb = byproductType != null ? Math.max(10, mB / 10) : 0;
        RECIPES.put(f, new Recipe(mB, output, byproduct, byproductMb));
    }

    private static void registerRecipe(FluidEntry fluid, int mb, ItemStack output, FluidEntry byproductType, int byproductMb) {
        Fluid byproduct = byproductType != null ? byproductType.getSource() : null;
        RECIPES.put(fluid.getSource(), new Recipe(mb, output, byproduct, byproductType != null ? byproductMb : 0));
    }

    static {
        registerAuto(ModFluids.HEAVYOIL, ModFluids.OIL_COKER);
        registerAuto(ModFluids.HEAVYOIL_VACUUM, ModFluids.REFORMATE);
        registerAuto(ModFluids.COALCREOSOTE, ModFluids.NAPHTHA_COKER);
        registerAuto(ModFluids.SMEAR, ModFluids.OIL_COKER);
        registerAuto(ModFluids.HEATINGOIL, ModFluids.OIL_COKER);
        registerAuto(ModFluids.HEATINGOIL_VACUUM, ModFluids.OIL_COKER);
        registerAuto(ModFluids.RECLAIMED, ModFluids.NAPHTHA_COKER);
        registerAuto(ModFluids.NAPHTHA, ModFluids.NAPHTHA_COKER);
        registerAuto(ModFluids.NAPHTHA_DS, ModFluids.NAPHTHA_COKER);
        registerAuto(ModFluids.NAPHTHA_CRACK, ModFluids.NAPHTHA_COKER);
        registerAuto(ModFluids.DIESEL, ModFluids.NAPHTHA_COKER);
        registerAuto(ModFluids.DIESEL_REFORM, ModFluids.NAPHTHA_COKER);
        registerAuto(ModFluids.DIESEL_CRACK, ModFluids.GAS_COKER);
        registerAuto(ModFluids.DIESEL_CRACK_REFORM, ModFluids.GAS_COKER);
        registerAuto(ModFluids.LIGHTOIL, ModFluids.GAS_COKER);
        registerAuto(ModFluids.LIGHTOIL_DS, ModFluids.GAS_COKER);
        registerAuto(ModFluids.LIGHTOIL_CRACK, ModFluids.GAS_COKER);
        registerAuto(ModFluids.LIGHTOIL_VACUUM, ModFluids.GAS_COKER);
        registerAuto(ModFluids.BIOFUEL, ModFluids.GAS_COKER);
        registerAuto(ModFluids.AROMATICS, ModFluids.GAS_COKER);
        registerAuto(ModFluids.REFORMATE, ModFluids.GAS_COKER);
        registerAuto(ModFluids.XYLENE, ModFluids.GAS_COKER);
        registerAuto(ModFluids.FISHOIL, ModFluids.MERCURY);
        registerAuto(ModFluids.SUNFLOWEROIL, ModFluids.GAS_COKER);

        registerSFAuto(ModFluids.WOODOIL, 340_000L, new ItemStack(Items.CHARCOAL), ModFluids.GAS_COKER);

        registerRecipe(ModFluids.WATZ, 4_000, new ItemStack(ModItems.getIngot(ModIngots.MUD).get(), 4), null, 0);
        registerRecipe(ModFluids.REDMUD, 450, new ItemStack(Items.IRON_INGOT, 1), ModFluids.MERCURY, 50);
        registerRecipe(ModFluids.BITUMEN, 16_000, new ItemStack(ModItems.COKE_PETROLEUM.get()), ModFluids.OIL_COKER, 1_600);
        registerRecipe(ModFluids.LUBRICANT, 12_000, new ItemStack(ModItems.COKE_PETROLEUM.get()), ModFluids.OIL_COKER, 1_200);
        // CALCIUM_SOLUTION -> Calcium-Pulver entfaellt (kein solches Item in diesem Port).
        registerRecipe(ModFluids.SOURGAS, 1_000, new ItemStack(ModItems.SULFUR.get()), ModFluids.GAS_COKER, 150);
        registerRecipe(ModFluids.SLOP, 1_000, new ItemStack(ModItems.getPowders(ModPowders.LIMESTONE).get()), ModFluids.COLLOID, 250);
        registerRecipe(ModFluids.VITRIOL, 4_000, new ItemStack(ModItems.getPowders(ModPowders.IRON).get()), ModFluids.SULFURIC_ACID, 500);
    }

    public static boolean has(Fluid fluid) {
        return RECIPES.containsKey(fluid);
    }

    public static Recipe get(Fluid fluid) {
        return RECIPES.get(fluid);
    }

    public static Map<Fluid, Recipe> getAll() {
        return Map.copyOf(RECIPES);
    }
}
