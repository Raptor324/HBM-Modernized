package com.hbm_m.api.fluids;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** 1:1 {@code api.hbm.fluidmk2.IFillableItem}; der Fluessigkeitstyp ist im Port die Quell-{@link Fluid}. */
public interface IFillableItem {

    /** Whether this stack can be filled with this type. Not particularly useful for normal operations */
    boolean acceptsFluid(Fluid type, ItemStack stack);
    /** Tries to fill the stack, returns the remainder that couldn't be added */
    int tryFill(Fluid type, int amount, ItemStack stack);
    /** Whether this stack can fill tiles with this type. Not particularly useful for normal operations */
    boolean providesFluid(Fluid type, ItemStack stack);
    /** Provides fluid with the maximum being the requested amount */
    int tryEmpty(Fluid type, int amount, ItemStack stack);
    /** Returns the first (or only) corrently held type, may return null. Currently only used for setting bedrock ores */
    @Nullable Fluid getFirstFluidType(ItemStack stack);
    /** Returns the fillstate for the specified fluid. Currently only used for setting bedrock ores */
    int getFill(ItemStack stack);
}
