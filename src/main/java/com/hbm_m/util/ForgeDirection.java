package com.hbm_m.util;

import net.minecraft.core.Direction;

/**
 * 1:1 {@code net.minecraftforge.common.util.ForgeDirection} (Forge 1.7.10): Reihenfolge, Versatz und Drehmatrizen
 * wie im Original, damit portierter Code mit {@code sideHit}-Zahlen und {@code getRotation} unveraendert rechnet.
 */
public enum ForgeDirection {
    /** -Y */ DOWN(0, -1, 0),
    /** +Y */ UP(0, 1, 0),
    /** -Z */ NORTH(0, 0, -1),
    /** +Z */ SOUTH(0, 0, 1),
    /** -X */ WEST(-1, 0, 0),
    /** +X */ EAST(1, 0, 0),
    UNKNOWN(0, 0, 0);

    public final int offsetX;
    public final int offsetY;
    public final int offsetZ;
    public final int flag;
    public static final ForgeDirection[] VALID_DIRECTIONS = { DOWN, UP, NORTH, SOUTH, WEST, EAST };
    public static final int[] OPPOSITES = { 1, 0, 3, 2, 5, 4, 6 };
    // Left hand rule rotation matrix for all possible axes of rotation
    public static final int[][] ROTATION_MATRIX = {
            { 0, 1, 4, 5, 3, 2, 6 },
            { 0, 1, 5, 4, 2, 3, 6 },
            { 5, 4, 2, 3, 0, 1, 6 },
            { 4, 5, 2, 3, 1, 0, 6 },
            { 2, 3, 1, 0, 4, 5, 6 },
            { 3, 2, 0, 1, 4, 5, 6 },
            { 0, 1, 2, 3, 4, 5, 6 },
    };

    ForgeDirection(int x, int y, int z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
        flag = 1 << ordinal();
    }

    public static ForgeDirection getOrientation(int id) {
        if (id >= 0 && id < VALID_DIRECTIONS.length) return VALID_DIRECTIONS[id];
        return UNKNOWN;
    }

    public ForgeDirection getOpposite() {
        return getOrientation(OPPOSITES[ordinal()]);
    }

    public ForgeDirection getRotation(ForgeDirection axis) {
        return getOrientation(ROTATION_MATRIX[axis.ordinal()][ordinal()]);
    }

    /** Port-Bruecke: Vanilla-Richtung zur Forge-Richtung (Ordinalwerte sind identisch). */
    public static ForgeDirection of(Direction d) {
        return d == null ? UNKNOWN : VALID_DIRECTIONS[d.get3DDataValue()];
    }

    public Direction toDirection() {
        return this == UNKNOWN ? null : Direction.from3DDataValue(ordinal());
    }
}
