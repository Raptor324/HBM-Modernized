package com.hbm_m.interfaces;

/**
 * Block entities that participate in {@link com.hbm_m.api.energy.EnergyNetwork} as dual
 * provider/receiver with a discrete operating mode (same encoding as {@code MachineBatteryBlockEntity}).
 */
public interface IEnergyModeHolder {

    /**
     * 0 = BOTH, 1 = INPUT only, 2 = OUTPUT only, 3 = DISABLED
     */
    int getCurrentMode();
}
