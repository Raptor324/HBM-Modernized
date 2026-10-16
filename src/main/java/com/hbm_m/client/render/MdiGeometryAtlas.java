package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;

/**
 * Shared GPU geometry atlas backing the {@link MdiBatchCoordinator}.
 * <p>
 * Holds:
 * <ul>
 *   <li>One vertex VBO ({@code GL_ARRAY_BUFFER}) containing the concatenated
 *       per-part vertex bytes (pos vec3 / normal vec3 / uv vec2 / int bone_id, stride 36).</li>
 *   <li>One index EBO ({@code GL_ELEMENT_ARRAY_BUFFER}) with the concatenated
 *       per-part {@code GL_UNSIGNED_INT} indices. Per-part draw commands use
 *       {@code baseVertex} (added to each element index) and a <b>byte offset</b>
 *       into the EBO for {@code glDrawElementsInstancedBaseVertexBaseInstance};
 *       for {@code DrawElementsIndirectCommand.firstIndex} in the indirect buffer,
 *       the same offset in <b>elements</b> (bytes / 4) is required. Local part indices
 *       remain as-is (0..N-1 per part).</li>
 *   <li>One instance VBO with the unsliced compressed instance layout
 *       ({@link InstancedStaticPartRenderer} loc 4..15 with divisor 1),
 *       large enough to hold the sum of all per-renderer
 *       {@code MAX_INSTANCES} budgets for a single frame.</li>
 *   <li>One indirect buffer for the {@link MdiBatchCoordinator} to stream
 *       commands into.</li>
 *   <li>One VAO with all of the above pre-bound.</li>
 * </ul>
 * <p>
 * Geometry registration is <b>lazy and growable</b>: each
 * {@link InstancedStaticPartRenderer} that first becomes MDI-eligible calls
 * {@link #registerGeometryIfAbsent} with a copy of its vertex bytes + index
 * stream. When the existing vertex/index buffers don't have room, we
 * reallocate at double capacity and re-upload all known parts. This is rare
 * (happens only on first-frame growth) and amortised across the session.
 *
 * @credit Flywheel / CrankShaft
 */

@OnlyIn(Dist.CLIENT)
public final class MdiGeometryAtlas {

    /** Vertex stride in bytes — pos vec3 + normal vec3 + uv vec2 + int bone_id = 36. */
    private static final int VERTEX_STRIDE_BYTES = SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES;

    /** Unsliced instance layout: 32 floats (128 B) per instance (see {@link InstancedStaticPartRenderer}). */
    private static final int INSTANCE_FLOATS = InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
    /** Float index of fade packed in {@code InstBboxSize.w} (single source of truth). */
    private static final int INSTANCE_FADE_FLOAT_OFFSET = InstancedStaticPartRenderer.FADE_FLOAT_OFFSET;

    private static volatile MdiGeometryAtlas INSTANCE;

    /** Slot record returned to the coordinator. */
    public static final class Slot {
        public final int baseVertex;
        /** Byte offset in EBO for sequential draws; for indirect draws — {@code firstIndexBytes / 4} in the {@code firstIndex} field. */
        public final int firstIndexBytes;
        public final int indexCount;
        Slot(int baseVertex, int firstIndexBytes, int indexCount) {
            this.baseVertex = baseVertex;
            this.firstIndexBytes = firstIndexBytes;
            this.indexCount = indexCount;
        }
    }

    /** Cached per-renderer geometry record (shares native buffer views with InstancedStaticPartRenderer). */
    private static final class GeoRecord {
        final ByteBuffer vertexBytesView;
        final IntBuffer indicesView;
        final int registeredVertexBytesLen;
        final int registeredIndexCount;
        Slot slot;
        GeoRecord(ByteBuffer vb, IntBuffer ib, int registeredVertexBytesLen, int registeredIndexCount) {
            this.vertexBytesView = vb;
            this.indicesView = ib;
            this.registeredVertexBytesLen = registeredVertexBytesLen;
            this.registeredIndexCount = registeredIndexCount;
        }
    }

    private boolean ready = false;
    private int vaoId = 0;
    private int vertexVboId = 0;
    private int indexEboId = 0;
    private int instanceVboId = 0;
    private int compactedInstanceVboId = 0;
    private int indirectBufId = 0;

    /**
     * Coarse validity gate for the atlas instance VBO windows: true while the
     * coordinator may SKIP re-uploading windows it marked {@code windowUploaded}
     * (nothing since the last successful upload pass invalidated the mirror -
     * capacity growth, anchor shift, atlas reset). Fine-grained span diffing
     * happens per window in {@link #uploadInstanceWindowSpanned} via the
     * coordinator-held CPU shadow of each window (GpuSpanUploader.diffUpload);
     * this flag gates the whole-window skip only.
     */
    private boolean instanceWindowsValid = false;
    /**
     * Pending-geometry-layout flag: an eviction (or a re-register removal) deferred
     * the full {@link #repackGeometryAndRefreshSlots}. Set by {@link #evictRendererLocked},
     * consumed (single repack) by the next slot consumer - {@link #getCurrentSlot} /
     * {@link #registerGeometryIfAbsent}. Batching a mass-cleanup wave into ONE repack
     * avoids the O(N^2) re-upload of all remaining parts (one full repack per eviction).
     */
    private boolean geometryRepackPending = false;

    // Allocated byte capacities and current high-water marks.
    private long vertexCapBytes = 0;
    private long vertexUsedBytes = 0;
    private long indexCapBytes = 0;
    private long indexUsedBytes = 0;
    private long instanceCapInstances = 0;
    /** Capacity of GL_DRAW_INDIRECT_BUFFER in bytes; commands are written via {@link GL15#glBufferSubData}. */
    private long indirectCmdCapBytes = 0L;

    /**
     * Per-renderer geometry cache. {@link LinkedHashMap} preserves registration order so
     * buffer repacks after growth stay aligned with stored {@link Slot}s.
     */
    private final Map<InstancedStaticPartRenderer, GeoRecord> geometryByRenderer = new LinkedHashMap<>();

    private MdiGeometryAtlas() { /* lazy init below */ }

    /** Current atlas instance without lazy creation (for diagnostics; null if not yet created). */
    public static MdiGeometryAtlas peekOrNull() {
        return INSTANCE;
    }

    public static MdiGeometryAtlas getOrCreate() {
        MdiGeometryAtlas inst = INSTANCE;
        if (inst != null) return inst;
        synchronized (MdiGeometryAtlas.class) {
            inst = INSTANCE;
            if (inst != null) return inst;
            inst = new MdiGeometryAtlas();
            try {
                inst.initialise();
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("[HBM-M MDI] MdiGeometryAtlas init failed; MDI path disabled", t);
                inst.ready = false;
            }
            INSTANCE = inst;
            return inst;
        }
    }

    /**
     * Complete reset of atlas on F3+T / disconnect. Render thread only.
     * <p>
     * Without this, {@link #geometryByRenderer} indefinitely retains old
     * {@link InstancedStaticPartRenderer} instances as keys: after {@code clearCaches()}
     * there are no longer references from BER, but the Map prevents GC collection and
     * accumulates duplicate geometry copies for new renderer instances —
     * causing incorrect mesh binding, polygon holes, and complete MDI failure until re-logging.
     */
    public static void resetForResourceLifecycle() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(MdiGeometryAtlas::resetForResourceLifecycle);
            return;
        }
        synchronized (MdiGeometryAtlas.class) {
            MdiGeometryAtlas inst = INSTANCE;
            if (inst == null) return;
            try {
                inst.destroyInternal();
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("[HBM-M MDI] MdiGeometryAtlas reset failed", t);
            } finally {
                INSTANCE = null;
            }
        }
    }

    private void destroyInternal() {
        // Native memory is owned by InstancedStaticPartRenderer and freed during its cleanup().
        // Duplicate memFree is unnecessary here since GeoRecord stores a view slice without separate allocation.
        geometryByRenderer.clear();
        vertexUsedBytes = 0L;
        indexUsedBytes = 0L;
        instanceWindowsValid = false;
        geometryRepackPending = false;

        // Same for the GL names: a failed initialise() (ready=false) can leave gen'd
        // objects behind. Delete them before the early return — fields are 0-initialised
        // and guarded != 0, so a never-generated name is a no-op (GL 4.6 spec §2.6.1.1:
        // gen'd names stay marked used until deleted).
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            if (vaoId != 0) GL30.glDeleteVertexArrays(vaoId);
            if (vertexVboId != 0) GL15.glDeleteBuffers(vertexVboId);
            if (indexEboId != 0) GL15.glDeleteBuffers(indexEboId);
            if (instanceVboId != 0) GL15.glDeleteBuffers(instanceVboId);
            if (compactedInstanceVboId != 0) GL15.glDeleteBuffers(compactedInstanceVboId);
            if (indirectBufId != 0) GL15.glDeleteBuffers(indirectBufId);
        } finally {
            guard.restore(vaoId);
        }
        vaoId = 0;
        vertexVboId = 0;
        indexEboId = 0;
        instanceVboId = 0;
        compactedInstanceVboId = 0;
        indirectBufId = 0;

        if (!ready) {
            return;
        }
        vertexCapBytes = 0L;
        indexCapBytes = 0L;
        instanceCapInstances = 0L;
        indirectCmdCapBytes = 0L;
        ready = false;
        MainRegistry.LOGGER.info("[HBM-M MDI] MdiGeometryAtlas reset (resource lifecycle)");
    }

    private void initialise() {
        if (!RenderSystem.isOnRenderThread()) {
            MainRegistry.LOGGER.warn("[HBM-M MDI] Atlas init invoked off render thread; deferring");
            return;
        }
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            vaoId = GL30.glGenVertexArrays();
            vertexVboId = GL15.glGenBuffers();
            indexEboId = GL15.glGenBuffers();
            instanceVboId = GL15.glGenBuffers();
            compactedInstanceVboId = GL15.glGenBuffers();
            indirectBufId = GL15.glGenBuffers();

            com.hbm_m.client.render.debug.GlDebugMarkers.labelVao(vaoId, "HBM_MdiAtlas_VAO");
            com.hbm_m.client.render.debug.GlDebugMarkers.labelBuffer(vertexVboId, "HBM_MdiAtlas_Vertex_VBO");
            com.hbm_m.client.render.debug.GlDebugMarkers.labelBuffer(indexEboId, "HBM_MdiAtlas_Index_EBO");
            com.hbm_m.client.render.debug.GlDebugMarkers.labelBuffer(instanceVboId, "HBM_MdiAtlas_Instance_VBO");
            com.hbm_m.client.render.debug.GlDebugMarkers.labelBuffer(compactedInstanceVboId, "HBM_MdiAtlas_CompactedInstance_VBO");
            com.hbm_m.client.render.debug.GlDebugMarkers.labelBuffer(indirectBufId, "HBM_MdiAtlas_Indirect_BUF");

            if (vaoId == 0 || vertexVboId == 0 || indexEboId == 0 || instanceVboId == 0 || compactedInstanceVboId == 0 || indirectBufId == 0) {
                throw new IllegalStateException("Failed to generate one or more atlas GL objects");
            }

            // Start with a modest 256 KB vertex / 64 KB index allocation; grow on demand.
            vertexCapBytes = 256L * 1024L;
            indexCapBytes = 64L * 1024L;
            instanceCapInstances = 4096L; // grow on demand
            instanceWindowsValid = false;

            GL30.glBindVertexArray(vaoId);

            // Vertex buffer + per-vertex attribute pointers (pos / normal / uv).
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexCapBytes, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, VERTEX_STRIDE_BYTES, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, VERTEX_STRIDE_BYTES, 12);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, VERTEX_STRIDE_BYTES, 24);
            GL30.glEnableVertexAttribArray(3);
            GL30.glVertexAttribIPointer(3, 1, GL11.GL_INT, VERTEX_STRIDE_BYTES, 32);

            // Index buffer.
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexEboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexCapBytes, GL15.GL_STATIC_DRAW);

            // Compacted instance VBO (allocated to same capacity for GPU culling stream compaction).
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, compactedInstanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceCapInstances * INSTANCE_FLOATS * 4L, GL15.GL_STREAM_DRAW);

            // Instance VBO + per-instance attribute pointers (loc 4..15), all
            // with divisor 1 — must mirror the unsliced layout in
            // InstancedStaticPartRenderer EXACTLY (compressed record: quat = 4x
            // GL_SHORT normalized, corner light = 4x GL_UNSIGNED_BYTE normalized).
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceCapInstances * INSTANCE_FLOATS * 4L, GL15.GL_STREAM_DRAW);
            int stride = INSTANCE_FLOATS * 4;
            // InstPos vec3 @ 0
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, 0);
            InstancedGlCompat.glVertexAttribDivisorCompat(4, 1);
            // InstRot: 4 snorm16 halves @ 12 (normalized -> [-1,1] floats in the vsh)
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, true, stride, InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(5, 1);
            // InstBboxMin vec3 @ 20
            GL20.glEnableVertexAttribArray(6);
            GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.BBOX_MIN_FLOAT_OFFSET * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(6, 1);
            // InstBboxSize vec4 (xyz + fade w) @ 32
            GL20.glEnableVertexAttribArray(7);
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.BBOX_SIZE_FLOAT_OFFSET * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(7, 1);
            // Corner light: 4 packed u8 words @ 48..60 (normalized -> x240 in the vsh)
            GL20.glEnableVertexAttribArray(8);
            GL20.glVertexAttribPointer(8, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET + 0) * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(8, 1);
            GL20.glEnableVertexAttribArray(9);
            GL20.glVertexAttribPointer(9, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET + 1) * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(9, 1);
            GL20.glEnableVertexAttribArray(10);
            GL20.glVertexAttribPointer(10, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET + 2) * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(10, 1);
            GL20.glEnableVertexAttribArray(11);
            GL20.glVertexAttribPointer(11, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET + 3) * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(11, 1);
            // InstUvRect vec4 @ 64 (sprite-local VBO → atlas remap)
            GL20.glEnableVertexAttribArray(12);
            GL20.glVertexAttribPointer(12, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.UVRECT_FLOAT_OFFSET * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(12, 1);
            // InstColor vec4 @ 80 (per-instance RGBA tint; white = passthrough)
            GL20.glEnableVertexAttribArray(13);
            GL20.glVertexAttribPointer(13, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.TINT_FLOAT_OFFSET * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(13, 1);
            // GradParams vec4 @ 96 (spatial tint falloff; axis<0 = off; w = joint chain base)
            GL20.glEnableVertexAttribArray(14);
            GL20.glVertexAttribPointer(14, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.GRAD_FLOAT_OFFSET * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(14, 1);
            // AnimParams vec4 @ 112 (parametric GPU animation: 4 per-frame chain
            // parameters, packed in node declaration order; the chain base index
            // lives in GradParams.w)
            GL20.glEnableVertexAttribArray(15);
            GL20.glVertexAttribPointer(15, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.ANIM_FLOAT_OFFSET * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(15, 1);

            // Indirect: allocate capacity once; each frame only updates via glBufferSubData (see MdiBatchCoordinator).
            long initialIndirectBytes = 4096L * (long) MdiBatchCoordinator.INDIRECT_CMD_STRIDE_BYTES;
            GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, indirectBufId);
            GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, initialIndirectBytes, GL15.GL_STREAM_DRAW);
            indirectCmdCapBytes = initialIndirectBytes;
            GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);

            GL30.glBindVertexArray(0);
            // Element array binding is part of VAO state — leave it as we set it
            // inside the VAO. The vertex array buffer binding outside the VAO
            // is restored by the guard below.

            ready = true;
            MainRegistry.LOGGER.info("[HBM-M MDI] MdiGeometryAtlas initialised (vao={}, vbo={}, ebo={}, inst={}, indirect={})",
                    vaoId, vertexVboId, indexEboId, instanceVboId, indirectBufId);
        } finally {
            guard.restore();
        }
    }

    public boolean isReady() {
        return ready;
    }

    /**
     * Render-thread-only defensive validity gate for the atlas VAO: when the
     * atlas is marked ready but its VAO name is dead (deleted behind our back -
     * e.g. by a double-deletion bug elsewhere, after the driver re-issued the
     * same numeric ID to another object), a bind would silently fall through to
     * a foreign VAO and the MDI multi-draw would fault in the driver. Tearing
     * down and re-initialising the atlas here also evicts all registered
     * geometry, so the caller MUST bail out of the current frame's draw when
     * this returns false: the freshly created atlas holds no slots and any
     * prepared draw commands would reference stale offsets. Renderers
     * re-register lazily on the next flush.
     *
     * @return true if this atlas is ready and its VAO is live; false when the
     *         caller must skip the MDI draw (the atlas was reset).
     */
    public boolean ensureVaoAlive() {
        if (!ready) {
            return false;
        }
        if (vaoId > 0 && GL30.glIsVertexArray(vaoId)) {
            return true;
        }
        MainRegistry.LOGGER.warn("[HBM-M MDI] Atlas VAO {} is no longer valid (deleted or recycled); resetting atlas and skipping this MDI draw", vaoId);
        resetForResourceLifecycle();
        // Recreate eagerly so the next frame's flush can register geometry again;
        // even when the fresh atlas comes up healthy, its slot table is empty and
        // the caller's prepared draw commands are stale - always bail this frame.
        getOrCreate();
        return false;
    }

    public int getVaoId() { return vaoId; }

    /**
     * Snapshot of the GL names currently owned by the live atlas (render thread only).
     * Deferred-deletion queues (drainPendingGlDeletions) must skip these: the driver
     * re-issues freed numeric IDs, so a stale queued entry can carry the same number
     * the live atlas owns now - deleting it kills the atlas VAO mid-frame and faults
     * the next MDI draw in the driver (native crash, no Java stack).
     */
    public static int[] liveGlNames() {
        MdiGeometryAtlas inst = INSTANCE;
        if (inst == null) return new int[0];
        return new int[] { inst.vaoId, inst.vertexVboId, inst.indexEboId, inst.instanceVboId,
                inst.compactedInstanceVboId, inst.indirectBufId };
    }

    /** True when {@code name} is a GL object the live atlas currently owns (render thread only). */
    public static boolean isLiveAtlasName(int name) {
        if (name <= 0) return false;
        for (int live : liveGlNames()) {
            if (live == name) return true;
        }
        return false;
    }
    public int getInstanceVboId() { return instanceVboId; }
    public int getCompactedInstanceVboId() { return compactedInstanceVboId; }
    public int getIndirectBufferId() { return indirectBufId; }

    public void bindInstanceVbo(int vboId) {
        if (!ready || vaoId <= 0) return;
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        int stride = INSTANCE_FLOATS * 4;
        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, true, stride, InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET * 4);
        GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.BBOX_MIN_FLOAT_OFFSET * 4);
        GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.BBOX_SIZE_FLOAT_OFFSET * 4);
        GL20.glVertexAttribPointer(8, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET + 0) * 4);
        GL20.glVertexAttribPointer(9, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET + 1) * 4);
        GL20.glVertexAttribPointer(10, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET + 2) * 4);
        GL20.glVertexAttribPointer(11, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET + 3) * 4);
        GL20.glVertexAttribPointer(12, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.UVRECT_FLOAT_OFFSET * 4);
        GL20.glVertexAttribPointer(13, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.TINT_FLOAT_OFFSET * 4);
        GL20.glVertexAttribPointer(14, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.GRAD_FLOAT_OFFSET * 4);
        // AnimParams vec4 @ 112: MUST be re-pointed together with the rest -
        // glVertexAttribPointer binds the CURRENT ARRAY_BUFFER into the VAO per
        // attribute; skipping 15 left it reading the plain instance VBO while
        // attribs 4..14 read the compacted one (GPU-culled draws), so parametric
        // parts received another record's joint parameters.
        GL20.glVertexAttribPointer(15, 4, GL11.GL_FLOAT, false, stride, InstancedStaticPartRenderer.ANIM_FLOAT_OFFSET * 4);
    }

    public void bindCompactedInstanceVbo() {
        bindInstanceVbo(compactedInstanceVboId);
    }

    public void restoreDefaultInstanceVbo() {
        bindInstanceVbo(instanceVboId);
    }

    /** Capacity of {@link GL40#GL_DRAW_INDIRECT_BUFFER} in bytes; used for orphaning prior to writing commands. */
    public long getIndirectCommandBufferCapBytes() {
        return indirectCmdCapBytes;
    }

    public int getInstanceFloatsPerInstance() { return INSTANCE_FLOATS; }
    public int getInstanceFadeFloatOffset() { return INSTANCE_FADE_FLOAT_OFFSET; }

    /**
     * Atlas VRAM estimate: used vertices/indices + capacity of the instance VBO,
     * the compacted instance VBO (same capacity as the instance VBO) and the
     * indirect buffer. Used by F3 debug overlay ({@code NucleusDebug}).
     */
    public synchronized long estimateVramBytes() {
        if (!ready) return 0L;
        return vertexUsedBytes + indexUsedBytes
                + instanceVboCapBytes()
                + indirectCmdCapBytes;
    }

    /**
     * Allocated-capacity VRAM estimate: the VBO/EBO store CAPS (grown
     * geometrically, so they exceed the used bytes above) plus both instance
     * VBOs and the indirect buffer. The ceiling for the F3 VRAM bar.
     */
    public synchronized long estimateAllocatedVramBytes() {
        if (!ready) return 0L;
        return vertexCapBytes + indexCapBytes
                + instanceVboCapBytes()
                + indirectCmdCapBytes;
    }

    /** Both instance VBOs (plain + compacted) hold the same capacity store. */
    private long instanceVboCapBytes() {
        return 2L * instanceCapInstances * INSTANCE_FLOATS * 4L;
    }

    /** MDI diagnostic counter: number of registered part meshes in the atlas. */
    public synchronized int getRegisteredGeometryCount() {
        return geometryByRenderer.size();
    }

    /**
     * Enables vertex attrib arrays 0..11 on the currently bound {@link #vaoId}.
     * After {@link ShaderInstance#apply()} / Embeddium chunk-batches, certain arrays
     * may become disabled; without this MDI draws only a subset of attributes
     * (e.g. only base mesh visible, moving doors/cogs missing).
     */
    public void enableVertexAttribArraysOnBoundVao() {
        if (!ready || vaoId <= 0) return;
        GlVaoSafety.bindVertexArray(vaoId);
        // Attributes 0..15 are used (0-3 for vertex, 4-15 for instance data).
        for (int i = 0; i <= 15; i++) {
            GL20.glEnableVertexAttribArray(i);
        }
    }

    /**
     * Acceptance gate for {@link MdiBatchCoordinator#submit}: only the unsliced
     * current compressed record layout is wired into this atlas.
     */
    public boolean acceptsInstanceDataSize(int floatsPerInstance) {
        return ready && floatsPerInstance == INSTANCE_FLOATS;
    }

    /**
     * Current {@link Slot} for an already registered renderer after any
     * {@link #repackGeometryAndRefreshSlots} (VBO/EBO expansion). Required under
     * {@link MdiBatchCoordinator#dispatch}: {@code submit} copies offsets into {@code Pending}
     * in the order of {@code flushInstancedBatches}, and between flushes an intermediate
     * {@code registerGeometryIfAbsent} call may trigger repack and invalidate slot offsets.
     */
    public synchronized Slot getCurrentSlot(InstancedStaticPartRenderer renderer) {
        if (!ready || renderer == null) return null;
        // Settle a pending eviction-wave repack BEFORE handing out offsets.
        repackIfDirtyLocked();
        GeoRecord rec = geometryByRenderer.get(renderer);
        return rec != null ? rec.slot : null;
    }

    /**
     * Evicts renderer geometry from atlas on {@link InstancedStaticPartRenderer#cleanup()}
     * prior to full {@link #resetForResourceLifecycle()}. Otherwise {@link #geometryByRenderer}
     * retains zombie keys whose retained buffers are freed, corrupting subsequent repacks.
     * <p>
     * The full repack is DEFERRED (see {@link #geometryRepackPending}): a mass-cleanup
     * wave (F3+T / disconnect drops dozens of renderers) then costs ONE repack instead
     * of one full re-upload of all remaining parts per eviction (O(N^2) record uploads).
     */
    public static void evictRendererIfRegistered(InstancedStaticPartRenderer renderer) {
        if (renderer == null) return;
        MdiGeometryAtlas inst = INSTANCE;
        if (inst == null) return;
        synchronized (inst) {
            if (INSTANCE != inst || !inst.ready) return;
            inst.evictRendererLocked(renderer);
        }
    }

    private void evictRendererLocked(InstancedStaticPartRenderer renderer) {
        GeoRecord rec = geometryByRenderer.remove(renderer);
        if (rec == null) return;
        if (geometryByRenderer.isEmpty()) {
            vertexUsedBytes = 0L;
            indexUsedBytes = 0L;
            geometryRepackPending = false;
            return;
        }
        // usedBytes still counts the evicted part - settle it before the next
        // consumer sizes an append or reads a Slot (repackIfDirtyLocked()).
        geometryRepackPending = true;
    }

    /** Caller must hold the atlas monitor ({@code synchronized} entry point). */
    private void repackIfDirtyLocked() {
        if (!geometryRepackPending) return;
        geometryRepackPending = false;
        repackGeometryAndRefreshSlots();
    }

    /**
     * Lazy-register a renderer's geometry. The first call uploads vertex+index
     * bytes into the atlas; subsequent calls return the cached slot.
     * <p>
     * <b>Atlas growth:</b> if the new geometry doesn't fit in the current
     * VBO/EBO capacity, both buffers are reallocated at next-power-of-two size
     * and ALL known parts are re-uploaded. This is rare (once per part per
     * session); not on the hot path.
     */
    public synchronized Slot registerGeometryIfAbsent(InstancedStaticPartRenderer renderer,
                                                       ByteBuffer vertexBytes,
                                                       IntBuffer indices,
                                                       int indexCount) {
        if (!ready) return null;
        GeoRecord existing = geometryByRenderer.get(renderer);
        if (existing != null) {
            if (existing.slot != null && vertexBytes != null && indices != null && indexCount > 0) {
                int inVertexLen = vertexBytes.remaining();
                if (inVertexLen == existing.registeredVertexBytesLen && indexCount == existing.registeredIndexCount) {
                    return existing.slot;
                }
            }
            evictRendererLocked(renderer);
        }
        // Settle a pending eviction-wave repack BEFORE sizing the append: the
        // high-water marks still include evicted parts until the repack runs.
        repackIfDirtyLocked();

        if (vertexBytes == null || indices == null || indexCount <= 0) return null;
        ByteBuffer vertexBytesView = vertexBytes.duplicate();
        IntBuffer indicesView = indices.duplicate();
        long vertexBytesLen = vertexBytesView.remaining();
        long indexBytesLen = (long) indexCount * 4L;
        long vertexCount = vertexBytesLen / VERTEX_STRIDE_BYTES;

        // Reuse view buffers directly without secondary memAlloc!
        GeoRecord rec = new GeoRecord(vertexBytesView, indicesView, (int) vertexBytesLen, indexCount);
        try {
            // Grow BOTH buffers first, then repack ONCE: each glBufferData resize
            // wipes its storage and requires a full re-upload of all known parts -
            // running ensure* back-to-back each with its own repack doubled the
            // re-upload work whenever both had to grow.
            boolean grew = ensureVertexCapacity(vertexUsedBytes + vertexBytesLen);
            grew |= ensureIndexCapacity(indexUsedBytes + indexBytesLen);
            if (grew) {
                repackGeometryAndRefreshSlots();
            }

            int baseVertex = (int) (vertexUsedBytes / VERTEX_STRIDE_BYTES);
            int firstIndexBytes = (int) indexUsedBytes;

            GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
            try {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVboId);
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, vertexUsedBytes, vertexBytesView.duplicate());
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexEboId);
                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexUsedBytes, indicesView.duplicate());
            } finally {
                guard.restore();
            }

            vertexUsedBytes += vertexBytesLen;
            indexUsedBytes += indexBytesLen;

            rec.slot = new Slot(baseVertex, firstIndexBytes, indexCount);
            geometryByRenderer.put(renderer, rec);
            MainRegistry.LOGGER.debug("[HBM-M MDI] Atlas registered renderer {}: verts={}, idx={}, baseVertex={}, firstIndexBytes={}",
                    System.identityHashCode(renderer), vertexCount, indexCount, baseVertex, firstIndexBytes);
            return rec.slot;
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] Geometry registration failed", t);
            return null;
        }
    }


    /** @return true if the buffer was grown (its storage was wiped and needs a repack). */
    private boolean ensureVertexCapacity(long requiredBytes) {
        if (requiredBytes <= vertexCapBytes) return false;
        long newCap = vertexCapBytes;
        while (newCap < requiredBytes) newCap *= 2L;
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newCap, GL15.GL_STATIC_DRAW);
            vertexCapBytes = newCap;
        } finally {
            guard.restore();
        }
        // glBufferData wiped the VBO - the caller repacks once after both
        // ensure* checks ran (see registerGeometryIfAbsent).
        return true;
    }

    /** @return true if the buffer was grown (its storage was wiped and needs a repack). */
    private boolean ensureIndexCapacity(long requiredBytes) {
        if (requiredBytes <= indexCapBytes) return false;
        long newCap = indexCapBytes;
        while (newCap < requiredBytes) newCap *= 2L;
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexEboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, newCap, GL15.GL_STATIC_DRAW);
            indexCapBytes = newCap;
        } finally {
            guard.restore();
        }
        return true;
    }

    /**
     * Re-uploads all registered parts in {@link #geometryByRenderer} order and recomputes
     * every {@link GeoRecord#slot}. Required after any {@code glBufferData} resize that
     * clears vertex or index storage; also fixes iteration order via {@link LinkedHashMap}
     * so GPU layout always matches {@link Slot} metadata.
     */
    private void repackGeometryAndRefreshSlots() {
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL30.glBindVertexArray(vaoId);
            long vOff = 0L;
            long iOff = 0L;
            for (Map.Entry<InstancedStaticPartRenderer, GeoRecord> e : geometryByRenderer.entrySet()) {
                GeoRecord rec = e.getValue();
                String rid = Integer.toHexString(System.identityHashCode(e.getKey()));
                if (rec.vertexBytesView == null || rec.indicesView == null) {
                    rec.slot = null;
                    continue;
                }
                if (rec.registeredVertexBytesLen <= 0 || rec.registeredIndexCount <= 0) {
                    rec.slot = null;
                    continue;
                }
                ByteBuffer vbView = rec.vertexBytesView.duplicate();
                vbView.clear();
                vbView.limit(rec.registeredVertexBytesLen);
                IntBuffer ibView = rec.indicesView.duplicate();
                ibView.clear();
                ibView.limit(rec.registeredIndexCount);
                int vLen = vbView.remaining();
                int idxCount = ibView.remaining();
                if (vLen != rec.registeredVertexBytesLen || idxCount != rec.registeredIndexCount) {
                    rec.slot = null;
                    continue;
                }
                long idxBytes = (long) idxCount * 4L;
                int baseVertex = (int) (vOff / VERTEX_STRIDE_BYTES);
                int firstIndexBytes = (int) iOff;
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVboId);
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, vOff, vbView);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexEboId);
                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, iOff, ibView);
                rec.slot = new Slot(baseVertex, firstIndexBytes, idxCount);
                vOff += vLen;
                iOff += idxBytes;
            }
            vertexUsedBytes = vOff;
            indexUsedBytes = iOff;
        } finally {
            guard.restore();
        }
    }

    /** Ensure the instance VBO can hold {@code instances} contiguous instance records. */
    public synchronized boolean ensureInstanceCapacity(int instances) {
        if (!ready) return false;
        if (instances <= instanceCapInstances) return true;
        long newCap = instanceCapInstances <= 0L ? 4096L : instanceCapInstances;
        while (newCap < instances) newCap *= 2L;
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            int newVbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, newVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newCap * INSTANCE_FLOATS * 4L, GL15.GL_STREAM_DRAW);

            if (instanceCapInstances > 0 && instanceVboId != 0) {
                GL31.glBindBuffer(GL31.GL_COPY_READ_BUFFER, instanceVboId);
                GL31.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, newVbo);
                GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, 0, 0, instanceCapInstances * INSTANCE_FLOATS * 4L);
                GL31.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
                GL31.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
                GL15.glDeleteBuffers(instanceVboId);
            }
            instanceVboId = newVbo;
            com.hbm_m.client.render.debug.GlDebugMarkers.labelBuffer(instanceVboId, "HBM_MdiAtlas_Instance_VBO");
            bindInstanceVbo(instanceVboId);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, compactedInstanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newCap * INSTANCE_FLOATS * 4L, GL15.GL_STREAM_DRAW);

            instanceCapInstances = newCap;
            instanceWindowsValid = false;
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] Instance VBO grow failed", t);
            return false;
        } finally {
            guard.restore();
        }
    }

    /**
     * Upload of instance data window to atlas at offset {@code destFloatOffset}.
     */
    public void uploadInstanceWindowSpanned(int destFloatOffset, FloatBuffer src,
                                            int srcFloatOffset, int floatCount) {
        if (!ready || floatCount <= 0) return;
        GpuSpanUploader.fullUpload(null, destFloatOffset, src, srcFloatOffset, floatCount, instanceVboId);
    }

    /**
     * Span-diff upload of an instance data window: {@code shadow} (when non-null,
     * with {@code shadowValidFloats} known-in-GPU words) is diffed against
     * {@code src} and only changed sub-spans reach the VBO - a window with one
     * changed machine of 5000 uploads one record instead of the whole window.
     * The coordinator guarantees the shadow mirrors the GPU window (transferred
     * only from a record whose previous upload succeeded; on window replacement
     * or atlas invalidation the shadow is dropped and this degrades to a full
     * upload).
     */
    public void uploadInstanceWindowSpanned(int destFloatOffset, FloatBuffer src,
                                            int srcFloatOffset, int floatCount,
                                            FloatBuffer shadow, int shadowValidFloats) {
        if (!ready || floatCount <= 0) return;
        // The shadow is a per-window CPU mirror indexed from 0 (NOT at
        // srcFloatOffset) - it stores exactly this window's floats.
        GpuSpanUploader.diffUpload(shadow, 0, shadowValidFloats,
                src, srcFloatOffset, destFloatOffset, floatCount, instanceVboId);
    }


    /** Invoked by coordinator after all frame windows have been successfully uploaded. */
    public void markInstanceWindowsValid() {
        instanceWindowsValid = true;
    }

    public boolean isInstanceWindowsValid() {
        return instanceWindowsValid;
    }

    public void onRenderOriginChanged() {
        instanceWindowsValid = false;
    }

    /**
     * Expands GL_DRAW_INDIRECT_BUFFER when necessary. Command updates are handled via SubData on the coordinator side.
     */
    public void ensureIndirectCommandByteCapacity(int needBytes) {
        if (!ready || needBytes <= 0) return;
        if (needBytes <= indirectCmdCapBytes) return;
        long newCap = indirectCmdCapBytes <= 0L ? 65536L : indirectCmdCapBytes;
        while (newCap < needBytes) {
            newCap *= 2L;
        }
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, indirectBufId);
            GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, newCap, GL15.GL_STREAM_DRAW);
            indirectCmdCapBytes = newCap;
        } finally {
            guard.restore();
        }
    }

    // ---- small GL state snapshot helper ----
    private static final class GLCapabilitiesGuard {
        private final int prevVao;
        private final int prevArrayBuf;
        private final int prevElemBuf;
        private GLCapabilitiesGuard(int vao, int ab, int eb) {
            this.prevVao = vao; this.prevArrayBuf = ab; this.prevElemBuf = eb;
        }
        static GLCapabilitiesGuard snapshot() {
            int v = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int a = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            int e = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
            return new GLCapabilitiesGuard(v, a, e);
        }
        void restore() {
            restore(0);
        }

        void restore(int excludedVao) {
            try {
                if (prevVao != 0 && prevVao != excludedVao) {
                    GL30.glBindVertexArray(prevVao);
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, prevElemBuf);
                } else {
                    GlVaoSafety.bindVertexArray(0);
                    BufferUploader.invalidate();
                }
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuf);
            } catch (Throwable ignored) {}
        }
        @SuppressWarnings("unused")
        private static void touchGL() { GL.getCapabilities(); }
    }
}
