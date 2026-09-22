package com.hbm_m.interfaces;

import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;

/**
 * Block entities that persist absolute world positions of other blocks (their core, their ports).
 * A contraption engine restores the entity from NBT at the new position but leaves those
 * references at the old coordinates; the engine glue ({@code SubLevelRelocateMixin} for Sable)
 * calls this with the engine's own block transform right after the NBT load. Implementations map
 * every stored position through it - the transform already includes the rotation.
 */
public interface IRelocatable {

    void relocate(UnaryOperator<BlockPos> transform);
}
