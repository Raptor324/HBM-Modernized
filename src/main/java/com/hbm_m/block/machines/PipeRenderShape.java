package com.hbm_m.block.machines;

import net.minecraft.util.StringRepresentable;

/**
 * Visual layout for multipart pipe models (NEO-style): isolated hub, straight through one axis, or complex with arms + octants.
 */
public enum PipeRenderShape implements StringRepresentable {
    ISOLATED("isolated"),
    THROUGH_X("through_x"),
    THROUGH_Y("through_y"),
    THROUGH_Z("through_z"),
    COMPLEX("complex");

    private final String serializedName;

    PipeRenderShape(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /**
     * NEO mask: EAST=pX(32), WEST=nX(16), UP=pY(8), DOWN=nY(4), SOUTH=pZ(2), NORTH=nZ(1).
     */
    public static PipeRenderShape fromConnections(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
        int mask = (east ? 32 : 0) + (west ? 16 : 0) + (up ? 8 : 0) + (down ? 4 : 0) + (south ? 2 : 0) + (north ? 1 : 0);
        if (mask == 0) {
            return ISOLATED;
        }
        if (mask == 32 || mask == 16) {
            return THROUGH_X;
        }
        if (mask == 8 || mask == 4) {
            return THROUGH_Y;
        }
        if (mask == 2 || mask == 1) {
            return THROUGH_Z;
        }
        return COMPLEX;
    }
}
