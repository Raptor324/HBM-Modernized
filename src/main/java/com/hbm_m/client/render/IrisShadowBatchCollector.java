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

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.IrisShaderApply;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.renderer.ShaderInstance;

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

    // Shadow projection stashed at recording time (RenderSystem state of shadow pass).
    private static final Matrix4f stashProj = new Matrix4f();
    private static boolean stashValid = false;

    public static void stashShadowMatrices(Matrix4f proj) {
        stashProj.set(proj);
        stashValid = true;
    }

    private static void clearStash() {
        stashValid = false;
    }

    /**
     * Universal part instance recording: 12-float 3x4 affine transform matrix
     * supporting translation, rotation, and non-uniform scaling.
     */
    public static void record(InstancedStaticPartRenderer renderer, Matrix4f fullTransform,
                              float[] bboxMin, float[] cornerUV16) {
        if (IrisExtendedShaderAccess.getPipelineGeneration() != disabledAtGeneration
                && !batchingEnabled) {
            batchingEnabled = true;
        }
        noteShadowRecord();
        Entry e = BY_RENDERER.get(renderer);
        if (e == null) {
            e = new Entry(renderer);
            ENTRIES.add(e);
            BY_RENDERER.put(renderer, e);
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
        if (IrisExtendedShaderAccess.getPipelineGeneration() != disabledAtGeneration
                && !batchingEnabled) {
            batchingEnabled = true;
        }
        noteShadowRecord();
        Entry e = BY_MESH.get(companion);
        if (e == null) {
            e = new Entry(companion);
            ENTRIES.add(e);
            BY_MESH.put(companion, e);
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

    private static void appendRecord(Entry e, InstancedStaticPartRenderer renderer, Matrix4f fullTransform,
                                     float[] bboxMin, float[] cornerUV16) {
        if ((e.count + 1) * FLOATS_PER_INSTANCE > e.data.capacity()) {
            FloatBuffer grown = MemoryUtil.memAllocFloat(e.data.capacity() * 2);
            e.data.flip();
            grown.put(e.data);
            MemoryUtil.memFree(e.data);
            e.data = grown;
        }
        FloatBuffer d = e.data;
        // 12-float 3x4 affine matrix (row-major: row0, row1, row2)
        // Row 0: m00, m10, m20, tx
        d.put(fullTransform.m00()).put(fullTransform.m10()).put(fullTransform.m20()).put(fullTransform.m30());
        // Row 1: m01, m11, m21, ty
        d.put(fullTransform.m01()).put(fullTransform.m11()).put(fullTransform.m21()).put(fullTransform.m31());
        // Row 2: m02, m12, m22, tz
        d.put(fullTransform.m02()).put(fullTransform.m12()).put(fullTransform.m22()).put(fullTransform.m32());
        d.put(0.0f); // Float 12: bone id / extra
        d.put(1.0f); // Float 13: fade alpha (1.0)
        // Floats 14..29 (16 floats): corner light / padding
        if (cornerUV16 != null && cornerUV16.length >= 16) {
            for (int i = 0; i < 16; i++) {
                d.put(cornerUV16[i]);
            }
        } else {
            for (int i = 0; i < 16; i++) {
                d.put(0.0f);
            }
        }
        // Floats 30..33: InstUvRect (identity for regular machines; the layout must
        // match InstancedStaticPartRenderer.INSTANCE_DATA_SIZE).
        float[] uvRect = (renderer != null) ? renderer.getActiveUvRect() : null;
        if (uvRect != null && uvRect.length >= 4) {
            d.put(uvRect[0]).put(uvRect[1]).put(uvRect[2]).put(uvRect[3]);
        } else {
            d.put(0.0f).put(0.0f).put(1.0f).put(1.0f);
        }
        // Floats 34..37: InstColor (the shadow FB does not use color; only the record
        // stride alignment with the VAO-read instance layout matters).
        float[] tint = (renderer != null) ? renderer.getActiveTint() : null;
        if (tint != null && tint.length >= 4) {
            d.put(tint[0]).put(tint[1]).put(tint[2]).put(tint[3]);
        } else {
            d.put(1.0f).put(1.0f).put(1.0f).put(1.0f);
        }
        // Floats 38..41: GradParams (axis<0 = off; stride alignment as with InstColor).
        float[] grad = (renderer != null) ? renderer.getActiveGrad() : null;
        if (grad != null && grad.length >= 4) {
            d.put(grad[0]).put(grad[1]).put(grad[2]).put(grad[3]);
        } else {
            d.put(-1.0f).put(0.0f).put(0.0f).put(0.0f);
        }
        // Floats 42..45: AnimParams (w = -1 = no parametric joint). The Iris shadow path
        // animates on the CPU (MachineBer.isParametricPath is disabled there), so the joint
        // motion is already baked into fullTransform; the stride must still match the
        // VAO-read instance layout (46 floats), or Tier 1/2 uploads and the Tier 3
        // record walk run past the buffer.
        d.put(0.0f).put(0.0f).put(0.0f).put(-1.0f);
        e.count++;
        com.hbm_m.client.render.NucleusDebug.recordShadowRecord(1);
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
    public static void flushGlobalShadowBatch() {
        if (ENTRIES.isEmpty()) {
            return;
        }
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
            if (NucleusGpuBaker.isEnabled()) {
                boolean baked = false;
                try (IrisPhaseGuard guard = IrisPhaseGuard.pushBlockEntities()) {
                    baked = NucleusGpuBaker.bakeAndDrawShadow(ENTRIES, packShadow, shadowProj);
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
                        if (e.renderer.drawShadowInstances(e.data, e.count, ours, shadowProj)) {
                            drawCalls++;
                            total += e.count;
                        }
                    }
                    // Restore pack program to keep Iris state tracking consistent
                    GL20.glUseProgram(packShadow.getId());
                }
                NucleusDebug.recordDraw(drawCalls, total, "Iris shadow instanced");
                NucleusDebug.recordShadowFlush("instanced", drawCalls, total);
            } else {
                // Tier 3 Fallback: per-record drawCompanion via IrisRenderBatch
                try (IrisRenderBatch batch = IrisRenderBatch.begin(true, shadowProj)) {
                    if (batch == null) {
                        disableBatching("shadow pack shader unavailable");
                        return;
                    }
                    final Matrix4f recordPose = new Matrix4f();
                    for (int i = 0; i < ENTRIES.size(); i++) {
                        Entry e = ENTRIES.get(i);
                        if (e.count <= 0) {
                            continue;
                        }
                        IrisCompanionMesh companion = e.getCompanionMesh();
                        if (companion == null) {
                            continue;
                        }
                        for (int r = 0; r < e.count; r++) {
                            int base = r * FLOATS_PER_INSTANCE;
                            float m00 = e.data.get(base + 0);
                            float m10 = e.data.get(base + 1);
                            float m20 = e.data.get(base + 2);
                            float tx  = e.data.get(base + 3);

                            float m01 = e.data.get(base + 4);
                            float m11 = e.data.get(base + 5);
                            float m21 = e.data.get(base + 6);
                            float ty  = e.data.get(base + 7);

                            float m02 = e.data.get(base + 8);
                            float m12 = e.data.get(base + 9);
                            float m22 = e.data.get(base + 10);
                            float tz  = e.data.get(base + 11);

                            // Reconstruct full affine transform matrix
                            recordPose.set(
                                m00, m01, m02, 0.0f,
                                m10, m11, m12, 0.0f,
                                m20, m21, m22, 0.0f,
                                tx,  ty,  tz,  1.0f
                            );
                            batch.drawCompanion(companion, recordPose, 0);
                            drawCalls++;
                        }
                    }
                    NucleusDebug.recordDraw(drawCalls, totalInstances(), "Iris shadow batch");
                    NucleusDebug.recordShadowFlush("companion", drawCalls, totalInstances());
                }
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M] Iris shadow batch flush failed", t);
        } finally {
            clearEntries();
            clearStash();
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
            MainRegistry.LOGGER.warn(
                    "[HBM-M] Iris shadow batch disabled ({}): {} - machines fall back "
                            + "to per-BE immediate shadow draws until the pipeline is rebuilt",
                    reason,
                    "records discarded for this frame");
        }
    }

    /** Cleanly releases native record memory buffers. */
    private static void clearEntries() {
        for (int i = 0; i < ENTRIES.size(); i++) {
            MemoryUtil.memFree(ENTRIES.get(i).data);
            ENTRIES.get(i).data = null;
        }
        ENTRIES.clear();
        BY_RENDERER.clear();
        BY_MESH.clear();
        stashValid = false;
    }

    /**
     * Start of main render pass (shadow pass completed).
     * If un-flushed records remain, the mixin failed to fire; after 2 consecutive frames,
     * disables shadow batching and falls back to immediate rendering.
     */
    public static void onMainPassFrameStart() {
        noteMainFrameStart();
        if (!ENTRIES.isEmpty()) {
            clearEntries();
            staleFrames++;
            if (batchingEnabled && staleFrames >= 2) {
                batchingEnabled = false;
                disabledAtGeneration = IrisExtendedShaderAccess.getPipelineGeneration();
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
    }
}
