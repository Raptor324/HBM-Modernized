package com.hbm_m.entity.train;

import com.hbm_m.platform.StackNbt;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code EntityRailCarCargo}: Waggon mit Inventar; die Zahl belegter Plaetze geht an den Client (Ladungsanzeige). */
public abstract class EntityRailCarCargo extends EntityRailCarBase implements Container {

    /** Original DataWatcher 10 */
    protected static final EntityDataAccessor<Integer> OCCUPIED_SLOTS = SynchedEntityData.defineId(EntityRailCarCargo.class, EntityDataSerializers.INT);

    protected ItemStack[] slots;

    public EntityRailCarCargo(EntityType<?> type, Level world) {
        super(type, world);
        this.slots = emptySlots();
    }

    private ItemStack[] emptySlots() {
        ItemStack[] s = new ItemStack[this.getContainerSize()];
        java.util.Arrays.fill(s, ItemStack.EMPTY);
        return s;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OCCUPIED_SLOTS, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OCCUPIED_SLOTS, 0);
    }
    *///?}

    public int getOccupiedSlots() {
        return this.entityData.get(OCCUPIED_SLOTS);
    }

    public int countOccupiedSlots() {
        int slots = 0;

        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!this.getItem(i).isEmpty()) slots++;
        }

        return slots;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slots[slot];
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (!this.slots[slot].isEmpty()) {
            ItemStack itemstack;

            if (this.slots[slot].getCount() <= amount) {
                itemstack = this.slots[slot];
                this.slots[slot] = ItemStack.EMPTY;
                return itemstack;
            } else {
                itemstack = this.slots[slot].split(amount);

                if (this.slots[slot].getCount() == 0) {
                    this.slots[slot] = ItemStack.EMPTY;
                }

                return itemstack;
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (!this.slots[slot].isEmpty()) {
            ItemStack itemstack = this.slots[slot];
            this.slots[slot] = ItemStack.EMPTY;
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        this.slots[slot] = stack;

        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) this.entityData.set(OCCUPIED_SLOTS, this.countOccupiedSlots());
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : slots) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public void setChanged() { }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !this.isRemoved() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void clearContent() {
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        ListTag nbttaglist = new ListTag();

        for (int i = 0; i < this.slots.length; ++i) {
            if (!this.slots[i].isEmpty()) {
                CompoundTag nbttagcompound1 = new CompoundTag();
                nbttagcompound1.putByte("Slot", (byte) i);
                StackNbt.save(this.slots[i], nbttagcompound1);
                nbttaglist.add(nbttagcompound1);
            }
        }

        nbt.put("Items", nbttaglist);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ListTag nbttaglist = nbt.getList("Items", 10);
        this.slots = emptySlots();

        for (int i = 0; i < nbttaglist.size(); ++i) {
            CompoundTag nbttagcompound1 = nbttaglist.getCompound(i);
            int j = nbttagcompound1.getByte("Slot") & 255;

            if (j >= 0 && j < this.slots.length) {
                this.slots[j] = StackNbt.parse(nbttagcompound1);
            }
        }

        this.entityData.set(OCCUPIED_SLOTS, this.countOccupiedSlots());
    }

    /** Original {@code getInventoryName}: Sprachschluessel des Inventars. */
    public abstract String getInventoryName();
}
