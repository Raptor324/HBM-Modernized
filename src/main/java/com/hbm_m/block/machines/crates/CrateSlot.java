package com.hbm_m.block.machines.crates;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * Слот для ящика. Оригинал ({@code TileEntityCrateBase.isItemValidForSlot}) принимает
 * любой предмет, включая другие ящики.
 */
public class CrateSlot extends Slot {

    public CrateSlot(Container container, int index, int xPosition, int yPosition) {
        super(container, index, xPosition, yPosition);
    }
}
