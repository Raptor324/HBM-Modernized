package com.hbm_m.energy;

import net.minecraft.world.item.ItemStack;


public class ItemEnergyStorage implements ILongEnergyStorage { // <-- МЕНЯЕМ ИНТЕРФЕЙС
    private final ItemStack stack;
    private final long capacity; // <-- long
    private final long maxReceive; // <-- long
    private final long maxExtract; // <-- long
    private final Object stackLock = new Object();

    public ItemEnergyStorage(ItemStack stack, long capacity, long maxReceive, long maxExtract) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("ItemStack cannot be null or empty");
        }
        this.stack = stack;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public void setEnergy(long energy) { // <-- long
        this.stack.getOrCreateTag().putLong("energy", energy); // <-- putLong
    }

    public long receiveEnergy(long maxReceive, boolean simulate) {
        synchronized(stackLock) { 
            long energy = getEnergyStored();
            long energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
            if (energyReceived < 0) energyReceived = 0;
            
            if (!simulate) {
                setEnergy(energy + energyReceived);
            }
            return energyReceived;
        }
    }
    
    public long extractEnergy(long maxExtract, boolean simulate) {
        synchronized(stackLock) {
            long energy = getEnergyStored();
            long energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
            if (energyExtracted < 0) energyExtracted = 0;
            
            if (!simulate) {
                setEnergy(energy - energyExtracted);
            }
            return energyExtracted;
        }
    }

    public long getEnergyStored() {
        if (this.stack == null || this.stack.isEmpty()) return 0L;
        return this.stack.hasTag() ? this.stack.getTag().getLong("energy") : 0L;
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