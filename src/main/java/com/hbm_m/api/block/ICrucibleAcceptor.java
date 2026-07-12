package com.hbm_m.api.block;

import com.hbm_m.inventory.material.MaterialStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 api.hbm.block.ICrucibleAcceptor.
 * Implemented by block entities that can receive molten material, either by
 * sideways flowing (channels, outlets) or by being poured into from above
 * (basins, channels, crucibles).
 *
 * flow/pour return the leftover stack, or null if everything was accepted.
 */
public interface ICrucibleAcceptor {

    /** Whether material may flow in sideways from the given side. */
    boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack);

    /** Sideways flow. Returns the leftover, or null if fully accepted. */
    @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack);

    /** Whether material may be poured in from above (side is usually UP). */
    boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack);

    /** Pour from above. Returns the leftover, or null if fully accepted. */
    @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack);
}
