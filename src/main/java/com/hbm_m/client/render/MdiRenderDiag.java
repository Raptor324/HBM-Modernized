package com.hbm_m.client.render;

import java.util.concurrent.atomic.AtomicBoolean;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

/**
 * Диагностика MDI через {@link ModClothConfig} (раздел rendering).
 */
public final class MdiRenderDiag {

    private static final AtomicBoolean BANNER = new AtomicBoolean();

    private MdiRenderDiag() {}

    private static ModClothConfig cfg() {
        try {
            return ModClothConfig.get();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isDebugEnabled() {
        ModClothConfig c = cfg();
        return c != null && c.mdiDebugLogDispatch;
    }

    public static boolean isVerboseEnabled() {
        ModClothConfig c = cfg();
        return c != null && c.mdiVerboseSubdraws;
    }

    public static boolean isForceSequentialDraw() {
        ModClothConfig c = cfg();
        return c != null && c.mdiForceSequentialDraw;
    }

    public static void logBannerOnce() {
        if (!isDebugEnabled() && !isVerboseEnabled() && !isForceSequentialDraw()) {
            return;
        }
        if (!BANNER.compareAndSet(false, true)) {
            return;
        }
        MainRegistry.LOGGER.info(
                "[HBM-M MDI] Диагностика (Cloth → rendering): mdiDebugLogDispatch, mdiVerboseSubdraws, mdiForceSequentialDraw, mdiUseTrueMultiDraw.");
    }
}
