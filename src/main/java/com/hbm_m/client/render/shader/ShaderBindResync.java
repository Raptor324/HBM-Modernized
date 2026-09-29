package com.hbm_m.client.render.shader;

import net.minecraft.client.renderer.ShaderInstance;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/**
 * Forced resynchronization of the static program cache of a custom
 * {@link ShaderInstance} against the real GL state.
 *
 * <p>With the shader pack disabled, Oculus once per frame performs a raw
 * {@code GlStateManager._glUseProgram(0)} (VanillaRenderingPipeline
 * .beginLevelRendering) without updating the private static
 * {@code ShaderInstance.lastProgramId}. If the previous frame ended with our
 * shader, the next {@code apply()} sees programId == lastProgramId and SKIPS
 * the real bind - all glUniform/glDrawElements go to program 0 ("No active
 * program", black geometry with a garbage matrix), and the state sticks for
 * all subsequent frames.</p>
 *
 * <p>Fix without mixins: before drawing, compare GL_CURRENT_PROGRAM with the
 * shader id; on a mismatch, {@code clear()} resets lastProgramId to -1 and
 * forces an honest glUseProgram in apply(). Normally this is a single
 * glGetInteger per pass.</p>
 *
 * <p>Call sites: {@code com.hbm_m.client.render.SingleMeshVboRenderer}
 * (rocket meshes, block_lit), the DH depth copy ({@code DhDepthCopy}), and
 * the AFTER_WEATHER pass entries in {@code EngineHandler} (Torex
 * clouds/flash).</p>
 */
public final class ShaderBindResync {

    private ShaderBindResync() {}

    /** Ensure the shader's next {@code apply()} performs an honest bind. */
    public static void ensureFreshBind(ShaderInstance shader) {
        if (shader == null) {
            return;
        }
        try {
            if (GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != shader.getId()) {
                shader.clear();
            }
        } catch (Throwable ignored) {
            // GL context unavailable/foreign patch - worst case equals old behavior
        }
    }

    /**
     * Whole-world desync watchdog: if RenderSystem considers some
     * {@link ShaderInstance} current while the actual GL program is 0 (raw
     * reset by Oculus with no follow-up bind), the next vanilla
     * {@code apply()} will skip the honest bind and THE WORLD STOPS
     * REDRAWING (the screen freezes until any foreign honest apply shows up).
     * fsp diagnostics showed this directly: prog=0 with a live RS shader
     * persists across all phase boundaries until another SingleMeshVboRenderer
     * appears.
     *
     * <p>Called on EVERY of our RenderLevelStageEvents (see ClientModEvents /
     * EngineHandler) - well before the next vanilla apply, so afterwards any
     * {@code apply()} performs a real glUseProgram and the frame keeps
     * redrawing even with none of our meshes on screen.</p>
     */
    public static void enforceGlProgramConsistency() {
        try {
            ShaderInstance current = com.mojang.blaze3d.systems.RenderSystem.getShader();
            if (current != null && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == 0) {
                current.clear();
            }
        } catch (Throwable ignored) {
        }
    }

    // -- Iris DepthColorStorage watchdog -------------------------------------
    private static volatile boolean irisChecked;
    private static java.lang.reflect.Field irisDepthColorField;

    /**
     * Reads the static flag of {@code net.irisshaders.iris.mixin.DepthColorStorage}
     * (null if Iris/Oculus is absent). The onTail mixin sets it on EVERY apply of
     * an unknown mod ShaderInstance; if the flag is still set at the end of the
     * frame, the Oculus vanilla pipeline routes the PRESENT through its composite
     * (the color buffer itself is fully correct!) - that is the "black screen".
     */
    @org.jetbrains.annotations.Nullable
    public static Boolean irisDepthColorDisabled() {
        try {
            if (!irisChecked) {
                irisChecked = true;
                Class<?> cl = Class.forName("net.irisshaders.iris.mixin.DepthColorStorage");
                for (java.lang.reflect.Field f : cl.getDeclaredFields()) {
                    if (f.getType() == boolean.class && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        irisDepthColorField = f;
                        break;
                    }
                }
            }
            return irisDepthColorField == null ? null
                    : (Boolean) irisDepthColorField.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Forcibly resets Iris masking (disableDepthColor): sets the flag back to
     * false and restores colorMask + depth test via the managed API. Called at
     * the end of our late passes so the end of the frame goes through the
     * vanilla present again.
     */
    public static void forceIrisDepthColorEnabled() {
        try {
            Boolean disabled = irisDepthColorDisabled();
            if (disabled == null || !disabled) {
                return;
            }
            irisDepthColorField.setBoolean(null, false);
            com.mojang.blaze3d.platform.GlStateManager._colorMask(true, true, true, true);
            com.mojang.blaze3d.platform.GlStateManager._enableDepthTest();
            MainRegistry_LOGGER.info("HBM iris DepthColorStorage reset (was masking)");
        } catch (Throwable ignored) {
        }
    }

    /**
     * Full invalidation of the static {@code ShaderInstance.lastProgramId} cache
     * (set to -1 so the NEXT apply() of any shader performs an honest
     * glUseProgram).
     *
     * <p>Why: the Oculus clobber ({@code _glUseProgram(0)}) arrives at the very
     * start of the frame, BEFORE the sky is drawn - and the first thing the
     * world draws is the skyBuffer via position_color. If the previous frame
     * ended with the same shader (the GUI is full of position_color), the
     * sky's apply() SKIPS the bind, the sky quad is not rasterized (core
     * profile, program 0), and the whole frame shows the clear color - in a
     * world below y&lt;0 that is almost black (2,2,0). Step-by-step px
     * measurements confirmed the frame is already black at fsp[px.sky]. There
     * is no Forge stage earlier than the sky, so the only fix point is the END
     * of the previous frame.</p>
     *
     * <p>The field is located by TYPE (the only static int in ShaderInstance -
     * lastProgramId), so reflection works under SRG names in production.</p>
     */
    public static void invalidateStaticProgramCache() {
        try {
            if (lastProgramIdField == null) {
                for (java.lang.reflect.Field f : ShaderInstance.class.getDeclaredFields()) {
                    if (f.getType() == int.class && java.lang.reflect.Modifier.isStatic(f.getModifiers())
                            && !java.lang.reflect.Modifier.isFinal(f.getModifiers())
                            && !f.isSynthetic()) {
                        f.setAccessible(true);
                        lastProgramIdField = f;
                        break;
                    }
                }
            }
            if (lastProgramIdField != null) {
                lastProgramIdField.setInt(null, -1);
            }
        } catch (Throwable ignored) {
        }
    }

    private static java.lang.reflect.Field lastProgramIdField;

    /**
     * Restores the standard texture trio via VANILLA managed calls: block atlas
     * (TU0), overlay (TU1), lightmap (TU2).
     *
     * <p>Why: px.pad.* diagnostics showed that in frames under Oculus, by the
     * start of the BE phase the physical binds of all texture units are 0
     * (units=[0/0/0]) while the GlStateManager cache considers them live - the
     * managed binds of vanilla and mods no-op, and everything drawn before the
     * first "raw" block_lit draw (the launcher with a rocket) gets black
     * textures. This call puts both the physical state and the cache into a
     * consistent state - call as early in the frame as possible (the first
     * RenderLevelStageEvent, before entities).</p>
     */
    public static void restoreVanillaTextureBindings() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            mc.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                    .bind(); // managed: TU0 + cache
            mc.gameRenderer.overlayTexture().setupOverlayColor(); // TU1 managed
            mc.gameRenderer.lightTexture().turnOnLightLayer();    // TU2 managed
            // Managed: a raw glActiveTexture would desync the unit cache
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Forces an honest resync of the blend factors.
     *
     * <p>GlStateManager._blendFuncSeparate NO-OPS when the factors match the
     * cache. If the cache diverged from the physical state (anyone in the
     * frame - our RenderTypes, foreign mods, the previous frame's state),
     * vanilla draws with NON-STANDARD blending break silently:</p>
     * <ul>
     *   <li>the vignette (multiply ZERO/ONE_MINUS_SRC_COLOR) draws with normal
     *       blending - its quad lands on screen as an opaque dark layer
     *       ("black screen");</li>
     *   <li>sun/moon (additive SRC_ALPHA/ONE) - "black squares" around them.</li>
     * </ul>
     * <p>The (0,0,0,0) sentinel guarantees a real GL call, then the vanilla
     * default is restored. No draws happen between the two calls - safe.</p>
     */
    public static void forceHonestBlendState() {
        try {
            com.mojang.blaze3d.platform.GlStateManager._blendFuncSeparate(0, 0, 0, 0);
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Resets the static cache of {@code com.mojang.blaze3d.shaders.BlendMode.lastApplied}.
     *
     * <p>How it breaks: {@code ShaderInstance.apply()} (1.20.1, line ~337) applies
     * the blend from the shader JSON. Our custom shaders (nuke_cloud/nuke_add)
     * carry NON-opaque modes (alpha/additive) - after their apply() the cache
     * stays non-opaque. The next VANILLA shader without blend in its json
     * (position_tex etc., opaque) sees the opacity change and calls
     * RenderSystem.disableBlend() - silently killing blending for vanilla draws
     * that enabled it manually: the vignette (multiply) renders as an opaque
     * dark texture fullscreen ("black screen" on Fancy), sun/moon (additive) as
     * black squares, translucent GUI layers as black slabs. After the reset to
     * null, the next apply() performs the FULL honest setup (enable/disable +
     * factors). The field is located by TYPE (the only static BlendMode in the
     * class) - works under SRG names in production too.</p>
     */
    public static void invalidateBlendModeCache() {
        try {
            if (blendLastAppliedField == null) {
                for (java.lang.reflect.Field f : com.mojang.blaze3d.shaders.BlendMode.class
                        .getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                            && !f.isSynthetic()
                            && f.getType() == com.mojang.blaze3d.shaders.BlendMode.class) {
                        f.setAccessible(true);
                        blendLastAppliedField = f;
                        break;
                    }
                }
            }
            if (blendLastAppliedField != null) {
                blendLastAppliedField.set(null, null);
            }
        } catch (Throwable ignored) {
        }
    }

    private static java.lang.reflect.Field blendLastAppliedField;

    private static final org.slf4j.Logger MainRegistry_LOGGER =
            com.hbm_m.main.MainRegistry.LOGGER;
}
