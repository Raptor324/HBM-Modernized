package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;

import net.minecraft.world.level.material.Fluid;

/**
 * Direkter Java-Port von {@code FractionRecipes} (1.7.10 Original, {@code com.hbm.inventory.recipes.FractionRecipes}):
 * ein Oel-Fraktionsturm spaltet 100mB eines "schweren" Oel-Fluids pro Zyklus in zwei leichtere Fraktionen auf.
 * Fest verdrahtete Java-Map statt JSON-Rezeptsystem, analog zu {@code CentrifugeRecipes} - die Rezeptliste ist
 * klein und aendert sich nicht zur Laufzeit.
 */
public final class FractionTowerRecipes {

    private FractionTowerRecipes() {}

    public record Split(Fluid outA, int amountA, Fluid outB, int amountB) {}

    private static final Map<Fluid, Split> RECIPES = new HashMap<>();

    private static void put(FluidEntry in, FluidEntry outA, int amountA, FluidEntry outB, int amountB) {
        RECIPES.put(in.getSource(), new Split(outA.getSource(), amountA, outB.getSource(), amountB));
    }

    static {
        put(ModFluids.HEAVYOIL,          ModFluids.BITUMEN,               30, ModFluids.SMEAR,               70);
        put(ModFluids.HEAVYOIL_VACUUM,   ModFluids.SMEAR,                 40, ModFluids.HEATINGOIL_VACUUM,   60);
        put(ModFluids.SMEAR,             ModFluids.HEATINGOIL,            60, ModFluids.LUBRICANT,           40);
        put(ModFluids.NAPHTHA,           ModFluids.HEATINGOIL,            40, ModFluids.DIESEL,              60);
        put(ModFluids.NAPHTHA_DS,        ModFluids.XYLENE,                60, ModFluids.DIESEL_REFORM,       40);
        put(ModFluids.NAPHTHA_CRACK,     ModFluids.HEATINGOIL,            30, ModFluids.DIESEL_CRACK,        70);
        put(ModFluids.LIGHTOIL,          ModFluids.DIESEL,                40, ModFluids.KEROSENE,            60);
        put(ModFluids.LIGHTOIL_DS,       ModFluids.DIESEL_REFORM,         60, ModFluids.KEROSENE_REFORM,     40);
        put(ModFluids.LIGHTOIL_CRACK,    ModFluids.KEROSENE,              70, ModFluids.PETROLEUM,           30);
        put(ModFluids.COALOIL,           ModFluids.COALGAS,               30, ModFluids.OIL_BASE,            70);
        put(ModFluids.COALCREOSOTE,      ModFluids.COALOIL,               10, ModFluids.BITUMEN,             90);
        put(ModFluids.REFORMATE,         ModFluids.AROMATICS,             40, ModFluids.XYLENE,              60);
        put(ModFluids.LIGHTOIL_VACUUM,   ModFluids.KEROSENE,              70, ModFluids.REFORMGAS,           30);
        put(ModFluids.EGG,               ModFluids.CHOLESTEROL,           50, ModFluids.RADIOSOLVENT,        50);
        put(ModFluids.OIL_COKER,         ModFluids.CRACKOIL,              30, ModFluids.HEATINGOIL,          70);
        put(ModFluids.NAPHTHA_COKER,     ModFluids.NAPHTHA_CRACK,         75, ModFluids.LIGHTOIL_CRACK,      25);
        put(ModFluids.GAS_COKER,         ModFluids.AROMATICS,             25, ModFluids.CARBONDIOXIDE,       75);
        put(ModFluids.CHLOROCALCITE_MIX, ModFluids.CHLOROCALCITE_CLEANED, 50, ModFluids.COLLOID,             50);
        put(ModFluids.BAUXITE_SOLUTION,  ModFluids.REDMUD,                50, ModFluids.SODIUM_ALUMINATE,    50);
    }

    public static boolean has(Fluid fluid) {
        return RECIPES.containsKey(fluid);
    }

    public static Split get(Fluid fluid) {
        return RECIPES.get(fluid);
    }
}
