package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;

import net.minecraft.world.level.material.Fluid;

/**
 * Direkter Java-Port von {@code VacuumRefineryRecipes} (1.7.10 Original,
 * {@code com.hbm.inventory.recipes.VacuumRefineryRecipes}): der Vakuumdestillator spaltet 100mB
 * Rohoel (Tank 0) pro Zyklus in vier Fraktionen auf (schweres Vakuumoel, Reformat, leichtes
 * Vakuumoel, Sauergas/Reformgas). Fest verdrahtete Java-Map statt JSON-Rezeptsystem, analog zu
 * {@link FractionTowerRecipes} - nur zwei Eingangsfluide, aendert sich nicht zur Laufzeit.
 */
public final class VacuumDistillRecipes {

    private VacuumDistillRecipes() {}

    public static final int HEAVY_MB = 40;
    public static final int REFORMATE_MB = 25;
    public static final int LIGHT_MB = 20;
    public static final int SOUR_MB = 15;

    public record Output(Fluid heavy, Fluid reformate, Fluid light, Fluid sour) {}

    private static final Map<Fluid, Output> RECIPES = new HashMap<>();

    private static void put(FluidEntry in, FluidEntry sour) {
        RECIPES.put(in.getSource(), new Output(
                ModFluids.HEAVYOIL_VACUUM.getSource(),
                ModFluids.REFORMATE.getSource(),
                ModFluids.LIGHTOIL_VACUUM.getSource(),
                sour.getSource()));
    }

    static {
        put(ModFluids.OIL_BASE, ModFluids.SOURGAS);
        put(ModFluids.OIL_DS, ModFluids.REFORMGAS);
    }

    public static boolean has(Fluid fluid) {
        return RECIPES.containsKey(fluid);
    }

    public static Output get(Fluid fluid) {
        return RECIPES.get(fluid);
    }
}
