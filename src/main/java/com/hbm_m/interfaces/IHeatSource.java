package com.hbm_m.interfaces;

/**
 * Port of {@code api.hbm.tile.IHeatSource} (1.7.10 Original) - minimal heat-network contract.
 * A block below a heat CONSUMER (e.g. Stirling Engine) implementing this interface can be drained
 * for heat each tick. Currently produced by the Basic Boiler; consumed by the Stirling Engine
 * family.
 */
public interface IHeatSource {

    int getHeatStored();

    int getMaxHeatStored();

    void useUpHeat(int amount);
}
