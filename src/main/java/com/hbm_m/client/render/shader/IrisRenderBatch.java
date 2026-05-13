package com.hbm_m.client.render.shader;



import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.hbm_m.client.render.IrisCompanionMesh;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;


/**
 * Per-block-entity batching session for the Iris {@code ExtendedShader} render path.
 * <p>
 * Without this class, every part of every machine triggers its own
 * {@code shader.apply()} (framebuffer bind + Iris CustomUniforms push + every uniform
 * upload) and {@code shader.clear()} (framebuffer rebind). For an Assembler with 9
 * animated parts and N visible machines, that is {@code 9 * N * 2} (main + shadow
 * pass) heavy GL state changes per frame - easily 50-60% of frame time at moderate
 * machine counts.
 * <p>
 * With this class, a renderer opens one batch session per BlockEntity (or per any
 * group of related draws), then issues lightweight {@link #drawCompanion} calls for
 * each part. The session pays the heavy {@code apply}/{@code clear} cost ONCE; per-draw
 * work is reduced to a single VAO bind, a {@code ModelViewMat} upload, and the actual
 * {@code glDrawElements}. This is the same optimization
 * {@code InstancedStaticPartRenderer.flushBatchIris} already applies when batching is
 * enabled - generalized so the per-part path benefits from it too.
 * <p>
 * Usage pattern (try-with-resources guarantees {@link #close} is called):
 * <pre>{@code
 * try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, projectionMatrix)) {
 *     if (batch != null) {
 *         renderer1.renderInBatch(...);
 *         renderer2.renderInBatch(...);
 *     } else {
 *         // shader pack didn't expose a usable shader - fall back to per-call path
 *     }
 * }
 * }</pre>
 * <p>
 * The session is a singleton; nested {@link #begin} calls return the outer session
 * unchanged so callers can compose sessions safely. Only the OUTER {@link #close} call
 * actually emits {@code shader.clear()}.
 */
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
@Environment(EnvType.CLIENT)*///?}
public final class IrisRenderBatch implements AutoCloseable {

    private static final IrisRenderBatch INSTANCE = new IrisRenderBatch();
    private static IrisRenderBatch ACTIVE;

    /** Outer-call wrapper that does nothing on close - only the outer {@link #begin} call gets the real instance. */
    private static final IrisRenderBatch NOOP_NESTED = new IrisRenderBatch();

    private boolean isOuter;
    /**
     * True for any "persistent" batch (shadow or main) that intentionally outlives
     * its caller's try-with-resources scope. Each pass — shadow and main — opens
     * exactly ONE persistent batch per frame so every dispatched BlockEntity in
     * that pass shares a single {@code shader.apply()} (framebuffer bind + Iris
     * CustomUniforms push + every sampler bind). Closed lazily by:
     * <ul>
     *   <li>{@link #begin} when the next call's pass differs from the active
     *       batch's {@link #isShadowPass} — i.e. the shadow→main transition
     *       inside one frame closes shadow and opens main, the main→shadow
     *       transition between frames does the inverse, and</li>
     *   <li>{@link #closePersistentIfActive()} fired from
     *       {@code RenderLevelStageEvent.AFTER_LEVEL} — the safety net for the
     *       last batch of the frame (whose pass-change close never gets
     *       triggered because no follow-up {@link #begin} happens this frame).</li>
     * </ul>
     * <p>
     * Without this, {@code IrisRenderBatch.begin()} pays {@code apply()/clear()}
     * on EVERY BE: ~6.5% of frame time per BE under BSL × N visible machines × 2
     * passes. On a 400-machine farm under BSL that's the dominant cost. With this,
     * the cost is paid exactly twice per frame regardless of how many BEs render.
     * <p>
     * Note: an earlier prototype additionally buffered the per-draw GL inside the
     * persistent shadow batch and flushed sorted-by-VAO at teardown. That broke
     * because the lazy teardown paths run AFTER Iris has already swapped its
     * framebuffer / unbound our shadow shader program, so the deferred
     * {@code glUniformMatrix4fv} hit {@code GL_INVALID_OPERATION: No active
     * program} and draws landed on the wrong framebuffer. Each draw is now
     * executed eagerly inside {@link #drawCompanion}; per-instance caches
     * (VAO, lightmap) still elide redundant binds for adjacent draws.
     */
    private boolean isPersistent;

    /** Pass identity of the active persistent batch — used to detect pass changes. */
    private boolean isShadowPass;

    private ShaderInstance shader;
    private int previousBlockEntityId;
    private int previousVao;
    private int previousArrayBuffer;
    private boolean previousCullEnabled;
    private IrisPhaseGuard phaseGuard;

    /**
     * Cached per-shader uniform handles. Re-resolved when a different shader is bound
     * (e.g. main vs shadow pass, or after a pipeline rebuild).
     * <p>
     * We key the cache by <b>both</b> the {@code ShaderInstance} identity and
     * the {@link IrisExtendedShaderAccess#getPipelineGeneration() pipeline
     * generation} rather than just the program ID, because GL drivers recycle
     * deleted program IDs: after a pipeline rebuild a fresh program can land
     * on the same integer as the dead one we had cached, and a plain
     * {@code programId == cached} check wrongly short-circuits, handing
     * stale uniform locations to {@code glUniformMatrix4fv}
     * ({@code GL_INVALID_OPERATION: Uniform must be a matrix type}).
     */
    private int cachedShaderProgram = -1;
    private ShaderInstance cachedShaderInstance;
    private long cachedPipelineGeneration = -1L;
    private Uniform uModelView;
    /** Direct uniform location for {@code ModelViewMat} - bypasses Mojang's
     *  Uniform proxy (saves several layers of indirection per draw). */
    private int locModelView = -1;
    private int locModelViewInverse = -1;
    private int locNormalMat = -1;

    /** Reusable scratch buffers - avoid alloc per draw call. */
    private final Matrix4f mvInverseTmp = new Matrix4f();
    private final Matrix3f normalTmp = new Matrix3f();
    private final float[] mvFloats = new float[16];
    private final float[] mvInverseFloats = new float[16];
    private final float[] normalMatFloats = new float[9];
    /** Scratch for quantizing the 8-corner UV2 into a stable slot key. */
    private final short[] cornerShort16 = new short[16];
    /** Scratch float view of the quantized corner UV2 (0..240). */
    private final float[] cornerFloat16 = new float[16];
    /** Quantized 16-probe (32 floats) samples for 2×4×2 sliced + Iris per-vertex path. */
    private final float[] quantProbe32 = new float[32];

    /**
     * Per-instance state caches - let us elide redundant GL calls when consecutive
     * draws share VAO / lightmap. Reset in {@link #setupOuter} on every batch start.
     */
    private int lastBoundVao = -1;
    private int lastBlockU = Integer.MIN_VALUE;
    private int lastSkyV = Integer.MIN_VALUE;

    /** Rolling cursor for lightmap slots in per-vertex mode (per batch). */

    private IrisRenderBatch() {}

    /**
     * @return the currently-open batch, or {@code null} if no batch is active.
     */
    public static IrisRenderBatch active() {
        return ACTIVE;
    }

    /**
     * @return {@code true} if a batch session is currently open. When {@code true}
     *         per-part renderers should call {@link #drawCompanion} on {@link #active()}
     *         instead of running their full standalone {@code apply}/{@code clear} path.
     */
    public static boolean isActive() {
        return ACTIVE != null;
    }

    /**
     * Opens a batch session for the given pass. Returns {@code null} if no usable
     * Iris {@code ExtendedShader} could be resolved (e.g. shader pack disabled, Iris
     * not loaded, reflection failed) - in which case the caller MUST fall back to
     * per-call rendering instead of relying on {@link #drawCompanion}.
     * <p>
     * <b>Both passes batched.</b> Shadow AND main pass each open one persistent
     * batch per frame. The returned handle is always {@link #NOOP_NESTED} on
     * success — the caller's try-with-resources is decorative; the underlying
     * batch outlives it. Subsequent BEs in the same pass piggyback on the same
     * {@code shader.apply()} via {@link #active()}. The batch is torn down when:
     * <ul>
     *   <li>a later {@code begin(...)} call's pass differs from the active
     *       batch's {@link #isShadowPass} — typical case is the shadow→main
     *       transition inside one frame, but it also handles the rare
     *       (debug-paused / single-stepped) main→shadow inverse, or</li>
     *   <li>{@link #closePersistentIfActive()} fires at {@code AFTER_LEVEL} —
     *       the safety net for the LAST batch of every frame (which has no
     *       follow-up begin() this frame to trigger pass-change close).</li>
     * </ul>
     * This collapses N × {@code apply()/clear()} pairs into exactly TWO per frame
     * total (one shadow, one main) regardless of machine count. On a 400-machine
     * farm under BSL the savings dominate the per-pass CPU budget — Iris's
     * {@code apply()} is the single most expensive call on this path because it
     * binds the framebuffer, pushes every CustomUniform, and re-binds every
     * sampler.
     * <p>
     * Per-draw work is still issued eagerly inside {@link #drawCompanion} (see
     * the field-level note on {@link #isPersistent} for why deferred sort-by-VAO
     * flushes are unsafe).
     *
     * @param shadowPass        whether we are inside Iris's shadow pass - selects
     *                          {@code SHADOW_*} variants of the entity shader
     * @param projectionMatrix  projection matrix to upload once into the shader
     */
    public static IrisRenderBatch begin(boolean shadowPass, Matrix4f projectionMatrix) {
        //? if forge {
        // Pass-change detection: if the active batch's pass differs from the
        // requested one, tear it down before opening the new one. The previous
        // batch's framebuffer/shader bindings are already invalidated by Iris's
        // own pass switch, but our cached uniform handles still match the
        // previous shader and our ACTIVE pointer still holds it; closing here
        // flushes that state properly so the next setupOuter() sees a clean slate.
        if (ACTIVE != null && ACTIVE.isPersistent && ACTIVE.isShadowPass != shadowPass) {
            ACTIVE.actuallyClose();
        }

        // Same-pass nested call - every subsequent BE in the same pass piggy-backs
        // on the existing persistent batch via active().
        if (ACTIVE != null && ACTIVE.isPersistent) {
            return NOOP_NESTED;
        }

        // Defensive: a non-persistent batch should never be ACTIVE here (we never
        // open one anymore) but if some legacy path is left, we don't trample it.
        if (ACTIVE != null) {
            return NOOP_NESTED;
        }

        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) {
            return null;
        }
        try {
            INSTANCE.setupOuter(shader, projectionMatrix);
            INSTANCE.isPersistent = true;
            INSTANCE.isShadowPass = shadowPass;
            ACTIVE = INSTANCE;
            return NOOP_NESTED;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("IrisRenderBatch.begin ({}) failed ({}), falling back to per-call path",
                    shadowPass ? "shadow" : "main", t.toString());
            INSTANCE.tryRestoreState();
            INSTANCE.isPersistent = false;
            INSTANCE.isShadowPass = false;
            return null;
        }
        //?}

        //? if fabric {
        /*// On Fabric there is no AFTER_BLOCK_ENTITIES event. The persistent
        // batch model used on Forge keeps shader.apply() bound across all BEs
        // in one pass — but the eventual shader.clear() fires at
        // AFTER_TRANSLUCENT, by which point Iris has already moved past the
        // block entity phase, swapped framebuffers, and unbound the program.
        // The deferred clear() then corrupts Iris's pipeline state, causing
        // "No active program" errors and models drawn to screen-space.
        //
        // Fix: use non-persistent per-BE batches on Fabric. Each BER's
        // try-with-resources opens its own apply()/clear() pair that closes
        // while Iris still has the correct phase active. Nested calls within
        // one BER return NOOP_NESTED to avoid redundant apply()/clear().
        // Cost: one apply()/clear() per BE instead of one per frame. This is
        // acceptable because Fabric block entity dispatch is synchronous and
        // Iris's apply() is fast when the framebuffer is already bound.

        // Nested call — a parent BER already holds the active batch.
        if (ACTIVE != null) {
            return NOOP_NESTED;
        }

        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) {
            return null;
        }
        try {
            INSTANCE.setupOuter(shader, projectionMatrix);
            INSTANCE.isPersistent = false;
            INSTANCE.isShadowPass = shadowPass;
            ACTIVE = INSTANCE;
            return INSTANCE;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("IrisRenderBatch.begin ({}) failed ({}), falling back to per-call path",
                    shadowPass ? "shadow" : "main", t.toString());
            INSTANCE.tryRestoreState();
            INSTANCE.isPersistent = false;
            INSTANCE.isShadowPass = false;
            return null;
        }
        *///?}
    }

    /**
     * Closes any persistent batch (shadow or main) still active at end-of-frame.
     * Called from {@code RenderLevelStageEvent.AFTER_LEVEL} as the safety net for
     * the LAST batch of every frame — its pass-change close in {@link #begin}
     * never fires because no follow-up begin() happens this frame. Also covers
     * leak-into-next-frame edge cases (e.g. player turned away so no main BE
     * dispatch but shadow camera still captured them).
     */
    public static void closePersistentIfActive() {
        if (ACTIVE != null && ACTIVE.isPersistent) {
            ACTIVE.actuallyClose();
        }
    }

    /**
     * Whether this batch is rendering into Iris's shadow pass. Lets callers
     * skip the expensive 8-corner {@link com.hbm_m.client.render.LightSampleCache}
     * sampling and the {@code writeInstanceLightmap}/{@code uploadLightmapRange}
     * pair that produces no visible output in shadow (depth-only) but dominates
     * the frame's CPU profile and poisons the light cache with shadow-camera
     * state. Use together with
     * {@link com.hbm_m.client.render.compat.ShaderCompatibilityDetector#isRenderingShadowPass()}
     * when no batch is active.
     */
    public boolean isShadowPass() {
        return isShadowPass;
    }

    private void setupOuter(ShaderInstance shader, Matrix4f projectionMatrix) {
        this.isOuter = true;
        this.shader = shader;

        // One-shot state snapshot: the per-call path used to do this on EVERY draw.
        // Doing it once per batch trims tens of forced GL pipeline flushes per frame.
        this.previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        this.previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        this.previousCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        // BSL et al. branch on `blockEntityId / 100`; force a neutral 0 so the
        // pack shader does not take EMISSIVE_RECOLOR or DrawEndPortal branches
        // based on whichever BE Iris last rendered. Same rationale as
        // InstancedStaticPartRenderer.flushBatchIris.
        this.previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        this.phaseGuard = IrisPhaseGuard.pushBlockEntities();

        RenderSystem.setShader(() -> shader);

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(projectionMatrix);
        }
        if (shader.MODEL_VIEW_MATRIX != null) {
            // Placeholder identity ModelView; per-draw drawCompanion() overrides via
            // a direct upload of the per-instance matrix.
            shader.MODEL_VIEW_MATRIX.set(IDENTITY);
        }
        Uniform fogStart = shader.getUniform("FogStart");
        if (fogStart != null) fogStart.set(RenderSystem.getShaderFogStart());
        Uniform fogEnd = shader.getUniform("FogEnd");
        if (fogEnd != null) fogEnd.set(RenderSystem.getShaderFogEnd());
        Uniform fogColor = shader.getUniform("FogColor");
        if (fogColor != null) {
            float[] fc = RenderSystem.getShaderFogColor();
            fogColor.set(fc[0], fc[1], fc[2], fc[3]);
        }
        Uniform sampler0 = shader.getUniform("Sampler0");
        if (sampler0 != null) sampler0.set(0);

        // Same texture-slot rebinds as the per-call path. ExtendedShader.apply()
        // copies these tracked slots onto the IrisSamplers ALBEDO/OVERLAY/LIGHTMAP
        // texture units - if we leave a stale ID in slot 0 (e.g. from an Embeddium
        // chunk re-bake) the pack shader samples the wrong image and the model
        // appears solid orange.
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        var blockAtlas = Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);
        com.mojang.blaze3d.systems.RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        com.mojang.blaze3d.systems.RenderSystem.bindTexture(blockAtlas.getId());

        // ONE heavy apply(): GlFramebuffer.bind() + Iris CustomUniforms.push() +
        // uploadIfNotNull for every uniform. This is the single largest CPU cost
        // we can amortise across multiple draws.
        shader.apply();

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();

        int programId = shader.getId();
        long currentGen = IrisExtendedShaderAccess.getPipelineGeneration();
        // Re-resolve if ANY of: (1) pipeline rebuilt (generation bumped),
        // (2) a different ShaderInstance was handed to us (main vs shadow,
        // or fresh instance after rebuild), (3) program ID differs. The
        // generation check is the authoritative signal against GL ID
        // recycling; the other two are defense in depth for edge cases
        // where the generation bump hasn't fired yet (e.g. mid-frame).
        if (cachedShaderProgram != programId
                || cachedShaderInstance != shader
                || cachedPipelineGeneration != currentGen) {
            cachedShaderProgram = programId;
            cachedShaderInstance = shader;
            cachedPipelineGeneration = currentGen;
            uModelView = shader.getUniform("ModelViewMat");
            // Direct uniform location for ModelViewMat - bypassing Mojang's
            // Uniform.upload() proxy stack saves ~5% per profiler trace on the
            // dense per-part path. Falls back to glGetUniformLocation if the
            // uniform isn't tracked by the ShaderInstance abstraction.
            locModelView = (uModelView != null) ? uModelView.getLocation()
                    : GL20.glGetUniformLocation(programId, "iris_ModelViewMat");
            locModelViewInverse = GL20.glGetUniformLocation(programId, "iris_ModelViewMatInverse");
            locNormalMat = GL20.glGetUniformLocation(programId, "iris_NormalMat");
        }

        // Reset per-draw caches: a new batch may bind a different VAO first, and
        // the previous batch's lightmap UV2 is no longer current.
        lastBoundVao = -1;
        lastBlockU = Integer.MIN_VALUE;
        lastSkyV = Integer.MIN_VALUE;
    }

    /**
     * Reusable identity matrix for the placeholder MODEL_VIEW_MATRIX uniform; the
     * real per-instance matrix is uploaded inside {@link #drawCompanion}.
     */
    private static final Matrix4f IDENTITY = new Matrix4f();

    /**
     * Issues a single draw using the active batch shader. Updates only the per-instance
     * uniforms ({@code ModelViewMat}, {@code iris_ModelViewMatInverse}, {@code iris_NormalMat})
     * and the per-draw lightmap UV2 attribute constant.
     * <p>
     * Must be called from within a {@link #begin}/{@link #close} pair on the OUTER
     * batch returned by {@link #active()}. The work happens eagerly under the
     * caller's still-bound shader program — deferring to teardown is unsafe
     * because Iris swaps framebuffer + program between shadow and main passes
     * before our lazy {@code actuallyClose()} runs.
     */
    public void drawCompanion(IrisCompanionMesh companion, Matrix4f modelView, int packedLight) {
        if (!isOuter || shader == null) return;
        if (companion == null || !companion.isBuilt()) return;
        int targetVao = companion.getVaoId();
        int targetIndexCount = companion.getIndexCount();
        if (targetVao <= 0 || targetIndexCount <= 0) return;

        // Persistent shadow batches outlive multiple BE dispatches, and the Iris
        // BlockEntityRenderDispatcher mixin overwrites CapturedRenderingState
        // .currentBlockEntity with each BE's id before its render() is called.
        // We set 0 once in setupOuter() (correct for BE #1) but for BE #2+ that
        // 0 has been clobbered by the mixin to the foreign id. Pack shaders that
        // branch on `blockEntityId / 100` (BSL emissive recolor, end-portal,
        // etc.) then mis-classify our draws. Force 0 here on every draw for
        // persistent batches; the call is cheap with the MethodHandle path
        // (one virtual invokeExact, no boxing).
        if (isPersistent) {
            IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);
        }

        // Fill mvFloats once and reuse for all three uniform uploads below,
        // avoiding repeated Matrix4f.get() calls.
        modelView.get(mvFloats);

        // VAO bind cache: adjacent draws of the same companion mesh elide the
        // bind + prepareForShader resolve. Cheap one-int compare on miss.
        if (lastBoundVao != targetVao) {
            GL30.glBindVertexArray(targetVao);
            // prepareForShader has its own program-id ring buffer cache, so this
            // call is nearly free for repeat program IDs (which is every frame
            // after the first); the real work happens once per (programId, mesh)
            // pair across the entire game session.
            companion.prepareForShader(shader.getId());
            lastBoundVao = targetVao;
        }

        float[] mvSrc = mvFloats;

        // Direct GL upload of ModelViewMat - skips Mojang's Uniform.set/upload
        // proxy stack (ShaderInstance.Uniform.upload → uploadAsMatrix →
        // RenderSystem.glUniformMatrix4 → GlStateManager._glUniformMatrix4 →
        // GL20C.nglUniformMatrix4fv). Profiler attributed ~8.67% of frame time
        // to those layers in the analogous flushBatchIris loop; we get the same
        // win here.
        if (locModelView >= 0) {
            GL20.glUniformMatrix4fv(locModelView, false, mvSrc);
        }

        // ExtendedShader.apply() derived iris_ModelViewMatInverse and iris_NormalMat
        // from the IDENTITY matrix we passed in; per-instance we re-derive them
        // from the actual ModelView and upload directly. Without this, pack
        // shaders (BSL/Complementary/RV/Solas) get wrong vertex normals and
        // render visibly broken geometry.
        //
        // Optimisation: compute MV^-1 once into mvInverseTmp, then derive
        // iris_NormalMat from it (transpose of MV^-1 upper-left 3x3) instead of
        // recomputing the invert from scratch - saves one Matrix4f.invert() per
        // draw, the most expensive joml call in this loop.
        boolean haveInverse = false;
        if (locModelViewInverse >= 0) {
            mvInverseTmp.set(mvSrc).invert();
            mvInverseTmp.get(mvInverseFloats);
            GL20.glUniformMatrix4fv(locModelViewInverse, false, mvInverseFloats);
            haveInverse = true;
        }
        if (locNormalMat >= 0) {
            if (haveInverse) {
                normalTmp.set(mvInverseTmp).transpose();
            } else {
                normalTmp.set(mvSrc[0], mvSrc[1], mvSrc[2],
                              mvSrc[4], mvSrc[5], mvSrc[6],
                              mvSrc[8], mvSrc[9], mvSrc[10])
                         .invert().transpose();
            }
            normalTmp.get(normalMatFloats);
            GL20.glUniformMatrix3fv(locNormalMat, false, normalMatFloats);
        }

        int uv2Loc = companion.getUv2Location();
        if (uv2Loc != -1) {
            // ivec2 attribute - must use the integer pipeline. See IrisCompanionMesh
            // for the full rationale.
            int blockU = Math.max(0, Math.min(240, packedLight & 0xFFFF));
            int skyV   = Math.max(0, Math.min(240, (packedLight >>> 16) & 0xFFFF));
            // Per-BE batching: adjacent parts of one BE share lighting, so this
            // elides the GL call ~10× per Advanced Assembler / ~5× per Chemical
            // Plant. LightSampleCache already collapses cross-part lookups, so
            // the cache hit rate here is essentially 100% within one BE.
            if (blockU != lastBlockU || skyV != lastSkyV) {
                GL30.glVertexAttribI2i(uv2Loc, blockU, skyV);
                lastBlockU = blockU;
                lastSkyV = skyV;
            }
        }

        GL11.glDrawElements(GL11.GL_TRIANGLES, targetIndexCount, GL11.GL_UNSIGNED_INT, 0);
    }

    /**
     * Variant of {@link #drawCompanion(IrisCompanionMesh, Matrix4f, int)} that
     * uses <b>per-vertex</b> lightmap UV2 derived by trilinear interpolation
     * from the 8 world-space corner samples in {@code cornerUV16}.
     * <p>
     * The companion mesh must support the per-vertex lightmap path
     * ({@link IrisCompanionMesh#supportsPerVertexLightmap()}) — it bakes
     * trilinear weights per vertex at build time so this call only pays the
     * per-instance combine + single {@code glBufferSubData}. When the mesh
     * doesn't support it (build failed, unusual vertex format) we fall back
     * transparently to the legacy constant-UV2 path using
     * {@code packedLightFallback}.
     * <p>
     * Why this is the right default under Iris: pack shaders read {@code vaUV2}
     * per vertex anyway, so supplying per-vertex values gives a smooth
     * in-mesh gradient at zero GPU cost over the constant-UV2 path — we only
     * trade a few dozen kilobytes of per-frame CPU arithmetic for proper
     * block-light response across multi-block machines. A torch on one side
     * of an Advanced Assembler now visibly brightens just that side.
     *
     * @param companion            the companion mesh to draw (must be built)
     * @param modelView            per-instance ModelView (same as the constant
     *                             path); {@code iris_ModelViewMatInverse} /
     *                             {@code iris_NormalMat} are derived from it
     * @param cornerUV16           {@code [c0.blockU, c0.skyV, c1.blockU, ...
     *                             c7.skyV]} — 16 floats, typically produced
     *                             by {@code LightSampleCache.getOrSample8}
     * @param packedLightFallback  packed light to use when the per-vertex
     *                             path can't run (companion mesh doesn't
     *                             support it); ignored on the happy path
     */
    public void drawCompanionWithPerVertexLight(IrisCompanionMesh companion,
                                                Matrix4f modelView,
                                                float[] cornerUV16,
                                                int packedLightFallback) {
        if (!isOuter || shader == null) return;
        if (companion == null || !companion.isBuilt()) return;
        int targetVao = companion.getVaoId();
        int targetIndexCount = companion.getIndexCount();
        if (targetVao <= 0 || targetIndexCount <= 0) return;

        // Fall back to the constant-UV2 path if the companion can't do per-
        // vertex (e.g. degenerate geometry, no weights). Never silently no-op
        // on a path users are expecting to see output from.
        if (!companion.supportsPerVertexLightmap() || cornerUV16 == null || cornerUV16.length < 16) {
            drawCompanion(companion, modelView, packedLightFallback);
            return;
        }

        // Shadow pass short-circuit. Shadow maps only care about depth — pack
        // shadow vertex programs typically ignore vaUV2 entirely. Running the
        // per-vertex trilinear path here burns the top profiler hotspot
        // (writeInstanceLightmap 13.79% + uploadLightmapRange 8.83% in the
        // reported trace) for zero visible output. Worse, the 8-corner
        // sampling triggered by the caller also poisons LightSampleCache with
        // values computed under Iris's shadow-camera RenderSystem state — the
        // main pass then picks those cached values up (same frame, same key),
        // which manifests as the "blocklight stripe moves sideways when I
        // pitch the camera up/down" lighting drift the user reported. The
        // constant-UV2 path is functionally equivalent for shadow and
        // untouched by the cache.
        if (isShadowPass) {
            drawCompanion(companion, modelView, packedLightFallback);
            return;
        }

        if (isPersistent) {
            IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);
        }

        modelView.get(mvFloats);

        if (lastBoundVao != targetVao) {
            GL30.glBindVertexArray(targetVao);
            companion.prepareForShader(shader.getId());
            lastBoundVao = targetVao;
        }

        float[] mvSrc = mvFloats;

        if (locModelView >= 0) {
            GL20.glUniformMatrix4fv(locModelView, false, mvSrc);
        }

        boolean haveInverse = false;
        if (locModelViewInverse >= 0) {
            mvInverseTmp.set(mvSrc).invert();
            mvInverseTmp.get(mvInverseFloats);
            GL20.glUniformMatrix4fv(locModelViewInverse, false, mvInverseFloats);
            haveInverse = true;
        }
        if (locNormalMat >= 0) {
            if (haveInverse) {
                normalTmp.set(mvInverseTmp).transpose();
            } else {
                normalTmp.set(mvSrc[0], mvSrc[1], mvSrc[2],
                              mvSrc[4], mvSrc[5], mvSrc[6],
                              mvSrc[8], mvSrc[9], mvSrc[10])
                         .invert().transpose();
            }
            normalTmp.get(normalMatFloats);
            GL20.glUniformMatrix3fv(locNormalMat, false, normalMatFloats);
        }

        // Hash the quantized corner UV2 into a stable key and request a slot that can
        // be reused across draws when the light field repeats (dense farms).
        long key = 1469598103934665603L;
        for (int k = 0; k < 16; k++) {
            int q = Math.round(cornerUV16[k]);
            if (q < 0) q = 0; else if (q > 240) q = 240;
            cornerShort16[k] = (short) q;
            cornerFloat16[k] = (float) q;
            key ^= (q & 0xFFFF);
            key *= 1099511628211L;
        }
        // Ensure we have a modest slot budget for the per-part path. Unlike the instanced
        // renderer, this path can't cheaply know the total instance count up front, so we
        // keep a fixed minimum and rely on eviction beyond it.
        companion.ensureLightmapCapacity(32);
        long alloc = companion.allocLightmapSlot(key);
        int cachedSlot = (int) (alloc & 0xFFFF_FFFFL);
        boolean reused = (alloc >>> 32) != 0L;
        if (!reused) {
            companion.writeInstanceLightmap(cachedSlot, cornerFloat16);
        }
        companion.finishLightmapWrites();
        companion.activatePerVertexLightmap();
        companion.bindLightmapForInstance(cachedSlot);
        // The constant-UV2 cache is now stale; bust it so a subsequent plain
        // drawCompanion() re-issues glVertexAttribI2i instead of trusting an
        // outdated "last" pair that no longer matches the VAO's state.
        lastBlockU = Integer.MIN_VALUE;
        lastSkyV = Integer.MIN_VALUE;

        GL11.glDrawElements(GL11.GL_TRIANGLES, targetIndexCount, GL11.GL_UNSIGNED_INT, 0);
    }

    /**
     * Per-vertex path for tall meshes that use a 2×4×2 world probe lattice
     * ({@link com.hbm_m.client.render.LightSampleCache#getOrSample16} → {@code float[32]}),
     * matching the vanilla VBO / instanced-sliced path under {@code USE_SLICED_LIGHT}.
     * <p>
     * When the mesh has no {@link IrisCompanionMesh#supportsSlicedPerVertexLightmap() sliced
     * weights}, fall back to {@link #drawCompanionWithPerVertexLight} (8 corners) or
     * {@link #drawCompanion}.
     */
    public void drawCompanionWithSlicedPerVertexLight(IrisCompanionMesh companion,
                                                      Matrix4f modelView,
                                                      float[] probeUV32,
                                                      int packedLightFallback) {
        if (!isOuter || shader == null) return;
        if (companion == null || !companion.isBuilt()) return;
        int targetVao = companion.getVaoId();
        int targetIndexCount = companion.getIndexCount();
        if (targetVao <= 0 || targetIndexCount <= 0) return;

        if (!companion.supportsSlicedPerVertexLightmap() || probeUV32 == null || probeUV32.length < 32) {
            // Do not map the first 16 floats of a 2×4×2 lattice onto 8-corner weights — layouts differ.
            drawCompanion(companion, modelView, packedLightFallback);
            return;
        }

        if (isShadowPass) {
            drawCompanion(companion, modelView, packedLightFallback);
            return;
        }

        if (isPersistent) {
            IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);
        }

        modelView.get(mvFloats);

        if (lastBoundVao != targetVao) {
            GL30.glBindVertexArray(targetVao);
            companion.prepareForShader(shader.getId());
            lastBoundVao = targetVao;
        }

        float[] mvSrc = mvFloats;

        if (locModelView >= 0) {
            GL20.glUniformMatrix4fv(locModelView, false, mvSrc);
        }

        boolean haveInverse = false;
        if (locModelViewInverse >= 0) {
            mvInverseTmp.set(mvSrc).invert();
            mvInverseTmp.get(mvInverseFloats);
            GL20.glUniformMatrix4fv(locModelViewInverse, false, mvInverseFloats);
            haveInverse = true;
        }
        if (locNormalMat >= 0) {
            if (haveInverse) {
                normalTmp.set(mvInverseTmp).transpose();
            } else {
                normalTmp.set(mvSrc[0], mvSrc[1], mvSrc[2],
                              mvSrc[4], mvSrc[5], mvSrc[6],
                              mvSrc[8], mvSrc[9], mvSrc[10])
                         .invert().transpose();
            }
            normalTmp.get(normalMatFloats);
            GL20.glUniformMatrix3fv(locNormalMat, false, normalMatFloats);
        }

        long key = 1469598103934665603L;
        for (int k = 0; k < 32; k++) {
            int q = Math.round(probeUV32[k]);
            if (q < 0) q = 0;
            else if (q > 240) q = 240;
            quantProbe32[k] = (float) q;
            key ^= (q & 0xFFFF);
            key *= 1099511628211L;
        }

        companion.ensureLightmapCapacity(32);
        long alloc = companion.allocLightmapSlot(key);
        int cachedSlot = (int) (alloc & 0xFFFF_FFFFL);
        boolean reused = (alloc >>> 32) != 0L;
        if (!reused) {
            companion.writeInstanceLightmap(cachedSlot, quantProbe32);
        }
        companion.finishLightmapWrites();
        companion.activatePerVertexLightmap();
        companion.bindLightmapForInstance(cachedSlot);
        lastBlockU = Integer.MIN_VALUE;
        lastSkyV = Integer.MIN_VALUE;

        GL11.glDrawElements(GL11.GL_TRIANGLES, targetIndexCount, GL11.GL_UNSIGNED_INT, 0);
    }

    @Override
    public void close() {
        if (!isOuter) return;
        // Persistent shadow batches deliberately outlive the caller's try-with-resources
        // so we can amortise apply()/clear() across every BE in one shadow pass. They
        // are torn down later by actuallyClose() - either from a pass-change in the
        // next begin() or from the AFTER_LEVEL safety-net.
        if (isPersistent) return;
        actuallyClose();
    }

    /**
     * Real teardown - invoked either from {@link #close} for non-persistent batches,
     * or from the persistent-batch lazy-close paths ({@link #begin} pass-change and
     * {@link #closePersistentIfActive}).
     */
    private void actuallyClose() {
        try {
            if (shader != null) {
                shader.clear();
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("IrisRenderBatch.close: shader.clear() threw {}", t.toString());
        } finally {
            tryRestoreState();
            isOuter = false;
            isPersistent = false;
            isShadowPass = false;
            shader = null;
            ACTIVE = null;
        }
    }

    private void tryRestoreState() {
        try {
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            if (previousCullEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(previousBlockEntityId);
            if (phaseGuard != null) {
                phaseGuard.close();
                phaseGuard = null;
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("IrisRenderBatch.tryRestoreState failed", t);
        }
    }

    /**
     * Drops cached uniform handles. Call after a shader pipeline rebuild (F3+T,
     * shader pack swap) so the next {@link #begin} re-resolves them against the
     * fresh program ID. Also force-closes any leftover persistent shadow batch so
     * we never reuse a batch built against a now-deleted shader program.
     */
    public static void invalidateCaches() {
        if (ACTIVE != null && ACTIVE.isPersistent) {
            ACTIVE.actuallyClose();
        }
        INSTANCE.cachedShaderProgram = -1;
        INSTANCE.cachedShaderInstance = null;
        INSTANCE.cachedPipelineGeneration = -1L;
        INSTANCE.uModelView = null;
        INSTANCE.locModelView = -1;
        INSTANCE.locModelViewInverse = -1;
        INSTANCE.locNormalMat = -1;
        INSTANCE.lastBoundVao = -1;
        INSTANCE.lastBlockU = Integer.MIN_VALUE;
        INSTANCE.lastSkyV = Integer.MIN_VALUE;
    }
}
