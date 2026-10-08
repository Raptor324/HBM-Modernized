package com.hbm_m.blockentity.machines;

import com.hbm_m.inventory.material.NTMMaterial;

/** 1:1 {@code IRenderFoundry}: was der Giesserei-Renderer von einem Becken/einer Form wissen muss. */
public interface IRenderFoundry {

    /** Returns whether a molten metal layer should be rendered in the TESR */
    boolean shouldRender();
    /** Returns the Y-offset of the molten metal layer */
    double getMoltenLevel();
    /** Returns the NTM Mat used, mainly for the color */
    NTMMaterial getMat();

    /* Return size constraints for the rectangle */
    double minX();
    double maxX();
    double minZ();
    double maxZ();
    double moldHeight();
    double outHeight();
}
