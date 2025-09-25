package com.hbm_m.block.entity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStorageBlockEntity implements IEnergyStorage {
    private int energy;
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;
    private BlockEntity blockEntity;

    public EnergyStorageBlockEntity(int capacity, int maxReceive, int maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energy = 0;
    }

    public EnergyStorageBlockEntity(int capacity, int maxReceive) {
        this(capacity, maxReceive, maxReceive);
    }

    public EnergyStorageBlockEntity(int capacity) {
        this(capacity, capacity, capacity);
    }

    // Устанавливаем связь с BlockEntity для автоматического вызова setChanged()
    public void setBlockEntity(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive())
            return 0;

        int energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate) {
            energy += energyReceived;
            if (blockEntity != null) {
                blockEntity.setChanged();
            }
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract())
            return 0;

        int energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) {
            energy -= energyExtracted;
            if (blockEntity != null) {
                blockEntity.setChanged();
            }
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
        if (blockEntity != null) {
            blockEntity.setChanged();
        }
    }

    public void addEnergy(int energy) {
        this.energy = Math.max(0, Math.min(capacity, this.energy + energy));
        if (blockEntity != null) {
            blockEntity.setChanged();
        }
    }

    public void consumeEnergy(int energy) {
        this.energy = Math.max(0, this.energy - energy);
        if (blockEntity != null) {
            blockEntity.setChanged();
        }
    }
}
