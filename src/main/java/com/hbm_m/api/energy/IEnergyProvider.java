package com.hbm_m.api.energy;

public interface IEnergyProvider extends IEnergyConnector {

    /**
     * Получить текущее количество энергии
     */
    long getEnergyStored();

    /**
     * Получить максимальную емкость
     */
    long getMaxEnergyStored();

    /**
     * Установить количество энергии (используется сетью)
     */
    void setEnergyStored(long energy);

    /**
     * Максимальная скорость отдачи энергии за тик
     */
    long getProvideSpeed();

    /**
     * Извлечь энергию из провайдера
     * @param maxExtract Максимальное количество энергии для извлечения
     * @param simulate Если true, только симулирует операцию без изменения состояния
     * @return Фактически извлеченное количество энергии
     */
    long extractEnergy(long maxExtract, boolean simulate);

    /**
     * Может ли этот провайдер отдавать энергию
     */
    boolean canExtract();
}