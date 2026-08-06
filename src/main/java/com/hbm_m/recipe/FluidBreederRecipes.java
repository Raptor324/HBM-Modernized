package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fluid-based breeder recipe registry - port of the 1.7.10 {@code com.hbm.inventory.recipes.FluidBreederRecipes}.
 * In the original mod this class exists but is never actually consumed by
 * {@code TileEntityMachineReactorBreeding} (only {@code TileEntityFusionBreeder}, the separate Fusion
 * Breeder, calls fluid-irradiation code, and even that class's own comment admits "i forgot what fluid
 * irradiation was even supposed to be for"). Since this port's {@code MachineBreederBlockEntity} already
 * has a {@code FluidTank} and fluid input/output item slots wired up in its menu/GUI, this registry is
 * used to give that tank a real purpose: in-place fluid-to-fluid conversion, mirroring the original's
 * defaults 1:1 (all three registered fluids already exist in this port's {@link ModFluids}).
 */
public class FluidBreederRecipes {

    public static final class FluidBreederRecipe {
        public final int amount;
        public final FluidStack output;

        public FluidBreederRecipe(int amount, FluidStack output) {
            this.amount = amount;
            this.output = output;
        }
    }

    private static final Map<Fluid, FluidBreederRecipe> RECIPES = new HashMap<>();

    public static void registerRecipes() {
        RECIPES.clear();
        register(ModFluids.GAS, 1_000, ModFluids.SYNGAS, 1_000);
        register(ModFluids.LIGHTOIL, 1_000, ModFluids.REFORMGAS, 1_000);
        register(ModFluids.LIGHTOIL_CRACK, 1_000, ModFluids.REFORMGAS, 1_000);
    }

    private static void register(ModFluids.FluidEntry input, int inputAmount, ModFluids.FluidEntry output, int outputAmount) {
        RECIPES.put(input.getSource(), new FluidBreederRecipe(inputAmount, new FluidStack(output.getSource(), outputAmount)));
    }

    public static FluidBreederRecipe getOutput(Fluid fluid) {
        if (fluid == null) return null;
        return RECIPES.get(fluid);
    }
}
