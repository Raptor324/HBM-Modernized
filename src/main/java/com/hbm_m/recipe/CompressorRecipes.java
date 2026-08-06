package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;

import net.minecraft.world.level.material.Fluid;

/**
 * Port of {@code com.hbm.inventory.recipes.CompressorRecipes} (1.7.10 Original). The Compressor's
 * default behavior (no recipe found) is a generic 1000mB same-fluid compression that raises the
 * tank pressure by 1 - only these 5 special-cased fluid/pressure pairs override that default.
 */
public final class CompressorRecipes {

    private CompressorRecipes() {}

    public record Key(Fluid fluid, int pressure) {}
    public record Recipe(int inputAmount, Fluid outFluid, int outAmount, int outPressure, int duration) {}

    private static final Map<Key, Recipe> RECIPES = new HashMap<>();

    private static void put(FluidEntry in, int inPressure, int inputAmount,
                             FluidEntry out, int outAmount, int outPressure, int duration) {
        RECIPES.put(new Key(in.getSource(), inPressure), new Recipe(inputAmount, out.getSource(), outAmount, outPressure, duration));
    }

    static {
        put(ModFluids.PETROLEUM, 0, 2_000, ModFluids.PETROLEUM, 2_000, 1, 20);
        put(ModFluids.PETROLEUM, 1, 2_000, ModFluids.LPG, 1_000, 0, 20);
        put(ModFluids.BLOOD, 3, 1_000, ModFluids.HEAVYOIL, 250, 0, 200);
        put(ModFluids.PERFLUOROMETHYL, 0, 1_000, ModFluids.PERFLUOROMETHYL, 1_000, 1, 50);
        put(ModFluids.PERFLUOROMETHYL, 1, 1_000, ModFluids.PERFLUOROMETHYL_COLD, 1_000, 0, 50);
    }

    @Nullable
    public static Recipe get(Fluid fluid, int pressure) {
        return RECIPES.get(new Key(fluid, pressure));
    }

    public static Map<Key, Recipe> getAll() {
        return Map.copyOf(RECIPES);
    }
}
