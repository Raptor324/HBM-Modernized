package com.hbm_m.api.redstoneoverradio;

/** Port of {@code api.hbm.redstoneoverradio.IRORValueProvider} (1.7.10 Original). */
public interface IRORValueProvider extends IRORInfo {
    /** Grabs the specified value from this ROR component; must not cause any changes to the component itself. */
    String provideRORValue(String name);
}
