package com.hbm_m.client.render.shader;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.hbm_m.client.render.GlVaoSafety;
import com.hbm_m.client.render.IrisCompanionMesh;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
@OnlyIn(Dist.CLIENT)
//?}
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
    private IrisDerivedMatrixUniforms.Locations matrixLocs = IrisDerivedMatrixUniforms.Locations.NONE;

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
        //? if forge || neoforge {
        // Shadow pass открывает NON-PERSISTENT батч: он живёт строго внутри
        // одного BER (try-with-resources вызывающего закрывает его до
        // возврата из render()). Раньше существовал персистентный shadow-батч,
        // переживавший границу проходов: его ленивый teardown (ExtendedShader
        // .clear() ребиндит MAIN FBO + восстановление устаревшей фазы) падал
        // на произвольные моменты основного прохода — на 1.20.1 это задваивало
        // анимированную растительность. Полный запрет кастомного GL в shadow
        // лечил это, но putBulkData на каждую часть стоил ~80% кадра (spark:
        // цепочки BufferBuilder.vertex). Компромисс: apply()/clear() ОДИН раз
        // теневого цикла BE (ровно как ванильные BE на endBatch), без
        // накопления инстансов и без состояния через границу проходов.
        // Fallback без батча — putBulkData через bufferSource.

        // Pass-change detection: if the active batch's pass differs from the
        // requested one, tear it down before opening the new one. Covers BOTH
        // persistent (main lingering into next frame's shadow) and non-persistent
        // (leaked shadow batch) — a stale cross-pass ACTIVE must never survive.
        if (ACTIVE != null && ACTIVE.isShadowPass != shadowPass) {
            ACTIVE.actuallyClose();
        }

        // Same-pass nested call - every subsequent BE in the same pass piggy-backs
        // on the existing persistent batch via active().
        if (ACTIVE != null && ACTIVE.isPersistent) {
            return NOOP_NESTED;
        }

        // Defensive: a non-persistent (shadow) batch is ACTIVE - nested call
        // within the same BER piggy-backs; only the outer try-with-resources
        // (which received the real INSTANCE) closes it.
        if (ACTIVE != null) {
            return NOOP_NESTED;
        }

        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) {
            return null;
        }
        try {
            INSTANCE.setupOuter(shader, projectionMatrix);
            // Main pass — персистентный (одно apply на кадр, закрытие в
            // presentAfterBlockEntities / AFTER_LEVEL / при смене фазы).
            // Shadow pass — только на время BER (закрывается close()).
            INSTANCE.isPersistent = !shadowPass;
            INSTANCE.isShadowPass = shadowPass;
            ACTIVE = INSTANCE;
            return shadowPass ? INSTANCE : NOOP_NESTED;
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
     * Closes any batch (persistent main or leaked non-persistent shadow) still
     * active at end-of-frame checkpoints. Called from {@code RenderLevelStageEvent
     * .AFTER_BLOCK_ENTITIES}/{@code AFTER_LEVEL} as the safety net for the LAST
     * batch of every frame. A non-persistent shadow batch is normally closed by
     * its BER's try-with-resources; if an exceptional path leaked one, closing
     * it here prevents a stale shadow-programmed ACTIVE from servicing the next
     * frame's main-pass BEs.
     */
    public static void closePersistentIfActive() {
        if (ACTIVE != null) {
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

        this.previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        this.previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        this.previousCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        this.previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        this.phaseGuard = IrisPhaseGuard.pushBlockEntities();

        RenderSystem.setShader(() -> shader);

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(projectionMatrix);
        }
        if (shader.MODEL_VIEW_MATRIX != null) {
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

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        var blockAtlas = Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);
        com.mojang.blaze3d.systems.RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        com.mojang.blaze3d.systems.RenderSystem.bindTexture(blockAtlas.getId());

        if (!IrisShaderApply.tryApply(shader)) {
            throw new IllegalStateException("ExtendedShader.apply() failed (destroyed GlResource or invalid pipeline phase)");
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();

        int programId = shader.getId();
        long currentGen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (cachedShaderProgram != programId
                || cachedShaderInstance != shader
                || cachedPipelineGeneration != currentGen) {
            cachedShaderProgram = programId;
            cachedShaderInstance = shader;
            cachedPipelineGeneration = currentGen;
            matrixLocs = IrisDerivedMatrixUniforms.resolve(shader);
        }

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
     * Binds the companion VAO and resolves Iris-extended attribute pointers.
     * The CPU {@link #lastBoundVao} cache can diverge from the real GL binding
     * when Embeddium/Iris rebind VAO 0 between draws in the same batch.
     */
    private void bindCompanionVao(IrisCompanionMesh companion, int targetVao) {
        if (shader == null || !companion.isBuilt()) return;
        GlStateManager._glBindVertexArray(targetVao);
        companion.prepareForShader(shader.getId());
        if (lastBoundVao != targetVao) {
            // VAO changed: its UV2 generic-attrib still holds whatever value was
            // last written on THAT VAO (possibly stale from a previous BE/frame,
            // or 0 on a freshly primed VAO). The constant-UV2 fast path below
            // would otherwise trust the lastBlockU/lastSkyV carried over from the
            // previous VAO and skip the glVertexAttribI2i re-issue when two BEs
            // share the same packedLight — rendering the 2nd+ machine with a
            // wrong/zero lightmap. restoreConstantLightmap() is a no-op in
            // constant mode, so the cache must be invalidated here.
            lastBlockU = Integer.MIN_VALUE;
            lastSkyV = Integer.MIN_VALUE;
        }
        lastBoundVao = targetVao;
    }

    /**
     * Detaches the companion VAO after a draw so intermediate vanilla/Iris work
     * (outline, chunk uploads) does not inherit our vertex layout. Restores the
     * VAO captured in {@link #setupOuter()} (including {@code 0} = unbind).
     * Do not substitute an empty dummy VAO here — that leaves {@code drawElements}
     * with no valid vertex state and can crash the GL driver.
     */
    private void releaseCompanionVaoAfterDraw(IrisCompanionMesh companion) {
        if (companion != null) {
            companion.restoreConstantLightmap();
        }
        GlVaoSafety.bindVertexArray(previousVao);
        lastBoundVao = -1;
    }

    /**
     * Runs a short vanilla draw (recipe icon, item BER overlay) while a persistent
     * Iris batch is open. Re-applies the batch shader afterward so the next
     * {@link #drawCompanion} still hits the correct program.
     */
    public static void runVanillaOverlay(Runnable draw) {
        IrisRenderBatch batch = ACTIVE;
        if (batch == null || !batch.isOuter || batch.shader == null) {
            draw.run();
            return;
        }
        batch.runVanillaOverlayInner(draw);
    }

    private void runVanillaOverlayInner(Runnable draw) {
        int vaoBeforeOverlay = GlVaoSafety.currentBinding();
        try {
            GlVaoSafety.bindVertexArray(0);
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            draw.run();
        } finally {
            RenderSystem.setShader(() -> shader);
            if (!IrisShaderApply.tryApply(shader)) {
                MainRegistry.LOGGER.warn("IrisRenderBatch.runVanillaOverlay: ExtendedShader.apply() failed");
            }
            GlVaoSafety.bindVertexArray(vaoBeforeOverlay);
            lastBoundVao = -1;
        }
    }

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

        bindCompanionVao(companion, targetVao);
        companion.restoreConstantLightmap();

        float[] mvSrc = mvFloats;

        // Direct GL upload of ModelViewMat - skips Mojang's Uniform.set/upload
        // proxy stack (ShaderInstance.Uniform.upload → uploadAsMatrix →
        // RenderSystem.glUniformMatrix4 → GlStateManager._glUniformMatrix4 →
        // GL20C.nglUniformMatrix4fv). Profiler attributed ~8.67% of frame time
        // to those layers in the analogous flushBatchIris loop; we get the same
        // win here.
        if (matrixLocs.modelView() >= 0) {
            GL20.glUniformMatrix4fv(matrixLocs.modelView(), false, mvSrc);
        }

        // ExtendedShader.apply() derived inverse/normal from the IDENTITY matrix
        // we passed in setupOuter; per-instance we re-derive from the real ModelView.
        boolean haveInverse = false;
        if (matrixLocs.modelViewInverse() >= 0) {
            mvInverseTmp.set(mvSrc).invert();
            mvInverseTmp.get(mvInverseFloats);
            GL20.glUniformMatrix4fv(matrixLocs.modelViewInverse(), false, mvInverseFloats);
            haveInverse = true;
        }
        if (matrixLocs.normalMat() >= 0) {
            if (haveInverse) {
                normalTmp.set(mvInverseTmp).transpose();
            } else {
                normalTmp.set(mvSrc[0], mvSrc[1], mvSrc[2],
                              mvSrc[4], mvSrc[5], mvSrc[6],
                              mvSrc[8], mvSrc[9], mvSrc[10])
                         .invert().transpose();
            }
            normalTmp.get(normalMatFloats);
            GL20.glUniformMatrix3fv(matrixLocs.normalMat(), false, normalMatFloats);
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
                companion.bindVaoIfNeeded();
                GL30.glVertexAttribI2i(uv2Loc, blockU, skyV);
                lastBlockU = blockU;
                lastSkyV = skyV;
            }
        }

        companion.bindVaoIfNeeded();
        GL11.glDrawElements(GL11.GL_TRIANGLES, targetIndexCount, GL11.GL_UNSIGNED_INT, 0);
        releaseCompanionVaoAfterDraw(companion);
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

        bindCompanionVao(companion, targetVao);

        float[] mvSrc = mvFloats;

        if (matrixLocs.modelView() >= 0) {
            GL20.glUniformMatrix4fv(matrixLocs.modelView(), false, mvSrc);
        }

        boolean haveInverse = false;
        if (matrixLocs.modelViewInverse() >= 0) {
            mvInverseTmp.set(mvSrc).invert();
            mvInverseTmp.get(mvInverseFloats);
            GL20.glUniformMatrix4fv(matrixLocs.modelViewInverse(), false, mvInverseFloats);
            haveInverse = true;
        }
        if (matrixLocs.normalMat() >= 0) {
            if (haveInverse) {
                normalTmp.set(mvInverseTmp).transpose();
            } else {
                normalTmp.set(mvSrc[0], mvSrc[1], mvSrc[2],
                              mvSrc[4], mvSrc[5], mvSrc[6],
                              mvSrc[8], mvSrc[9], mvSrc[10])
                         .invert().transpose();
            }
            normalTmp.get(normalMatFloats);
            GL20.glUniformMatrix3fv(matrixLocs.normalMat(), false, normalMatFloats);
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
        releaseCompanionVaoAfterDraw(companion);
    }

    /**
     * Per-vertex path for tall meshes that use a 2×4×2 world probe lattice.
     * REMOVED together with the sliced-light system: the mesh per-vertex lightmap
     * is now 8-corner trilinear only (see {@link #drawCompanionWithPerVertexLight}).
     */

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
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            lastBoundVao = -1;

            if (previousCullEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();

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
        INSTANCE.matrixLocs = IrisDerivedMatrixUniforms.Locations.NONE;
        INSTANCE.lastBoundVao = -1;
        INSTANCE.lastBlockU = Integer.MIN_VALUE;
        INSTANCE.lastSkyV = Integer.MIN_VALUE;
    }
}
