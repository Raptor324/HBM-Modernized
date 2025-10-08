package com.hbm_m.multiblock;

import net.minecraft.util.StringRepresentable;

public enum PartRole implements StringRepresentable {
    DEFAULT("default"),
    ENERGY_CONNECTOR("energy_connector"),
    FLUID_CONNECTOR("fluid_connector"),
    ITEM_INPUT("item_input"),
    ITEM_OUTPUT("item_output");
    
    private final String name;
    
    PartRole(String name) {
        this.name = name;
    }
    
    @Override
    public String getSerializedName() {
        return this.name;
    }
}
