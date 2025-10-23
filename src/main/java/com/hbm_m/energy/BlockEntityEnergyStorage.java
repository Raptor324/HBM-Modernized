package com.hbm_m.energy;

import com.hbm_m.config.ModClothConfig; // (Если нужно)
import com.hbm_m.main.MainRegistry; // (Если нужно)

// (Убери import net.minecraftforge.energy.EnergyStorage;)

public class BlockEntityEnergyStorage implements ILongEnergyStorage { // Имплементируем новый интерфейс

    protected long energy; // <-- ТИП LONG
    protected final long capacity;
    protected final long maxReceive;
    protected final long maxExtract;

    public BlockEntityEnergyStorage(long capacity, long maxReceive, long maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energy = 0;
    }

    // (Этот конструктор был у тебя, можешь оставить)
    public BlockEntityEnergyStorage(long capacity, long maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (energyReceived < 0) {
            energyReceived = 0; // Безопасность
        }
        if (!simulate) {
            this.energy += energyReceived;
            // (Твоя логика дебага)
            if (energyReceived > 0 && ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("[EnergyStorage]: Received {} FE (now {})", energyReceived, this.energy);
            }
        }
        return energyReceived;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        long energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (energyExtracted < 0) {
            energyExtracted = 0; // Безопасность
        }
        if (!simulate) {
            this.energy -= energyExtracted;
            // (Твоя логика дебага)
            if (energyExtracted > 0 && ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("[EnergyStorage]: Extracted {} FE (now {})", energyExtracted, this.energy);
            }
        }
        return energyExtracted;
    }

    @Override
    public long getEnergyStored() {
        return this.energy;
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
