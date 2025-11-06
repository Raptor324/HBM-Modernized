package com.hbm_m.energy; // (или com.hbm_m.energy)

import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.main.MainRegistry;

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

    public long extractEnergy(long maxExtract, boolean simulate) {
        if (maxExtract < 0) {
            MainRegistry.LOGGER.warn("Negative energy extraction requested: {}", maxExtract);
            return 0L;
        }
        
        int intExtract = (int) Math.min(Integer.MAX_VALUE, maxExtract);
        long result = this.forgeStorage.extractEnergy(intExtract, simulate);
        
        if (result < 0) {
            MainRegistry.LOGGER.error("Negative energy returned: {}", result);
            return 0L;
        }
        return result;
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
