package com.hbm_m.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Loader-agnostic replacement for Forge's ItemStackHandler.
 *
 * This class exists to keep machine inventories in common code without depending on
 * Forge-only item handler/capability APIs.
 */
public class ItemStackHandler {

    private final NonNullList<ItemStack> stacks;

    public ItemStackHandler(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public int getSlots() {
        return stacks.size();
    }

    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        int extracted = Math.min(amount, existing.getCount());
        ItemStack result = existing.copy();
        result.setCount(extracted);

        if (!simulate) {
            ItemStack remainder = existing.copy();
            remainder.shrink(extracted);
            stacks.set(slot, remainder.isEmpty() ? ItemStack.EMPTY : remainder);
            onContentsChanged(slot);
        }
        return result;
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack existing = stacks.get(slot);
        if (!isItemValid(slot, stack)) return stack;

        // Minimal semantics: merge if same item+tags, otherwise replace only if empty.
        if (existing.isEmpty()) {
            if (!simulate) {
                stacks.set(slot, stack.copy());
                onContentsChanged(slot);
            }
            return ItemStack.EMPTY;
        }

        if (!ItemStack.isSameItemSameTags(existing, stack)) return stack;

        int maxStackSize = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize());
        int space = maxStackSize - existing.getCount();
        if (space <= 0) return stack;

        int toMove = Math.min(space, stack.getCount());
        if (!simulate) {
            existing.grow(toMove);
            onContentsChanged(slot);
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(toMove);
        return remainder;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Items", ContainerHelper.saveAllItems(new CompoundTag(), stacks).getList("Items", Tag.TAG_COMPOUND));
        tag.putInt("Size", stacks.size());
        return tag;
    }

    public int getSlotLimit(int slot) {
        ItemStack stack = stacks.get(slot);
        return stack.isEmpty() ? 64 : stack.getMaxStackSize();
    }

    public void deserializeNBT(CompoundTag tag) {
        stacks.clear();
        int size = tag.contains("Size") ? tag.getInt("Size") : stacks.size();
        for (int i = 0; i < size; i++) stacks.add(ItemStack.EMPTY);

        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        ContainerHelper.loadAllItems(tagFromList(items), stacks);
    }

    private static CompoundTag tagFromList(ListTag list) {
        CompoundTag t = new CompoundTag();
        t.put("Items", list);
        return t;
    }

    protected void onContentsChanged(int slot) {
        // override hook
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}

