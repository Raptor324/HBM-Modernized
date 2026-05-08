package com.hbm_m.api.fluids;

import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * HBM registers its own water/lava types; vanilla buckets and many pipes use {@link Fluids#WATER} / {@link Fluids#LAVA}.
 * Treats those pairs as the same substance for tank I/O and load/unload slots.
 */
public final class VanillaFluidEquivalence {

    private VanillaFluidEquivalence() {}

    /**
     * Нельзя использовать {@code ModFluids.WATER.getSource()} и т.п.: пока DeferredRegister не привязан,
     * Architectury кидает NPE (до регистров жидкостей вызывает {@link FluidDuctBlockEntity#onLoad} и т.д.).
     */
    private static boolean isHbmPlainWaterFluid(Fluid f) {
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(f);
        if (key == null || !RefStrings.MODID.equals(key.getNamespace())) return false;
        String p = key.getPath();
        return "water".equals(p) || "water_flowing".equals(p);
    }

    private static boolean isHbmPlainLavaFluid(Fluid f) {
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(f);
        if (key == null || !RefStrings.MODID.equals(key.getNamespace())) return false;
        String p = key.getPath();
        return "lava".equals(p) || "lava_flowing".equals(p);
    }

    public static boolean isWater(Fluid f) {
        if (f == null || f == Fluids.EMPTY) return false;
        // Vanilla
        if (f == Fluids.WATER || f == Fluids.FLOWING_WATER) return true;
        // HBM аналоги vanilla water (регистр: water / water_flowing)
        return isHbmPlainWaterFluid(f);
    }

    public static boolean isLava(Fluid f) {
        if (f == null || f == Fluids.EMPTY) return false;
        // Vanilla
        if (f == Fluids.LAVA || f == Fluids.FLOWING_LAVA) return true;
        return isHbmPlainLavaFluid(f);
    }

    /** Same fluid registry entry, or both are the water (or both lava) family. */
    public static boolean sameSubstance(Fluid a, Fluid b) {
        if (a == b) return true;
        if (isWater(a) && isWater(b)) return true;
        if (isLava(a) && isLava(b)) return true;
        return false;
    }

    /**
     * Fluid to put in a {@link net.minecraftforge.fluids.FluidStack} when filling vanilla-style item handlers (buckets)
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
