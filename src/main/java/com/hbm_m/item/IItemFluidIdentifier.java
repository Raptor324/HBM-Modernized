package com.hbm_m.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * Interface for items that identify a fluid type (e.g. fluid identifier).
 * Used by machines to set tank type from the held/inserted identifier.
 */
public interface IItemFluidIdentifier {

    /**
     * Returns the fluid type this identifier is set to.
     * Level and pos may be null when used in GUI.
     */
    Fluid getType(Level level, BlockPos pos, ItemStack stack);
}
