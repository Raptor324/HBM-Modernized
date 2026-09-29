package com.hbm_m.client.render.shader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.main.MainRegistry;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} else if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

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
    private static Method irisIsShaderPackInUse = null;
    private static Method irisIsRenderingShadowPass = null;
    private static Object irisApiInstance = null;

    /**
     * Hot-path MethodHandles for the two Iris API queries invoked every frame
     * (often per-BE per-pass). {@link Method#invoke} boxes arguments into
     * {@code Object[]} and re-runs reflection access checks each call;
     * {@link MethodHandle#invokeExact} is JIT friendly and avoids both. Bound
     * via {@code asType()} to {@code (Object)boolean} so call sites can
     * invokeExact without knowing the concrete IrisApi class.
     */
    private static MethodHandle irisIsShaderPackInUseMH = null;
    private static MethodHandle irisIsRenderingShadowPassMH = null;

    // Cache for performance and for access from background threads (Sodium chunk builder)
    private static boolean lastState = false;
    /**
     * Thread-safe cache: updated only from the render thread, read from any thread.
     * Sodium builds chunks on background threads - they cannot call the Iris API directly.
     */
    private static volatile boolean cachedShaderActive = false;
    /** Deferred invalidation - processed in ClientTickEvent.END */
    private static volatile boolean pendingChunkInvalidation = false;

    private static void init() {
        if (initialized) return;

        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstanceMethod = irisApiClass.getMethod("getInstance");
            irisApiInstance = getInstanceMethod.invoke(null);
            irisIsShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
            irisIsRenderingShadowPass = irisApiClass.getMethod("isRenderingShadowPass");

            // MethodHandle bind. Both methods return primitive `boolean`,
            // so adapt to (Object)boolean so the call sites can invokeExact
            // without an extra unboxing hop.
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                irisIsShaderPackInUse.setAccessible(true);
                irisIsRenderingShadowPass.setAccessible(true);
                irisIsShaderPackInUseMH = lookup.unreflect(irisIsShaderPackInUse)
                        .asType(MethodType.methodType(boolean.class, Object.class));
                irisIsRenderingShadowPassMH = lookup.unreflect(irisIsRenderingShadowPass)
                        .asType(MethodType.methodType(boolean.class, Object.class));
            } catch (Throwable mhFail) {
                MainRegistry.LOGGER.warn("ShaderCompatibilityDetector: MethodHandle binding failed ({}), using Method.invoke", mhFail.toString());
                irisIsShaderPackInUseMH = null;
                irisIsRenderingShadowPassMH = null;
            }

            MainRegistry.LOGGER.info("ShaderCompatibilityDetector: API found and cached (MH={}).",
                    irisIsShaderPackInUseMH != null);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // Iris / Oculus not present on classpath
        } catch (Throwable e) {
            MainRegistry.LOGGER.error("ShaderCompatibilityDetector: Failed to cache API", e);
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
        if (!initialized) init();
        if (irisApiInstance == null || (irisIsShaderPackInUseMH == null && irisIsShaderPackInUse == null)) {
            cachedShaderActive = false;
            return;
        }
        try {
            boolean isActive;
            if (irisIsShaderPackInUseMH != null) {
                isActive = (boolean) irisIsShaderPackInUseMH.invokeExact((Object) irisApiInstance);
            } else {
                Boolean inUse = (Boolean) irisIsShaderPackInUse.invoke(irisApiInstance);
                isActive = inUse != null && inUse;
            }

            cachedShaderActive = isActive;

            if (isActive != lastState) {
                MainRegistry.LOGGER.info("Shader state changed: {}", isActive ? "Active" : "Inactive");
                lastState = isActive;
                OcclusionCullingHelper.clearCache();
                // Defer invalidation - calling from the render loop breaks the Sodium iteration (wrapped is null)
                pendingChunkInvalidation = true;
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
     * <p>Hot-path note: this is queried per-part from the BER/collect paths, so the
     * reflection is skipped entirely when no Iris pipeline is active (the common case)
     * - {@code isRenderingShadowPass()} can only be true while a pack is in use.
     * The result is NOT frame-cached: Iris starts the shadow pass mid-frame (after
     * {@code ClientRenderFlags.onFrameStart}), so a frame-level snapshot would feed
     * stale {@code false} into shadow instancing routing.</p>
     */
    public static boolean isRenderingShadowPass() {
        if (!initialized) init();
        if (irisApiInstance == null || !cachedShaderActive) return false;
        try {
            boolean result;
            if (irisIsRenderingShadowPassMH != null) {
                result = (boolean) irisIsRenderingShadowPassMH.invokeExact((Object) irisApiInstance);
            } else if (irisIsRenderingShadowPass != null) {
                Boolean boxed = (Boolean) irisIsRenderingShadowPass.invoke(irisApiInstance);
                result = boxed != null && boxed;
            } else {
                return false;
            }
            // 1.21.1 diagnostics (shadows not cast): confirm that the shadow pass
            // is detected at all via IrisApi on this loader.
            if (result && !loggedShadowPassDetected) {
                loggedShadowPassDetected = true;
                MainRegistry.LOGGER.info(
                        "ShaderCompatibilityDetector: Iris shadow pass detected (isRenderingShadowPass=true) - API works on this loader");
            }
            return result;
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean loggedShadowPassDetected = false;

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
