package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.IrisShaderApply;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

@OnlyIn(Dist.CLIENT)
/**
 * Global shadow-pass batch collector for Iris / Oculus shaderpacks.
 * <p>
 * During the shadow pass, BlockEntityRenderers record their instance transformations
 * (position, rotation, 3x4 affine matrices, bone palettes) into this collector rather than
 * issuing immediate individual draw calls.
 * <p>
 * At the end of the shadow pass, {@link #flushGlobalShadowBatch()} is invoked:
 * <ul>
 *   <li><b>Tier 1 (GPU Compute Bake):</b> {@link NucleusGpuBaker} bakes multiblock parts
 *       directly in VRAM and draws with the active pack shadow shader.</li>
 *   <li><b>Tier 2 (Instanced Shadow):</b> When compatible, draws instanced batches using
 *       {@link com.hbm_m.client.render.shader.IrisInstancedShaders}.</li>
 *   <li><b>Tier 3 (Batch Companion Fallback):</b> Submits companion meshes using
 *       {@link IrisRenderBatch#begin(boolean, Matrix4f)}.</li>
 * </ul>
 *
 * @credit Iris / coderbot / IMS
 */
public final class IrisShadowBatchCollector {

    /** Emergency JVM override -Dhbm.shadowInstancing=false; the config nucleusShadowInstancing is the primary source. */
    private static final boolean SHADOW_INSTANCING_FLAG =
            !"false".equalsIgnoreCase(System.getProperty("hbm.shadowInstancing", "true"));

    /** Floats per instance record — matches instanced data layout. */
    private static final int FLOATS_PER_INSTANCE = InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
    private static final int GROW_INSTANCES = 256;

    public static final class Entry {
        public final InstancedStaticPartRenderer renderer;
        public final IrisCompanionMesh customMesh;
        public FloatBuffer data;
        public int count;
        public FloatBuffer prebakeDirectView = null;
        public long mutationEpoch = -1L;

        public FloatBuffer activeBuffer() {
            return prebakeDirectView != null ? prebakeDirectView : data;
        }

        Entry(InstancedStaticPartRenderer renderer) {
            this.renderer = renderer;
            this.customMesh = null;
            this.data = MemoryUtil.memAllocFloat(GROW_INSTANCES * FLOATS_PER_INSTANCE);
        }

        Entry(IrisCompanionMesh customMesh) {
            this.renderer = null;
            this.customMesh = customMesh;
            this.data = MemoryUtil.memAllocFloat(GROW_INSTANCES * FLOATS_PER_INSTANCE);
        }

        public IrisCompanionMesh getCompanionMesh() {
            if (customMesh != null) return customMesh;
            return renderer != null ? renderer.getOrBuildCompanionForShadowBatch() : null;
        }

        public String debugTag() {
            if (renderer != null) {
                String tag = renderer.getMdiTraceTag();
                if (tag != null && !tag.isEmpty()) return tag;
                return renderer.getClass().getSimpleName();
            }
            if (customMesh != null) {
                String tag = customMesh.getDebugName();
                if (tag != null && !tag.isEmpty() && !"unknown".equals(tag)) return tag;
            }
            return "unknown";
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>(64);
    /** Identity indices over ENTRIES: record() is the hot path (thousands per frame);
     *  the previous linear scan cost O(entries) per record (audit 0925). */
    private static final java.util.IdentityHashMap<InstancedStaticPartRenderer, Entry> BY_RENDERER =
            new java.util.IdentityHashMap<>(64);
    private static final java.util.IdentityHashMap<IrisCompanionMesh, Entry> BY_MESH =
            new java.util.IdentityHashMap<>(64);
    private static boolean batchingEnabled = true;
    private static int staleFrames = 0;
    private static boolean disabledLogged = false;
    private static long disabledAtGeneration = -1L;
    /** Wall clock of the disable; the latch re-arms after this many ms even without
     * a pipeline generation change (a one-shot failure used to latch shadow
     * batching off until the next Iris rebuild - part of the "mode does not
     * reset" symptom, 2026-10-06 audit). */
    private static final long DISABLE_RETRY_MS = 10_000L;
    private static long disabledAtMs = Long.MIN_VALUE;

    static {
        // Audit 2026-10-05 §3.2: onMeshDestroyed alone frees per-mesh records,
        // but entries can also outlive their meshes in bulk (e.g. meshes
        // destroyed between the record and the flush). Register the session
        // teardown so F3+T / disconnect releases every remaining native record
        // buffer through the central invalidation point.
        RenderCacheManager.register(reason -> resetAllMemory());
    }

    private IrisShadowBatchCollector() {}

    public static boolean isBatchingEnabled() {
        return batchingEnabled;
    }

    // ── Per-frame counter for MachineBer animation caching ─────────────
    // Tracks rendering frames across shadow and main passes.
    private static long renderFrame = 0;
    private static boolean shadowSinceMain = false;

    public static long renderFrame() {
        return renderFrame;
    }

    static void noteShadowRecord() {
        if (!shadowSinceMain) {
            shadowSinceMain = true;
            renderFrame++;
        }
    }

    private static void noteMainFrameStart() {
        com.hbm_m.client.render.NucleusDispatcherBypass.noteMainFrameStart();
        if (!shadowSinceMain) {
            renderFrame++;
        }
        shadowSinceMain = false;
    }

    // Shadow projection + player camera stashed at recording time (RenderSystem state
    // of the shadow pass). The camera is stashed because appendRecord adds it to the
    // record translation, and every consumer (Tier 1 bake ModelViewMat T(-cam), Tier 2,
    // Tier 3) folds it back out at draw time. The records themselves are LIGHT-space:
    // each one decomposes the Iris shadow poseStack MV_shadow * T(bePos - camPos) * part,
    // so the light rotation rides inside the record. The shadow MV itself is deliberately
    // NOT stashed: the contract forbids absorbing it into the records.
    private static final Matrix4f stashProj = new Matrix4f();
    private static final Matrix4f stashShadowMv = new Matrix4f();
    private static final Matrix4f stashShadowMvInv = new Matrix4f();
    private static boolean stashShadowMvValid = false;
    private static float stashCamX, stashCamY, stashCamZ;
    private static boolean stashValid = false;
    private static int shadowAnchorX, shadowAnchorY, shadowAnchorZ;
    private static boolean anchorSpaceShadowActive = false;

    public static void beginShadowPass(Matrix4f shadowMv, Matrix4f shadowProj, double camX, double camY, double camZ) {
        stashShadowMv.set(shadowMv);
        stashShadowMvInv.set(shadowMv).invertAffine();
        stashProj.set(shadowProj);
        stashCamX = (float) camX;
        stashCamY = (float) camY;
        stashCamZ = (float) camZ;
        stashValid = true;
        stashShadowMvValid = true;
        anchorSpaceShadowActive = false;
    }

    public static boolean isAnchorSpaceShadowActive() {
        return anchorSpaceShadowActive;
    }

    public static void buildShadowModelView(Matrix4f outMv, float camX, float camY, float camZ) {
        if (anchorSpaceShadowActive && stashShadowMvValid) {
            outMv.set(stashShadowMv).translate(
                    (float) shadowAnchorX - camX,
                    (float) shadowAnchorY - camY,
                    (float) shadowAnchorZ - camZ);
        } else {
            outMv.translation(-camX, -camY, -camZ);
        }
    }

    public static void activateAnchorSpacePrebakes() {
        if (!batchingEnabled || !stashValid || !stashShadowMvValid) {
            return;
        }
        BlockPos anchor = FrameViewState.anchorOrigin();
        shadowAnchorX = anchor.getX();
        shadowAnchorY = anchor.getY();
        shadowAnchorZ = anchor.getZ();
        anchorSpaceShadowActive = true;

        for (java.util.Map.Entry<InstancedStaticPartRenderer, PrebakeSnapshot> pe : PREBAKES.entrySet()) {
            InstancedStaticPartRenderer renderer = pe.getKey();
            PrebakeSnapshot s = pe.getValue();
            if (s.epoch != prebakeEpoch || s.count <= 0 || s.records == null) {
                continue;
            }
            if (s.anchorX != shadowAnchorX || s.anchorY != shadowAnchorY || s.anchorZ != shadowAnchorZ) {
                continue;
            }
            Entry e = BY_RENDERER.get(renderer);
            if (e == null) {
                e = new Entry(renderer);
                BY_RENDERER.put(renderer, e);
            }
            if (e.count == 0) {
                ENTRIES.add(e);
            }
            e.prebakeDirectView = s.records;
            e.count = s.count;
            e.mutationEpoch = s.bakedMutationEpoch;
        }
    }

    private static final Matrix4f RECOVERED_SHADOW_MV = new Matrix4f();

    public static void ensureAnchorSpaceShadowFromBePose(Matrix4f beShadowPose, BlockPos bePos) {
        if (anchorSpaceShadowActive || !batchingEnabled || PREBAKES.isEmpty()) {
            return;
        }
        var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        RECOVERED_SHADOW_MV.set(beShadowPose).translate(
                (float) (cam.x - bePos.getX()),
                (float) (cam.y - bePos.getY()),
                (float) (cam.z - bePos.getZ()));
        beginShadowPass(RECOVERED_SHADOW_MV, com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix(), cam.x, cam.y, cam.z);
        activateAnchorSpacePrebakes();
    }

    public static void stashShadowMatrices(Matrix4f proj) {
        stashProj.set(proj);
        var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        stashCamX = (float) cam.x;
        stashCamY = (float) cam.y;
        stashCamZ = (float) cam.z;
        stashValid = true;
    }

    private static void clearStash() {
        stashValid = false;
        stashShadowMvValid = false;
        anchorSpaceShadowActive = false;
        // SHADOW-EPOCH: the shadow basis is only meaningful for the shadow pass that
        // stashed the matrices; the next pass recalibrates from fresh fallback records.
        prebakeCalibrated = false;
        prebakeCalibSawFirst = false;
        prebakeCalibFrame = -1L;
    }

    /**
     * Universal part instance recording: the affine pose is decomposed into
     * pos + normalized rotation (the record layout the shared instance VAO reads);
     * non-uniform scale is dropped, matching the main-pass instanced format.
     */
    public static void record(InstancedStaticPartRenderer renderer, Matrix4f fullTransform,
                              float[] bboxMin, float[] cornerUV16) {
        if (!batchingEnabled
                && (IrisExtendedShaderAccess.getPipelineGeneration() != disabledAtGeneration
                    || System.currentTimeMillis() - disabledAtMs >= DISABLE_RETRY_MS)) {
            batchingEnabled = true;
        }
        noteShadowRecord();
        Entry e = BY_RENDERER.get(renderer);
        if (e == null) {
            e = new Entry(renderer);
            BY_RENDERER.put(renderer, e);
        }
        if (e.count == 0) {
            shrinkOversizedBuffer(e);
            ENTRIES.add(e);
        }
        appendRecord(e, renderer, fullTransform, bboxMin, cornerUV16);
    }

    /**
     * Universal custom mesh recording for shadow pass.
     */
    public static void recordCustomMesh(IrisCompanionMesh companion, Matrix4f fullTransform) {
        if (companion == null || !companion.isBuilt()) {
            return;
        }
        if (!batchingEnabled
                && (IrisExtendedShaderAccess.getPipelineGeneration() != disabledAtGeneration
                    || System.currentTimeMillis() - disabledAtMs >= DISABLE_RETRY_MS)) {
            batchingEnabled = true;
        }
        noteShadowRecord();
        Entry e = BY_MESH.get(companion);
        if (e == null) {
            e = new Entry(companion);
            BY_MESH.put(companion, e);
        }
        if (e.count == 0) {
            shrinkOversizedBuffer(e);
            ENTRIES.add(e);
        }
        // Custom mesh: renderer == null -> identity uvRect (atlas UVs).
        appendRecord(e, null, fullTransform, null, null);
    }

    /**
     * Records an instance via position and rotation quaternion (backward compatibility).
     */
    public static void record(InstancedStaticPartRenderer renderer, Vector3f pos, Quaternionf rot,
                              float[] bboxMin, float[] cornerUV16) {
        Matrix4f mat = new Matrix4f().translationRotate(pos, rot);
        record(renderer, mat, bboxMin, cornerUV16);
    }

    /** Render-thread-only scratch for the affine → pos/quat decomposition (hot record path). */
    private static final Vector3f SCRATCH_POS = new Vector3f();
    private static final Quaternionf SCRATCH_ROT = new Quaternionf();
    private static final Matrix4f SCRATCH_FALLBACK_MAT = new Matrix4f();

    private static void appendRecord(Entry e, InstancedStaticPartRenderer renderer, Matrix4f fullTransform,
                                     float[] bboxMin, float[] cornerUV16) {
        if (e.prebakeDirectView != null) {
            int floats = e.count * FLOATS_PER_INSTANCE;
            if (floats + FLOATS_PER_INSTANCE > e.data.capacity()) {
                int newCap = e.data.capacity() * 2;
                while (newCap < floats + FLOATS_PER_INSTANCE) newCap *= 2;
                FloatBuffer grown = MemoryUtil.memAllocFloat(newCap);
                MemoryUtil.memFree(e.data);
                e.data = grown;
            }
            e.data.clear();
            MemoryUtil.memCopy(MemoryUtil.memAddress0(e.prebakeDirectView), MemoryUtil.memAddress0(e.data), (long) floats * 4L);
            e.data.position(floats);
            e.prebakeDirectView = null;
            e.mutationEpoch = -1L;
        } else if ((e.count + 1) * FLOATS_PER_INSTANCE > e.data.capacity()) {
            FloatBuffer grown = MemoryUtil.memAllocFloat(e.data.capacity() * 2);
            e.data.flip();
            grown.put(e.data);
            MemoryUtil.memFree(e.data);
            e.data = grown;
        }

        if (anchorSpaceShadowActive && stashShadowMvValid) {
            SCRATCH_FALLBACK_MAT.set(stashShadowMvInv).mul(fullTransform);
            SCRATCH_POS.set(
                SCRATCH_FALLBACK_MAT.m30() + (stashCamX - shadowAnchorX),
                SCRATCH_FALLBACK_MAT.m31() + (stashCamY - shadowAnchorY),
                SCRATCH_FALLBACK_MAT.m32() + (stashCamZ - shadowAnchorZ)
            );
            SCRATCH_FALLBACK_MAT.getNormalizedRotation(SCRATCH_ROT);
        } else {
            SCRATCH_POS.set(fullTransform.m30() + stashCamX,
                            fullTransform.m31() + stashCamY,
                            fullTransform.m32() + stashCamZ);
            fullTransform.getNormalizedRotation(SCRATCH_ROT);
        }

        float[] uvRect = (renderer != null) ? renderer.getActiveUvRect() : null;
        float[] tint = (renderer != null) ? renderer.getActiveTint() : null;
        float[] grad = (renderer != null) ? renderer.getActiveGrad() : null;
        encodeLightWords(cornerUV16, SCRATCH_LIGHT_WORDS);
        writeRecordFloats(e, SCRATCH_POS, SCRATCH_ROT, bboxMin, SCRATCH_LIGHT_WORDS, uvRect, tint, grad);
        e.count++;
        com.hbm_m.client.render.NucleusDebug.recordShadowRecord(1);
    }

    /**
     * Writes ONE 32-float (128-byte) shadow record (pos/quat/bbox/fade/light/uvRect/
     * tint/grad/anim) in the shared instance layout — the single writer for BOTH record
     * sources: the shadow-pass BER fallback ({@link #appendRecord}) and the SHADOW-EPOCH
     * main-pass prebake conversion ({@link #consumePrebaked}). Does NOT bump {@code e.count}.
     * <p>
     * {@code lightWords} carries the corner light ALREADY packed into the 4 record words
     * (8 corners x 2 u8, see {@link InstancedStaticPartRenderer#packLightWord}): both
     * record sources write ZERO light words (the shadow pass is unlit; the shadow FSH
     * samples the lightmap, so real main-pass light would make consumed records' shadow
     * color diverge from the fallback's). The API keeps the parameter for callers that
     * may need real light words later (appendRecord encodes its 0..240 floats first).
     */
    private static void writeRecordFloats(Entry e, Vector3f pos, Quaternionf rot,
                                          float[] bboxMin, int[] lightWords,
                                          float[] uvRect, float[] tint, float[] grad) {
        FloatBuffer d = e.data;
        // Floats 0..11: the SAME instance layout the main pass writes and the shared
        // instance VAO + block_lit_instanced_iris.vsh read: pos @0, quat @3 (packed
        // snorm16 words 3-4), bboxMin @5, bboxSize.xyz @8, fade @11. Writing a raw 3x4
        // matrix here made Tier 2 (drawShadowInstances) interpret matrix elements as
        // position and quaternion - shattered machines in the shadow map (Tier 1/3 read
        // the matrix form, which is why the breakage only showed with GPU Bake off +
        // Shadow Batch on).
        d.put(pos.x).put(pos.y).put(pos.z);
        d.put(Float.intBitsToFloat(InstancedStaticPartRenderer.packQuatWord(rot.x, rot.y)));
        d.put(Float.intBitsToFloat(InstancedStaticPartRenderer.packQuatWord(rot.z, rot.w)));
        if (bboxMin != null && bboxMin.length >= 6) {
            d.put(bboxMin[0]).put(bboxMin[1]).put(bboxMin[2]);
            d.put(bboxMin[3] - bboxMin[0]).put(bboxMin[4] - bboxMin[1]).put(bboxMin[5] - bboxMin[2]);
        } else {
            // Custom-mesh records carry no bbox; a unit box keeps the light-interp
            // weights finite (light is zeros in the shadow pass anyway).
            d.put(0.0f).put(0.0f).put(0.0f);
            d.put(1.0f).put(1.0f).put(1.0f);
        }
        d.put(1.0f); // Float 11: fade alpha (1.0)
        // Floats 12..15 (4 words): corner light, packed u8 (already encoded).
        for (int w = 0; w < 4; w++) {
            d.put(Float.intBitsToFloat(lightWords[w]));
        }
        // Floats 16..19: InstUvRect (identity for regular machines; the layout must
        // match InstancedStaticPartRenderer.INSTANCE_DATA_SIZE).
        if (uvRect != null && uvRect.length >= 4) {
            d.put(uvRect[0]).put(uvRect[1]).put(uvRect[2]).put(uvRect[3]);
        } else {
            d.put(0.0f).put(0.0f).put(1.0f).put(1.0f);
        }
        // Floats 20..23: InstColor (the shadow FB does not use color; only the record
        // stride alignment with the VAO-read instance layout matters).
        if (tint != null && tint.length >= 4) {
            d.put(tint[0]).put(tint[1]).put(tint[2]).put(tint[3]);
        } else {
            d.put(1.0f).put(1.0f).put(1.0f).put(1.0f);
        }
        // Floats 24..27: GradParams (axis<0 = off; w = parametric joint CHAIN
        // base index, forced to -1 here: the shadow path animates on the CPU, so
        // the vertex shader's joint block must stay off). Stride alignment as
        // with InstColor.
        if (grad != null && grad.length >= 4) {
            d.put(grad[0]).put(grad[1]).put(grad[2]).put(-1.0f);
        } else {
            d.put(-1.0f).put(0.0f).put(0.0f).put(-1.0f);
        }
        // Floats 28..31: AnimParams (4 per-frame joint-chain parameters; the
        // chain base index lives in GradParams.w = -1 above, so the joint block
        // never runs for shadow records). The Iris shadow path animates on the
        // CPU (MachineBer.isParametricPath is disabled there), so the joint
        // motion is already baked into fullTransform; the stride must still match
        // the VAO-read instance layout (32 floats), or Tier 1/2 uploads and the
        // Tier 3 record walk run past the buffer.
        d.put(0.0f).put(0.0f).put(0.0f).put(0.0f);
    }

    /** Scratch: encoded corner-light words for {@link #appendRecord} (render thread only). */
    private static final int[] SCRATCH_LIGHT_WORDS = new int[4];

    /** Encodes 16 corner-light floats (0..240 texel units) into the 4 packed record words. */
    private static void encodeLightWords(float[] cornerUV16, int[] outWords) {
        for (int w = 0; w < 4; w++) {
            int b = w * 4;
            if (cornerUV16 != null && cornerUV16.length >= b + 4) {
                outWords[w] = InstancedStaticPartRenderer.packLightWord(
                        cornerUV16[b], cornerUV16[b + 1], cornerUV16[b + 2], cornerUV16[b + 3]);
            } else {
                outWords[w] = 0;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHADOW-EPOCH: main-pass record reuse for the Iris shadow pass.
    //
    // Problem: every shadow frame Oculus re-dispatches every machine BER, which
    // re-runs the full CPU collection (~14% of the frame on machine clusters).
    //
    // Mechanism (per-frame order: shadow N -> main N -> shadow N+1 ...):
    //
    //  1. RECORD in MAIN. The main pass already computes everything a shadow
    //     record needs: addInstance (with collectMain) converts the part pose to
    //     a bob-stripped WORLD record (convertToWorldRecord: pos = world part
    //     position relative to FrameViewState.anchorOrigin, quat = world part
    //     rotation; light corners sampled by fillInstanceCornerLight). At
    //     InstancedStaticPartRenderer.flush the whole flushed record set is
    //     COPIED here as a per-renderer PrebakeSnapshot (records + roster keys +
    //     per-slot posQuat-stability flags), stamped with prebakeEpoch.
    //
    //  2. CONSUME in SHADOW. The dispatcher mixin (NucleusDispatcherBypass
    //     .shouldBypass, shadow branch) asks MachineBer.tryConsumePrebakedShadow
    //     whether the machine may skip its BER. The gate is generic and
    //     FAIL-CLOSED: spec has no render hooks (they draw immediate content
    //     into the shadow map), every drawn part resolves to an initialized
    //     InstancedStaticPartRenderer (no single-VBO/immediate parts), animated
    //     parts beyond the animation zone are skipped exactly as renderAll
    //     skips them, and EVERY drawn part renderer holds a fresh snapshot slot
    //     for this machine's posKey (epoch match + run map). On success the
    //     machine's slots are converted world -> light-space and appended into
    //     the regular ENTRIES (same 32-float record layout, single writer
    //     writeRecordFloats): pos = R_shadow*(S_t + (bePos - camPos)) + t_shadow
    //     + cam, rot = q_shadow * q_world, light/uvRect/tint copied from the main
    //     record (zero re-sampling), fade = 1.0, GradParams.w forced -1,
    //     AnimParams zeros - byte-parity with the fallback records.
    //
    //  3. CALIBRATION (self-proving, no Iris internals). R_shadow / t_shadow
    //     are solved from the fallback records themselves: while uncalibrated,
    //     NO machine is skipped, so the first dispatched machines run their BER
    //     and tryRecordShadowInstance feeds notePrebakeCalibration. For a
    //     and tryRecordShadowInstance feeds notePrebakeCalibration. For a slot whose
    //     stability was VERIFIED by an actual posQuat compare (slotPosQuatVerified -
    //     roster-assert / skip-write trust does not qualify) the light record is
    //     MV_shadow * T(be-cam) * S with S
    //     known from the snapshot -> R_shadow = R_rec * R_S^-1,
    //     t_shadow = recTr - R_shadow * ((be-cam) + S_t). Two independent
    //     stable slots must agree (R within 1e-3, t within 1.0) before
    //     prebakeCalibrated() turns true for the frame; any disagreement
    //     restarts validation. Frames where no stable slot appears simply run
    //     the old path.
    //
    //  4. FALLBACK COVERAGE. Anything without a complete fresh record set runs
    //     the normal shadow BER and records exactly as before: newcomers
    //     (first frame), machines outside the MAIN frustum but inside the
    //     shadow frustum, dirty/TTL-rebuilding machines before their first
    //     flush, fade-transitioning machines beyond the static cutoff, hook
    //     and immediate-part machines, contraptions (never managed), and
    //     renderers whose snapshot went stale (epoch mismatch). A skipped BER
    //     is provably covered every time; animated parts use the previous main
    //     pose (one-frame shadow latency, identical to what the main pass
    //     drew). Reset paths: onRendererDestroyed / resetAllMemory free the
    //     snapshots; clearStash + the renderFrame bump invalidate calibration
    //     per shadow pass.
    // ═══════════════════════════════════════════════════════════════════════

    /** Bumped once per main render pass (onMainPassFrameStart); freshness stamp of the snapshots. */
    private static long prebakeEpoch = 0L;

    private static final java.util.IdentityHashMap<InstancedStaticPartRenderer, PrebakeSnapshot> PREBAKES =
            new java.util.IdentityHashMap<>(64);

    /** Per-renderer copy of the last main-pass flushed record set (world-space records). */
    private static final class PrebakeSnapshot {
        FloatBuffer records;      // count * FLOATS_PER_INSTANCE (direct, absolute reads)
        boolean[] posQuatStable;  // per slot of THIS publish (renderer bookkeeping, tolerance compare)
        /**
         * Per slot: the stable flag was earned by an actual posQuat compare this frame
         * (not roster-assert trust, not the static-pose skip-write assumption). Only
         * compare-verified slots may calibrate the shadow basis - a trusted slot whose
         * pose moved since an older snapshot (door mid-travel, melt rising) solves a
         * t_shadow wrong by exactly the part travel and poisons every consumed machine.
         */
        boolean[] posQuatVerified;
        int count;
        long epoch = -1L;
        long bakedMutationEpoch = -1L;
        long bakedLayoutEpoch = -1L;
        /**
         * Anchor origin at publish time. The records are anchor-relative (world
         * pos = record + anchor), so consumers MUST resolve them against the
         * snapshot's own anchor, never a freshly captured one: during the shadow
         * pass a FrameViewState.anchorOrigin() call used to trigger capture()
         * mid-pass, whose checkAnchorDrift can rebase the anchor and free these
         * very records under the reader (use-after-free). With the snapshot
         * anchor the conversion stays self-consistent; if the anchor really did
         * drift, onRenderOriginChanged has already cleared PREBAKES, so the
         * consume fails closed.
         */
        int anchorX, anchorY, anchorZ;
        /** posKey -> contiguous slot runs; a key reappearing non-contiguously is marked ambiguous (len = -1). */
        final Long2IntOpenHashMap firstSlotByKey = new Long2IntOpenHashMap(16);
        final Long2IntOpenHashMap runLenByKey = new Long2IntOpenHashMap(16);
    }

    /**
     * Publishes the renderer's flushed main-pass records as the prebake snapshot
     * (called from InstancedStaticPartRenderer.flush BEFORE the flush branches -
     * the buffer is unflipped, position = count * stride). Gated by the caller on
     * an active external shader + shadow batching; without Iris nothing is
     * published and the shadow pass keeps the pure fallback path.
     */
    public static void publishPrebake(InstancedStaticPartRenderer renderer,
                                      FloatBuffer records, int count,
                                      long[] keys, boolean[] posQuatStable, boolean[] posQuatVerified) {
        if (count <= 0 || keys == null || keys.length < count
                || posQuatStable == null || posQuatStable.length < count
                || posQuatVerified == null || posQuatVerified.length < count) {
            return;
        }
        PrebakeSnapshot s = PREBAKES.get(renderer);
        if (s == null) {
            s = new PrebakeSnapshot();
            PREBAKES.put(renderer, s);
        }
        BlockPos anchor = FrameViewState.anchorOrigin();
        int curAnchorX = anchor.getX(), curAnchorY = anchor.getY(), curAnchorZ = anchor.getZ();
        long mutEpoch = renderer.getRecordMutationEpoch();
        long layEpoch = renderer.getSlotLayoutEpoch();
        boolean anchorSame = s.anchorX == curAnchorX && s.anchorY == curAnchorY && s.anchorZ == curAnchorZ;

        if (s.count == count && anchorSame && s.bakedMutationEpoch == mutEpoch && s.records != null) {
            // Records and slot layout are 100% unchanged since last publish!
            // Just refresh the epoch stamp and verified/stable arrays if needed.
            s.epoch = prebakeEpoch;
            if (s.posQuatStable == null || s.posQuatStable.length < count) {
                s.posQuatStable = new boolean[count];
            }
            System.arraycopy(posQuatStable, 0, s.posQuatStable, 0, count);
            if (s.posQuatVerified == null || s.posQuatVerified.length < count) {
                s.posQuatVerified = new boolean[count];
            }
            System.arraycopy(posQuatVerified, 0, s.posQuatVerified, 0, count);
            return;
        }

        int floats = count * FLOATS_PER_INSTANCE;
        s.records = ensurePrebakeCapacity(s.records, floats);
        s.records = shrinkOversizedPrebake(s.records, floats);
        FloatBuffer src = records.duplicate();
        src.position(0);
        src.limit(floats);
        s.records.clear();
        s.records.put(src);
        s.bakedMutationEpoch = mutEpoch;
        if (s.posQuatStable == null || s.posQuatStable.length < count) {
            s.posQuatStable = new boolean[count];
        }
        System.arraycopy(posQuatStable, 0, s.posQuatStable, 0, count);
        if (s.posQuatVerified == null || s.posQuatVerified.length < count) {
            s.posQuatVerified = new boolean[count];
        }
        System.arraycopy(posQuatVerified, 0, s.posQuatVerified, 0, count);
        boolean layoutDirty = s.count != count || s.bakedLayoutEpoch != layEpoch || s.firstSlotByKey.isEmpty();
        s.count = count;
        s.epoch = prebakeEpoch;
        s.anchorX = curAnchorX;
        s.anchorY = curAnchorY;
        s.anchorZ = curAnchorZ;
        if (layoutDirty) {
            s.firstSlotByKey.clear();
            s.runLenByKey.clear();
            int runStart = 0;
            for (int i = 1; i <= count; i++) {
                if (i == count || keys[i] != keys[runStart]) {
                    long k = keys[runStart];
                    if (s.firstSlotByKey.containsKey(k)) {
                        s.runLenByKey.put(k, -1);   // ambiguous layout - fail closed for this machine
                    } else {
                        s.firstSlotByKey.put(k, runStart);
                        s.runLenByKey.put(k, i - runStart);
                    }
                    runStart = i;
                }
            }
            s.bakedLayoutEpoch = layEpoch;
        }
    }

    private static FloatBuffer ensurePrebakeCapacity(FloatBuffer buf, int floats) {
        if (buf != null && buf.capacity() >= floats) {
            return buf;
        }
        int cap = 64 * FLOATS_PER_INSTANCE;
        while (cap < floats) {
            cap <<= 1;
        }
        if (buf != null) {
            MemoryUtil.memFree(buf);
        }
        return MemoryUtil.memAllocFloat(cap);
    }

    /**
     * Shrinks a snapshot that is more than 4x the current need (the
     * {@code shrinkOversizedBuffer} pattern): a renderer that once peaked at a
     * large instance count would otherwise keep the oversized off-heap buffer
     * until destroy. Re-allocates to the power-of-two of the need (not to the
     * bare floor, unlike shrinkOversizedBuffer) so a recurring spike does not
     * grow/shrink-flap every publish. Safe: publishPrebake rewrites
     * {@code [0, floats)} below and every consumer reads absolutely within
     * {@code s.count}.
     */
    private static FloatBuffer shrinkOversizedPrebake(FloatBuffer buf, int floats) {
        if (buf == null || buf.capacity() <= floats * 4) {
            return buf;
        }
        int cap = 64 * FLOATS_PER_INSTANCE;
        while (cap < floats) {
            cap <<= 1;
        }
        if (cap >= buf.capacity()) {
            return buf;
        }
        FloatBuffer fresh = MemoryUtil.memAllocFloat(cap);
        buf.position(0).limit(floats);
        fresh.put(buf);
        fresh.clear();
        MemoryUtil.memFree(buf);
        return fresh;
    }

    /**
     * Decodes the record's packed quat (2 snorm16 words @3..4, see
     * {@link InstancedStaticPartRenderer#packQuatWord}) into {@code out}.
     */
    private static void decodeRecordQuat(FloatBuffer records, int base, Quaternionf out) {
        int wxy = Float.floatToRawIntBits(records.get(base + InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET));
        int wzw = Float.floatToRawIntBits(records.get(base + InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET + 1));
        out.set(InstancedStaticPartRenderer.decodeSnorm16(wxy),
                InstancedStaticPartRenderer.decodeSnorm16(wxy >>> 16),
                InstancedStaticPartRenderer.decodeSnorm16(wzw),
                InstancedStaticPartRenderer.decodeSnorm16(wzw >>> 16));
    }

    /**
     * Freshness gate for one (renderer, machine): the snapshot was published by the
     * last completed main pass and holds at least one contiguous record run for the
     * machine's posKey.
     */
    public static boolean hasFreshPrebake(InstancedStaticPartRenderer renderer, long posKey) {
        PrebakeSnapshot s = PREBAKES.get(renderer);
        if (s == null || s.epoch != prebakeEpoch || s.runLenByKey.get(posKey) <= 0) {
            return false;
        }
        if (anchorSpaceShadowActive && (s.anchorX != shadowAnchorX || s.anchorY != shadowAnchorY || s.anchorZ != shadowAnchorZ)) {
            return false;
        }
        return true;
    }

    /** Whether the shadow basis is calibrated for the CURRENT shadow pass. */
    public static boolean prebakeCalibrated() {
        return prebakeCalibrated && prebakeCalibFrame == renderFrame;
    }

    // ── Shadow-basis calibration (solved from fallback records, per shadow pass) ──
    private static final Matrix3f PREBAKE_R_SHADOW = new Matrix3f();
    private static final Vector3f PREBAKE_T_SHADOW = new Vector3f();
    private static final Quaternionf PREBAKE_Q_SHADOW = new Quaternionf();
    private static final Matrix3f PREBAKE_R_CAND = new Matrix3f();
    private static final Vector3f PREBAKE_T_CAND = new Vector3f();
    private static final Matrix3f PREBAKE_S_ROT = new Matrix3f();
    private static final Matrix3f PREBAKE_REC_ROT = new Matrix3f();
    private static final Quaternionf PREBAKE_Q_TMP = new Quaternionf();
    private static final Quaternionf PREBAKE_Q_OUT = new Quaternionf();
    private static final Vector3f PREBAKE_V = new Vector3f();
    private static final float[] PREBAKE_BBOX6 = new float[6];
    private static final int[] PREBAKE_LIGHT_WORDS = new int[4];
    private static final float[] PREBAKE_UV4 = new float[4];
    private static final float[] PREBAKE_TINT4 = new float[4];
    private static final float[] PREBAKE_GRAD3 = new float[3];
    private static long prebakeCalibFrame = -1L;
    private static boolean prebakeCalibrated = false;
    private static boolean prebakeCalibSawFirst = false;
    private static long prebakeCalibFirstPosKey = Long.MIN_VALUE;

    /**
     * Calibration feed from a fallback/probe record (called by
     * IrisInstancedBatchRenderer.tryRecordShadowInstance BEFORE any machine is
     * being skipped): solves the shadow basis from a posQuat-stable snapshot slot
     * and validates it against an independent second solve.
     *
     * @param lightPose the light-space record pose (MV_shadow * T(bePos - camPos) * S)
     *                  exactly as handed to {@link #record}
     */
    public static void notePrebakeCalibration(InstancedStaticPartRenderer renderer,
                                              BlockPos blockPos, Matrix4f lightPose) {
        if (blockPos == null || prebakeCalibrated || !stashValid) {
            return;
        }
        if (prebakeCalibFrame != renderFrame) {
            // New shadow pass: the previous calibration is invalid.
            prebakeCalibFrame = renderFrame;
            prebakeCalibrated = false;
            prebakeCalibSawFirst = false;
            prebakeCalibFirstPosKey = Long.MIN_VALUE;
        }
        PrebakeSnapshot s = PREBAKES.get(renderer);
        if (s == null || s.epoch != prebakeEpoch) {
            return;
        }
        long posKey = blockPos.asLong();
        // Calibration pairs THIS record call's pose with a snapshot slot of the same
        // (renderer, machine) run - but the call site cannot know which slot of a
        // multi-slot run the record belongs to. Restrict to single-slot runs (the
        // common case: one part renderer = one record per machine), where the pairing
        // is provably exact; multi-slot machines simply do not calibrate.
        if (s.runLenByKey.get(posKey) != 1) {
            return;
        }
        int first = s.firstSlotByKey.get(posKey);
        if (s.posQuatStable[first]) {
            solveShadowBasis(s, first, blockPos, lightPose);
        }
    }

    private static void solveShadowBasis(PrebakeSnapshot s, int slot, BlockPos bePos, Matrix4f lightPose) {
        int base = slot * FLOATS_PER_INSTANCE;
        // lightPose = MV_shadow * T(bePos - camPos) * S; S (the machine part pose,
        // bob-stripped world coordinates) is known from the main-pass record.
        // R_light comes from the shadow record pose, R_S from the snapshot record.
        lightPose.getNormalizedRotation(PREBAKE_Q_OUT);
        PREBAKE_REC_ROT.rotation(PREBAKE_Q_OUT);
        decodeRecordQuat(s.records, base, PREBAKE_Q_TMP);
        PREBAKE_S_ROT.rotation(PREBAKE_Q_TMP);
        PREBAKE_S_ROT.invert();
        // R_shadow = R_light * R_S^-1 (pure rotations - no distance scaling).
        PREBAKE_R_CAND.set(PREBAKE_REC_ROT).mul(PREBAKE_S_ROT);
        // S_t = recPosMain - (bePos - anchor) (the record is anchor-relative to the
        // snapshot's PUBLISH-time anchor - never re-capture mid-shadow-pass).
        float stx = s.records.get(base)     - (bePos.getX() - s.anchorX);
        float sty = s.records.get(base + 1) - (bePos.getY() - s.anchorY);
        float stz = s.records.get(base + 2) - (bePos.getZ() - s.anchorZ);
        float wx = (bePos.getX() - stashCamX) + stx;
        float wy = (bePos.getY() - stashCamY) + sty;
        float wz = (bePos.getZ() - stashCamZ) + stz;
        // t_shadow = T_light - R_shadow * world; use a proper JOML transform.
        // (Manual m<col><row> indexing applied the TRANSPOSED rotation, poisoning
        // the candidate by (R - R^T) * (pos - cam), growing with camera distance.)
        PREBAKE_V.set(wx, wy, wz);
        PREBAKE_R_CAND.transform(PREBAKE_V);
        PREBAKE_T_CAND.set(lightPose.m30() - PREBAKE_V.x, lightPose.m31() - PREBAKE_V.y, lightPose.m32() - PREBAKE_V.z);
        if (!prebakeCalibSawFirst) {
            PREBAKE_R_SHADOW.set(PREBAKE_R_CAND);
            PREBAKE_T_SHADOW.set(PREBAKE_T_CAND);
            prebakeCalibFirstPosKey = bePos.asLong();
            prebakeCalibSawFirst = true;
            return;
        }
        // The two calibration samples must come from DIFFERENT machines: samples
        // sharing a posKey repeat the identical error by construction, so the
        // agreement check proves nothing.
        if (bePos.asLong() == prebakeCalibFirstPosKey) {
            return;
        }
        if (prebakeBasisAgrees(PREBAKE_R_CAND, PREBAKE_T_CAND)) {
            PREBAKE_R_SHADOW.set(PREBAKE_R_CAND);
            PREBAKE_T_SHADOW.set(PREBAKE_T_CAND);
            PREBAKE_Q_SHADOW.setFromNormalized(PREBAKE_R_SHADOW);
            prebakeCalibrated = true;
        } else {
            // Disagreement between independent solves - restart validation
            // (fail-closed: the shadow basis stays unusable until two agree).
            prebakeCalibSawFirst = false;
        }
    }

    private static boolean prebakeBasisAgrees(Matrix3f r, Vector3f t) {
        if (Math.abs(r.m00() - PREBAKE_R_SHADOW.m00()) > 1.0e-3f) return false;
        if (Math.abs(r.m01() - PREBAKE_R_SHADOW.m01()) > 1.0e-3f) return false;
        if (Math.abs(r.m02() - PREBAKE_R_SHADOW.m02()) > 1.0e-3f) return false;
        if (Math.abs(r.m10() - PREBAKE_R_SHADOW.m10()) > 1.0e-3f) return false;
        if (Math.abs(r.m11() - PREBAKE_R_SHADOW.m11()) > 1.0e-3f) return false;
        if (Math.abs(r.m12() - PREBAKE_R_SHADOW.m12()) > 1.0e-3f) return false;
        if (Math.abs(r.m20() - PREBAKE_R_SHADOW.m20()) > 1.0e-3f) return false;
        if (Math.abs(r.m21() - PREBAKE_R_SHADOW.m21()) > 1.0e-3f) return false;
        if (Math.abs(r.m22() - PREBAKE_R_SHADOW.m22()) > 1.0e-3f) return false;
        // t error scales with the machine's distance; two independent solves agree
        // only if the basis is systematically sound (wrong-S candidates fail on R).
        // The t tolerance is deliberately TIGHT (1e-2): the basis is exact, so two
        // honest solves differ by float noise only. A loose tolerance (was 1.0) let
        // a translation-poisoned solve (part pose moved since the snapshot) "agree"
        // with a correct one - oven door travel is 0.75, crucible melt 0.875 - and
        // the locked wrong basis crawled the consumed machines' shadows per pass.
        return Math.abs(t.x - PREBAKE_T_SHADOW.x) <= 1.0e-2f
                && Math.abs(t.y - PREBAKE_T_SHADOW.y) <= 1.0e-2f
                && Math.abs(t.z - PREBAKE_T_SHADOW.z) <= 1.0e-2f;
    }

    /**
     * Converts the machine's snapshot slots of ONE renderer into light-space records
     * and appends them to the regular shadow ENTRIES (single writer
     * {@link #writeRecordFloats}; same 32-float record layout, ZERO light words like
     * the fallback - no per-shadow-frame light re-sampling).
     * Precondition: {@link #hasFreshPrebake} + {@link #prebakeCalibrated()} passed
     * for this renderer (the caller verified every part before the first consume).
     * <p>
     * ATOMIC: the append is all-or-nothing per renderer — a mid-loop failure rolls
     * this entry back so the caller's fallback BER re-records the machine from
     * scratch instead of doubling the already-appended records.
     */
    public static boolean consumePrebaked(InstancedStaticPartRenderer renderer, long posKey, BlockPos bePos) {
        if (!batchingEnabled || !stashValid || !prebakeCalibrated()) {
            return false;
        }
        PrebakeSnapshot s = PREBAKES.get(renderer);
        if (s == null || s.epoch != prebakeEpoch || s.records == null) {
            return false;
        }
        int len = s.runLenByKey.get(posKey);
        if (len <= 0) {
            return false;
        }
        int first = s.firstSlotByKey.get(posKey);
        if (first < 0 || first + len > s.count) {
            return false;
        }
        Entry e = BY_RENDERER.get(renderer);
        if (e == null) {
            e = new Entry(renderer);
            BY_RENDERER.put(renderer, e);
        }
        int rollbackCount = e.count;
        boolean rollbackFromEntries = e.count == 0;
        if (e.count == 0) {
            shrinkOversizedBuffer(e);
            ENTRIES.add(e);
        }
        noteShadowRecord();
        // Snapshot-relative machine offset: the records are relative to the
        // publish-time anchor stored IN the snapshot (a mid-shadow-pass
        // FrameViewState.anchorOrigin() re-capture is exactly the drift/UAF
        // hazard this snapshot anchor removes).
        float ax = bePos.getX() - s.anchorX;
        float ay = bePos.getY() - s.anchorY;
        float az = bePos.getZ() - s.anchorZ;
        // The machine's camera-relative offset, rotated into light space. The
        // calibration solve STRIPS this term from the shadow-MV translation
        // (t_shadow = recTr - R_shadow*((be-cam) + S_t) = the TRUE MV translation),
        // so it must be re-added here: the fallback record is
        // trans(MV_shadow * T(be-cam) * S) + cam = R_shadow*(S_t + (be-cam))
        //                                        + t_shadow + cam.
        float mx = bePos.getX() - stashCamX;
        float my = bePos.getY() - stashCamY;
        float mz = bePos.getZ() - stashCamZ;
        // All-or-nothing append: the caller consumes one machine across SEVERAL
        // renderers and runs the fallback shadow BER on a false return, so a partial
        // append here would be re-recorded by the fallback (doubled shadow records).
        // Every per-renderer precondition is checked before the loop, but the
        // atomicity is enforced, not assumed: any mid-loop failure (e.g. an
        // allocation throw in the buffer growth below) rolls the entry back so the
        // fallback re-records the machine from scratch.
        try {
            appendPrebakedRange(e, s, first, len, ax, ay, az, mx, my, mz);
        } catch (Throwable t) {
            e.count = rollbackCount;
            if (rollbackFromEntries) {
                ENTRIES.remove(e);
            }
            throw t;
        }
        return true;
    }

    /** Append loop body of {@link #consumePrebaked} (atomicity handled by the caller). */
    private static void appendPrebakedRange(Entry e, PrebakeSnapshot s, int first, int len,
                                            float ax, float ay, float az, float mx, float my, float mz) {
        int neededFloats = (e.count + len) * FLOATS_PER_INSTANCE;
        if (neededFloats > e.data.capacity()) {
            int newCap = e.data.capacity() * 2;
            while (newCap < neededFloats) newCap *= 2;
            FloatBuffer grown = MemoryUtil.memAllocFloat(newCap);
            e.data.flip();
            grown.put(e.data);
            MemoryUtil.memFree(e.data);
            e.data = grown;
        }
        long srcBaseAddr = MemoryUtil.memAddress0(s.records);
        long dstBaseAddr = MemoryUtil.memAddress0(e.data);
        float offX = mx - ax; // = s.anchorX - stashCamX
        float offY = my - ay; // = s.anchorY - stashCamY
        float offZ = mz - az; // = s.anchorZ - stashCamZ
        float addX = PREBAKE_T_SHADOW.x + stashCamX;
        float addY = PREBAKE_T_SHADOW.y + stashCamY;
        float addZ = PREBAKE_T_SHADOW.z + stashCamZ;

        int written = 0;
        for (int i = first; i < first + len; i++) {
            long srcAddr = srcBaseAddr + (long) i * FLOATS_PER_INSTANCE * 4L;
            if (MemoryUtil.memGetFloat(srcAddr + (long) InstancedStaticPartRenderer.FADE_FLOAT_OFFSET * 4L) < 0.0f) {
                continue;
            }
            long dstAddr = dstBaseAddr + (long) (e.count + written) * FLOATS_PER_INSTANCE * 4L;
            // Bulk copy all 128 bytes (32 floats) of the record (copies bboxMin, bboxSize, uvRect, tint, grad.xyz)
            MemoryUtil.memCopy(srcAddr, dstAddr, (long) FLOATS_PER_INSTANCE * 4L);

            // Transform position into light space
            PREBAKE_V.set(MemoryUtil.memGetFloat(srcAddr) + offX,
                          MemoryUtil.memGetFloat(srcAddr + 4L) + offY,
                          MemoryUtil.memGetFloat(srcAddr + 8L) + offZ);
            PREBAKE_R_SHADOW.transform(PREBAKE_V);
            MemoryUtil.memPutFloat(dstAddr, PREBAKE_V.x + addX);
            MemoryUtil.memPutFloat(dstAddr + 4L, PREBAKE_V.y + addY);
            MemoryUtil.memPutFloat(dstAddr + 8L, PREBAKE_V.z + addZ);

            // Transform packed quaternion into light space
            int wxy = MemoryUtil.memGetInt(srcAddr + (long) InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET * 4L);
            int wzw = MemoryUtil.memGetInt(srcAddr + (long) (InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET + 1) * 4L);
            PREBAKE_Q_TMP.set(InstancedStaticPartRenderer.decodeSnorm16(wxy),
                              InstancedStaticPartRenderer.decodeSnorm16(wxy >>> 16),
                              InstancedStaticPartRenderer.decodeSnorm16(wzw),
                              InstancedStaticPartRenderer.decodeSnorm16(wzw >>> 16));
            PREBAKE_Q_SHADOW.mul(PREBAKE_Q_TMP, PREBAKE_Q_OUT);
            MemoryUtil.memPutInt(dstAddr + 12L, InstancedStaticPartRenderer.packQuatWord(PREBAKE_Q_OUT.x, PREBAKE_Q_OUT.y));
            MemoryUtil.memPutInt(dstAddr + 16L, InstancedStaticPartRenderer.packQuatWord(PREBAKE_Q_OUT.z, PREBAKE_Q_OUT.w));

            // Float 11: fade = 1.0f; Floats 12..15: light = 0; Floats 27..31: grad.w = -1.0f, animParams = 0
            MemoryUtil.memPutFloat(dstAddr + 11L * 4L, 1.0f);
            MemoryUtil.memPutLong(dstAddr + 12L * 4L, 0L);
            MemoryUtil.memPutLong(dstAddr + 14L * 4L, 0L);
            MemoryUtil.memPutFloat(dstAddr + 27L * 4L, -1.0f);
            MemoryUtil.memPutLong(dstAddr + 28L * 4L, 0L);
            MemoryUtil.memPutLong(dstAddr + 30L * 4L, 0L);
            written++;
        }
        if (written > 0) {
            e.count += written;
            e.data.position(e.count * FLOATS_PER_INSTANCE);
            com.hbm_m.client.render.NucleusDebug.recordShadowRecord(written);
        }
    }

    /**
     * Flush at the end of shadow block entity rendering pass (invoked from Iris mixin).
     * <p>
     * <b>Primary path — Tier 1 (GPU Compute Bake)</b>: Bakes multiblock geometry in VRAM and draws with
     * the active pack shadow shader program.
     * <p>
     * <b>Secondary path — Tier 2 (Instanced Shadow)</b>: Uses {@code IrisInstancedShaders} when distortion
     * is compatible or absent.
     * <p>
     * <b>Fallback — Tier 3</b>: Per-record {@code drawCompanion} via {@link IrisRenderBatch}.
     */
    /** ALLOCATED-side accessors for cross-renderer consume atomicity: pre-consume entry count (-1 = no entry). */
    public static int entryCountFor(InstancedStaticPartRenderer renderer) {
        Entry e = BY_RENDERER.get(renderer);
        return e == null ? -1 : e.count;
    }

    /** Cross-renderer rollback: restores the entry count captured by {@link #entryCountFor}; -1 removes the entry. */
    public static void truncateEntryTo(InstancedStaticPartRenderer renderer, int count) {
        Entry e = BY_RENDERER.get(renderer);
        if (e == null) {
            return;
        }
        if (count < 0) {
            e.count = 0;
            ENTRIES.remove(e);
        } else {
            e.count = count;
        }
    }

    public static void flushGlobalShadowBatch() {
        if (ENTRIES.isEmpty()) {
            // Same invalidation duties as the finally block below: without this, a
            // pass whose records all went through other flush paths would leave
            // stashValid/prebakeCalibrated claiming validity into the next pass.
            // Today the renderFrame bump in noteShadowRecord makes the stale basis
            // unusable anyway (fail-closed by accident); this makes it explicit.
            clearStash();
            return;
        }
        com.hbm_m.client.render.debug.GlDebugMarkers.pushGroup("HBM_GlobalShadow_Flush");
        try {
            if (!stashValid) {
                disableBatching("no shadow matrices stashed at record time");
                return;
            }
            if (IrisRenderBatch.active() != null) {
                disableBatching("leaked render batch is active at shadow flush");
                return;
            }
            Matrix4f shadowProj = new Matrix4f(stashProj);

            ShaderInstance packShadow = IrisExtendedShaderAccess.getBlockShader(true);
            if (packShadow == null) {
                disableBatching("shadow pack shader unavailable");
                return;
            }
            if (!IrisShaderApply.tryApply(packShadow)) {
                disableBatching("shadow pack shader apply failed");
                return;
            }

            // ── Tier 1: GPU Compute Bake ───────────────────────────────
            // Bakes geometry directly on GPU into VRAM; shaderpack applies native distortion.
            // Records are light-space (shadow poseStack decomposition, camera offset in
            // the translation): the bake output stays camera-independent (cacheable),
            // the camera is folded out via ModelViewMat = T(-cam) at draw time.
            if (NucleusGpuBaker.isEnabled()) {
                boolean baked = false;
                try (IrisPhaseGuard guard = IrisPhaseGuard.pushBlockEntities()) {
                    baked = NucleusGpuBaker.bakeAndDrawShadow(ENTRIES, packShadow, shadowProj,
                            stashCamX, stashCamY, stashCamZ);
                }
                if (baked) {
                    NucleusDebug.recordDraw(1, totalInstances(), "GPU bake shadow flush");
                    NucleusDebug.recordShadowFlush("GPU bake", ENTRIES.size(), totalInstances());
                    return;
                }
                NucleusDebug.recordShadowFlush("bake skipped", 0, 0);
            }

            com.hbm_m.client.render.shader.IrisInstancedShaders.captureLiveFramebuffer(true);
            ShaderInstance ours = com.hbm_m.client.render.shader.IrisInstancedShaders.getOrCreate(true);

            int drawCalls = 0;
            int total = 0;
            boolean instancedSafe = com.hbm_m.config.ModClothConfig.get().nucleusShadowInstancing
                    && SHADOW_INSTANCING_FLAG
                    && com.hbm_m.client.render.shader.IrisShadowDistortion.isCompatible();
            if (ours != null && instancedSafe) {
                try (IrisPhaseGuard guard = IrisPhaseGuard.pushBlockEntities()) {
                    for (int i = 0; i < ENTRIES.size(); i++) {
                        Entry e = ENTRIES.get(i);
                        if (e.count <= 0 || e.renderer == null) {
                            continue;
                        }
                        if (e.renderer.drawShadowInstances(e.activeBuffer(), e.count, ours, shadowProj,
                                stashCamX, stashCamY, stashCamZ)) {
                            drawCalls++;
                            total += e.count;
                        }
                    }
                    // Restore pack program to keep Iris state tracking consistent
                    GlStateManager._glUseProgram(packShadow.getId());
                }
                NucleusDebug.recordDraw(drawCalls, total, "Iris shadow instanced");
                NucleusDebug.recordShadowFlush("instanced", drawCalls, total);
            } else {
                // Tier 3 Fallback: per-record drawCompanion via IrisRenderBatch
                int calls = drawEntriesViaCompanionBatch(ENTRIES, shadowProj,
                        stashCamX, stashCamY, stashCamZ);
                if (calls < 0) {
                    disableBatching("shadow pack shader unavailable");
                    return;
                }
                drawCalls = calls;
                NucleusDebug.recordDraw(drawCalls, totalInstances(), "Iris shadow batch");
                NucleusDebug.recordShadowFlush("companion", drawCalls, totalInstances());
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M] Iris shadow batch flush failed", t);
        } finally {
            com.hbm_m.client.render.debug.GlDebugMarkers.popGroup();
            clearEntries();
            clearStash();
        }
    }

    /**
     * Tier-3 companion batch walk over the given entries (per-record
     * drawCompanion via {@link IrisRenderBatch#begin(boolean, Matrix4f)}).
     * Returns the draw call count, or -1 when the batch could not be opened
     * (the caller decides whether that disables shadow batching).
     */
    private static int drawEntriesViaCompanionBatch(List<Entry> entries, Matrix4f shadowProj,
                                                    float camX, float camY, float camZ) {
        int drawCalls = 0;
        try (IrisRenderBatch batch = IrisRenderBatch.begin(true, shadowProj)) {
            if (batch == null) {
                return -1;
            }
            final Matrix4f recordPose = new Matrix4f();
            final Vector3f pos = new Vector3f();
            final Quaternionf rot = new Quaternionf();
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                if (e.count <= 0) {
                    continue;
                }
                IrisCompanionMesh companion = e.getCompanionMesh();
                if (companion == null) {
                    continue;
                }
                FloatBuffer buf = e.activeBuffer();
                for (int r = 0; r < e.count; r++) {
                    int base = r * FLOATS_PER_INSTANCE;
                    if (anchorSpaceShadowActive && stashShadowMvValid) {
                        pos.set(buf.get(base + 0),
                                buf.get(base + 1),
                                buf.get(base + 2));
                        decodeRecordQuat(buf, base, rot);
                        buildShadowModelView(recordPose, camX, camY, camZ);
                        recordPose.translate(pos).rotate(rot);
                    } else {
                        pos.set(buf.get(base + 0) - camX,
                                buf.get(base + 1) - camY,
                                buf.get(base + 2) - camZ);
                        decodeRecordQuat(buf, base, rot);
                        recordPose.translationRotate(pos, rot);
                    }
                    batch.drawCompanion(companion, recordPose, 0);
                    drawCalls++;
                }
            }
        }
        return drawCalls;
    }

    /**
     * 2026-10-07 shadow-fix: Tier-3 re-draw of the entries the Tier-1 shadow
     * bake refused this frame (bakeEntry false - a cache-budget admission
     * refusal, or an upload/expansion failure). Called
     * from NucleusGpuBaker.bakeAndDrawShadow AFTER its pool flush; only entries
     * the bake reported as failed arrive here, so nothing can double-draw.
     * Runs the same walk as flushGlobalShadowBatch's Tier-3 branch (absolute
     * record reads - ENTRIES still hold this frame's data, they are drained
     * only after the flush returns).
     */
    static void redrawFailedBakeEntries(List<Entry> failed, Matrix4f shadowProj,
                                        float camX, float camY, float camZ) {
        if (failed == null || failed.isEmpty() || !stashValid) {
            return;
        }
        com.hbm_m.client.render.shader.IrisInstancedShaders.captureLiveFramebuffer(true);
        ShaderInstance ours = com.hbm_m.client.render.shader.IrisInstancedShaders.getOrCreate(true);
        boolean instancedSafe = com.hbm_m.config.ModClothConfig.get().nucleusShadowInstancing
                && SHADOW_INSTANCING_FLAG
                && com.hbm_m.client.render.shader.IrisShadowDistortion.isCompatible();

        List<Entry> companionFallback = null;
        if (ours != null && instancedSafe) {
            ShaderInstance packShadow = IrisExtendedShaderAccess.getBlockShader(true);
            int drawCalls = 0;
            int instancedTotal = 0;
            try (IrisPhaseGuard guard = IrisPhaseGuard.pushBlockEntities()) {
                for (int i = 0; i < failed.size(); i++) {
                    Entry e = failed.get(i);
                    if (e.count <= 0) continue;
                    if (e.renderer != null && e.renderer.drawShadowInstances(
                            e.activeBuffer(), e.count, ours, shadowProj, camX, camY, camZ)) {
                        drawCalls++;
                        instancedTotal += e.count;
                    } else {
                        if (companionFallback == null) companionFallback = new java.util.ArrayList<>();
                        companionFallback.add(e);
                    }
                }
                if (packShadow != null) {
                    GlStateManager._glUseProgram(packShadow.getId());
                }
            }
            if (drawCalls > 0) {
                NucleusDebug.recordDraw(drawCalls, instancedTotal, "Iris shadow bake-fallback-instanced");
                NucleusDebug.recordShadowFlush("bake-fallback-instanced", drawCalls, instancedTotal);
            }
            if (companionFallback == null || companionFallback.isEmpty()) {
                return;
            }
        } else {
            companionFallback = failed;
        }

        int calls = drawEntriesViaCompanionBatch(companionFallback, shadowProj, camX, camY, camZ);
        if (calls >= 0) {
            int instances = 0;
            for (int i = 0; i < companionFallback.size(); i++) {
                instances += companionFallback.get(i).count;
            }
            NucleusDebug.recordDraw(calls, instances, "Iris shadow bake-fallback");
            NucleusDebug.recordShadowFlush("bake-fallback", calls, instances);
        }
    }

    private static int totalInstances() {
        int total = 0;
        for (int i = 0; i < ENTRIES.size(); i++) {
            total += ENTRIES.get(i).count;
        }
        return total;
    }

    private static void disableBatching(String reason) {
        clearEntries();
        clearStash();
        NucleusDebug.recordShadowFlush("off", 0, 0);
        if (batchingEnabled) {
            batchingEnabled = false;
            disabledAtGeneration = IrisExtendedShaderAccess.getPipelineGeneration();
            disabledAtMs = System.currentTimeMillis();
            MainRegistry.LOGGER.warn(
                    "[HBM-M] Iris shadow batch disabled ({}): {} - machines fall back "
                            + "to per-BE immediate shadow draws until the pipeline is rebuilt",
                    reason,
                    "records discarded for this frame");
        }
    }

    /**
     * Bounds off-heap retention (audit 2026-10-07 L3a): record buffers double on demand
     * and an entry used to keep its session high-water mark for as long as the owning
     * renderer/mesh lives (the LRU sweep only frees dynamic holders). Runs ONLY at
     * re-activation (count == 0, buffer drained by clearEntries), never per-frame in
     * clearEntries(): a renderer that legitimately needs a large batch re-grows by
     * doubling on the next appends instead of thrashing reallocs every frame.
     */
    private static void shrinkOversizedBuffer(Entry e) {
        if (e.data.capacity() > 4 * GROW_INSTANCES * FLOATS_PER_INSTANCE) {
            FloatBuffer fresh = MemoryUtil.memAllocFloat(GROW_INSTANCES * FLOATS_PER_INSTANCE);
            MemoryUtil.memFree(e.data);
            e.data = fresh;
        }
    }

    /** Resets per-frame entry counts without freeing native off-heap memory. */
    private static void clearEntries() {
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry e = ENTRIES.get(i);
            e.count = 0;
            e.prebakeDirectView = null;
            e.mutationEpoch = -1L;
            if (e.data != null) {
                e.data.clear();
            }
        }
        ENTRIES.clear();
        stashValid = false;
    }

    /** Eviction hook: called when a part renderer is destroyed. */
    public static void onRendererDestroyed(InstancedStaticPartRenderer renderer) {
        PrebakeSnapshot ps = PREBAKES.remove(renderer);
        if (ps != null && ps.records != null) {
            MemoryUtil.memFree(ps.records);
            ps.records = null;
        }
        Entry e = BY_RENDERER.remove(renderer);
        if (e != null && e.data != null) {
            MemoryUtil.memFree(e.data);
            e.data = null;
        }
    }

    /** Eviction hook: called when a companion mesh is destroyed. */
    public static void onMeshDestroyed(IrisCompanionMesh mesh) {
        Entry e = BY_MESH.remove(mesh);
        if (e != null && e.data != null) {
            MemoryUtil.memFree(e.data);
            e.data = null;
        }
    }

    public static void onRenderOriginChanged() {
        for (PrebakeSnapshot ps : PREBAKES.values()) {
            if (ps != null && ps.records != null) {
                MemoryUtil.memFree(ps.records);
                ps.records = null;
            }
        }
        PREBAKES.clear();
        prebakeCalibrated = false;
        prebakeCalibSawFirst = false;
        prebakeCalibFrame = -1L;
    }

    /** Cleanly releases all persistent off-heap shadow memory buffers (e.g. on client level unload). */
    public static void resetAllMemory() {
        for (PrebakeSnapshot ps : PREBAKES.values()) {
            if (ps != null && ps.records != null) {
                MemoryUtil.memFree(ps.records);
                ps.records = null;
            }
        }
        PREBAKES.clear();
        prebakeCalibrated = false;
        prebakeCalibSawFirst = false;
        prebakeCalibFrame = -1L;
        for (Entry e : BY_RENDERER.values()) {
            if (e != null && e.data != null) {
                MemoryUtil.memFree(e.data);
                e.data = null;
            }
        }
        BY_RENDERER.clear();
        for (Entry e : BY_MESH.values()) {
            if (e != null && e.data != null) {
                MemoryUtil.memFree(e.data);
                e.data = null;
            }
        }
        BY_MESH.clear();
        ENTRIES.clear();
        stashValid = false;
        // Audit 2026-10-07 #14: clear the F3 "Shadow:" stats here as well. This
        // is a registered RenderCacheManager hook (pack flip, F3+T, world
        // unload), and with the pack disabled renderShadows never runs, so the
        // per-frame mixin reset cannot fire and the line would stick forever.
        NucleusDebug.resetShadowStats();
    }

    /**
     * Start of main render pass (shadow pass completed).
     * If un-flushed records remain, the mixin failed to fire; after 2 consecutive frames,
     * disables shadow batching and falls back to immediate rendering.
     */
    public static void onMainPassFrameStart() {
        noteMainFrameStart();
        // SHADOW-EPOCH: snapshots published by the previous main pass stay fresh
        // until this bump; the next shadow pass consumes them against THIS epoch.
        prebakeEpoch++;
        com.hbm_m.client.render.shader.ShaderCompatibilityDetector.setRenderingShadowPass(false);
        if (!ENTRIES.isEmpty()) {
            clearEntries();
            staleFrames++;
            if (batchingEnabled && staleFrames >= 2) {
                batchingEnabled = false;
                disabledAtGeneration = IrisExtendedShaderAccess.getPipelineGeneration();
                // Stamp the retry latch here too: without it a stale disabledAtMs from
                // an earlier disableBatching (> DISABLE_RETRY_MS old) would make the
                // re-enable gate in record()/recordCustomMesh() re-arm immediately,
                // and with the flush mixin still missing the path would cycle
                // (collect 2 stale frames -> drop -> disable) every retry window.
                disabledAtMs = System.currentTimeMillis();
                if (!disabledLogged) {
                    disabledLogged = true;
                    MainRegistry.LOGGER.warn(
                            "[HBM-M] Iris shadow BE flush hook never fired (mixin not applied?) - "
                                    + "falling back to per-BE immediate shadow draws");
                }
            }
        } else {
            staleFrames = 0;
        }
        var prebakeIt = PREBAKES.entrySet().iterator();
        while (prebakeIt.hasNext()) {
            PrebakeSnapshot ps = prebakeIt.next().getValue();
            if (ps != null && prebakeEpoch - ps.epoch > 24L) {
                if (ps.records != null) {
                    MemoryUtil.memFree(ps.records);
                    ps.records = null;
                }
                prebakeIt.remove();
            }
        }
        for (Entry e : BY_RENDERER.values()) {
            if (e != null && e.count == 0 && e.data != null) {
                shrinkOversizedBuffer(e);
            }
        }
    }
}
