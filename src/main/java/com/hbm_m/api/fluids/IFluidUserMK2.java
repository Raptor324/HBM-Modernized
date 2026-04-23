package com.hbm_m.api.fluids;

import com.hbm_m.api.network.NodeNet;
import com.hbm_m.inventory.fluid.tank.FluidTank;

/**
 * IFluidConnectorMK2 + доступ к бакам + константы давления.
 * Аналог IFluidUserMK2 из 1.7.10.
 * Также является ILoadedEntry для проверки валидности ссылки в сети.
 */
public interface IFluidUserMK2 extends IFluidConnectorMK2, NodeNet.ILoadedEntry {

    int HIGHEST_VALID_PRESSURE = 5;
    int[] DEFAULT_PRESSURE_RANGE = { 0, 0 };

    /** Все баки этой машины. Используется для display/debug. */
    FluidTank[] getAllTanks();
}
