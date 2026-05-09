package com.hbm_m.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

//? if forge {
/*import net.minecraftforge.items.ItemStackHandler;

public abstract class ModItemStackHandler extends ItemStackHandler {

    public ModItemStackHandler(int size) {
        super(size);
    }
}
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.ArrayList;
import java.util.List;

public abstract class ModItemStackHandler {

    protected final ItemStack[] stacks;

    public ModItemStackHandler(int size) {
        this.stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) stacks[i] = ItemStack.EMPTY;
    }

    public int getSlots() {
        return stacks.length;
    }

    public @NotNull ItemStack getStackInSlot(int slot) {
        validateSlot(slot);
        return stacks[slot];
    }

    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        validateSlot(slot);
        stacks[slot] = stack;
        onContentsChanged(slot);
    }

    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        validateSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!isItemValid(slot, stack)) return stack;

        ItemStack existing = stacks[slot];
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());

        if (!existing.isEmpty()) {
            if (!isSameItem(existing, stack)) return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0) return stack;

        int toInsert = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) {
                stacks[slot] = stack.copyWithCount(toInsert);
            } else {
                stacks[slot].grow(toInsert);
            }
            onContentsChanged(slot);
        }
        return toInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toInsert);
    }

    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlot(slot);
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = stacks[slot];
        if (existing.isEmpty()) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getCount());
        ItemStack extracted = existing.copyWithCount(toExtract);
        if (!simulate) {
            existing.shrink(toExtract);
            if (existing.isEmpty()) stacks[slot] = ItemStack.EMPTY;
            onContentsChanged(slot);
        }
        return extracted;
    }

    public int getSlotLimit(int slot) {
        return 64;
    }

    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    protected abstract void onContentsChanged(int slot);

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= stacks.length)
            throw new RuntimeException("Slot " + slot + " not in valid range [0, " + stacks.length + ")");
    }

    private static boolean isSameItem(ItemStack a, ItemStack b) {
        return a.is(b.getItem()) && ItemStack.isSameItemSameTags(a, b);
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        ListTag list = new ListTag();
        for (int i = 0; i < stacks.length; i++) {
            if (!stacks[i].isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                stacks[i].save(entry);
                list.add(entry);
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", list);
        tag.putInt("Size", stacks.length);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        for (int i = 0; i < stacks.length; i++) stacks[i] = ItemStack.EMPTY;
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot < stacks.length) stacks[slot] = ItemStack.of(entry);
        }
    }

    // ── Fabric Transfer API ───────────────────────────────────────────────────

    public Storage<ItemVariant> getStorage() {
        List<SingleSlotStorage<ItemVariant>> slots = new ArrayList<>();
        for (int i = 0; i < stacks.length; i++) slots.add(getSlotStorage(i));
        return new CombinedStorage<>(slots);
    }

    public SingleSlotStorage<ItemVariant> getSlotStorage(int slot) {
        return new SlotStorage(slot);
    }

    @SuppressWarnings("UnstableApiUsage")
    private final class SlotStorage extends SnapshotParticipant<ItemStack>
            implements SingleSlotStorage<ItemVariant> {

        private final int slot;

        SlotStorage(int slot) {
            this.slot = slot;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
            ItemStack incoming = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
            if (!isItemValid(slot, incoming)) return 0;

            ItemStack existing = stacks[slot];
            int limit = Math.min(getSlotLimit(slot), resource.getItem().getMaxStackSize());
            if (!existing.isEmpty()) {
                if (!resource.matches(existing)) return 0;
                limit -= existing.getCount();
            }
            if (limit <= 0) return 0;
            int toInsert = (int) Math.min(maxAmount, limit);

            updateSnapshots(tx);
            if (existing.isEmpty()) {
                stacks[slot] = resource.toStack(toInsert);
            } else {
                stacks[slot].grow(toInsert);
            }
            return toInsert;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
            ItemStack existing = stacks[slot];
            if (existing.isEmpty() || !resource.matches(existing)) return 0;
            int toExtract = (int) Math.min(maxAmount, existing.getCount());
            updateSnapshots(tx);
            existing.shrink(toExtract);
            if (existing.isEmpty()) stacks[slot] = ItemStack.EMPTY;
            return toExtract;
        }

        @Override public boolean isResourceBlank() { return stacks[slot].isEmpty(); }
        @Override public ItemVariant getResource()  { return ItemVariant.of(stacks[slot]); }
        @Override public long getAmount()            { return stacks[slot].getCount(); }
        @Override public long getCapacity()          { return getSlotLimit(slot); }

        @Override protected ItemStack createSnapshot()           { return stacks[slot].copy(); }
        @Override protected void readSnapshot(ItemStack snapshot){ stacks[slot] = snapshot; }
        @Override protected void onFinalCommit()                 { onContentsChanged(slot); }
    }
}
//?}