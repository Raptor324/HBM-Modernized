package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;

import net.minecraft.world.level.material.Fluid;

/**
 * Direkter Java-Port von {@code ReformingRecipes} (1.7.10 Original,
 * {@code com.hbm.inventory.recipes.ReformingRecipes}): der katalytische Reformer wandelt 100mB
 * eines Oel-Fluids (Tank 0) pro Zyklus in drei Ausgangsfluide um (u.a. immer etwas Wasserstoff,
 * anders als beim Hydrotreater wird hier KEIN Wasserstoff verbraucht, sondern erzeugt). Fest
 * verdrahtete Java-Map, analog zu {@link FractionTowerRecipes}.
 */
public final class CatalyticReformerRecipes {

    private CatalyticReformerRecipes() {}

    public record Triple(Fluid outA, int amountA, Fluid outB, int amountB, Fluid outC, int amountC) {}

    private static final Map<Fluid, Triple> RECIPES = new HashMap<>();

    private static void put(FluidEntry in, FluidEntry a, int amountA, FluidEntry b, int amountB, FluidEntry c, int amountC) {
        RECIPES.put(in.getSource(), new Triple(a.getSource(), amountA, b.getSource(), amountB, c.getSource(), amountC));
    }

    static {
        put(ModFluids.HEATINGOIL,      ModFluids.NAPHTHA,    50, ModFluids.PETROLEUM,   15, ModFluids.HYDROGEN, 10);
        put(ModFluids.NAPHTHA,         ModFluids.REFORMATE,  50, ModFluids.PETROLEUM,   15, ModFluids.HYDROGEN, 10);
        put(ModFluids.NAPHTHA_CRACK,   ModFluids.REFORMATE,  50, ModFluids.AROMATICS,   10, ModFluids.HYDROGEN, 5);
        put(ModFluids.NAPHTHA_COKER,   ModFluids.REFORMATE,  50, ModFluids.REFORMGAS,   10, ModFluids.HYDROGEN, 5);
        put(ModFluids.LIGHTOIL,        ModFluids.AROMATICS,  50, ModFluids.REFORMGAS,   10, ModFluids.HYDROGEN, 15);
        put(ModFluids.LIGHTOIL_CRACK,  ModFluids.AROMATICS,  50, ModFluids.REFORMGAS,   5,  ModFluids.HYDROGEN, 20);
        put(ModFluids.PETROLEUM,       ModFluids.UNSATURATEDS, 85, ModFluids.REFORMGAS, 10, ModFluids.HYDROGEN, 5);
        put(ModFluids.SOURGAS,         ModFluids.SULFURIC_ACID, 75, ModFluids.PETROLEUM, 10, ModFluids.HYDROGEN, 15);
        put(ModFluids.CHOLESTEROL,     ModFluids.ESTRADIOL,  50, ModFluids.REFORMGAS,   35, ModFluids.HYDROGEN, 15);
    }

    public static boolean has(Fluid fluid) {
        return RECIPES.containsKey(fluid);
    }

    public static Triple get(Fluid fluid) {
        return RECIPES.get(fluid);
    }
}
