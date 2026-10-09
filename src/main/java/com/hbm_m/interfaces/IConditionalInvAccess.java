package com.hbm_m.interfaces;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Original {@code IConditionalInvAccess}: Maschinen, deren Inventarzugriff von der angesprochenen Anschlusszelle
 * abhaengt. Die Anschlusszellen ({@code UniversalMachinePartBlockEntity}) fragen den Kern mit ihrer eigenen Position.
 */
public interface IConditionalInvAccess {

    //? if forge {
    /** Item-Handler fuer genau diese Zelle und Seite; {@code null} heisst: kein Zugriff. */
    @Nullable
    net.minecraftforge.items.IItemHandler getConditionalItemHandler(BlockPos part, @Nullable Direction side);
    //?} elif neoforge {
    /*/^* Item-Handler fuer genau diese Zelle und Seite; {@code null} heisst: kein Zugriff. ^/
    @Nullable
    net.neoforged.neoforge.items.IItemHandler getConditionalItemHandler(BlockPos part, @Nullable Direction side);
    *///?}
}
