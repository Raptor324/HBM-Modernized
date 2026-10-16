package com.hbm_m.interfaces;

import net.minecraft.core.Direction;

/**
 * Базовый интерфейс для всех энергетических объектов.
 * Позволяет проводам и машинам понять, можно ли подключиться.
 */
public interface IEnergyConnector {
    /**
     * Может ли этот блок подключаться к энергосети с указанной стороны
     * @param side Сторона подключения (null = любая)
     */
    boolean canConnectEnergy(Direction side);

    /**
     * Аналог allowDirectProvision из 1.7.10: разрешает ли приёмник прямое питание
     * от соседнего генератора в обход сети (по умолчанию true, как в оригинале).
     * Общее объявление для IEnergyReceiver и IEnergyProvider, чтобы классы,
     * реализующие оба интерфейса, не получали конфликт default-методов.
     */
    default boolean allowDirectProvision() { return true; }
}