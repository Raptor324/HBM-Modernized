package com.hbm_m.inventory.fluid.trait;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.material.Fluid;

public class FluidTraitManager {
    private static final Map<Fluid, Map<Class<? extends FluidTrait>, FluidTrait>> FLUID_TRAITS = new HashMap<>();

    public static void addTrait(Fluid fluid, FluidTrait trait) {
        FLUID_TRAITS.computeIfAbsent(fluid, k -> new HashMap<>()).put(trait.getClass(), trait);
    }

    @SuppressWarnings("unchecked")
    public static <T extends FluidTrait> T getTrait(Fluid fluid, Class<T> traitClass) {
        if (!FLUID_TRAITS.containsKey(fluid)) return null;
        return (T) FLUID_TRAITS.get(fluid).get(traitClass);
    }

    public static boolean hasTrait(Fluid fluid, Class<? extends FluidTrait> traitClass) {
        return FLUID_TRAITS.containsKey(fluid) && FLUID_TRAITS.get(fluid).containsKey(traitClass);
    }
}