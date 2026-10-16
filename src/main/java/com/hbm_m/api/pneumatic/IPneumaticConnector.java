package com.hbm_m.api.pneumatic;

import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code IPneumaticConnector} (1.7.10): welche Seiten eines Blocks sich ans Rohrnetz
 * anschliessen lassen.
 *
 * @implNote {@code dir} ist die Seite <b>dieses</b> Blocks, nicht die des Nachbarn, der sich
 *           anschliessen will.
 */
public interface IPneumaticConnector {

    default boolean canConnectPneumatic(@Nullable Direction dir) {
        return dir != null;
    }
}
