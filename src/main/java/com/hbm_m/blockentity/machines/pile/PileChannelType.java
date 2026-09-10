package com.hbm_m.blockentity.machines.pile;

import net.minecraft.core.Direction;

/**
 * 1:1-Port von {@code TileEntityPileCore.PileChannelType}.
 *
 * <p>Senkrechte Kanaele sind immer Steuerstabkanaele. Waagerechte richten sich danach, ob sie auf
 * der Achse des Meilers liegen (Brennstoff) oder quer dazu (Lueftung).</p>
 */
public enum PileChannelType {
    FUEL,
    VENTILATION,
    CONTROL;

    public static PileChannelType of(Direction channelDir, PileOrientation pileOrientation) {
        if (channelDir == Direction.UP || channelDir == Direction.DOWN) return CONTROL;
        if (PileOrientation.of(channelDir) == pileOrientation) return FUEL;
        return VENTILATION;
    }
}
