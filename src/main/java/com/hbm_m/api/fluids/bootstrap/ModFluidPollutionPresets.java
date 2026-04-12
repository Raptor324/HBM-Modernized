package com.hbm_m.api.fluids.bootstrap;

import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.PollutionType;

/**
 * Pollution coefficients from 1.7.10 {@code Fluids} + {@code PollutionHandler}.
 */
public final class ModFluidPollutionPresets {

    private static final float SOOT_PER_SECOND = 1f / 25f;
    private static final float POISON_PER_SECOND = 1f / 50f;
    private static final float HEAVY_METAL_PER_SECOND = 1f / 50f; // same scale as 1.7 HEAVY_METAL

    public static final float SOOT_UNREFINED_OIL = SOOT_PER_SECOND * 0.1f;
    public static final float SOOT_REFINED_OIL = SOOT_PER_SECOND * 0.025f;
    public static final float SOOT_GAS = SOOT_PER_SECOND * 0.005f;
    public static final float LEAD_FUEL = HEAVY_METAL_PER_SECOND * 0.025f;
    public static final float POISON_OIL = POISON_PER_SECOND * 0.0025f;
    public static final float POISON_EXTREME = POISON_PER_SECOND * 0.025f;
    public static final float POISON_MINOR = POISON_PER_SECOND * 0.001f;

    public static final FT_Polluting P_OIL = new FT_Polluting()
            .burn(PollutionType.SOOT, SOOT_UNREFINED_OIL)
            .release(PollutionType.POISON, POISON_OIL);

    public static final FT_Polluting P_FUEL = new FT_Polluting()
            .burn(PollutionType.SOOT, SOOT_REFINED_OIL)
            .release(PollutionType.POISON, POISON_OIL);

    public static final FT_Polluting P_FUEL_LEADED = new FT_Polluting()
            .burn(PollutionType.SOOT, SOOT_REFINED_OIL)
            .burn(PollutionType.HEAVYMETAL, LEAD_FUEL)
            .release(PollutionType.POISON, POISON_OIL)
            .release(PollutionType.HEAVYMETAL, LEAD_FUEL * 0.1f);

    public static final FT_Polluting P_GAS = new FT_Polluting()
            .burn(PollutionType.SOOT, SOOT_GAS);

    public static final FT_Polluting P_LIQUID_GAS = new FT_Polluting()
            .burn(PollutionType.SOOT, SOOT_GAS * 2f);

    private ModFluidPollutionPresets() {}
}
