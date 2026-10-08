package com.hbm_m.block.rail;

import com.hbm_m.util.ForgeDirection;

import net.minecraft.world.level.Level;

/** 1:1 {@code RailStandardCurveWide7} ({@code rail_large_curve_7}): Kurve mit Breite 6 (7 m). */
public class RailStandardCurveWide7 extends RailStandardCurveBase {

    public RailStandardCurveWide7(Properties properties) {
        super(properties);
        this.width = 6;
    }

    @Override
    protected boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        dir = dir.getOpposite();

        int dX = dir.offsetX;
        int dZ = dir.offsetZ;
        int rX = rot.offsetX;
        int rZ = rot.offsetZ;

        // Original prueft nur die Zellen der 5-m-Kurve
        return isReplaceable(world, x + dX, y, z + dZ) &&
                isReplaceable(world, x + rX, y, z + rZ) &&
                isReplaceable(world, x + dX + rX, y, z + dZ + rZ) &&
                isReplaceable(world, x + dX + rX * 2, y, z + dZ + rZ * 2) &&
                isReplaceable(world, x + dX * 2 + rX, y, z + dZ * 2 + rZ) &&
                isReplaceable(world, x + dX * 2 + rX * 2, y, z + dZ * 2 + rZ * 2) &&
                isReplaceable(world, x + dX * 3 + rX, y, z + dZ * 3 + rZ) &&
                isReplaceable(world, x + dX * 3 + rX * 2, y, z + dZ * 3 + rZ * 2) &&
                isReplaceable(world, x + dX * 2 + rX * 3, y, z + dZ * 2 + rZ * 3) &&
                isReplaceable(world, x + dX * 3 + rX * 3, y, z + dZ * 3 + rZ * 3) &&
                isReplaceable(world, x + dX * 4 + rX * 3, y, z + dZ * 4 + rZ * 3) &&
                isReplaceable(world, x + dX * 3 + rX * 4, y, z + dZ * 3 + rZ * 4) &&
                isReplaceable(world, x + dX * 4 + rX * 4, y, z + dZ * 4 + rZ * 4);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {

        safeRem = true;

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        dir = dir.getOpposite();

        int dX = dir.offsetX;
        int dZ = dir.offsetZ;
        int rX = rot.offsetX;
        int rZ = rot.offsetZ;

        setBlock(world, x + dX, y, z + dZ, dir.ordinal());
        setBlock(world, x + dX * 2, y, z + dZ * 2, dir.ordinal());
        setBlock(world, x + rX, y, z + rZ, rot.ordinal());
        setBlock(world, x + dX + rX, y, z + dZ + rZ, rot.ordinal());
        setBlock(world, x + dX * 2 + rX, y, z + dZ * 2 + rZ, rot.ordinal());
        setBlock(world, x + dX * 3 + rX, y, z + dZ * 3 + rZ, dir.ordinal());
        setBlock(world, x + dX * 4 + rX, y, z + dZ * 4 + rZ, dir.ordinal());
        setBlock(world, x + dX * 2 + rX * 2, y, z + dZ * 2 + rZ * 2, rot.ordinal());
        setBlock(world, x + dX * 3 + rX * 2, y, z + dZ * 3 + rZ * 2, dir.ordinal());
        setBlock(world, x + dX * 4 + rX * 2, y, z + dZ * 4 + rZ * 2, dir.ordinal());
        setBlock(world, x + dX * 5 + rX * 2, y, z + dZ * 5 + rZ * 2, dir.ordinal());
        setBlock(world, x + dX * 3 + rX * 3, y, z + dZ * 3 + rZ * 3, rot.ordinal());
        setBlock(world, x + dX * 4 + rX * 3, y, z + dZ * 4 + rZ * 3, dir.ordinal());
        setBlock(world, x + dX * 5 + rX * 3, y, z + dZ * 5 + rZ * 3, dir.ordinal());
        setBlock(world, x + dX * 4 + rX * 4, y, z + dZ * 4 + rZ * 4, rot.ordinal());
        setBlock(world, x + dX * 5 + rX * 4, y, z + dZ * 5 + rZ * 4, dir.ordinal());
        setBlock(world, x + dX * 6 + rX * 4, y, z + dZ * 6 + rZ * 4, dir.ordinal());
        setBlock(world, x + dX * 5 + rX * 5, y, z + dZ * 5 + rZ * 5, rot.ordinal());
        setBlock(world, x + dX * 5 + rX * 6, y, z + dZ * 5 + rZ * 6, rot.ordinal());
        setBlock(world, x + dX * 6 + rX * 5, y, z + dZ * 6 + rZ * 5, rot.ordinal());
        setBlock(world, x + dX * 6 + rX * 6, y, z + dZ * 6 + rZ * 6, rot.ordinal());

        safeRem = false;
    }
}
