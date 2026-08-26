package com.hbm_m.compat.dh;

import dev.architectury.platform.Platform;

/**
 * Soft-dependency helper for Distant Horizons.
 * No hard import of DH classes — class-loading is via reflection/String targets,
 * so the mod compiles and runs without DH installed.
 */
public final class DhCompat {

    private static Boolean cachedModPresent;

    private DhCompat() {}

    public static boolean isModPresent() {
        if (cachedModPresent != null) return cachedModPresent;
        boolean present = false;
        try {
            present = Platform.isModLoaded("distanthorizons");
        } catch (Throwable ignored) {}
        if (!present) {
            try {
                Class.forName("com.seibel.distanthorizons.api.DhApi");
                present = true;
            } catch (Throwable ignored) {}
        }
        if (!present) {
            try {
                Class.forName("com.seibel.distanthorizons.core.render.renderer.LodRenderer");
                present = true;
            } catch (Throwable ignored) {}
        }
        cachedModPresent = present;
        return present;
    }

    /** For mixin plugin — resets cache in tests if needed. */
    static void resetCacheForTests() {
        cachedModPresent = null;
    }
}
