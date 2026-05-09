//? if forge {
/*package com.hbm_m.api.energy;

import net.minecraftforge.energy.IEnergyStorage;

public class HbmForgeWrapper implements IEnergyStorage {

    private final ConverterBlockEntity tile;

    public HbmForgeWrapper(ConverterBlockEntity tile) {
        this.tile = tile;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        // Просто заливаем в буфер тайла
        long space = tile.getMaxEnergyStored() - tile.getEnergyStored();
        long amount = Math.min(maxReceive, space);
        if (!simulate) {
            tile.setEnergyStored(tile.getEnergyStored() + amount);
        }
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        // Просто забираем из буфера тайла
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
//? if fabric {
package com.hbm_m.api.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

public class HbmForgeWrapper extends SnapshotParticipant<Long> implements EnergyStorage {

    private final ConverterBlockEntity tile;

    public HbmForgeWrapper(ConverterBlockEntity tile) {
        this.tile = tile;
    }

    @Override
    protected Long createSnapshot() {
        // Сохраняем текущее количество энергии до транзакции
        return tile.getEnergyStored();
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        // Восстанавливаем энергию, если транзакция отменена
        tile.setEnergyStored(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        // Вызывается при успешном применении транзакции
        tile.setChanged();
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (!supportsInsertion()) return 0;
        long space = tile.getMaxEnergyStored() - tile.getEnergyStored();
        long amount = Math.min(maxAmount, space);
        if (amount > 0) {
            updateSnapshots(transaction);
            tile.setEnergyStored(tile.getEnergyStored() + amount);
        }
        return amount;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (!supportsExtraction()) return 0;
        long amount = Math.min(maxAmount, tile.getEnergyStored());
        if (amount > 0) {
            updateSnapshots(transaction);
            tile.setEnergyStored(tile.getEnergyStored() - amount);
        }
        return amount;
    }

    @Override
    public long getAmount() {
        return tile.getEnergyStored();
    }

    @Override
    public long getCapacity() {
        return tile.getMaxEnergyStored();
    }

    @Override
    public boolean supportsInsertion() {
        return tile.canReceive();
    }

    @Override
    public boolean supportsExtraction() {
        return tile.canExtract();
    }
}
//?}