package com.hbm_m.api.fluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * HBM registers its own water/lava types; vanilla buckets and many pipes use {@link Fluids#WATER} / {@link Fluids#LAVA}.
 * Treats those pairs as the same substance for tank I/O and load/unload slots.
 */
public final class VanillaFluidEquivalence {

    private VanillaFluidEquivalence() {}

    public static boolean isWater(Fluid f) {
        if (f == null || f == Fluids.EMPTY) return false;
        return f == Fluids.WATER || f == Fluids.FLOWING_WATER
            || f == ModFluids.WATER.getSource() || f == ModFluids.WATER.getFlowing();
    }

    public static boolean isLava(Fluid f) {
        if (f == null || f == Fluids.EMPTY) return false;
        return f == Fluids.LAVA || f == Fluids.FLOWING_LAVA
            || f == ModFluids.LAVA.getSource() || f == ModFluids.LAVA.getFlowing();
    }

    /** Same fluid registry entry, or both are the water (or both lava) family. */
    public static boolean sameSubstance(Fluid a, Fluid b) {
        if (a == b) return true;
        if (isWater(a) && isWater(b)) return true;
        if (isLava(a) && isLava(b)) return true;
        return false;
    }

    /**
     * Fluid to put in a {@link FluidStack} when filling vanilla-style item handlers (buckets)
     * from a tank that stores the HBM registered variant.
     */
    public static Fluid forVanillaContainerFill(Fluid tankType) {
        if (isWater(tankType)) {
            return Fluids.WATER;
        }
        if (isLava(tankType)) {
            return Fluids.LAVA;
        }
        return tankType;
    }
}
