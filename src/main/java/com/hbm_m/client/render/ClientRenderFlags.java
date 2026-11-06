package com.hbm_m.client.render;

import com.hbm_m.config.ModClothConfig;

import net.minecraft.util.Mth;

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

    /**
     * True while a WIAJ demo screen ({@code GuiWorldInAJar}) renders its scene:
     * machines must draw through the per-BE VBO tier with the screen's own
     * PoseStack/bufferSource instead of registering instances in the level's
     * instanced graph, and block_lit fog uniforms must be forced off (stale
     * level fog would wash the scene out at GUI depth). Set/cleared around the
     * scene pass in {@code GuiWorldInAJar}.
     */
    private static boolean guiRenderFrame = false;

    /**
     * Global machine alpha inside the WIAJ demo scene (0-1, 1 = opaque). The demo's
     * smooth-placement actor wrapper ({@code ActorTileEntity}) drives it for the
     * fade-in of a machine being lowered into place; {@code MachineBer} reads it in
     * the {@link #guiRenderFrame()} branch and it flows into the regular per-part
     * fade pipeline (blend + depthMask off, fade uniform / translucent putBulkData).
     * Reset to 1 around every actor render, so a plain demo frame is unchanged.
     */
    private static float guiFadeAlpha = 1.0F;

    private ClientRenderFlags() {}

    /** Call from {@link com.hbm_m.client.render.culling.InstancedRenderFrame#onBeforeBlockEntities}. */
    public static void onFrameStart() {
        ModClothConfig cfg = ModClothConfig.get();
        // Fallback flipped live (either direction): retained MDI records and
        // coordinator caches hold instanced draws - drop them so no stale copy
        // survives the switch (part-renderer buffers are discarded in flush()).
        if (forceVanillaImmediate != cfg.forceVanillaImmediatePath) {
            MdiBatchCoordinator.clearCachedRedraw();
        }
        enableOcclusionCulling = cfg.enableOcclusionCulling;
        maxInstances = cfg.maxInstancedInstancesPerPart;
        forceVanillaImmediate = cfg.forceVanillaImmediatePath;
        mdiCleanFrameReuse = cfg.mdiCleanFrameReuse;
        nucleusDirtySkip = cfg.nucleusRenderDirtySkip && DIRTY_SKIP_FLAG;
        // One Iris API poll per frame - isExternalShaderActive() reads the cache afterwards.
        com.hbm_m.client.render.shader.ShaderCompatibilityDetector.updateState();
        com.hbm_m.client.render.shader.ShaderCompatibilityDetector.setRenderingShadowPass(false);
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
     * Enabled automatically when the active pack's gbuffer layout has a VERIFIED
     * encoder ({@link com.hbm_m.client.render.shader.IrisInstancedEncoders},
     * detected from the pack's gbuffer program source, never pack names): the
     * byte-paired packing scheme (albedo/normal/light packed into unorm8 pairs -
     * verified against Photon). This is the ONLY recognized scheme: a general
     * vanilla-parity encoder for identity-directive packs was tried and removed -
     * an identity directive alone does not guarantee content parity, and shaderpacks such as BSL 10.x
     * / Insanity / Flashlight composite black through it.
     * <p>
     * For every other pack there is no valid generic encoder, so main-pass
     * instancing falls back to per-instance companion via the pack's
     * BLOCK_ENTITY program, and the shadow batch to the pack's SHADOW_* program:
     * correct under any pack (verified: the Complementary family renders through
     * exactly this path). A wrong-content instanced write under a deferred pack
     * renders black/corrupted machines (the composite decodes garbage), so
     * instancing is never enabled blindly.
     */
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

    public static boolean guiRenderFrame() {
        return guiRenderFrame;
    }

    public static void setGuiRenderFrame(boolean value) {
        guiRenderFrame = value;
    }

    public static float guiFadeAlpha() {
        return guiFadeAlpha;
    }

    public static void setGuiFadeAlpha(float alpha) {
        guiFadeAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
    }
}
