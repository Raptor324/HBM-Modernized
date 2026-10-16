package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.client.render.culling.HiZDepthPyramid;
import com.hbm_m.client.render.debug.GlDebugMarkers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;

@OnlyIn(Dist.CLIENT)
/**
 * <b>Tier 1: GPU Compute Bake</b> — Bakes OBJ model part instances into a
 * BLOCK-format vertex buffer using an OpenGL 4.3 compute shader, followed by
 * draws using the active shaderpack's native program (pack shadow or gbuffers).
 * <p>
 * <b>Draw-call structure (global batch):</b> per pass (main/shadow) one shared
 * output SSBO + one EBO + one command buffer ("pool") hold every cache-admitted
 * mesh at per-mesh {@code vertBase} / {@code eboWordBase} offsets. The bake
 * phase ({@link #bakeMain} collect loop, {@link #bakeAndDrawShadow} entries
 * loop) only writes; the flush issues ONE {@code glMultiDrawElementsIndirect}
 * ({@link #presentMainBatch} at end of the main BE walk, in-place at the end of
 * the shadow bake loop). On GL 4.6 / ARB_indirect_parameters hardware the flush
 * instead issues ONE {@code glMultiDrawElementsIndirectCount} over commands the
 * cull assembler wrote GPU-side - no CPU visible-count readback per flush. Caches
 * that do not fit the budget are refused (bake returns false) and the mesh renders
 * through the existing Tier-2/3 instanced fallback for that frame.
 * <p>
 * Under this architecture, the shaderpack applies its own shadow distortion and gbuffer
 * encoding natively without requiring hardcoded static schemas or runtime AST regex
 * rewriting, ensuring full compatibility across modern shaderpacks.
 * <p>
 * <b>Camera contract:</b> the compute output is camera-INDEPENDENT. Main-pass records
 * are anchor-relative (FrameViewState); shadow records are LIGHT-space — each one is an
 * exact pos/quat decomposition of the Iris shadow poseStack
 * {@code MV_shadow * T(bePos - camPos) * part}, so the light view rotation rides INSIDE
 * the records, with the stashed player camera added to the translation (and folded back
 * out at draw time). The compute writes vertices in exactly those spaces and the
 * draw-time uniforms finish the transform: main draws with
 * {@code ModelViewMat = R_cam * T(-relCam)} and the stripped projection, shadow draws
 * with {@code ModelViewMat = T(-cam)} and the shadow projection — T(-cam) only cancels
 * the camera offset baked into the records; RenderSystem's modelview is identity in the
 * Iris shadow pass (Iris only swaps the projection) and is never absorbed into the
 * shadow map. Packs reading gl_ModelViewMatrix see a vanilla-shaped full view matrix.
 * <p>
 * <b>Bake cache:</b> because the output no longer depends on the camera, each part
 * renderer keeps per-pass caches (output SSBO + uploaded record SSBO + diff snapshot).
 * A frame with unchanged records skips the record upload AND the compute expansion
 * entirely. A frame with a few changed records uploads ONLY the dirty records and
 * re-expands ONLY the dirty instances: the CPU diff fills a per-cache dirty-index
 * SSBO (word 0 = count, words 1.. = instance indices) and the expansion dispatch
 * maps gl_GlobalInvocationID.y through that list — span-level re-expansion instead
 * of a whole-renderer re-bake. Full re-bakes (first bake, record-count or bone-epoch
 * change, buffer realloc, admission fallback) always dispatch in IDENTITY mode
 * (y = instance index) and never read the dirty list. A camera-only change (frustum
 * moved, records stable) skips the expansion and re-runs only the cheap
 * instance-granularity cull. Caches that do not fit the budget (config
 * nucleusBakeCacheMB, default 192) stay on the immediate path — the budget can
 * never thrash.
 * <p>
 * <b>Compute pipeline (expansion program):</b>
 * <ul>
 *   <li>SSBO 0: Input mesh vertices — one-time u32-ALIGNED relayout of the companion
 *       mesh (stride padded to a multiple of 4), so every {@code ldWord} resolves to a
 *       single fetch (kills the 54-byte-stride double-fetch on 1.21.1).</li>
 *   <li>SSBO 1: Instance records (32 floats / 128 bytes per instance).</li>
 *   <li>SSBO 2: Output vertex buffer — 56-byte dense layout for the main pass
 *       (ends with a computed {@code at_midBlock} for PBR shaderpacks),
 *       slim 24-byte layout (pos+uv0+normal only) for the shadow pass.</li>
 *   <li>SSBO 3: Bone palette (up to 64 4x4 matrices).</li>
 *   <li>SSBO 4: Dirty-index list (dirty-list mode only; word 0 = count, words 1.. =
 *       instance indices). Full re-bakes dispatch identity mode and never read it.</li>
 *   <li>Cull program SSBO 4-equivalent: control block (visible count, indirect words,
 *       visible list).</li>
 * </ul>
 * Record uploads go through {@link PersistentUploadStaging} (persistently mapped ring
 * + ordered GPU-GPU copy) with a glBufferSubData fallback.
 * <p>
 * <b>Cull program (passes):</b> 0 = per-instance frustum test (record pos+quat rotates
 * the model bbox into a conservative record-space AABB; 6 planes) writing a compacted
 * visible list; 2 = single-invocation assembler that turns the visible count into the
 * indirect expansion-dispatch and glDrawElementsIndirect command words; 1 = indirect
 * EBO build (visible instance i, base index k → visible[i]*vertCount + base[k]).
 * <p>
 * <b>Fallback:</b> if GL 4.3 compute is unsupported or a dispatch fails,
 * {@link #isEnabled()} returns false and execution falls back to Tier 2 (ExtendedShader)
 * or Tier 3 (vanilla instancing).
 *
 * @credit CrankShaft / Flywheel / Iris; 2D compute grid dispatch, LDS volumetric light cache, and std140 uniform buffer consolidation
 */
public final class NucleusGpuBaker {

    private static final int LOCAL_SIZE = 64;
    private static final int UBO_BINDING_BAKE_PARAMS = 0;
    private static int bakeParamsUbo = -1;
    private static final java.nio.ByteBuffer UBO_BYTE_BUF = org.lwjgl.system.MemoryUtil.memAlloc(88);
    private static final java.nio.IntBuffer UBO_INT_VIEW = UBO_BYTE_BUF.asIntBuffer();

    private static final boolean KILL_SWITCH =
            !"false".equalsIgnoreCase(System.getProperty("hbm.gpuBake", "true"));

    /** Frustum cull pre-pass toggle: -Dhbm.bakeCull=false falls back to full expansion + full draw. */
    private static final boolean CULL_SWITCH =
            !"false".equalsIgnoreCase(System.getProperty("hbm.bakeCull", "true"));

    /**
     * Total VRAM budget for per-renderer bake caches (output + record + control SSBOs;
     * the heap record snapshot is counted at its byte size to keep the budget honest).
     * An entry that does not fit after LRU eviction is refused and falls back to
     * the Tier-2/3 instanced path for that frame, so the budget can never thrash.
     * 0 or less disables
     * caching. Default 192 MB (config nucleusBakeCacheMB; -Dhbm.bakeCacheMB=N overrides).
     */
    private static long cacheBudgetBytes() {
        Integer flag = Integer.getInteger("hbm.bakeCacheMB");
        int mb = flag != null ? flag : com.hbm_m.config.ModClothConfig.get().nucleusBakeCacheMB;
        if (mb <= 0) {
            return 0L; // documented disable path
        }
        return Math.max(16, Math.min(1024, mb)) * 1024L * 1024L;
    }
    /**
     * A cache unused for this many render frames may be evicted to admit a new one.
     * 24 frames is ~1.2 s at 20 fps: caches survive momentary looks-away but stop
     * pinning VRAM soon after the camera leaves a farm.
     */
    private static final long LRU_FRAME_LIMIT = 24;

    /**
     * Admission backoff window (frames): after a budget refusal (allocateCache /
     * ensureOutputCapacity / ensureCacheRecords) the entry stays refused (Tier-2/3
     * fallback) for this many frames instead of re-attempting admission - and its
     * evictStaleCaches / relayoutPool churn - EVERY frame. Behavior with
     * budget available is unchanged: the stamp is only set on an actual refusal.
     */
    private static final long ADMISSION_BACKOFF_FRAMES = 32;

    private static boolean checked = false;
    private static boolean available = false;
    private static boolean logged = false;
    /** One-shot latch: a non-identity RenderSystem modelview seen at shadow flush time. */
    private static boolean foreignShadowMvLogged = false;
    private static int programId = -1;
    /** Latches an initialization failure (e.g. GLSL compile error): without it every flush would retry compilation and spam the log with stack traces. */
    private static boolean initFailed = false;

    /** Instance-granularity cull program (pass 0 cull / 1 EBO build / 2 assemble indirect words). */
    private static int cullProgramId = -1;
    private static boolean cullInitFailed = false;
    private static int cuPass, cuInstCount, cuVertCount, cuIdxCount, cuInstStride, cuPlanes;
    /** Global-batch placement uniforms: output vertBase + EBO word base (pool path). */
    private static int cuVertBase, cuEboBase;
    /** Pool command-store slot for pass 2's GPU-written indirect command (-1 = path off). */
    private static int cuCmdSlot;
    /** Pass 1 identity-fill flag: 1 = write EVERY instance slot (the expandPoolEbo
     * no-cull growth), 0 = write the visible list (cull EBO build). Uniforms are
     * per-program state, so every entry point sets it before dispatching. */
    private static int cuIdentity;
    /** Optional Hi-Z occlusion uniforms (see CULL_SOURCE pass 0). */
    private static int cuHiZMode, cuClipMat, cuViewSize, cuPyramidLevels;

    private static final int BONE_PALETTE_SIZE = 64;
    private static int boneSsbo = -1;
    private static boolean hasBoneMatrices = false;
    /** Bumped on every bone palette upload; meshes with a bone-id attribute fold it into their cache key. */
    private static long boneEpoch = 0L;

    // ── Dense OUTPUT bake layout, MAIN pass (56 bytes, 4-byte aligned) ──
    // The input companion mesh uses IrisVertexFormats.ENTITY, whose stride
    // on 1.21.1 is 54 bytes (unaligned: invisible padding after normal).
    // Addressing it via floats caused an off-by-2 byte stride misalignment
    // and exploding triangles on 1.21.1/Iris (on 1.20.1/Oculus stride 56 is
    // 4-byte aligned). The compute shader reads inputs by byte offsets
    // using bitfield unpacking and writes to this dense 56-byte layout,
    // matching the attribute order of Iris ENTITY formats. The appended
    // at_midBlock word mirrors the Iris/Oculus TERRAIN extension attribute
    // (BYTE x 3, packed by ExtendedDataHelper.packMidBlock semantics) which
    // the ENTITY-format companion mesh does not carry.
    private static final int OUT_STRIDE_BYTES = 56;
    private static final int OUT_OFF_POS = 0;      // 3F
    private static final int OUT_OFF_COLOR = 12;   // 4UB
    private static final int OUT_OFF_UV0 = 16;     // 2F
    private static final int OUT_OFF_UV1 = 24;     // 2S (integer pipeline)
    private static final int OUT_OFF_UV2 = 28;     // 2S (integer pipeline)
    private static final int OUT_OFF_NORMAL = 32;  // 3B + 1 pad
    private static final int OUT_OFF_ENTITY = 36;  // 2US
    private static final int OUT_OFF_MIDTEX = 40;  // 2F
    private static final int OUT_OFF_TANGENT = 48; // 4B
    private static final int OUT_OFF_MIDBLOCK = 52; // 3B (x,y,z) + 1 pad, computed in the bake

    // ── Slim SHADOW output layout (24 bytes) ──────────────────────────
    // Shadow programs only need position + UV0 (depth/shadowcolor) and the
    // normal: writing the full 56-byte layout spent >2x the required SSBO
    // bandwidth on the most expensive pass. at_midBlock is intentionally
    // absent here: it feeds gbuffers-only pack features (parallax/waving).
    private static final int SHADOW_STRIDE_BYTES = 24;
    private static final int SH_OFF_POS = 0;       // 3F
    private static final int SH_OFF_UV0 = 12;      // 2F
    private static final int SH_OFF_NORMAL = 20;   // 3B + 1 pad

    /** Total VRAM + snapshot bytes currently held by per-renderer caches. */
    private static long cacheBytes = 0;
    private static boolean cacheOverflowLogged = false;
    /** Cumulative admission (budget) refusals since the last session reset
     * (releaseResourcesInternal); gives the one-shot overflow log's refusal
     * count a session-scale meaning. Touched only under the class lock. */
    private static long admissionRefusals = 0;

    private static final java.util.ArrayList<BakedMesh> BAKED = new java.util.ArrayList<>(64);

    static final class BakedMesh {
        final IrisCompanionMesh mesh;
        final int vertCount;
        final int idxCount;
        final int[] baseIndices;
        /** Base mesh indices as an SSBO for the cull program's EBO-build pass. */
        int baseIdxSsbo = -1;
        // P3: one-time aligned relayout of the input mesh; -1 = bind the mesh VBO
        // directly (its stride is already a multiple of 4).
        int inputSsbo = -1;
        int inputStride;
        final ModeCache shadow = new ModeCache();
        final ModeCache main = new ModeCache();
        // Global-batch placement: vertex offset of this
        // mesh's output range and uint-word offset of its EBO range, per mode
        // (index 0 = shadow, 1 = main). Recomputed by relayoutPool on any
        // range-capacity change; -1 = no range assigned.
        final int[] vertBase = { -1, -1 };
        final int[] eboWordBase = { -1, -1 };

        BakedMesh(IrisCompanionMesh mesh, int vertCount, int idxCount, int[] baseIndices) {
            this.mesh = mesh;
            this.vertCount = vertCount;
            this.idxCount = idxCount;
            this.baseIndices = baseIndices;
        }

        String debugTag() {
            if (mesh != null) {
                String name = mesh.getDebugName();
                if (name != null && !name.isEmpty() && !"unknown".equals(name)) {
                    return name;
                }
            }
            return "mesh#" + System.identityHashCode(this);
        }

        ModeCache cache(int mode) {
            return mode == MODE_SHADOW ? shadow : main;
        }

        /** Mode index of one of this mesh's caches (0 = shadow, 1 = main). */
        int modeOf(ModeCache mc) {
            return mc == main ? MODE_MAIN : MODE_SHADOW;
        }
    }

    /** Per-pass cache of one BakedMesh: everything needed to draw without re-baking. */
    private static final class ModeCache {
        int filled = -1;               // CPU EBO fill level (no-cull path)
        int eboCapacity = 0;
        int recordSsbo = -1;
        final long[] recordSize = { -1L };
        float[] recordCopy;            // heap snapshot of the records at last bake
        int recordCount = -1;
        long boneKey = -1;             // bone epoch folded into validity
        // Span-level re-expansion state: dirty-index list (word 0 = dirty count,
        // words 1.. = dirty instance indices) + CPU-side scratch of the same list,
        // filled by diffRecords. Comparison-only state: quantization never touches
        // the uploaded or expanded bytes (see quantizeCmpBits).
        int dirtySsbo = -1;
        long dirtyBytes = -1;
        int[] dirtyIdx = null;
        int dirtyCount = 0;
        // cull state
        int ctlSsbo = -1;
        long ctlBytes = -1;
        long frustumStamp = Long.MIN_VALUE;
        boolean cullValid = false;
        boolean outputValid = false;
        long lastUseFrame = 0L;
        // Instance-granularity cull result of the last cull run for THIS cache
        // (ctl word 0 read back once per FLUSH batch, not per cull run): valid
        // whenever cullValid is true - a cache hit reuses the still-true last count.
        long lastVisibleCount = 0L;
        // Set when a cull run refreshed the ctl header and the CPU-side count has
        // not been resolved yet (the batched readback in flushPool owns it now -
        // one pipeline sync per pool flush instead of one per renderer per frame).
        boolean countsDirty = false;
        // Scratch index into the staging SSBO during the batched counts readback.
        int stagingSlot = -1;
        // GPU-count flush path (GL 4.6): generation of the pool command store and
        // the pending-slot index at this cache's last cull run's pass-2 write. A
        // flush may draw the GPU-written command only while both still match -
        // otherwise the slot was orphaned by a store grow or belongs to another
        // pending position (the walk order shifted between cache hits).
        int cmdGenStamp = -1;
        int cmdSlotWritten = -1;
        // Companions of lastVisibleCount: instances culled by the optional Hi-Z
        // occlusion phase (frustum-passed but depth-occluded) and whether that
        // cull run sampled the pyramid (uHiZMode = 1).
        long lastOccludedCount = 0L;
        boolean lastCullHiZ = false;
        // ── Global-batch state ──
        // Instance capacity of this cache's range in the pass pool (0 = no range).
        int outInstances = 0;
        // Instance capacity of this cache's EBO range in the pass pool.
        int eboInstances = 0;
        // Admission backoff stamp: frameStamp of the last budget refusal
        // (-1 = none). See ADMISSION_BACKOFF_FRAMES.
        long admissionRefusedFrame = -1L;
    }

    // ── Global multi-draw batch (one indirect multi-draw per pass) ─────
    // One output SSBO + one EBO + one command buffer per pass holding every
    // cache-admitted mesh at per-mesh vertBase / eboWordBase offsets. The
    // flush binds ONE VAO (full/slim layout), ONE bindIrisExtendedAttributes
    // call and issues ONE glMultiDrawElementsIndirect over the command buffer.
    // Zero-instance commands are compacted out CPU-side (primcount == number
    // of meshes that actually draw), so no no-op commands reach the GPU.
    private static final int INDIRECT_CMD_BYTES = 20; // DrawElementsIndirectCommand
    /** Cull-program SSBO binding of the pool command store (pass 2 writes slots). */
    private static final int CULL_CMD_BINDING = 4;
    /** Minimum pool store once allocated (avoids VAO/buffer churn on eviction). */
    private static final long MIN_POOL_BYTES = 1L << 20;
    /**
     * Idle frames (no relayout of the pass) before the shrink policy may return
     * tail capacity. 600 frames ≈ 30 s at 20 fps: well past any chunk-load /
     * spawn wave, so a transient peak is never shrunk mid-wave. The shrink runs
     * from the flush path only (never per frame, never mid-walk - the walk's
     * own relayoutPool stamps lastRelayoutFrame).
     */
    private static final long SHRINK_IDLE_FRAMES = 600;
    /** Minimum SAVED bytes for a shrink to fire (avoids driver churn over crumbs). */
    private static final long SHRINK_MIN_SAVINGS = 8L << 20;

    private static final class PassPool {
        final boolean slim;
        int outputSsbo = -1;
        long outputAllocBytes = 0;
        int eboSsbo = -1;
        long eboAllocBytes = 0;
        /** GPU-count flush path (GL 4.6): one 20-byte indirect command per
         * pending-entry slot, written by cull pass 2 (see ensurePoolCmdBuffer). */
        int cmdSsbo = -1;
        long cmdSlots = 0;
        /** Bumped whenever the slot store is (re)specified: orphaned bytes must
         * never reach a draw, so stale generation stamps fall back to the CPU path. */
        int cmdGen = 0;
        /** Per-slot owner: which cache last wrote each slot. The stamp pair alone
         * cannot detect that a slot's CONTENT was overwritten by a different
         * cache in an earlier frame (same generation, same pending index) - the
         * owner identity check closes that hole. Regrown with the store. */
        ModeCache[] cmdSlotOwners = new ModeCache[64];
        /** CPU-path command upload store (compacted commands). Kept separate from
         * the slot store so a fallback flush never orphans pass-2-written slots. */
        int cmdCpuSsbo = -1;
        /** GL 4.6 parameter buffer: the 4-byte drawcount consumed by
         * glMultiDrawElementsIndirectCount (re-specified per flush). */
        int paramSsbo = -1;
        int vao = -1;
        /** CPU-side scratch for command building (grown lazily). */
        java.nio.ByteBuffer cmdCpu = null;
        /** Counts pool store reallocs for the F3 counters (resize = full re-upload). */
        long reallocs = 0;
        /** Session count of pool store SHRINKS (idle-tail release, maybeShrinkPool).
         *  A shrink uses the same orphaning realloc as growth - it also increments
         *  {@link #reallocs}; this counter splits the F3 line into grow vs shrink. */
        long shrinks = 0;
        /** frameStamp of the last relayoutPool run for this pass: the deferral
         * gate refuses further mid-walk growth relayouts in the same frame
         * (see relayoutPool, 2026-10-07 shadow-fix). */
        long lastRelayoutFrame = -1L;

        PassPool(boolean slim) { this.slim = slim; }
    }

    private static final PassPool POOL_MAIN = new PassPool(false);
    private static final PassPool POOL_SHADOW = new PassPool(true);

    private static PassPool pool(int mode) {
        return mode == MODE_SHADOW ? POOL_SHADOW : POOL_MAIN;
    }

    private static String poolName(PassPool pool) {
        return pool == POOL_SHADOW ? "Shadow" : "Main";
    }

    /** One baked mesh awaiting the pass flush. */
    private static final class PendingDraw {
        final BakedMesh baked;
        final ModeCache cache;
        final int instanceCount;

        PendingDraw(BakedMesh baked, ModeCache cache, int instanceCount) {
            this.baked = baked;
            this.cache = cache;
            this.instanceCount = instanceCount;
        }
    }

    private static final java.util.ArrayList<PendingDraw> PENDING_MAIN = new java.util.ArrayList<>(64);
    private static final java.util.ArrayList<PendingDraw> PENDING_SHADOW = new java.util.ArrayList<>(64);

    /** F3 accessors: pool store realloc count of the last flush (per pass). */
    public static long poolMainReallocs() { return POOL_MAIN.reallocs; }
    public static long poolShadowReallocs() { return POOL_SHADOW.reallocs; }
    /** F3 accessors: session pool store shrink counts (per pass). */
    public static long poolMainShrinks() { return POOL_MAIN.shrinks; }
    public static long poolShadowShrinks() { return POOL_SHADOW.shrinks; }

    // ── F3 VRAM breakdown (per component; pure field math, no GL calls) ──

    /** ALLOCATED bytes of one pass pool (output + EBO + cmd/param stores), F3 only. */
    public static long poolAllocatedBytes(int mode) {
        synchronized (NucleusGpuBaker.class) {
            return poolAllocatedBytes(pool(mode));
        }
    }

    /** USED bytes of one pass pool: the exact live-range math of usedGpuBytes, per mode. */
    public static long poolUsedBytes(int mode) {
        synchronized (NucleusGpuBaker.class) {
            long total = 0L;
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                ModeCache mc = bm.cache(mode);
                total += (long) bm.vertCount * mc.outInstances * outStrideBytes(mode)
                        + (long) bm.idxCount * mc.eboInstances * 4L;
            }
            return total;
        }
    }

    /**
     * ALLOCATED bytes of everything in {@link #allocatedGpuBytes()} EXCEPT the
     * two pass pools: per-cache SSBOs (records/dirty/ctl), counts staging, bone
     * palette and the bake-params UBO.
     */
    public static long cacheNonPoolAllocatedBytes() {
        synchronized (NucleusGpuBaker.class) {
            long total = 0L;
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                total += cacheAllocatedBytes(bm.main);
                total += cacheAllocatedBytes(bm.shadow);
            }
            total += countsStagingBytes;
            if (boneSsbo != -1) total += (long) BONE_PALETTE_SIZE * 16L * 4L;
            if (bakeParamsUbo != -1) total += 88L;
            return total;
        }
    }

    /** USED bytes of the same non-pool coverage as {@link #cacheNonPoolAllocatedBytes()}. */
    public static long cacheNonPoolUsedBytes() {
        synchronized (NucleusGpuBaker.class) {
            long total = 0L;
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                total += cacheUsedBytes(bm.main);
                total += cacheUsedBytes(bm.shadow);
            }
            total += countsStagingBytes;
            if (boneSsbo != -1) total += (long) BONE_PALETTE_SIZE * 16L * 4L;
            if (bakeParamsUbo != -1) total += 88L;
            return total;
        }
    }

    // ── F3 VRAM accounting (NucleusDebug VRAM bar) ─────────────────────
    // Both getters are computed from the CAPACITY FIELDS, never from
    // cacheBytes (that counter is VRAM-only, mirroring this coverage; the CPU
    // heap snapshots recordCopy / dirtyIdx are deliberately unaccounted).

    /**
     * Total GPU bytes actually ALLOCATED by the Tier-1 bake. Coverage: both
     * pass pools (output + EBO stores at their 1.25x-headroom allocation
     * targets, the GPU-count command slot store, the 4-byte GL 4.6 parameter
     * buffer), every live per-cache SSBO (records, dirty list, ctl), counts
     * staging, the bone palette and the bake-params UBO. NOT counted: the
     * CPU-path command store (re-specified per flush via glBufferData, a few
     * KB, no tracked capacity) and the CPU heap snapshots (recordCopy /
     * dirtyIdx - no tracked capacity, not VRAM).
     */
    public static long allocatedGpuBytes() {
        synchronized (NucleusGpuBaker.class) {
            long total = poolAllocatedBytes(POOL_MAIN) + poolAllocatedBytes(POOL_SHADOW);
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                total += cacheAllocatedBytes(bm.main);
                total += cacheAllocatedBytes(bm.shadow);
            }
            total += countsStagingBytes;
            if (boneSsbo != -1) total += (long) BONE_PALETTE_SIZE * 16L * 4L;
            if (bakeParamsUbo != -1) total += 88L;
            return total;
        }
    }

    /**
     * Total GPU bytes USED by the Tier-1 bake on the same coverage as
     * {@link #allocatedGpuBytes()}. Pool ranges use the exact live-range math
     * of relayoutPool's trial pass (vertCount * outInstances * stride +
     * idxCount * eboInstances * 4); per-cache record stores use the record
     * count. Everything without a used tracker (ctl/dirty headers) is reported
     * at its allocated size - the slack there is small or by design.
     */
    public static long usedGpuBytes() {
        synchronized (NucleusGpuBaker.class) {
            long total = 0L;
            for (int mode = MODE_SHADOW; mode <= MODE_MAIN; mode++) {
                for (int i = 0; i < BAKED.size(); i++) {
                    BakedMesh bm = BAKED.get(i);
                    ModeCache mc = bm.cache(mode);
                    total += (long) bm.vertCount * mc.outInstances * outStrideBytes(mode)
                            + (long) bm.idxCount * mc.eboInstances * 4L;
                }
            }
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                total += cacheUsedBytes(bm.main);
                total += cacheUsedBytes(bm.shadow);
            }
            total += countsStagingBytes;
            if (boneSsbo != -1) total += (long) BONE_PALETTE_SIZE * 16L * 4L;
            if (bakeParamsUbo != -1) total += 88L;
            return total;
        }
    }

    private static long poolAllocatedBytes(PassPool pool) {
        return pool.outputAllocBytes + pool.eboAllocBytes
                + pool.cmdSlots * INDIRECT_CMD_BYTES
                + (pool.paramSsbo != -1 ? 4L : 0L);
    }

    /** Per-cache SSBO allocation. The pool-path output/EBO bytes are charged
     * by {@link #poolAllocatedBytes} instead. */
    private static long cacheAllocatedBytes(ModeCache mc) {
        return Math.max(mc.recordSize[0], 0L) + Math.max(mc.dirtyBytes, 0L)
                + Math.max(mc.ctlBytes, 0L);
    }

    private static long cacheUsedBytes(ModeCache mc) {
        long records = mc.recordCount >= 0
                ? (long) mc.recordCount * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE * 4L
                : 0L;
        return records + Math.max(mc.dirtyBytes, 0L) + Math.max(mc.ctlBytes, 0L);
    }

    /** Discards pending collect batches (exception path in the present hook). */
    public static void discardPendingBatches() {
        PENDING_MAIN.clear();
        PENDING_SHADOW.clear();
    }

    private NucleusGpuBaker() {}

    static {
        // Session-end teardown: F3+T / disconnect destroy the companion meshes
        // (MeshRenderCache.clearAll -> IrisCompanionMesh.destroy), but BAKED
        // entries are keyed by mesh identity and would otherwise keep their GL
        // objects (pool ranges, SSBOs, base VAOs/EBOs) alive for the whole
        // session. The hook runs on the render thread inside
        // RenderCacheManager.invalidateAll, next to the other engine caches.
        RenderCacheManager.register(reason -> releaseAll());
    }

    /** Returns true if Tier 1 compute bake is supported and enabled. Cheap after first call. */
    public static boolean isEnabled() {
        if (!HardwareTierManager.isTier1()) {
            return false;
        }
        // nucleusGpuBake config is the primary source; -Dhbm.gpuBake=false is the emergency override.
        if (!com.hbm_m.config.ModClothConfig.get().nucleusGpuBake || !KILL_SWITCH) {
            return false;
        }
        if (!checked) {
            checked = true;
            try {
                var caps = org.lwjgl.opengl.GL.getCapabilities();
                // Baking needs compute + SSBO as a pair: on exotic hardware with
                // ARB_compute_shader but without ARB_shader_storage_buffer_object,
                // glBindBufferBase(SSBO) throws a GL error - cut that off here, ahead
                // of the catch-all in bake (which remains the last line of defense:
                // any dispatch failure disables Tier 1 permanently).
                available = (caps.OpenGL43 || caps.GL_ARB_compute_shader)
                        && (caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object);
                if (!available && !logged) {
                    logged = true;
                    com.hbm_m.main.MainRegistry.LOGGER.info(
                            "[HBM-M] NucleusGpuBaker: no compute shaders (GL<4.3, no ARB) - "
                                    + "Tier 1 disabled, Tier 2/3 stays active");
                }
            } catch (Exception t) {
                available = false;
            }
        }
        return available;
    }

    /** Cull pre-pass available: toggle on, indirect draws present, cull program compiled. */
    private static boolean cullEnabled() {
        return CULL_SWITCH && cullProgramId != -1;
    }

    // ── GPU-count flush path (GL 4.6 / ARB_indirect_parameters) ─────────
    // glMultiDrawElementsIndirectCount lets the pool flush draw WITHOUT the CPU
    // visible-count readback: cull pass 2 writes each entry's indirect command
    // (indexCount = idxCount * GPU visible count) into the pool command store and
    // the flush executes it directly. macOS (GL 4.1 cap) and pre-Polaris AMD /
    // pre-Skylake Intel stacks never take this path - the CPU readback +
    // compaction flush stays the fallback, byte-for-byte as before.
    private static boolean mdicChecked = false;
    private static boolean mdicAvailable = false;

    /** Capability probe, cached once like {@link #isEnabled()}: GL 4.6 core or ARB_indirect_parameters. */
    private static boolean mdicEnabled() {
        if (!mdicChecked) {
            mdicChecked = true;
            try {
                var caps = org.lwjgl.opengl.GL.getCapabilities();
                mdicAvailable = caps.OpenGL46 || caps.GL_ARB_indirect_parameters;
                if (!mdicAvailable) {
                    com.hbm_m.main.MainRegistry.LOGGER.info(
                            "[HBM-M] NucleusGpuBaker: no GL4.6/ARB_indirect_parameters - "
                                    + "pool flush keeps the CPU count readback");
                }
            } catch (Exception t) {
                mdicAvailable = false;
            }
        }
        return mdicAvailable;
    }

    private static long frameStamp() {
        return IrisShadowBatchCollector.renderFrame();
    }

    // ── Optional Hi-Z occlusion for the bake cull (nucleusBakeHiZ) ──────
    // The shared HiZDepthPyramid is rebuilt at most once per frame PER PASS
    // (shadow and main read different depth textures), right before that
    // pass's first cull dispatch of the frame. A missing depth source falls
    // back to frustum-only permanently for the frame; any GL error during the
    // build/bind disables the feature for the SESSION (logged once) - the
    // fallback is exactly the previous behavior, so the flag is strictly
    // opt-in and seamless.
    private static boolean hiZSessionDisabled = false;
    private static boolean hiZSessionLogged = false;
    private static boolean hiZActiveLogged = false;
    /** Frame stamp of the last build ATTEMPT per pass (success or fallback). */
    private static long hiZAttemptShadowFrame = -1L, hiZAttemptMainFrame = -1L;
    /** Outcome of the current frame's attempt per pass. */
    private static boolean hiZShadowActive = false, hiZMainActive = false;
    /** Viewport of the pyramid source texture at the last successful build. */
    private static int hiZViewWidth = 0, hiZViewHeight = 0;
    private static final java.nio.IntBuffer HI_Z_VIEWPORT = org.lwjgl.BufferUtils.createIntBuffer(16);
    private static final long[] HI_Z_OCCLUDED_OUT = new long[1];

    /**
     * Builds the Hi-Z pyramid for this pass unless already built this frame;
     * returns the {@code uHiZMode} to dispatch the cull with (1 = pyramid,
     * 0 = frustum-only). Depth source: under an active shaderpack the ACTIVE
     * gbuffer depth attachment of the bound FBO (depthtex0 while block entities
     * render - Iris renders translucents after them; shadowtex0 while the shadow
     * pass renders); the vanilla main render target depth texture is used only
     * in the MAIN pass (never as a shadow-pass fallback - light-space test needs
     * light-space depth). Success requires BOTH a clean GL error sweep AND the
     * pyramid actually being built ({@link HiZDepthPyramid#isReady()}); either
     * failure disables Hi-Z for the session with one log.
     */
    private static int ensureBakeHiZ(int mode) {
        // Shadow pass never builds the pyramid: the occlusion test projects AABBs
        // through the LINEAR shadowProj * SHADOW_MV matrix, while shader packs
        // distort shadow clip coordinates in the shadow VERTEX shader, so
        // shadowtex0 depth lives in distorted light space - Hi-Z culling there
        // would falsely cull visible machines (holes in shadows). Also, the
        // shadow viewport (shadow map resolution) differs from the screen, so
        // building both passes through the singleton reallocated the pyramid
        // texture twice per frame. Shadows keep frustum-only culling.
        if (mode == MODE_SHADOW || hiZSessionDisabled || !com.hbm_m.config.ModClothConfig.get().nucleusBakeHiZ) {
            return 0;
        }
        long f = frameStamp();
        if (mode == MODE_SHADOW) {
            if (hiZAttemptShadowFrame == f) {
                return hiZShadowActive ? 1 : 0;
            }
        } else {
            if (hiZAttemptMainFrame == f) {
                return hiZMainActive ? 1 : 0;
            }
        }
        boolean active = false;
        try {
            // Drain stale errors so the post-build sweep attributes errors to the
            // pyramid build and not to unrelated earlier state.
            while (GL11.glGetError() != GL11.GL_NO_ERROR) {
                // discard
            }
            // Pack path: active depth attachment of the CURRENTLY bound FBO -
            // gbuffer depthtex0 in the main pass (translucents render after block
            // entities), shadowtex0 during the shadow pass. The vanilla main
            // render target depth is a legal source ONLY in the main pass: the
            // shadow-pass occlusion test runs in LIGHT-space clip space, where
            // camera-space scene depth sampled at shadow-map screen coordinates
            // is semantically meaningless - if the shadow FBO's depth attachment
            // is not a texture, fall back to frustum-only for the frame instead.
            int depthTex = 0;
            if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()) {
                int irisDepth = MdiBatchCoordinator.getActiveGbufferDepthTexture();
                if (irisDepth > 0) {
                    depthTex = irisDepth;
                }
            }
            if (depthTex <= 0 && mode != MODE_SHADOW) {
                var mainTarget = Minecraft.getInstance().getMainRenderTarget();
                depthTex = mainTarget != null ? mainTarget.getDepthTextureId() : 0;
            }
            HI_Z_VIEWPORT.clear();
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, HI_Z_VIEWPORT);
            int w = HI_Z_VIEWPORT.get(2);
            int h = HI_Z_VIEWPORT.get(3);
            if (depthTex > 0 && w > 0 && h > 0) {
                // The downsample READS the depth texture currently attached to the
                // bound FBO - the exact pattern MdiBatchCoordinator already runs
                // every frame. The pack's FBO is never unbound/rebound (pack-owned
                // state); HiZDepthPyramid.regenerate issues the
                // GL_FRAMEBUFFER_BARRIER_BIT that orders prior rasterized depth
                // before the shader reads, and a texture READ while attached is
                // not a feedback loop (only the pyramid texture is written).
                HiZDepthPyramid.get().regenerate(depthTex, w, h);
                boolean glOk = true;
                while (GL11.glGetError() != GL11.GL_NO_ERROR) {
                    glOk = false;
                }
                // The absence of GL errors is NOT a success criterion: when the
                // downsample shaders fail to compile, HiZDepthPyramid.ensureShaders
                // latches destroyed (no GL error) and regenerate() silently no-ops -
                // getPyramidTextureId() would then serve the 1x1 placeholder while
                // uViewSize/uPyramidLevels describe the real viewport, and the
                // out-of-bounds texelFetches (undefined, commonly 0.0) would cull
                // every frustum-passed instance. A not-ready pyramid is a failed
                // build: frustum-only for the session, logged once.
                active = glOk && HiZDepthPyramid.get().isReady();
                if (active) {
                    hiZViewWidth = w;
                    hiZViewHeight = h;
                    if (!hiZActiveLogged) {
                        hiZActiveLogged = true;
                        com.hbm_m.main.MainRegistry.LOGGER.info(
                                "[HBM-M] NucleusGpuBaker: bake Hi-Z occlusion active (pass={}, depthTex={}, {}x{})",
                                mode == MODE_SHADOW ? "shadow" : "main", depthTex, w, h);
                    }
                } else {
                    disableBakeHiZForSession();
                }
            }
        } catch (Exception t) {
            disableBakeHiZForSession();
        }
        if (mode == MODE_SHADOW) {
            hiZAttemptShadowFrame = f;
            hiZShadowActive = active;
        } else {
            hiZAttemptMainFrame = f;
            hiZMainActive = active;
        }
        return active ? 1 : 0;
    }

    /** GL error during the build, a dispatch failure, or the pyramid not actually
     * built (downsample shader compile failure): frustum-only until restart, one log. */
    private static void disableBakeHiZForSession() {
        hiZSessionDisabled = true;
        if (!hiZSessionLogged) {
            hiZSessionLogged = true;
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: bake Hi-Z occlusion disabled for this session "
                            + "(pyramid build failed or GL error) - bake cull stays frustum-only");
        }
    }

    /**
     * Cheap (no GL work) preview of {@link #ensureBakeHiZ}: whether this pass's
     * cull would dispatch with the pyramid this frame. True when the build has
     * not been attempted yet this frame (the cull it precedes will attempt it)
     * or when this frame's attempt succeeded. Used by bakeEntry to keep re-running
     * the cull pre-pass while the occlusion phase is live: with Hi-Z on, the cull
     * output is a function of the depth pyramid TOO, so the pure
     * planes+records cache-hit skip would let an occluded instance stay hidden
     * after its occluder disappears while the camera stands still (frustum-only
     * cull output was plane-pure, the skip was lossless there). While Hi-Z is
     * active this re-culls every frame per cache - the same always-recull
     * discipline the MDI coordinator uses while its occlusion gate is on.
     */
    private static boolean bakeHiZPlanned(int mode) {
        // Mirrors ensureBakeHiZ: shadow pass is always frustum-only (see there).
        if (mode == MODE_SHADOW || hiZSessionDisabled || !com.hbm_m.config.ModClothConfig.get().nucleusBakeHiZ) {
            return false;
        }
        long f = frameStamp();
        if (mode == MODE_SHADOW) {
            return hiZAttemptShadowFrame != f || hiZShadowActive;
        }
        return hiZAttemptMainFrame != f || hiZMainActive;
    }

    // ── F3 bake-cull diagnostics (read by NucleusDebug.appendDebugLines) ──
    // Totals of the LAST frame that baked each pass: recordBakeStats() resets on
    // a new renderFrame stamp, so (total, visible) always belong to the same
    // frame - self-consistent without any reset ordering against
    // NucleusDebug.onFrameStart (which runs between the shadow and main phases
    // and must not wipe the shadow numbers). Accumulation is single-threaded
    // (render thread); volatile keeps the F3 reader safe.
    private static volatile long bakeMainFrame = -1L, bakeMainTotal = 0L, bakeMainVisible = 0L;
    private static volatile long bakeShadowFrame = -1L, bakeShadowTotal = 0L, bakeShadowVisible = 0L;
    // Optional Hi-Z phase reporting: instances that PASSED the frustum test but
    // were culled by occlusion (ctl word 15) and whether the pass's cull ran
    // with the pyramid (hi-z) or frustum-only, of the last frame that baked.
    private static volatile long bakeMainOccluded = 0L, bakeShadowOccluded = 0L;
    private static volatile boolean bakeMainHiZ = false, bakeShadowHiZ = false;

    private static void recordBakeStats(int mode, int total, long visible, long occluded, boolean hiZ) {
        long f = frameStamp();
        if (mode == MODE_SHADOW) {
            if (bakeShadowFrame != f) {
                bakeShadowFrame = f;
                bakeShadowTotal = 0L;
                bakeShadowVisible = 0L;
                bakeShadowOccluded = 0L;
                bakeShadowHiZ = false;
            }
            bakeShadowTotal += total;
            bakeShadowVisible += visible;
            bakeShadowOccluded += occluded;
            bakeShadowHiZ = hiZ;
        } else {
            if (bakeMainFrame != f) {
                bakeMainFrame = f;
                bakeMainTotal = 0L;
                bakeMainVisible = 0L;
                bakeMainOccluded = 0L;
                bakeMainHiZ = false;
            }
            bakeMainTotal += total;
            bakeMainVisible += visible;
            bakeMainOccluded += occluded;
            bakeMainHiZ = hiZ;
        }
    }

    /** F3 accessors: frame stamp + instance totals of the last baked pass (main). */
    public static long lastBakeMainFrame() { return bakeMainFrame; }
    public static long lastBakeMainTotal() { return bakeMainTotal; }
    public static long lastBakeMainVisible() { return bakeMainVisible; }
    /** F3 accessors: occlusion-culled count + mode of the last baked pass (main). */
    public static long lastBakeMainOccluded() { return bakeMainOccluded; }
    public static boolean lastBakeMainHiZ() { return bakeMainHiZ; }

    /** F3 accessors: frame stamp + instance totals of the last baked pass (shadow). */
    public static long lastBakeShadowFrame() { return bakeShadowFrame; }
    public static long lastBakeShadowTotal() { return bakeShadowTotal; }
    public static long lastBakeShadowVisible() { return bakeShadowVisible; }
    /** F3 accessors: occlusion-culled count + mode of the last baked pass (shadow). */
    public static long lastBakeShadowOccluded() { return bakeShadowOccluded; }
    public static boolean lastBakeShadowHiZ() { return bakeShadowHiZ; }

    // ── F3 span-expansion diagnostics ──────────────────────────────────
    // Per-frame totals of dirty-list (span) expansions — dispatches issued with a
    // partial dirty list (0 < dirtyCount < recordCount) and dirty instances
    // expanded — per pass. Same last-baked-frame reset discipline as
    // recordBakeStats: totals belong to the last frame that span-expanded, so a
    // quiet frame simply reads as idle. Single-threaded accumulation (render
    // thread); volatile keeps the F3 reader safe.
    private static volatile long expMainDirtyFrame = -1L,
            expMainDirtyDispatches = 0L, expMainDirtyInstances = 0L;
    private static volatile long expShadowDirtyFrame = -1L,
            expShadowDirtyDispatches = 0L, expShadowDirtyInstances = 0L;

    private static void recordExpansionStats(int mode, boolean dirtyMode, int dirtyCount) {
        if (!dirtyMode || dirtyCount <= 0) {
            return;
        }
        long f = frameStamp();
        if (mode == MODE_SHADOW) {
            if (expShadowDirtyFrame != f) {
                expShadowDirtyFrame = f;
                expShadowDirtyDispatches = 0L;
                expShadowDirtyInstances = 0L;
            }
            expShadowDirtyDispatches++;
            expShadowDirtyInstances += dirtyCount;
        } else {
            if (expMainDirtyFrame != f) {
                expMainDirtyFrame = f;
                expMainDirtyDispatches = 0L;
                expMainDirtyInstances = 0L;
            }
            expMainDirtyDispatches++;
            expMainDirtyInstances += dirtyCount;
        }
    }

    /** F3 accessors: span-expansion totals of the last span-expanded frame (main). */
    public static long lastSpanMainDirtyFrame() { return expMainDirtyFrame; }
    public static long lastSpanMainDirtyDispatches() { return expMainDirtyDispatches; }
    public static long lastSpanMainDirtyInstances() { return expMainDirtyInstances; }

    /** F3 accessors: span-expansion totals of the last span-expanded frame (shadow). */
    public static long lastSpanShadowDirtyFrame() { return expShadowDirtyFrame; }
    public static long lastSpanShadowDirtyDispatches() { return expShadowDirtyDispatches; }
    public static long lastSpanShadowDirtyInstances() { return expShadowDirtyInstances; }

    /**
     * Shared staging SSBO for the batched visible-count readback
     * ({@link #resolvePendingCounts}): one 64-byte ctl header per pending draw,
     * filled GPU-side with glCopyBufferSubData (async, no CPU sync), then read
     * back with ONE glGetBufferSubData per pool flush. Grows like the pool
     * stores; freed by releaseResourcesInternal.
     */
    private static int countsStagingSsbo = -1;
    private static long countsStagingBytes = 0;
    /** CPU-side mirror of the staging SSBO; grown to the batch size on demand (untracked memAlloc: freed by releaseResourcesInternal). */
    private static java.nio.ByteBuffer ctlBatchReadback = org.lwjgl.system.MemoryUtil.memAlloc(4096);
    /** 4-byte drawcount scratch for the GL 4.6 parameter-buffer upload (re-specified per flush). */
    private static java.nio.ByteBuffer mdicCountBuf = org.lwjgl.system.MemoryUtil.memAlloc(4);

    /**
     * Tier 1 shadow flush: bakes shadow collector instance records and submits
     * a single draw call per part renderer using the active pack shadow shader.
     * The shader program must already be applied with its framebuffer bound.
     *
     * @param camX/Y/Z player camera position (world); shadow records are light-space
     *                 (exact decompositions of the Iris shadow poseStack, with the
     *                 camera offset added to the translation) and ModelViewMat = T(-cam)
     *                 folds that offset back out at draw time.
     * @return true only when the Tier-1 bake AND draw actually went through and
     *         the pool flush issued at least one draw command; false when falling
     *         back to Tier 2/3. A false here covers: capability/refusal refusals
     *         (as before), unresolved ModelViewMat/ProjMat on the pack shadow
     *         program, all entries skipped (companion unavailable/empty), and a
     *         0-command flush. Skipped entries are collected into the bakeFailed
     *         list so the Tier-3 companion batch covers them whenever the Tier-1
     *         draw itself went through.
     */
    public static boolean bakeAndDrawShadow(List<IrisShadowBatchCollector.Entry> entries,
                                            ShaderInstance shader, Matrix4f shadowProj,
                                            float camX, float camY, float camZ) {
        if (!isEnabled() || !RenderSystem.isOnRenderThread() || shader == null) {
            return false;
        }
        bakeGlDrainForeign("shadow bake entry");
        // Shadow ModelViewMat is T(-cam) and NOTHING else. The light rotation rides
        // INSIDE the recorded instance records (each record is an exact pos/quat
        // decomposition of the Iris shadow poseStack MV_shadow * T(bePos - cam) * part),
        // and RenderSystem's modelview is identity during the Iris shadow pass (Iris
        // only swaps the projection; it never touches the modelview). Construction
        // therefore uses the camera only. A foreign non-identity matrix left over at
        // flush time is logged once and IGNORED - absorbing it into the shadow map
        // would reintroduce exactly the camera dependence this contract removed.
        Matrix4f flushMv = RenderSystem.getModelViewMatrix();
        if (flushMv != null && !flushMv.equals(new Matrix4f()) && !foreignShadowMvLogged) {
            foreignShadowMvLogged = true;
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: non-identity RenderSystem modelview at shadow "
                            + "flush - ignored (shadow ModelViewMat stays T(-cam); the light "
                            + "rotation lives inside the instance records)");
        }
        SHADOW_MV.translation(-camX, -camY, -camZ);
        Matrix4f worldToClip = new Matrix4f(shadowProj).mul(SHADOW_MV);
        float[] planes = new float[24];
        extractFrustumPlanes(worldToClip, planes);
        long frustumStamp = stampPlanes(planes);

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        // NOTE: the *BINDING query pnames (0x2F43), NOT the target enums (0x2F40/0x2F42) -
        // querying a target enum is GL_INVALID_ENUM and the returned garbage then leaks
        // into the restore-binds below.
        int prevDrawIndirect = GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING);
        int prevDispatchIndirect = GL11.glGetInteger(GL43.GL_DISPATCH_INDIRECT_BUFFER_BINDING);
        // Tier 2/3 and IrisRenderBatch all reset this before applying the pack program;
        // the bake path did not - blockEntityId stuck at whatever BE Iris rendered last,
        // a value that changes frame to frame (pack special cases keyed on it flicker).
        // Neutral 0 is written unconditionally and pack-agnostically; the full
        // rationale (stale uniform, one-uniform-per-draw-call MDI limit) lives in
        // IrisExtendedShaderAccess.setCurrentRenderedBlockEntity.
        int previousBlockEntityId = com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                .setCurrentRenderedBlockEntity(0);
        try {
            if (programId == -1 && !initProgram()) {
                return false;
            }
            ensureBoneBuffer();
            // Draw state mirrors Tier 2 drawShadowInstances: multiblock machinery has
            // mixed winding order; culling is disabled to prevent missing faces.
            try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();

                // 2026-10-07 shadow-fix: entries whose bake is refused this
                // frame are collected and re-drawn through the Tier-3 companion
                // batch AFTER the pool flush below, instead of vanishing from
                // the shadow map for the frame (the main path has always had
                // this per-entry fallback).
                java.util.ArrayList<IrisShadowBatchCollector.Entry> bakeFailed = null;
                // Honest Tier-1 success reporting (2026-10-15): true only when the
                // pool flush actually issued draw commands. Entries silently
                // skipped here (companion unavailable / unbuilt / empty, or a
                // null getOrCreate) used to vanish from the shadow map while
                // bakeAndDrawShadow still returned true - which gated the Tier
                // 2/3 fallback OFF at the collector. They now join bakeFailed
                // (same as a bakeEntry refusal) and the frame's outcome is
                // decided by the flush's issued-command count below.
                boolean anyCollected = false;

                if (GlDebugMarkers.ENABLED) {
                    GlDebugMarkers.pushGroup("HBM_Shadow_BakeCollect");
                }
                try {
                    for (int i = 0; i < entries.size(); i++) {
                        IrisShadowBatchCollector.Entry e = entries.get(i);
                        if (e.count <= 0) {
                            continue;
                        }
                        IrisCompanionMesh mesh = e.getCompanionMesh();
                        if (mesh == null || !mesh.isBuilt()) {
                            bakeSkipLog(e, "companion unbuilt/failed");
                            if (bakeFailed == null) {
                                bakeFailed = new java.util.ArrayList<>();
                            }
                            bakeFailed.add(e);
                            continue;
                        }
                        if (mesh.getMeshVertexCount() <= 0 || mesh.getMeshStrideBytes() <= 0
                                || mesh.getMeshIndices() == null || mesh.getMeshIndices().length == 0) {
                            bakeSkipLog(e, "companion empty mesh (verts=" + mesh.getMeshVertexCount() + ")");
                            if (bakeFailed == null) {
                                bakeFailed = new java.util.ArrayList<>();
                            }
                            bakeFailed.add(e);
                            continue;
                        }
                        BakedMesh baked = getOrCreate(mesh);
                        if (baked == null) {
                            if (bakeFailed == null) {
                                bakeFailed = new java.util.ArrayList<>();
                            }
                            bakeFailed.add(e);
                            continue;
                        }
                        FloatBuffer records = e.data.duplicate();
                        records.flip();
                        if (!bakeEntry(baked, records, e.count, MODE_SHADOW, planes, frustumStamp, worldToClip)) {
                            // Bake refused (budget refusal, upload or expansion
                            // failure): re-draw via the Tier-3 companion batch after
                            // the flush instead of skipping silently.
                            if (bakeFailed == null) {
                                bakeFailed = new java.util.ArrayList<>();
                            }
                            bakeFailed.add(e);
                            continue;
                        }

                        // ── Global-batch collect path: defer the draw to the single
                        // flushPool below.
                        PENDING_SHADOW.add(new PendingDraw(baked, baked.shadow, e.count));
                        anyCollected = true;
                    }
                } finally {
                    if (GlDebugMarkers.ENABLED) {
                        GlDebugMarkers.popGroup();
                    }
                }

                // ── Global-batch flush: ONE multi-draw for every entry baked above ──
                boolean matricesOk;
                int issuedCommands = 0;
                if (GlDebugMarkers.ENABLED) {
                    GlDebugMarkers.pushGroup("HBM_Shadow_MultiDrawFlush");
                }
                try {
                    GlStateManager._glUseProgram(shader.getId());
                    // Fail-closed matrices (2026-10-15): a shadow record stores world
                    // + camera, so a ModelViewMat/ProjMat that cannot be resolved on
                    // the active pack program would send the multi-draw off-map with
                    // NO error. That is a Tier-1 refusal, not a silent skip.
                    matricesOk = setMatrices(shader, shadowProj, SHADOW_MV);
                    if (matricesOk) {
                        // Explicit unit-0 atlas bind (setShaderTexture alone mutates the
                        // VANILLA ShaderInstance's sampler state, not the applied pack program).
                        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
                        RenderSystem.bindTexture(Minecraft.getInstance().getTextureManager()
                                .getTexture(TextureAtlas.LOCATION_BLOCKS).getId());
                        issuedCommands = flushPool(POOL_SHADOW, MODE_SHADOW, shader, PENDING_SHADOW);
                    }
                    PENDING_SHADOW.clear();

                    // Pack program is active; restore previous VAO and VBO state cleanly.
                    GlStateManager._glUseProgram(shader.getId());
                    com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
                } finally {
                    if (GlDebugMarkers.ENABLED) {
                        GlDebugMarkers.popGroup();
                    }
                }

                // ── Tier-3 fallback for entries the bake refused this frame ──
                // Runs after the pool flush and the VAO restore, so the
                // companion batch captures the caller's GL state as its
                // "previous". Only engaged when the Tier-1 draw itself went
                // through (issuedCommands > 0): when it did NOT, Tier 2/3 at
                // the collector re-draw the WHOLE entry list, and a Tier-3
                // redraw here would double-draw those same entries.
                // Cannot double-draw otherwise: only entries whose bake was
                // refused or skipped are re-drawn; the batch walk reads the
                // same light-space records the collector appended (ENTRIES is
                // drained by the flush's finally, after this returns).
                if (issuedCommands > 0 && bakeFailed != null) {
                    IrisShadowBatchCollector.redrawFailedBakeEntries(
                            bakeFailed, shadowProj, camX, camY, camZ);
                }

                if (!matricesOk || issuedCommands == 0) {
                    String reason = !matricesOk
                            ? "unresolved matrices (ModelViewMat/ProjMat not found on the pack shadow program)"
                            : (anyCollected
                                    ? "pool flush issued 0 draw commands"
                                    : "all entries skipped (companion unavailable/empty)");
                    logShadowDecline(reason, shader);
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException t) {
            // Data-shape bug: skip this entry, keep Tier 1 alive (see bakeMain).
            long now = System.currentTimeMillis();
            if (now - dataFailLogMs > 10000) {
                dataFailLogMs = now;
                com.hbm_m.main.MainRegistry.LOGGER.error(
                        "[HBM-M] NucleusGpuBaker shadow bake failed (data error, bake stays enabled) "
                                + "- falling back to Tier 2", t);
            }
            // The Throwable catch below clears the shadow pending list; this catch
            // returned without it, so entries appended earlier in the walk survived
            // and the next flush emitted DUPLICATE draw commands for the same cache
            // (z-fighting/flicker) - flushPool has no per-cache dedup.
            PENDING_SHADOW.clear();
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            return false;
        } catch (Exception t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[HBM-M] NucleusGpuBaker shadow bake failed - "
                    + "falling back to Tier 2", t);
            available = false;
            PENDING_SHADOW.clear();
            releaseResourcesInternal();
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            return false;
        } finally {
            com.hbm_m.client.render.shader.ShaderBindResync.markForeignProgramBind();
            com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                    .restoreCurrentRenderedBlockEntity(previousBlockEntityId);
            GL40.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, prevDrawIndirect);
            GL43.glBindBuffer(GL43.GL_DISPATCH_INDIRECT_BUFFER, prevDispatchIndirect);
        }
    }

    private static final int MODE_SHADOW = 0;
    private static final int MODE_MAIN = 1;
    // Control block layout (uint words) — the EBO-build dispatch reads a full
    // (x,y,z) triplet, so regions must not overlap:
    //   word 0      : visibleCount (atomic counter, reset by CPU before cull)
    //   words 1..3  : reserved (the expansion is frustum-independent and dispatches directly)
    //   words 4..8  : glDrawElementsIndirect command (indexCount, 1, 0, 0, 0)
    //   words 9..11 : EBO-build glDispatchComputeIndirect triplet (X, 1, 1)
    //   words 12..14: pad
    //   word 15     : culled-by-occlusion counter (written by pass 0 when uHiZMode = 1)
    //   words 16..  : compacted visible instance indices
    /** Byte offset of the glDrawElementsIndirect command (word 4). */
    private static final int DRAW_CMD_OFFSET = 16;
    /** Byte offset of the EBO-build glDispatchComputeIndirect triplet (word 9). */
    private static final int EBO_DISPATCH_OFFSET = 36;
    /** Control block header words before the visible-index list. */
    private static final int CTL_HEADER_WORDS = 16;

    private static final Matrix4f MAIN_MV = new Matrix4f();
    private static final Matrix4f SHADOW_MV = new Matrix4f();
    /** Scratch for the record-space -> clip matrix uniform of the cull pass 0. */
    private static final float[] CLIP_MAT_FLOATS = new float[16];

    /**
     * Tier 1 main pass, GLOBAL-BATCH collect variant: identical bake (cache check +
     * record upload + cull + expansion into this mesh's pool range) but NO draw -
     * the baked mesh is appended to the pending list and drawn by the single
     * end-of-phase glMultiDrawElementsIndirect in {@link #presentMainBatch}.
     * Returns false when the bake is refused (e.g. cache-budget admission), so the
     * caller renders the mesh through the existing Tier-2/3 instanced fallback.
     */
    public static boolean bakeMain(FloatBuffer records, int count, IrisCompanionMesh mesh,
                                   ShaderInstance packShader, Matrix4f proj, Matrix4f viewRot,
                                   float relCamX, float relCamY, float relCamZ) {
        if (!isEnabled() || !RenderSystem.isOnRenderThread() || packShader == null
                || mesh == null || !mesh.isBuilt() || count <= 0 || records == null) {
            return false;
        }
        bakeGlDrainForeign("main bake entry");
        // Same matrix contract as the shadow bake (see bakeAndDrawShadow).
        MAIN_MV.set(viewRot).translate(-relCamX, -relCamY, -relCamZ);
        float[] planes = new float[24];
        // worldToClip feeds both the frustum planes and the optional Hi-Z phase
        // of the cull.
        Matrix4f worldToClip = new Matrix4f(proj).mul(MAIN_MV);
        extractFrustumPlanes(worldToClip, planes);
        long frustumStamp = stampPlanes(planes);

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        // *BINDING query pnames - see the note in bakeAndDrawShadow.
        int prevDrawIndirect = GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING);
        int prevDispatchIndirect = GL11.glGetInteger(GL43.GL_DISPATCH_INDIRECT_BUFFER_BINDING);
        // The compute dispatches below leave programId bound; restoring the caller's
        // program keeps Iris's ExtendedShader.lastApplied skip-gate consistent with
        // the real state for every consumer after this flush (the epilogue binds of
        // 0 in the other compute passes were the per-frame poison of the same gate).
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousBlockEntityId = com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                .setCurrentRenderedBlockEntity(0);
        try {
            if (programId == -1 && !initProgram()) {
                return false;
            }
            BakedMesh baked = getOrCreate(mesh);
            if (baked == null) {
                return false;
            }
            boolean success;
            if (GlDebugMarkers.ENABLED) {
                GlDebugMarkers.pushGroup("HBM_BakeMain_" + mesh.getDebugName());
            }
            try {
                success = bakeEntry(baked, records, count, MODE_MAIN, planes, frustumStamp, worldToClip);
            } finally {
                if (GlDebugMarkers.ENABLED) {
                    GlDebugMarkers.popGroup();
                }
            }
            if (!success) {
                return false;
            }
            // Successful bake: flushBatchIris returns here and the Tier-2/3
            // companion fallback never runs, so IrisCompanionMesh's own low-demand
            // decay would freeze with the lightmap ring parked at its old peak.
            // Feed the idle counter instead; a sustained lull frees the buffers
            // (see IrisCompanionMesh.noteBakeSuccess).
            baked.mesh.noteBakeSuccess();
            PENDING_MAIN.add(new PendingDraw(baked, baked.main, count));
            return true;
        } catch (IllegalArgumentException t) {
            // Data-shape bug (buffer bounds, bad counts): the GL context is fine, so
            // do NOT kill Tier 1 for the whole session (2026-10-05: one out-of-range
            // dirty index permanently disabled bake). Skip the entry; uploadDirtyRecords
            // invalidates its cache so the next call takes the clean full path.
            long now = System.currentTimeMillis();
            if (now - dataFailLogMs > 10000) {
                dataFailLogMs = now;
                com.hbm_m.main.MainRegistry.LOGGER.error(
                        "[HBM-M] NucleusGpuBaker main bake failed (data error, bake stays enabled) "
                                + "- falling back to companion path", t);
            }
            return false;
        } catch (Exception t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[HBM-M] NucleusGpuBaker main bake failed - "
                    + "falling back to companion path", t);
            available = false;
            releaseResourcesInternal();
            return false;
        } finally {
            com.hbm_m.client.render.shader.ShaderBindResync.markForeignProgramBind();
            com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                    .restoreCurrentRenderedBlockEntity(previousBlockEntityId);
            GL40.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, prevDrawIndirect);
            GL43.glBindBuffer(GL43.GL_DISPATCH_INDIRECT_BUFFER, prevDispatchIndirect);
            com.mojang.blaze3d.platform.GlStateManager._glUseProgram(previousProgram);
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
    }

    // ── Bake entry: cache check + record upload + cull + expansion ─────

    /**
     * Ensures the output vertex data for {@code count} instances of this mesh/mode is
     * ready in VRAM. Returns false only on a hard failure (caller falls back).
     * <p>
     * Cache hit conditions: records equal to the snapshot (per-record diff, see
     * {@link #diffRecords}) AND same bone epoch AND valid output → skip upload +
     * expansion entirely. A PARTIAL diff hit uploads only the dirty records and
     * re-expands only the dirty instances (dirty-list dispatch). Every full-bake
     * path - cache alloc/first bake, record-count change, bone-epoch change,
     * output/ctl/EBO realloc, kill switches - dispatches in IDENTITY mode
     * (y = instance index) and never reads the dirty list, so a full re-bake can
     * never consume stale dirty-list state. Unchanged frustum stamp additionally
     * skips the cull pre-pass (pure draw). When the budget cannot admit a cache,
     * the entry is refused (false) and the caller falls back to Tier 2/3.
     */
    private static boolean bakeEntry(BakedMesh baked, FloatBuffer records, int count, int mode,
                                     float[] planes, long frustumStamp, Matrix4f worldToClip) {
        ModeCache mc = baked.cache(mode);
        // 2026-10-07 shadow-fix relayout deferral: snapshotted BEFORE this bake's
        // own relayouts stamp the pool (see relayoutPool) - admission, capacity
        // growth and EBO expansion may relayout only when no relayout ran for
        // this pass earlier in the frame's walk. A deferral refusal is a
        // ONE-frame soft skip (the entry is refused this frame, the next
        // frame's walk retries with no backoff).
        boolean relayoutAllowed = !poolRelayoutDeferred(mode);
        if (!cacheAllocated(mc)) {
            // 2026-10-07 shadow-fix: two soft-skip reasons checked BEFORE
            // admission is attempted - the relayout deferral above and the
            // admission backoff window (a previous budget refusal; the entry
            // stays refused without re-attempting the admission churn every
            // frame).
            if (!relayoutAllowed || admissionBackoffActive(mc)
                    || !allocateCache(baked, mc, count, mode)) {
                if (relayoutAllowed) {
                    stampAdmissionRefusal(mc);
                }
                return false;
            }
        }
        // Grow the output range BEFORE the diff/cull/expansion so a pool realloc
        // invalidates only state this bake then rebuilds (cull + expansion) -
        // never state it has already built.
        // A refusal (cache budget or relayout deferral) parks the range and
        // refuses this frame's entry - the admission-failure contract.
        if (!ensureOutputCapacity(baked, mc, count, mode, relayoutAllowed)) {
            // Only a real budget refusal arms the backoff window; a deferral
            // refusal is the one-frame soft skip documented above.
            if (relayoutAllowed) {
                stampAdmissionRefusal(mc);
            }
            return false;
        }

        int floats = count * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        boolean recordsChanged;
        // dirtyMode is true only for a partial diff hit (0 < dirtyCount < count):
        // dirtyCount == count degenerates to a full re-bake, which runs identity
        // mode with one contiguous whole-buffer upload instead of count runs.
        boolean dirtyMode = false;
        int dirtyCount = 0;
        if (mc.outputValid && mc.recordCount == count && mc.boneKey == boneKey(baked)) {
            dirtyCount = diffRecords(mc, records, count, mode);
            recordsChanged = dirtyCount > 0;
            dirtyMode = dirtyCount > 0 && dirtyCount < count;
        } else {
            recordsChanged = true;
        }
        boolean cull = cullEnabled();
        boolean frustumChanged = !mc.cullValid || mc.frustumStamp != frustumStamp;

        if (GlDebugMarkers.ENABLED && GlDebugMarkers.isActive()) {
            GlDebugMarkers.insertMarker("BakeDecision: " + baked.debugTag()
                    + " recChanged=" + recordsChanged
                    + " dirtyMode=" + dirtyMode
                    + " dirtyCount=" + dirtyCount
                    + " cull=" + cull
                    + " frustumChanged=" + frustumChanged);
        }

        if (recordsChanged) {
            if (!ensureCacheRecords(mc, (long) floats * 4L)) {
                // Budget refused the record-buffer growth (same contract as
                // admission failure): refuse the entry, caller falls back.
                stampAdmissionRefusal(mc);
                return false;
            }
            // Dirty span upload lands in recordSsbo BEFORE the cull dispatch below
            // (the cull reads recordSsbo; the GL order + runCull's BUFFER_UPDATE
            // barrier make the fresh records visible to it).
            boolean uploaded = dirtyMode
                    ? uploadDirtyRecords(records, count, mc, dirtyCount)
                    : uploadRecords(records, count, mc.recordSsbo);
            if (!uploaded) {
                // Self-heal: drop the snapshot so the next call takes the FULL path
                // (fresh diff, fresh dirtyIdx) instead of trusting a shadow whose
                // GPU write failed - never mark a snapshot synced on upload failure.
                mc.recordCopy = null;
                mc.recordCount = -1;
                mc.outputValid = false;
                return false;
            }
            bakeGlError("upload records");
        }
        // LRU stamp: refreshed ONLY after the admission and growth steps above
        // succeeded. A budget refusal here refuses the entry (Tier-2/3 fallback) while
        // path while the parked pool range stayed allocated AND lastUseFrame
        // kept up with the current frame - the sole eviction gate
        // (releaseCacheIfStale: now - lastUseFrame > LRU_FRAME_LIMIT) could
        // never fire, so the range was a session-long "zombie". Now a refusing
        // cache ages out of evictStaleCaches like any other idle cache.
        mc.lastUseFrame = frameStamp();

        // Cull pre-pass: cheap (instance-count work). Re-run whenever the frustum moved,
        // the records changed, or the occlusion phase is live this frame (with Hi-Z the
        // cull output also depends on the freshly rebuilt depth pyramid - the pure
        // planes+records skip would keep an occluded instance hidden after its occluder
        // is removed while the camera stands still); its indirect words then drive
        // expansion and draw.
        if (cull && (recordsChanged || frustumChanged || bakeHiZPlanned(mode))) {
            if (GlDebugMarkers.ENABLED) {
                GlDebugMarkers.pushGroup("HBM_CullPrePass_" + baked.debugTag());
            }
            try {
                int hiZModeUsed = runCull(baked, mc, count, planes, worldToClip, mode);
                if (hiZModeUsed < 0) {
                    // ctl growth refused (budget): the entry keeps its previous
                    // valid state but is not re-culled this frame - skip it before
                    // the unconditional cull-valid stamping below.
                    return false;
                }
                mc.frustumStamp = frustumStamp;
                mc.cullValid = true;
                mc.lastCullHiZ = hiZModeUsed == 1;
                bakeGlError("cull pre-pass");
                // The visible count is NOT read back here. This used to be a
                // glGetBufferSubData per renderer per cull run - with animating
                // machines that is a full CPU-GPU pipeline sync PER RENDERER PER
                // FRAME (40+ syncs/frame at 1000 machines: GPU 100% sawtooth, render
                // thread pinned in native wait, OS kill). The count is only consumed
                // by flushPool (indirect-command indexCount) which now resolves all
                // dirty caches in ONE batched readback per pool flush; lastVisibleCount
                // keeps the previous run's value until then (F3 stats skew by one
                // phase at most).
                mc.countsDirty = true;
            } finally {
                if (GlDebugMarkers.ENABLED) {
                    GlDebugMarkers.popGroup();
                }
            }
        }

        if (recordsChanged) {
            if (GlDebugMarkers.ENABLED) {
                GlDebugMarkers.pushGroup(dirtyMode ? "HBM_Expand_DirtySpan_" + baked.debugTag() : "HBM_Expand_FullIdentity_" + baked.debugTag());
            }
            try {
                if (!runExpansion(baked, mc, count, mode, dirtyMode, dirtyCount, relayoutAllowed)) {
                    // Mesh lacks the attributes this mode needs (old dispatchBake contract):
                    // skip the entry for this frame.
                    return false;
                }
                bakeGlError("expansion");
                // Snapshot the freshly baked records. An exact raw copy is sufficient:
                // diffRecords canonicalizes (-0.0f -> +0.0f, plus shadow pos/quat mantissa
                // quantization) on BOTH sides of every comparison, so the stored bytes
                // never need to be quantized themselves.
                if (mc.recordCopy == null || mc.recordCopy.length < floats) {
                    // CPU heap snapshot: deliberately NOT cacheBytes-accounted (that
                    // counter is VRAM-only, see allocateCache).
                    mc.recordCopy = new float[floats];
                }
                if (dirtyMode) {
                    // Refresh only the dirty records' snapshot words.
                    int stride = InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
                    int pos = records.position();
                    for (int i = 0; i < dirtyCount; i++) {
                        int off = mc.dirtyIdx[i] * stride;
                        records.position(pos + off);
                        records.get(mc.recordCopy, off, stride);
                    }
                    records.position(pos);
                } else {
                    int pos = records.position();
                    records.get(mc.recordCopy, 0, floats);
                    records.position(pos);
                }
                mc.recordCount = count;
                mc.boneKey = boneKey(baked);
                mc.outputValid = true;
            } finally {
                if (GlDebugMarkers.ENABLED) {
                    GlDebugMarkers.popGroup();
                }
            }
        }
        // F3 bake-cull diagnostics: last-baked total and visible-after-cull of
        // this pass (frustum-only or hi-z cull; full count when the cull pre-pass
        // is off), plus the occlusion-culled count when the pyramid was sampled.
        recordBakeStats(mode, count,
                cull && mc.cullValid ? mc.lastVisibleCount : count,
                cull && mc.cullValid ? mc.lastOccludedCount : 0L,
                cull && mc.cullValid && mc.lastCullHiZ);
        recordExpansionStats(mode, dirtyMode, dirtyCount);
        return true;
    }

    /** Bone epoch folded into the cache key: 0 for meshes without a bone-id attribute. */
    private static long boneKey(BakedMesh baked) {
        int offBoneId = meshBoneOffset(baked);
        return offBoneId >= 0 ? boneEpoch : 0L;
    }

    /**
     * Whether this cache owns real storage: a range in the pass pool.
     */
    private static boolean cacheAllocated(ModeCache mc) {
        return mc.outInstances > 0;
    }

    /**
     * Admission backoff (see {@link #ADMISSION_BACKOFF_FRAMES}): true while this
     * cache is inside the window after its last budget refusal.
     */
    private static boolean admissionBackoffActive(ModeCache mc) {
        long refused = mc.admissionRefusedFrame;
        return refused >= 0L && frameStamp() - refused < ADMISSION_BACKOFF_FRAMES;
    }

    /**
     * Stamps a budget refusal, but only when no backoff window is already
     * active: the window always expires ADMISSION_BACKOFF_FRAMES after the
     * FIRST refusal of a streak, so one admission retry happens per window.
     */
    private static void stampAdmissionRefusal(ModeCache mc) {
        if (!admissionBackoffActive(mc)) {
            mc.admissionRefusedFrame = frameStamp();
        }
    }

    /**
     * True when a pool relayout already ran for this pass this frame (the
     * 2026-10-07 shadow-fix deferral): further growth relayouts from OTHER
     * entries of the same walk are refused for the frame. bakeEntry snapshots
     * this ONCE at its start (before its own relayouts stamp the pool), so the
     * back-to-back layout runs of ONE bake (allocateCache + expandPoolEbo)
     * count as the single relayout of the frame. Callers also use the snapshot
     * to suppress the admission-backoff stamp - a deferral refusal is a
     * ONE-frame soft skip (the growth succeeds next frame, before entries are
     * pending), not a budget refusal, and must not arm the entry's
     * ADMISSION_BACKOFF_FRAMES window.
     */
    private static boolean poolRelayoutDeferred(int mode) {
        return pool(mode).lastRelayoutFrame == frameStamp();
    }

    private static int meshBoneOffset(BakedMesh baked) {
        int offBoneId = baked.mesh.getMeshAttribByteOffsetOr("bone_id",
                baked.mesh.getMeshAttribByteOffsetOr("boneId", -1));
        if (offBoneId < 0 && baked.mesh.getMeshStrideBytes() == 36) {
            offBoneId = 32;
        }
        return offBoneId;
    }

    /**
     * Canonicalizes -0.0f to +0.0f: a sign-of-zero flip must not dirty a record
     * (poseStack decompositions and light interpolation legitimately produce 0.0
     * and -0.0 for the same quantity). Applied on BOTH sides of every comparison
     * (live words and the cached snapshot), so the snapshot can stay an exact raw
     * copy of what was uploaded.
     */
    private static float canonicalize(float v) {
        return Float.floatToRawIntBits(v) == 0x80000000 ? 0.0f : v;
    }

    /**
     * SHADOW comparison quantization: raw-bits mask of the LOW 3 MANTISSA BITS
     * (rawBits &amp; ~0x7), applied to the pos words (0..2) and the packed quat words (3..4)
     * ONLY, on BOTH sides of the comparison. Chosen because shadow records are
     * rebuilt every frame by IrisShadowBatchCollector.appendRecord from a poseStack
     * decomposition + getNormalizedRotation and WILL ulp-jitter in the low 1-2
     * mantissa bits (a guarantee, not a risk): masking 3 bits swallows that jitter
     * with a 1-bit margin, at a worst-case absolute error of 4 ulp (~2^-20
     * relative — far below anything visible in a shadow map; light/uv/tint words
     * keep the exact compare). COMPARISON-ONLY: the bytes uploaded to recordSsbo
     * and expanded by the compute are never modified. An exponent-boundary carry
     * (e.g. 0x3FF -> 0x401 across the mantissa wrap) can still dirty one record
     * once — harmless now that a dirty record costs one per-record expansion.
     */
    private static int quantizeCmpBits(int rawBits) {
        return rawBits & ~0x7;
    }

    /**
     * Last record word covered by the shadow quantization: pos 0..2 + the packed
     * snorm16 quat words 3..4 (the quat moved from words 3..6 into 2 packed words
     * with the compressed record - the quantization domain shifted with it).
     */
    private static final int SHADOW_QUANT_LAST_WORD = 4;

    /**
     * Per-record diff of the live records against the cached snapshot, filling
     * {@code mc.dirtyIdx[0..dirtyCount)} with the changed instance indices.
     * Returns the dirty count. MAIN records: exact raw-bits equality after -0.0f
     * canonicalization on both sides. SHADOW records: pos/quat words additionally
     * quantized on both sides (see {@link #quantizeCmpBits}); all other words
     * exact. Bitwise-equal words short-circuit before any canonicalization.
     */
    private static int diffRecords(ModeCache mc, FloatBuffer records, int count, int mode) {
        int stride = InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        int floats = count * stride;
        if (mc.recordCopy == null || mc.recordCopy.length < floats) {
            return count; // no usable snapshot: full re-bake
        }
        if (SCRATCH_CMP.length < floats) {
            SCRATCH_CMP = new float[floats];
        }
        if (mc.dirtyIdx == null || mc.dirtyIdx.length < count) {
            // Defensive growth (the allocation sized this for the admission count);
            // CPU heap scratch - deliberately NOT cacheBytes-accounted (VRAM-only
            // counter, see allocateCache).
            mc.dirtyIdx = new int[count];
        }
        int pos = records.position();
        records.get(SCRATCH_CMP, 0, floats);
        records.position(pos);
        boolean quant = mode == MODE_SHADOW;
        float[] snap = mc.recordCopy;
        int[] dirty = mc.dirtyIdx;
        int n = 0;
        int o = 0;
        for (int r = 0; r < count; r++, o += stride) {
            for (int w = 0; w < stride; w++) {
                float a = SCRATCH_CMP[o + w];
                float b = snap[o + w];
                if (Float.floatToRawIntBits(a) == Float.floatToRawIntBits(b)) {
                    // NaN poison guard: raw-bits equality can hold across two
                    // NaN-poisoned words forever. Any non-finite tint/grad word
                    // forces the record dirty so a poisoned record can never be
                    // cached instead of re-uploaded.
                    if (Float.isFinite(a)) {
                        continue; // bitwise equal: also equal under canonicalization
                    }
                    dirty[n++] = r;
                    break;
                }
                int ia = Float.floatToRawIntBits(canonicalize(a));
                int ib = Float.floatToRawIntBits(canonicalize(b));
                if (quant && w <= SHADOW_QUANT_LAST_WORD) {
                    ia = quantizeCmpBits(ia);
                    ib = quantizeCmpBits(ib);
                }
                if (ia != ib) {
                    dirty[n++] = r;
                    break;
                }
            }
        }
        mc.dirtyCount = n;
        return n;
    }

    /** Scratch for record comparison — grown lazily. */
    private static float[] SCRATCH_CMP = new float[InstancedStaticPartRenderer.INSTANCE_DATA_SIZE * 4096];

    private static boolean runExpansion(BakedMesh baked, ModeCache mc, int count, int mode,
                                        boolean dirtyMode, int dirtyCount, boolean relayoutAllowed) {
        // Capacity was ensured by bakeEntry BEFORE the cull (a realloc there would
        // invalidate output/EBO state the cull below is about to build).
        ensureBoneBuffer();
        if (!cullEnabled()) {
            // No cull: CPU-expanded pool EBO range, full dispatch, full draw.
            if (!expandPoolEbo(baked, mc, count, mode, relayoutAllowed)) {
                return false;
            }
        }
        if (dirtyMode) {
            // Span-level re-expansion: refresh the dirty-index list for THIS bake
            // (word 0 = count, words 1.. = instance indices) right before the
            // dispatch that reads it. Full re-bakes skip this entirely and run
            // identity mode, so they can never read stale list contents.
            ensureDirtyBuffer(mc, count);
            uploadDirtyList(mc, dirtyCount);
            bakeGlError("upload dirty list");
        }
        // Instance-major expansion covers the selected instances (frustum-
        // independent output; non-dirty slots keep their valid previous data);
        // the cull trims only the draw via the visible-list EBO.
        return runExpansionProgram(baked, mc.recordSsbo,
                pool(mode).outputSsbo, count, mode,
                dirtyMode, mc.dirtySsbo, dirtyCount, baked.vertBase[mode]);
    }

    private static boolean runExpansionProgram(BakedMesh baked, int recordSsbo, int outputTargetSsbo,
                                            int count, int mode, boolean dirtyListMode,
                                            int dirtySsbo, int dirtyCount, int vertBase) {
        // All input byte offsets must resolve: ldWord with -1 would cause out-of-bounds reads.
        int offPos = baked.mesh.getMeshOffset("position", -1);
        int offColor = baked.mesh.getMeshOffset("color", -1);
        int offNormal = baked.mesh.getMeshOffset("normal", -1);
        int offUv0 = baked.mesh.getMeshOffset("uv", 0);
        int offUv1 = baked.mesh.getMeshOffset("uv", 1);
        int offUv2 = baked.mesh.getMeshOffset("uv", 2);
        int offMidTex = baked.mesh.getMeshAttribByteOffsetOr("mc_midTexCoord",
                baked.mesh.getMeshOffset("genericFloat2", -1));
        int offTangent = baked.mesh.getMeshAttribByteOffsetOr("at_tangent",
                baked.mesh.getMeshOffset("genericByte4", -1));
        if (mode == MODE_SHADOW) {
            // Shadow passes write only depth/shadowcolor; position and UV0 are strictly required.
            if (offPos < 0 || offUv0 < 0) {
                return false;
            }
        } else {
            if ((offPos | offColor | offNormal | offUv0 | offUv1 | offUv2 | offMidTex | offTangent) < 0) {
                return false;
            }
        }

        int strideBytes = baked.inputSsbo != -1 ? baked.inputStride : baked.mesh.getMeshStrideBytes();
        int inputSsbo = baked.inputSsbo != -1 ? baked.inputSsbo : baked.mesh.getMeshVboId();

        GlStateManager._glUseProgram(programId);
        UBO_INT_VIEW.clear();
        UBO_INT_VIEW.put(strideBytes);
        UBO_INT_VIEW.put(InstancedStaticPartRenderer.INSTANCE_DATA_SIZE);
        UBO_INT_VIEW.put(outStrideBytes(mode) / 4);
        UBO_INT_VIEW.put(baked.vertCount);
        UBO_INT_VIEW.put(count);
        UBO_INT_VIEW.put(offPos);
        UBO_INT_VIEW.put(offColor);
        UBO_INT_VIEW.put(offNormal);
        UBO_INT_VIEW.put(offUv0);
        UBO_INT_VIEW.put(offUv1);
        UBO_INT_VIEW.put(offUv2);
        UBO_INT_VIEW.put(offMidTex);
        UBO_INT_VIEW.put(offTangent);
        UBO_INT_VIEW.put(mode);
        UBO_INT_VIEW.put(InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET);
        UBO_INT_VIEW.put(0); // uTransformMode
        UBO_INT_VIEW.put(hasBoneMatrices ? 1 : 0);
        UBO_INT_VIEW.put(meshBoneOffset(baked));
        UBO_INT_VIEW.put((int) ((baked.vertCount * (long) strideBytes + 3) / 4));
        // uDirtyMode: 1 = y indexes the dirty-list SSBO (span re-expansion),
        // 0 = identity (y is the instance index; the list is never read).
        UBO_INT_VIEW.put(dirtyListMode ? 1 : 0);
        // uVertBase: global-batch placement of this mesh's output range - the
        // expansion writes at vertBase + inst*vertCount + lv. The value comes
        // from the caller (baked.vertBase[mode]).
        UBO_INT_VIEW.put(vertBase);
        UBO_INT_VIEW.flip();

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bakeParamsUbo);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0L, UBO_BYTE_BUF);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, UBO_BINDING_BAKE_PARAMS, bakeParamsUbo);

        // WAR synchronization with previous draw call: ensures shader storage and vertex attribute
        // arrays are synchronized before compute write. BUFFER_UPDATE covers record uploads
        // (glCopyBufferSubData via staging / glBufferSubData fallback) landing in SSBO 1.
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT
                | GL43.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
                | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, inputSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, recordSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, outputTargetSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, boneSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 4, dirtyListMode ? dirtySsbo : 0);
        int numVertGroups = (baked.vertCount + LOCAL_SIZE - 1) / LOCAL_SIZE;
        // Dirty-list mode dispatches ONE WORKGROUP ROW PER DIRTY INSTANCE (the
        // shader maps y through the dirty list); identity mode covers all of them.
        GL43.glDispatchCompute(numVertGroups, dirtyListMode ? dirtyCount : count, 1);
        // Expansion is the LAST GPU op of a bake (bakeEntry runs the cull first),
        // so this unbind set is pipeline-exit hygiene: the pooled flush reaches
        // a foreign pack-program draw right after it (see also runCull's
        // trailing set).
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 4, 0);
        GL42.glMemoryBarrier(GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        return true;
    }

    private static int outStrideBytes(int mode) {
        return mode == MODE_SHADOW ? SHADOW_STRIDE_BYTES : OUT_STRIDE_BYTES;
    }

    // ── Cull pre-pass (compute, instance granularity) ──────────────────

    private static int runCull(BakedMesh baked, ModeCache mc, int count, float[] planes,
                               Matrix4f worldToClip, int mode) {
        if (!ensureCtlBuffer(mc, count)) {
            // Budget refused the ctl growth (fail closed): no valid ctl store
            // for this count - skip the whole cull; the caller drops the entry
            // for this frame (return -1 never collides with hiZ modes 0/1).
            mc.cullValid = false;
            mc.countsDirty = false;
            mc.stagingSlot = -1;
            mc.frustumStamp = Long.MIN_VALUE;
            return -1;
        }
        // Reset the visible counter only; the indirect words are rewritten by the assembler.
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.ctlSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, CTL_ZERO_COUNT);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        // Capture the PHYSICAL unit-10 binding BEFORE ensureBakeHiZ:
        // HiZDepthPyramid.regenerate's finally binds texture 0 on unit 10, so a
        // capture taken after the call would read 0 instead of the pack's texture.
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE10);
        int prevUnit10Texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);

        // Optional Hi-Z phase: build the pyramid BEFORE the cull program is bound
        // (regenerate switches programs itself) - at most once per frame per pass,
        // before the pass's first cull dispatch. 0 = frustum-only (current behavior).
        int hiZMode = ensureBakeHiZ(mode);

        GlStateManager._glUseProgram(cullProgramId);

        // Bind the pyramid to sampler unit 10 (same convention as NucleusGpuCuller)
        // through GlStateManager (cache-coherent), restoring the captured binding.
        boolean hiZBound = false;
        if (hiZMode == 1) {
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE10);
            com.mojang.blaze3d.platform.GlStateManager._bindTexture(HiZDepthPyramid.get().getPyramidTextureId());
            hiZBound = true;
        }
        GL20.glUniform1i(cuHiZMode, hiZMode);
        if (hiZMode == 1) {
            if (cuClipMat >= 0) {
                worldToClip.get(CLIP_MAT_FLOATS);
                GL20.glUniformMatrix4fv(cuClipMat, false, CLIP_MAT_FLOATS);
            }
            if (cuViewSize >= 0) {
                GL20.glUniform2f(cuViewSize, (float) hiZViewWidth, (float) hiZViewHeight);
            }
            if (cuPyramidLevels >= 0) {
                GL20.glUniform1i(cuPyramidLevels, HiZDepthPyramid.get().getMipLevels());
            }
        }

        GL20.glUniform1i(cuPass, 0);
        // The EBO identity fill (expandPoolEbo) shares this program and
        // leaves uIdentity at 1 - reset it so pass 1 reads the visible list.
        GL20.glUniform1i(cuIdentity, 0);
        GL20.glUniform1i(cuInstCount, count);
        GL20.glUniform1i(cuVertCount, baked.vertCount);
        GL20.glUniform1i(cuIdxCount, baked.idxCount);
        GL20.glUniform1i(cuInstStride, InstancedStaticPartRenderer.INSTANCE_DATA_SIZE);
        GL20.glUniform4fv(cuPlanes, planes);
        // Global-batch placement: EBO entries land at the mesh's pool range with
        // absolute indices shifted by vertBase.
        GL20.glUniform1i(cuVertBase, baked.vertBase[baked.modeOf(mc)]);
        GL20.glUniform1i(cuEboBase, baked.eboWordBase[baked.modeOf(mc)]);
        // BUFFER_UPDATE: the records were just written by glCopyBufferSubData (staging)
        // or glBufferSubData (fallback) - make those writes visible to SSBO reads.
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, mc.recordSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, mc.ctlSsbo);
        GL43.glDispatchCompute((count + LOCAL_SIZE - 1) / LOCAL_SIZE, 1, 1);
        // Binding 0 is unbound here, not after the assembler: a frustum-only
        // re-cull (records unchanged) ends the bake after this run, and this is
        // the last guard before foreign pack-program draws see SSBO state.
        // Binding 1 stays resident - the assembler rebinds the same buffer below
        // with no GPU work in between (P5 trim; its trailing unbind set owns it).
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
        if (hiZBound) {
            // The pyramid is only sampled by pass 0: restore the pack's unit-10
            // binding before the assembler/EBO passes run.
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE10);
            com.mojang.blaze3d.platform.GlStateManager._bindTexture(prevUnit10Texture);
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            hiZBound = false;
        }

        // Assemble: visible count -> indirect dispatch/draw words (single invocation).
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
        GL20.glUniform1i(cuPass, 2);
        // GPU-count flush path (GL 4.6): pass 2 also writes this entry's indirect
        // command into the pool command store, slot = this entry's pending index
        // (the append follows the cull in the same walk step; an entry that fails
        // later leaves its slot beyond drawcount or has it overwritten by the next
        // entry's run). Capable hardware only - the CPU readback + compaction
        // flush never consumes these slots, and the stamp pair recorded here lets
        // the flush prove the slot is current before drawing it.
        int cmdSlot = -1;
        if (mdicEnabled()) {
            java.util.ArrayList<PendingDraw> pendingList =
                    mode == MODE_SHADOW ? PENDING_SHADOW : PENDING_MAIN;
            PassPool cmdPool = pool(mode);
            int cmdBuf = ensurePoolCmdBuffer(cmdPool, pendingList.size() + 1);
            if (cmdBuf != -1) {
                cmdSlot = pendingList.size();
                mc.cmdGenStamp = cmdPool.cmdGen;
                mc.cmdSlotWritten = cmdSlot;
                // Own the slot explicitly: a stamp pair alone can't tell that
                // this slot's content was rewritten by a DIFFERENT cache in an
                // earlier frame (same generation, same pending index).
                cmdPool.cmdSlotOwners[cmdSlot] = mc;
                GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, CULL_CMD_BINDING, cmdBuf);
            } else {
                // Allocation failed: no slot was written this run, so kill any
                // stamp pair from an earlier run - a stale pair could still match
                // the current generation + pending index and pair an OLD
                // GPU-written indexCount with THIS run's fresh EBO.
                mc.cmdGenStamp = -1;
                mc.cmdSlotWritten = -1;
            }
        }
        GL20.glUniform1i(cuCmdSlot, cmdSlot);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, mc.ctlSsbo);
        GL43.glDispatchCompute(1, 1, 1);
        // Binding 1 stays resident: the EBO-build pass rebinds the same buffer
        // below with no GPU work in between (P5 trim; its trailing unbind set
        // owns it).
        if (cmdSlot != -1) {
            // CULL_CMD_BINDING (slot 4) is unbound here: pass 1 never touches
            // slot 4, and a frustum-only re-cull can end the bake right here -
            // the pool command store must not stay visible to foreign
            // pack-program draws. (The expansion shader rebinds slot 4 itself,
            // but only on paths where an expansion dispatch follows.)
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, CULL_CMD_BINDING, 0);
        }

        // EBO build: one uint per drawn index, keyed off the visible list; dispatched
        // indirectly with the workgroup count assembled above (ctl word 7).
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_COMMAND_BARRIER_BIT);
        GL20.glUniform1i(cuPass, 1);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, mc.ctlSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, baked.baseIdxSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3,
                pool(baked.modeOf(mc)).eboSsbo);
        GL43.glBindBuffer(GL43.GL_DISPATCH_INDIRECT_BUFFER, mc.ctlSsbo);
        GL43.glDispatchComputeIndirect(EBO_DISPATCH_OFFSET);
        // Pipeline-exit hygiene (deliberate, do not batch away): every path out of
        // bakeEntry can reach a foreign pack-program draw from here (the pooled
        // flush), and pack GLSL is arbitrary -
        // the bake leaves no SSBO bindings resident across it.
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, 0);
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_ELEMENT_ARRAY_BARRIER_BIT
                | GL43.GL_COMMAND_BARRIER_BIT);
        return hiZMode;
    }

    /**
     * Pool command store for the GPU-count flush path: one
     * {@code DrawElementsIndirectCommand} per pending-entry slot, written by
     * cull pass 2. Growth (2x, 64-slot floor) re-specifies the store - an
     * orphaning glBufferData destroys every previously written slot, so the
     * generation bump marks them stale and the next flush takes the CPU path
     * once while the affected caches re-cull (frustumStamp reset by the flush
     * scan; an instance-count dispatch, no re-expansion) and rewrite their
     * slots at the new generation.
     *
     * @return the buffer name, or -1 on a GL failure (the flush falls back)
     */
    private static int ensurePoolCmdBuffer(PassPool pool, int minSlots) {
        try {
            if (pool.cmdSsbo == -1) {
                pool.cmdSsbo = GL15.glGenBuffers();
                GlDebugMarkers.labelBuffer(pool.cmdSsbo, "HBM_PoolCmd_" + poolName(pool) + "_SSBO");
            }
            if (minSlots > pool.cmdSlots) {
                long newSlots = Math.max(minSlots, Math.max(pool.cmdSlots * 2, 64));
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, pool.cmdSsbo);
                GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER,
                        newSlots * INDIRECT_CMD_BYTES, GL15.GL_DYNAMIC_DRAW);
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
                pool.cmdSlots = newSlots;
                pool.cmdGen++;
                // Store re-specified: every previously written slot is orphaned,
                // so drop the owner table alongside the generation bump.
                pool.cmdSlotOwners = new ModeCache[(int) newSlots];
            }
            return pool.cmdSsbo;
        } catch (Exception t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: pool command store allocation failed - "
                            + "flush falls back to the CPU count path this frame", t);
            return -1;
        }
    }

    /** Header reset before each cull run: visible counter (word 0), reserved words
     * 1..3 and 12..15 (word 15 = optional culled-by-occlusion counter). The indirect
     * words 4..11 are rewritten by the assembler; words 16+ hold the visible list. */
    private static final int[] CTL_ZERO_COUNT = new int[CTL_HEADER_WORDS];

    // ── Record upload (persistent staging with fallback) ───────────────

    /**
     * Grows a per-cache record SSBO with budget accounting (delta against the old
     * size). Growth is budget-gATED like admission now: count growth used to
     * inflate cacheBytes past the budget with no way back (409 MB against 192 MB).
     *
     * @return false when the growth was refused (the entry falls back to the
     *         Tier-2/3 path this frame; the cache stays at its old
     *         capacity and remains valid for smaller counts).
     */
    private static boolean ensureCacheRecords(ModeCache mc, long bytes) {
        if (bytes > mc.recordSize[0]) {
            long newBytes = Math.max(bytes, Math.max(mc.recordSize[0], 0) * 2);
            long delta = newBytes - Math.max(mc.recordSize[0], 0);
            synchronized (NucleusGpuBaker.class) {
                if (cacheBytes + delta > cacheBudgetBytes()) {
                    // Same anti-livelock as ensureOutputCapacity: evict LRU and
                    // retry once before refusing, so an already-allocated cache
                    // can free budget instead of being pinned at full capacity.
                    evictStaleCaches();
                    if (cacheBytes + delta > cacheBudgetBytes()) {
                        return false;
                    }
                }
                cacheBytes += delta;
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.recordSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            mc.recordSize[0] = newBytes;
        }
        return true;
    }

    /**
     * Uploads {@code count} instance records via the persistent staging ring
     * (glBufferSubData fallback when the ring is exhausted or unavailable).
     * The destination storage must be pre-sized (ensureCacheRecords).
     */
    private static boolean uploadRecords(FloatBuffer records, int count, int destSsbo) {
        int floats = count * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        if (records == null || records.remaining() < floats) {
            return false;
        }
        PersistentUploadStaging staging = PersistentUploadStaging.getOrCreate();
        if (staging != null && staging.copyRegion(records, records.position(), floats, destSsbo, 0L)) {
            return true;
        }
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, destSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, records);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        // No synchronous glGetError verification here: it polled every upload in
        // a healthy state (see allowGlErrorDiag for the same cost argument), and
        // the PopDebugGroup/GLDebug callback already reports GL errors with the
        // stage context when debug logging is enabled.
        return true;
    }

    /**
     * Span-level record upload: only the dirty instance records reach the GPU, as
     * ONE span per contiguous dirty run (records are 32-float aligned, so a run of
     * {@code n} instances is {@code n * 32} floats at byte offset
     * {@code startIndex * 32 * 4}). Consecutive dirty indices merge into a run;
     * isolated dirty records (the common case) produce one tiny span each.
     * Destination storage must be pre-sized (ensureCacheRecords).
     */
    private static boolean uploadDirtyRecords(FloatBuffer records, int count, ModeCache mc,
                                              int dirtyCount) {
        int stride = InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        if (records == null || records.remaining() < count * stride
                || mc.dirtyIdx == null || mc.dirtyIdx.length < dirtyCount) {
            return false;
        }
        // Invariant: every dirty index must be inside [0, count). diffRecords fills
        // them bounded by ITS count, so an out-of-range value means diff/cache state
        // drift (observed 2026-10-05: runStart == count+1 crashed the fallback loop
        // with newPosition > limit and the broad catch then killed Tier 1 for the
        // whole session). Bail out to the caller's fallback instead of crashing, and
        // dump the cache state once per cache so the drift source is identifiable.
        for (int i = 0; i < dirtyCount; i++) {
            int idx = mc.dirtyIdx[i];
            if (idx < 0 || idx >= count) {
                if (DIRTY_RANGE_DUMP.add(System.identityHashCode(mc))) {
                    com.hbm_m.main.MainRegistry.LOGGER.error(
                            "[HBM-M] NucleusGpuBaker: out-of-range dirty index (modeCache@{}): dirty[{}]={}, "
                                    + "count={}, recordCount={}, stride={}, buffer pos={} lim={} cap={}"
                                    + " - invalidating the diff snapshot (full re-bake next call), bake stays enabled",
                            System.identityHashCode(mc), i, idx, count, mc.recordCount, stride,
                            records.position(), records.limit(), records.capacity());
                }
                // Self-heal: drop the snapshot so the next call takes the FULL path
                // (fresh diff, fresh dirtyIdx) instead of re-hitting the drift forever.
                mc.recordCopy = null;
                mc.recordCount = -1;
                mc.outputValid = false;
                return false;
            }
        }
        PersistentUploadStaging staging = PersistentUploadStaging.getOrCreate();
        int base = records.position();
        int firstUnstaged = 0;
        if (staging != null) {
            firstUnstaged = dirtyCount; // optimistic: all runs staged unless one fails
            for (int i = 0; i < dirtyCount; ) {
                int runStart = mc.dirtyIdx[i];
                int j = i;
                while (j + 1 < dirtyCount && mc.dirtyIdx[j + 1] == mc.dirtyIdx[j] + 1) {
                    j++;
                }
                int runEnd = mc.dirtyIdx[j]; // inclusive
                if (!staging.copyRegion(records, base + runStart * stride,
                        (runEnd - runStart + 1) * stride, mc.recordSsbo,
                        (long) runStart * stride * 4L)) {
                    firstUnstaged = i; // this run and everything after: subdata fallback
                    break;
                }
                i = j + 1;
            }
        }
        if (firstUnstaged < dirtyCount) {
            // glBufferSubData fallback for the runs the staging ring could not take
            // (runs before firstUnstaged already landed and are correct).
            int pos = records.position();
            int limit = records.limit();
            try {
                for (int i = firstUnstaged; i < dirtyCount; ) {
                    int runStart = mc.dirtyIdx[i];
                    int j = i;
                    while (j + 1 < dirtyCount && mc.dirtyIdx[j + 1] == mc.dirtyIdx[j] + 1) {
                        j++;
                    }
                    int runEnd = mc.dirtyIdx[j]; // inclusive
                    int from = base + runStart * stride;
                    // Reset the FULL window before each run: the previous run left
                    // position/limit inside its own span, and position(from) for a
                    // later run is validated against that stale limit - the observed
                    // crash is exactly "newPosition > limit: (138 > 46)" for dirty
                    // runs {0, 3} of a 4-record entry (run 0 set limit=46, run 3
                    // wanted position 138).
                    records.position(pos).limit(limit);
                    records.position(from).limit(from + (runEnd - runStart + 1) * stride);
                    GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.recordSsbo);
                    GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER,
                            (long) runStart * stride * 4L, records);
                    i = j + 1;
                }
            } finally {
                records.position(pos).limit(limit);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            }
        }
        return true;
    }

    /** Scratch for the dirty-list upload ([count, idx...]) — grown lazily. */
    private static java.nio.IntBuffer SCRATCH_DIRTY_LIST = null;

    /** Grows the per-cache dirty-index SSBO (instance capacity + count word, budget-accounted). */
    private static void ensureDirtyBuffer(ModeCache mc, int count) {
        long bytes = (long) (1 + count) * 4L;
        if (bytes > mc.dirtyBytes) {
            long newBytes = Math.max(bytes, Math.max(mc.dirtyBytes, 0) * 2);
            synchronized (NucleusGpuBaker.class) {
                cacheBytes += newBytes - Math.max(mc.dirtyBytes, 0);
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.dirtySsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            mc.dirtyBytes = newBytes;
        }
    }

    /** Uploads the CPU-side dirty list ([dirtyCount, instance indices...]) into the SSBO. */
    private static void uploadDirtyList(ModeCache mc, int dirtyCount) {
        int words = 1 + dirtyCount;
        if (SCRATCH_DIRTY_LIST == null || SCRATCH_DIRTY_LIST.capacity() < words) {
            SCRATCH_DIRTY_LIST = org.lwjgl.BufferUtils
                    .createIntBuffer(1 + Math.max(mc.dirtyIdx != null ? mc.dirtyIdx.length : 0, dirtyCount));
        }
        SCRATCH_DIRTY_LIST.clear();
        SCRATCH_DIRTY_LIST.put(dirtyCount);
        SCRATCH_DIRTY_LIST.put(mc.dirtyIdx, 0, dirtyCount);
        SCRATCH_DIRTY_LIST.flip();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.dirtySsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, SCRATCH_DIRTY_LIST);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    // ── Initialization & Resources ─────────────────────────────────────

    /**
     * Fail-loud injection of the instance record layout into an embedded compute
     * source: every {@code #define NAME <fallback>u} marker is replaced with the
     * InstancedStaticPartRenderer-owned value before compilation. Same mechanism as
     * NucleusGpuCuller.compileComputeShader — hardcoded GLSL literals drifted before
     * and corrupted the whole baked batch, so a missing marker must abort init.
     */
    private static String injectRecordLayout(String source, String shaderName, Object... nameValuePairs) {
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            String name = (String) nameValuePairs[i];
            String define = "#define " + name + " " + nameValuePairs[i + 1] + "u";
            if (!source.contains(define)) {
                if (!source.contains("#define " + name)) {
                    throw new IllegalStateException(shaderName + " is missing the " + name + " define marker");
                }
                source = source.replaceFirst("(?m)^#define " + name + " \\d+u$", define);
            }
        }
        return source;
    }

    private static boolean initProgram() {
        if (initFailed) {
            return false;
        }
        // Hoisted so the catch can free partially created GL objects: glCreateProgram
        // returns 0 on failure, so 0 marks "never created" here.
        int p = 0;
        int cs = 0;
        try {
            p = GL20.glCreateProgram();
            cs = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
            com.hbm_m.platform.RenderHooks.safeShaderSource(cs, injectRecordLayout(COMPUTE_SOURCE, "COMPUTE_SOURCE",
                    "INST_STRIDE", InstancedStaticPartRenderer.INSTANCE_DATA_SIZE,
                    "INST_POS_WORD", InstancedStaticPartRenderer.POS_FLOAT_OFFSET,
                    "INST_QUAT_WORD", InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET,
                    "INST_BMIN_WORD", InstancedStaticPartRenderer.BBOX_MIN_FLOAT_OFFSET,
                    "INST_BSIZE_WORD", InstancedStaticPartRenderer.BBOX_SIZE_FLOAT_OFFSET,
                    "INST_FADE_WORD", InstancedStaticPartRenderer.FADE_FLOAT_OFFSET,
                    "INST_UVRECT_WORD", InstancedStaticPartRenderer.UVRECT_FLOAT_OFFSET,
                    "INST_TINT_WORD", InstancedStaticPartRenderer.TINT_FLOAT_OFFSET,
                    "INST_GRAD_WORD", InstancedStaticPartRenderer.GRAD_FLOAT_OFFSET));
            GL20.glCompileShader(cs);
            if (GL20.glGetShaderi(cs, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("compute compile: " + GL20.glGetShaderInfoLog(cs, 4096));
            }
            GL20.glAttachShader(p, cs);
            GL20.glLinkProgram(p);
            GL20.glDeleteShader(cs);
            if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("compute link: " + GL20.glGetProgramInfoLog(p, 4096));
            }
            programId = p;
            GlDebugMarkers.labelProgram(programId, "HBM_BakeExpand_CS");
            bakeParamsUbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(bakeParamsUbo, "HBM_BakeParams_UBO");
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bakeParamsUbo);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, 88L, GL15.GL_DYNAMIC_DRAW);
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, UBO_BINDING_BAKE_PARAMS, bakeParamsUbo);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
            initCullProgram();
            if (!logged) {
                logged = true;
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] NucleusGpuBaker: Tier 1 ACTIVE (programId={}, cull={}, cacheBudget={} MB)",
                        programId, cullEnabled(), cacheBudgetBytes() >> 20);
            }
            return true;
        } catch (Exception t) {
            // Partial-init cleanup (GL 4.6 §7.1/§7.3; mirrors NucleusGpuCuller.
            // compileComputeShader): a compile failure throws before the explicit
            // shader delete, and a failed-link program stays live until
            // glDeleteProgram - a shader flagged while attached only completes its
            // deletion once detached, which the program delete below does. Skip the
            // program delete when programId already owns the name (assignment at
            // the top precedes the buffer setup that can still throw); it is
            // tracked and freed by releaseResourcesInternal instead.
            if (cs != 0) GL20.glDeleteShader(cs);
            if (p != 0 && p != programId) GL20.glDeleteProgram(p);
            initFailed = true;
            com.hbm_m.main.MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker init failed - Tier 1 disabled", t);
            return false;
        }
    }

    private static void initCullProgram() {
        if (cullInitFailed || !CULL_SWITCH) {
            return;
        }
        // Hoisted so the catch can free partially created GL objects (0 = never
        // created, glCreateProgram returns 0 on failure).
        int p = 0;
        int cs = 0;
        try {
            p = GL20.glCreateProgram();
            cs = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
            com.hbm_m.platform.RenderHooks.safeShaderSource(cs, injectRecordLayout(CULL_SOURCE, "CULL_SOURCE",
                    "INST_STRIDE", InstancedStaticPartRenderer.INSTANCE_DATA_SIZE,
                    "INST_POS_WORD", InstancedStaticPartRenderer.POS_FLOAT_OFFSET,
                    "INST_QUAT_WORD", InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET,
                    "INST_BMIN_WORD", InstancedStaticPartRenderer.BBOX_MIN_FLOAT_OFFSET,
                    "INST_BSIZE_WORD", InstancedStaticPartRenderer.BBOX_SIZE_FLOAT_OFFSET,
                    "INST_FADE_WORD", InstancedStaticPartRenderer.FADE_FLOAT_OFFSET));
            GL20.glCompileShader(cs);
            if (GL20.glGetShaderi(cs, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("cull compile: " + GL20.glGetShaderInfoLog(cs, 4096));
            }
            GL20.glAttachShader(p, cs);
            GL20.glLinkProgram(p);
            GL20.glDeleteShader(cs);
            if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("cull link: " + GL20.glGetProgramInfoLog(p, 4096));
            }
            cullProgramId = p;
            GlDebugMarkers.labelProgram(cullProgramId, "HBM_BakeCull_CS");
            cuPass = GL20.glGetUniformLocation(p, "uPass");
            cuIdentity = GL20.glGetUniformLocation(p, "uIdentity");
            cuInstCount = GL20.glGetUniformLocation(p, "uInstCount");
            cuVertCount = GL20.glGetUniformLocation(p, "uVertCount");
            cuIdxCount = GL20.glGetUniformLocation(p, "uIdxCount");
            cuInstStride = GL20.glGetUniformLocation(p, "uInstStride");
            cuPlanes = GL20.glGetUniformLocation(p, "uPlane");
            cuVertBase = GL20.glGetUniformLocation(p, "uVertBase");
            cuEboBase = GL20.glGetUniformLocation(p, "uEboBase");
            cuCmdSlot = GL20.glGetUniformLocation(p, "uCmdSlot");
            cuHiZMode = GL20.glGetUniformLocation(p, "uHiZMode");
            cuClipMat = GL20.glGetUniformLocation(p, "uClipMat");
            cuViewSize = GL20.glGetUniformLocation(p, "uViewSize");
            cuPyramidLevels = GL20.glGetUniformLocation(p, "uPyramidLevels");
        } catch (Exception t) {
            // Partial-init cleanup (GL 4.6 §7.1/§7.3, see initProgram). Unlike
            // initProgram this catch zeroes cullProgramId below, so the local
            // program becomes untracked in EVERY failure window - always delete
            // it, plus the shader (still unflagged on a compile failure, still
            // attached on a link failure).
            if (cs != 0) GL20.glDeleteShader(cs);
            if (p != 0) GL20.glDeleteProgram(p);
            cullInitFailed = true;
            cullProgramId = -1;
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker cull program failed - running without cull", t);
        }
    }

    private static final int MIN_EBO_INSTANCES = 1024;

    /**
     * True when {@code targetCapacity} instances of this mesh still fit int
     * bounds: {@code idxCount * targetCapacity} sizes the fill buffer / EBO
     * range and {@code vertCount * targetCapacity} bounds the largest written
     * vertex index. Same Integer.MAX_VALUE guard relayoutPool applies to its
     * summed ranges, here per expansion in long math - the int product used to
     * wrap negative and throw out of memAllocInt / Buffer.put, and the bake
     * catch-all then shut Tier 1 down for the whole session.
     */
    private static boolean eboExpansionFits(BakedMesh baked, int targetCapacity) {
        return (long) baked.idxCount * targetCapacity <= Integer.MAX_VALUE
                && (long) baked.vertCount * targetCapacity <= Integer.MAX_VALUE;
    }

    /**
     * GL_MAX_COMPUTE_WORK_GROUP_COUNT[0], latched once on first identity-fill
     * dispatch (conservative GL 4.3 spec minimum 65535 when the query fails).
     * The flat-1D fill dispatches ceil(idxCount*slotCount/64) groups along X;
     * exceeding the limit raises INVALID_VALUE and SKIPS the dispatch, so such
     * fills must route to the CPU loop BEFORE any capacity field latches - a
     * skipped dispatch over the just-orphaned store would otherwise be read by
     * every later draw (undefined contents, permanent for the session).
     */
    private static int maxComputeGroupsX = -1;
    private static boolean identityFillRefuseLogged = false;

    /**
     * True when {@code groups} X work-groups fit the driver's compute
     * work-group-count limit; latches the limit on first call and logs a
     * refusal once (the refusal is deterministic per mesh size - every frame
     * would spam otherwise).
     */
    private static boolean identityFillGroupsFit(long groups) {
        if (maxComputeGroupsX < 0) {
            int limit = 65535; // GL 4.3 spec minimum for MAX_COMPUTE_WORK_GROUP_COUNT[0]
            try {
                int queried = GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0);
                if (queried > 0) {
                    limit = queried;
                }
            } catch (Throwable ignored) {
                // keep the spec floor (query unavailable on a broken context)
            }
            maxComputeGroupsX = limit;
        }
        if (groups > maxComputeGroupsX) {
            if (!identityFillRefuseLogged) {
                identityFillRefuseLogged = true;
                com.hbm_m.main.MainRegistry.LOGGER.warn(
                        "[HBM-M] NucleusGpuBaker: identity EBO fill needs {} X work-groups, "
                                + "driver limit {} - oversized fills stay on the CPU path",
                        groups, maxComputeGroupsX);
            }
            return false;
        }
        return true;
    }

    /**
     * GPU identity fill of an EBO range (cull program, uPass = 1 with uIdentity
     * = 1): one flat dispatch writes {@code slotCount * idxCount} entries -
     * {@code vertBase + slot * vertCount + baseIdx[k]} for EVERY slot - byte for
     * byte what the CPU fill loops build, without pulling tens of MB through the
     * CPU per expansion. Flat 1D addressing because the 2D (x = index groups,
     * y = slots) cull layout cannot address slot counts past the Y work-group
     * limit (typically 65535) that the doubling capacities easily exceed.
     * Returns false (caller falls back to the CPU fill, which overwrites the
     * whole store BEFORE any capacity field latches) when the cull program is
     * not available, or when the X work-group count would exceed the driver's
     * {@link #maxComputeGroupsX} limit (both pre-checked - the dispatch itself
     * is no longer error-polled, see the barrier comment below). Callers
     * must guard the overflow with {@link #eboExpansionFits} first so the
     * dispatch fits int bounds. {@code eboSsbo} must be a live buffer name (&gt; 0).
     */
    private static boolean dispatchIdentityEboFill(BakedMesh baked, int eboSsbo,
                                                   int slotCount, int vertBase, int eboWordBase) {
        if (cullProgramId == -1) {
            return false;
        }
        long invocations = (long) baked.idxCount * slotCount; // int-bounded by eboExpansionFits
        long groups = (invocations + LOCAL_SIZE - 1) / LOCAL_SIZE;
        if (!identityFillGroupsFit(groups)) {
            return false;
        }
        GlStateManager._glUseProgram(cullProgramId);
        GL20.glUniform1i(cuPass, 1);
        GL20.glUniform1i(cuIdentity, 1);
        GL20.glUniform1i(cuInstCount, slotCount);
        GL20.glUniform1i(cuVertCount, baked.vertCount);
        GL20.glUniform1i(cuIdxCount, baked.idxCount);
        GL20.glUniform1i(cuVertBase, vertBase);
        GL20.glUniform1i(cuEboBase, eboWordBase);
        // BUFFER_UPDATE covers the orphaning glBufferData that sized the store
        // just before this dispatch (and the pool store allocation upstream).
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, baked.baseIdxSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, eboSsbo);
        GL43.glDispatchCompute((int) groups, 1, 1);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, 0);
        // Same barrier set as the cull EBO-build pass: draws consume the EBO
        // as element indices right after the bake.
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_ELEMENT_ARRAY_BARRIER_BIT);
        // No post-dispatch glGetError: the work-group count is pre-validated by
        // identityFillGroupsFit (the only deterministic INVALID_VALUE source),
        // and a synchronous poll here cost every expansion in a healthy state
        // (see allowGlErrorDiag). GLDebug reports any residual driver error.
        return true;
    }

    /**
     * P3: one-time u32-aligned relayout of the input mesh. Iris ENTITY stride is
     * 54 bytes on 1.21.1 (not a multiple of 4), so every ldWord split into two
     * buffer fetches. Padding each vertex row to a multiple of 4 makes every
     * attribute access single-fetch; strides that are already aligned (56 on
     * 1.20.1, 36 on bone meshes) bind the mesh VBO directly with no copy.
     */
    private static void buildAlignedInput(BakedMesh baked) {
        int stride = baked.mesh.getMeshStrideBytes();
        int aligned = (stride + 3) & ~3;
        baked.inputStride = aligned;
        if (aligned == stride) {
            baked.inputSsbo = -1;
            return;
        }
        java.nio.ByteBuffer raw = MemoryUtil.memAlloc(baked.vertCount * stride);
        java.nio.ByteBuffer out = MemoryUtil.memAlloc(baked.vertCount * aligned);
        try {
            int prev = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, baked.mesh.getMeshVboId());
            GL15.glGetBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, raw);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prev);
            for (int v = 0; v < baked.vertCount; v++) {
                raw.position(v * stride).limit(v * stride + stride);
                out.position(v * aligned).limit(v * aligned + aligned);
                out.put(raw);
            }
            out.clear();
            out.limit(baked.vertCount * aligned);
            baked.inputSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(baked.inputSsbo, "HBM_AlignedIn_" + baked.debugTag());
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, baked.inputSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, out, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        } catch (Exception t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: aligned input relayout failed - using raw mesh VBO", t);
            if (baked.inputSsbo != -1) {
                GL15.glDeleteBuffers(baked.inputSsbo);
                baked.inputSsbo = -1;
            }
            baked.inputStride = stride;
        } finally {
            MemoryUtil.memFree(raw);
            MemoryUtil.memFree(out);
        }
    }

    /**
     * Output VAO over a bake output buffer with its own fresh EBO (bound inside the VAO).
     * {@code slim} = shadow layout (Position loc0, UV0 loc2, Normal loc5);
     * full = the six standard attributes (locations 0..5, dense 56-byte layout).
     * Returns the VAO handle; the created EBO handle lands in
     * {@code createdEboOut[0]} ({@code -1} only if the create failed).
     * Exception-safe: a mid-create failure deletes the partial names before
     * rethrowing - until the return, neither name has a field reference, so no
     * caller rollback could otherwise reach them. The pre-call VAO binding is
     * saved on entry and restored on every exit (a mid-walk allocation must not
     * leave the caller's draws running against the wrong VAO).
     */
    private static int createOutputVao(int vbo, boolean slim, int[] createdEboOut) {
        int previousVao = com.hbm_m.client.render.GlVaoSafety.currentBinding();
        int vao = GL30.glGenVertexArrays();
        int ebo = -1;
        try {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            if (slim) {
                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, SHADOW_STRIDE_BYTES, SH_OFF_POS);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, SHADOW_STRIDE_BYTES, SH_OFF_UV0);
                GL20.glEnableVertexAttribArray(5);
                GL20.glVertexAttribPointer(5, 3, GL11.GL_BYTE, true, SHADOW_STRIDE_BYTES, SH_OFF_NORMAL);
            } else {
                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, OUT_STRIDE_BYTES, OUT_OFF_POS);
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, OUT_STRIDE_BYTES, OUT_OFF_COLOR);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, OUT_STRIDE_BYTES, OUT_OFF_UV0);
                // UV1/UV2: 16-bit short integers bound via glVertexAttribIPointer.
                GL20.glEnableVertexAttribArray(3);
                GL30.glVertexAttribIPointer(3, 2, GL11.GL_SHORT, OUT_STRIDE_BYTES, OUT_OFF_UV1);
                GL20.glEnableVertexAttribArray(4);
                GL30.glVertexAttribIPointer(4, 2, GL11.GL_SHORT, OUT_STRIDE_BYTES, OUT_OFF_UV2);
                GL20.glEnableVertexAttribArray(5);
                GL20.glVertexAttribPointer(5, 3, GL11.GL_BYTE, true, OUT_STRIDE_BYTES, OUT_OFF_NORMAL);
            }
            ebo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            createdEboOut[0] = ebo;
            return vao;
        } catch (Exception t) {
            // Mid-create failure (GL 4.6 §2.6.1.2: generated names stay used until
            // deleted): restore the caller's VAO binding, then delete the partial
            // names and rethrow into the caller's rollback.
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL30.glDeleteVertexArrays(vao);
            if (ebo != -1) GL15.glDeleteBuffers(ebo);
            throw t;
        }
    }

    /**
     * Binds Iris extended vertex attributes (mc_midTexCoord, at_tangent, at_midBlock)
     * on the pack program from the given output layout. iris_Entity is supplied as
     * constant 0 (multiblock machines are not entity-specific).
     * <p>
     * MUST run on every main-pass draw (including culled ones): the locations are
     * pack-program-dependent, so the pointers are captured per draw — and
     * glVertexAttribPointer snapshots whatever is bound to GL_ARRAY_BUFFER, so the
     * owning output SSBO is bound here first (stale global bindings would otherwise
     * corrupt the VAO's attribute state permanently).
     */
    // Extended-attribute location cache, keyed per (ShaderInstance, pipeline
    // generation) - same pattern as the matrix uniform cache below: Iris can
    // relink a program and reuse the same ShaderInstance, and the shared pass
    // VAO outlives ANY single pack program, so the enable state alone proves
    // nothing - the pointers must be (re)written unconditionally on every call,
    // with the locations themselves fetched only on a cache miss.
    private static ShaderInstance extAttribShader;
    private static long extAttribPipelineGen = -1L;
    private static int cachedEntLoc = -2, cachedMidTexLoc = -2,
            cachedTangentLoc = -2, cachedMidBlockLoc = -2;

    private static void bindIrisExtendedAttributes(ShaderInstance shader, int outputSsbo,
                                                   int strideBytes, int offMidTex, int offTangent,
                                                   int offMidBlock) {
        long gen = com.hbm_m.client.render.shader.IrisExtendedShaderAccess.getPipelineGeneration();
        if (extAttribShader != shader || extAttribPipelineGen != gen) {
            extAttribShader = shader;
            extAttribPipelineGen = gen;
            int program = shader.getId();
            cachedEntLoc = GL20.glGetAttribLocation(program, "iris_Entity");
            cachedMidTexLoc = GL20.glGetAttribLocation(program, "mc_midTexCoord");
            cachedTangentLoc = GL20.glGetAttribLocation(program, "at_tangent");
            cachedMidBlockLoc = GL20.glGetAttribLocation(program, "at_midBlock");
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, outputSsbo);
        if (cachedEntLoc >= 0) {
            // Supply constant 0 for entity ID since multiblock machines are not entity-specific.
            GL20.glDisableVertexAttribArray(cachedEntLoc);
            GL30.glVertexAttribI2i(cachedEntLoc, 0, 0);
        }
        // Always re-issue the pointer: the VAO is shared across pack programs,
        // so an already-enabled array may carry a FOREIGN program's offset/type.
        // glDisable on a disabled array is a no-op; GL_ARRAY_BUFFER points at
        // the output SSBO (bound above), so the pointers snapshot the right
        // buffer.
        if (cachedMidTexLoc >= 0) {
            GL20.glEnableVertexAttribArray(cachedMidTexLoc);
            GL20.glVertexAttribPointer(cachedMidTexLoc, 2, GL11.GL_FLOAT, false, strideBytes, offMidTex);
        }
        if (cachedTangentLoc >= 0) {
            GL20.glEnableVertexAttribArray(cachedTangentLoc);
            GL20.glVertexAttribPointer(cachedTangentLoc, 4, GL11.GL_BYTE, true, strideBytes, offTangent);
        }
        if (cachedMidBlockLoc >= 0) {
            GL20.glEnableVertexAttribArray(cachedMidBlockLoc);
            // 3 NON-normalized GLbytes - byte-for-byte how Iris binds the TERRAIN
            // format's MID_BLOCK element (BYTE x 3 GENERIC, normalized=false).
            // Packs read at_midBlock.xyz as raw signed bytes and divide by 64.0
            // themselves; a vec4 declaration sees w = 1.0 (GL default for a
            // 3-component pointer), same as native Iris terrain.
            GL20.glVertexAttribPointer(cachedMidBlockLoc, 3, GL11.GL_BYTE, false, strideBytes, offMidBlock);
        }
    }

    // ── Shadow slim-layout attribute rebinding (2026-10-15) ────────────
    // The shadow pool VAO hard-codes Position@0, UV0@2, Normal@5 (the
    // IrisVertexFormats.ENTITY ordinals). Iris sources (1.20.1): the pack shadow
    // program is an ExtendedShader (a ShaderInstance) whose attribute locations
    // come from vanilla Uniform.glBindAttribLocation at the format's ordinal
    // positions, with names redirected to "iris_"-prefixed variants
    // (MixinShaderInstance#iris$redirectBindAttributeLocation) - so 0/2/5 IS the
    // expected layout for the ENTITY-format shadow program. But the VAO outlives
    // any single pack program and Iris can re-link/reuse the ShaderInstance, so
    // the locations are resolved from the ACTIVE program before every shadow
    // multi-draw (glGetAttribLocation, cached per ShaderInstance + pipeline
    // generation like the matrix uniform cache) and the pointers are re-issued
    // at the resolved locations when they differ. All -1 (program uses none of
    // the slim attributes) leaves the VAO untouched - enabled-but-unread arrays
    // are harmless, so partial mismatches only rebind what the program reads.
    private static ShaderInstance slimAttribShader;
    private static long slimAttribPipelineGen = -1L;
    private static int slimLocPos = -2, slimLocUv = -2, slimLocNormal = -2;
    private static boolean slimAttribMismatchLogged = false;

    private static void bindSlimShadowAttributes(ShaderInstance shader, int outputSsbo) {
        if (shader == null || outputSsbo == -1) {
            return;
        }
        long gen = com.hbm_m.client.render.shader.IrisExtendedShaderAccess.getPipelineGeneration();
        if (slimAttribShader != shader || slimAttribPipelineGen != gen) {
            slimAttribShader = shader;
            slimAttribPipelineGen = gen;
            int program = shader.getId();
            // ExtendedShader programs carry iris_-prefixed attribute names
            // (MixinShaderInstance); FallbackShader/vanilla ones use plain names.
            slimLocPos = firstAttrib(program, "iris_Position", "Position");
            slimLocUv = firstAttrib(program, "iris_UV0", "UV0");
            slimLocNormal = firstAttrib(program, "iris_Normal", "Normal");
        }
        if (slimLocPos < 0 && slimLocUv < 0 && slimLocNormal < 0) {
            return; // program consumes none of the slim attributes - nothing to bind
        }
        if (slimLocPos == 0
                && (slimLocUv < 0 || slimLocUv == 2)
                && (slimLocNormal < 0 || slimLocNormal == 5)) {
            return; // matches the hard-coded VAO layout - pointers already correct
        }
        if (!slimAttribMismatchLogged) {
            slimAttribMismatchLogged = true;
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[HBM-M] NucleusGpuBaker: pack shadow program attribute locations "
                            + "(Position={}, UV0={}, Normal={}) differ from the shadow VAO layout "
                            + "(0, 2, 5) - rebinding slim shadow attributes per program",
                    slimLocPos, slimLocUv, slimLocNormal);
        }
        // Same discipline as bindIrisExtendedAttributes: the pointers snapshot
        // GL_ARRAY_BUFFER, so the owning output SSBO is bound first. The VAO is
        // already bound by the flush caller; the rebind lands in ITS state.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, outputSsbo);
        if (slimLocPos >= 0) {
            GL20.glEnableVertexAttribArray(slimLocPos);
            GL20.glVertexAttribPointer(slimLocPos, 3, GL11.GL_FLOAT, false,
                    SHADOW_STRIDE_BYTES, SH_OFF_POS);
        }
        if (slimLocUv >= 0) {
            GL20.glEnableVertexAttribArray(slimLocUv);
            GL20.glVertexAttribPointer(slimLocUv, 2, GL11.GL_FLOAT, false,
                    SHADOW_STRIDE_BYTES, SH_OFF_UV0);
        }
        if (slimLocNormal >= 0) {
            GL20.glEnableVertexAttribArray(slimLocNormal);
            GL20.glVertexAttribPointer(slimLocNormal, 3, GL11.GL_BYTE, true,
                    SHADOW_STRIDE_BYTES, SH_OFF_NORMAL);
        }
    }

    /** First resolvable vertex-attribute location among the given names (-1 if none). */
    private static int firstAttrib(int program, String... names) {
        for (String name : names) {
            int loc = GL20.glGetAttribLocation(program, name);
            if (loc >= 0) {
                return loc;
            }
        }
        return -1;
    }

    /**
     * Sets ModelViewMat / ProjMat on the active pack program using raw GL uniform locations.
     * Raw locations avoid overhead and type mismatch issues with Minecraft's Uniform wrappers.
     */
    private static ShaderInstance matrixShader;
    /** Pipeline generation folded into the location cache: Iris can rebuild a pipeline
     * and reuse the same ShaderInstance with a NEW GL program (stale locations otherwise). */
    private static long matrixPipelineGen = -1L;
    private static com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.Locations matrixLocs =
            com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.Locations.NONE;
    private static int projLoc = -2;

    /**
     * @return true when BOTH the model-view and projection uniforms were resolved and
     *         uploaded. False = the active program declares neither ModelViewMat nor
     *         iris_ModelViewMat, or neither ProjMat nor iris_ProjMat. Callers on the
     *         shadow path MUST treat false as a Tier-1 refusal (the shadow records are
     *         world+camera spaced - drawing with a missing model-view sends the geometry
     *         off-map with no GL error). Main-pass callers may ignore the result (their
     *         programs always declare the vanilla uniforms).
     */
    private static boolean setMatrices(ShaderInstance shader, Matrix4f proj, Matrix4f modelView) {
        long gen = com.hbm_m.client.render.shader.IrisExtendedShaderAccess.getPipelineGeneration();
        if (matrixShader != shader || matrixPipelineGen != gen) {
            matrixShader = shader;
            matrixPipelineGen = gen;
            matrixLocs = com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.resolve(shader);
            int program = shader.getId();
            projLoc = GL20.glGetUniformLocation(program, "iris_ProjMat");
            if (projLoc < 0) {
                projLoc = GL20.glGetUniformLocation(program, "ProjMat");
            }
        }
        modelView.get(MV_FLOATS);
        int locModelView = matrixLocs.modelView();
        if (locModelView >= 0) {
            GL20.glUniformMatrix4fv(locModelView, false, MV_FLOATS);
        }
        if (projLoc >= 0) {
            proj.get(PROJ_FLOATS);
            GL20.glUniformMatrix4fv(projLoc, false, PROJ_FLOATS);
        }
        // Derived matrices: required for shaderpacks (e.g. Photon) calculating world-space
        // normals via inverse model-view or normal matrices.
        int locInverse = matrixLocs.modelViewInverse();
        if (locInverse >= 0) {
            INVERSE_SCRATCH.set(modelView).invertAffine();
            INVERSE_SCRATCH.get(INV_FLOATS);
            GL20.glUniformMatrix4fv(locInverse, false, INV_FLOATS);
        }
        int locNormalMat = matrixLocs.normalMat();
        if (locNormalMat >= 0) {
            NORMAL_TMP.set(modelView);
            NORMAL_TMP.get(NORM_FLOATS);
            GL20.glUniformMatrix3fv(locNormalMat, false, NORM_FLOATS);
        }
        return locModelView >= 0 && projLoc >= 0;
    }

    private static final Matrix4f INVERSE_SCRATCH = new Matrix4f();
    private static final float[] MV_FLOATS = new float[16];
    private static final float[] PROJ_FLOATS = new float[16];
    private static final float[] INV_FLOATS = new float[16];
    private static final org.joml.Matrix3f NORMAL_TMP = new org.joml.Matrix3f();
    private static final float[] NORM_FLOATS = new float[9];

    /**
     * Gribb–Hartmann frustum plane extraction for a column-major clip matrix
     * (records-space → clip): left/right/bottom/top/near/far, each (n.xyz, d)
     * with "inside" on the positive side. Planes are unnormalized — the cull
     * shader's |n|·extent radius test is scale-invariant.
     */
    private static void extractFrustumPlanes(Matrix4f clip, float[] out24) {
        // JOML mXY = column X, row Y → matrix rows:
        // row0 = (m00, m10, m20, m30); row1 = (m01, m11, m21, m31);
        // row2 = (m02, m12, m22, m32); row3 = (m03, m13, m23, m33).
        float r00 = clip.m00(), r01 = clip.m10(), r02 = clip.m20(), r03 = clip.m30();
        float r10 = clip.m01(), r11 = clip.m11(), r12 = clip.m21(), r13 = clip.m31();
        float r20 = clip.m02(), r21 = clip.m12(), r22 = clip.m22(), r23 = clip.m32();
        float r30 = clip.m03(), r31 = clip.m13(), r32 = clip.m23(), r33 = clip.m33();
        setPlane(out24, 0,  r30 + r00, r31 + r01, r32 + r02, r33 + r03); // left
        setPlane(out24, 4,  r30 - r00, r31 - r01, r32 - r02, r33 - r03); // right
        setPlane(out24, 8,  r30 + r10, r31 + r11, r32 + r12, r33 + r13); // bottom
        setPlane(out24, 12, r30 - r10, r31 - r11, r32 - r12, r33 - r13); // top
        setPlane(out24, 16, r30 + r20, r31 + r21, r32 + r22, r33 + r23); // near
        setPlane(out24, 20, r30 - r20, r31 - r21, r32 - r22, r33 - r23); // far
    }

    private static void setPlane(float[] out, int idx, float a, float b, float c, float d) {
        out[idx] = a;
        out[idx + 1] = b;
        out[idx + 2] = c;
        out[idx + 3] = d;
    }

    /** Stable stamp of the 6 frustum planes (any camera motion changes it). */
    private static long stampPlanes(float[] planes) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < 24; i++) {
            h = (h ^ Float.floatToRawIntBits(planes[i])) * 0x100000001b3L;
        }
        return h;
    }

    // ── Cache allocation, budget, eviction ─────────────────────────────

    /**
     * Allocates the per-mode cache objects. Returns false (the caller refuses the
     * entry; the mesh renders via the Tier-2/3 fallback this frame) when the budget
     * cannot admit this entry even after LRU eviction. Output is sized for the
     * requested count and grows on demand.
     */
    private static boolean allocateCache(BakedMesh baked, ModeCache mc, int count, int mode) {
        long outBytes = (long) baked.vertCount * count * outStrideBytes(mode);
        long recBytes = (long) count * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE * 4L;
        long recStorage = Math.max(recBytes, 1L << 16); // matches mc.recordSize[0] below
        long ctlBytes = (long) (CTL_HEADER_WORDS + count) * 4L;
        // Span re-expansion state: dirty-index SSBO (count word + instance capacity)
        // and its CPU-side scratch. The SSBO is VRAM and budget-accounted; the CPU
        // scratch (dirtyIdx) and the recordCopy heap snapshot are NOT - cacheBytes
        // is a VRAM-only counter (matches allocatedGpuBytes coverage) and charging
        // heap bytes to it stole budget from real GL allocations, pushing meshes
        // onto the Tier-2/3 fallback for no VRAM reason.
        long dirtySsboBytes = (long) (1 + count) * 4L;
        // Global-batch path: the output+EBO live in the pass pool, accounted once
        // there. For admission, reserve the worst-case pool growth this range can
        // cause (full realloc); the estimate is rolled back right after allocation
        // and the pool accounts its ACTUAL delta in ensurePoolStorage.
        long eboBytes = (long) baked.idxCount * count * 4L;
        long poolNeed = outBytes + eboBytes;
        long need = recStorage + ctlBytes + dirtySsboBytes + poolNeed;
        synchronized (NucleusGpuBaker.class) {
            if (cacheBytes + need > cacheBudgetBytes()) {
                evictStaleCaches();
                if (cacheBytes + need > cacheBudgetBytes()) {
                    admissionRefusals++;
                    if (!cacheOverflowLogged) {
                        cacheOverflowLogged = true;
                        com.hbm_m.main.MainRegistry.LOGGER.info(
                                "[HBM-M] NucleusGpuBaker: admission refused: entry needs {} MB, {} MB free after {} MB in use"
                                        + " ({} refusals total) - oversized entries stay on the immediate path",
                                Math.max(1L, need >> 20),
                                Math.max(0L, (cacheBudgetBytes() - cacheBytes) >> 20),
                                cacheBytes >> 20,
                                admissionRefusals);
                    }
                    return false;
                }
            }
            // Flat upfront reservation: every later accounting op (growth, free) moves
            // cacheBytes by deltas against exactly these initial sizes, so a failure
            // rollback subtracts `need` and the books stay balanced.
            cacheBytes += need;
        }
        try {
            // Assign the mesh's pool range FIRST (may realloc the pool stores and
            // invalidate other caches of this pass - this cache is fresh anyway).
            mc.outInstances = count;
            mc.eboInstances = count;
            if (!relayoutPool(mode)) {
                // Pool allocation failed: no per-cache objects exist yet, roll the
                // whole reservation back (the pool already rolled back its own delta).
                mc.outInstances = 0;
                mc.eboInstances = 0;
                synchronized (NucleusGpuBaker.class) {
                    cacheBytes -= need;
                }
                return false;
            }
            synchronized (NucleusGpuBaker.class) {
                cacheBytes -= poolNeed; // the pool accounted its actual delta
            }
            mc.recordSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(mc.recordSsbo, "HBM_Record_" + baked.debugTag() + "_" + (mode == MODE_SHADOW ? "Shadow" : "Main"));
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.recordSsbo);
            mc.recordSize[0] = recStorage;
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, mc.recordSize[0], GL15.GL_DYNAMIC_COPY);
            mc.recordCopy = new float[(int) (recBytes / 4L)];
            mc.ctlSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(mc.ctlSsbo, "HBM_Ctl_" + baked.debugTag() + "_" + (mode == MODE_SHADOW ? "Shadow" : "Main"));
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.ctlSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, ctlBytes, GL15.GL_DYNAMIC_COPY);
            mc.ctlBytes = ctlBytes;
            mc.dirtySsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(mc.dirtySsbo, "HBM_Dirty_" + baked.debugTag() + "_" + (mode == MODE_SHADOW ? "Shadow" : "Main"));
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.dirtySsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, dirtySsboBytes, GL15.GL_DYNAMIC_COPY);
            mc.dirtyBytes = dirtySsboBytes;
            mc.dirtyIdx = new int[count];
            mc.dirtyCount = 0;
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            return true;
        } catch (Exception t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: cache allocation failed - immediate path", t);
            // Release partial objects WITHOUT the field-delta accounting (freeModeCache
            // would subtract sizes that were never added) - drop the flat reservation.
            if (mc.recordSsbo != -1) GL15.glDeleteBuffers(mc.recordSsbo);
            if (mc.ctlSsbo != -1) GL15.glDeleteBuffers(mc.ctlSsbo);
            if (mc.dirtySsbo != -1) GL15.glDeleteBuffers(mc.dirtySsbo);
            mc.recordSsbo = -1;
            mc.ctlSsbo = -1;
            mc.dirtySsbo = -1;
            mc.recordSize[0] = -1;
            mc.ctlBytes = -1;
            mc.dirtyBytes = -1;
            mc.dirtyIdx = null;
            mc.dirtyCount = 0;
            mc.recordCopy = null;
            mc.outInstances = 0;
            mc.eboInstances = 0;
            synchronized (NucleusGpuBaker.class) {
                cacheBytes -= need - poolNeed;
            }
            return false;
        }
    }

    /** Frees caches unused for LRU_FRAME_LIMIT frames to admit fresh allocations.
     * Fully releases BakedMesh entries whose companion mesh was closed (skin change,
     * world switch) - they can never bake again and would otherwise hold dead GL
     * objects until budget pressure. getOrCreate rebuilds on the next use. */
    private static void evictStaleCaches() {
        long now = frameStamp();
        for (int i = BAKED.size() - 1; i >= 0; i--) {
            BakedMesh bm = BAKED.get(i);
            if (!bm.mesh.isBuilt()) {
                releaseBakedMesh(bm);
                BAKED.remove(i);
                continue;
            }
            releaseCacheIfStale(bm.shadow, now);
            releaseCacheIfStale(bm.main, now);
        }
    }

    private static void releaseCacheIfStale(ModeCache mc, long now) {
        // Pool-aware liveness (cacheAllocated = outInstances > 0): idle caches
        // age out of the global-batch path and stop pinning their pool ranges +
        // record snapshots after LRU_FRAME_LIMIT frames. freeModeCache is
        // pool-safe: it
        // releases the range (outInstances = 0), which the next relayoutPool
        // reclaims from the front.
        if (cacheAllocated(mc) && now - mc.lastUseFrame > LRU_FRAME_LIMIT) {
            freeModeCache(mc);
        }
    }

    /** Releases every GL object owned by a BakedMesh (both caches + mesh SSBOs). */
    private static void releaseBakedMesh(BakedMesh bm) {
        freeModeCache(bm.shadow);
        freeModeCache(bm.main);
        if (bm.inputSsbo != -1) GL15.glDeleteBuffers(bm.inputSsbo);
        if (bm.baseIdxSsbo != -1) GL15.glDeleteBuffers(bm.baseIdxSsbo);
    }

    private static void freeModeCache(ModeCache mc) {
        synchronized (NucleusGpuBaker.class) {
            // Global-batch path: output/EBO live in the pass pool and are accounted
            // there once; freeing the cache only releases its RANGE (outInstances/
            // eboInstances = 0 below - the space is reclaimed from the front by the
            // next relayout, never leaked).
            cacheBytes -= Math.max(mc.ctlBytes, 0)
                    + Math.max(mc.recordSize[0], 0)
                    + Math.max(mc.dirtyBytes, 0);
        }
        mc.outInstances = 0;
        mc.eboInstances = 0;
        if (mc.recordSsbo != -1) GL15.glDeleteBuffers(mc.recordSsbo);
        if (mc.ctlSsbo != -1) GL15.glDeleteBuffers(mc.ctlSsbo);
        if (mc.dirtySsbo != -1) GL15.glDeleteBuffers(mc.dirtySsbo);
        mc.recordSsbo = -1;
        mc.recordSize[0] = -1;
        mc.ctlSsbo = -1;
        mc.ctlBytes = -1;
        mc.dirtySsbo = -1;
        mc.dirtyBytes = -1;
        mc.dirtyIdx = null;
        mc.dirtyCount = 0;
        mc.filled = -1;
        mc.recordCopy = null;
        mc.recordCount = -1;
        mc.boneKey = -1;
        mc.cullValid = false;
        mc.outputValid = false;
        mc.countsDirty = false;
        mc.stagingSlot = -1;
        mc.frustumStamp = Long.MIN_VALUE;
        mc.admissionRefusedFrame = -1L;
    }

    // ── Global-batch pool: range layout, storage, flush ────────────────

    /**
     * Grows the cache's range capacity in the pass pool and re-lays-out ranges.
     * MUST run before the cull pre-pass of a bake: a pool realloc invalidates
     * output/EBO state that the same bake then rebuilds (cull + expansion), never
     * state it has already built.
     *
     * @return false when the growth was refused (cache budget, pool realloc
     *         failure) - the capacity fields are rolled back and the caller
     *         refuses the entry (Tier-2/3 fallback); the parked range is
     *         deliberately NOT freed (a mid-frame free shifts other meshes'
     *         pool bases and invalidates their already-baked output).
     */
    private static boolean ensureOutputCapacity(BakedMesh baked, ModeCache mc, int count, int mode,
                                                boolean relayoutAllowed) {
        boolean grew = false;
        int oldOut = mc.outInstances;
        int oldEbo = mc.eboInstances;
        if (count > mc.outInstances) {
            // 1.25x step aligned to 16 instances (was doubling): with the pool's
            // own headroom the old 2x step stacked to ~3x live need in VRAM.
            mc.outInstances = Math.max(count, mc.outInstances + Math.max(16, mc.outInstances >> 2));
            grew = true;
        }
        // The EBO range must cover count instances too (the cull EBO-build writes
        // idxCount entries per VISIBLE instance, visible <= count; the no-cull
        // CPU fill grows it further via expandPoolEbo).
        if (count > mc.eboInstances) {
            mc.eboInstances = Math.max(count, mc.eboInstances + Math.max(16, mc.eboInstances >> 2));
            grew = true;
        }
        if (grew) {
            // 2026-10-07 shadow-fix relayout deferral: a relayout already ran
            // this frame's walk - refuse soft WITHOUT the evict sweep; the
            // caller refuses the entry for this frame (Tier-2/3 fallback) and
            // the growth succeeds on a later frame, before entries are pending.
            // Capacity fields roll back (the parked-range contract is preserved).
            if (!relayoutAllowed) {
                mc.outInstances = oldOut;
                mc.eboInstances = oldEbo;
                return false;
            }
            // Admission backoff: a capacity GROWTH inside the window is
            // refused without re-attempting the relayout
            // (and its evict sweep) every frame. Same-capacity requests pass.
            if (admissionBackoffActive(mc)) {
                mc.outInstances = oldOut;
                mc.eboInstances = oldEbo;
                return false;
            }
            if (!relayoutPool(mode)) {
                // Budget refusal: the ONLY budget-freeing mechanism is LRU eviction
                // (evictStaleCaches), and without this retry a refusing cache had
                // to wait until its own lastUseFrame went stale before eviction
                // could release it - the refresh moved AFTER the growth steps in
                // bakeEntry, so this in-place retry frees budget the same frame.
                // Evict, then retry the layout once. Eviction only releases pool
                // RANGES (outInstances = 0); bases shift on the retry's relayout,
                // which is the normal invalidation contract.
                evictStaleCaches();
                if (!relayoutPool(mode)) {
                    mc.outInstances = oldOut;
                    mc.eboInstances = oldEbo;
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * CPU expansion of the mesh's EBO range in the pass pool up to {@code count}
     * instances (no-cull path; the cull path builds the EBO on the GPU). Entries
     * are absolute: vertBase + slot*vertCount + baseIdx. 1.25x growth step
     * aligned to 16 instances, cached - same policy as ensureOutputCapacity
     * (the old doubling stacked with the pool's 1.25x store headroom to ~2.5x
     * live EBO need).
     */
    private static boolean expandPoolEbo(BakedMesh baked, ModeCache mc, int count, int mode,
                                         boolean relayoutAllowed) {
        if (mc.eboCapacity >= count && mc.filled == count) {
            return true;
        }
        int targetCapacity = Math.max(count, Math.max(MIN_EBO_INSTANCES,
                mc.eboCapacity + Math.max(16, mc.eboCapacity >> 2)));
        // Overflow guard (long math, same Integer.MAX_VALUE check relayoutPool
        // applies to its summed ranges): placed BEFORE any capacity/relayout
        // mutation, so a refusal leaves the pool layout untouched - the int
        // product used to wrap negative and throw out of memAllocInt, and the
        // bake catch-all then shut Tier 1 down for the whole session.
        if (!eboExpansionFits(baked, targetCapacity)) {
            return false;
        }
        // Refusals roll the capacity field back and fail SOFT (false): this used
        // to throw IllegalStateException, which bakeMain's catch converts into a
        // whole-session Tier 1 shutdown - a budget refusal must never do that.
        // Relayout deferral (2026-10-07 shadow-fix, see relayoutPool): refuse
        // soft BEFORE touching the capacity fields when a relayout already ran
        // this frame's walk (snapshot taken at bakeEntry start, so THIS bake's
        // own back-to-back layout runs below still count as its single
        // relayout); runExpansion fails the entry for the frame (companion
        // fallback), the growth succeeds next frame.
        if (!relayoutAllowed && (count > mc.eboInstances || targetCapacity > mc.eboInstances)) {
            return false;
        }
        if (count > mc.eboInstances) {
            int oldEbo = mc.eboInstances;
            mc.eboInstances = Math.max(count, mc.eboInstances + Math.max(16, mc.eboInstances >> 2));
            if (!relayoutPool(mode)) {
                mc.eboInstances = oldEbo;
                return false;
            }
        }
        PassPool pool = pool(mode);
        if (targetCapacity > mc.eboInstances) {
            int oldEbo = mc.eboInstances;
            mc.eboInstances = targetCapacity;
            if (!relayoutPool(mode)) {
                mc.eboInstances = oldEbo;
                return false;
            }
        }
        // GPU identity fill first: the same cull program pass 1 (EBO build)
        // uses, with the same placement uniforms (uVertBase/uEboBase) - writes
        // exactly the bytes the CPU loop below produces, without pulling tens
        // of MB through the CPU per expansion. The CPU loop runs unchanged
        // whenever the dispatch is refused - cullProgramId at -1 (bakeCull kill
        // switch / compile failure), the X work-group limit, or a dispatch GL
        // error (see dispatchIdentityEboFill): this no-cull path must stay CPU
        // there, and nothing latches before the CPU bytes are in.
        if (pool.eboSsbo > 0 && dispatchIdentityEboFill(baked, pool.eboSsbo,
                targetCapacity, baked.vertBase[mode], baked.eboWordBase[mode])) {
            mc.eboCapacity = targetCapacity;
            mc.filled = count;
            return true;
        }
        IntBuffer data = MemoryUtil.memAllocInt((int) ((long) baked.idxCount * targetCapacity));
        try {
            int vertBase = baked.vertBase[mode];
            for (int i = 0; i < targetCapacity; i++) {
                int base = vertBase + i * baked.vertCount;
                for (int k = 0; k < baked.idxCount; k++) {
                    data.put(baked.baseIndices[k] + base);
                }
            }
            data.flip();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, pool.eboSsbo);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER,
                    (long) baked.eboWordBase[mode] * 4L, data);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            mc.eboCapacity = targetCapacity;
            mc.filled = count;
        } finally {
            MemoryUtil.memFree(data);
        }
        return true;
    }

    /**
     * Recomputes per-mesh vertBase/eboWordBase placement for one pass from the
     * live range capacities (prefix sums over BAKED meshes in BAKED order) and
     * grows the pool stores when the summed ranges exceed the allocated size.
     * Frees the stores when the last range disappears; no shrink otherwise -
     * freed ranges are reclaimed from the front by the next relayout (no leak,
     * no realloc churn).
     * <p>
     * Invalidation model: a pool store realloc (orphaning glBufferData) wipes the
     * pass's baked bytes, so EVERY cache of the pass is invalidated (full re-bake;
     * the F3 pool counters surface this as a pool realloc). A base shift of a
     * single live cache (another mesh's range grew ahead of it in BAKED order)
     * invalidates just that mesh.
     *
     * @return false on a GL allocation failure (caller falls back / aborts Tier 1).
     */
    private static boolean relayoutPool(int mode) {
        PassPool pool = pool(mode);
        // 2026-10-07 shadow-fix: relayouts are DEFERRED by the walk's callers
        // (see poolRelayoutDeferred + the relayoutAllowed snapshot in bakeEntry):
        // ONE growth relayout per pass per frame. A mid-walk relayout reallocs
        // the pool stores (orphaning glBufferData) and wipes EVERY cache of the
        // pass - entries already baked and appended to the pending list earlier
        // in the walk were then skipped by the flush (their output was gone)
        // and vanished for exactly that frame: model flicker in main, holes in
        // the shadow map. Past the first relayout of a frame, growth is refused
        // soft by the callers: they roll their capacity fields back and refuse
        // the entry (the admission-failure contract, Tier-2/3 fallback); the
        // growth succeeds on a later frame's walk, before entries are pending.
        // relayoutPool itself stays ungated so the
        // back-to-back layout runs of ONE bake (allocateCache +
        // expandPoolEbo's EBO target growth) count as the single relayout.
        int stride = outStrideBytes(mode);
        long verts = 0, words = 0;
        long outNeeded = 0, eboNeeded = 0;
        // Trial pass: compute the whole new layout WITHOUT committing anything,
        // so a refusal below cannot leave half-updated bases behind (a base shift
        // without the backing store resize would point draws at foreign bytes).
        int n = BAKED.size();
        int[] newVertBases = new int[n];
        int[] newEboBases = new int[n];
        for (int i = 0; i < n; i++) {
            BakedMesh bm = BAKED.get(i);
            ModeCache mc = bm.cache(mode);
            newVertBases[i] = (int) verts;
            newEboBases[i] = (int) words;
            verts += (long) bm.vertCount * mc.outInstances;
            words += (long) bm.idxCount * mc.eboInstances;
            outNeeded += (long) bm.vertCount * mc.outInstances * stride;
            eboNeeded += (long) bm.idxCount * mc.eboInstances * 4L;
        }
        if (verts > Integer.MAX_VALUE || words > Integer.MAX_VALUE) {
            return false; // cannot happen within the cache budget; guard anyway
        }
        // Budget gate for GROWTH: admission checks an entry's worst-case reserve,
        // but count growth (chunk-load waves, spawn waves) used to bypass the
        // budget entirely - the pool inflated to 409 MB against a 192 MB budget
        // with 1000 stress machines and nothing could evict it back. Only the
        // delta the pool will actually charge is gated here: the growth branch
        // of ensurePoolStorage allocates poolStoreTarget(needed) (1.25x headroom)
        // exactly when needed exceeds the current allocation, so the gate below
        // mirrors that condition term by term (gate and charge must match or
        // the budget leaks by the headroom).
        long delta = (outNeeded > pool.outputAllocBytes
                              ? poolStoreTarget(outNeeded) - pool.outputAllocBytes : 0L)
                   + (eboNeeded > pool.eboAllocBytes
                              ? poolStoreTarget(eboNeeded) - pool.eboAllocBytes : 0L);
        if (delta > 0) {
            synchronized (NucleusGpuBaker.class) {
                if (cacheBytes + delta > cacheBudgetBytes()) {
                    return false; // callers roll their own capacity fields back
                }
            }
        }
        if (!ensurePoolStorage(pool, outNeeded, eboNeeded)) {
            return false;
        }
        // Commit pass: write the bases and invalidate shifted live ranges (their
        // baked bytes moved within the store; the next bake of each rebuilds).
        for (int i = 0; i < n; i++) {
            BakedMesh bm = BAKED.get(i);
            ModeCache mc = bm.cache(mode);
            boolean shifted = bm.vertBase[mode] != newVertBases[i]
                    || bm.eboWordBase[mode] != newEboBases[i];
            bm.vertBase[mode] = newVertBases[i];
            bm.eboWordBase[mode] = newEboBases[i];
            if (shifted && mc.outInstances > 0) {
                invalidateBakedOutput(mc);
            }
        }
        pool.lastRelayoutFrame = frameStamp();
        return true;
    }

    /**
     * Allocation target for a pool store given the summed live range size:
     * 1.25x headroom over the current need (floor {@link #MIN_POOL_BYTES}) so a
     * chunk-load admission wave does not re-orphan - and with it fully re-bake -
     * the whole pass on every single growth step. A realloc still rebakes every
     * mesh of the pass (unchanged invalidation model); the headroom only cuts
     * HOW OFTEN that happens. (Was 1.5x, which stacked with the callers' old
     * 2x range growth to ~3x live need in VRAM.)
     */
    private static long poolStoreTarget(long needed) {
        return Math.max(needed + (needed >> 2), MIN_POOL_BYTES);
    }

    /** Wipes everything derived from the pool-resident output/EBO bytes of a cache. */
    private static void invalidateBakedOutput(ModeCache mc) {
        mc.outputValid = false;
        mc.cullValid = false;
        mc.countsDirty = false;
        mc.stagingSlot = -1;
        mc.filled = -1;
        mc.eboCapacity = 0;
        mc.frustumStamp = Long.MIN_VALUE;
    }

    /** Grows (or empties) the pool stores to cover the summed live ranges. */
    private static boolean ensurePoolStorage(PassPool pool, long outNeeded, long eboNeeded) {
        try {
            boolean resized = false;
            if (outNeeded > pool.outputAllocBytes) {
                long target = poolStoreTarget(outNeeded);
                if (pool.outputSsbo == -1) {
                    pool.outputSsbo = GL15.glGenBuffers();
                    GlDebugMarkers.labelBuffer(pool.outputSsbo, "HBM_PoolOut_" + poolName(pool) + "_SSBO");
                }
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, pool.outputSsbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, target, GL15.GL_DYNAMIC_COPY);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                synchronized (NucleusGpuBaker.class) {
                    cacheBytes += target - pool.outputAllocBytes;
                }
                pool.outputAllocBytes = target;
                resized = true;
                // Invalidate per store, immediately: glBufferData orphaned every
                // baked byte of this pass, so no cache may keep outputValid across
                // the next statement (the previous after-BOTH invalidation left a
                // window where an exception between the two reallocs returned
                // false with caches claiming outputValid over an orphaned store).
                for (int i = 0; i < BAKED.size(); i++) {
                    invalidateBakedOutput(BAKED.get(i).cache(modeOfPool(pool)));
                }
            }
            if (eboNeeded > pool.eboAllocBytes) {
                long target = poolStoreTarget(eboNeeded);
                if (pool.eboSsbo == -1) {
                    pool.eboSsbo = GL15.glGenBuffers();
                    GlDebugMarkers.labelBuffer(pool.eboSsbo, "HBM_PoolEbo_" + poolName(pool) + "_EBO");
                }
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, pool.eboSsbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, target, GL15.GL_DYNAMIC_COPY);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                synchronized (NucleusGpuBaker.class) {
                    cacheBytes += target - pool.eboAllocBytes;
                }
                pool.eboAllocBytes = target;
                resized = true;
                for (int i = 0; i < BAKED.size(); i++) {
                    invalidateBakedOutput(BAKED.get(i).cache(modeOfPool(pool)));
                }
            }
            if (resized) {
                pool.reallocs++;
                // Surfaced in F3 via the pool realloc counters (invalidation itself
                // now happens per store, right after each realloc).
            }
            if (outNeeded == 0 && eboNeeded == 0
                    && (pool.outputAllocBytes > 0 || pool.eboAllocBytes > 0)) {
                // Last range gone: release the stores (and the VAO that references them).
                if (pool.vao != -1) {
                    GL30.glDeleteVertexArrays(pool.vao);
                    pool.vao = -1;
                }
                if (pool.outputSsbo != -1) {
                    GL15.glDeleteBuffers(pool.outputSsbo);
                    pool.outputSsbo = -1;
                    synchronized (NucleusGpuBaker.class) {
                        cacheBytes -= pool.outputAllocBytes;
                    }
                    pool.outputAllocBytes = 0;
                }
                if (pool.eboSsbo != -1) {
                    GL15.glDeleteBuffers(pool.eboSsbo);
                    pool.eboSsbo = -1;
                    synchronized (NucleusGpuBaker.class) {
                        cacheBytes -= pool.eboAllocBytes;
                    }
                    pool.eboAllocBytes = 0;
                }
            } else if (pool.vao == -1 && pool.outputSsbo != -1 && pool.eboSsbo != -1) {
                // Buffer OBJECTS survive orphaning reallocs, so the VAO is created once.
                pool.vao = createOutputVaoWithEbo(pool.outputSsbo, pool.eboSsbo, pool.slim);
                GlDebugMarkers.labelVao(pool.vao, "HBM_Pool_" + poolName(pool) + "_VAO");
            }
            return true;
        } catch (Exception t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: bake pool allocation failed", t);
            return false;
        }
    }

    private static int modeOfPool(PassPool pool) {
        return pool == POOL_SHADOW ? MODE_SHADOW : MODE_MAIN;
    }

    /**
     * Pool SHRINK policy (audit 2026-10-07 VRAM ledger [A]): the pool store
     * used to stay at 1.5x the all-time peak forever - growth was budget-gated,
     * but demand DROPS (evictStaleCaches frees live ranges) never returned the
     * tail capacity to the driver. Called from the flush path only, after the
     * pass's multi-draw was issued:
     * <ul>
     *   <li>the pass must be idle-stable: no relayoutPool for
     *       {@link #SHRINK_IDLE_FRAMES} frames (so no growth is in flight and
     *       the current live ranges are the settled demand);</li>
     *   <li>the savings must exceed {@link #SHRINK_MIN_SAVINGS}.</li>
     * </ul>
     * The shrink itself mirrors the growth realloc contract exactly: an
     * orphaning glBufferData to {@link #poolStoreTarget}(live need), the same
     * per-store invalidateBakedOutput sweep (every orphaned byte is re-baked by
     * its next bake - the cost reason for the idle+hysteresis gates), and the
     * same cacheBytes adjustment under the class lock. The reallocs counter
     * increments (an orphan IS a full pass re-bake, per F3 semantics); a
     * separate shrinks counter splits the F3 line. Never runs mid-walk: the
     * collect walk's relayouts stamp lastRelayoutFrame, so a walk in progress
     * resets the idle window.
     */
    private static void maybeShrinkPool(PassPool pool, int mode) {
        long now = frameStamp();
        if (pool.outputSsbo == -1 && pool.eboSsbo == -1) {
            return; // nothing allocated (or last-range release already ran)
        }
        if (now - pool.lastRelayoutFrame < SHRINK_IDLE_FRAMES) {
            return;
        }
        // Live need = exact relayoutPool trial math over the CURRENT ranges.
        long outNeeded = 0L, eboNeeded = 0L;
        long stride = outStrideBytes(mode);
        for (int i = 0; i < BAKED.size(); i++) {
            BakedMesh bm = BAKED.get(i);
            ModeCache mc = bm.cache(mode);
            outNeeded += (long) bm.vertCount * mc.outInstances * stride;
            eboNeeded += (long) bm.idxCount * mc.eboInstances * 4L;
        }
        long outTarget = poolStoreTarget(outNeeded);
        long eboTarget = poolStoreTarget(eboNeeded);
        long outSave = pool.outputAllocBytes - outTarget;
        long eboSave = pool.eboAllocBytes - eboTarget;
        try {
            boolean shrunk = false;
            if (outSave >= SHRINK_MIN_SAVINGS && pool.outputSsbo != -1) {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, pool.outputSsbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, outTarget, GL15.GL_DYNAMIC_COPY);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                synchronized (NucleusGpuBaker.class) {
                    cacheBytes -= pool.outputAllocBytes - outTarget;
                }
                pool.outputAllocBytes = outTarget;
                shrunk = true;
            }
            if (eboSave >= SHRINK_MIN_SAVINGS && pool.eboSsbo != -1) {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, pool.eboSsbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, eboTarget, GL15.GL_DYNAMIC_COPY);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                synchronized (NucleusGpuBaker.class) {
                    cacheBytes -= pool.eboAllocBytes - eboTarget;
                }
                pool.eboAllocBytes = eboTarget;
                shrunk = true;
            }
            if (shrunk) {
                // Orphaned every baked byte of the pass: same sweep as a growth
                // realloc (per store, immediately - no cache may keep
                // outputValid across this).
                for (int i = 0; i < BAKED.size(); i++) {
                    invalidateBakedOutput(BAKED.get(i).cache(mode));
                }
                pool.reallocs++;
                pool.shrinks++;
                // Restart the idle window so the pass cannot shrink every frame.
                pool.lastRelayoutFrame = now;
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] NucleusGpuBaker: {} pool shrunk to {} output / {} EBO bytes (idle release)",
                        mode == MODE_SHADOW ? "shadow" : "main", outTarget, eboTarget);
            }
        } catch (Exception t) {
            // Fail closed: leave the pool at its current (larger) sizes - the
            // invalidate sweep below only ran for completed shrinks, so caches
            // of an untouched store stay valid. Retry on a future flush.
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: pool shrink failed (sizes unchanged)", t);
        }
    }

    /**
     * GPU-count flush path (GL 4.6 / ARB_indirect_parameters, cull on): ONE
     * glMultiDrawElementsIndirectCount over the pool command store and NO CPU
     * count readback - the per-flush glGetBufferSubData pipeline sync of the
     * fallback below is gone. Every pending entry's command was written by its
     * last cull run's pass 2 (indexCount = idxCount * GPU visible count); a
     * cache-hit frame reuses the still-valid command exactly like the CPU path
     * reuses lastVisibleCount (both live under cullValid). The drawcount is
     * CPU-known (pending.size()): zero-index commands are legal GPU no-ops, so
     * nothing is compacted, and the 4-byte drawcount upload is stream-ordered
     * client data - no barrier. The GPU-written commands reach the draw through
     * the COMMAND bit of runCull's trailing barrier.
     *
     * @return true when the GPU-count draw was issued; false to fall back to the
     *         CPU readback + compaction path (capability missing, a realloc-wiped
     *         entry, or a stale command slot after a store grow / pending reorder).
     *         Failing caches get frustumStamp reset so their next bake re-culls
     *         (instance-count dispatch, no re-expansion) and rewrites the slot;
     *         the CPU path uploads into a separate store, so all other slots
     *         survive the fallback and the GPU path resumes the next frame.
     */
    private static boolean flushPoolGpuCounts(PassPool pool, int mode, ShaderInstance packShader,
                                              java.util.ArrayList<PendingDraw> pending) {
        if (!mdicEnabled() || !cullEnabled() || pending.isEmpty()) {
            return false;
        }
        int n = pending.size();
        if (pool.cmdSlots < n || pool.vao == -1) {
            return false;
        }
        long instances = 0L;
        boolean allCurrent = true;
        // F3 visibility stats: the GPU-count path never reads ctl headers back,
        // so lastVisibleCount would stay stale/zero. When the debug screen is
        // open, pay for one batched readback so the counters are real; every
        // other frame the stats are the known-zero estimate (documented below).
        boolean statsReadback = com.hbm_m.platform.RenderHooks.isDebugScreenVisible();
        if (statsReadback) {
            resolvePendingCounts(pending);
        }
        for (int i = 0; i < n; i++) {
            PendingDraw pd = pending.get(i);
            ModeCache mc = pd.cache;
            if (!mc.outputValid || !mc.cullValid
                    || mc.cmdGenStamp != pool.cmdGen || mc.cmdSlotWritten != i
                    || i >= pool.cmdSlotOwners.length || pool.cmdSlotOwners[i] != mc) {
                // The GPU-written command must not reach the draw: the entry was
                // wiped by a pool realloc, or its slot holds an orphaned/foreign
                // command (store grew mid-walk / the pending order shifted /
                // another cache overwrote the slot in an earlier frame).
                mc.frustumStamp = Long.MIN_VALUE; // force a cheap re-cull next bake
                allCurrent = false;
                continue;
            }
            instances += (int) mc.lastVisibleCount; // F3 stats only on this path
        }
        if (!allCurrent) {
            return false;
        }
        if (pool.paramSsbo == -1) {
            pool.paramSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(pool.paramSsbo, "HBM_PoolParam_" + poolName(pool) + "_SSBO");
        }
        mdicCountBuf.clear();
        mdicCountBuf.putInt(n).flip();
        // Drawcount: 4 bytes via re-specify (rename/orphan - the same no-sync
        // pattern as the CPU path's command upload; uploads are ordered against
        // the draw, no barrier). No other consumer exists for this binding.
        GL15.glBindBuffer(GL46.GL_PARAMETER_BUFFER, pool.paramSsbo);
        GL15.glBufferData(GL46.GL_PARAMETER_BUFFER, mdicCountBuf, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, pool.cmdSsbo);
        GL30.glBindVertexArray(pool.vao);
        if (mode == MODE_SHADOW) {
            // Shadow draw: resolve the ACTIVE pack program's slim-layout attribute
            // locations and rebind the VAO pointers if they differ from the
            // hard-coded 0/2/5 (see bindSlimShadowAttributes).
            bindSlimShadowAttributes(packShader, pool.outputSsbo);
        } else if (mode == MODE_MAIN) {
            // One bindIrisExtendedAttributes call for the WHOLE multi-draw: the
            // locations are pack-program-dependent and the pointers snapshot
            // GL_ARRAY_BUFFER, so the pool output is bound as the source first.
            bindIrisExtendedAttributes(packShader, pool.outputSsbo, OUT_STRIDE_BYTES,
                    OUT_OFF_MIDTEX, OUT_OFF_TANGENT, OUT_OFF_MIDBLOCK);
        }
        if (GlDebugMarkers.ENABLED && GlDebugMarkers.isActive()) {
            for (int i = 0; i < n; i++) {
                PendingDraw pd = pending.get(i);
                ModeCache mc = pd.cache;
                GlDebugMarkers.insertMarker("SubDraw[" + i + "]: tag=" + pd.baked.debugTag()
                        + " inst=" + mc.lastVisibleCount
                        + " idxCount=" + (pd.baked.idxCount * (int) mc.lastVisibleCount)
                        + " vertBase=" + pd.baked.vertBase[mode]
                        + " eboBase=" + pd.baked.eboWordBase[mode]
                        + " dirty=" + mc.dirtyCount + "/" + mc.recordCount
                        + " gen=" + pool.cmdGen);
            }
        }
        GL46.glMultiDrawElementsIndirectCount(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT,
                0L, 0L, n, 0);
        NucleusDebug.recordDraw(n, (int) instances,
                mode == MODE_MAIN ? "GPU bake main" : "GPU bake shadow");
        bakeGlError(mode == MODE_MAIN ? "main multi-draw (gpu counts)"
                : "shadow multi-draw (gpu counts)");
        GL15.glBindBuffer(GL46.GL_PARAMETER_BUFFER, 0);
        return true;
    }

    /**
     * ONE indirect multi-draw for every mesh baked into this pass's pool this
     * frame. On GL 4.6 / ARB_indirect_parameters (see
     * {@link #flushPoolGpuCounts}) the commands are drawn straight from the
     * store cull pass 2 wrote - no CPU count readback. Otherwise the command
     * buffer is built CPU-side (one command per drawn mesh; zero-instance
     * commands compacted out), uploaded and issued as a single
     * glMultiDrawElementsIndirect. firstIndex = the mesh's EBO range start,
     * baseVertex = 0 (output vertices already carry vertBase), instanceCount = 1
     * (the cull trims via the compacted visible-list EBO).
     *
     * @return the number of indirect draw commands ISSUED (the GPU-count path
     *         reports pending.size(); the CPU path the post-compaction count).
     *         The shadow caller uses this for honest Tier-1 success reporting:
     *         0 means nothing was rasterized and the frame must fall back to
     *         Tier 2/3. Main-pass callers ignore the value.
     */
    private static int flushPool(PassPool pool, int mode, ShaderInstance packShader,
                                  java.util.ArrayList<PendingDraw> pending) {
        if (flushPoolGpuCounts(pool, mode, packShader, pending)) {
            // Same idle-tail release as the CPU path below: on capable hardware
            // this is the NORM for every flush, so the shrink check must sit
            // here too or a stable GL 4.6 scene would never release tail VRAM.
            maybeShrinkPool(pool, mode);
            return pending.size();
        }
        resolvePendingCounts(pending);
        int cmdBytesNeeded = pending.size() * INDIRECT_CMD_BYTES;
        if (pool.cmdCpu == null || pool.cmdCpu.capacity() < cmdBytesNeeded) {
            int cap = Math.max(cmdBytesNeeded,
                    pool.cmdCpu != null ? pool.cmdCpu.capacity() * 2 : 64 * INDIRECT_CMD_BYTES);
            // Untracked memAlloc scratch (GC never reclaims it): allocate the
            // replacement first, then free the old one so a failed grow keeps
            // the previous buffer usable.
            java.nio.ByteBuffer prev = pool.cmdCpu;
            pool.cmdCpu = org.lwjgl.system.MemoryUtil.memAlloc(cap);
            if (prev != null) {
                org.lwjgl.system.MemoryUtil.memFree(prev);
            }
        }
        java.nio.ByteBuffer buf = pool.cmdCpu;
        buf.clear();
        int commands = 0;
        int instances = 0;
        for (int i = 0; i < pending.size(); i++) {
            PendingDraw pd = pending.get(i);
            ModeCache mc = pd.cache;
            if (!mc.outputValid) {
                continue; // invalidated by a pool realloc after its bake - re-bakes next frame
            }
            int indexCount;
            if (cullEnabled()) {
                if (!mc.cullValid) {
                    continue; // EBO/visible list wiped by a realloc - no valid draw
                }
                indexCount = pd.baked.idxCount * (int) mc.lastVisibleCount;
                instances += (int) mc.lastVisibleCount;
            } else {
                if (mc.filled < pd.instanceCount) {
                    continue;
                }
                indexCount = pd.baked.idxCount * pd.instanceCount;
                instances += pd.instanceCount;
            }
            if (indexCount <= 0) {
                continue; // nothing visible: compacted out CPU-side (no no-op command)
            }
            // DrawElementsIndirectCommand: indexCount, instanceCount, firstIndex,
            // baseVertex, baseInstance.
            buf.putInt(indexCount).putInt(1).putInt(pd.baked.eboWordBase[mode]).putInt(0).putInt(0);
            commands++;
        }
        if (commands == 0) {
            return 0;
        }
        buf.flip();
        if (pool.cmdCpuSsbo == -1) {
            pool.cmdCpuSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(pool.cmdCpuSsbo, "HBM_PoolCmdCpu_" + poolName(pool) + "_SSBO");
        }
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, pool.cmdCpuSsbo);
        // Single-call implicit orphan + upload: glBufferData re-specifies the
        // whole store, so the previous frame's glMultiDrawElementsIndirect can
        // never observe an overwrite of a store it is still reading from (the
        // old unsynchronized glBufferSubData into the same buffer object had
        // exactly that in-flight hazard). The driver serves this as a
        // rename/orphan - no CPU sync, no stall - and command buffers are a few
        // KB, so re-specifying per frame is free. This store is deliberately
        // SEPARATE from the pass-2 slot store (pool.cmdSsbo): an orphan here
        // must not destroy GPU-written slots that cache-hit entries still draw
        // from on the GPU-count path.
        GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindVertexArray(pool.vao);
        if (mode == MODE_SHADOW) {
            // Shadow draw: resolve the ACTIVE pack program's slim-layout attribute
            // locations and rebind the VAO pointers if they differ from the
            // hard-coded 0/2/5 (see bindSlimShadowAttributes).
            bindSlimShadowAttributes(packShader, pool.outputSsbo);
        } else if (mode == MODE_MAIN) {
            // One bindIrisExtendedAttributes call for the WHOLE multi-draw: the
            // locations are pack-program-dependent and the pointers snapshot
            // GL_ARRAY_BUFFER, so the pool output is bound as the source first.
            bindIrisExtendedAttributes(packShader, pool.outputSsbo, OUT_STRIDE_BYTES,
                    OUT_OFF_MIDTEX, OUT_OFF_TANGENT, OUT_OFF_MIDBLOCK);
        }
        if (GlDebugMarkers.ENABLED && GlDebugMarkers.isActive()) {
            int cmdIdx = 0;
            for (int i = 0; i < pending.size(); i++) {
                PendingDraw pd = pending.get(i);
                ModeCache mc = pd.cache;
                if (!mc.outputValid) continue;
                int count = cullEnabled() ? (mc.cullValid ? (int) mc.lastVisibleCount : 0) : (mc.filled >= pd.instanceCount ? pd.instanceCount : 0);
                if (count <= 0) continue;
                GlDebugMarkers.insertMarker("SubDraw[" + cmdIdx + "]: tag=" + pd.baked.debugTag()
                        + " inst=" + count
                        + " idxCount=" + (pd.baked.idxCount * count)
                        + " vertBase=" + pd.baked.vertBase[mode]
                        + " eboBase=" + pd.baked.eboWordBase[mode]
                        + " dirty=" + mc.dirtyCount + "/" + mc.recordCount
                        + " gen=" + pool.cmdGen);
                cmdIdx++;
            }
        }
        GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, 0L, commands, 0);
        NucleusDebug.recordDraw(commands, instances,
                mode == MODE_MAIN ? "GPU bake main" : "GPU bake shadow");
        bakeGlError(mode == MODE_MAIN ? "main multi-draw" : "shadow multi-draw");
        // Idle-tail release: at most twice per frame (once per pass), gated by
        // 600 idle frames + 8 MiB savings inside - free after the flush, never
        // mid-walk and never per frame.
        maybeShrinkPool(pool, mode);
        return commands;
    }

    /**
     * ONE CPU-GPU sync per pool flush instead of one per renderer per cull run.
     * The cull pre-passes queue their compute dispatches during the collect walk;
     * the CPU-side visible count (indirect-command indexCount) is only needed
     * HERE. Each dirty ctl header is copied into the staging SSBO GPU-side
     * (glCopyBufferSubData - async, no CPU sync; the BUFFER_UPDATE barrier makes
     * the queued compute writes visible to the copy), then the whole batch is
     * read back with a single glGetBufferSubData.
     */
    private static void resolvePendingCounts(java.util.ArrayList<PendingDraw> pending) {
        int dirty = 0;
        for (int i = 0; i < pending.size(); i++) {
            PendingDraw pd = pending.get(i);
            if (pd.cache.countsDirty && pd.cache.cullValid && pd.cache.ctlSsbo != -1) {
                dirty++;
            }
        }
        if (dirty == 0) {
            return;
        }
        long neededBytes = (long) dirty * CTL_HEADER_WORDS * 4L;
        if (countsStagingBytes < neededBytes) {
            long target = Math.max(neededBytes, Math.max(countsStagingBytes * 2, 4096L));
            long delta = target - countsStagingBytes;
            // Budget-gated like ensureCacheRecords (the growth used to charge
            // cacheBytes ungated - audit 2026-10-07 ledger [B](2)). On refusal:
            // fail closed - the affected entries keep countsDirty but lose
            // cullValid, so flushPool skips them this frame and the next bake
            // re-runs the cheap instance-count cull.
            synchronized (NucleusGpuBaker.class) {
                if (cacheBytes + delta > cacheBudgetBytes()) {
                    evictStaleCaches();
                    if (cacheBytes + delta > cacheBudgetBytes()) {
                        for (int i = 0; i < pending.size(); i++) {
                            PendingDraw pd = pending.get(i);
                            if (pd.cache.countsDirty && pd.cache.cullValid && pd.cache.ctlSsbo != -1) {
                                pd.cache.cullValid = false;
                            }
                        }
                        return;
                    }
                }
                cacheBytes += delta;
            }
            if (countsStagingSsbo == -1) {
                countsStagingSsbo = GL15.glGenBuffers();
                GlDebugMarkers.labelBuffer(countsStagingSsbo, "HBM_CountsStaging_SSBO");
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, countsStagingSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, target, GL15.GL_STREAM_READ);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            countsStagingBytes = target;
        }
        GL42.glMemoryBarrier(GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
        int slot = 0;
        for (int i = 0; i < pending.size(); i++) {
            PendingDraw pd = pending.get(i);
            ModeCache mc = pd.cache;
            if (!mc.countsDirty || !mc.cullValid || mc.ctlSsbo == -1) {
                continue;
            }
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, mc.ctlSsbo);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, countsStagingSsbo);
            GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER,
                    0L, (long) slot * CTL_HEADER_WORDS * 4L, (long) CTL_HEADER_WORDS * 4L);
            mc.stagingSlot = slot;
            slot++;
        }
        GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
        GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
        int readbackBytes = slot * CTL_HEADER_WORDS * 4;
        if (ctlBatchReadback.capacity() < readbackBytes) {
            // Untracked memAlloc scratch (GC never reclaims it): allocate the
            // replacement first, then free the old one so a failed grow keeps
            // the previous buffer usable.
            java.nio.ByteBuffer prev = ctlBatchReadback;
            ctlBatchReadback = org.lwjgl.system.MemoryUtil.memAlloc(
                    Math.max(readbackBytes, prev.capacity() * 2));
            org.lwjgl.system.MemoryUtil.memFree(prev);
        }
        ctlBatchReadback.clear();
        ctlBatchReadback.limit(readbackBytes);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, countsStagingSsbo);
        GL15.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, ctlBatchReadback);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        bakeGlError("batched counts readback");
        // NO flip(): LWJGL's glGetBufferSubData fills remaining() bytes but does
        // NOT advance the position, so after clear()+limit() the window is already
        // [0, readbackBytes) - flip() would collapse the limit to 0 and every
        // relative get below would underflow (2026-10-06 session kill).
        for (int i = 0; i < pending.size(); i++) {
            PendingDraw pd = pending.get(i);
            ModeCache mc = pd.cache;
            if (!mc.countsDirty || !mc.cullValid || mc.ctlSsbo == -1 || mc.stagingSlot < 0
                    || mc.stagingSlot >= slot) {
                continue;
            }
            int slotBase = mc.stagingSlot * CTL_HEADER_WORDS * 4;
            ctlBatchReadback.position(slotBase);
            int visible = ctlBatchReadback.getInt();
            if (visible < 0 || visible > pd.instanceCount) {
                // FAIL CLOSED: a glitched/stale header must not clamp UP to the full
                // count - flushPool would then issue indexCount over EBO words this
                // cull run never wrote (garbage indices = stretched/deformed
                // geometry). Invalidate the cull state instead: flushPool skips the
                // entry this frame and the next bakeEntry re-runs the cull
                // (frustumChanged fires on !cullValid).
                mc.cullValid = false;
                mc.countsDirty = false;
                mc.stagingSlot = -1;
                continue;
            }
            mc.lastVisibleCount = visible;
            // getInt(index) is ABSOLUTE (position-independent): the occlusion word
            // lives at slotBase + word 15 (2026-10-06 audit: the bare 15*4 read
            // returned slot 0's counter for every later slot - F3 stats only).
            int occluded = ctlBatchReadback.getInt(slotBase + 15 * 4);
            mc.lastOccludedCount = mc.lastCullHiZ ? (occluded & 0xFFFFFFFFL) : 0L;
            mc.countsDirty = false;
            mc.stagingSlot = -1;
        }
        ctlBatchReadback.clear();
        ctlBatchReadback.limit(ctlBatchReadback.capacity());
    }

    /**
     * End-of-phase flush of the global MAIN-pass bake batch. Called once per frame
     * from {@code InstancedRenderFrame.presentAfterBlockEntities} AFTER the flat
     * flush walk (every main-pass bake flows MachineRenderRegistry.flushAll ->
     * MachineSpec.flush -> MachinePartRenderer.flush -> InstancedStaticPartRenderer
     * .flush -> IrisInstancedBatchRenderer.flushBatchIris -> bakeMain). Issues ONE
     * glMultiDrawElementsIndirect through the pack's own gbuffers program. No-op
     * when nothing baked or without a pack shader.
     */
    public static void presentMainBatch(Matrix4f projection) {
        if (PENDING_MAIN.isEmpty()) {
            return;
        }
        if (!isEnabled() || !RenderSystem.isOnRenderThread()) {
            PENDING_MAIN.clear();
            return;
        }
        ShaderInstance packShader = com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                .getBlockShader(false);
        if (packShader == null) {
            PENDING_MAIN.clear();
            return;
        }
        bakeGlDrainForeign("main multi-draw entry");
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        // *BINDING query pnames - see the note in bakeAndDrawShadow.
        int prevDrawIndirect = GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING);
        try (com.hbm_m.client.render.shader.IrisPhaseGuard guard =
                     com.hbm_m.client.render.shader.IrisPhaseGuard.pushBlockEntities()) {
            if (!com.hbm_m.client.render.shader.IrisShaderApply.tryApply(packShader)) {
                // One re-resolve + retry before giving up: a transient apply failure
                // (destroyed GlResource during an Iris pipeline rebuild) used to
                // silently drop the WHOLE frame's bake batch - the collect walk's
                // compute work discarded, machines invisible for a frame.
                com.hbm_m.client.render.shader.IrisExtendedShaderAccess.invalidateShaderCache();
                com.hbm_m.client.render.shader.IrisRenderBatch.invalidateCaches();
                packShader = com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                        .getBlockShader(false);
                if (packShader == null
                        || !com.hbm_m.client.render.shader.IrisShaderApply.tryApply(packShader)) {
                    return;
                }
            }
            try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();
                // Pack program active; same matrix contract as the collect bake:
                // ModelViewMat = R_cam * T(-relCam), ProjMat = stripped projection.
                GlStateManager._glUseProgram(packShader.getId());
                Matrix4f viewRot = new Matrix4f(FrameViewState.viewBasisRotation());
                MAIN_MV.set(viewRot).translate(-FrameViewState.relCamX(),
                        -FrameViewState.relCamY(), -FrameViewState.relCamZ());
                setMatrices(packShader, projection, MAIN_MV);
                // Explicit unit-0 atlas bind (the bake path never relies on
                // setShaderTexture alone).
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
                RenderSystem.bindTexture(Minecraft.getInstance().getTextureManager()
                        .getTexture(TextureAtlas.LOCATION_BLOCKS).getId());
                int previousBlockEntityId = com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                        .setCurrentRenderedBlockEntity(0);
                if (GlDebugMarkers.ENABLED) {
                    GlDebugMarkers.pushGroup("HBM_PresentMainBatch");
                }
                try {
                    flushPool(POOL_MAIN, MODE_MAIN, packShader, PENDING_MAIN);
                } finally {
                    if (GlDebugMarkers.ENABLED) {
                        GlDebugMarkers.popGroup();
                    }
                    com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                            .restoreCurrentRenderedBlockEntity(previousBlockEntityId);
                    // Restore the pack shader's matrices from RenderSystem so the
                    // iris_ModelViewMat uniform no longer holds our -relCam matrix.
                    // setMatrices uploads via raw glUniformMatrix4fv, bypassing the
                    // RenderSystem cache - without this restore a later hand/weapon
                    // draw whose cached RenderSystem matrix is unchanged would skip
                    // its re-upload and render with our matrix (disappears).
                    setMatrices(packShader, RenderSystem.getProjectionMatrix(),
                            RenderSystem.getModelViewMatrix());
                }
                packShader.clear();
            }
        } catch (Exception t) {
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[HBM-M] NucleusGpuBaker: global main multi-draw failed - "
                            + "falling back to companion path", t);
            available = false;
            releaseResourcesInternal();
        } finally {
            PENDING_MAIN.clear();
            com.hbm_m.client.render.shader.ShaderBindResync.markForeignProgramBind();
            GL40.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, prevDrawIndirect);
            // Force the next vanilla immediate draw to rebind its VAO honestly
            // (mirrors MdiBatchCoordinator's exit-to-foreign-GL pattern).
            com.mojang.blaze3d.vertex.BufferUploader.invalidate();
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
    }

    private static BakedMesh getOrCreate(IrisCompanionMesh mesh) {
        for (int i = 0; i < BAKED.size(); i++) {
            if (BAKED.get(i).mesh == mesh) {
                return BAKED.get(i);
            }
        }
        BakedMesh baked = null;
        try {
            int vertCount = mesh.getMeshVertexCount();
            int stride = mesh.getMeshStrideBytes();
            int[] base = mesh.getMeshIndices();
            var format = mesh.getMeshFormat();
            if (vertCount <= 0 || stride == 0 || base == null || base.length == 0 || format == null) {
                return null;
            }
            baked = new BakedMesh(mesh, vertCount, base.length, base);
            ensureBoneBuffer();
            // Base indices as an SSBO for the cull program's EBO-build pass.
            baked.baseIdxSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(baked.baseIdxSsbo, "HBM_BaseIdx_" + baked.debugTag());
            IntBuffer baseBuf = MemoryUtil.memAllocInt(base.length);
            try {
                baseBuf.put(base).flip();
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, baked.baseIdxSsbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, baseBuf, GL15.GL_STATIC_DRAW);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            } finally {
                MemoryUtil.memFree(baseBuf);
            }
            buildAlignedInput(baked);
            BAKED.add(baked);
            return baked;
        } catch (Exception t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker: baked mesh init failed", t);
            if (baked != null) {
                // Partial-failure cleanup: baked was never added to BAKED, so no
                // later teardown (evictStaleCaches / releaseResourcesInternal) can
                // reach these fresh names - delete them here or a persistent
                // failure re-leaks a set every bake attempt. releaseBakedMesh is
                // NOT used: it also frees the ModeCaches, whose cacheBytes
                // accounting never ran for a partially built mesh. Delete +
                // reset keeps this block idempotent; a retry builds a new
                // BakedMesh from clean -1 fields.
                if (baked.baseIdxSsbo != -1) GL15.glDeleteBuffers(baked.baseIdxSsbo);
                if (baked.inputSsbo != -1) GL15.glDeleteBuffers(baked.inputSsbo);
                baked.baseIdxSsbo = -1;
                baked.inputSsbo = -1;
            }
            return null;
        }
    }

    /**
     * Output VAO over {@code vbo} with a PRE-EXISTING EBO bound inside the VAO.
     * Also consumes the placeholder EBO {@link #createOutputVao} reports via its
     * out-param: rebinding the caller's EBO orphans it (it has no data store and
     * no other owner), so it is deleted here instead of leaking one placeholder
     * per call (one per pool VAO). The pre-call VAO binding
     * is saved on entry and restored on every exit.
     */
    private static int createOutputVaoWithEbo(int vbo, int ebo, boolean slim) {
        int previousVao = com.hbm_m.client.render.GlVaoSafety.currentBinding();
        int[] placeholderEbo = {-1};
        int vao = createOutputVao(vbo, slim, placeholderEbo);
        try {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        } catch (Exception t) {
            // The half-built VAO and the placeholder EBO are unreachable by the
            // caller in this window (its vao field is still -1) - restore the
            // caller's VAO binding, delete both names, and rethrow
            // (GL 4.6 §2.6.1.2/§5.1.2).
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL30.glDeleteVertexArrays(vao);
            if (placeholderEbo[0] != -1) {
                GL15.glDeleteBuffers(placeholderEbo[0]);
            }
            throw t;
        }
        com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
        int staleEbo = placeholderEbo[0];
        if (staleEbo != -1) GL15.glDeleteBuffers(staleEbo);
        return vao;
    }

    /** Diagnostics tracking for skipped bake entries (prevents log spam). */
    private static final java.util.HashSet<Integer> SKIP_LOGGED = new java.util.HashSet<>();

    /** One invariant-dump per mode cache for out-of-range dirty indices (see uploadDirtyRecords). */
    private static final java.util.HashSet<Integer> DIRTY_RANGE_DUMP = new java.util.HashSet<>();

    /** Rate limit (ms) for data-shape bake failures that must NOT disable Tier 1. */
    private static volatile long dataFailLogMs;

    /** Tagged GL-error diagnostics counter (the GLDebug callback also logs them; this adds the stage tag). */
    private static int glErrorDiags = 0;
    /** Start of the current 5 s diagnostics window (see allowGlErrorDiag). */
    private static long glErrorWindowStart = 0L;

    /**
     * Rolling-window gate for the GL-error diagnostics. The counter used to be
     * capped at 16 per SESSION with no reset, so the first 16 ambient errors of
     * any foreign storm permanently blinded every bakeGlError/bakeGlDrainForeign
     * check for the rest of the process - including the bake path's own failure
     * signal (2026-10-06 audit). Opt-in on top of the window: a healthy frame
     * raises 0 errors, so the counter never moved and the window alone never
     * throttled - every call drained with synchronous glGetError polls. Poll
     * only while a debug config flag is on; the 5 s window / &lt;16 budget still
     * cap a real error storm once enabled.
     */
    private static boolean allowGlErrorDiag() {
        ModClothConfig cfg;
        try {
            cfg = ModClothConfig.get();
        } catch (Throwable t) {
            return false;
        }
        if (cfg == null || (!cfg.enableDebugLogging && !cfg.mdiDebugLogDispatch)) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - glErrorWindowStart >= 5000L) {
            glErrorWindowStart = now;
            glErrorDiags = 0;
        }
        return glErrorDiags < 16;
    }

    private static void bakeGlError(String stage) {
        if (!allowGlErrorDiag()) {
            return;
        }
        int e;
        while ((e = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            glErrorDiags++;
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[HBM-M] NucleusGpuBaker: GL error 0x{} after stage [{}]",
                    Integer.toHexString(e), stage);
            if (glErrorDiags >= 16) {
                break;
            }
        }
    }

    /**
     * Pre-stage drain: clears errors raised by FOREIGN work since the last drain
     * (Iris uniform pushes into a desynced program, other mods' draws) and labels
     * them explicitly, so the post-stage {@link #bakeGlError} tags only ever name
     * OUR OWN errors. Without this, a GL error raised between our entry points is
     * drained by the next post-stage check and falsely blamed on that stage -
     * the 2026-10-05 log showed Iris uniform-push errors reported as
     * "[cull pre-pass]" / "[main multi-draw]" bake failures this way.
     * Same session cap as {@link #bakeGlError}.
     */
    private static void bakeGlDrainForeign(String stage) {
        if (!allowGlErrorDiag()) {
            return;
        }
        int e;
        while ((e = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            glErrorDiags++;
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[HBM-M] NucleusGpuBaker: GL error 0x{} pending before stage [{}] (raised outside the bake path)",
                    Integer.toHexString(e), stage);
            if (glErrorDiags >= 16) {
                break;
            }
        }
    }

    private static void bakeSkipLog(IrisShadowBatchCollector.Entry e, String reason) {
        int key = e.renderer != null ? System.identityHashCode(e.renderer) : System.identityHashCode(e.customMesh);
        if (SKIP_LOGGED.add(key)) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[HBM-M] NucleusGpuBaker: entry {} skipped in shadow bake ({}), instances={}",
                    key, reason, e.count);
        }
    }

    // ── Tier-1 shadow decline observability (2026-10-15) ──────────────
    // bakeAndDrawShadow now returns false whenever it effectively drew nothing
    // (all entries skipped, 0-command flush, unresolved matrices). Each decline
    // bumps a session counter (F3-accessible) and logs at most once per 30 s so
    // the Tier-2/3 fallback engagement is observable without spamming.
    private static volatile long shadowDeclines = 0L;
    private static long shadowDeclineLogMs = Long.MIN_VALUE;

    /** Session-total count of Tier-1 shadow bakes declined to Tier 2/3. */
    public static long shadowDeclineCount() {
        return shadowDeclines;
    }

    private static void logShadowDecline(String reason, ShaderInstance shader) {
        shadowDeclines++;
        long now = System.currentTimeMillis();
        if (now - shadowDeclineLogMs < 30000L) {
            return;
        }
        shadowDeclineLogMs = now;
        String program = "(unknown)";
        try {
            program = shader != null ? shader.getName() + "#" + shader.getId() : "(no shader)";
        } catch (Throwable ignored) {
            // diagnostics only - never fail the bake for a log name
        }
        com.hbm_m.main.MainRegistry.LOGGER.info(
                "[HBM-M] NucleusGpuBaker: Tier 1 shadow bake declined - falling back to Tier 2/3 "
                        + "(reason: {}, program: {}) [{} declines this session]",
                reason, program, shadowDeclines);
    }


    public static void setBoneMatrices(org.joml.Matrix4f[] bones) {
        if (!isEnabled()) return;
        boneEpoch++;
        if (bones == null || bones.length == 0) {
            hasBoneMatrices = false;
            return;
        }
        ensureBoneBuffer();
        int count = Math.min(bones.length, BONE_PALETTE_SIZE);
        FloatBuffer buf = MemoryUtil.memAllocFloat(count * 16);
        try {
            for (int i = 0; i < count; i++) {
                org.joml.Matrix4f m = bones[i];
                if (m != null) {
                    m.get(buf);
                    buf.position((i + 1) * 16);
                } else {
                    for (int r = 0; r < 4; r++) {
                        for (int c = 0; c < 4; c++) {
                            buf.put(r == c ? 1.0f : 0.0f);
                        }
                    }
                }
            }
            buf.flip();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, boneSsbo);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, buf);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            hasBoneMatrices = true;
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    public static void uploadBonePalette(FloatBuffer matrices, int count) {
        if (!isEnabled() || matrices == null || count <= 0) {
            hasBoneMatrices = false;
            return;
        }
        boneEpoch++;
        ensureBoneBuffer();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, boneSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, matrices);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        hasBoneMatrices = true;
    }

    private static void ensureBoneBuffer() {
        if (boneSsbo == -1) {
            boneSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(boneSsbo, "HBM_BoneSsbo");
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, boneSsbo);
            FloatBuffer defaultBones = MemoryUtil.memAllocFloat(BONE_PALETTE_SIZE * 16);
            try {
                for (int i = 0; i < BONE_PALETTE_SIZE; i++) {
                    for (int r = 0; r < 4; r++) {
                        for (int c = 0; c < 4; c++) {
                            defaultBones.put(r == c ? 1.0f : 0.0f);
                        }
                    }
                }
                defaultBones.flip();
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, defaultBones, GL15.GL_DYNAMIC_DRAW);
            } finally {
                MemoryUtil.memFree(defaultBones);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            }
        }
    }

    /**
     * Grows a per-cache control-block SSBO. Growth is budget-gated like
     * {@link #ensureCacheRecords} (it previously charged cacheBytes with no
     * gate - audit 2026-10-07 ledger [B](2)) with the same evict-retry-once
     * anti-livelock.
     *
     * @return false when the growth was refused: the caller must skip the cull
     *         for this entry this frame (the existing store, if any, stays at
     *         its old capacity and remains valid for smaller counts).
     */
    private static boolean ensureCtlBuffer(ModeCache mc, int count) {
        long bytes = (long) (CTL_HEADER_WORDS + count) * 4L;
        if (bytes > mc.ctlBytes) {
            long newBytes = Math.max(bytes, Math.max(mc.ctlBytes, 0) * 2);
            long delta = newBytes - Math.max(mc.ctlBytes, 0);
            synchronized (NucleusGpuBaker.class) {
                if (cacheBytes + delta > cacheBudgetBytes()) {
                    evictStaleCaches();
                    if (cacheBytes + delta > cacheBudgetBytes()) {
                        return false;
                    }
                }
                cacheBytes += delta;
            }
            if (mc.ctlSsbo == -1) {
                mc.ctlSsbo = GL15.glGenBuffers();
                GlDebugMarkers.labelBuffer(mc.ctlSsbo, "HBM_Ctl_Grow_SSBO");
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.ctlSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            mc.ctlBytes = newBytes;
            // Reallocated control block: visible list and indirect words are gone.
            mc.cullValid = false;
        } else if (mc.ctlSsbo == -1) {
            // First allocation that fits the reserved -1 bytes can never happen
            // (ctlBytes starts at -1), but guard a store-less cache anyway so
            // runCull never subdata-writes into buffer name 0.
            return false;
        }
        return true;
    }

    /**
     * Public session-end teardown (RenderCacheManager hook, see the static
     * initializer): full release of every BakedMesh, the bake/cull programs,
     * shared buffers and both pass pools, plus the session-wide diagnostic sets.
     * Render thread only - invalidateAll enforces it, matching the crash-path
     * callers of {@link #releaseResourcesInternal}.
     */
    public static void releaseAll() {
        releaseResourcesInternal();
    }

    private static void releaseResourcesInternal() {
        for (BakedMesh bm : BAKED) {
            releaseBakedMesh(bm);
        }
        BAKED.clear();
        synchronized (NucleusGpuBaker.class) {
            cacheBytes = 0;
        }
        if (programId != -1) {
            GL20.glDeleteProgram(programId);
            programId = -1;
        }
        if (cullProgramId != -1) {
            GL20.glDeleteProgram(cullProgramId);
            cullProgramId = -1;
        }
        cullInitFailed = false;
        if (boneSsbo != -1) {
            GL15.glDeleteBuffers(boneSsbo);
            boneSsbo = -1;
            hasBoneMatrices = false;
        }
        if (bakeParamsUbo != -1) {
            GL15.glDeleteBuffers(bakeParamsUbo);
            bakeParamsUbo = -1;
        }
        // Global-batch pools: release stores + VAOs, zero the accounting
        // (cacheBytes was reset to 0 above).
        releasePool(POOL_MAIN);
        releasePool(POOL_SHADOW);
        if (countsStagingSsbo != -1) {
            GL15.glDeleteBuffers(countsStagingSsbo);
            countsStagingSsbo = -1;
            countsStagingBytes = 0;
        }
        // Untracked memAlloc readback scratch (GC never reclaims it): free and
        // re-create so the statics stay non-null for the next session (fresh
        // context, fresh backing memory; the old sizes are the initial ones).
        org.lwjgl.system.MemoryUtil.memFree(ctlBatchReadback);
        ctlBatchReadback = org.lwjgl.system.MemoryUtil.memAlloc(4096);
        org.lwjgl.system.MemoryUtil.memFree(mdicCountBuf);
        mdicCountBuf = org.lwjgl.system.MemoryUtil.memAlloc(4);
        PENDING_MAIN.clear();
        PENDING_SHADOW.clear();
        // Session-wide diagnostic sets (identity hash codes of renderers/mode
        // caches): dead after a teardown, grew unbounded for the whole session
        // until now - covers the lifecycle hook and this crash path alike.
        SKIP_LOGGED.clear();
        DIRTY_RANGE_DUMP.clear();
        // Reset the one-way latches so a FULL reload (F3+T / world reload via the
        // RenderCacheManager hook) genuinely restores Tier 1 after a transient
        // failure: checked/available/initFailed stayed false forever once any
        // bake Throwable or probe failure flipped them, while cullInitFailed WAS
        // reset here - the inconsistency that marked the omission (2026-10-06
        // audit: "the mode does not reset"). The log-once latches reset too so a
        // post-reload storm is visible again.
        synchronized (NucleusGpuBaker.class) {
            checked = false;
            available = true;
            initFailed = false;
            glErrorDiags = 0;
            glErrorWindowStart = 0L;
            cacheOverflowLogged = false;
            admissionRefusals = 0;
            foreignShadowMvLogged = false;
            dataFailLogMs = 0L;
        }
    }

    private static void releasePool(PassPool pool) {
        if (pool.vao != -1) {
            GL30.glDeleteVertexArrays(pool.vao);
            pool.vao = -1;
        }
        if (pool.outputSsbo != -1) {
            GL15.glDeleteBuffers(pool.outputSsbo);
            pool.outputSsbo = -1;
            pool.outputAllocBytes = 0;
        }
        if (pool.eboSsbo != -1) {
            GL15.glDeleteBuffers(pool.eboSsbo);
            pool.eboSsbo = -1;
            pool.eboAllocBytes = 0;
        }
        if (pool.cmdSsbo != -1) {
            GL15.glDeleteBuffers(pool.cmdSsbo);
            pool.cmdSsbo = -1;
        }
        pool.cmdSlots = 0;
        pool.cmdGen = 0;
        pool.cmdSlotOwners = new ModeCache[0];
        if (pool.cmdCpuSsbo != -1) {
            GL15.glDeleteBuffers(pool.cmdCpuSsbo);
            pool.cmdCpuSsbo = -1;
        }
        if (pool.paramSsbo != -1) {
            GL15.glDeleteBuffers(pool.paramSsbo);
            pool.paramSsbo = -1;
        }
        // Untracked memAlloc scratch (GC never reclaims it): freed explicitly.
        if (pool.cmdCpu != null) {
            org.lwjgl.system.MemoryUtil.memFree(pool.cmdCpu);
            pool.cmdCpu = null;
        }
    }

    // ── Expansion compute shader: one invocation per output vertex ─────
    // Output is CAMERA-INDEPENDENT (main = anchor-space, shadow = light-space:
    // the shadow records decompose the Iris shadow poseStack, so the light
    // rotation rides inside; the camera offset carried in the record translation
    // is folded out by draw-time ModelViewMat = T(-cam), and the shader subtracts
    // nothing, so the expanded output never contains camera orientation and stays
    // cacheable)
    // and FRUSTUM-INDEPENDENT: vertices are written INSTANCE-MAJOR, so the
    // cull pre-pass can rebuild the visible-list EBO at will without touching
    // the expanded data. The expansion covers the SELECTED instances: identity
    // mode (uDirtyMode = 0) dispatches ALL of them (full re-bakes), dirty-list
    // mode dispatches only the instances named in the per-cache dirty-index
    // SSBO (span re-expansion; untouched slots keep their valid previous
    // output). The cull trims only the DRAW.
    // MODE_SHADOW (0): slim 24-byte output (pos+uv0+normal).
    // MODE_MAIN (1): 56-byte output (UV2 via trilinear interpolation of the
    // 8-corner light records + a computed at_midBlock).
    //
    // INPUT (SSBO 0): aligned relayout of the companion mesh (stride multiple of 4)
    // or the raw mesh VBO when its stride is already aligned. Attributes are read
    // at byte offsets via ldWord (single fetch on aligned strides).

    private static final String COMPUTE_SOURCE = """
            #version 430 core
            // Instance record layout (float words) — injected at init by
            // injectRecordLayout from the InstancedStaticPartRenderer offset constants
            // (the literals below are only a fallback; a drift here corrupts every
            // baked vertex). Layout: pos @0, quat @3 (4 snorm16 halves packed in 2
            // words), bboxMin @5, bboxSize @8, fade @11, corner light @12 (4 packed
            // u8 words), tint @20, grad @24 (32 words = 128 bytes per record).
            #define INST_STRIDE 32u
            #define INST_POS_WORD 0u
            #define INST_QUAT_WORD 3u
            #define INST_BMIN_WORD 5u
            #define INST_BSIZE_WORD 8u
            #define INST_FADE_WORD 11u
            #define INST_UVRECT_WORD 16u
            #define INST_TINT_WORD 20u
            #define INST_GRAD_WORD 24u
            layout(local_size_x = 64, local_size_y = 1) in;

            layout(std430, binding = 0) readonly restrict buffer MeshBuf { uint meshW[]; };
            layout(std430, binding = 1) readonly restrict buffer InstBuf { float instData[]; };
            layout(std430, binding = 2) restrict buffer OutBuf { float outData[]; };
            layout(std430, binding = 3) readonly restrict buffer BonePaletteSSBO { mat4 uBoneMatrices[64]; };
            // Dirty-index list (bound only in dirty-list mode): word 0 = count,
            // words 1.. = instance indices. Identity mode (uDirtyMode == 0) never
            // reads it, so full re-bakes can never consume stale list contents.
            layout(std430, binding = 4) readonly restrict buffer DirtyBuf { uint dirtyData[]; };

            layout(std140, binding = 0) uniform BakeParams {
                int uMeshStride; // Input mesh byte stride (multiple of 4 after relayout)
                int uInstStride; // Instance record float stride (32)
                int uOutStride;  // Output vertex float stride (14 main / 6 shadow)
                int uVertCount;
                int uInstCount;
                int uOffPos;     // Input byte offsets
                int uOffColor;
                int uOffNormal;
                int uOffUv0;
                int uOffUv1;
                int uOffUv2;
                int uOffMidTex;
                int uOffTangent;
                int uBakeMode;   // 0 shadow, 1 main
                int uOffLight;   // Float offset of the packed 8-corner light words (12)
                int uTransformMode; // 0 pos+quat, 1 3x4 affine matrix
                int uHasBones;      // 0 no bones, 1 bone palette active
                int uOffBoneId;     // Byte offset of bone id in mesh (-1 if none)
                int uMeshWords;     // Input mesh size in uint32 words (OOB guard for unaligned tails)
                int uDirtyMode;     // 0 identity (y = instance), 1 dirty-list (y = dirty slot)
                int uVertBase;      // Global-batch output placement: vertex offset of this mesh's range
            };

            shared vec2 s_light[8];
            shared vec3 s_bmin;
            shared vec3 s_bsize;
            shared vec4 s_uvRect;

            uint ldWord(uint base, int byteOff) {
                if (byteOff < 0) return 0u;
                uint a = base + uint(byteOff);
                uint wi = a >> 2u;
                uint w0 = meshW[wi];
                uint sh = (a & 3u) * 8u;
                if (sh == 0u) return w0;
                // Unaligned tail of the LAST vertex: the second word lies past the
                // buffer (raw 54-byte stride fallback) - the attribute's valid bytes
                // all live in w0's high half; reading meshW[wi+1] would be OOB.
                if (wi + 1u >= uint(uMeshWords)) return w0 >> sh;
                uint w1 = meshW[wi + 1u];
                return (w0 >> sh) | (w1 << (32u - sh));
            }

            float ldFloat(uint base, int byteOff) {
                if (byteOff < 0) return 0.0;
                return uintBitsToFloat(ldWord(base, byteOff));
            }

            vec3 quatRotate(vec4 q, vec3 v) {
                return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);
            }

            // snorm16 decode (plain ALU): the record packs the quat as 4 signed
            // 16-bit halves in 2 float words (low half first) - the CPU encoder is
            // InstancedStaticPartRenderer.packSnorm16, the VAO reads the same bytes
            // as GL_SHORT normalized.
            float snorm16ToFloat(uint h) {
                // int overload sign-extends the 16-bit field (mirrors the VAO's
                // GL_SHORT normalized fetch and HalfFloat-style software decode).
                return clamp(float(bitfieldExtract(int(h), 0, 16)) / 32767.0, -1.0, 1.0);
            }

            vec4 instQuat(uint base) {
                uint wxy = floatBitsToUint(instData[base + INST_QUAT_WORD]);
                uint wzw = floatBitsToUint(instData[base + INST_QUAT_WORD + 1u]);
                return vec4(snorm16ToFloat(wxy), snorm16ToFloat(wxy >> 16u),
                            snorm16ToFloat(wzw), snorm16ToFloat(wzw >> 16u));
            }

            void main() {
                // Instance selection: dirty-list mode maps y through the compacted
                // dirty-index list (one workgroup row per DIRTY instance; the count
                // word is re-checked as an OOB guard); identity mode treats y as the
                // raw instance index and covers every instance (full re-bakes).
                uint inst;
                if (uDirtyMode == 1) {
                    uint dy = gl_GlobalInvocationID.y;
                    if (dy >= dirtyData[0u]) return;
                    inst = dirtyData[1u + dy];
                    if (inst >= uint(uInstCount)) return;
                } else {
                    inst = gl_GlobalInvocationID.y;
                    if (inst >= uint(uInstCount)) return;
                }

                uint iBase = uint(inst * uInstStride);     // Instance record float offset

                uint lid = gl_LocalInvocationID.x;
                if (lid < 8u) {
                    // Corner light: 4 packed u8 words, 2 corners per word (low byte
                    // first): word j = c2j.u, c2j.v, c2j+1.u, c2j+1.v. Corner lid ->
                    // word lid>>1, u/v at byte shifts (lid&1)*16 + {0, 8}; decode to
                    // the 0..240 texel grid (byte x 240/255).
                    uint lBase = iBase + uint(uOffLight);
                    uint wraw = floatBitsToUint(instData[lBase + (lid >> 1u)]);
                    uint sh = (lid & 1u) * 16u;
                    s_light[lid] = vec2(float((wraw >> sh) & 0xFFu), float((wraw >> (sh + 8u)) & 0xFFu))
                            * (240.0 / 255.0);
                } else if (lid == 8u) {
                    s_bmin = vec3(instData[iBase + INST_BMIN_WORD], instData[iBase + INST_BMIN_WORD + 1u], instData[iBase + INST_BMIN_WORD + 2u]);
                    s_bsize = max(vec3(instData[iBase + INST_BSIZE_WORD], instData[iBase + INST_BSIZE_WORD + 1u], instData[iBase + INST_BSIZE_WORD + 2u]), vec3(1e-4));
                } else if (lid == 9u) {
                    // Atlas remap rect (words 16..19): sprite-local [0..1] UVs ->
                    // atlas UVs, mirrors block_lit_instanced.vsh's InstUvRect.
                    s_uvRect = vec4(instData[iBase + INST_UVRECT_WORD],
                                    instData[iBase + INST_UVRECT_WORD + 1u],
                                    instData[iBase + INST_UVRECT_WORD + 2u],
                                    instData[iBase + INST_UVRECT_WORD + 3u]);
                }
                barrier();

                uint lv = gl_GlobalInvocationID.x;
                if (lv >= uint(uVertCount)) return;

                uint mBase = uint(lv * uint(uMeshStride)); // Input mesh byte offset
                // Instance-MAJOR output (see header comment): output slot s always
                // belongs to raw instance inst, independent of any cull compaction.
                // Global-batch placement: the mesh's range starts at uVertBase
                // vertices into the pass pool.
                uint o = (uint(uVertBase) + inst * uint(uVertCount) + lv) * uint(uOutStride);

                // Invisible instance (FADE_MODE_INVISIBLE, fade word < 0): the
                // bake output bypasses the vsh fade discard, so culling disabled
                // would draw it as garbage. Write degenerate zero positions -
                // zero-area triangles rasterize into nothing in both the shadow
                // and the main pass.
                if (instData[iBase + INST_FADE_WORD] < 0.0) {
                    outData[o + 0u] = 0.0;
                    outData[o + 1u] = 0.0;
                    outData[o + 2u] = 0.0;
                    return;
                }

                vec3 pos = vec3(ldFloat(mBase, uOffPos),
                                ldFloat(mBase, uOffPos + 4),
                                ldFloat(mBase, uOffPos + 8));

                // Normal decode
                vec3 nrm;
                uint nRaw = 0u;
                if (uOffNormal >= 0) {
                    nRaw = ldWord(mBase, uOffNormal);
                    nrm = vec3(float(bitfieldExtract(int(nRaw), 0, 8)) / 127.0,
                               float(bitfieldExtract(int(nRaw), 8, 8)) / 127.0,
                               float(bitfieldExtract(int(nRaw), 16, 8)) / 127.0);
                } else {
                    nrm = vec3(0.0, 1.0, 0.0);
                }

                // Tangent decode (packed 4 GLbytes, byte 3 = bitangent w sign):
                // rotated together with the normal below so the TBN stays valid
                // on rotated instances.
                vec3 tng = vec3(1.0, 0.0, 0.0);
                uint tRaw = 0u;
                if (uOffTangent >= 0) {
                    tRaw = ldWord(mBase, uOffTangent);
                    tng = vec3(float(bitfieldExtract(int(tRaw), 0, 8)) / 127.0,
                               float(bitfieldExtract(int(tRaw), 8, 8)) / 127.0,
                               float(bitfieldExtract(int(tRaw), 16, 8)) / 127.0);
                }

                // Dynamic skeletal bone animation support
                vec4 localPos = vec4(pos, 1.0);
                if (uHasBones > 0 && uOffBoneId >= 0) {
                    int bone = int(ldWord(mBase, uOffBoneId));
                    if (bone >= 0 && bone < 64) {
                        localPos = uBoneMatrices[bone] * localPos;
                        nrm = mat3(uBoneMatrices[bone]) * nrm;
                        tng = mat3(uBoneMatrices[bone]) * tng;
                    }
                }

                vec3 outPos;
                vec3 nOut;
                vec3 tOut = tng;
                if (uTransformMode == 1) {
                    // Full 12-float 3x4 affine transform matrix (row0, row1, row2)
                    vec4 row0 = vec4(instData[iBase + 0u], instData[iBase + 1u], instData[iBase + 2u], instData[iBase + 3u]);
                    vec4 row1 = vec4(instData[iBase + 4u], instData[iBase + 5u], instData[iBase + 6u], instData[iBase + 7u]);
                    vec4 row2 = vec4(instData[iBase + 8u], instData[iBase + 9u], instData[iBase + 10u], instData[iBase + 11u]);

                    outPos = vec3(dot(row0, localPos), dot(row1, localPos), dot(row2, localPos));

                    // Cofactor / adjoint normal matrix for arbitrary affine transforms (transposed for GLSL column-major order)
                    mat3 normalMat = transpose(mat3(
                        cross(row1.xyz, row2.xyz),
                        cross(row2.xyz, row0.xyz),
                        cross(row0.xyz, row1.xyz)
                    ));
                    float nLenSq = dot(normalMat * nrm, normalMat * nrm);
                    nOut = (nLenSq > 1e-8) ? normalize(normalMat * nrm) : nrm;
                    // Same adjoint matrix as the normal: for the rigid rotations/
                    // translations these instances use it equals the linear part.
                    float tLenSq = dot(normalMat * tng, normalMat * tng);
                    tOut = (tLenSq > 1e-8) ? normalize(normalMat * tng) : tng;
                } else {
                    // pos(3) + packed quat (2 words). No camera subtraction (see header comment).
                    vec4 rot = instQuat(iBase);
                    outPos = quatRotate(rot, localPos.xyz)
                            + vec3(instData[iBase + INST_POS_WORD], instData[iBase + INST_POS_WORD + 1u], instData[iBase + INST_POS_WORD + 2u]);
                    nOut = normalize(quatRotate(rot, nrm));
                    tOut = normalize(quatRotate(rot, tng));
                }

                outData[o + 0u] = outPos.x;
                outData[o + 1u] = outPos.y;
                outData[o + 2u] = outPos.z;

                if (uBakeMode == 0) {
                    // Slim shadow layout (24 bytes): pos@0..2, uv0@3..4, normal@5.
                    // UV0 through the atlas remap rect (identity {0,0,1,1} on
                    // regular machines, so this is a no-op there).
                    outData[o + 3u] = s_uvRect.x + ldFloat(mBase, uOffUv0) * s_uvRect.z;
                    outData[o + 4u] = s_uvRect.y + ldFloat(mBase, uOffUv0 + 4) * s_uvRect.w;
                    uint nSlim = uint(int(round(clamp(nOut.x, -1.0, 1.0) * 127.0)) & 255)
                            | (uint(int(round(clamp(nOut.y, -1.0, 1.0) * 127.0)) & 255) << 8)
                            | (uint(int(round(clamp(nOut.z, -1.0, 1.0) * 127.0)) & 255) << 16);
                    outData[o + 5u] = uintBitsToFloat(nSlim);
                    return;
                }

                // Main 56-byte dense layout, 14 words per vertex - the write indices
                // below MUST stay in lockstep with OUT_STRIDE_BYTES / OUT_OFF_* and the
                // VAO attrib offsets on the Java side (a drift here writes the tangent
                // of vertex N over the position of vertex N+1: geometry corruption).
                // color@3, uv0@4..5, uv1@6, uv2@7, normal@8, entity@9, midtex@10..11,
                // tangent@12, midblock@13.
                //
                // Color: mesh color x per-instance tint (records @20..23) with a spatial
                // falloff (records @24..27). The gradient is evaluated on the MODEL-space
                // vertex position (pos, before the instance transform); the
                // mix(white, tint, smoothstep) semantics match block_lit_instanced.vsh
                // exactly. The 4UB channel clamps overbright (>1); glow brightness under
                // shader packs comes from uv2 (lightmap, see lightOverride).
                uint cRaw = (uOffColor >= 0) ? ldWord(mBase, uOffColor) : 0xFFFFFFFFu;
                // Float 11 is a fade MODE sentinel (1.0/0.9999/0.9998), not an alpha:
                // the distance ramp is a vsh feature the bake path does not apply, so
                // all modes must bake fully opaque (0.9999*255 = 254 darkened machine
                // pixels by 0.4% and fed gl_Color.a into pack AO/alpha thresholds).
                float fadeRaw = instData[iBase + INST_FADE_WORD];
                float fade = fadeRaw >= 0.999 ? 1.0 : clamp(fadeRaw, 0.0, 1.0);
                float tA = instData[iBase + INST_TINT_WORD + 3u]; // emission strength (heat 0..1), NOT alpha
                float gAxis = instData[iBase + INST_GRAD_WORD];
                vec3 tCol = vec3(instData[iBase + INST_TINT_WORD], instData[iBase + INST_TINT_WORD + 1u], instData[iBase + INST_TINT_WORD + 2u]);
                vec3 colMul;
                // Init 1.0: no gradient (gAxis < 0) means full-weight emission,
                // so uniformly heated parts keep their tA glow (matches
                // block_lit_instanced.vsh); the branch below overwrites gt.
                float gt = 1.0;
                // Fail-closed gate: axis must be finite and in [0, 2] (NaN fails
                // every comparison, so a zeroed/corrupt word disables the gradient
                // entirely, never divides by zero -> +Inf -> full tint).
                if (gAxis >= 0.0 && gAxis <= 2.0) {
                    float gCoord = gAxis < 0.5 ? pos.x : (gAxis < 1.5 ? pos.y : pos.z);
                    float denom = instData[iBase + INST_GRAD_WORD + 1u] - instData[iBase + INST_GRAD_WORD + 2u];
                    // Epsilon guard: zero-length gradient span must not produce Inf/NaN.
                    gt = clamp((gCoord - instData[iBase + INST_GRAD_WORD + 2u])
                             / (abs(denom) < 1e-4 ? 1e-4 : denom), 0.0, 1.0);
                    gt = gt * gt * (3.0 - 2.0 * gt);
                    colMul = mix(vec3(1.0), tCol, gt);
                } else {
                    colMul = tCol;
                }
                uint r8 = uint(clamp(float((cRaw >>  0) & 0xFFu) * colMul.r, 0.0, 255.0));
                uint g8 = uint(clamp(float((cRaw >>  8) & 0xFFu) * colMul.g, 0.0, 255.0));
                uint b8 = uint(clamp(float((cRaw >> 16) & 0xFFu) * colMul.b, 0.0, 255.0));
                uint a8 = uint(clamp(float((cRaw >> 24) & 0xFFu) * fade, 0.0, 255.0));
                outData[o + 3u] = uintBitsToFloat(r8 | (g8 << 8) | (b8 << 16) | (a8 << 24));

                // UV0 (2 floats), through the atlas remap rect (sprite-local
                // [0..1] skins -> atlas; identity rect on regular machines)
                outData[o + 4u] = s_uvRect.x + ldFloat(mBase, uOffUv0) * s_uvRect.z;
                outData[o + 5u] = s_uvRect.y + ldFloat(mBase, uOffUv0 + 4) * s_uvRect.w;

                // UV1 (overlay) — 2 shorts packed into one word
                outData[o + 6u] = (uOffUv1 >= 0) ? uintBitsToFloat(ldWord(mBase, uOffUv1)) : 0.0;

                // UV2 (lightmap) — 2 ushorts packed into one word
                uint uv2Bits;
                {
                    vec3 w = clamp((pos - s_bmin) / s_bsize, 0.0, 1.0);
                    vec2 x00 = mix(s_light[0], s_light[1], w.x);
                    vec2 x10 = mix(s_light[2], s_light[3], w.x);
                    vec2 x01 = mix(s_light[4], s_light[5], w.x);
                    vec2 x11 = mix(s_light[6], s_light[7], w.x);
                    vec2 y0 = mix(x00, x10, w.y);
                    vec2 y1 = mix(x01, x11, w.y);
                    vec2 lm = mix(y0, y1, w.z);
                    // Emission: push the lightmap toward fullbright by heat*gradient so
                    // the glow follows the tint (hot metal), not just lightOverride parts.
                    float emit = clamp(tA * gt, 0.0, 1.0);
                    lm = mix(lm, vec2(240.0), emit);
                    int bu = int(clamp(lm.x, 0.0, 240.0));
                    int sv = int(clamp(lm.y, 0.0, 240.0));
                    uv2Bits = uint(bu | (sv << 16));
                }
                outData[o + 7u] = uintBitsToFloat(uv2Bits);

                // Normal: packed 4 GLbytes
                uint nPacked = uint(int(round(clamp(nOut.x, -1.0, 1.0) * 127.0)) & 255)
                        | (uint(int(round(clamp(nOut.y, -1.0, 1.0) * 127.0)) & 255) << 8)
                        | (uint(int(round(clamp(nOut.z, -1.0, 1.0) * 127.0)) & 255) << 16)
                        | (nRaw & 0xFF000000u);
                outData[o + 8u] = uintBitsToFloat(nPacked);

                // iris_Entity — constant 0
                outData[o + 9u] = uintBitsToFloat(0u);

                // mc_midTexCoord — 2 floats, remapped alongside UV0 (vanilla
                // parity: both are atlas-space, so pack shaders comparing the
                // two stay consistent)
                float midU = (uOffMidTex >= 0) ? ldFloat(mBase, uOffMidTex) : 0.0;
                float midV = (uOffMidTex >= 0) ? ldFloat(mBase, uOffMidTex + 4) : 0.0;
                outData[o + 10u] = s_uvRect.x + midU * s_uvRect.z;
                outData[o + 11u] = s_uvRect.y + midV * s_uvRect.w;

                // at_tangent — 4 bytes packed into one word (word 12 = byte 48):
                // rotated tangent (world axes) with the original bitangent w
                // sign byte preserved.
                uint tPacked = 0u;
                if (uOffTangent >= 0) {
                    tPacked = uint(int(round(clamp(tOut.x, -1.0, 1.0) * 127.0)) & 255)
                            | (uint(int(round(clamp(tOut.y, -1.0, 1.0) * 127.0)) & 255) << 8)
                            | (uint(int(round(clamp(tOut.z, -1.0, 1.0) * 127.0)) & 255) << 16)
                            | (tRaw & 0xFF000000u);
                }
                outData[o + 12u] = uintBitsToFloat(tPacked);

                // at_midBlock (word 13 = byte 52, last of the 56) - mirrors the
                // Iris/Oculus TERRAIN extension attribute that our ENTITY-format
                // companion mesh does not carry. Three signed bytes = offset FROM
                // the vertex TO the center of the vertex's block (blockCenter -
                // vertex), in 1/64 block units, truncated toward zero and folded
                // into u8 two's complement - byte-for-byte ExtendedDataHelper.
                // computeMidBlock semantics from Oculus 1.20.1-1.8.0:
                // packMidBlock = (int)(component * 64.0) & 255, x=bits 0..7,
                // y=8..15, z=16..23, byte 3 unused. Because blockCenter =
                // floor(outPos) + 0.5, the offset always lands in (-0.5, 0.5], so
                // *64 fits a signed byte with no truncation artifacts. The block
                // association comes from the WORLD-axes vertex position (outPos,
                // matching the emitted vertex; the fractional part of outPos is
                // the position inside the world block since anchorOrigin is an
                // integer BlockPos). Per-instance by design — output slots are
                // already instance-major, so this stays cache-consistent; packs
                // divide by 64.0 themselves.
                vec3 blockCenter = floor(outPos) + 0.5;
                vec3 midOff = blockCenter - outPos;
                uint midBits = (uint(int(midOff.x * 64.0)) & 255u)
                             | ((uint(int(midOff.y * 64.0)) & 255u) << 8)
                             | ((uint(int(midOff.z * 64.0)) & 255u) << 16);
                outData[o + 13u] = uintBitsToFloat(midBits);
            }
            """;

    // ── Cull compute shader ────────────────────────────────────────────
    // pass 0: one invocation per instance — conservative record-space AABB
    //   (quat-rotated model bbox) vs the 6 frustum planes; visible indices
    //   compacted into ctl[16..] via atomicAdd.
    //   KNOWN LIMIT: the AABB covers only pos+quat and the record bbox - skeletal
    //   bone deformation escaping the bbox can pop at the frustum edge (bone meshes
    //   re-bake every frame anyway via boneEpoch, so only the cull footprint is off).
    // pass 2: single-invocation assembler — visible count -> the glDrawElementsIndirect
    //   command and the EBO-build dispatch triplet (each indirect consumer reads a
    //   full (x,y,z) triple — no region may overlap).
    // pass 1: one invocation per drawn EBO index — visible[slot]*vertCount + base[k].
    //
    // Control block (uint words): 0 visibleCount (atomic), 1..3 reserved,
    // 4..8 glDrawElementsIndirect command, 9..11 EBO dispatch triplet,
    // 12..15 pad, 16.. visible list.

    private static final String CULL_SOURCE = """
            #version 430 core
            // Instance record layout (float words) — injected at init by
            // injectRecordLayout from the InstancedStaticPartRenderer offset constants
            // (the literals below are only a fallback). Layout: pos @0, quat @3 (4
            // snorm16 halves packed in 2 words), bboxMin @5, bboxSize @8, fade @11.
            #define INST_STRIDE 32u
            #define INST_POS_WORD 0u
            #define INST_QUAT_WORD 3u
            #define INST_BMIN_WORD 5u
            #define INST_BSIZE_WORD 8u
            #define INST_FADE_WORD 11u
            layout(local_size_x = 64) in;

            layout(std430, binding = 0) readonly restrict buffer InstBuf { float instData[]; };
            layout(std430, binding = 1) restrict buffer CtlBuf { uint ctl[]; };
            layout(std430, binding = 2) readonly restrict buffer EboBase { uint baseIdx[]; };
            layout(std430, binding = 3) writeonly restrict buffer EboBuf { uint ebo[]; };
            // Pool command store (pass 2, GPU-count flush path): slot uCmdSlot of
            // the shared pass-pool command buffer - one DrawElementsIndirectCommand
            // per pending entry, consumed by glMultiDrawElementsIndirectCount.
            layout(std430, binding = 4) writeonly restrict buffer CmdBuf { uint cmdWords[]; };

            uniform int uPass;       // 0 = cull, 1 = EBO build, 2 = assemble indirect words
            uniform int uInstCount;
            uniform int uVertCount;
            uniform int uIdxCount;
            uniform int uInstStride; // Instance record float stride (32)
            uniform vec4 uPlane[6];  // (n.xyz, d), inside = positive side
            // Global-batch placement: EBO entries land at
            // uEboBase words into the pass EBO, absolute vertex indices
            // shifted by uVertBase (the mesh's range start in the pass output).
            uniform int uVertBase;
            uniform int uEboBase;
            // Pool command slot this cache's indirect command lands at (-1 = the
            // GPU-count flush path is off: no GL 4.6 capability).
            uniform int uCmdSlot;
            // 1 = pass 1 is an IDENTITY fill (every instance slot, flat 1D
            // dispatch - the no-cull EBO growth of expandPoolEbo);
            // 0 = the cull EBO build (visible list, 2D dispatch from the
            // assembler). Per-program state: every dispatch entry sets it.
            uniform int uIdentity;
            // Optional Hi-Z occlusion (uHiZMode 1; frustum-only when 0). uClipMat is
            // the same record-space->clip product the frustum planes were extracted
            // from (bakeAndDrawShadow / bakeMain). The pyramid is the shared
            // HiZDepthPyramid texture reduced in Forward-Z (max = farthest occluder).
            uniform int uHiZMode;
            uniform mat4 uClipMat;
            uniform vec2 uViewSize;      // pyramid source viewport (px)
            uniform int uPyramidLevels;
            layout(binding = 10) uniform sampler2D uDepthPyramid;

            vec3 quatRotate(vec4 q, vec3 v) {
                return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);
            }

            // snorm16 decode (plain ALU): the record packs the quat as 4 signed
            // 16-bit halves in 2 float words (low half first) - the CPU encoder is
            // InstancedStaticPartRenderer.packSnorm16, the VAO reads the same bytes
            // as GL_SHORT normalized.
            float snorm16ToFloat(uint h) {
                // int overload sign-extends the 16-bit field (mirrors the VAO's
                // GL_SHORT normalized fetch and HalfFloat-style software decode).
                return clamp(float(bitfieldExtract(int(h), 0, 16)) / 32767.0, -1.0, 1.0);
            }

            vec4 instQuat(uint base) {
                uint wxy = floatBitsToUint(instData[base + INST_QUAT_WORD]);
                uint wzw = floatBitsToUint(instData[base + INST_QUAT_WORD + 1u]);
                return vec4(snorm16ToFloat(wxy), snorm16ToFloat(wxy >> 16u),
                            snorm16ToFloat(wzw), snorm16ToFloat(wzw >> 16u));
            }

            // Conservative Hi-Z occluder depth for a screen-space AABB. Mirrors
            // nucleus_cull.comp hizOccluder() exactly: mip chosen from the pixel
            // extent, four texelFetches at the rect corners, MAX reduction
            // (Forward-Z: the pyramid stores the FARTHEST occluder; sky footprints
            // are filtered by the downsampler). A sub-pixel rect answers 1.0 =
            // VISIBLE (the pyramid cannot answer at this resolution - returning
            // near depth here culled legitimate distant instances in the MDI cull).
            float hizOccluder(vec4 aabb) {
                vec4 uv = clamp(vec4(min(aabb.xy, aabb.zw), max(aabb.xy, aabb.zw)), 0.0, 1.0);
                ivec4 rect = ivec4(uv * uViewSize.xyxy + vec4(0.5, 0.5, -0.5, -0.5));
                if (any(lessThan(rect.zw, rect.xy))) {
                    return 1.0;
                }
                rect = min(rect, ivec4(uViewSize.xyxy) - 1) >> 1;

                ivec2 extent = rect.zw - rect.xy;
                int level = max(findMSB(max(extent.x, extent.y)), 0);
                level += any(greaterThan((rect.zw >> level) - (rect.xy >> level), ivec2(1))) ? 1 : 0;
                level = min(level, uPyramidLevels - 1);

                ivec4 bounds = rect >> level;

                float depth01 = texelFetch(uDepthPyramid, bounds.xw, level).r;
                float depth11 = texelFetch(uDepthPyramid, bounds.zw, level).r;
                float depth10 = texelFetch(uDepthPyramid, bounds.zy, level).r;
                float depth00 = texelFetch(uDepthPyramid, bounds.xy, level).r;

                return max(max(depth00, depth01), max(depth10, depth11));
            }

            // Projects the 8 corners of the conservative record-space AABB through
            // uClipMat into a screen-space AABB + the box's nearest window depth.
            // Returns false when any corner is behind the near plane (w <= 0): the
            // box touches the camera plane, Hi-Z must not answer (visible).
            bool projectAabb(vec3 center, vec3 he, out vec4 aabb, out float minDepth) {
                aabb = vec4(1.0e9, 1.0e9, -1.0e9, -1.0e9);
                minDepth = 1.0e9;
                for (int i = 0; i < 8; i++) {
                    vec3 sign3 = vec3(i & 1, (i >> 1) & 1, (i >> 2) & 1);
                    vec3 p = center + mix(-he, he, sign3);
                    vec4 clip = uClipMat * vec4(p, 1.0);
                    if (clip.w <= 0.0) {
                        return false;
                    }
                    vec3 ndc = clip.xyz / clip.w;
                    aabb.xy = min(aabb.xy, ndc.xy);
                    aabb.zw = max(aabb.zw, ndc.xy);
                    // Forward-Z window depth (0 = near, 1 = far, standard GL depth
                    // range [0,1]) - the convention the pyramid is reduced in and the
                    // same hardwired Forward-Z NucleusGpuCuller.cull assumes. DH's
                    // reverse-Z LOD depth never appears in this source texture: it is
                    // re-encoded Forward-Z into the main buffer by DhDepthCopy.
                    minDepth = min(minDepth, ndc.z * 0.5 + 0.5);
                }
                aabb = aabb * 0.5 + 0.5; // NDC -> uv
                return true;
            }

            void main() {
                uint gid = gl_GlobalInvocationID.x;
                if (uPass == 0) {
                    if (gid >= uint(uInstCount)) return;
                    uint ib = gid * uint(uInstStride);
                    // FADE_MODE_INVISIBLE (-1.0): the instance must not cast or
                    // exist at all - drop it before the frustum planes so it never
                    // lands in the compacted draw list (the vanilla MDI path
                    // discards instFade <= 0 in the vsh; this is the bake mirror).
                    if (instData[ib + INST_FADE_WORD] < 0.0) return;
                    vec3 center = vec3(instData[ib + INST_POS_WORD], instData[ib + INST_POS_WORD + 1u], instData[ib + INST_POS_WORD + 2u]);
                    vec4 q = instQuat(ib);
                    vec3 bmin = vec3(instData[ib + INST_BMIN_WORD], instData[ib + INST_BMIN_WORD + 1u], instData[ib + INST_BMIN_WORD + 2u]);
                    vec3 ext = vec3(instData[ib + INST_BSIZE_WORD], instData[ib + INST_BSIZE_WORD + 1u], instData[ib + INST_BSIZE_WORD + 2u]) * 0.5;
                    // Conservative record-space AABB: rotate the model-space center and
                    // take the abs-rotation rows times the model-space half extent.
                    center += quatRotate(q, bmin + ext);
                    float xx = q.x * q.x, yy = q.y * q.y, zz = q.z * q.z;
                    float xy = q.x * q.y, xz = q.x * q.z, yz = q.y * q.z;
                    float wx = q.w * q.x, wy = q.w * q.y, wz = q.w * q.z;
                    vec3 he;
                    he.x = abs(1.0 - 2.0 * (yy + zz)) * ext.x + abs(2.0 * (xy - wz)) * ext.y + abs(2.0 * (xz + wy)) * ext.z;
                    he.y = abs(2.0 * (xy + wz)) * ext.x + abs(1.0 - 2.0 * (xx + zz)) * ext.y + abs(2.0 * (yz - wx)) * ext.z;
                    he.z = abs(2.0 * (xz - wy)) * ext.x + abs(2.0 * (yz + wx)) * ext.y + abs(1.0 - 2.0 * (xx + yy)) * ext.z;
                    for (int i = 0; i < 6; i++) {
                        vec3 n = uPlane[i].xyz;
                        float d = dot(n, center) + uPlane[i].w;
                        float r = dot(abs(n), he);
                        if (d + r < 0.0) return; // fully outside this plane
                    }
                    // Hi-Z occlusion phase (strictly optional, uHiZMode selects at
                    // dispatch time): culled only when the box's nearest depth is
                    // behind the stored occluder depth at EVERY sampled footprint
                    // texel - conservative, mirroring NucleusGpuCuller's
                    // depthSphere <= occluderDepth semantics.
                    if (uHiZMode == 1) {
                        vec4 aabb;
                        float minDepth;
                        if (projectAabb(center, he, aabb, minDepth)) {
                            if (minDepth >= hizOccluder(aabb)) {
                                atomicAdd(ctl[15u], 1u); // F3: culled-by-occlusion counter (pad word)
                                return;
                            }
                        }
                    }
                    uint slot = atomicAdd(ctl[0u], 1u);
                    ctl[16u + slot] = gid;
                } else if (uPass == 1) {
                    uint n;
                    uint k;
                    uint slot;
                    uint inst;
                    if (uIdentity != 0) {
                        // Identity fill: every slot is drawn, dispatched flat 1D
                        // (the 2D x=index-groups / y=slot layout below cannot
                        // address slot counts past the Y work-group limit).
                        n = uint(uInstCount);
                        k = gid % uint(uIdxCount);
                        slot = gid / uint(uIdxCount);
                        inst = slot;
                    } else {
                        n = ctl[0u];
                        k = gid;
                        slot = gl_GlobalInvocationID.y;
                        inst = ctl[16u + slot];
                    }
                    if (k >= uint(uIdxCount) || slot >= n) return;
                    uint outIdx = uint(uEboBase) + slot * uint(uIdxCount) + k;
                    ebo[outIdx] = uint(uVertBase) + inst * uint(uVertCount) + baseIdx[k];
                } else {
                    if (gid != 0u) return;
                    uint n = ctl[0u];
                    // glDrawElementsIndirect command (words 4..8)
                    ctl[4u] = uint(uIdxCount) * n; // indexCount
                    ctl[5u] = 1u;                  // instanceCount
                    ctl[6u] = 0u;                  // firstIndex
                    ctl[7u] = 0u;                  // baseVertex
                    ctl[8u] = 0u;                  // baseInstance
                    if (uCmdSlot >= 0) {
                        // Pool command for the GPU-count flush path: the flush
                        // draws this store with glMultiDrawElementsIndirectCount
                        // (drawcount = pending entry count, CPU-known; zero-index
                        // commands are legal no-ops), so indexCount carries the
                        // GPU-side visible count and no CPU readback is needed.
                        // firstIndex = the mesh's EBO range start; baseVertex = 0
                        // (output vertices already carry vertBase), same shape as
                        // the CPU-built command in flushPool.
                        uint w = uint(uCmdSlot) * 5u;
                        cmdWords[w + 0u] = uint(uIdxCount) * n;
                        cmdWords[w + 1u] = 1u;
                        cmdWords[w + 2u] = uint(uEboBase);
                        cmdWords[w + 3u] = 0u;
                        cmdWords[w + 4u] = 0u;
                    }
                    // EBO-build dispatch triplet (words 9..11) - the dispatcher
                    // reads a full (x,y,z) triple: X = index groups, Y = visible instance count, Z = 1.
                    ctl[9u]  = (uint(uIdxCount) + 63u) / 64u;
                    ctl[10u] = n;
                    ctl[11u] = 1u;
                }
            }
            """;
}
