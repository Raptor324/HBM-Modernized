package com.hbm_m.energy;

import net.minecraft.world.item.ItemStack;

// (Импортируй свой новый интерфейс)
import com.hbm_m.energy.ILongEnergyStorage;

public class ItemEnergyStorage implements ILongEnergyStorage { // <-- МЕНЯЕМ ИНТЕРФЕЙС
    private final ItemStack stack;
    private final long capacity; // <-- long
    private final long maxReceive; // <-- long
    private final long maxExtract; // <-- long

    public ItemEnergyStorage(ItemStack stack, long capacity, long maxReceive, long maxExtract) {
        this.stack = stack;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public void setEnergy(long energy) { // <-- long
        this.stack.getOrCreateTag().putLong("energy", energy); // <-- putLong
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) { // <-- long
        long energy = getEnergyStored();
        long energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive)); // <-- long
        if (energyReceived < 0) {
            energyReceived = 0;
        }
        if (!simulate) {
            setEnergy(energy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) { // <-- long
        long energy = getEnergyStored();
        long energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract)); // <-- long
        if (energyExtracted < 0) {
            energyExtracted = 0;
        }
        if (!simulate) {
            setEnergy(energy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public long getEnergyStored() { // <-- long
        return this.stack.hasTag() ? this.stack.getTag().getLong("energy") : 0L; // <-- getLong
    }

    @Override
    public long getMaxEnergyStored() { // <-- long
        return this.capacity;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }
}