package com.hbm_m.energy;

import net.minecraftforge.energy.EnergyStorage;

public class BlockEntityEnergyStorage extends EnergyStorage {

    public BlockEntityEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer, maxTransfer, 0);
    }

    public BlockEntityEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract, 0);
    }

    // Этот метод нужен для загрузки из NBT
    public void setEnergy(int energy) {
        if (energy < 0) {
            energy = 0;
        }
        if (energy > this.capacity) {
            energy = this.capacity;
        }
        this.energy = energy;
    }

    // НОВЫЕ МЕТОДЫ-ГЕТТЕРЫ 

    /**
     * Возвращает максимальное количество энергии, которое можно принять за один тик.
     * @return Максимальная скорость приема.
     */
    public int getMaxReceive() {
        return this.maxReceive;
    }

    /**
     * Возвращает максимальное количество энергии, которое можно извлечь за один тик.
     * @return Максимальная скорость извлечения.
     */
    public int getMaxExtract() {
        return this.maxExtract;
    }
}