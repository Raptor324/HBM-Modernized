package com.hbm_m.api.energy;

import net.minecraft.util.StringRepresentable;

/**
 * Визуальная форма центра провода: контакт (Core) или прямой сегмент через блок (CX/CY/CZ в cable_neo.obj).
 */
public enum WireCenterVisual implements StringRepresentable {
    JUNCTION("junction"),
    /** Ровно N+S — CZ. */
    STRAIGHT_Z("straight_z"),
    /** Ровно E+W — CX. */
    STRAIGHT_X("straight_x"),
    /** Ровно U+D — CY. */
    STRAIGHT_Y("straight_y");

    private final String serializedName;

    WireCenterVisual(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
