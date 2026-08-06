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
        // Sulfuric Acid and Chlorocalcite Mix in the original both require a solid ingredient
        // (S.dust() / powder_flux) that this port's fluid-only MixerRecipe can't represent -
        // both are already producible via other machines (Crystallizer / Fraction Tower), so
        // they're skipped here rather than approximated with fabricated fluid substitutes.

        // Biofuel: Fish Oil + Wood Oil -> Biofuel, Sunflower Oil + Wood Oil -> Biofuel
        // Ported 1:1 from original MixerRecipes.java (register(Fluids.BIOFUEL, ...)).
        add(ModFluids.FISHOIL.getSource(), 500,
            ModFluids.WOODOIL.getSource(), 500,
            ModFluids.BIOFUEL.getSource(), 250,
            20, 50L);
        add(ModFluids.SUNFLOWEROIL.getSource(), 500,
            ModFluids.WOODOIL.getSource(), 500,
            ModFluids.BIOFUEL.getSource(), 200,
            20, 50L);

        // Nitroglycerin: Petroleum + Nitric Acid -> Nitroglycerin, Fish Oil + Nitric Acid -> Nitroglycerin
        // Ported 1:1 from original MixerRecipes.java (register(Fluids.NITROGLYCERIN, ...)).
        add(ModFluids.PETROLEUM.getSource(), 1000,
            ModFluids.NITRIC_ACID.getSource(), 1000,
            ModFluids.NITROGLYCERIN.getSource(), 1000,
            20, 50L);
        add(ModFluids.FISHOIL.getSource(), 500,
            ModFluids.NITRIC_ACID.getSource(), 500,
            ModFluids.NITROGLYCERIN.getSource(), 1000,
            20, 50L);

        // ---------------------------------------------------------------------------------
        // Additional recipes ported from the original 1.7.10 MixerRecipes.java.
        // Only fluid+fluid recipes are portable here: this port's MixerRecipe record has no
        // solid-input slot, so every original recipe involving setSolid(...) was skipped.
        // ---------------------------------------------------------------------------------

        // Fracksol: Sulfuric Acid + Petroleum -> Fracksol
        add(ModFluids.SULFURIC_ACID.getSource(), 900,
            ModFluids.PETROLEUM.getSource(), 100,
            ModFluids.FRACKSOL.getSource(), 1000,
            20, 50L);

        // Salient: Seed Slurry + Blood -> Salient
        add(ModFluids.SEEDSLURRY.getSource(), 500,
            ModFluids.BLOOD.getSource(), 500,
            ModFluids.SALIENT.getSource(), 1000,
            20, 50L);

        // Phosgene: Unsaturateds + Chlorine -> Phosgene
        add(ModFluids.UNSATURATEDS.getSource(), 500,
            ModFluids.CHLORINE.getSource(), 500,
            ModFluids.PHOSGENE.getSource(), 1000,
            20, 50L);

        // Solvent: Naphtha + Aromatics -> Solvent (and its cracked/DS/coker variants)
        add(ModFluids.NAPHTHA.getSource(), 500,
            ModFluids.AROMATICS.getSource(), 500,
            ModFluids.SOLVENT.getSource(), 1000,
            50, 50L);
        add(ModFluids.NAPHTHA_CRACK.getSource(), 500,
            ModFluids.AROMATICS.getSource(), 500,
            ModFluids.SOLVENT.getSource(), 1000,
            50, 50L);
        add(ModFluids.NAPHTHA_DS.getSource(), 500,
            ModFluids.AROMATICS.getSource(), 500,
            ModFluids.SOLVENT.getSource(), 1000,
            50, 50L);
        add(ModFluids.NAPHTHA_COKER.getSource(), 500,
            ModFluids.AROMATICS.getSource(), 500,
            ModFluids.SOLVENT.getSource(), 1000,
            50, 50L);

        // Radiosolvent: Reformgas + Chlorine -> Radiosolvent
        add(ModFluids.REFORMGAS.getSource(), 750,
            ModFluids.CHLORINE.getSource(), 250,
            ModFluids.RADIOSOLVENT.getSource(), 1000,
            50, 50L);

        // Petroil: Reclaimed Oil + Lubricant -> Petroil
        add(ModFluids.RECLAIMED.getSource(), 800,
            ModFluids.LUBRICANT.getSource(), 200,
            ModFluids.PETROIL.getSource(), 1000,
            30, 50L);

        // Lubricant: Heating Oil + Unsaturateds -> Lubricant (and oil/ethanol variants)
        add(ModFluids.HEATINGOIL.getSource(), 500,
            ModFluids.UNSATURATEDS.getSource(), 500,
            ModFluids.LUBRICANT.getSource(), 1000,
            20, 50L);
        add(ModFluids.FISHOIL.getSource(), 800,
            ModFluids.ETHANOL.getSource(), 200,
            ModFluids.LUBRICANT.getSource(), 1000,
            20, 50L);
        add(ModFluids.SUNFLOWEROIL.getSource(), 800,
            ModFluids.ETHANOL.getSource(), 200,
            ModFluids.LUBRICANT.getSource(), 1000,
            20, 50L);

        // Biofuel: Fish Oil / Sunflower Oil + Wood Oil -> Biofuel
        add(ModFluids.FISHOIL.getSource(), 500,
            ModFluids.WOODOIL.getSource(), 500,
            ModFluids.BIOFUEL.getSource(), 250,
            20, 50L);
        add(ModFluids.SUNFLOWEROIL.getSource(), 500,
            ModFluids.WOODOIL.getSource(), 500,
            ModFluids.BIOFUEL.getSource(), 200,
            20, 50L);

        // Nitroglycerin: Petroleum / Fish Oil + Nitric Acid -> Nitroglycerin
        add(ModFluids.PETROLEUM.getSource(), 1000,
            ModFluids.NITRIC_ACID.getSource(), 1000,
            ModFluids.NITROGLYCERIN.getSource(), 1000,
            20, 50L);
        add(ModFluids.FISHOIL.getSource(), 500,
            ModFluids.NITRIC_ACID.getSource(), 500,
            ModFluids.NITROGLYCERIN.getSource(), 1000,
            20, 50L);

        // Syngas: Coal Oil + Steam -> Syngas
        add(ModFluids.COALOIL.getSource(), 500,
            ModFluids.STEAM.getSource(), 500,
            ModFluids.SYNGAS.getSource(), 1000,
            50, 50L);

        // Oxyhydrogen: Hydrogen + Air / Oxygen -> Oxyhydrogen
        add(ModFluids.HYDROGEN.getSource(), 500,
            ModFluids.AIR.getSource(), 2000,
            ModFluids.OXYHYDROGEN.getSource(), 1000,
            50, 50L);
        add(ModFluids.HYDROGEN.getSource(), 500,
            ModFluids.OXYGEN.getSource(), 500,
            ModFluids.OXYHYDROGEN.getSource(), 1000,
            50, 50L);

        // Reformed fuels: Diesel/Diesel Crack/Kerosene + Reformate -> respective reformed fuel
        add(ModFluids.DIESEL.getSource(), 900,
            ModFluids.REFORMATE.getSource(), 100,
            ModFluids.DIESEL_REFORM.getSource(), 1000,
            50, 50L);
        add(ModFluids.DIESEL_CRACK.getSource(), 900,
            ModFluids.REFORMATE.getSource(), 100,
            ModFluids.DIESEL_CRACK_REFORM.getSource(), 1000,
            50, 50L);
        add(ModFluids.KEROSENE.getSource(), 900,
            ModFluids.REFORMATE.getSource(), 100,
            ModFluids.KEROSENE_REFORM.getSource(), 1000,
            50, 50L);
    }
}
