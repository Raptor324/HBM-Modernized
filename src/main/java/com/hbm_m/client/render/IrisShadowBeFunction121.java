package com.hbm_m.client.render;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.RenderBuffers;

/**
 * 1.21.1 shadow-pass block-entity dispatch for Nucleus machines.
 * <p>
 * <b>Why this exists.</b> On Iris 1.20.1 / Oculus, block entities reach the
 * shadow pass through a {@code BlockEntityRenderFunction} that the shader mod
 * itself installs (or through Embeddium's lists via the vanilla-list path),
 * so our machines - dispatched through Sodium/Embeddium's culled lists in the
 * MAIN pass - simply flow into the shadow dispatcher. On Iris 1.21.1 that
 * integration point vanished: a grep over the Iris 1.21.1 tree proves
 * {@code ShadowRenderingState.setBlockEntityRenderFunction} has ZERO callers
 * (the only reference is the setter itself,
 * {@code common/src/main/java/net/irisshaders/iris/shadows/ShadowRenderingState.java:14}),
 * so the field keeps its initializer value
 * {@code ShadowRenderer::renderBlockEntities} forever, and that method iterates
 * {@code ShadowRenderer.visibleBlockEntities} - a list populated ONLY by
 * {@code MixinPreventRebuildNearInShadowPass} (vanilla {@code LevelRenderer}
 * {@code setupRender} tail), which self-documents that it does not apply when a
 * terrain renderer is installed, and under Sodium the vanilla sections it
 * iterates are never built. Net effect: on 1.21.1 + Iris + Sodium block
 * entities are STRUCTURALLY never dispatched in the shadow pass, and our
 * machines cast no shadows. Iris's own Sodium shadow compat
 * ({@code iris/compat/sodium/mixin/MixinSodiumWorldRenderer.java} +
 * {@code MixinRenderSectionManagerShadow.java}) covers terrain render lists
 * only, no BEs.
 * <p>
 * <b>What this does.</b> The first time a shadow pass actually starts (hooked
 * from {@code MixinIrisShadowRenderer.nucleus$onShadowPassStart} - at that
 * point Iris is present, a pack is active and the Iris shadow classes are
 * already loaded and initialized), this class installs a
 * {@code ShadowRenderingState.BlockEntityRenderFunction} implemented as a
 * {@link Proxy} over the Iris interface (resolved reflectively - Iris is a
 * runtime-only dependency, same policy as
 * {@code ShaderCompatibilityDetector}'s IrisApi access; no compile-time Iris
 * types anywhere in this file).
 * <p>
 * The wrapper preserves the vanilla-list behavior exactly: the previous
 * function is - provably, per the grep above - the method reference
 * {@code ShadowRenderer::renderBlockEntities}, whose target method is
 * {@code public} (ShadowRenderer.java:591) and stateless beyond its arguments,
 * so invoking it reflectively on the passed {@code ShadowRenderer} instance
 * with the passed arguments is byte-for-byte the previous behavior. No
 * accessor mixin is needed to "capture before overwrite": the field has no
 * other writer on 1.21.1 (see the grep above; re-verified 2026-10-15), and no
 * code path runs between the initializer and our install that could change
 * it. If a third-party mod (Flywheel/DH) ever DOES install its own function
 * before our first shadow pass, we would overwrite theirs - mitigation is
 * documented in the report; our wrapper chains to
 * {@code ShadowRenderer::renderBlockEntities}, so the vanilla list is never
 * lost either way.
 * <p>
 * After chaining to the vanilla list, the wrapper renders every live Nucleus
 * machine through {@link NucleusDispatcherBypass#renderShadowBlockEntities},
 * mirroring the exact loop contract of
 * {@code ShadowRenderer.renderBlockEntities} (ShadowRenderer.java:591-617):
 * the passed {@code PoseStack} is Iris's shadow MODELVIEW-derived stack and is
 * mutated in place - per BE {@code pushPose → translate(pos − camera) →
 * dispatcher.render(be, tickDelta, poseStack, buffers.bufferSource()) →
 * popPose} - never cleared or replaced. The dispatcher call lands in our
 * BERs, which during the shadow pass record into
 * {@link IrisShadowBatchCollector} - the same Tier 1/2/3 record pipeline the
 * 1.20.1 path uses, untouched.
 * <p>
 * Guards: inert without Iris (never installed - the hook only fires from
 * inside {@code ShadowRenderer.renderShadows}), inert without machines
 * (empty presence buckets return 0 immediately), and compiled to a no-op on
 * 1.20.1 (stonecutter gate below - Oculus 1.20.1 owns its own shadow BE
 * function and must not be replaced). Static field state survives resource
 * reloads (nothing in Iris resets it), so once per session is sufficient.
 */
public final class IrisShadowBeFunction121 {

    private static final String STATE_CLASS = "net.irisshaders.iris.shadows.ShadowRenderingState";
    private static final String FUNCTION_IFACE = "net.irisshaders.iris.shadows.ShadowRenderingState$BlockEntityRenderFunction";
    private static final String SHADOW_RENDERER_CLASS = "net.irisshaders.iris.shadows.ShadowRenderer";

    /** Set once the proxy is installed; never uninstalled (Iris never resets the field). */
    private static boolean installed = false;

    /** {@code ShadowRenderer.renderBlockEntities(RenderBuffers, PoseStack, Camera, d, d, d, float, boolean, boolean)} */
    private static Method vanillaRenderBlockEntities;

    private IrisShadowBeFunction121() {}

    /**
     * Called from {@code MixinIrisShadowRenderer} at {@code renderShadows} HEAD
     * (both versions; no-ops everywhere but 1.21.1 and only before first install).
     */
    public static void onShadowPassStart() {
        //? if >= 1.21.1 {
        /*if (installed) {
            return;
        }
        try {
            Class<?> stateClass = Class.forName(STATE_CLASS);
            Class<?> functionClass = Class.forName(FUNCTION_IFACE);

            // Resolve the previous/default function: the field's initializer value,
            // ShadowRenderer::renderBlockEntities (public, ShadowRenderer.java:591).
            Class<?> shadowRendererClass = Class.forName(SHADOW_RENDERER_CLASS);
            vanillaRenderBlockEntities = shadowRendererClass.getMethod("renderBlockEntities",
                    RenderBuffers.class, PoseStack.class, net.minecraft.client.Camera.class,
                    double.class, double.class, double.class,
                    float.class, boolean.class, boolean.class);

            Object proxy = Proxy.newProxyInstance(functionClass.getClassLoader(),
                    new Class<?>[] {functionClass}, IrisShadowBeFunction121::dispatch);

            stateClass.getMethod("setBlockEntityRenderFunction", functionClass)
                    .invoke(null, proxy);
            installed = true;
            MainRegistry.LOGGER.info(
                    "[HBM-M] Installed Iris 1.21.1 shadow BlockEntityRenderFunction "
                            + "(no default install point exists on this version; vanilla list "
                            + "behavior chained through ShadowRenderer::renderBlockEntities)");
        } catch (Throwable t) {
            // Fail soft: without the function machines simply cast no shadows
            // (the pre-existing 1.21.1 behavior); retry on the next shadow pass.
            vanillaRenderBlockEntities = null;
            MainRegistry.LOGGER.warn(
                    "[HBM-M] Could not install Iris 1.21.1 shadow BE function - "
                            + "machines will not cast Iris shadows", t);
        }
        *///?}
    }

    /**
     * InvocationHandler for the proxy: kept as a method reference
     * ({@code (proxy, method, args) -> ...}) so the interface types stay
     * reflective and the file compiles without Iris on the classpath.
     * Signature (Iris 1.21.1, ShadowRenderingState.java:26-28):
     * {@code int renderBlockEntities(ShadowRenderer, RenderBuffers, PoseStack,
     * Camera, double cameraX, cameraY, cameraZ, float tickDelta,
     * boolean hasEntityFrustum, boolean lightsOnly)}.
     */
    private static Object dispatch(Object proxy, Method method, Object[] args) throws Throwable {
        // args: 0=ShadowRenderer, 1=RenderBuffers, 2=PoseStack, 3=Camera,
        //       4/5/6=cameraX/Y/Z, 7=tickDelta, 8=hasEntityFrustum, 9=lightsOnly
        RenderBuffers buffers = (RenderBuffers) args[1];
        PoseStack modelView = (PoseStack) args[2];
        double camX = (Double) args[4];
        double camY = (Double) args[5];
        double camZ = (Double) args[6];
        float tickDelta = (Float) args[7];
        boolean hasEntityFrustum = (Boolean) args[8];
        boolean lightsOnly = (Boolean) args[9];

        // 1) Vanilla list behavior, exactly as before we replaced the field
        //    (the initializer reference is public and stateless beyond args).
        int vanilla = (Integer) vanillaRenderBlockEntities.invoke(args[0],
                buffers, modelView, args[3],
                camX, camY, camZ, tickDelta, hasEntityFrustum, lightsOnly);

        // 2) Our live machines, same loop contract as ShadowRenderer's own loop:
        //    the passed stack IS the shadow modelView, mutated per-BE via
        //    push/translate/render/pop (never cleared).
        int ours = NucleusDispatcherBypass.renderShadowBlockEntities(modelView,
                buffers.bufferSource(), camX, camY, camZ, tickDelta,
                hasEntityFrustum, lightsOnly);

        return vanilla + ours;
    }

    /** For diagnostics: whether the 1.21.1 shadow BE function is installed. */
    public static boolean isInstalled() {
        return installed;
    }
}
