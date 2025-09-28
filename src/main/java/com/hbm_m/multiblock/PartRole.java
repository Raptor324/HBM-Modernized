package com.hbm_m.multiblock;

// Содержит роли, которые могут быть назначены частям мультиблочной структуры.
// Используется IMultiblockPart и MultiblockStructureHelper для управления поведением частей

public enum PartRole {
    
    DEFAULT,
    // A standard structural block with no special function

    ENERGY_CONNECTOR,
    // A block that can accept energy and forward it to the controller

    FLUID_CONNECTOR,
    // A block that can accept fluids from an external inventory (e.g., pipes, tanks)

    ITEM_INPUT,
    // A block that can pull items from an external inventory

    ITEM_OUTPUT
    // A block that can output items to an external inventory
}