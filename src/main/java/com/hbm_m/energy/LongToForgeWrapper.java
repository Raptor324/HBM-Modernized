package com.hbm_m.energy; // (или com.hbm_m.energy)

import com.hbm_m.energy.ILongEnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

// Обертка, которая реализует IEnergyStorage (int) поверх ILongEnergyStorage (long)
public class LongToForgeWrapper implements IEnergyStorage {
    private final ILongEnergyStorage longStorage;

    public LongToForgeWrapper(ILongEnergyStorage longStorage) {
        this.longStorage = longStorage;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        // Принимаем int, передаем как long, возвращаем как int
        return (int) this.longStorage.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        // Извлекаем int, передаем как long, возвращаем как int
        return (int) this.longStorage.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        // ВНИМАНИЕ: Усечение!
        // Отдаем int, даже если хранится long
        return (int) Math.min(Integer.MAX_VALUE, this.longStorage.getEnergyStored());
    }

    @Override
    public int getMaxEnergyStored() {
        // ВНИМАНИЕ: Усечение!
        return (int) Math.min(Integer.MAX_VALUE, this.longStorage.getMaxEnergyStored());
    }

    @Override
    public boolean canExtract() { return this.longStorage.canExtract(); }
    @Override
    public boolean canReceive() { return this.longStorage.canReceive(); }
}
