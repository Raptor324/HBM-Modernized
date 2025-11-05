package com.hbm_m.energy; // (или com.hbm_m.api.energy)

/**
 * Аналог IEnergyStorage, но использующий long для хранения
 * и передачи больших объёмов энергии.
 */
public interface ILongEnergyStorage {

    /**
     * Добавляет энергию в хранилище.
     * @param maxReceive Максимальное кол-во для приёма.
     * @param simulate   Если true, приём симулируется.
     * @return Количество энергии, которое БЫЛО принято (long).
     */
    long receiveEnergy(long maxReceive, boolean simulate);

    /**
     * Извлекает энергию из хранилища.
     * @param maxExtract Максимальное кол-во для извлечения.
     * @param simulate   Если true, извлечение симулируется.
     * @return Количество энергии, которое БЫЛО извлечено (long).
     */
    long extractEnergy(long maxExtract, boolean simulate);

    /**
     * @return Текущее кол-во энергии в хранилище (long).
     */
    long getEnergyStored();

    /**
     * @return Максимальная ёмкость хранилища (long).
     */
    long getMaxEnergyStored();

    /**
     * @return Может ли хранилище отдавать энергию.
     */
    boolean canExtract();

    /**
     * @return Может ли хранилище принимать энергию.
     */
    boolean canReceive();
}
