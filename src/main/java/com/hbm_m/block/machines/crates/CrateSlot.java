package com.hbm_m.block.machines.crates;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

/**
 * Слот для ящика с валидацией: запрещает класть другие ящики внутрь.
 */
public class CrateSlot extends Slot {

    public CrateSlot(Container container, int index, int xPosition, int yPosition) {
        super(container, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return CrateValidation.isValidForCrate(stack) && super.mayPlace(stack);
    }
}
