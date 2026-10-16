package com.hbm_m.client.render;

import com.hbm_m.config.ModClothConfig;

/**
 * Per-render-frame snapshot of hot {@link ModClothConfig} rendering flags.
 * Updated once at the start of the block-entity pass to avoid thousands of
 * {@link ModClothConfig#get()} calls when instancing large machine fields.
 * <p>
 * Instancing/MDI/GPU-bone skinning are enabled automatically and no longer have
 * config toggles; the only manual fallback is {@link #forceVanillaImmediate()}
 * (the vanilla immediate path in full).
 */
public final class ClientRenderFlags {

    private static boolean enableOcclusionCulling;
    /** Same default as {@link ModClothConfig#maxInstancedInstancesPerPart} until {@link #onFrameStart()}. */
    private static int maxInstances = 4096;
    private static boolean forceVanillaImmediate;
    private static boolean mdiCleanFrameReuse = true;
    private static boolean nucleusDirtySkip = true;

    /**
     * Emergency JVM override {@code -Dhbm.dirtySkip=false}: the config
     * ({@code nucleusRenderDirtySkip}) is the primary source; the flag can only force OFF.
     * Read once at class load.
     */
    private static final boolean DIRTY_SKIP_FLAG =
            !"false".equalsIgnoreCase(System.getProperty("hbm.dirtySkip", "true"));

    /** Last Iris states: a change invalidates fast-path records (worldGen bump). */
    private static boolean lastIrisExternal = false;
    private static boolean lastIrisExtended = false;

    private ClientRenderFlags() {}

    /** Call from {@link com.hbm_m.client.render.culling.InstancedRenderFrame#onBeforeBlockEntities}. */
    public static void onFrameStart() {
        ModClothConfig cfg = ModClothConfig.get();
        enableOcclusionCulling = cfg.enableOcclusionCulling;
        maxInstances = cfg.maxInstancedInstancesPerPart;
        forceVanillaImmediate = cfg.forceVanillaImmediatePath;
        mdiCleanFrameReuse = cfg.mdiCleanFrameReuse;
        nucleusDirtySkip = cfg.nucleusRenderDirtySkip && DIRTY_SKIP_FLAG;
        // One Iris API poll per frame - isExternalShaderActive() reads the cache afterwards.
        com.hbm_m.client.render.shader.ShaderCompatibilityDetector.updateState();
        // Iris state change: fast-path records lack instanceLightUV, which only Iris
        // paths fill in - global invalidation via worldGen.
        boolean irisExternal = com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive();
        boolean irisExtended = com.hbm_m.client.render.shader.ShaderCompatibilityDetector.canUseIrisExtendedShader();
        if (irisExternal != lastIrisExternal || irisExtended != lastIrisExtended) {
            lastIrisExternal = irisExternal;
            lastIrisExtended = irisExtended;
            NucleusRenderVersion.bump();
        }
    }

    /**
     * Instanced batching is always enabled unless the user forces the vanilla
     * immediate path via {@link ModClothConfig#forceVanillaImmediatePath}.
     */
    public static boolean useInstancedBatching() {
        return !forceVanillaImmediate;
    }

    /**
     * GPU bone skinning (per-vertex bone id + per-instance base×part matrix) is
     * applied automatically wherever the engine uses it; no user toggle.
     */
    public static boolean gpuBoneSkinning() {
        return true;
    }

    public static boolean enableOcclusionCulling() {
        return enableOcclusionCulling;
    }

    /**
     * MDI clean-frame reuse (submitClean/retained draw list). Kill-switch
     * {@link ModClothConfig#mdiCleanFrameReuse}; default on.
     */
    public static boolean mdiCleanFrameReuse() {
        return mdiCleanFrameReuse;
    }

    /**
     * Fast-path dirty-skip (GPU-driven machine assembly): clean BlockEntities
     * ({@code RenderDirtyTracker}) skip the per-frame rebuild and confirm presence
     * via a roster assert. Kill-switches: {@link ModClothConfig#nucleusRenderDirtySkip}
     * or {@code -Dhbm.dirtySkip=false}.
     */
    public static boolean nucleusDirtySkip() {
        return nucleusDirtySkip;
    }

    /**
     * True {@code glDrawElementsInstanced} under Iris/Oculus via our ExtendedShader.
     * Enabled automatically when the active pack's gbuffer scheme is recognized and
     * an encoder exists ({@link com.hbm_m.client.render.shader.IrisInstancedEncoders} -
     * detected from the pack's gbuffer program source, not pack names; the "packed"
     * scheme packs gbuffer into unorm8 pairs: albedo/normal/light).
     * <p>
     * Without a recognized scheme, main-pass instancing falls back to per-instance
     * companion via the pack's BLOCK_ENTITY program, and the shadow batch to the
     * pack's SHADOW_* program: correct under any pack. A single-target "vanilla"
     * FSH under a deferred pack renders black machines (the composite decodes
     * garbage), so instancing is never enabled blindly.
     */
    public static boolean irisTrueInstancing() {
        return com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()
                && com.hbm_m.client.render.shader.IrisInstancedEncoders.hasEncoder();
    }

    /**
     * Force the vanilla immediate (putBulkData) path for every machine render.
     * Read live (also safe before {@link #onFrameStart}).
     */
    public static boolean forceVanillaImmediate() {
        try {
            return ModClothConfig.get().forceVanillaImmediatePath;
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
