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

    /**
     * Original {@code TileEntityProxyCombo.heatSource()}: darf das Phantomteil an {@code localOffset} (Gitteroffset zum
     * Kern, vor Drehung, s. {@code UniversalMachinePartBlockEntity.getLocalOffsetFromController}) diese Waermequelle
     * nach aussen weiterreichen? Standard {@code false} = HBM-Verhalten (Teile sind keine Waermequelle).
     * Space-Addon: Dyson-Waermewandler (Zellen ueber dem Kern).
     */
    default boolean providesHeatThroughPart(@org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos localOffset) {
        return false;
    }
}
