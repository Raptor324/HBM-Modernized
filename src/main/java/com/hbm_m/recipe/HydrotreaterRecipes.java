package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;

import net.minecraft.world.level.material.Fluid;

/**
 * Direkter Java-Port von {@code HydrotreatingRecipes} (1.7.10 Original,
 * {@code com.hbm.inventory.recipes.HydrotreatingRecipes}): der Hydrotreater entschwefelt 100mB
 * eines Oel-Fluids (Tank 0) pro Zyklus unter Verbrauch von Wasserstoff (Tank 1), erzeugt
 * entschwefeltes Oel (Tank 2) und Sauergas (Tank 3). Fest verdrahtete Java-Map statt
 * JSON-Rezeptsystem, analog zu {@link FractionTowerRecipes}.
 */
public final class HydrotreaterRecipes {

    private HydrotreaterRecipes() {}

    public record Recipe(int hydrogenMb, Fluid output, int outputMb, Fluid sourGas, int sourGasMb) {}

    private static final Map<Fluid, Recipe> RECIPES = new HashMap<>();

    private static void put(FluidEntry in, int hydrogenMb, FluidEntry out, int outMb, FluidEntry sour, int sourMb) {
        RECIPES.put(in.getSource(), new Recipe(hydrogenMb, out.getSource(), outMb, sour.getSource(), sourMb));
    }

    static {
        put(ModFluids.OIL_BASE, 5, ModFluids.OIL_DS, 90, ModFluids.SOURGAS, 15);
        put(ModFluids.CRACKOIL, 5, ModFluids.CRACKOIL_DS, 90, ModFluids.SOURGAS, 15);
        put(ModFluids.GAS, 5, ModFluids.PETROLEUM, 80, ModFluids.SOURGAS, 15);
        put(ModFluids.DIESEL_CRACK, 10, ModFluids.DIESEL, 80, ModFluids.SOURGAS, 30);
        put(ModFluids.DIESEL_CRACK_REFORM, 10, ModFluids.DIESEL_REFORM, 80, ModFluids.SOURGAS, 30);
        put(ModFluids.COALOIL, 10, ModFluids.COALGAS, 80, ModFluids.SOURGAS, 15);
    }

    public static boolean has(Fluid fluid) {
        return RECIPES.containsKey(fluid);
    }

    public static Recipe get(Fluid fluid) {
        return RECIPES.get(fluid);
    }
}
