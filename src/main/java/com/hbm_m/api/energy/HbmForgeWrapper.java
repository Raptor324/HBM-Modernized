package com.hbm_m.api.energy;

import net.minecraftforge.energy.IEnergyStorage;

public class HbmForgeWrapper implements IEnergyStorage {

    private final ConverterBlockEntity blockEntity;

    public HbmForgeWrapper(ConverterBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        // Фордж пытается залить энергию.
        // Конвертируем int -> long и передаем в BE
        long accepted = blockEntity.receiveEnergy(maxReceive, simulate);
        return (int) Math.min(accepted, Integer.MAX_VALUE);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        // Фордж пытается высосать энергию.
        long extracted = blockEntity.extractEnergy(maxExtract, simulate);
        return (int) Math.min(extracted, Integer.MAX_VALUE);
    }

    @Override
    public int getEnergyStored() {
        // Если энергии больше, чем 2 млрд, показываем максимум int
        return (int) Math.min(blockEntity.getEnergyStored(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(blockEntity.getMaxEnergyStored(), Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() {
        return blockEntity.canExtract();
    }

    @Override
    public boolean canReceive() {
        return blockEntity.canReceive();
    }
}