package com.hbm_m.energy;

import com.hbm_m.main.MainRegistry;


public class BlockEntityEnergyStorage implements ILongEnergyStorage { // Имплементируем новый интерфейс

    protected long energy;
    protected final long capacity;
    protected final long maxReceive;
    protected final long maxExtract;
    private final Object energyLock = new Object();

    public BlockEntityEnergyStorage(long capacity, long maxReceive, long maxExtract) {
        this.capacity = Math.max(0, capacity);
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
        
        if (capacity < 0) MainRegistry.LOGGER.warn("Invalid capacity: {}", capacity);
        if (maxReceive < 0) MainRegistry.LOGGER.warn("Invalid maxReceive: {}", maxReceive);
        if (maxExtract < 0) MainRegistry.LOGGER.warn("Invalid maxExtract: {}", maxExtract);
        
        this.energy = 0;
    }

    public BlockEntityEnergyStorage(long capacity, long maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        synchronized(energyLock) {  // ← ДОБАВИТЬ
            long cap = Math.max(0, this.capacity);
            long energy_cur = Math.max(0, this.energy);
            long energyReceived = Math.min(cap - energy_cur, Math.min(Math.max(0, this.maxReceive), maxReceive));
            if (energyReceived < 0) energyReceived = 0;
            
            if (!simulate) {
                this.energy = energy_cur + energyReceived;
            }
            return energyReceived;
        }
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        synchronized(energyLock) {  // ← ДОБАВИТЬ
            if (maxExtract < 0 || this.energy < 0) {
                this.energy = Math.max(0, this.energy);
                return 0L;
            }
            
            long energyExtracted = Math.min(this.energy, Math.min(this.maxExtract, maxExtract));
            if (energyExtracted < 0) energyExtracted = 0;
            
            if (!simulate) {
                this.energy -= energyExtracted;
                if (this.energy < 0) this.energy = 0;
            }
            return energyExtracted;
        }
    }

    @Override
    public long getEnergyStored() {
        synchronized(energyLock) {
            return this.energy;
        }
    }

    @Override
    public long getMaxEnergyStored() {
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

    // Твой метод для NBT, теперь принимает long
    public void setEnergy(long energy) {
        if (energy < 0L) energy = 0L;
        if (energy > this.capacity) energy = this.capacity;
        this.energy = energy;
    }

    // Геттеры для maxReceive/maxExtract
    public long getMaxReceive() { return this.maxReceive; }
    public long getMaxExtract() { return this.maxExtract; }
}
