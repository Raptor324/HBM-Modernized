package com.hbm_m.blockentity.machines.pile;

import net.minecraft.core.Direction;

/**
 * 1:1-Port von {@code TileEntityPileCore.PileOrientation}: die Achse, auf der der Meiler steht.
 *
 * <p>Sie ergibt sich aus der Seite, von der aus zusammengebaut wurde. Ein Kanal, der auf dieser
 * Achse liegt, ist ein Brennstoffkanal; ein Kanal quer dazu ist ein Lueftungskanal.</p>
 */
public enum PileOrientation {
    NORTH_SOUTH,
    EAST_WEST,
    NEITHER;

    public static PileOrientation of(Direction dir) {
        if (dir == Direction.NORTH || dir == Direction.SOUTH) return NORTH_SOUTH;
        if (dir == Direction.EAST || dir == Direction.WEST) return EAST_WEST;
        return NEITHER;
    }
}
