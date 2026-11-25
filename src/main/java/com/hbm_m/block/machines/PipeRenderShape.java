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
        // 1:1 RenderTestPipe.renderWorldBlock: the straight through_* model (both opposing
        // arms) is drawn for a double connection OR a SINGLE connection on that axis
        // (masks 0b100000/0b010000, 0b001000/0b000100, 0b000010/0b000001). A single arm
        // alone looks like a half-block stub — the original never draws that.
        // Only mixed multi-axis connections fall to COMPLEX (arms + corner octants).
        if (mask == 48 || mask == 32 || mask == 16) { // east and/or west
            return THROUGH_X;
        }
        if (mask == 24 || mask == 8 || mask == 4) { // up and/or down
            return THROUGH_Y;
        }
        if (mask == 3 || mask == 2 || mask == 1) { // north and/or south
            return THROUGH_Z;
        }
        return COMPLEX;
    }
}
