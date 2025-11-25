package com.hbm_m.api.energy;

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
}