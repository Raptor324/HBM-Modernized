package com.hbm_m.block;

import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;

/**
 * audit10: gemeinsamer Ersatz fuer die {@code breakBlock}-Schleifen des Originals ({@code dropContents(start, end)}):
 * die Slots [start, end) fallen beim Entfernen des Blocks als Gegenstaende heraus.
 */
public final class BlockDropUtil {

    private BlockDropUtil() {}

    public static void dropSlots(Level level, BlockPos pos, ModItemStackHandler inv, int start, int end) {
        if (level == null || level.isClientSide || inv == null) return;
        // Wie BaseMachineBlockEntity#dropInventoryContents: beim Verschieben durch Create/Sable nicht verschuetten (Dupe)
        if (com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) return;
        int to = Math.min(end, inv.getSlots());
        SimpleContainer c = new SimpleContainer(Math.max(0, to - start));
        for (int i = start; i < to; i++) {
            c.setItem(i - start, inv.getStackInSlot(i).copy());
        }
        Containers.dropContents(level, pos, c);
    }
}
