package com.hbm_m.interfaces;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;

/**
 * Original {@code TileEntityProxyDyn.IProxyDelegateProvider} (Fluidanteil): der Kern liefert fuer eine
 * bestimmte Anschlusszelle einen eigenen Fluid-Teilnehmer (z.B. nur die Kuehltanks der Chemiefabrik).
 * Die Anschlusszellen ({@code UniversalMachinePartBlockEntity}) fragen mit ihrer eigenen Position;
 * {@code null} heisst: der Kern selbst.
 */
public interface IFluidPartDelegateProvider {

    /** Ein {@code IFluidUserMK2} (Sender/Empfaenger) fuer diese Zelle oder {@code null}. */
    @Nullable
    Object getFluidDelegateForPart(BlockPos partPos);
}
