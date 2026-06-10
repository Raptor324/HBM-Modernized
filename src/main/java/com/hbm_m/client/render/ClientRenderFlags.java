package com.hbm_m.client.render;

import com.hbm_m.config.ModClothConfig;

/**
 * Per-render-frame snapshot of hot {@link ModClothConfig} rendering flags.
 * Updated once at the start of the block-entity pass to avoid thousands of
 * {@link ModClothConfig#get()} calls when instancing large machine fields.
 */
public final class ClientRenderFlags {

    private static boolean useInstancedBatching;
    private static boolean gpuBoneSkinning;
    private static boolean enableOcclusionCulling;
    private static boolean useSlicedLight;
    /** Same default as {@link ModClothConfig#maxInstancedInstancesPerPart} until {@link #onFrameStart()}. */
    private static int maxInstances = 4096;

    private ClientRenderFlags() {}

    /** Call from {@link com.hbm_m.client.render.culling.InstancedRenderFrame#onBeforeBlockEntities}. */
    public static void onFrameStart() {
        ModClothConfig cfg = ModClothConfig.get();
        useInstancedBatching = cfg.useInstancedStaticRendering;
        gpuBoneSkinning = cfg.gpuBoneSkinning;
        enableOcclusionCulling = cfg.enableOcclusionCulling;
        useSlicedLight = cfg.useSlicedLight;
        maxInstances = cfg.maxInstancedInstancesPerPart;
    }

    public static boolean useInstancedBatching() {
        return useInstancedBatching;
    }

    public static boolean gpuBoneSkinning() {
        return gpuBoneSkinning;
    }

    public static boolean enableOcclusionCulling() {
        return enableOcclusionCulling;
    }

    /** {@link ModClothConfig#useSlicedLight} — when false, MDI atlas path can batch unsliced parts. */
    public static boolean useSlicedLight() {
        return useSlicedLight;
    }

    /**
     * Read at renderer init (may run before {@link #onFrameStart}); falls back to live config.
     */
    public static boolean useSlicedLightForNewRenderer() {
        try {
            return ModClothConfig.get().useSlicedLight;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Per-part instance cap. Safe before {@link #onFrameStart()} (renderer construction, buffer sizing).
     */
    public static int maxInstances() {
        if (maxInstances > 0) {
            return maxInstances;
        }
        try {
            int cfg = ModClothConfig.get().maxInstancedInstancesPerPart;
            return cfg > 0 ? cfg : 4096;
        } catch (Throwable ignored) {
            return 4096;
        }
    }
}
