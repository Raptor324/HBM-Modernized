package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.world.level.material.Fluid;

/**
 * Recipe table for the Industrial Mixer.
 *
 * <p>Port of the relevant subset of {@code com.hbm.inventory.recipes.MixerRecipes} from 1.7.10:
 * the Mixer combines two input fluids into a single output fluid over time while consuming
 * energy. Recipes are populated via a static initializer so this class doesn't need to be
 * touched/called from {@code MainRegistry} - simply referencing {@link #getAll()} or
 * {@link #findRecipe(Fluid, Fluid)} triggers class loading and population.</p>
 */
public final class MixerRecipes {

    private static final List<MixerRecipe> RECIPES = new ArrayList<>();

    private MixerRecipes() {}

    /**
     * Single mixer recipe: two input fluids (in any tank order) combine into one output fluid.
     */
    public record MixerRecipe(
            Fluid inputA, int amountA,
            Fluid inputB, int amountB,
            Fluid output, int outputAmount,
            int duration,
            long energyPerTick) {

        /** Returns true if the given pair of fluids matches this recipe in either tank order. */
        public boolean matches(Fluid tankA, Fluid tankB) {
            boolean direct = VanillaFluidEquivalence.sameSubstance(tankA, inputA)
                    && VanillaFluidEquivalence.sameSubstance(tankB, inputB);
            boolean swapped = VanillaFluidEquivalence.sameSubstance(tankA, inputB)
                    && VanillaFluidEquivalence.sameSubstance(tankB, inputA);
            return direct || swapped;
        }

        /**
         * Returns true if {@code tankA} corresponds to {@link #inputA} (i.e. tanks are NOT
         * swapped relative to this recipe's declaration order). Used to determine which tank
         * to drain by which amount.
         */
        public boolean isDirectOrder(Fluid tankA) {
            return VanillaFluidEquivalence.sameSubstance(tankA, inputA);
        }
    }

    /**
     * Finds a recipe matching the two given input fluids (order-independent).
     * Returns {@code null} if either fluid is empty/none or no recipe matches.
     */
    @Nullable
    public static MixerRecipe findRecipe(Fluid tankA, Fluid tankB) {
        if (tankA == null || tankB == null) return null;
        if (VanillaFluidEquivalence.sameSubstance(tankA, ModFluids.NONE.getSource())) return null;
        if (VanillaFluidEquivalence.sameSubstance(tankB, ModFluids.NONE.getSource())) return null;

        for (MixerRecipe recipe : RECIPES) {
            if (recipe.matches(tankA, tankB)) {
                return recipe;
            }
        }
        return null;
    }

    public static List<MixerRecipe> getAll() {
        return List.copyOf(RECIPES);
    }

    private static void add(Fluid inputA, int amountA, Fluid inputB, int amountB,
                             Fluid output, int outputAmount, int duration, long energyPerTick) {
        RECIPES.add(new MixerRecipe(inputA, amountA, inputB, amountB, output, outputAmount, duration, energyPerTick));
    }

    static {
        // Sulfuric Acid: Vitriol + Water -> Sulfuric Acid
        // Direct port of the original HBM vitriol-process recipe.
        add(ModFluids.VITRIOL.getSource(), 1000,
            ModFluids.WATER.getSource(), 1000,
            ModFluids.SULFURIC_ACID.getSource(), 2000,
            100, 50L);

        // Biofuel: Ethanol + Sunflower Oil -> Biofuel
        // Transesterification of plant oil with ethanol yields biodiesel/biofuel.
        add(ModFluids.ETHANOL.getSource(), 1000,
            ModFluids.SUNFLOWEROIL.getSource(), 1000,
            ModFluids.BIOFUEL.getSource(), 2000,
            100, 50L);

        // Nitroglycerin: Nitric Acid + Peroxide -> Nitroglycerin
        // Simplified nitration step using fluids available in this codebase.
        add(ModFluids.NITRIC_ACID.getSource(), 1000,
            ModFluids.PEROXIDE.getSource(), 1000,
            ModFluids.NITROGLYCERIN.getSource(), 1000,
            200, 100L);

        // Chlorocalcite Mix: Calcium Solution + Chlorocalcite Solution -> Chlorocalcite Mix
        // Part of the calcium chloride / lithium extraction processing chain.
        add(ModFluids.CALCIUM_SOLUTION.getSource(), 1000,
            ModFluids.CHLOROCALCITE_SOLUTION.getSource(), 1000,
            ModFluids.CHLOROCALCITE_MIX.getSource(), 2000,
            150, 75L);
    }
}
