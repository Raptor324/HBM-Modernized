package com.hbm_m.client.render.shader;

import net.minecraft.client.renderer.ShaderInstance;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/**
 * Activation window of the "NT direct" draw mode: while active, the NT particle
 * flushes (mushroom clouds, flash, gas flame, tower steam, ashes) render through
 * OUR core ShaderInstances instead of the shader pack's programs.
 *
 * <p>Why: under an active pack, the pack's per-program directives and material
 * pipeline take over the NT render types routed through pack ExtendedShaders.
 * Photon specifically declares {@code blend.gbuffers_textured = off} and renders
 * its translucents with its own deferred pipeline: vertex-colored emissive quads
 * come out color-multiplied-by-alpha, scene-lit, fog-drowned at chunk borders
 * (MK5 mushroom) or drawn without blending at all (square flash).</p>
 *
 * <p>Inside the window:
 * <ol>
 *   <li>{@link #begin()} pre-binds the pack pipeline FBO by applying the pack's
 *       TEXTURED_COLOR ExtendedShader once - everything drawn later in the window
 *       lands in the pack's colortex0 and survives its final composite;</li>
 *   <li>{@code VertexBufferMixin} undoes Iris's {@code disableDepthColor} masking
 *       right after every {@code ShaderInstance.apply()} (the mask would otherwise
 *       silence our unknown-to-Iris shaders) - see the mixin;</li>
 *   <li>the NT render types bind our own core shaders ({@code nuke_cloud},
 *       {@code nuke_add}, {@code nt_color_tex_lightmap}) whose JSON blend and
 *       vanilla fragment behavior are applied at apply() - the last word on
 *       blending and color is ours, the pack's fog/lighting never runs.</li>
 * </ol>
 * Scene fog still applies (the shaders read FogStart/FogEnd/FogColor from
 * RenderSystem state, same as the no-pack path).</p>
 */

@OnlyIn(Dist.CLIENT)
public final class NtDirectPass {

    private static int depth = 0;
    private static boolean active;
    /** The pack ExtendedShader applied by {@link #begin()} (null = no pack). */
    private static net.minecraft.client.renderer.ShaderInstance activePackShader;
    /**
     * The pack pipeline FBO captured right after {@code packShader.apply()} in
     * {@link #begin()} (raw {@code GL_DRAW_FRAMEBUFFER_BINDING} read - the
     * managed {@code GlStateManager} cache is unreliable under Oculus). Read by
     * {@code VertexBufferMixin} to re-bind it before every in-window draw:
     * a RenderType teardown ({@code clearRenderState}) inside the window can
     * re-bind the vanilla MAIN target (shards carrying TRANSLUCENT_TARGET on
     * Fabulous-style targets), which would silently redirect every later batch
     * of the same flush out of the pack composite.
     */
    private static int packFbo = -1;

    private NtDirectPass() {}

    /**
     * Opens the window. Under Iris, first re-binds the pack pipeline FBO via the
     * TEXTURED_COLOR ExtendedShader (the same binding the previous per-draw
     * routing relied on) - without it our draws would go into the vanilla main
     * target, which Iris overwrites in its final blit (content would vanish).
     */
    public static void begin() {
        if (depth++ > 0) {
            return;
        }
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // Oculus/Iris switch or zero the GL program through their own paths
            // without updating ShaderInstance.lastProgramId; the cached bind then
            // skips inside apply() and the draw fires with program 0 ("No active
            // program"). Two guards: invalidateStaticProgramCache covers the
            // window's own VANILLA core shaders (their apply gates on the
            // lastProgramId static), ensureFreshBind below covers the PACK
            // ExtendedShader - its apply() gates on Iris's own static
            // ExtendedShader.lastApplied instead, which the vanilla reset cannot
            // touch (2026-10-06: this gap was the 85k-error "No active program"
            // storm - apply() pushed every uniform into program 0 once the
            // window's vanilla draws had left the program there).
            ShaderBindResync.invalidateStaticProgramCache();
            ShaderInstance packShader = FarContentShaders.resolveTexColor();
            activePackShader = packShader;
            packFbo = -1;
            if (packShader != null) {
                try {
                    ShaderBindResync.ensureFreshBind(packShader);
                    packShader.apply();
                    // Capture the pack FBO while it is guaranteed bound: apply()
                    // has just bound the pack pipeline's framebuffer. A raw read -
                    // it does not desync the managed cache (read-only).
                    packFbo = org.lwjgl.opengl.GL11.glGetInteger(
                            org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING);
                    // The pack apply() itself may leave the color mask shut
                    // (Iris's DepthColorStorage applies to ANY apply, including
                    // the pack's own program on some pack versions) - make sure
                    // the very first in-window draw is not masked.
                    ShaderBindResync.forceIrisDepthColorEnabled();
                } catch (Throwable ignored) {
                }
            }
        }
        active = true;
    }

    public static void end() {
        if (--depth > 0) {
            return;
        }
        depth = 0;
        // Close the window: unbind the pack shader the same way an Iris-managed
        // pass would end - clear() resets the vanilla statics and lets the
        // ExtendedShader unbind its framebuffer, so no state of the window leaks
        // into the rest of the frame (the pack pipeline re-applies its own
        // programs for the composite regardless). Everything is guarded: any
        // failure here must degrade to the previous behavior (window closed,
        // state left as it was).
        if (activePackShader != null) {
            try {
                activePackShader.clear();
            } catch (Throwable ignored) {
            }
        }
        activePackShader = null;
        packFbo = -1;
        active = false;
    }

    /**
     * Executes a render action under the NtDirectPass window and flushes all pending batches.
     * Guaranteed to wrap both intermediate type-switch flushes and the final batch flush.
     */
    public static void run(Runnable renderAction) {
        begin();
        try {
            if (renderAction != null) {
                renderAction.run();
            }
            com.hbm_m.particle.nt.ParticleEngineNT.buffer().endBatch();
        } finally {
            end();
        }
    }

    /** Read by {@code VertexBufferMixin} after every ShaderInstance.apply() - must be cheap. */
    public static boolean isActive() {
        return active;
    }

    /**
     * The pack pipeline FBO captured in {@link #begin()} (-1 = unknown). The
     * mixin re-binds it before every in-window draw via the MANAGED
     * {@code GlStateManager._glBindFramebuffer}, so the managed cache stays in
     * sync with the physical bind.
     */
    public static int getPackFbo() {
        return packFbo;
    }
}
