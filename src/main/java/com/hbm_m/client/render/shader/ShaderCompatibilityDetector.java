package com.hbm_m.client.render.shader;


import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.main.MainRegistry;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/**
 * Cross-loader detector of the Iris/Oculus shader state.
 *
 * <p>Detects whether an Iris pipeline is active via reflection, so the VBO
 * renderer can correctly route draw calls through an Iris ExtendedShader
 * instead of raw-GL against the vanilla shader Iris has replaced (otherwise
 * "GL No active program").</p>
 *
 * <p><b>Loader-agnostic:</b> mod presence is checked via
 * {@link dev.architectury.platform.Platform} (works on Forge/NeoForge/Fabric),
 * so the class body is 100% common - only the client annotation is gated
 * ({@code @OnlyIn} on forge/neoforge, {@code @Environment} on fabric).</p>
 */
@OnlyIn(Dist.CLIENT)
public class ShaderCompatibilityDetector {

    private ShaderCompatibilityDetector() {}

    private static boolean initialized = false;

    // Cache for performance and for access from background threads (Sodium chunk builder)
    private static boolean lastState = false;
    /**
     * Thread-safe cache: updated only from the render thread, read from any thread.
     * Sodium builds chunks on background threads - they cannot call the Iris API directly.
     */
    private static volatile boolean cachedShaderActive = false;
    private static volatile boolean cachedShadowPass = false;
    /** Deferred invalidation - processed in ClientTickEvent.END */
    private static volatile boolean pendingChunkInvalidation = false;
    /**
     * Deferred FULL render-cache invalidation on a pack enable/disable flip,
     * processed together with {@link #pendingChunkInvalidation} in
     * {@link #processPendingChunkInvalidation()} (ClientTickEvent.END).
     * Without it the Tier-1 bake path's GPU state (bake pools at 1.5x headroom,
     * per-cache SSBOs, shared stores) plus the Iris companion meshes and their
     * lightmap rings stayed allocated after the pack was switched off - the
     * VRAM bar stayed pinned right under its peak until F3+T (audit 2026-10-07
     * VRAM ledger [C]). Both transition directions use the same teardown; every
     * subsystem it releases rebuilds lazily on the next use, so nothing that a
     * re-enabled pack still needs is lost.
     */
    private static volatile boolean pendingRenderCacheInvalidation = false;

    private static void init() {
        if (initialized) return;
        // The IrisApi v0 reflection (dual-package lookup + MethodHandle bind)
        // lives in IrisReflectionBridge - the single reflection point.
        IrisReflectionBridge.ensureIrisApi();
        if (IrisReflectionBridge.irisApiInstance() != null) {
            MainRegistry.LOGGER.info("ShaderCompatibilityDetector: API found and cached (MH={}).",
                    IrisReflectionBridge.irisPackInUseHandle() != null);
        }
        initialized = true;
    }

    /**
     * Polls the Iris API once per frame (from
     * {@link com.hbm_m.client.render.ClientRenderFlags#onFrameStart}).
     * Previously {@link #isExternalShaderActive()} hit the MethodHandle on every
     * call (per-part per-BE per-pass) - on machine farms that cost percent-level
     * frame time.
     */
    public static void updateState() {
        // Block id disguises: poll for a not-yet-disguised loaded pack. FIRST thing
        // in the method - the IrisApi early-return below must not gate it.
        ShaderBlockIdDisguises.applyIfPackChanged();
        if (!initialized) init();
        if (IrisReflectionBridge.irisApiInstance() == null
                || (IrisReflectionBridge.irisPackInUseHandle() == null && IrisReflectionBridge.irisPackInUseMethod() == null)) {
            cachedShaderActive = false;
            return;
        }
        try {
            boolean isActive;
            if (IrisReflectionBridge.irisPackInUseHandle() != null) {
                isActive = (boolean) IrisReflectionBridge.irisPackInUseHandle()
                        .invokeExact((Object) IrisReflectionBridge.irisApiInstance());
            } else {
                Boolean inUse = (Boolean) IrisReflectionBridge.irisPackInUseMethod()
                        .invoke(IrisReflectionBridge.irisApiInstance());
                isActive = inUse != null && inUse;
            }

            cachedShaderActive = isActive;

            if (isActive != lastState) {
                MainRegistry.LOGGER.info("Shader state changed: {}", isActive ? "Active" : "Inactive");
                lastState = isActive;
                OcclusionCullingHelper.clearCache();
                // Defer invalidation - calling from the render loop breaks the Sodium iteration (wrapped is null)
                pendingChunkInvalidation = true;
                // Full GPU teardown on EITHER direction (vanilla->pack frees the
                // vanilla-path renderer VBOs that the bake path replaced; pack->vanilla
                // frees the bake pools, per-cache SSBOs, companion meshes and lightmap
                // rings - all lazily rebuilt). RenderCacheManager.invalidateAll itself
                // defers to the render thread via recordRenderCall.
                pendingRenderCacheInvalidation = true;
            }
        } catch (Throwable e) {
            cachedShaderActive = false;
        }
    }

    public static boolean isExternalShaderActive() {
        // O(1): state is polled once per frame in updateState().
        // Background threads (Sodium chunk builder) also read this cache.
        return cachedShaderActive;
    }

    /**
     * Call from ClientTickEvent.END - invalidates chunks on shader change.
     * Do NOT call from the render loop - it breaks the Sodium iteration
     * (ReferenceOpenHashSet.wrapped is null).
     */
    public static void processPendingChunkInvalidation() {
        if (pendingRenderCacheInvalidation) {
            pendingRenderCacheInvalidation = false;
            MainRegistry.LOGGER.info(
                    "ShaderCompatibilityDetector: shader pack toggled - full render cache invalidation");
            com.hbm_m.client.render.cache.RenderCacheManager.invalidateAll(
                    com.hbm_m.client.render.cache.RenderCacheManager.Reason.RESOURCE_RELOAD);
        }
        if (!pendingChunkInvalidation) return;
        pendingChunkInvalidation = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            try {
                mc.levelRenderer.allChanged();
            } catch (Exception e) {
                MainRegistry.LOGGER.debug("Chunk invalidation on shader change: {}", e.getMessage());
            }
        }
    }

    /**
     * Checks whether Iris's shadow pass is currently rendering (for realtime shadows).
     *
     * <p>Cached in a primitive boolean field updated per frame/render phase (head/return of renderShadows),
     * completely eliminating 100k-300k MethodHandle invocations per second.</p>
     */
    public static boolean isRenderingShadowPass() {
        return cachedShaderActive && cachedShadowPass;
    }

    public static void setRenderingShadowPass(boolean active) {
        cachedShadowPass = active;
    }

    /**
     * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer#shouldRenderOffScreen}.
     * When {@code true}, Sodium/vanilla still invoke BER even if the BE AABB is outside
     * the main camera frustum - required so shader-pack shadow maps include off-screen
     * casters whose shadows remain visible on screen.
     */
    public static boolean shouldRenderBlockEntityOffScreen() {
        return isExternalShaderActive();
    }

    /**
     * True when an active Iris pipeline can hand out an {@code ExtendedShader} for our raw-GL
     * draws. When false, callers should fall back to the vanilla shader path or to
     * {@code bufferSource.putBulkData} delegation.
     */
    public static boolean canUseIrisExtendedShader() {
        return isExternalShaderActive() && IrisExtendedShaderAccess.isReflectionAvailable();
    }

    /**
     * Static geometry of machines/doors is always provided by the BER/VBO system.
     * Baked world quads are not used for these models.
     */
    public static boolean useVboGeometry() {
        return true;
    }
}
