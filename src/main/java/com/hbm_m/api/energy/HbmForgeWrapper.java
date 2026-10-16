//? if forge {
package com.hbm_m.api.energy;

import net.minecraftforge.energy.IEnergyStorage;

public class HbmForgeWrapper implements IEnergyStorage {

    private final ConverterBlockEntity tile;

    public HbmForgeWrapper(ConverterBlockEntity tile) {
        this.tile = tile;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        long space = tile.getMaxEnergyStored() - tile.getEnergyStored();
        long amount = Math.min(maxReceive, space);
        if (!simulate) {
            tile.setEnergyStored(tile.getEnergyStored() + amount);
        }
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        long amount = Math.min(maxExtract, tile.getEnergyStored());
        if (!simulate) {
            tile.setEnergyStored(tile.getEnergyStored() - amount);
        }
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(tile.getEnergyStored(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(tile.getMaxEnergyStored(), Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() { return true; }

    @Override
    public boolean canReceive() { return true; }
}
//?}
//? if neoforge {
/*package com.hbm_m.api.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

public class HbmForgeWrapper implements IEnergyStorage {

    private final ConverterBlockEntity tile;

    public HbmForgeWrapper(ConverterBlockEntity tile) {
        this.tile = tile;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        long space = tile.getMaxEnergyStored() - tile.getEnergyStored();
        long amount = Math.min(maxReceive, space);
        if (!simulate) {
            tile.setEnergyStored(tile.getEnergyStored() + amount);
        }
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        long amount = Math.min(maxExtract, tile.getEnergyStored());
        if (!simulate) {
            tile.setEnergyStored(tile.getEnergyStored() - amount);
        }
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(tile.getEnergyStored(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(tile.getMaxEnergyStored(), Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() { return true; }

    @Override
    public boolean canReceive() { return true; }
}
*///?}