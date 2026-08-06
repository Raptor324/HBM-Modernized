package com.hbm_m.block.network;

import net.minecraft.util.StringRepresentable;

/** Curve state of a bendable conveyor segment - replaces the original's meta-encoded path direction. */
public enum ConveyorBend implements StringRepresentable {
    STRAIGHT("straight"),
    LEFT("left"),
    RIGHT("right");

    private final String name;

    ConveyorBend(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
