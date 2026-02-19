package com.hbm_m.block.custom.machines.crates;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Слот для ящика с валидацией: запрещает класть другие ящики внутрь.
 */
public class CrateSlot extends SlotItemHandler {

    public CrateSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return CrateValidation.isValidForCrate(stack) && super.mayPlace(stack);
    }
}
