package com.hbm_m.inventory;

import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraftforge.items.IItemHandlerModifiable;
//?}

/**
 * Forge-side adapter for {@link com.hbm_m.inventory.ItemStackHandler}.
 *
 * Common code stores inventories in a loader-agnostic handler; Forge menus/slots
 * (e.g. {@code SlotItemHandler}) require {@code IItemHandler}.
 */
//? if forge {
public final class ForgeItemHandlerAdapter implements IItemHandlerModifiable {

    private final com.hbm_m.inventory.ItemStackHandler delegate;

    public ForgeItemHandlerAdapter(com.hbm_m.inventory.ItemStackHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return delegate.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        delegate.setStackInSlot(slot, stack);
    }
}
//?} else {
/*public final class ForgeItemHandlerAdapter {
    public ForgeItemHandlerAdapter(com.hbm_m.inventory.ItemStackHandler delegate) {
    }
}*///?}

