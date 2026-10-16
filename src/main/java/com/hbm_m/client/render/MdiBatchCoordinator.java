package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

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
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
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
 * <b>Fade ordering (variant G):</b> each instance snapshot is partitioned into
 * [opaque | fading back-to-front] ({@link #partitionOpaqueFirst}), and the command buffer
 * is assembled as [all opaque sub-draws][all fading sub-draws] — executed in a single
 * {@code glMultiDrawElementsIndirect}, but opaque geometry is guaranteed to seal
 * the depth buffer prior to the fading phase. Without this, a distant opaque machine
 * behind a fading machine would fail the depth test (as fading fragments write depth
 * when {@code depthMask(true)}) and the terrain behind it would leak through.
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
    // buffer shifted by re-laying-out. The [opaque|fading] phase order and the global
    // back-to-front sorting of fading instances are expressed via the COMMAND ORDER of the
    // indirect buffer: the cull shader (nucleus_cull.comp) compacts in-place - the
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

    static final class Pending {
        final InstancedStaticPartRenderer renderer;
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
        int baseInstance;
        int instanceCount;
        int[] instanceCullIndices;
        long[] instanceOcclusionKeys;
        FloatBuffer instanceData;
        /** Native buffer allocated with {@code memAllocFloat} in {@link #submit}; freed in {@link #endFrame}. */
        boolean instanceDataNativeOwned;
        /**
         * Boundary between opaque and fading instances after {@link #partitionOpaqueFirst}:
         * records [0, opaqueCount) have fade ≈ 1.0, and [opaqueCount, instanceCount) are fading,
         * sorted back-to-front. -1 indicates partitioning has not yet been executed.
         * instanceCullIndices and instanceOcclusionKeys are NOT reordered: they are not read in MDI.
         */
        int opaqueCount = -1;
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
        /** Minimum fade value of snapshot (computed during partitioning instead of per-frame scan). */
        float cachedMinFade = 1.0f;
        // -- Stable window in the instance atlas -----------------------------
        /** Window base in records (instance units); -1 = window not yet placed. */
        int atlasWindowBase = -1;
        /** Window capacity in records (grows x2 on overflow). */
        int atlasWindowCap = 0;
        /** The current snapshot is already uploaded into the window and the shadow is valid - the upload is skipped. */
        boolean windowUploaded = false;
        Pending(InstancedStaticPartRenderer renderer) { this.renderer = renderer; }
    }

    /**
     * Contiguous range of opaque instances for a single {@link Pending} (opaque phase).
     * Output command buffer: [opaque windows...][fading slots of 1 instance...] in ONE
     * multi-draw; opaque always precedes fading (seals depth prior to blending).
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

    /**
     * Single fading instance in global back-to-front order. The fading phase executes
     * with depth-write enabled: self-overlap within a model must resolve via depth-test,
     * while mutual depth-rejection between machines is eliminated by global camera-distance
     * sorting (distant instances drawn first, near blended on top). One instance per command —
     * commands are inexpensive (32 B), and fading instances typically number in the dozens.
     */
    static final class FadeSlot {
        Pending owner;
        int srcInstance;
        float distSq;
        /** Instance fade value (for minFade aggregation without reading snapshot data). */
        float fade = 1.0f;
        int baseInstance;
        FadeSlot(Pending owner, int srcInstance, float distSq, int baseInstance) {
            this.owner = owner;
            this.srcInstance = srcInstance;
            this.distSq = distSq;
            this.baseInstance = baseInstance;
        }

        /** Pool reuse: allocation only when the fading instance count grows. */
        void set(Pending owner, int srcInstance, float distSq, int baseInstance) {
            this.owner = owner;
            this.srcInstance = srcInstance;
            this.distSq = distSq;
            this.baseInstance = baseInstance;
        }
    }

    private final Matrix4f projectionMatrix;
    private final List<Pending> pending = new ArrayList<>(16);
    private int totalInstances = 0;

    // -- Frame scratch (rendering is single-threaded): pools instead of new lists --
    // PreparedMdi holds these lists only within dispatch() - by the next
    // frame it is dead. SubDraw/FadeSlot objects are reused: allocation only
    // when the window/fading-instance count grows.
    private final ArrayList<Pending> drawListScratch = new ArrayList<>(64);
    private final ArrayList<SubDraw> subDrawPool = new ArrayList<>(64);
    private final ArrayList<FadeSlot> fadeSlotPool = new ArrayList<>(64);
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
            } catch (Throwable t) {
                hasDrawIndirect = false;
                hasBaseInstance = false;
            }
            capsResolved = true;
        }
    }

    public static boolean isMdiAvailable() {
        ensureCapsResolved();
        boolean ok = hasDrawIndirect && hasBaseInstance;
        if (!loggedOnce) {
            loggedOnce = true;
            try {
                String vendor = GL11.glGetString(GL11.GL_VENDOR);
                String renderer = GL11.glGetString(GL11.GL_RENDERER);
                String version = GL11.glGetString(GL11.GL_VERSION);
                if (ok) {
                    MainRegistry.LOGGER.info(
                            "[HBM-M MDI] Draw indirect + base_instance available (atlas batch path). GL_VENDOR='{}', GL_RENDERER='{}', GL_VERSION='{}'",
                            vendor, renderer, version);
                } else {
                    MainRegistry.LOGGER.info(
                            "[HBM-M MDI] Draw indirect/base_instance NOT available (hasDrawIndirect={}, hasBaseInstance={}) — vanilla instanced path. GL_VENDOR='{}', GL_RENDERER='{}', GL_VERSION='{}'",
                            hasDrawIndirect, hasBaseInstance, vendor, renderer, version);
                }
            } catch (Throwable ignored) {
                // GL string queries can fail very early; safe to skip the log line.
            }
        }
        return ok;
    }

    public static MdiBatchCoordinator beginFrame(Matrix4f projectionMatrix) {
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            return null;
        }
        if (!isMdiAvailable()) return null;

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
            s.totalInstances = 0;
            ACTIVE.remove();
        }
    }

    public void endFrame() {
        try {
            dispatch();
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] dispatch failed; future flushes will use legacy path", t);
        } finally {
            // Pending records persist in retainedList; snapshots are needed for the next frame. Only reset session.
            pending.clear();
            totalInstances = 0;
            if (ACTIVE.get() == this) ACTIVE.remove();
        }
    }

    public static void clearCachedRedraw() {
        // Dimension change / resource reload / atlas reset: retained records are invalidated.
        dropAllRetained();
    }

    private record PreparedMdi(List<Pending> drawList, List<SubDraw> opaqueSubs, List<FadeSlot> fadeSlots,
                               int drawTotalInstances, int pendingSize, int droppedNoSlot, float minFade) {}

    private static void freeDrawListInstanceBuffers(List<Pending> drawList) {
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            if (p.instanceDataNativeOwned && p.instanceData != null) {
                MemoryUtil.memFree(p.instanceData);
                p.instanceData = null;
                p.instanceDataNativeOwned = false;
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
    }

    /** Removes record from retained list/map and returns snapshot buffer to pool. */
    private static void uninstallRetained(Pending p) {
        Pending mapped = retainedByRenderer.get(p.renderer);
        if (mapped == p) {
            retainedByRenderer.remove(p.renderer);
        }
        releaseWindow(p);
        recycleSnapshot(p);
    }

    /** Drops retained records without assertion during frame — renderer became culled or unloaded. */
    private static void dropUnassertedRetained() {
        for (Iterator<Pending> it = retainedList.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.assertedFrame != retainedFrameStamp) {
                uninstallRetained(p);
                it.remove();
            }
        }
    }

    /** Complete purge of retained state (world change, resource reload, atlas reset). */
    private static void dropAllRetained() {
        for (int i = 0; i < retainedList.size(); i++) {
            Pending mapped = retainedByRenderer.get(retainedList.get(i).renderer);
            if (mapped == retainedList.get(i)) {
                retainedByRenderer.remove(retainedList.get(i).renderer);
            }
            recycleSnapshot(retainedList.get(i));
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
        } catch (Throwable t) {
            recyclePooled(instanceSnapshot);
            MainRegistry.LOGGER.warn("[HBM-M MDI] instance snapshot alloc/copy failed: {}", t.toString());
            return false;
        }

        Pending p = new Pending(renderer);
        p.baseVertex = slot.baseVertex;
        p.firstIndexBytes = slot.firstIndexBytes;
        p.indexCount = slot.indexCount;
        p.submitBaseVertex = slot.baseVertex;
        p.submitFirstIndexBytes = slot.firstIndexBytes;
        p.submitIndexCount = slot.indexCount;
        p.baseInstance = totalInstances;
        p.instanceCount = instanceCount;
        p.dirtySubmittedFrame = retainedFrameStamp;
        p.instanceData = instanceSnapshot;
        p.instanceDataNativeOwned = true;
        if (sourceCullIndices != null && sourceCullIndices.length >= instanceCount) {
            p.instanceCullIndices = new int[instanceCount];
            System.arraycopy(sourceCullIndices, 0, p.instanceCullIndices, 0, instanceCount);
        } else {
            p.instanceCullIndices = null;
        }
        if (sourceOcclusionKeys != null && sourceOcclusionKeys.length >= instanceCount) {
            p.instanceOcclusionKeys = new long[instanceCount];
            System.arraycopy(sourceOcclusionKeys, 0, p.instanceOcclusionKeys, 0, instanceCount);
        } else {
            p.instanceOcclusionKeys = null;
        }

        // Retained setup: dirty submit replaces renderer record IN-PLACE
        // (stable list order -> stable upload offsets for remaining entries).
        // Secondary submit of the same renderer in one frame (Embeddium multi-pass):
        // max-submit rule — keep the fullest submit of this frame.
        Pending prev = retainedByRenderer.put(renderer, p);
        if (prev != null) {
            int idx = retainedList.indexOf(prev);
            if (prev.dirtySubmittedFrame == retainedFrameStamp
                    && prev.instanceCount >= instanceCount) {
                // First submit this frame was larger / more complete — retain it.
                retainedByRenderer.put(renderer, prev);
                if (idx >= 0) {
                    retainedList.set(idx, prev);
                }
                recycleSnapshot(p);
                p = prev;
            } else {
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
                p.windowUploaded = false;
                prev.atlasWindowBase = -1;
                prev.atlasWindowCap = 0;
                recycleSnapshot(prev);
            }
        } else {
            retainedList.add(p);
        }
        p.assertedFrame = retainedFrameStamp;
        p.partitionValid = false;

        pending.add(p);
        totalInstances += instanceCount;
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
        } catch (Throwable ignored) {
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
        totalInstances = 0;

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

        // Variant G+: partition [opaque | fading back-to-front] — ONLY for records
        // with new snapshots; clean records reuse previous frame's partition and caches.
        int cleanRenderers = 0;
        int cleanInstances = 0;
        int drawTotalInstances = 0;
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            if (!p.partitionValid) {
                partitionOpaqueFirst(p, instanceFloatsPerInstance, instanceFadeOffset);
                p.cachedMinFade = computeCachedMinFade(p, instanceFloatsPerInstance, instanceFadeOffset);
                p.partitionValid = true;
            } else {
                cleanRenderers++;
                cleanInstances += p.instanceCount;
            }
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
            anyAlloc = true;
        }
        if (anyAlloc) {
            int base = 0;
            for (int i = 0; i < drawList.size(); i++) {
                Pending p = drawList.get(i);
                if (p.atlasWindowBase != base) {
                    p.atlasWindowBase = base;
                    p.windowUploaded = false;
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

        // Opaque phase: one command per window (base = the window's stable base).
        // Fading: a flat global list sorted by camera distance - the back-to-front
        // order is carried by the COMMAND ORDER (the cull shader compacts
        // in-place: compacted position = input position), data does not move.
        // A fading instance's baseInstance = window base + record index in the snapshot.
        // Lists and objects are pooled: allocation only on growth (see the scratch fields).
        int subCount = 0;
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            if (p.opaqueCount > 0) {
                SubDraw s;
                if (subCount < subDrawPool.size()) {
                    s = subDrawPool.get(subCount);
                } else {
                    s = new SubDraw(null, 0, 0, 0);
                    subDrawPool.add(s);
                }
                subCount++;
                s.set(p, 0, p.opaqueCount, p.atlasWindowBase);
            }
        }
        List<SubDraw> opaqueSubs = subDrawPool.subList(0, subCount);

        float camX = FrameViewState.relCamX();
        float camY = FrameViewState.relCamY();
        float camZ = FrameViewState.relCamZ();
        int fadeCount = 0;
        for (int di = 0; di < drawList.size(); di++) {
            Pending p = drawList.get(di);
            if (p.instanceData == null) continue;
            int first = Math.max(0, p.opaqueCount);
            for (int i = first; i < p.instanceCount; i++) {
                int base = i * instanceFloatsPerInstance;
                float dx = p.instanceData.get(base) - camX;
                float dy = p.instanceData.get(base + 1) - camY;
                float dz = p.instanceData.get(base + 2) - camZ;
                FadeSlot s;
                if (fadeCount < fadeSlotPool.size()) {
                    s = fadeSlotPool.get(fadeCount);
                } else {
                    s = new FadeSlot(null, 0, 0f, 0);
                    fadeSlotPool.add(s);
                }
                fadeCount++;
                s.set(p, i, dx * dx + dy * dy + dz * dz, p.atlasWindowBase + i);
                s.fade = p.instanceData.get(base + instanceFadeOffset);
            }
        }
        fadeSlotPool.subList(0, fadeCount).sort((a, b) -> Float.compare(b.distSq, a.distSq));
        List<FadeSlot> fadeSlots = fadeSlotPool.subList(0, fadeCount);

        if (!uploadWindowsToAtlas(drawList, atlas, instanceFloatsPerInstance)) {
            freeDrawListInstanceBuffers(drawList);
            dropAllRetained();
            return null;
        }

        return new PreparedMdi(drawList, opaqueSubs, fadeSlots, drawTotalInstances, pendingSize, droppedNoSlot, 1f);
    }

    /** Minimum fade value of snapshot (opaque region >= threshold by construction; fading tail suffices). */
    private static float computeCachedMinFade(Pending p, int floatsPerInstance, int fadeOffset) {
        float min = 1.0f;
        if (p.instanceData == null) {
            return min;
        }
        int first = Math.max(0, p.opaqueCount);
        for (int i = first; i < p.instanceCount; i++) {
            float fa = p.instanceData.get(i * floatsPerInstance + fadeOffset);
            if (fa < min) {
                min = fa;
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
        boolean shadowValid = atlas.isInstanceShadowValid();
        for (int i = 0; i < drawList.size(); i++) {
            Pending p = drawList.get(i);
            if (p.instanceData == null) {
                return false;
            }
            int floats = p.instanceCount * instanceFloatsPerInstance;
            if (p.instanceData.remaining() < floats) {
                return false;
            }
            if (shadowValid && p.windowUploaded) {
                continue;
            }
            atlas.uploadInstanceWindowSpanned(p.atlasWindowBase * instanceFloatsPerInstance,
                    p.instanceData, 0, floats);
            p.windowUploaded = true;
        }
        atlas.markInstanceShadowValid();
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
        List<FadeSlot> fadeSlots = prepared.fadeSlots();
        if (drawList.isEmpty() || (opaqueSubs.isEmpty() && fadeSlots.isEmpty())) {
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

        int nCmd = opaqueSubs.size() + fadeSlots.size();
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
        // Command order matches upload order (baseInstance synchronized):
        // [opaque windows...][fading slots globally back-to-front].
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
            while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                cmdBuf.putInt(0);
            }
            totalInstances += sub.count;
        }
        for (int i = 0; i < fadeSlots.size(); i++) {
            FadeSlot s = fadeSlots.get(i);
            Pending p = s.owner;
            int rowStart = cmdBuf.position();
            cmdBuf.putInt(p.indexCount);
            cmdBuf.putInt(gpuCullActive ? 0 : 1);
            cmdBuf.putInt(p.firstIndexBytes >>> 2);
            cmdBuf.putInt(p.baseVertex);
            cmdBuf.putInt(s.baseInstance);
            cmdBuf.putInt(1);
            while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                cmdBuf.putInt(0);
            }
            totalInstances++;
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

        try {
            int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int prevArrayBuf = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            int prevIndirectBuf = 0;
            try {
                prevIndirectBuf = GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING);
            } catch (Throwable ignored) {
            }
            boolean cullWas = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            boolean depthTestWas = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean depthMaskWas = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            boolean blendWas = GL11.glIsEnabled(GL11.GL_BLEND);
            int prevSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            int prevDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            int prevSrcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            int prevDstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            boolean primitiveRestartWas = false;
            try {
                primitiveRestartWas = GL11.glIsEnabled(GL31.GL_PRIMITIVE_RESTART);
            } catch (Throwable ignored) {
            }

            String dispatchDrawMode = "MULTI";
            boolean gpuCulled = false;

            try {
                GL30.glBindVertexArray(atlas.getVaoId());
                atlas.enableVertexAttribArraysOnBoundVao();

                // Lightmap freshness is owned by RenderFrameLight.ensureLightTextureUpdated()
                // (present start) on top of vanilla's own updateLightTexture at the start of
                // GameRenderer.renderLevel — an extra rebuild here was a per-frame duplicate
                // of a 256×256 CPU upload.

                RenderSystem.setShader(() -> shader);
                applyCommonUniforms(shader, projection);
                // REGRESSION-STOP (MDI): same block_lit contract as VanillaInstancedBatchRenderer — otherwise white batches.
                SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
                shader.apply();
                InstancedStaticPartRenderer.bindBlockLitTexturesBeforeDraw(shader);

                float minFade = prepared.minFade();
                // Cached minima: cachedMinFade precomputed during snapshot partition,
                // fade of individual slots captured during enumeration — per-frame
                // strided scan of all instances is unnecessary.
                for (int i = 0; i < opaqueSubs.size(); i++) {
                    SubDraw sub = opaqueSubs.get(i);
                    if (sub.owner.instanceData != null && sub.owner.cachedMinFade < minFade) {
                        minFade = sub.owner.cachedMinFade;
                    }
                }
                for (int i = 0; i < fadeSlots.size(); i++) {
                    if (fadeSlots.get(i).fade < minFade) {
                        minFade = fadeSlots.get(i).fade;
                    }
                }

                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();
                if (minFade < 0.99f) {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                }

                if (primitiveRestartWas) {
                    GL11.glDisable(GL31.GL_PRIMITIVE_RESTART);
                }

                // REGRESSION-FIX: capacity must accommodate ALL commands (nCmd =
                // opaque sub-draws + 1-instance fading slots), not just renderer count —
                // otherwise growing fading instances under-ensured buffer and glBufferSubData truncated with GL_INVALID_VALUE.
                int cmdByteLen = nCmd * INDIRECT_CMD_STRIDE_BYTES;
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, atlas.getIndirectBufferId());
                atlas.ensureIndirectCommandByteCapacity(cmdByteLen);
                GL15.glBufferSubData(GL40.GL_DRAW_INDIRECT_BUFFER, 0, cmdBuf);

                if (gpuCullActive) {
                    try {
                        // Full pyramid rebuild - only when occlusion is enabled by the gate;
                        // in frustum-only mode the culler does not sample the pyramid (it holds
                        // the previous-frame/placeholder one), saving a full SPD screen downsample.
                        if (occlusionEnabled) {
                            HiZDepthPyramid.get().regenerate(target.getDepthTextureId(), target.width, target.height);
                        }
                        float zFar = mc.gameRenderer != null ? mc.gameRenderer.getDepthFar() : 1024.0f;
                        gpuCulled = NucleusGpuCuller.cull(
                                atlas,
                                nCmd,
                                HiZDepthPyramid.get(),
                                FrameViewState.viewMatrix(),
                                projection,
                                0.05f,
                                zFar,
                                target.width,
                                target.height,
                                !occlusionEnabled
                        );
                    } catch (Throwable t) {
                        MainRegistry.LOGGER.warn("[HBM-M MDI] GPU occlusion culling failed, falling back to full batch: {}", t.getMessage());
                        gpuCulled = false;
                    }
                    
                    // RESTORE SHADER PROGRAM OVERWRITTEN BY COMPUTE PASSES
                    GL20.glUseProgram(shader.getId());

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
                            } catch (Throwable t) {
                                MainRegistry.LOGGER.warn("[HBM-M GPU Cull] Diagnostic readback failed: {}", t.getMessage());
                            }
                        }
                    } else {
                        // Safe CPU fallback / unculled restoration:
                        // Rewrite original instance counts to indirect buffer and retain default instance VBO
                        cmdBuf.clear();
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
                            while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                                cmdBuf.putInt(0);
                            }
                        }
                        for (int i = 0; i < fadeSlots.size(); i++) {
                            FadeSlot s = fadeSlots.get(i);
                            Pending p = s.owner;
                            int rowStart = cmdBuf.position();
                            cmdBuf.putInt(p.indexCount);
                            cmdBuf.putInt(1);
                            cmdBuf.putInt(p.firstIndexBytes >>> 2);
                            cmdBuf.putInt(p.baseVertex);
                            cmdBuf.putInt(s.baseInstance);
                            cmdBuf.putInt(1);
                            while (cmdBuf.position() < rowStart + INDIRECT_CMD_STRIDE_BYTES) {
                                cmdBuf.putInt(0);
                            }
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
                try {
                    GLCapabilities capsBarrier = GL.getCapabilities();
                    if (capsBarrier != null && capsBarrier.glMemoryBarrier != 0L) {
                        GL42.glMemoryBarrier(GL42.GL_COMMAND_BARRIER_BIT);
                    }
                } catch (Throwable ignored) {
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

                // Depth is written during BOTH phases: opaque seals volume, fading
                // provides self-overlap inside model (spikes inside casing).
                // Mutual depth-rejection of fading machines is prevented by global
                // back-to-front sorting of fadeSlots (distant in command buffer before near).
                if (canMulti) {
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
                }

                if (minFade < 0.99f) {
                    RenderSystem.disableBlend();
                }
                if (!"NONE".equals(dispatchDrawMode)) {
                    NucleusDebug.recordDraw("MULTI".equals(dispatchDrawMode) ? 1 : nCmd,
                            prepared.drawTotalInstances(),
                            "MULTI".equals(dispatchDrawMode) ? "MDI (multi)" : "MDI (indirect loop)");
                }
                logDispatchDiagStatic(prepared.pendingSize, opaqueSubs, fadeSlots, prepared.drawTotalInstances,
                        atlas, prepared.droppedNoSlot, dispatchDrawMode, gameTime);
            } finally {
                if (gpuCulled) {
                    atlas.restoreDefaultInstanceVbo();
                }
                if (primitiveRestartWas) {
                    GL11.glEnable(GL31.GL_PRIMITIVE_RESTART);
                }
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, prevIndirectBuf);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuf);
                GL30.glBindVertexArray(prevVao);
                RenderSystem.depthMask(depthMaskWas);
                RenderSystem.depthFunc(prevDepthFunc);
                if (depthTestWas) {
                    RenderSystem.enableDepthTest();
                } else {
                    RenderSystem.disableDepthTest();
                }
                if (cullWas) {
                    RenderSystem.enableCull();
                } else {
                    RenderSystem.disableCull();
                }
                RenderSystem.blendFuncSeparate(prevSrcRgb, prevDstRgb, prevSrcA, prevDstA);
                if (blendWas) {
                    RenderSystem.enableBlend();
                } else {
                    RenderSystem.disableBlend();
                }
                restoreVanillaSolidShader();
            }
        } finally {
            // cmdBufScratch is persistent - do not free (reused by the next dispatch).
        }
    }

    private static void restoreVanillaSolidShader() {
        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
    }

    /**
     * Reorders snapshot records to [opaque in original order | fading back-to-front]
     * and sets {@link Pending#opaqueCount}.
     *
     * <p>Logic delegates to shared helper {@link InstancedStaticPartRenderer#partitionInstancesOpaqueFirst}:
     * direct path (GPU bones chain parts) partitions its buffers with identical code
     * so phase order (opaque before fading) matches across all render paths.
     */
    private static void partitionOpaqueFirst(Pending p, int floatsPerInstance, int fadeOffset) {
        p.opaqueCount = 0;
        if (p.instanceData == null || p.instanceCount <= 0) {
            return;
        }
        p.opaqueCount = InstancedStaticPartRenderer.partitionInstancesOpaqueFirst(
                p.instanceData, p.instanceCount, floatsPerInstance, fadeOffset, null);
    }

    private static void applyCommonUniforms(ShaderInstance shader, Matrix4f projection) {
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
    }

    private static void logDispatchDiagStatic(int pendingSize, List<SubDraw> opaqueSubs, List<FadeSlot> fadeSlots,
                                              int drawTotalInstances,
                                              MdiGeometryAtlas atlas, int droppedNoSlot, String drawMode, long gameTime) {
        if (!MdiRenderDiag.isDebugEnabled() && !MdiRenderDiag.isVerboseEnabled()) {
            return;
        }
        String summary = String.format(
                "[HBM-M MDI] dispatch gameTime=%s draws=%d/%d droppedNoSlot=%d instances=%d atlasParts=%d mode=%s",
                gameTime == -1L ? "?" : Long.toString(gameTime),
                opaqueSubs.size() + fadeSlots.size(), pendingSize, droppedNoSlot, drawTotalInstances,
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
        for (FadeSlot s : fadeSlots) {
            Pending p = s.owner;
            String tag = p.renderer.getMdiTraceTag();
            if (tag == null) {
                tag = "?";
            }
            MainRegistry.LOGGER.info(
                    "[HBM-M MDI]   sub tag={} rid=0x{} idxCount={} instances=1 baseInstance={} baseVertex={} firstIndexBytes={} phase=FADE distSq={}",
                    tag,
                    Integer.toHexString(System.identityHashCode(p.renderer)),
                    p.indexCount,
                    s.baseInstance,
                    p.baseVertex,
                    p.firstIndexBytes,
                    String.format(java.util.Locale.ROOT, "%.1f", s.distSq));
        }
    }
}
