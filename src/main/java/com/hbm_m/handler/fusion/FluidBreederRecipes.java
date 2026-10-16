package com.hbm_m.handler.fusion;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.world.level.material.Fluid;

/**
 * 1:1-Port von {@code com.hbm.inventory.recipes.FluidBreederRecipes} (1.7.10).
 *
 * <p>Bestrahlung von Fluiden im Fusionsbrueter. Wie beim bereits portierten
 * {@code RBMKOutgasserRecipes} liegt die Tabelle statisch im Code - das Original haelt sie
 * ebenfalls in einer statischen {@code HashMap} (dort ueber {@code SerializableRecipe}
 * zusaetzlich per JSON ueberschreibbar).</p>
 */
public final class FluidBreederRecipes {

    private FluidBreederRecipes() {}

    /** Ein Rezept: {@code amountIn} mB Eingangsfluid werden zu {@code amountOut} mB Ausgangsfluid. */
    public record FluidBreederRecipe(int amountIn, Fluid output, int amountOut) {}

    private static final Map<Fluid, FluidBreederRecipe> RECIPES = new LinkedHashMap<>();

    private static void register(Fluid input, int amountIn, Fluid output, int amountOut) {
        RECIPES.put(input, new FluidBreederRecipe(amountIn, output, amountOut));
    }

    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        initialized = true;

        register(ModFluids.GAS.getSource(), 1_000, ModFluids.SYNGAS.getSource(), 1_000);
        register(ModFluids.LIGHTOIL.getSource(), 1_000, ModFluids.REFORMGAS.getSource(), 1_000);
        register(ModFluids.LIGHTOIL_CRACK.getSource(), 1_000, ModFluids.REFORMGAS.getSource(), 1_000);
    }

    @Nullable
    public static FluidBreederRecipe getOutput(Fluid type) {
        init();
        return RECIPES.get(type);
    }

    public static Map<Fluid, FluidBreederRecipe> getRecipes() {
        init();
        return RECIPES;
    }
}
