package com.hbm_m.api.energy;

import net.minecraftforge.energy.IEnergyStorage;

/**
 * Обертка для совместимости между HBM Energy (long) и Forge Energy (int).
 * Работает с младшими (LOW) или старшими (HIGH) 32 битами long-значения.
 *
 * ВАЖНО: HIGH режим работает с битами умноженными на 2^32!
 */
public class LongEnergyWrapper implements IEnergyStorage {

    private final IEnergyConnector handler;
    private final BitMode mode;

    public enum BitMode {
        LOW,  // Младшие 32 бита (0-2,147,483,647)
        HIGH  // Старшие 32 бита (каждая единица = 2^32 в long)
    }

    public LongEnergyWrapper(IEnergyConnector handler, BitMode mode) {
        this.handler = handler;
        this.mode = mode;
    }

    // --- Утилиты для работы с битами ---
    private static int getLow(long val) {
        return (int) (val & 0xFFFFFFFFL);
    }

    private static int getHigh(long val) {
        return (int) (val >> 32);
    }

    private static long pack(int high, int low) {
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }

    // --- Forge Energy IEnergyStorage ---
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!(handler instanceof IEnergyReceiver receiver) || !receiver.canReceive()) {
            return 0;
        }

        long currentEnergy = receiver.getEnergyStored();
        long maxEnergy = receiver.getMaxEnergyStored();

        if (mode == BitMode.LOW) {
            // LOW режим: работаем с младшими битами
            long toReceive = Math.min(maxReceive, maxEnergy - currentEnergy);
            long received = receiver.receiveEnergy(toReceive, simulate);
            return (int) Math.min(received, Integer.MAX_VALUE);

        } else {
            // HIGH режим: работаем со старшими битами
            int currentHigh = getHigh(currentEnergy);
            int maxHigh = getHigh(maxEnergy);
            int currentLow = getLow(currentEnergy);

            int canReceive = Math.min(maxReceive, maxHigh - currentHigh);
            if (canReceive <= 0) return 0;

            if (!simulate) {
                // Увеличиваем HIGH биты, сохраняя LOW
                long newEnergy = pack(currentHigh + canReceive, currentLow);
                receiver.setEnergyStored(newEnergy);
            }

            return canReceive;
        }
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!(handler instanceof IEnergyProvider provider) || !provider.canExtract()) {
            return 0;
        }

        long currentEnergy = provider.getEnergyStored();

        if (mode == BitMode.LOW) {
            // LOW режим: работаем с младшими битами
            long toExtract = Math.min(maxExtract, currentEnergy);
            long extracted = provider.extractEnergy(toExtract, simulate);
            return (int) Math.min(extracted, Integer.MAX_VALUE);

        } else {
            // HIGH режим: работаем со старшими битами
            int currentHigh = getHigh(currentEnergy);
            int currentLow = getLow(currentEnergy);

            int canExtract = Math.min(maxExtract, currentHigh);
            if (canExtract <= 0) return 0;

            if (!simulate) {
                // Уменьшаем HIGH биты, сохраняя LOW
                long newEnergy = pack(currentHigh - canExtract, currentLow);
                provider.setEnergyStored(newEnergy);
            }

            return canExtract;
        }
    }

    @Override
    public int getEnergyStored() {
        long energy = 0;

        if (handler instanceof IEnergyProvider p) {
            energy = p.getEnergyStored();
        } else if (handler instanceof IEnergyReceiver r) {
            energy = r.getEnergyStored();
        }

        return (mode == BitMode.LOW) ? getLow(energy) : getHigh(energy);
    }

    @Override
    public int getMaxEnergyStored() {
        long maxEnergy = 0;

        if (handler instanceof IEnergyProvider p) {
            maxEnergy = p.getMaxEnergyStored();
        } else if (handler instanceof IEnergyReceiver r) {
            maxEnergy = r.getMaxEnergyStored();
        }

        return (mode == BitMode.LOW) ? getLow(maxEnergy) : getHigh(maxEnergy);
    }

    @Override
    public boolean canExtract() {
        return handler instanceof IEnergyProvider p && p.canExtract();
    }

    @Override
    public boolean canReceive() {
        return handler instanceof IEnergyReceiver r && r.canReceive();
    }
}