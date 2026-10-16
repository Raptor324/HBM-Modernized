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
 * shader id; on a mismatch, bind the shader's program directly (managed
 * {@code GlStateManager._glUseProgram}). Normally this is a single
 * glGetInteger per pass. {@code shader.clear()} is NOT usable here: it only
 * resets the vanilla {@code ShaderInstance} statics and binds program 0, while
 * {@code ExtendedShader.apply()} gates its bind on Iris's own static
 * {@code ExtendedShader.lastApplied}, which nothing but an actual bind can
 * satisfy.</p>
 *
 * <p>Call sites: {@code com.hbm_m.client.render.SingleMeshVboRenderer}
 * (rocket meshes, block_lit), the DH depth copy ({@code DhDepthCopy}), the
 * AFTER_WEATHER pass entries in {@code EngineHandler} (Torex clouds/flash),
 * the far-content depth copy in {@code EngineHandler} (via
 * {@code IrisShaderApply}), {@code IrisShaderApply.tryApply},
 * {@code MdiBatchCoordinator} (MDI flush - compute passes clobber the program
 * between apply and the draws) and all three flush stages of
 * {@code VanillaInstancedBatchRenderer}.</p>
 */
public final class ShaderBindResync {

    private ShaderBindResync() {}

    /**
     * Set by our code paths that bind programs via raw {@code glUseProgram}
     * (compute bake/cull/Hi-Z passes, scatter uploads). The next
     * {@link #ensureFreshBind} consumes the flag and FORCES the direct managed
     * re-bind even when the numeric program-id compare would pass.
     *
     * <p>Why the numeric compare alone is not enough: it compares
     * {@code GL_CURRENT_PROGRAM} with {@code shader.getId()} as plain integers,
     * but nvidia freely recycles deleted program names. A stale shader object
     * whose program was destroyed by a pipeline rebuild can numerically match
     * whatever live program we left bound; the re-bind is then skipped and Iris's
     * {@code ExtendedShader.apply()} pushes every uniform into the foreign
     * program (observed 2026-10-05 under BSL with GPU bake on: ~23k
     * GL_INVALID_OPERATION per session, machines textured with a compute
     * program, bake multi-draw dying with GL_INVALID_OPERATION). Recycled ids
     * are caught by the fingerprint probe in
     * {@code IrisExtendedShaderAccess#programLive}; this flag covers the
     * still-live foreign-bind window between our raw binds and the next
     * apply.</p>
     */
    private static volatile boolean foreignProgramSinceLastSync;

    /**
     * Marks that one of our raw {@code glUseProgram} calls broke the shader-apply
     * bind cache; the next {@link #ensureFreshBind} forces an honest rebind.
     * Cheap (one volatile store) - call it in the epilogue of any entry point
     * that ends with a foreign program bound.
     */
    public static void markForeignProgramBind() {
        foreignProgramSinceLastSync = true;
    }

    /**
     * Ensure the shader's next {@code apply()} uploads its uniforms into the
     * shader's OWN program.
     *
     * <p>Mechanism: when the real {@code GL_CURRENT_PROGRAM} disagrees with
     * {@code shader.getId()} (or a foreign bind was marked), bind the shader's
     * program DIRECTLY via {@code GlStateManager._glUseProgram}. A subsequent
     * {@code apply()} then skips or repeats its internal bind - both branches
     * leave the correct program bound, so every glUniform lands where it
     * belongs.</p>
     *
     * <p>Why NOT {@code shader.clear()} (the previous approach): vanilla
     * {@code ShaderInstance.clear()} only resets the VANILLA statics
     * ({@code lastProgramId}, {@code lastAppliedShader}) and binds program 0 -
     * verified in bytecode (1.20.1). Iris's {@code ExtendedShader.apply()}
     * overrides apply entirely and gates its {@code glUseProgram} on the static
     * {@code ExtendedShader.lastApplied} (object identity), which NEITHER
     * clear() NOR any Oculus mixin resets (MixinShaderInstance touches only
     * attrib locations, depth-color locking and log spam). So after any foreign
     * bind, {@code clear()} followed by {@code apply()} left the WRONG program
     * bound (0 or ours) and every uniform push failed with GL_INVALID_OPERATION
     * - zero visual artifacts, pure log storm, every flush while instances were
     * dirty (i.e. whenever the camera moved).</p>
     */
    public static void ensureFreshBind(ShaderInstance shader) {
        if (shader == null) {
            return;
        }
        boolean force = foreignProgramSinceLastSync;
        foreignProgramSinceLastSync = false;
        try {
            if (force || GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != shader.getId()) {
                com.mojang.blaze3d.platform.GlStateManager._glUseProgram(shader.getId());
                if (GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != shader.getId()) {
                    // Safety net only: on the audited stack (Forge 1.20.1 47.4.10/47.4.20)
                    // _glUseProgram is an UNCONDITIONAL pass-through - no static program
                    // cache exists (verified in bytecode and in the 47.4.20 patch set,
                    // 2026-10-06). The caches that actually diverge from the real bind
                    // state are the VANILLA static ShaderInstance.lastProgramId and Iris's
                    // static ExtendedShader.lastApplied - and a forced real bind here
                    // satisfies both, because each gate only skips when the REAL program
                    // already equals the target. This raw fallback can therefore only
                    // fire on a future build that reintroduces a guard; keep it cheap.
                    GL20.glUseProgram(shader.getId());
                }
            } else {
                sampleSkipBind(shader);
            }
        } catch (Exception ignored) {
            // GL context unavailable/foreign patch - worst case equals old behavior
        }
    }

    private static volatile long lastSkipSampleMs;
    /** Shader ids whose numeric-match sample already ran at INFO this session. */
    private static final java.util.HashSet<Integer> SKIP_SAMPLES_LOGGED = new java.util.HashSet<>();

    /**
     * One sample per 5 s of the dangerous state this class exists for: the apply()
     * is about to SKIP its bind because the bound program id numerically equals
     * {@code shader.getId()}. With nvidia recycling deleted program names this is
     * also exactly the signature of a STALE shader object - its program destroyed
     * by a pipeline rebuild, the name now owned by a different live program; every
     * uniform push then fails with GL_INVALID_OPERATION while the rendering keeps
     * working (the real program's uniform state is untouched). The sample names
     * the shader so a stale cache is identifiable from the log alone.
     *
     * <p>The numeric-match path is also the HEALTHY steady state (bound program
     * genuinely is this shader's), so only the FIRST sample per shader id logs at
     * INFO - every 5 s repetition of a known id goes to DEBUG to keep a clean log
     * clean (2026-10-07: users read the repeated INFO line as an error report).</p>
     */
    private static void sampleSkipBind(ShaderInstance shader) {
        long now = System.currentTimeMillis();
        if (now - lastSkipSampleMs < 5000) {
            return;
        }
        lastSkipSampleMs = now;
        String name;
        int id;
        String cls;
        boolean live;
        long gen;
        try {
            name = shader.getName();
            id = shader.getId();
            cls = shader.getClass().getSimpleName();
            live = GL20.glIsProgram(id);
            gen = IrisExtendedShaderAccess.getPipelineGeneration();
        } catch (Exception t) {
            name = "<no name>";
            id = -1;
            cls = "<no class>";
            live = false;
            gen = -1L;
        }
        boolean first = id >= 0 && SKIP_SAMPLES_LOGGED.add(id);
        if (SKIP_SAMPLES_LOGGED.size() > 32) {
            SKIP_SAMPLES_LOGGED.clear();
        }
        String line = "[HBM-M] ShaderBindResync: skip-bind apply (numeric match): shader='{}' id={} ({}), "
                + "glIsProgram={}, pipelineGen={}. Healthy when the bound program really is this "
                + "shader's; if GL_INVALID_OPERATION uniform errors repeat in the same seconds, "
                + "this cached object is stale (program destroyed, name recycled to another program).";
        if (first) {
            MainRegistry_LOGGER.info(line, name, id, cls, live, gen);
        } else {
            MainRegistry_LOGGER.debug(line, name, id, cls, live, gen);
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
        } catch (Exception ignored) {
        }
    }

    // -- Iris DepthColorStorage watchdog -------------------------------------
    private static volatile boolean irisChecked;
    private static java.lang.reflect.Field irisDepthColorField;

    /**
     * Reads the static flag of Iris' {@code DepthColorStorage} (null if the
     * flag is unavailable). The onTail mixin sets it on EVERY apply of
     * an unknown mod ShaderInstance; if the flag is still set at the end of
     * the frame, the Oculus vanilla pipeline routes the PRESENT through its composite
     * (the color buffer itself is fully correct!) - that is the "black screen".
     * <p>
     * Package note: on 1.21.1 the class lives at
     * {@code net.irisshaders.iris.gl.blending.DepthColorStorage} and loads
     * reflectively. On 1.20.1 the class lives inside the mixin package
     * {@code net.irisshaders.iris.mixin.*}, which the mixin transformer REFUSES
     * to load from outside ({@code IllegalClassLoadError}, mixins.oculus.json) -
     * so on 1.20.1 the read side is dropped entirely and the caller treats the
     * flag as unknown ({@code forceIrisDepthColorEnabled*} falls back to direct
     * RenderSystem mask resets).
     */
    @org.jetbrains.annotations.Nullable
    public static Boolean irisDepthColorDisabled() {
        try {
            if (!irisChecked) {
                irisChecked = true;
                Class<?> cl = Class.forName("net.irisshaders.iris.gl.blending.DepthColorStorage");
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
        } catch (VirtualMachineError t) {
            // OOM/StackOverflow are never compat failures - let them propagate
            throw t;
        } catch (Throwable t) {
            // ANY other failure is an expected compat degradation (class absent
            // on 1.20.1 / Iris versions without the gl.blending class). One-time
            // log, then degrade to "no info" - the reset side still unconditionally
            // restores the GL masks via RenderSystem.
            logOnceProbeIssue("irisDepthColorDisabled", t, false);
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
        if (forceIrisDepthColorEnabledCore()) {
            MainRegistry_LOGGER.info("HBM iris DepthColorStorage reset (was masking)");
        }
    }

    /** Per-draw variant used by the NtDirectPass window (VertexBufferMixin) - no logging. */
    public static void forceIrisDepthColorEnabledQuiet() {
        forceIrisDepthColorEnabledCore();
    }

    private static boolean forceIrisDepthColorEnabledCore() {
        Boolean disabled = irisDepthColorDisabled();
        if (disabled != null && !disabled) {
            return false; // flag says not masking - nothing to reset
        }
        try {
            if (disabled != null) {
                irisDepthColorField.setBoolean(null, false);
            }
            // Direct RenderSystem reset - correct both when the flag was readable
            // (1.21.1) and when it was not (1.20.1 mixin-package ban): the masks
            // were disabled by Iris' MixinShaderInstance.onTail regardless of
            // whether we can read the flag back.
            com.mojang.blaze3d.systems.RenderSystem.colorMask(true, true, true, true);
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.platform.GlStateManager._enableDepthTest();
            return true;
        } catch (Exception ignored) {
            return false;
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
        } catch (Exception t) {
            // vanilla class - never absent; a failure here means the sky-black-screen
            // fix is silently dead (mapping/access mismatch) - surface it once
            logOnceProbeIssue("invalidateStaticProgramCache", t, true);
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
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
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
        } catch (Exception t) {
            // vanilla class - never absent; a failure here silently re-enables the
            // blend-cache black-screen bug - surface it once
            logOnceProbeIssue("invalidateBlendModeCache", t, true);
        }
    }

    private static java.lang.reflect.Field blendLastAppliedField;

    /**
     * One-time log for silent probe failures (catch-narrowing audit): DEBUG for
     * probes whose failure is the expected "mod absent" case, WARN for compat
     * fixes that would otherwise die silently.
     */
    private static final java.util.HashSet<String> PROBE_ISSUE_LOGGED = new java.util.HashSet<>();

    private static void logOnceProbeIssue(String site, Throwable t, boolean warn) {
        synchronized (PROBE_ISSUE_LOGGED) {
            if (PROBE_ISSUE_LOGGED.add(site)) {
                String detail = t == null ? "expected (mod absent/relocated or mapping mismatch)"
                        : t.toString();
                if (warn) {
                    MainRegistry_LOGGER.warn("[HBM-M] ShaderBindResync: '{}' failed once - compat fix inactive ({})",
                            site, detail);
                } else {
                    MainRegistry_LOGGER.debug("[HBM-M] ShaderBindResync: '{}' failed once ({})", site, detail);
                }
            }
        }
    }

    private static final org.slf4j.Logger MainRegistry_LOGGER =
            com.hbm_m.main.MainRegistry.LOGGER;
}
