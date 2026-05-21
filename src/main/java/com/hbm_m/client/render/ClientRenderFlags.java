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

    private ClientRenderFlags() {}

    /** Call from {@link com.hbm_m.client.render.culling.InstancedRenderFrame#onBeforeBlockEntities}. */
    public static void onFrameStart() {
        ModClothConfig cfg = ModClothConfig.get();
        useInstancedBatching = cfg.useInstancedStaticRendering;
        gpuBoneSkinning = cfg.gpuBoneSkinning;
        enableOcclusionCulling = cfg.enableOcclusionCulling;
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
}
