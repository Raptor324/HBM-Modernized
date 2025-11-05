package com.hbm_m.energy; // (или com.hbm_m.energy)

import com.hbm_m.energy.ILongEnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

// Обертка, которая реализует ILongEnergyStorage (long) поверх IEnergyStorage (int)
public class ForgeToLongWrapper implements ILongEnergyStorage {
    private final IEnergyStorage forgeStorage;

    public ForgeToLongWrapper(IEnergyStorage forgeStorage) {
        this.forgeStorage = forgeStorage;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        // Принимаем long, усекаем до int
        int intReceive = (int) Math.min(Integer.MAX_VALUE, maxReceive);
        return this.forgeStorage.receiveEnergy(intReceive, simulate);
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        // Принимаем long, усекаем до int
        int intExtract = (int) Math.min(Integer.MAX_VALUE, maxExtract);
        return this.forgeStorage.extractEnergy(intExtract, simulate);
    }

    // Здесь усечение не нужно, int безопасно превращается в long
    @Override
    public long getEnergyStored() { return this.forgeStorage.getEnergyStored(); }
    @Override
    public long getMaxEnergyStored() { return this.forgeStorage.getMaxEnergyStored(); }

    @Override
    public boolean canExtract() { return this.forgeStorage.canExtract(); }
    @Override
    public boolean canReceive() { return this.forgeStorage.canReceive(); }
}
