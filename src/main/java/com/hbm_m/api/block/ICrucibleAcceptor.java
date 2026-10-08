package com.hbm_m.api.block;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.Mats.MaterialStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code api.hbm.block.ICrucibleAcceptor}: nimmt fluessiges Material auf - durch Giessen von oben (mit genauem
 * Auftreffpunkt) oder seitliches Fliessen. Im Original vom Block implementiert, im Port vom Block-Entity (bei
 * Multiblocks vom Kern). Rueckgabe ist jeweils der Rest, {@code null} wenn alles aufgenommen wurde.
 */
public interface ICrucibleAcceptor {

    /*
     * Pouring: The metal leaves the channel/crucible and usually (but not always) falls down. The additional double coords give a more precise impact location.
     * Also useful for entities like large crucibles since they are filled from the top.
     */
    boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack);
    @Nullable MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack);

    /*
     * Flowing: The "safe" transfer of metal using a channel or other means, usually from block to block and usually horizontally (but not necessarily).
     */
    boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, MaterialStack stack);
    @Nullable MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack);
}
