package com.hbm_m.inventory;

import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Vanilla {@link Container} адаптер поверх {@link ModItemStackHandler}.
 *
 * Нужен, чтобы общие {@link net.minecraft.world.inventory.AbstractContainerMenu} могли
 * использовать обычные {@link net.minecraft.world.inventory.Slot} без Forge-only {@code SlotItemHandler}.
 */
public final class ModItemStackHandlerContainer implements Container {
    private final ModItemStackHandler handler;
    private final Runnable onDirty;

    public ModItemStackHandlerContainer(ModItemStackHandler handler, Runnable onDirty) {
        this.handler = handler;
        this.onDirty = (onDirty != null) ? onDirty : () -> {};
    }

    @Override
    public int getContainerSize() {
        return handler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack extracted = handler.extractItem(slot, amount, false);
        if (!extracted.isEmpty()) onDirty.run();
        return extracted;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack existing = handler.getStackInSlot(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        handler.setStackInSlot(slot, ItemStack.EMPTY);
        onDirty.run();
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack);
        onDirty.run();
    }

    @Override
    public void setChanged() {
        onDirty.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
        onDirty.run();
    }
}

