package com.hbm_m.interfaces;

public interface ILongEnergyMenu {
    // [ИСПРАВЛЕНИЕ] Добавляем третий аргумент long delta
    void setEnergy(long energy, long maxEnergy, long delta);

    // Сервер вызывает это, чтобы узнать, что отправлять
    long getEnergyStatic();
    long getMaxEnergyStatic();

    // [ИСПРАВЛЕНИЕ] Добавляем этот метод, чтобы меню могло читать дельту с TileEntity
    long getEnergyDeltaStatic();
}