package com.hbm_m.client.render.culling;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Temporal hysteresis for GPU visibility — avoids 1-frame depth/readback flicker
 * on large multiblock AABBs (lag-1 depth, sparse screen samples).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class GpuCullVisibilityStabilizer {

    /** Consecutive GPU-occluded frames before hiding an instance. */
    private static final int HIDE_STREAK_TO_CULL = 10;
    /** Consecutive GPU-visible frames before showing after being hidden (reduces 1-frame flicker). */
    private static final int SHOW_STREAK_TO_REVEAL = 2;
    private static final int MAX_ENTRIES = 32768;

    private static final Long2IntOpenHashMap hiddenStreak = new Long2IntOpenHashMap();
    private static final Long2IntOpenHashMap visibleStreak = new Long2IntOpenHashMap();

    private GpuCullVisibilityStabilizer() {}

    /**
     * @param occlusionKey {@link OcclusionCullingHelper} cache key (pos + shadow phase); 0 = no hysteresis
     */
    public static boolean shouldRenderInstance(long occlusionKey, boolean gpuVisible) {
        if (occlusionKey == 0L) {
            return gpuVisible;
        }
        if (gpuVisible) {
            int show = visibleStreak.getOrDefault(occlusionKey, 0) + 1;
            visibleStreak.put(occlusionKey, show);
            hiddenStreak.remove(occlusionKey);
            if (show >= SHOW_STREAK_TO_REVEAL) {
                return true;
            }
            // Still warming up visibility — keep drawing until streak confirms (avoids pop-in flicker).
            return true;
        }
        visibleStreak.remove(occlusionKey);
        int streak = hiddenStreak.getOrDefault(occlusionKey, 0) + 1;
        hiddenStreak.put(occlusionKey, streak);
        if (streak >= HIDE_STREAK_TO_CULL) {
            return false;
        }
        return true;
    }

    public static void onFrameStart() {
        if (hiddenStreak.size() > MAX_ENTRIES || visibleStreak.size() > MAX_ENTRIES) {
            hiddenStreak.clear();
            visibleStreak.clear();
            return;
        }
        var it = hiddenStreak.long2IntEntrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getIntValue() >= HIDE_STREAK_TO_CULL + 12) {
                it.remove();
            }
        }
    }

    public static void clear() {
        hiddenStreak.clear();
        visibleStreak.clear();
    }
}
