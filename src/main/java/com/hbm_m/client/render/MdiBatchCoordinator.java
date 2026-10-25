package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

import com.hbm_m.client.render.shader.ModShaders;
import org.joml.Matrix4f;
import org.lwjgl.opengl.ARBDrawIndirect;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.culling.GpuCullingCapability;
import com.hbm_m.client.render.culling.HiZDepthPyramid;
import com.hbm_m.client.render.culling.NucleusGpuCuller;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * Optional Multi-Draw Indirect aggregation path for {@link InstancedStaticPartRenderer}.
 * <p>
 * Today each {@code InstancedStaticPartRenderer.flushBatchVanilla} issues one
 * {@code glDrawElementsInstanced} call. A typical machine BER drives 8-11 part
 * renderers, so the per-frame draw call count on the vanilla path scales as
 * {@code partsPerMachine}. When all part renderers share the same vertex
 * format (pos vec3 / normal vec3 / uv vec2, stride 32) AND the same legacy
 * (unsliced) per-instance layout (loc 3..11), we can collapse those into
 * <b>one</b> indirect-command buffer + either a loop of {@code glDrawElementsIndirect},
 * or (optionally) a single {@code glMultiDrawElementsIndirect} — sharing a single atlas
 * VAO and a unified per-instance VBO.
 * <p>
 * <b>Fallback chain (in order):</b>
 * <ol>
 *   <li>{@code hasDrawIndirect && hasBaseInstance} (GL 4.0+ draw indirect + base instance in command):
 *       atlas path; one {@code glMultiDrawElementsIndirect} per flush.</li>
 *   <li>Otherwise: legacy per-renderer {@code glDrawElementsInstanced}
 *       (the {@code flushBatchVanilla} path stays as-is).</li>
 * </ol>
 * <p>
 * <b>Eligibility constraints for an individual flush:</b>
 * <ul>
 *   <li>Iris/Oculus NOT active (we only optimise the vanilla shader path).</li>
 *   <li>Renderer initialised, instanceCount &gt; 0.</li>
 * </ul>
 * <p>
 * <b>Fade ordering (variant G):</b> each renderer window is one sub-draw of a single
 * {@code glMultiDrawElementsIndirect}; fading alpha is resolved by the GPU fade ramp
 * (or per-fragment dither discard with {@code nucleusDitherFade}) and gated by the
 * cached window minimum, so the coordinator performs no CPU-side opaque/fading
 * partition and no fading-tail sort.
 * <p>
 * <b>Iris path:</b> untouched. {@link InstancedStaticPartRenderer#flush(Matrix4f)}
 * still routes to {@code flushBatchIris} when an external shader is active —
 * the coordinator's eligibility test rejects those flushes up front.
 *
 * @credit Flywheel / janh
 */

@OnlyIn(Dist.CLIENT)

public final class MdiBatchCoordinator {

    /**
     * Payload of {@code DrawElementsIndirectCommand} (5×uint32). In the buffer each command
     * is aligned to {@link #INDIRECT_CMD_STRIDE_BYTES}: otherwise certain drivers break multi/indirect fetching.
     */
    static final int INDIRECT_CMD_PACKED_BYTES = 20;
    /** Stride between commands in GL_DRAW_INDIRECT_BUFFER (multiple of 4, >= 20). */
    static final int INDIRECT_CMD_STRIDE_BYTES = 32;

    private static volatile boolean capsResolved = false;
    /** Whether {@code glDrawElementsIndirect} and/or {@code glMultiDrawElementsIndirect} (or ARB equivalents) are supported. */
    private static volatile boolean hasDrawIndirect = false;
    private static volatile boolean hasBaseInstance = false;
    private static volatile boolean loggedOnce = false;

    private static final ThreadLocal<MdiBatchCoordinator> ACTIVE = new ThreadLocal<>();

    private static long lastGpuCullLogTimeMs = 0L;

    // -- Stable windows of the instance atlas ----------------------------
    // Instance records live in stable per-renderer windows (base + capacity) of
    // the atlas instance VBO: the GpuSpanUploader span-diff sees only records that
    // actually changed (fade quantum, light, machine enter/leave), not the whole
    // buffer shifted by re-laying-out. There is NO per-frame sort: the command order
    // is the stable retained-list order of the windows (the CPU-side opaque/fading
    // partition and the fading-tail sort were removed - see prepareMdiDraw); blending
    // of fading records therefore runs with depth writes disabled (see the draw
    // dispatch below). The cull shader (nucleus_cull.comp) compacts in-place - the
    // survivor's position in the compacted VBO equals its input position - so instance
    // data NEVER moves. Per-frame repacking of fade slots and
    // cascading shifts of opaque windows (the main source of upload churn on camera
    // motion) are eliminated.
    private static final int WINDOW_MIN_CAP = 4;
    /** Free holes {base, capacity} in records; reclaimed by the next repack. */
    private static final java.util.ArrayList<int[]> atlasHoles = new ArrayList<>();
    private static int atlasHoleTotalInstances = 0;
    /** Window placement watermark (in records); holes between windows are normal. */
    private static int atlasWatermark = 0;

    /** Returns window {@code p} to the hole pool (called when a retained record is removed). */
    private static void releaseWindow(Pending p) {
        if (p.atlasWindowBase < 0) {
            p.atlasWindowCap = 0;
            return;
        }
        atlasHoles.add(new int[]{p.atlasWindowBase, p.atlasWindowCap});
        atlasHoleTotalInstances += p.atlasWindowCap;
        p.atlasWindowBase = -1;
        p.atlasWindowCap = 0;
        coalesceTailHoles();
    }

    /** Tail holes adjacent to the watermark return their space to it. */
    private static void coalesceTailHoles() {
        boolean merged = true;
        while (merged && atlasWatermark > 0) {
            merged = false;
            for (int i = 0; i < atlasHoles.size(); i++) {
                int[] h = atlasHoles.get(i);
                if (h[0] + h[1] == atlasWatermark) {
                    atlasWatermark = h[0];
                    atlasHoleTotalInstances -= h[1];
                    atlasHoles.remove(i);
                    merged = true;
                    break;
                }
            }
        }
    }

    // ── Retained draw list (clean-frame reuse) ──────────────────────────
    // Snapshots, partitioning, and fade caches persist across frames: clean renderers
    // (submitClean, no buffer writes during the frame) reuse the previous frame's record
    // without copying snapshots or calculating span diffs; dirty submits replace
    // records in-place (stable list order -> stable upload offsets).

    /** Frame timestamp for assertion semantics: incremented in {@link #beginFrame}. */
    private static long retainedFrameStamp = 0L;
    private static final List<Pending> retainedList = new ArrayList<>(64);
    private static final IdentityHashMap<InstancedStaticPartRenderer, Pending> retainedByRenderer = new IdentityHashMap<>();
    /** Pool of native snapshot buffers — avoids calling memAllocFloat on every submit. */
    private static final ArrayDeque<FloatBuffer> snapshotPool = new ArrayDeque<>();
    private static final int SNAPSHOT_POOL_MAX_BUFFERS = 128;
    /** Pool of retired Pending records — dirty churn replaces a record every frame, so the
     *  record object and its per-instance index arrays are reused instead of feeding the GC. */
    private static final ArrayDeque<Pending> pendingPool = new ArrayDeque<>();
    private static final int PENDING_POOL_MAX_RECORDS = 128;

    static final class Pending {
        /** Owner renderer; nulled while the record sits in the record pool so the pool never pins a BER. */
        InstancedStaticPartRenderer renderer;
        int baseVertex;
        /**
         * Byte offset into the EBO for {@link GL42#glDrawElementsInstancedBaseVertexBaseInstance}
         * (the {@code indices} parameter with a bound EBO is in bytes).
         * In {@link GL40#glDrawElementsIndirect} the {@code firstIndex} field of the command
         * is the index offset in <b>elements</b> ({@code GL_UNSIGNED_INT}: divide bytes by 4).
         */
        int firstIndexBytes;
        int indexCount;
        /** Snapshot at submit time — for comparison with {@link #dispatch} (repack between submit and draw). */
        int submitBaseVertex = -1;
        int submitFirstIndexBytes = -1;
        int submitIndexCount = -1;
        int instanceCount;
        int[] instanceCullIndices;
        long[] instanceOcclusionKeys;
        FloatBuffer instanceData;
        /** Native buffer allocated with {@code memAllocFloat} in {@link #submit}; freed in {@link #endFrame}. */
        boolean instanceDataNativeOwned;
        // ── Retained / clean-frame reuse ───────────────────────────────
        /** Timestamp of last submit / submitClean (frame when renderer asserted presence). */
        long assertedFrame;
        /** Timestamp of last dirty submit (for max-submit rule within a frame). */
        long dirtySubmittedFrame;
        /**
         * Snapshot is synchronized with renderer records: partition and cachedMinFade
         * are precomputed and valid until the next dirty submit.
         */
        boolean partitionValid;
        /** Minimum GPU-ramp alpha of the snapshot (recomputed on dirty submits; the MDI blend gate input). */
        float cachedMinFade = 1.0f;
        /**
         * Min-fade cache validity: the scan is camera-dependent, so it reruns
         * when the records change (any dirty submit) OR the camera moves more
         * than a fraction of a block - a static camera on a static farm never
         * rescans (the unconditional per-frame scan was 7% of the frame over
         * ~50k instances, spark 2026-10-08).
         */
        boolean minFadeValid;
        float minFadeCamX, minFadeCamY, minFadeCamZ;
        /**
         * Records in the snapshot with a distance-faded mode (&lt; FADE_MODE_NONE_DIVIDER),
         * counted once per dirty submit: 0 lets the camera-move rescan of
         * computeWindowMinFade short-circuit to 1f without touching the buffer —
         * walking/strafing otherwise rescans every record of every window each
         * frame (1.6 ms/frame, spark 10-09). NOTE: FADE_MODE_STATIC (0.9999) is
         * below the divider, so every normal static-machine window counts as
         * "fading" — STATIC records can still produce alpha &lt; 1 through the
         * static-zone distance ramp and MUST go through the full scan. The
         * short-circuit only fires for windows that are 100% FADE_MODE_NONE
         * (missiles, contraptions).
         */
        int fadingRecords;
        // -- Stable window in the instance atlas -----------------------------
        /** Window base in records (instance units); -1 = window not yet placed. */
        int atlasWindowBase = -1;
        /** Window capacity in records (grows x2 on overflow). */
        int atlasWindowCap = 0;
        /** The current snapshot is already uploaded into the window and the shadow is valid - the upload is skipped. */
        boolean windowUploaded = false;
        // -- Span-diff shadow (GpuSpanUploader.diffUpload) -------------------
        /**
         * CPU mirror of the GPU contents of this window: written by
         * {@link #uploadWindowsToAtlas} (fresh copy after a full upload) or
         * transferred from the replaced record in {@link #submit} when the
         * previous window upload succeeded (its snapshot IS the GPU mirror).
         * Lets a dirty re-submit upload only changed records instead of the
         * whole window. Recycled with the record (see {@link #recycleSnapshot}).
         */
        FloatBuffer gpuShadowData;
        /** How many floats of {@link #gpuShadowData} are known to mirror the GPU (records appended since are not covered). */
        int gpuShadowValidFloats = 0;
        Pending(InstancedStaticPartRenderer renderer) { this.renderer = renderer; }
    }

    /**
     * One {@link Pending} window's instances as a single sub-draw.
     * Output command buffer: one command per window, all issued in ONE multi-draw.
     */
    static final class SubDraw {
        Pending owner;
        int firstInstance;
        int count;
        int baseInstance;
        SubDraw(Pending owner, int firstInstance, int count, int baseInstance) {
            this.owner = owner;
            this.firstInstance = firstInstance;
            this.count = count;
            this.baseInstance = baseInstance;
        }

        /** Pool reuse: allocation only when the command list grows. */
        void set(Pending owner, int firstInstance, int count, int baseInstance) {
            this.owner = owner;
            this.firstInstance = firstInstance;
            this.count = count;
            this.baseInstance = baseInstance;
        }
    }

    private final Matrix4f projectionMatrix;
    private final List<Pending> pending = new ArrayList<>(16);

    // -- Frame scratch (rendering is single-threaded): pools instead of new lists --
    // PreparedMdi holds these lists only within dispatch() - by the next
    // frame it is dead. SubDraw objects are reused: allocation only
    // when the window count grows.
    private final ArrayList<Pending> drawListScratch = new ArrayList<>(64);
    private final ArrayList<SubDraw> subDrawPool = new ArrayList<>(64);
    /** Persistent indirect command buffer (grows on demand), instead of memAlloc/Free per dispatch. */
    private static ByteBuffer cmdBufScratch;

    /** Latest projection from the render event (camera may move within a game tick). */
    public void refreshProjection(Matrix4f projection) {
        if (projection != null) {
            projectionMatrix.set(projection);
        }
    }

    private MdiBatchCoordinator(Matrix4f projectionMatrix) {
        // Clone: Forge may reuse the same Matrix4f across stages; sharing the
        // reference risks ProjMat changing between beginFrame and dispatch.
        this.projectionMatrix = new Matrix4f(projectionMatrix);
    }

    public static void ensureCapsResolved() {
        if (capsResolved) return;
        synchronized (MdiBatchCoordinator.class) {
            if (capsResolved) return;
            try {
                GLCapabilities caps = GL.getCapabilities();
                if (caps != null) {
                    boolean multi = caps.glMultiDrawElementsIndirect != 0L || caps.GL_ARB_multi_draw_indirect;
                    boolean single = caps.glDrawElementsIndirect != 0L || caps.GL_ARB_draw_indirect;
                    hasDrawIndirect = multi || single;
                    hasBaseInstance = caps.glDrawElementsInstancedBaseVertexBaseInstance != 0L;
                } else {
                    hasDrawIndirect = false;
                    hasBaseInstance = false;
                }
            } catch (Exception t) {
                hasDrawIndirect = false;
                hasBaseInstance = false;
            }
            capsResolved = true;
        }
    }

    public static boolean isMdiAvailable() {
        return HardwareTierManager.isTier1();
    }

    public static MdiBatchCoordinator beginFrame(Matrix4f projectionMatrix) {
        if (!HardwareTierManager.isTier1()) {
            return null;
        }
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            return null;
        }
        retainedFrameStamp++;
        MdiBatchCoordinator session = new MdiBatchCoordinator(projectionMatrix);
        ACTIVE.set(session);
        MdiRenderDiag.logBannerOnce();
        return session;
    }

    public static MdiBatchCoordinator active() {
        return ACTIVE.get();
    }

    /**
     * Discards the active MDI session without issuing {@link #dispatch} — called on F3+T / GPU cache clears
     * when the frame should no longer target the atlas (see {@link MdiGeometryAtlas#resetForResourceLifecycle}).
     */
    public static void discardActiveSessionNoDispatch() {
        MdiBatchCoordinator s = ACTIVE.get();
        if (s != null) {
            // Pending records are retained; snapshots remain alive, and the next frame will re-assert or drop them.
            s.pending.clear();
            ACTIVE.remove();
        }
        // Audit 2026-10-07 #9: no dispatch will follow this frame, so no
        // recordGpuCull will re-assert the overlay values - clear them so the
        // F3 "GPU Cull: Active" line cannot keep showing the discarded
        // session's numbers (the freshness stamp in NucleusDebug covers the
        // remaining sticky cases; this covers the teardown itself).
        NucleusDebug.resetGpuCullStats();
    }

    /**
     * Retained-record purge for runtime degradation
     * ({@code HardwareTierManager.triggerRuntimeDegradation}): safe from
     * in-frame failure paths like {@link #dropAllRetained}, but unlike
     * {@link #clearCachedRedraw} it deliberately does NOT free the snapshot
     * pool / cmdBufScratch - those are event-driven-only (can run mid-dispatch
     * here, where the frees would pull the buffers out from under the
     * unwinding dispatch).
     */
    public static void dropRetainedForDegradation() {
        dropAllRetained();
    }

    public void endFrame() {
        try {
            dispatch();
        } catch (Exception t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] dispatch failed; future flushes will use legacy path", t);
        } finally {
            // Pending records persist in retainedList; snapshots are needed for the next frame. Only reset session.
            pending.clear();
            if (ACTIVE.get() == this) ACTIVE.remove();
        }
    }

    public static void clearCachedRedraw() {
        // Dimension change / resource reload / atlas reset: retained records are invalidated.
        dropAllRetained();
        // Release the persistent high-water natives (snapshot pool + indirect-command
        // scratch) so a session ends without retaining the largest-ever footprint.
        // Safe only here: every caller is event-driven (RenderCacheManager
        // SESSION_END/RESOURCE_RELOAD, forceVanillaImmediate flip) and can never run
        // mid-dispatch, unlike dropAllRetained() which is also called from in-frame
        // failure paths. Both buffers re-grow on demand (takeSnapshot / dispatch).
        FloatBuffer pooled;
        while ((pooled = snapshotPool.pollFirst()) != null) {
            MemoryUtil.memFree(pooled);
        }
        // Retired records hold only on-heap arrays; clearing the pool releases the
        // largest-ever index-array footprint along with the natives above.
        pendingPool.clear();
        if (cmdBufScratch != null) {
            MemoryUtil.memFree(cmdBufScratch);
            cmdBufScratch = null;
        }
    }

    private record PreparedMdi(List<Pending> drawList, List<SubDraw> opaqueSubs,
                               int drawTotalInstances, int pendingSize, int droppedNoSlot, float minFade,
                               boolean ditherFade) {}

    private static void freeDrawListInstanceBuffers(List<Pending> drawList) {
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            if (p.instanceDataNativeOwned && p.instanceData != null) {
                MemoryUtil.memFree(p.instanceData);
                p.instanceData = null;
                p.instanceDataNativeOwned = false;
            }
            if (p.gpuShadowData != null) {
                MemoryUtil.memFree(p.gpuShadowData);
                p.gpuShadowData = null;
                p.gpuShadowValidFloats = 0;
            }
        }
    }

    // ── Snapshot pool + retained list helpers ───────────────────────────

    private static FloatBuffer takeSnapshot(int floats) {
        FloatBuffer buf = snapshotPool.pollFirst();
        if (buf != null && buf.capacity() < floats) {
            MemoryUtil.memFree(buf);
            buf = null;
        }
        if (buf == null) {
            buf = MemoryUtil.memAllocFloat(floats);
        }
        buf.clear();
        return buf;
    }

    private static void recyclePooled(FloatBuffer buf) {
        if (buf == null) {
            return;
        }
        if (snapshotPool.size() < SNAPSHOT_POOL_MAX_BUFFERS) {
            buf.clear();
            snapshotPool.addFirst(buf);
        } else {
            MemoryUtil.memFree(buf);
        }
    }

    private static void recycleSnapshot(Pending p) {
        if (p.instanceDataNativeOwned && p.instanceData != null) {
            FloatBuffer buf = p.instanceData;
            p.instanceData = null;
            p.instanceDataNativeOwned = false;
            recyclePooled(buf);
        }
        recycleShadow(p);
    }

    /** Returns the span-diff shadow buffer to the pool (or nulls it). */
    private static void recycleShadow(Pending p) {
        if (p.gpuShadowData != null) {
            FloatBuffer shadow = p.gpuShadowData;
            p.gpuShadowData = null;
            p.gpuShadowValidFloats = 0;
            recyclePooled(shadow);
        } else {
            p.gpuShadowValidFloats = 0;
        }
    }

    /** Takes a record object from the pool (or allocates one): dirty submits replace the
     *  previous record on every churned frame, so the record object itself is worth reusing. */
    private static Pending acquirePending(InstancedStaticPartRenderer renderer) {
        Pending p = pendingPool.pollFirst();
        if (p == null) {
            return new Pending(renderer);
        }
        p.renderer = renderer;
        return p;
    }

    /** Returns a retired record to the pool. The native snapshot must already be released
     *  via {@link #recycleSnapshot}. The renderer reference is detached so the pool never
     *  pins a BER, and the atlas window is reset so a re-acquired record cannot inherit
     *  the previous owner's window placement. Per-instance index arrays stay attached:
     *  the next owner reuses them when large enough (see {@link #submit}). */
    private static void recyclePending(Pending p) {
        if (pendingPool.size() >= PENDING_POOL_MAX_RECORDS) {
            return;
        }
        p.renderer = null;
        p.instanceData = null;
        p.instanceDataNativeOwned = false;
        p.gpuShadowData = null;
        p.gpuShadowValidFloats = 0;
        p.partitionValid = false;
        p.minFadeValid = false;
        p.windowUploaded = false;
        p.atlasWindowBase = -1;
        p.atlasWindowCap = 0;
        pendingPool.addFirst(p);
    }

    /** Removes record from retained list/map, returns snapshot buffer to pool and the record object to the pool. */
    private static void uninstallRetained(Pending p) {
        Pending mapped = retainedByRenderer.get(p.renderer);
        if (mapped == p) {
            retainedByRenderer.remove(p.renderer);
        }
        releaseWindow(p);
        recycleSnapshot(p);
        recyclePending(p);
    }

    public void evictRenderer(InstancedStaticPartRenderer renderer) {
        evict(renderer);
    }

    public static void evict(InstancedStaticPartRenderer renderer) {
        if (renderer == null) return;
        Pending p = retainedByRenderer.remove(renderer);
        if (p != null) {
            retainedList.remove(p);
            uninstallRetained(p);
        }
    }

    /**
     * Drops retained records without assertion for TWO consecutive frames — renderer
     * became culled or unloaded. The one-frame grace is transactional: a machine whose
     * render collection threw or early-returned this frame (renderers flush with
     * instanceCount==0 and never assert) keeps drawing LAST frame's records and window
     * and is refreshed next frame, instead of being dropped and re-created in the same
     * churn (window released to the hole pool, re-placed with a full re-upload — a
     * visible one-frame vanish + upload spike). A truly removed/unloaded machine
     * lingers exactly one extra frame with its final records; teardown via
     * {@link #dropAllRetained} (world change/reload) stays immediate.
     */
    private static void dropUnassertedRetained() {
        for (Iterator<Pending> it = retainedList.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (retainedFrameStamp - p.assertedFrame >= 2) {
                uninstallRetained(p);
                it.remove();
            }
        }
    }

    /** Complete purge of retained state (world change, resource reload, atlas reset). */
    private static void dropAllRetained() {
        for (int i = 0; i < retainedList.size(); i++) {
            Pending rp = retainedList.get(i);
            Pending mapped = retainedByRenderer.get(rp.renderer);
            if (mapped == rp) {
                retainedByRenderer.remove(rp.renderer);
            }
            recycleSnapshot(rp);
            recyclePending(rp);
        }
        retainedList.clear();
        retainedByRenderer.clear();
        // Full reset of the window layout: world change/reload/atlas reset - GPU content
        // is lost, base stability is meaningless; the next placement starts from scratch.
        atlasHoles.clear();
        atlasHoleTotalInstances = 0;
        atlasWatermark = 0;
    }

    public static void onRenderOriginChanged() {
        dropAllRetained();
    }

    public boolean submit(InstancedStaticPartRenderer renderer,
                          int indexCount,
                          int instanceCount,
                          int instanceDataSize,
                          FloatBuffer instanceDataFlipped,
                          int[] sourceCullIndices,
                          long[] sourceOcclusionKeys,
                          ByteBuffer atlasVertexBytes,
                          IntBuffer atlasIndices,
                          int atlasIndexCount) {
        if (renderer == null || instanceCount <= 0 || indexCount <= 0) return false;

        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (!atlas.acceptsInstanceDataSize(instanceDataSize)) return false;

        MdiGeometryAtlas.Slot slot = atlas.registerGeometryIfAbsent(renderer,
                atlasVertexBytes, atlasIndices, atlasIndexCount);
        if (slot == null) return false;

        int instanceFloats = instanceCount * instanceDataSize;
        FloatBuffer instanceSnapshot = takeSnapshot(instanceFloats);
        try {
            FloatBuffer srcView = instanceDataFlipped.duplicate();
            if (srcView.remaining() < instanceFloats) {
                recyclePooled(instanceSnapshot);
                return false;
            }
            int srcStart = srcView.position();
            srcView.limit(srcStart + instanceFloats);
            instanceSnapshot.put(srcView);
            instanceSnapshot.flip();
        } catch (Exception t) {
            recyclePooled(instanceSnapshot);
            MainRegistry.LOGGER.warn("[HBM-M MDI] instance snapshot alloc/copy failed: {}", t.toString());
            return false;
        }

        // Max-submit rule (Embeddium multi-pass): a secondary submit of the same renderer
        // within one frame is discarded when an earlier, at-least-as-large submit is already
        // retained. The decision runs BEFORE the record and its index arrays are built, so
        // a multi-pass loser never allocates; the earlier record is just re-asserted.
        Pending prev = retainedByRenderer.get(renderer);
        if (prev != null
                && prev.dirtySubmittedFrame == retainedFrameStamp
                && prev.instanceCount >= instanceCount) {
            // First submit this frame was larger / more complete — retain it.
            recyclePooled(instanceSnapshot);
            prev.assertedFrame = retainedFrameStamp;
            prev.partitionValid = false;
            prev.minFadeValid = false;
            // The winning dirty submit this frame already listed prev here (submit
            // tail); a second listing would inflate pendingSize diagnostics and run
            // the slot check twice. pending is only cleared at dispatch, so within
            // one submit batch the record is still present.
            if (!pending.contains(prev)) {
                pending.add(prev);
            }
            return true;
        }

        Pending p = acquirePending(renderer);
        p.baseVertex = slot.baseVertex;
        p.firstIndexBytes = slot.firstIndexBytes;
        p.indexCount = slot.indexCount;
        p.submitBaseVertex = slot.baseVertex;
        p.submitFirstIndexBytes = slot.firstIndexBytes;
        p.submitIndexCount = slot.indexCount;
        p.instanceCount = instanceCount;
        p.dirtySubmittedFrame = retainedFrameStamp;
        p.instanceData = instanceSnapshot;
        p.instanceDataNativeOwned = true;
        // MDI always takes the dither path (see prepareMdiDraw): per-instance
        // fade counts are never consumed there, so the per-dirty-submit buffer
        // walk is pure waste.
        p.fadingRecords = 0;
        // Per-instance index arrays: reuse the replaced record's arrays (ownership moves —
        // prev is recycled right below) or the pooled record's own leftovers when large
        // enough; otherwise every churned dirty frame would hand the GC a fresh
        // int[]/long[] per renderer. Allocation only on growth.
        if (sourceCullIndices != null && sourceCullIndices.length >= instanceCount) {
            int[] dst = null;
            if (prev != null && prev.instanceCullIndices != null
                    && prev.instanceCullIndices.length >= instanceCount) {
                dst = prev.instanceCullIndices;
                prev.instanceCullIndices = null;
            } else if (p.instanceCullIndices != null
                    && p.instanceCullIndices.length >= instanceCount) {
                dst = p.instanceCullIndices;
            }
            if (dst == null) {
                dst = new int[instanceCount];
            }
            System.arraycopy(sourceCullIndices, 0, dst, 0, instanceCount);
            p.instanceCullIndices = dst;
        } else {
            p.instanceCullIndices = null;
        }
        if (sourceOcclusionKeys != null && sourceOcclusionKeys.length >= instanceCount) {
            long[] dst = null;
            if (prev != null && prev.instanceOcclusionKeys != null
                    && prev.instanceOcclusionKeys.length >= instanceCount) {
                dst = prev.instanceOcclusionKeys;
                prev.instanceOcclusionKeys = null;
            } else if (p.instanceOcclusionKeys != null
                    && p.instanceOcclusionKeys.length >= instanceCount) {
                dst = p.instanceOcclusionKeys;
            }
            if (dst == null) {
                dst = new long[instanceCount];
            }
            System.arraycopy(sourceOcclusionKeys, 0, dst, 0, instanceCount);
            p.instanceOcclusionKeys = dst;
        } else {
            p.instanceOcclusionKeys = null;
        }

        // Retained setup: dirty submit replaces renderer record IN-PLACE
        // (stable list order -> stable upload offsets for remaining entries).
        retainedByRenderer.put(renderer, p);
        if (prev != null) {
            int idx = retainedList.indexOf(prev);
            if (idx >= 0) {
                retainedList.set(idx, p);
            } else {
                retainedList.add(p);
            }
            // The stable atlas window moves with the renderer: the snapshot is
            // replaced (the diff will re-upload changed records to the same base);
            // the placement geometry (base/capacity) is preserved.
            p.atlasWindowBase = prev.atlasWindowBase;
            p.atlasWindowCap = prev.atlasWindowCap;
            // Span-diff shadow: when the previous window upload succeeded, the
            // replaced snapshot is the CPU mirror of the GPU window - hand it to
            // the new record as the diff base instead of paying a full-window
            // re-upload for one changed machine. An unuploaded/invalidated
            // window proves nothing about the GPU contents - the shadow is
            // dropped (diffUpload degrades to a full upload).
            if (prev.windowUploaded && prev.gpuShadowData != null) {
                p.gpuShadowData = prev.gpuShadowData;
                p.gpuShadowValidFloats = prev.gpuShadowValidFloats;
            } else {
                p.gpuShadowData = null;
                p.gpuShadowValidFloats = 0;
            }
            prev.gpuShadowData = null;
            prev.gpuShadowValidFloats = 0;
            p.windowUploaded = false;
            prev.atlasWindowBase = -1;
            prev.atlasWindowCap = 0;
            recycleSnapshot(prev);
            recyclePending(prev);
        } else {
            retainedList.add(p);
        }
        p.assertedFrame = retainedFrameStamp;
        p.partitionValid = false;
        p.minFadeValid = false;

        pending.add(p);
        return true;
    }

    /**
     * Clean re-assertion of presence: renderer made zero writes in this phase
     * (skip-write in {@code addInstance}), buffer is synchronized with snapshot.
     * Previous frame record (snapshot, partition, cachedMinFade) is reused —
     * without copying snapshot, diffing, or re-uploading window.
     *
     * @return true if presence accepted; false if standard {@link #submit} is required.
     */
    public boolean submitClean(InstancedStaticPartRenderer renderer, int indexCount, int instanceCount) {
        if (!ClientRenderFlags.mdiCleanFrameReuse()) {
            return false;
        }
        Pending p = retainedByRenderer.get(renderer);
        if (p == null || !p.partitionValid) {
            return false;
        }
        if (p.instanceCount != instanceCount || p.indexCount != indexCount) {
            return false;
        }
        if (!renderer.mdiBufferSynced || renderer.mdiRecordWriteHappened) {
            return false;
        }
        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady() || atlas.getCurrentSlot(renderer) == null) {
            return false;
        }
        p.assertedFrame = retainedFrameStamp;
        return true;
    }

    /**
     * Re-asserts presence for a renderer whose static instances were completely short-circuited
     * on the CPU by RetainedInstanceGraph (instanceCount == 0 on the CPU, but persistent instances
     * are retained in the GPU atlas window).
     */
    public boolean tryAssertRetainedPresence(InstancedStaticPartRenderer renderer) {
        if (!ClientRenderFlags.mdiCleanFrameReuse()) {
            return false;
        }
        com.hbm_m.client.render.retained.RetainedInstanceGraph rig =
                com.hbm_m.client.render.retained.RetainedInstanceGraph.get();
        if (rig != null && !rig.hasActiveInstances(renderer)) {
            return false;
        }
        Pending p = retainedByRenderer.get(renderer);
        if (p == null || !p.partitionValid || p.instanceCount <= 0) {
            return false;
        }
        if (!renderer.mdiBufferSynced || renderer.mdiRecordWriteHappened) {
            return false;
        }
        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady() || atlas.getCurrentSlot(renderer) == null) {
            return false;
        }
        p.assertedFrame = retainedFrameStamp;
        return true;
    }

    private void dispatch() {
        PreparedMdi prepared = prepareMdiDraw();
        if (prepared == null) {
            return;
        }
        long gameTime = resolveLevelGameTime();
        // Snapshots are NOT freed after dispatch: retained list persists across frames
        // (submitClean reuses them without copying). Recycled to pool on replacement
        // (dirty submit), dropping (no assert / atlas slot lost), or clearCachedRedraw().
        executeMdiGlDraw(prepared, projectionMatrix, gameTime);
    }

    private static long resolveLevelGameTime() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                return mc.level.getGameTime();
            }
        } catch (Exception ignored) {
        }
        return -1L;
    }

    private PreparedMdi prepareMdiDraw() {
        // Phase B: retained records without frame assertion — renderer out of view/world.
        dropUnassertedRetained();

        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady()) {
            return null;
        }

        // Dirty submits: slot check and drift diagnostics. Multi-pass losers
        // already filtered out in submit; nothing to install into retained.
        int pendingSize = pending.size();
        for (int i = pending.size() - 1; i >= 0; i--) {
            Pending p = pending.get(i);
            if (p.renderer == null) {
                // Record recycled mid-frame (origin change / cache clear) while still
                // listed here; it is no longer in retainedList — nothing left to check.
                pending.remove(i);
                continue;
            }
            MdiGeometryAtlas.Slot slot = atlas.getCurrentSlot(p.renderer);
            if (slot == null) {
                MainRegistry.LOGGER.warn(
                        "[HBM-M MDI] Atlas slot missing for renderer {}; dropping this MDI sub-draw (others still draw)",
                        System.identityHashCode(p.renderer));
                uninstallRetained(p);
                retainedList.remove(p);
                pending.remove(i);
                continue;
            }
            boolean slotDrift = p.submitBaseVertex != slot.baseVertex
                    || p.submitFirstIndexBytes != slot.firstIndexBytes
                    || p.submitIndexCount != slot.indexCount;
            if (slotDrift) {
                if (MdiRenderDiag.isDebugEnabled() || MdiRenderDiag.isVerboseEnabled()) {
                    String tag = p.renderer.getMdiTraceTag();
                    MainRegistry.LOGGER.warn(
                            "[HBM-M MDI] slot drift tag={} rid=0x{} submit(bv,fiB,ic)=({},{},{}) atlas=({},{},{})",
                            tag != null ? tag : "?",
                            Integer.toHexString(System.identityHashCode(p.renderer)),
                            p.submitBaseVertex, p.submitFirstIndexBytes, p.submitIndexCount,
                            slot.baseVertex, slot.firstIndexBytes, slot.indexCount);
                }
            }
            p.baseVertex = slot.baseVertex;
            p.firstIndexBytes = slot.firstIndexBytes;
            p.indexCount = slot.indexCount;
        }
        pending.clear();

        // Frame draw list = retained list (stable order -> stable upload offsets;
        // reused records require no re-upload). Slot check with lazy re-registration
        // of geometry from retained bytes of renderer (atlas repack/reset).
        List<Pending> drawList = drawListScratch;
        drawList.clear();
        int droppedNoSlot = 0;
        for (Iterator<Pending> it = retainedList.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            MdiGeometryAtlas.Slot slot = atlas.getCurrentSlot(p.renderer);
            if (slot == null && p.renderer.atlasVertexBytesRetained != null
                    && p.renderer.atlasIndicesRetained != null
                    && p.renderer.atlasIndexCountRetained > 0) {
                slot = atlas.registerGeometryIfAbsent(p.renderer, p.renderer.atlasVertexBytesRetained,
                        p.renderer.atlasIndicesRetained, p.renderer.atlasIndexCountRetained);
            }
            if (slot == null) {
                uninstallRetained(p);
                it.remove();
                droppedNoSlot++;
                continue;
            }
            p.baseVertex = slot.baseVertex;
            p.firstIndexBytes = slot.firstIndexBytes;
            p.indexCount = slot.indexCount;
            drawList.add(p);
        }
        if (drawList.isEmpty()) {
            return null;
        }

        int instanceFloatsPerInstance = atlas.getInstanceFloatsPerInstance();
        int instanceFadeOffset = atlas.getInstanceFadeFloatOffset();
        float camX = FrameViewState.relCamX();
        float camY = FrameViewState.relCamY();
        float camZ = FrameViewState.relCamZ();

        // Variant G+: every renderer window draws as ONE sub-draw in a single
        // multi-draw; the CPU-side opaque/fading partition and the per-frame
        // membership scan were removed with it. The only camera-dependent
        // per-frame work left is the blend gate input: the min GPU-ramp alpha
        // over the window's records (the record stores a static fade MODE).
        // Dither mode (nucleusDitherFade) resolves alpha per fragment on the
        // GPU (screen-door discard, no blending) and gates to a constant 1.
        // The user's nucleusDitherFade toggle picks the fade STYLE; the CPU-side
        // min-fade scan stays dead either way (see executeMdiGlDraw): dither mode
        // needs no blend decision at all, and smooth-alpha mode enables GL_BLEND
        // unconditionally - for every opaque instance (vFadeAlpha == 1.0)
        // GL_SRC_ALPHA/ONE_MINUS_SRC_ALPHA with depthMask(true) is a mathematical
        // identity, so no per-window blend gate is required.
        boolean ditherFade = com.hbm_m.config.ModClothConfig.get().nucleusDitherFade;
        int cleanRenderers = 0;
        int cleanInstances = 0;
        int drawTotalInstances = 0;
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            p.cachedMinFade = 1f;
            p.minFadeValid = true;
            p.partitionValid = true;
            cleanRenderers++;
            cleanInstances += p.instanceCount;
            drawTotalInstances += p.instanceCount;
        }
        NucleusDebug.recordMdiReuse(drawList.size(), cleanRenderers, cleanInstances);
        if (drawTotalInstances == 0) {
            dropAllRetained();
            return null;
        }

        // -- Stable atlas windows: placement / growth / defragmentation ------
        // A window moves only when capacity runs out (instanceCount grew -
        // new machines of this part-renderer entered the zone); the repack on any
        // (re)placement compacts the layout and returns holes. Bases are stable
        // between repacks -> the span-diff sees only changed records.
        boolean anyAlloc = false;
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            if (p.atlasWindowBase >= 0 && p.atlasWindowCap >= p.instanceCount) {
                continue;
            }
            int oldCap = p.atlasWindowCap;
            p.atlasWindowCap = Math.max(WINDOW_MIN_CAP, Math.max(p.instanceCount, oldCap * 2));
            p.atlasWindowBase = -1;
            p.windowUploaded = false;
            // The window will be re-placed at a new offset: the shadow mirrors the
            // OLD GPU location, so keeping it would make diffUpload see zero
            // differences and upload nothing to the new window.
            recycleShadow(p);
            anyAlloc = true;
        }
        if (anyAlloc) {
            int base = 0;
            for (int i = 0; i < drawList.size(); i++) {
                Pending p = drawList.get(i);
                if (p.atlasWindowBase != base) {
                    p.atlasWindowBase = base;
                    p.windowUploaded = false;
                    recycleShadow(p);
                }
                base += p.atlasWindowCap;
            }
            atlasHoles.clear();
            atlasHoleTotalInstances = 0;
            atlasWatermark = base;
        }
        if (!atlas.ensureInstanceCapacity(atlasWatermark)) {
            freeDrawListInstanceBuffers(drawList);
            dropAllRetained();
            return null;
        }

        // Preserved MDI draw commands: one command per part-geometry (p.instanceCount).
        int subCount = 0;
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            if (p.instanceCount > 0) {
                SubDraw s;
                if (subCount < subDrawPool.size()) {
                    s = subDrawPool.get(subCount);
                } else {
                    s = new SubDraw(null, 0, 0, 0);
                    subDrawPool.add(s);
                }
                subCount++;
                s.set(p, 0, p.instanceCount, p.atlasWindowBase);
            }
        }
        List<SubDraw> opaqueSubs = subDrawPool.subList(0, subCount);

        if (!uploadWindowsToAtlas(drawList, atlas, instanceFloatsPerInstance)) {
            freeDrawListInstanceBuffers(drawList);
            dropAllRetained();
            return null;
        }

        return new PreparedMdi(drawList, opaqueSubs, drawTotalInstances, pendingSize, droppedNoSlot, 1f,
                ditherFade);
    }

    /**
     * Count of distance-faded records (mode &lt; FADE_MODE_NONE_DIVIDER) in a fresh
     * snapshot - see {@link Pending#fadingRecords}. Runs once per dirty submit over
     * data still warm in cache, so the per-camera-move rescan can skip windows with
     * no distance-faded records at all (100% FADE_MODE_NONE - missiles, contraptions)
     * with one integer compare instead of a full buffer walk. Normal static-machine
     * windows always count &gt; 0: FADE_MODE_STATIC sits below the divider because
     * its records still go through the distance ramp in computeWindowMinFade.
     */
    private static int countFadingRecords(FloatBuffer data, int count, int stride, int fadeOffset) {
        if (data == null || count <= 0) {
            return 1; // unknown - force the full scan path
        }
        long addr = MemoryUtil.memAddress(data);
        int fading = 0;
        for (int i = 0; i < count; i++) {
            float mode = MemoryUtil.memGetFloat(addr + (long) (i * stride + fadeOffset) * 4L);
            // FADE_MODE_INVISIBLE (freed/hidden slots) must not count as fading -
            // the vertex shader clips them out and they carry no blend contribution.
            if (mode >= 0.0f && mode < InstancedStaticPartRenderer.FADE_MODE_NONE_DIVIDER) {
                fading++;
            }
        }
        return fading;
    }

    /**
     * Min GPU-ramp alpha over one window's records ({@link Pending#cachedMinFade},
     * the MDI blend gate) - CPU mirror of the vsh fade. The record stores a static
     * fade MODE (1.0/0.9999/0.9998): mode NONE short-circuits to 1 without a
     * distance test, so an all-far window costs one float read per record. Records
     * are anchor-relative: the distance is (record pos - relCam), the same form the
     * direct path feeds {@link RenderDistanceHelper#evalInstanceAlpha}.
     */
    private static float computeWindowMinFade(Pending p, int stride, int fadeOffset,
                                              float camX, float camY, float camZ) {
        FloatBuffer data = p.instanceData;
        if (data == null || p.instanceCount <= 0) {
            return 1f;
        }
        // The snapshot is always a direct native buffer (MemoryUtil.memAllocFloat
        // in takeSnapshot) and is flipped (position = 0) by the time this runs,
        // so the raw address is valid for instanceCount * stride floats and the
        // bounds-checked FloatBuffer.get reads can be raw memGetFloat reads.
        long addr = MemoryUtil.memAddress(data);
        float min = 1f;
        for (int i = 0; i < p.instanceCount; i++) {
            int base = i * stride;
            float mode = MemoryUtil.memGetFloat(addr + (long) (base + fadeOffset) * 4L);
            // CPU mirror of gpuFadeAlpha's "mode < 0 -> clipped out": a freed slot
            // sits at anchor-relative (0,0,0), up to SQR_MAX_ANCHOR_DISTANCE (256
            // blocks) from relCam, and must never drag cachedMinFade (the blend
            // gate) below 1 regardless of that distance.
            if (mode < 0.0f) {
                continue;
            }
            if (mode >= InstancedStaticPartRenderer.FADE_MODE_NONE_DIVIDER) {
                continue;
            }
            float dx = MemoryUtil.memGetFloat(addr + (long) base * 4L) - camX;
            float dy = MemoryUtil.memGetFloat(addr + (long) (base + 1) * 4L) - camY;
            float dz = MemoryUtil.memGetFloat(addr + (long) (base + 2) * 4L) - camZ;
            float a = RenderDistanceHelper.evalInstanceAlpha(
                    (double) dx * dx + (double) dy * dy + (double) dz * dz, mode);
            if (a < min) {
                min = a;
                if (min <= 0f) {
                    return 0f;
                }
            }
        }
        return min;
    }

    /**
     * Stable-window upload: each {@link Pending} writes its snapshot always
     * to the same atlas base - the {@link GpuSpanUploader} diff sees only
     * records that actually changed (fade quantum, light, machine enter/leave), not
     * the whole buffer shifted by re-laying-out. A window unchanged since the last
     * successful upload with a valid shadow is skipped entirely (including the memcmp scan).
     * <p>
     * Correctness invariant: the shadow mirrors the actual GPU buffer contents
     * (all records go through GpuSpanUploader; a capacity growth / anchor drift /
     * atlas reset resets shadow validity -> full uploads), so the diff is
     * safe even for a window placed for the first time into someone else's former hole.
     */
    private static boolean uploadWindowsToAtlas(List<Pending> drawList,
                                                MdiGeometryAtlas atlas, int instanceFloatsPerInstance) {
        boolean shadowValid = atlas.isInstanceWindowsValid();
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            if (p.instanceData == null) {
                return false;
            }
            int floats = p.instanceCount * instanceFloatsPerInstance;
            if (p.instanceData.remaining() < floats) {
                return false;
            }
            if (!shadowValid) {
                // Atlas instance VBO was invalidated (realloc/anchor shift/reset):
                // the shadow claims to mirror GPU contents that no longer exist.
                p.windowUploaded = false;
                recycleShadow(p);
            }
            if (shadowValid && p.windowUploaded) {
                continue;
            }
            // Undersized shadow (window grew since the transfer): recycle - the
            // diff base must cover the whole window or it is not a base at all.
            if (p.gpuShadowData != null && p.gpuShadowData.capacity() < floats) {
                recycleShadow(p);
            }
            atlas.uploadInstanceWindowSpanned(p.atlasWindowBase * instanceFloatsPerInstance,
                    p.instanceData, 0, floats, p.gpuShadowData, p.gpuShadowValidFloats);
            if (p.gpuShadowData == null && floats > 0) {
                // First upload without a transferred shadow (full upload): seed
                // the CPU mirror so the NEXT dirty submit can span-diff instead
                // of re-uploading the whole window again.
                FloatBuffer shadow = takeSnapshot(floats);
                GpuSpanUploader.syncShadow(shadow, 0, p.instanceData, 0, floats);
                p.gpuShadowData = shadow;
            }
            p.gpuShadowValidFloats = floats;
            p.windowUploaded = true;
        }
        atlas.markInstanceWindowsValid();
        return true;
    }

    // Adaptive occlusion gate (CrankShaft OCCLUSION_VERTICES pattern with hysteresis):
    // threshold on total MDI instances; below it the cull dispatch runs frustum-only
    // and the Hi-Z pyramid is not rebuilt (see executeMdiGlDraw).
    /** Config nucleusGpuCullMinInstances; -Dhbm.gpuCull.minInstances=N overrides. */
    private static int occlusionMinInstances() {
        Integer flag = Integer.getInteger("hbm.gpuCull.minInstances");
        if (flag != null) {
            return Math.max(0, flag);
        }
        return Math.max(0, com.hbm_m.config.ModClothConfig.get().nucleusGpuCullMinInstances);
    }
    private static boolean occlusionLatch = false;
    private static long lastOcclusionGateLogMs = 0L;

    private static void logOcclusionGate(String state, int totalInstances) {
        long now = System.currentTimeMillis();
        if (now - lastOcclusionGateLogMs < 1000L) {
            return;
        }
        lastOcclusionGateLogMs = now;
        int threshold = occlusionMinInstances();
        MainRegistry.LOGGER.info("[HBM-M GPU Cull] Occlusion gate {} (instances={}, threshold={}, hysteresis={})",
                state, totalInstances, threshold, threshold >> 1);
    }

    private static void executeMdiGlDraw(PreparedMdi prepared, Matrix4f projection, long gameTime) {
        List<Pending> drawList = prepared.drawList;
        List<SubDraw> opaqueSubs = prepared.opaqueSubs();
        if (drawList.isEmpty() || opaqueSubs.isEmpty()) {
            return;
        }
        // Embeddium/Iris dispatch stage events from shadow terrain passes as well —
        // flushing is disallowed there (same contract as flushBatchIris); also protects
        // FrameViewState from capturing shadow pass camera.
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }

        MdiGeometryAtlas atlas = MdiGeometryAtlas.getOrCreate();
        if (atlas == null || !atlas.isReady()) {
            return;
        }
        // Defensive VAO-liveness gate (render thread): a dead/recycled atlas VAO
        // would make this bind fall through to a foreign VAO and fault the MDI
        // draw in the driver. On failure the atlas was reset (slot table empty,
        // prepared commands stale) - bail out of this frame's draw.
        if (!atlas.ensureVaoAlive()) {
            return;
        }

        ShaderInstance shader = ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            return;
        }

        boolean gpuCullEligible = (ModClothConfig.get().getEffectiveOcclusionCullingMode() == ModClothConfig.OcclusionCullingMode.GPU)
                && GpuCullingCapability.isSupported()
                && !ShaderCompatibilityDetector.isRenderingShadowPass();
        var mc = Minecraft.getInstance();
        var target = mc.getMainRenderTarget();
        boolean gpuCullActive = gpuCullEligible && target != null && target.getDepthTextureId() > 0;

        int nCmd = opaqueSubs.size();
        int totalInstances = 0;
        // Persistent growing buffer: memAlloc/memFree per dispatch would mean an extra
        // trip to the allocator and malloc churn on weak CPUs.
        int needed = nCmd * INDIRECT_CMD_STRIDE_BYTES;
        ByteBuffer cmdBuf = cmdBufScratch;
        if (cmdBuf == null || cmdBuf.capacity() < needed) {
            if (cmdBuf != null) {
                MemoryUtil.memFree(cmdBuf);
            }
            cmdBuf = MemoryUtil.memAlloc(Math.max(needed, 4096));
            cmdBufScratch = cmdBuf;
        }
        cmdBuf.clear();
        cmdBuf.order(ByteOrder.nativeOrder());
        int globalOffset = 0;
        for (int i = 0; i < opaqueSubs.size(); i++) {
            SubDraw sub = opaqueSubs.get(i);
            Pending p = sub.owner;
            int rowStart = cmdBuf.position();
            cmdBuf.putInt(p.indexCount);
            cmdBuf.putInt(gpuCullActive ? 0 : sub.count);
            cmdBuf.putInt(p.firstIndexBytes >>> 2);
            cmdBuf.putInt(p.baseVertex);
            cmdBuf.putInt(sub.baseInstance);
            cmdBuf.putInt(sub.count);
            cmdBuf.putInt(globalOffset); // pad1 -> globalInstanceOffset
            cmdBuf.putInt(0);            // pad2
            while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                cmdBuf.putInt(0);
            }
            globalOffset += sub.count;
            totalInstances += sub.count;
        }
        cmdBuf.flip();

        // Adaptive occlusion gate (the CrankShaft OCCLUSION_VERTICES pattern with hysteresis):
        // a full Hi-Z pyramid rebuild is a fixed per-frame cost (SPD downsample of
        // the whole screen + barriers); on small scenes it outweighs the occlusion benefit.
        // Below the threshold the cull dispatch runs in frustum-only mode (the pyramid is
        // not rebuilt nor sampled), above 2x the threshold the two-phase mode returns.
        // -Dhbm.gpuCull.minInstances=0 - occlusion always active (the old behavior).
        boolean occlusionEnabled = gpuCullActive;
        int occlusionMin = occlusionMinInstances();
        if (occlusionEnabled && occlusionMin > 0) {
            if (!occlusionLatch) {
                if (totalInstances >= occlusionMin) {
                    occlusionLatch = true;
                    logOcclusionGate("ENABLED", totalInstances);
                }
            } else if (totalInstances < (occlusionMin >> 1)) {
                occlusionLatch = false;
                logOcclusionGate("DISABLED", totalInstances);
            }
            occlusionEnabled = occlusionLatch;
        }

        com.hbm_m.client.render.debug.GlDebugMarkers.pushGroup("HBM_Vanilla_MDI_Dispatch");
        try {
            String dispatchDrawMode = "MULTI";
            boolean gpuCulled = false;

            try {
                GL30.glBindVertexArray(atlas.getVaoId());
                // The driver can silently ignore the bind when the name was deleted or
                // recycled behind our back (GL_INVALID_OPERATION "VAO names must be
                // generated..."): the next line would then enable attribs 4..15 on a
                // FOREIGN VAO and glMultiDrawElementsIndirect faults in the driver
                // (native crash, hs_err_pid50040). Verify the binding actually stuck.
                int bound = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
                if (bound != atlas.getVaoId()) {
                    MainRegistry.LOGGER.error(
                            "[HBM-M MDI] Atlas VAO {} bind was rejected by the driver (GL_VERTEX_ARRAY_BINDING={}, foreign VAO); resetting atlas and skipping this frame's MDI draw",
                            atlas.getVaoId(), bound);
                    MdiGeometryAtlas.resetForResourceLifecycle();
                    return;
                }
                atlas.enableVertexAttribArraysOnBoundVao();

                // Lightmap freshness is owned by RenderFrameLight.ensureLightTextureUpdated()
                // (present start) on top of vanilla's own updateLightTexture at the start of
                // GameRenderer.renderLevel — an extra rebuild here was a per-frame duplicate
                // of a 256×256 CPU upload.

                RenderSystem.setShader(() -> shader);
                // Min fade over the batch must be known BEFORE uniform upload:
                // when any instance is fading, the screen-door dither path is
                // forced (uDitherFade = 1) regardless of the config flag — see
                // the forced-dither decision in applyCommonUniforms below.
                // Cached minima: cachedMinFade
                // precomputed during snapshot partition, fade of individual
                // slots captured during enumeration — per-frame strided scan
                // of all instances is unnecessary.
                boolean ditherFade = prepared.ditherFade();
                applyCommonUniforms(shader, projection, ditherFade);
                // REGRESSION-STOP (MDI): same block_lit contract as VanillaInstancedBatchRenderer — otherwise white batches.
                SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
                // PROGRAM CACHE DESYNC PROTECTION: the compute cull passes below (and
                // Oculus's per-frame raw glUseProgram(0)) desync static lastProgramId
                // from the physical program - force an honest bind here (same guard as
                // VanillaInstancedBatchRenderer / SingleMeshVboRenderer).
                com.hbm_m.client.render.shader.ShaderBindResync.ensureFreshBind(shader);
                shader.apply();
                InstancedStaticPartRenderer.bindBlockLitTexturesBeforeDraw(shader);

                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();
                if (!ditherFade) {
                    // Smooth alpha fade mode (nucleusDitherFade == false): for every
                    // opaque instance (vFadeAlpha == 1.0) GL_SRC_ALPHA /
                    // GL_ONE_MINUS_SRC_ALPHA with depthMask(true) writes 100% srcColor
                    // and full depth - visually identical to blending off - while the
                    // far fade ring alpha-blends into the already-drawn terrain/sky.
                    // No CPU-side min-fade scan is needed for this decision.
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                }
                // No alpha blending on the MDI path, ever. Dither mode never
                // blends - the FSH discards by screen hash. When the batch
                // contains fading instances (minFade < 1), the dither path is
                // FORCED (uDitherFade uploaded as 1 in applyCommonUniforms) even
                // if the config flag is off: the command order of the single
                // unsorted glMultiDrawElementsIndirect below is the stable
                // retained-list order, not back-to-front, so alpha blending is
                // unusable here — depthMask(false) for the whole batch would
                // leave fully opaque machines without depth writes (self-overlap
                // mess with cull disabled), while depth writes on would let a
                // near translucent fragment depth-reject the geometry drawn
                // after it (punch holes in machines behind it). The dither
                // discard needs no blending, so depth writes stay enabled and
                // fading renders as screen-door. A batch without fading
                // instances (minFade >= 1) has nothing to blend either.

                // REGRESSION-FIX: capacity must accommodate ALL commands (nCmd =
                // opaque sub-draws + 1-instance fading slots), not just renderer count —
                // otherwise growing fading instances under-ensured buffer and glBufferSubData truncated with GL_INVALID_VALUE.
                int cmdByteLen = nCmd * INDIRECT_CMD_STRIDE_BYTES;
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, atlas.getIndirectBufferId());
                atlas.ensureIndirectCommandByteCapacity(cmdByteLen);
                GL15.glBufferSubData(GL40.GL_DRAW_INDIRECT_BUFFER, 0, cmdBuf);

                if (gpuCullActive) {
                    com.hbm_m.client.render.debug.GlDebugMarkers.pushGroup("HBM_MDI_HiZ_Cull");
                    try {
                        // Full pyramid rebuild - only when occlusion is enabled by the gate;
                        // in frustum-only mode the culler does not sample the pyramid (it holds
                        // the previous-frame/placeholder one), saving a full SPD screen downsample.
                        if (occlusionEnabled) {
                            int depthTex = target.getDepthTextureId();
                            if (ShaderCompatibilityDetector.isExternalShaderActive()) {
                                int irisDepth = getActiveGbufferDepthTexture();
                                if (irisDepth > 0) {
                                    depthTex = irisDepth;
                                }
                            }
                            HiZDepthPyramid.get().regenerate(depthTex, target.width, target.height);
                        }
                        float zFar = mc.gameRenderer != null ? mc.gameRenderer.getDepthFar() : 1024.0f;
                        gpuCulled = NucleusGpuCuller.cull(
                                atlas,
                                nCmd,
                                totalInstances,
                                HiZDepthPyramid.get(),
                                FrameViewState.viewMatrix(),
                                projection,
                                0.05f,
                                zFar,
                                target.width,
                                target.height,
                                !occlusionEnabled
                        );
                    } catch (Exception t) {
                        MainRegistry.LOGGER.warn("[HBM-M MDI] GPU occlusion culling failed, falling back to full batch: {}", t.getMessage());
                        gpuCulled = false;
                    } finally {
                        com.hbm_m.client.render.debug.GlDebugMarkers.popGroup();
                    }

                    // Stamp EVERY dispatch (outside the 1 Hz readback gate): the F3
                    // freshness gate compares against the current frame, and a stamp
                    // written only by the readback made a working culler print
                    // "Waiting/No depth" 59 of 60 frames (10-09).
                    if (gpuCulled) {
                        NucleusDebug.noteGpuCullDispatch(nCmd, totalInstances);
                    }
                    
                    // RESTORE SHADER PROGRAM OVERWRITTEN BY COMPUTE PASSES
                    GlStateManager._glUseProgram(shader.getId());

                    if (gpuCulled) {
                        atlas.bindCompactedInstanceVbo();

                        // Diagnostic readback is a full pipeline sync (glGetBufferSubData
                        // of the whole indirect buffer): only pay it when the stats are
                        // actually consumed — F3 overlay open or MDI diag logging enabled.
                        boolean cullStatsNeeded = MdiRenderDiag.isDebugEnabled()
                                || com.hbm_m.platform.RenderHooks.isDebugScreenVisible();
                        long now = System.currentTimeMillis();
                        if (cullStatsNeeded && now - lastGpuCullLogTimeMs >= 1000L) {
                            lastGpuCullLogTimeMs = now;
                            try {
                                GL42.glMemoryBarrier(GL43.GL_BUFFER_UPDATE_BARRIER_BIT | GL43.GL_SHADER_STORAGE_BARRIER_BIT);
                                cmdBuf.clear();
                                // Restrict the read to the bytes actually written this frame:
                                // the scratch capacity is rounded up (>=4096), and a read past
                                // the live command range is a driver-side GL_INVALID_VALUE that
                                // leaves stale zeros here - reported as a phantom "100% culled".
                                cmdBuf.limit(nCmd * INDIRECT_CMD_STRIDE_BYTES);
                                GL15.glGetBufferSubData(GL40.GL_DRAW_INDIRECT_BUFFER, 0, cmdBuf);
                                int totalIn = 0;
                                int totalOut = 0;
                                for (int i = 0; i < nCmd; i++) {
                                    int instanceCount = cmdBuf.getInt(i * INDIRECT_CMD_STRIDE_BYTES + 4);
                                    int originalCount = cmdBuf.getInt(i * INDIRECT_CMD_STRIDE_BYTES + 20);
                                    totalIn += originalCount;
                                    totalOut += instanceCount;
                                }
                                int culled = Math.max(0, totalIn - totalOut);
                                float pct = totalIn > 0 ? (culled * 100.0f / totalIn) : 0f;
                                MainRegistry.LOGGER.info("[HBM-M GPU Cull] Active: cmds={}, instances: {} -> {} (culled: {} / {}%), depthTex={}, Hi-Z mips={}",
                                        nCmd, totalIn, totalOut, culled, String.format(java.util.Locale.ROOT, "%.1f", pct),
                                        target.getDepthTextureId(), HiZDepthPyramid.get().getMipLevels());
                                if (culled == 0 && totalIn > 0) {
                                    MainRegistry.LOGGER.info("[HBM-M GPU Cull] NOTE: 0 instances were culled (100% passed frustum + Hi-Z occlusion test).");
                                }
                                NucleusDebug.recordGpuCull(true, nCmd, totalIn, totalOut);
                            } catch (Exception t) {
                                MainRegistry.LOGGER.warn("[HBM-M GPU Cull] Diagnostic readback failed: {}", t.getMessage());
                            }
                        }
                    } else {
                        // Persistent F3 state: the fallback is otherwise invisible
                        // (the 2 Hz log below only writes the console).
                        NucleusDebug.noteGpuCullFallback(nCmd);
                        // Safe CPU fallback / unculled restoration:
                        // Rewrite original instance counts to indirect buffer and retain default instance VBO
                        cmdBuf.clear();
                        globalOffset = 0;
                        for (int i = 0; i < opaqueSubs.size(); i++) {
                            SubDraw sub = opaqueSubs.get(i);
                            Pending p = sub.owner;
                            int rowStart = cmdBuf.position();
                            cmdBuf.putInt(p.indexCount);
                            cmdBuf.putInt(sub.count);
                            cmdBuf.putInt(p.firstIndexBytes >>> 2);
                            cmdBuf.putInt(p.baseVertex);
                            cmdBuf.putInt(sub.baseInstance);
                            cmdBuf.putInt(sub.count);
                            cmdBuf.putInt(globalOffset); // pad1 -> globalInstanceOffset
                            cmdBuf.putInt(0);            // pad2
                            while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                                cmdBuf.putInt(0);
                            }
                            globalOffset += sub.count;
                        }
                        cmdBuf.flip();
                        GL15.glBufferSubData(GL40.GL_DRAW_INDIRECT_BUFFER, 0, cmdBuf);

                        long now = System.currentTimeMillis();
                        if (now - lastGpuCullLogTimeMs >= 2000L) {
                            lastGpuCullLogTimeMs = now;
                            MainRegistry.LOGGER.warn("[HBM-M GPU Cull] Fallback active! gpuCulled=false; restored CPU unculled instances.");
                            NucleusDebug.recordGpuCull(false, nCmd, 0, 0);
                        }
                    }
                } else {
                    // Culling intentionally OFF (config or unsupported HW): that is the
                    // steady state, not an incident - do not log it, it only spams.
                    boolean modeOff = ModClothConfig.get().getEffectiveOcclusionCullingMode()
                            == ModClothConfig.OcclusionCullingMode.OFF;
                    if (!modeOff) {
                        long now = System.currentTimeMillis();
                        if (now - lastGpuCullLogTimeMs >= 3000L) {
                            lastGpuCullLogTimeMs = now;
                            MainRegistry.LOGGER.info("[HBM-M GPU Cull] Inactive: eligible={}, targetNull={}, depthTexId={}, mode={}, gpuSupported={}, shadowPass={}",
                                    gpuCullEligible,
                                    target == null,
                                    target != null ? target.getDepthTextureId() : -1,
                                    ModClothConfig.get().getEffectiveOcclusionCullingMode(),
                                    GpuCullingCapability.isSupported(),
                                    ShaderCompatibilityDetector.isRenderingShadowPass());
                            NucleusDebug.recordGpuCull(false, nCmd, 0, 0);
                        }
                    }
                }
                try {
                    GLCapabilities capsBarrier = GL.getCapabilities();
                    if (capsBarrier != null && capsBarrier.glMemoryBarrier != 0L) {
                        GL42.glMemoryBarrier(GL42.GL_COMMAND_BARRIER_BIT);
                    }
                } catch (Exception ignored) {
                }

                // CRITICAL SAFETY FOR MDI:
                // Ensure the indirect buffer is explicitly bound to GL_DRAW_INDIRECT_BUFFER.
                // If GL_DRAW_INDIRECT_BUFFER is 0 or unbound, glMultiDrawElementsIndirect/glDrawElementsIndirect
                // treats the offset (0L) as a CPU host memory pointer, instantly causing EXCEPTION_ACCESS_VIOLATION
                // in the native graphics driver (e.g. nvoglv64.dll / atio6axx.dll).
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, atlas.getIndirectBufferId());
                GL30.glBindVertexArray(atlas.getVaoId());
                // Re-bind the joint specs texture after the GPU-cull dispatch: the
                // sampler uniform value persists per program, but the TEXTURE on
                // the specs unit must survive any intermediate compute work.
                NucleusJointSpecs.bindSampler();

                GLCapabilities caps2 = GL.getCapabilities();
                boolean canMulti = caps2 != null
                        && (caps2.glMultiDrawElementsIndirect != 0L || caps2.GL_ARB_multi_draw_indirect);
                boolean canSingle = caps2 != null
                        && (caps2.glDrawElementsIndirect != 0L || caps2.GL_ARB_draw_indirect);

                // Depth writes are always ON here: the MDI path never blends
                // (see the gate comment above) - fading is screen-door dither,
                // so opaque machines keep depth-correct self-overlap.
                try {
                    if (canMulti) {
                        if (com.hbm_m.client.render.debug.GlDebugMarkers.ENABLED && com.hbm_m.client.render.debug.GlDebugMarkers.isActive()) {
                            for (int i = 0; i < opaqueSubs.size(); i++) {
                                SubDraw sub = opaqueSubs.get(i);
                                Pending p = sub.owner;
                                String tag = (p != null && p.renderer != null && p.renderer.getMdiTraceTag() != null)
                                        ? p.renderer.getMdiTraceTag()
                                        : (p != null && p.renderer != null ? p.renderer.getClass().getSimpleName() : "unknown");
                                int ic = p != null ? p.indexCount : 0;
                                int bv = p != null ? p.baseVertex : 0;
                                int fi = p != null ? (p.firstIndexBytes >>> 2) : 0;
                                int bi = sub.baseInstance;
                                com.hbm_m.client.render.debug.GlDebugMarkers.insertMarker(i, "SubDraw[" + i + "]: tag=" + tag
                                        + " idxCount=" + ic
                                        + " inst=" + sub.count
                                        + " baseVert=" + bv
                                        + " firstIdx=" + fi
                                        + " baseInst=" + bi);
                            }
                        }
                        if (caps2.glMultiDrawElementsIndirect != 0L) {
                            GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                                    GL11.GL_UNSIGNED_INT, 0L, nCmd, INDIRECT_CMD_STRIDE_BYTES);
                        } else {
                            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES,
                                    GL11.GL_UNSIGNED_INT, 0L, nCmd, INDIRECT_CMD_STRIDE_BYTES);
                        }
                        dispatchDrawMode = "MULTI";
                    } else if (canSingle) {
                        for (int i = 0; i < nCmd; i++) {
                            long cmdOff = (long) i * INDIRECT_CMD_STRIDE_BYTES;
                            if (caps2.glDrawElementsIndirect != 0L) {
                                GL40.glDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, cmdOff);
                            } else {
                                ARBDrawIndirect.glDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, cmdOff);
                            }
                        }
                        dispatchDrawMode = "IND_LOOP";
                    } else {
                        MainRegistry.LOGGER.error(
                                "[HBM-M MDI] Neither glDrawElementsIndirect nor glMultiDrawElementsIndirect available — skipping atlas draw");
                        dispatchDrawMode = "NONE";
                        HardwareTierManager.triggerRuntimeDegradation("MdiBatchCoordinator.executeMdiGlDraw (no indirect entrypoint)", null);
                    }
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("[HBM-M MDI] Draw dispatch failed - triggering runtime degradation", t);
                    dispatchDrawMode = "NONE";
                    HardwareTierManager.triggerRuntimeDegradation("MdiBatchCoordinator.executeMdiGlDraw", t);
                }

                if (!"NONE".equals(dispatchDrawMode)) {
                    NucleusDebug.recordDraw("MULTI".equals(dispatchDrawMode) ? 1 : nCmd,
                            prepared.drawTotalInstances(),
                            "MULTI".equals(dispatchDrawMode) ? "MDI (multi)" : "MDI (indirect loop)");
                }
                logDispatchDiagStatic(prepared.pendingSize, opaqueSubs, prepared.drawTotalInstances,
                        atlas, prepared.droppedNoSlot, dispatchDrawMode, gameTime);
            } finally {
                if (gpuCulled) {
                    atlas.restoreDefaultInstanceVbo();
                }
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

                // Explicitly restore active texture back to unit 0
                GlStateManager._activeTexture(GL13.GL_TEXTURE0);

                // Exiting to VAO 0: force the next vanilla/TACZ immediate draw to
                // rebind its VAO honestly (bindVertexArray(0) does this too - kept
                // explicit so this exit stays safe if the bind call is ever replaced).
                com.mojang.blaze3d.vertex.BufferUploader.invalidate();
                GlVaoSafety.bindVertexArray(0);

                RenderSystem.depthMask(true);
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.enableDepthTest();
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();

                // Explicitly unbind custom shader program to prevent leaking into subsequent passes
                GlStateManager._glUseProgram(0);
                restoreVanillaSolidShader();
            }
        } finally {
            com.hbm_m.client.render.debug.GlDebugMarkers.popGroup();
            // cmdBufScratch is persistent across frames (reused by the next dispatch);
            // freed only at session end via clearCachedRedraw.
        }
    }


    private static void restoreVanillaSolidShader() {
        // Leave the program unbound but invalidate the vanilla static so the next
        // apply() of ANY vanilla shader performs an honest bind: with the static
        // left claiming "solid is current" while program 0 is really bound, a
        // repeated solid apply would skip its bind and draw at program 0.
        GlStateManager._glUseProgram(0);
        com.hbm_m.client.render.shader.ShaderBindResync.invalidateStaticProgramCache();
        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
    }

    private static void applyCommonUniforms(ShaderInstance shader, Matrix4f projection, boolean forceDitherFade) {
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(projection);
        // InstPos/InstRot are anchor-relative: ModelViewMat = R_cam * T(-relCam) (FrameViewState).
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(FrameViewState.viewMatrix());
        var fogStart = shader.getUniform("FogStart");
        if (fogStart != null) fogStart.set(RenderSystem.getShaderFogStart());
        var fogEnd = shader.getUniform("FogEnd");
        if (fogEnd != null) fogEnd.set(RenderSystem.getShaderFogEnd());
        var fogColor = shader.getUniform("FogColor");
        if (fogColor != null) {
            float[] c = RenderSystem.getShaderFogColor();
            fogColor.set(c[0], c[1], c[2], c[3]);
        }
        var fade = shader.getUniform("FadeAlpha");
        if (fade != null) fade.set(1.0f);
        // GPU fade ramp endpoints: the record stores a static fade MODE, the ramp
        // is evaluated per instance in the vertex shader (block_lit_instanced.vsh).
        SingleMeshVboRenderer.applyFadeRampUniforms(shader, forceDitherFade);
    }

    private static void logDispatchDiagStatic(int pendingSize, List<SubDraw> opaqueSubs,
                                              int drawTotalInstances,
                                              MdiGeometryAtlas atlas, int droppedNoSlot, String drawMode, long gameTime) {
        if (!MdiRenderDiag.isDebugEnabled() && !MdiRenderDiag.isVerboseEnabled()) {
            return;
        }
        String summary = String.format(
                "[HBM-M MDI] dispatch gameTime=%s draws=%d/%d droppedNoSlot=%d instances=%d atlasParts=%d mode=%s",
                gameTime == -1L ? "?" : Long.toString(gameTime),
                opaqueSubs.size(), pendingSize, droppedNoSlot, drawTotalInstances,
                atlas.getRegisteredGeometryCount(), drawMode);
        if (MdiRenderDiag.isDebugEnabled() || MdiRenderDiag.isVerboseEnabled()) {
            MainRegistry.LOGGER.info(summary);
        } else {
            MainRegistry.LOGGER.debug(summary);
        }
        if (!MdiRenderDiag.isVerboseEnabled()) {
            return;
        }
        for (SubDraw sub : opaqueSubs) {
            Pending p = sub.owner;
            String tag = p.renderer.getMdiTraceTag();
            if (tag == null) {
                tag = "?";
            }
            MainRegistry.LOGGER.info(
                    "[HBM-M MDI]   sub tag={} rid=0x{} idxCount={} instances={} baseInstance={} baseVertex={} firstIndexBytes={} phase=OPAQUE",
                    tag,
                    Integer.toHexString(System.identityHashCode(p.renderer)),
                    p.indexCount,
                    sub.count,
                    sub.baseInstance,
                    p.baseVertex,
                    p.firstIndexBytes);
        }
    }

    /** Package-visible: NucleusGpuBaker's optional bake Hi-Z phase uses the same
     * depth-source selection (active gbuffer depth attachment under a pack). */
    static int getActiveGbufferDepthTexture() {
        int depthType = org.lwjgl.opengl.GL30.glGetFramebufferAttachmentParameteri(
                org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER,
                org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT,
                org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        if (depthType == org.lwjgl.opengl.GL11.GL_TEXTURE) {
            return org.lwjgl.opengl.GL30.glGetFramebufferAttachmentParameteri(
                    org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER,
                    org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT,
                    org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        }
        return -1;
    }
}

