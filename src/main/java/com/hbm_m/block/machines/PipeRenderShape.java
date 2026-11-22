package com.hbm_m.block.machines;

import net.minecraft.util.StringRepresentable;

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


    public static PipeRenderShape fromConnections(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
        int mask = (east ? 32 : 0) + (west ? 16 : 0) + (up ? 8 : 0) + (down ? 4 : 0) + (south ? 2 : 0) + (north ? 1 : 0);
        if (mask == 0) {
            return ISOLATED;
        }
        // THROUGH_* means a full straight pipe spanning BOTH directions (the through_*_base
        // models enable the two opposing faces): only true straight runs qualify. A
        // single-sided connection must be COMPLEX so the multipart draws just the one arm.
        if (mask == 48) { // east + west
            return THROUGH_X;
        }
        if (mask == 24) { // up + down
            return THROUGH_Y;
        }
        if (mask == 3) { // north + south
            return THROUGH_Z;
        }
        return COMPLEX;
    }
}
