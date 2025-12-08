package com.hbm_m.api.energy;

public interface ILongEnergyMenu {
    // Клиент вызывает это, когда приходит пакет
    void setEnergy(long energy, long maxEnergy);

    // Сервер вызывает это, чтобы узнать, что отправлять
    long getEnergyStatic();
    long getMaxEnergyStatic();
}