package com.hbm_m.client.render.culling;

import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Lightweight per-frame counters for instanced / MDI diagnostics (Cloth: enableDebugLogging or mdiDebugLogDispatch).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class InstancedRenderStats {

    private static int chunkSlicesDeferred;
    private static int duplicatePresentAttempts;
    private static int overflowAdds;
    private static long presentStartNanos;

    private InstancedRenderStats() {}

    public static void beginPresent(int chunkSlices) {
        chunkSlicesDeferred = chunkSlices;
        duplicatePresentAttempts = 0;
        overflowAdds = InstancedStaticPartRenderer.drainOverflowAddCount();
        presentStartNanos = System.nanoTime();
    }

    public static void recordDuplicatePresentAttempt() {
        duplicatePresentAttempts++;
    }

    public static void endPresent() {
        if (!shouldLog()) {
            return;
        }
        int overflow = InstancedStaticPartRenderer.drainOverflowAddCount();
        long ms = (System.nanoTime() - presentStartNanos) / 1_000_000L;
        MainRegistry.LOGGER.debug(
                "[HBM-M Instanced] present chunkSlices={} tookMs={} overflowAdds={} dupFlushBlocked={}",
                chunkSlicesDeferred, ms, overflow, duplicatePresentAttempts);
    }

    private static boolean shouldLog() {
        try {
            ModClothConfig c = ModClothConfig.get();
            return c != null && (c.enableDebugLogging || c.mdiDebugLogDispatch);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void clear() {
        chunkSlicesDeferred = 0;
        duplicatePresentAttempts = 0;
        overflowAdds = 0;
        presentStartNanos = 0L;
        InstancedStaticPartRenderer.drainOverflowAddCount();
    }
}
