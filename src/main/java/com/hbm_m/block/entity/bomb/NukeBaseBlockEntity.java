package com.hbm_m.block.entity.bomb;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class NukeBaseBlockEntity extends BlockEntity implements WorldlyContainer, Nameable, MenuProvider {

    public final List<ItemStack> slots;
    @Nullable
    private Component customName;

    public NukeBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.slots = new ArrayList<>();
        for (int i = 0; i < size; i++) this.slots.add(ItemStack.EMPTY);
    }

    public abstract boolean isReady();
    public boolean isFilled() { return false; }

    @Override
    public Component getName() {
        return customName != null ? customName : getDefaultName();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    public Component getCustomName() { return customName; }

    protected abstract Component getDefaultName();

    @Override
    public int getContainerSize() { return slots.size(); }

    @Override
    public ItemStack getItem(int index) { return slots.get(index); }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = slots.get(index);
        slots.set(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        slots.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
    }

    @Override
    public int getMaxStackSize() { return 64; }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 128;
    }

    @Override
    public void startOpen(Player player) {}
    @Override
    public void stopOpen(Player player) {}

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return false; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = slots.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.getCount() <= amount) {
            slots.set(slot, ItemStack.EMPTY);
            return stack;
        }
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) slots.set(slot, ItemStack.EMPTY);
        return split;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) { return true; }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        int n = slots.size();
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = i;
        return out;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ListTag list = tag.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            byte b = itemTag.getByte("Slot");
            if (b >= 0 && b < slots.size()) {
                slots.set(b, ItemStack.of(itemTag));
            }
        }
        if (tag.contains("CustomName", 8)) {
            try {
                customName = Component.Serializer.fromJson(com.google.gson.JsonParser.parseString(tag.getString("CustomName")));
            } catch (Exception e) {
                customName = null;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                list.add(itemTag);
            }
        }
        tag.put("Items", list);
        if (customName != null) {
            tag.putString("CustomName", net.minecraft.network.chat.Component.Serializer.toJson(customName));
        }
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
    }
}
