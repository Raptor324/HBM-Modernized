package com.hbm_m.inventory.fluid;

/**
 * Hazard diamond special symbols (1.7.10 {@code com.hbm.render.util.EnumSymbol}).
 */
public enum FluidHazardSymbol {
    NONE(0, 0),
    RADIATION(195, 2),
    NOWATER(195, 63),
    ACID(195, 124),
    ASPHYXIANT(195, 185),
    CROYGENIC(134, 185),
    ANTIMATTER(73, 185),
    OXIDIZER(12, 185);

    public final int x;
    public final int y;

    FluidHazardSymbol(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
