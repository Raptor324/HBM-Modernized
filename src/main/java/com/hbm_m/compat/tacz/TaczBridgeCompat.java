package com.hbm_m.compat.tacz;
//? if forge {
import net.minecraftforge.fml.ModList;
//?} elif neoforge {
/*import net.neoforged.fml.ModList;
*///?}

/**
 * Presence + runtime gate for the {@code hbm_tacz} addon (the TACZ hybrid SEDNA bridge).
 *
 * <p>This class deliberately knows nothing about TACZ or the addon's classes — it only checks
 * mod presence so the config GUI can show/hide the bridge switch, and exposes the runtime
 * master switch value ({@code ModClothConfig.taczBridge}) for the addon's own gates. All TACZ
 * wiring itself lives in the {@code hbm_tacz} addon; without it in the modlist this class is
 * inert and nothing in HBM-Modernized changes behavior.
 */
public final class TaczBridgeCompat {

    public static final String ADDON_MOD_ID = "hbm_tacz";

    private TaczBridgeCompat() {}

    /** {@code true} when the hbm_tacz addon is in the modlist (config entry becomes visible). */
    public static boolean isLoaded() {
        try {
            return ModList.get() != null && ModList.get().isLoaded(ADDON_MOD_ID);
        } catch (Throwable t) {
            // ModList can be absent during very early phases (schema static init).
            return false;
        }
    }

    /**
     * Runtime master switch: {@code true} (default) = the addon bridges weapons into TACZ;
     * {@code false} = weapons render and behave through the vanilla SEDNA pipeline as if the
     * addon were absent. Always {@code false} when the addon is not loaded, so the value can
     * be polled unconditionally.
     */
    public static boolean bridgeEnabled() {
        return isLoaded() && com.hbm_m.config.ModClothConfig.get().taczBridge;
    }
}
