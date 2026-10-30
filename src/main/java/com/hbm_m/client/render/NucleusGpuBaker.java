package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
import com.hbm_m.client.render.shader.GlShaderUtil;

import static com.hbm_m.client.render.BakeVramPoolManager.*;

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


    private static final int UBO_BINDING_BAKE_PARAMS = 0;
    static int bakeParamsUbo = -1;
    private static final java.nio.ByteBuffer UBO_BYTE_BUF = org.lwjgl.system.MemoryUtil.memAlloc(96);
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
    public static long cacheBudgetBytes() {
        return BakeVramPoolManager.cacheBudgetBytes();
    }


    private static boolean checked = false;
    private static boolean available = false;
    private static boolean logged = false;
    /** One-shot latch: a non-identity RenderSystem modelview seen at shadow flush time. */
    private static boolean foreignShadowMvLogged = false;
    private static int programId = -1;
    /** Latches an initialization failure (e.g. GLSL compile error): without it every flush would retry compilation and spam the log with stack traces. */
    private static boolean initFailed = false;

    /** Instance-granularity cull program (pass 0 cull / 1 EBO build / 2 assemble indirect words). */
    static int cullProgramId = -1;
    private static boolean cullInitFailed = false;
    static int cuPass, cuInstCount, cuVertCount, cuIdxCount, cuInstStride, cuPlanes;
    /** Global-batch placement uniforms: output vertBase + EBO word base (pool path). */
    static int cuVertBase, cuEboBase;
    /** Pool command-store slot for pass 2's GPU-written indirect command (-1 = path off). */
    static int cuCmdSlot;
    /** Pass 1 identity-fill flag: 1 = write EVERY instance slot (the expandPoolEbo
     * no-cull growth), 0 = write the visible list (cull EBO build). Uniforms are
     * per-program state, so every entry point sets it before dispatching. */
    static int cuIdentity;
    /** Optional Hi-Z occlusion and subpixel culling uniforms (see CULL_SOURCE pass 0). */
    static int cuHiZMode, cuClipMat, cuViewSize, cuMip0Size, cuPyramidLevels, cuSubpixelMinNdc, cuIsShadowPass;

    static final int BONE_PALETTE_SIZE = 64;
    static int boneSsbo = -1;
    private static boolean hasBoneMatrices = false;
    /** Bumped on every bone palette upload; meshes with a bone-id attribute fold it into their cache key. */
    private static long boneEpoch = 0L;


    public static final PassPool POOL_MAIN = BakeVramPoolManager.POOL_MAIN;
    public static final PassPool POOL_SHADOW = BakeVramPoolManager.POOL_SHADOW;

    public static PassPool pool(int mode) {
        return BakeVramPoolManager.pool(mode);
    }

    public static String poolName(PassPool pool) {
        return BakeVramPoolManager.poolName(pool);
    }

    public static int outStrideBytes(int mode) {
        return BakeVramPoolManager.outStrideBytes(mode);
    }

    public static int roundUpInstanceCapacity(int count) {
        return BakeVramPoolManager.roundUpInstanceCapacity(count);
    }

    public static final int INDIRECT_CMD_BYTES = BakeVramPoolManager.INDIRECT_CMD_BYTES;
    public static final int CULL_CMD_BINDING = BakeVramPoolManager.CULL_CMD_BINDING;

    private static final java.util.List<BakedMesh> BAKED = BakeVramPoolManager.BAKED;

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

    private static final class MainBakeQueueEntry {
        final BakedMesh baked;
        final FloatBuffer records;
        final int count;
        final Matrix4f proj;
        final Matrix4f viewRot;
        final float relCamX, relCamY, relCamZ;
        final long mutationEpoch;

        MainBakeQueueEntry(BakedMesh baked, FloatBuffer records, int count, Matrix4f proj,
                           Matrix4f viewRot, float relCamX, float relCamY, float relCamZ,
                           long mutationEpoch) {
            this.baked = baked;
            this.records = records;
            this.count = count;
            this.proj = proj;
            this.viewRot = viewRot;
            this.relCamX = relCamX;
            this.relCamY = relCamY;
            this.relCamZ = relCamZ;
            this.mutationEpoch = mutationEpoch;
        }
    }

    private static final java.util.ArrayList<PendingDraw> PENDING_MAIN = new java.util.ArrayList<>(64);
    private static final java.util.ArrayList<MainBakeQueueEntry> MAIN_BAKE_QUEUE = new java.util.ArrayList<>(64);
    private static final java.util.ArrayList<PendingDraw> PENDING_SHADOW = new java.util.ArrayList<>(64);

    /** F3 accessors: pool store realloc count of the last flush (per pass). */
    public static long poolMainReallocs() { return BakeVramPoolManager.poolMainReallocs(); }
    public static long poolShadowReallocs() { return BakeVramPoolManager.poolShadowReallocs(); }
    public static long poolMainShrinks() { return BakeVramPoolManager.poolMainShrinks(); }
    public static long poolShadowShrinks() { return BakeVramPoolManager.poolShadowShrinks(); }

    public static long poolAllocatedBytes(int mode) { return BakeVramPoolManager.poolAllocatedBytes(mode); }
    public static long poolUsedBytes(int mode) { return BakeVramPoolManager.poolUsedBytes(mode); }
    public static long cacheNonPoolAllocatedBytes() { return BakeVramPoolManager.cacheNonPoolAllocatedBytes(); }
    public static long cacheNonPoolUsedBytes() { return BakeVramPoolManager.cacheNonPoolUsedBytes(); }
    public static long allocatedGpuBytes() { return BakeVramPoolManager.allocatedGpuBytes(); }
    public static long usedGpuBytes() { return BakeVramPoolManager.usedGpuBytes(); }

    /** Discards pending collect batches (exception path in the present hook). */
    public static void discardPendingBatches() {
        MAIN_BAKE_QUEUE.clear();
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
    static boolean cullEnabled() {
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
        // mode == MODE_SHADOW already returned above (shadow stays frustum-only).
        if (hiZAttemptMainFrame == f) {
            return hiZMainActive ? 1 : 0;
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
            if (depthTex <= 0) {
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
                                "main", depthTex, w, h);
                    }
                } else {
                    disableBakeHiZForSession();
                }
            }
        } catch (Exception t) {
            disableBakeHiZForSession();
        }
        hiZAttemptMainFrame = f;
        hiZMainActive = active;
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
    static long countsStagingBytes = 0;
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
        if (IrisShadowBatchCollector.isAnchorSpaceShadowActive()) {
            IrisShadowBatchCollector.buildShadowModelView(SHADOW_MV, camX, camY, camZ);
        } else {
            SHADOW_MV.translation(-camX, -camY, -camZ);
        }
        Matrix4f worldToClip = new Matrix4f(shadowProj).mul(SHADOW_MV);
        float[] planes = new float[24];
        IrisBakeBridge.extractFrustumPlanes(worldToClip, planes);
        long frustumStamp = IrisBakeBridge.stampPlanes(planes);

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
            if (hasBoneMatrices) {
                ensureBoneBuffer();
            }
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
                boolean needShadowRelayout = false;
                for (int i = 0; i < entries.size(); i++) {
                    IrisShadowBatchCollector.Entry e = entries.get(i);
                    if (e.count <= 0) continue;
                    IrisCompanionMesh mesh = e.getCompanionMesh();
                    if (mesh == null || !mesh.isBuilt() || mesh.getMeshVertexCount() <= 0
                            || mesh.getMeshStrideBytes() <= 0
                            || mesh.getMeshIndices() == null || mesh.getMeshIndices().length == 0) {
                        continue;
                    }
                    BakedMesh baked = getOrCreate(mesh);
                    if (baked == null) continue;
                    ModeCache mc = baked.shadow;
                    mc.lastUseFrame = frameStamp();
                    if (!cacheAllocated(mc)) {
                        if (preallocateCacheBuffers(baked, mc, e.count, MODE_SHADOW)) {
                            needShadowRelayout = true;
                        }
                    } else {
                        boolean grew = false;
                        if (e.count > mc.outInstances) {
                            mc.outInstances = roundUpInstanceCapacity(e.count);
                            grew = true;
                        }
                        if (e.count > mc.eboInstances) {
                            mc.eboInstances = roundUpInstanceCapacity(e.count);
                            grew = true;
                        }
                        if (grew) {
                            invalidateBakedOutput(mc);
                            needShadowRelayout = true;
                        }
                    }
                }
                if (needShadowRelayout) {
                    relayoutPool(MODE_SHADOW);
                }
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
                        FloatBuffer records = e.activeBuffer().duplicate();
                        records.flip();
                        if (!bakeEntry(baked, records, e.count, MODE_SHADOW, planes, frustumStamp, worldToClip, e.mutationEpoch)) {
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
                    matricesOk = IrisBakeBridge.setMatrices(shader, shadowProj, SHADOW_MV);
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

                boolean flushSucceeded = matricesOk && (issuedCommands > 0 || issuedCommands == -1);

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
                if (flushSucceeded && bakeFailed != null) {
                    IrisShadowBatchCollector.redrawFailedBakeEntries(
                            bakeFailed, shadowProj, camX, camY, camZ);
                }

                if (!flushSucceeded) {
                    String reason = !matricesOk
                            ? "unresolved matrices (ModelViewMat/ProjMat not found on the pack shadow program)"
                            : (anyCollected
                                    ? "pool flush evaluated 0 valid entries"
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

    public static final int MODE_SHADOW = 0;
    public static final int MODE_MAIN = 1;
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
        return bakeMain(records, count, mesh, packShader, proj, viewRot, relCamX, relCamY, relCamZ, -1L);
    }

    public static boolean bakeMain(FloatBuffer records, int count, IrisCompanionMesh mesh,
                                   ShaderInstance packShader, Matrix4f proj, Matrix4f viewRot,
                                   float relCamX, float relCamY, float relCamZ, long mutationEpoch) {
        if (!isEnabled() || !RenderSystem.isOnRenderThread() || packShader == null
                || mesh == null || !mesh.isBuilt() || count <= 0 || records == null) {
            return false;
        }
        if (mesh.getMeshVertexCount() <= 0 || mesh.getMeshStrideBytes() <= 0
                || mesh.getMeshIndices() == null || mesh.getMeshIndices().length == 0) {
            return false;
        }
        if (programId == -1 && !initProgram()) {
            return false;
        }
        BakedMesh baked = getOrCreate(mesh);
        if (baked == null) {
            return false;
        }
        MAIN_BAKE_QUEUE.add(new MainBakeQueueEntry(baked, records.duplicate(), count,
                new Matrix4f(proj), new Matrix4f(viewRot), relCamX, relCamY, relCamZ, mutationEpoch));
        return true;
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
                                     float[] planes, long frustumStamp, Matrix4f worldToClip,
                                     long mutationEpoch) {
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
            if (mutationEpoch > 0L && mc.sourceMutationEpoch == mutationEpoch && !mc.hasTrackedJoints) {
                dirtyCount = 0;
                recordsChanged = false;
                dirtyMode = false;
            } else {
                dirtyCount = BakeRecordDiffer.diffRecords(mc, records, count, mode);
                recordsChanged = dirtyCount > 0;
                dirtyMode = dirtyCount > 0 && dirtyCount < count;
            }
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
        if (dirtyMode && !ensureDirtyBuffer(mc, count)) {
            dirtyMode = false;
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
        mc.sourceMutationEpoch = mutationEpoch;
        mc.hasTrackedJoints = NucleusJointSpecs.getJointCount() > 0;
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
        return BakeVramPoolManager.cacheAllocated(mc);
    }

    private static boolean admissionBackoffActive(ModeCache mc) {
        return BakeVramPoolManager.admissionBackoffActive(mc);
    }

    private static void stampAdmissionRefusal(ModeCache mc) {
        BakeVramPoolManager.stampAdmissionRefusal(mc);
    }

    private static boolean poolRelayoutDeferred(int mode) {
        return BakeVramPoolManager.poolRelayoutDeferred(mode);
    }

    private static int meshBoneOffset(BakedMesh baked) {
        int offBoneId = baked.mesh.getMeshAttribByteOffsetOr("bone_id",
                baked.mesh.getMeshAttribByteOffsetOr("boneId", -1));
        if (offBoneId < 0 && baked.mesh.getMeshStrideBytes() == 36) {
            offBoneId = 32;
        }
        return offBoneId;
    }


    private static boolean runExpansion(BakedMesh baked, ModeCache mc, int count, int mode,
                                        boolean dirtyMode, int dirtyCount, boolean relayoutAllowed) {
        // Capacity was ensured by bakeEntry BEFORE the cull (a realloc there would
        // invalidate output/EBO state the cull below is about to build).
        if (hasBoneMatrices) {
            ensureBoneBuffer();
        }
        if (!cullEnabled()) {
            // No cull: CPU-expanded pool EBO range, full dispatch, full draw.
            if (!BakeVramPoolManager.expandPoolEbo(baked, mc, count, mode, relayoutAllowed)) {
                return false;
            }
        }
        if (dirtyMode) {
            // Span-level re-expansion: refresh the dirty-index list for THIS bake
            // (word 0 = count, words 1.. = instance indices) right before the
            // dispatch that reads it. Full re-bakes skip this entirely and run
            // identity mode, so they can never read stale list contents.
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
        // COMPUTE_SOURCE already guards < 0 offsets for color, normal, uv1, midTex, and tangent
        // (and uOffUv2 is not read by COMPUTE_SOURCE at all because UV2 is computed from s_light).
        // Only position and uv0 are strictly required.
        if (offPos < 0 || offUv0 < 0) {
            return false;
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
        UBO_INT_VIEW.put(Float.floatToRawIntBits(FrameViewState.animTicks()));
        UBO_INT_VIEW.put(Float.floatToRawIntBits(FrameViewState.animPartialTick()));
        UBO_INT_VIEW.flip();

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bakeParamsUbo);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0L, UBO_BYTE_BUF);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, UBO_BINDING_BAKE_PARAMS, bakeParamsUbo);

        // Save the pack's texture binding on SAMPLER_UNIT (GL_TEXTURE4) before binding uJointSpecs:
        // shaderpacks bind shadowtex/specular on unit 4 during tryApply(packShader), and leaving
        // uJointSpecs bound there projects a bright stripe perpendicular to the sun/moon in flushPool.
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + NucleusJointSpecs.SAMPLER_UNIT);
        int prevUnit4Texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        NucleusJointSpecs.bindSampler();

        // WAR synchronization with previous draw call: ensures shader storage and vertex attribute
        // arrays are synchronized before compute write. BUFFER_UPDATE covers record uploads
        // (glCopyBufferSubData via staging / glBufferSubData fallback) landing in SSBO 1.
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT
                | GL43.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
                | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, inputSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, recordSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, outputTargetSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, hasBoneMatrices && boneSsbo != -1 ? boneSsbo : 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 4, dirtyListMode ? dirtySsbo : 0);
        int numVertGroups = (baked.vertCount + LOCAL_SIZE - 1) / LOCAL_SIZE;
        int totalInst = dirtyListMode ? dirtyCount : count;
        int groupsY = Math.min(totalInst, 65535);
        int groupsZ = (totalInst + 65534) / 65535;
        GL43.glDispatchCompute(numVertGroups, groupsY, Math.max(groupsZ, 1));
        // Restore the pack's unit-4 texture binding before flushPool draws with packShader.
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + NucleusJointSpecs.SAMPLER_UNIT);
        GlStateManager._bindTexture(prevUnit4Texture);
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
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

        // Optional Hi-Z phase: build the pyramid BEFORE the cull program is bound
        // (regenerate switches programs itself) - at most once per frame per pass,
        // before the pass's first cull dispatch. 0 = frustum-only (current behavior).
        int prevUnit10Texture = 0;
        int hiZMode = 0;
        if (bakeHiZPlanned(mode)) {
            // Capture the PHYSICAL unit-10 binding BEFORE ensureBakeHiZ:
            // HiZDepthPyramid.regenerate's finally binds texture 0 on unit 10, so a
            // capture taken after the call would read 0 instead of the pack's texture.
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE10);
            prevUnit10Texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            hiZMode = ensureBakeHiZ(mode);
        }

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
        if (cuClipMat >= 0 && worldToClip != null) {
            worldToClip.get(CLIP_MAT_FLOATS);
            GL20.glUniformMatrix4fv(cuClipMat, false, CLIP_MAT_FLOATS);
        }
        if (cuIsShadowPass >= 0) {
            GL20.glUniform1i(cuIsShadowPass, mode == MODE_SHADOW ? 1 : 0);
        }
        if (cuSubpixelMinNdc >= 0) {
            int vh = Math.max(1, Minecraft.getInstance().getWindow().getHeight());
            float minNdc = (mode == MODE_SHADOW) ? (2.0f * 1.25f / 2048.0f) : (2.0f * 0.85f / vh);
            GL20.glUniform1f(cuSubpixelMinNdc, minNdc);
        }
        if (hiZMode == 1) {
            if (cuViewSize >= 0) {
                GL20.glUniform2f(cuViewSize, (float) hiZViewWidth, (float) hiZViewHeight);
            }
            if (cuMip0Size >= 0) {
                int mip0W = Math.max(1, Integer.highestOneBit(Math.max(2, hiZViewWidth) - 1));
                int mip0H = Math.max(1, Integer.highestOneBit(Math.max(2, hiZViewHeight) - 1));
                GL20.glUniform2f(cuMip0Size, (float) mip0W, (float) mip0H);
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
        return BakeVramPoolManager.ensurePoolCmdBuffer(pool, minSlots);
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
        return BakeRecordDiffer.ensureCacheRecords(mc, bytes);
    }

    private static boolean uploadRecords(FloatBuffer records, int count, int destSsbo) {
        return BakeRecordDiffer.uploadRecords(records, count, destSsbo);
    }

    private static boolean uploadDirtyRecords(FloatBuffer records, int count, ModeCache mc,
                                              int dirtyCount) {
        return BakeRecordDiffer.uploadDirtyRecords(records, count, mc, dirtyCount);
    }

    private static boolean ensureDirtyBuffer(ModeCache mc, int count) {
        return BakeRecordDiffer.ensureDirtyBuffer(mc, count);
    }

    private static void uploadDirtyList(ModeCache mc, int dirtyCount) {
        BakeRecordDiffer.uploadDirtyList(mc, dirtyCount);
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
        ensureShadersLoaded();
        try {
            String source = injectRecordLayout(COMPUTE_SOURCE, "COMPUTE_SOURCE",
                    "INST_STRIDE", InstancedStaticPartRenderer.INSTANCE_DATA_SIZE,
                    "INST_POS_WORD", InstancedStaticPartRenderer.POS_FLOAT_OFFSET,
                    "INST_QUAT_WORD", InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET,
                    "INST_BMIN_WORD", InstancedStaticPartRenderer.BBOX_MIN_FLOAT_OFFSET,
                    "INST_BSIZE_WORD", InstancedStaticPartRenderer.BBOX_SIZE_FLOAT_OFFSET,
                    "INST_FADE_WORD", InstancedStaticPartRenderer.FADE_FLOAT_OFFSET,
                    "INST_UVRECT_WORD", InstancedStaticPartRenderer.UVRECT_FLOAT_OFFSET,
                    "INST_TINT_WORD", InstancedStaticPartRenderer.TINT_FLOAT_OFFSET,
                    "INST_GRAD_WORD", InstancedStaticPartRenderer.GRAD_FLOAT_OFFSET,
                    "INST_ANIM_WORD", InstancedStaticPartRenderer.ANIM_FLOAT_OFFSET);
            int p = GlShaderUtil.createComputeProgram(source, "HBM_BakeExpand_CS");
            if (p == 0) {
                initFailed = true;
                return false;
            }
            programId = p;
            bakeParamsUbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(bakeParamsUbo, "HBM_BakeParams_UBO");
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bakeParamsUbo);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, 96L, GL15.GL_DYNAMIC_DRAW);
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
            initFailed = true;
            com.hbm_m.main.MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker init failed - Tier 1 disabled", t);
            return false;
        }
    }

    private static void initCullProgram() {
        if (cullInitFailed || !CULL_SWITCH) {
            return;
        }
        ensureShadersLoaded();
        try {
            String source = injectRecordLayout(CULL_SOURCE, "CULL_SOURCE",
                    "INST_STRIDE", InstancedStaticPartRenderer.INSTANCE_DATA_SIZE,
                    "INST_POS_WORD", InstancedStaticPartRenderer.POS_FLOAT_OFFSET,
                    "INST_QUAT_WORD", InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET,
                    "INST_BMIN_WORD", InstancedStaticPartRenderer.BBOX_MIN_FLOAT_OFFSET,
                    "INST_BSIZE_WORD", InstancedStaticPartRenderer.BBOX_SIZE_FLOAT_OFFSET,
                    "INST_FADE_WORD", InstancedStaticPartRenderer.FADE_FLOAT_OFFSET,
                    "INST_GRAD_WORD", InstancedStaticPartRenderer.GRAD_FLOAT_OFFSET);
            int p = GlShaderUtil.createComputeProgram(source, "HBM_BakeCull_CS");
            if (p == 0) {
                cullInitFailed = true;
                return;
            }
            cullProgramId = p;
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
            cuMip0Size = GL20.glGetUniformLocation(p, "uMip0Size");
            cuPyramidLevels = GL20.glGetUniformLocation(p, "uPyramidLevels");
            cuSubpixelMinNdc = GL20.glGetUniformLocation(p, "uSubpixelMinNdc");
            cuIsShadowPass = GL20.glGetUniformLocation(p, "uIsShadowPass");
        } catch (Exception t) {
            cullInitFailed = true;
            cullProgramId = -1;
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker cull program failed - running without cull", t);
        }
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
     * @return true when BOTH the model-view and projection uniforms were resolved and
     *         uploaded. False = the active program declares neither ModelViewMat nor
     *         iris_ModelViewMat, or neither ProjMat nor iris_ProjMat. Callers on the
     *         shadow path MUST treat false as a Tier-1 refusal (the shadow records are
     *         world+camera spaced - drawing with a missing model-view sends the geometry
     *         off-map with no GL error). Main-pass callers may ignore the result (their
     *         programs always declare the vanilla uniforms).
     */
    
    
    /**
     * Gribb–Hartmann frustum plane extraction for a column-major clip matrix.
     * Delegates to centralized CpuFrustumCuller.extractPlanes.
     */
    
    /** Stable stamp of the 6 frustum planes (any camera motion changes it). */
    
    // ── Cache allocation, budget, eviction ─────────────────────────────

    /**
     * Pre-allocates per-cache buffers (recordSsbo, ctlSsbo, dirtySsbo, recordCopy, dirtyIdx)
     * and sets outInstances and eboInstances WITHOUT calling relayoutPool.
     * This is used by the capacity pre-pass in bakeAndDrawShadow and presentMainBatch.
     */
    private static boolean preallocateCacheBuffers(BakedMesh baked, ModeCache mc, int count, int mode) {
        return BakeVramPoolManager.preallocateCacheBuffers(baked, mc, count, mode);
    }

    private static boolean allocateCache(BakedMesh baked, ModeCache mc, int count, int mode) {
        return BakeVramPoolManager.allocateCache(baked, mc, count, mode);
    }

    private static void evictStaleCaches() {
        BakeVramPoolManager.evictStaleCaches();
    }

    private static void releaseCacheIfStale(ModeCache mc, long now) {
        BakeVramPoolManager.releaseCacheIfStale(mc, now);
    }

    private static void releaseBakedMesh(BakedMesh bm) {
        BakeVramPoolManager.releaseBakedMesh(bm);
    }

    private static void freeModeCache(ModeCache mc) {
        BakeVramPoolManager.freeModeCache(mc);
    }

    private static boolean ensureOutputCapacity(BakedMesh baked, ModeCache mc, int count, int mode,
                                                boolean relayoutAllowed) {
        return BakeVramPoolManager.ensureOutputCapacity(baked, mc, count, mode, relayoutAllowed);
    }

    private static boolean expandPoolEbo(BakedMesh baked, ModeCache mc, int count, int mode,
                                         boolean relayoutAllowed) {
        return BakeVramPoolManager.expandPoolEbo(baked, mc, count, mode, relayoutAllowed);
    }

    private static boolean relayoutPool(int mode) {
        return BakeVramPoolManager.relayoutPool(mode);
    }

    private static long poolStoreTarget(long needed) {
        return BakeVramPoolManager.poolStoreTarget(needed);
    }

    private static void invalidateBakedOutput(ModeCache mc) {
        BakeVramPoolManager.invalidateBakedOutput(mc);
    }

    private static boolean ensurePoolStorage(PassPool pool, long outNeeded, long eboNeeded,
                                            long validOutCopyBytes, long validEboCopyBytes) {
        return BakeVramPoolManager.ensurePoolStorage(pool, outNeeded, eboNeeded, validOutCopyBytes, validEboCopyBytes);
    }

    private static void maybeShrinkPool(PassPool pool, int mode) {
        BakeVramPoolManager.maybeShrinkPool(pool, mode);
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
        if (ensurePoolCmdBuffer(pool, n) == -1 || pool.vao == -1) {
            return false;
        }
        long instances = 0L;
        boolean anyRepaired = false;
        for (int i = 0; i < n; i++) {
            PendingDraw pd = pending.get(i);
            ModeCache mc = pd.cache;
            if (!mc.outputValid || !mc.cullValid) {
                return false;
            }
            if (mc.cmdGenStamp != pool.cmdGen || mc.cmdSlotWritten != i
                    || i >= pool.cmdSlotOwners.length || pool.cmdSlotOwners[i] != mc) {
                anyRepaired = true;
                GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, mc.ctlSsbo);
                GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, pool.cmdSsbo);
                GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER,
                        DRAW_CMD_OFFSET, (long) i * INDIRECT_CMD_BYTES, INDIRECT_CMD_BYTES);
                mc.cmdGenStamp = pool.cmdGen;
                mc.cmdSlotWritten = i;
                pool.cmdSlotOwners[i] = mc;
            }
            instances += mc.countsDirty ? pd.instanceCount : (int) mc.lastVisibleCount;
            mc.countsDirty = false;
        }
        if (anyRepaired) {
            GL42.glMemoryBarrier(GL42.GL_COMMAND_BARRIER_BIT);
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
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
            IrisBakeBridge.bindSlimShadowAttributes(packShader, pool.outputSsbo);
        } else if (mode == MODE_MAIN) {
            // One bindIrisExtendedAttributes call for the WHOLE multi-draw: the
            // locations are pack-program-dependent and the pointers snapshot
            // GL_ARRAY_BUFFER, so the pool output is bound as the source first.
            IrisBakeBridge.bindIrisExtendedAttributes(packShader, pool.outputSsbo, OUT_STRIDE_BYTES,
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
     *         -1 means valid entries were evaluated and all were legitimately
     *         culled to 0 visible instances (a successful no-op flush), while 0
     *         means no valid entries could be evaluated.
     *         The shadow caller uses this for honest Tier-1 success reporting:
     *         0 means nothing was rasterized and the frame must fall back to
     *         Tier 2/3. Main-pass callers ignore the value.
     */
    private static int flushPool(PassPool pool, int mode, ShaderInstance packShader,
                                  java.util.ArrayList<PendingDraw> pending) {
        if (flushPoolGpuCounts(pool, mode, packShader, pending)) {
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
        int validEvaluated = 0;
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
            validEvaluated++;
            if (indexCount <= 0) {
                continue; // nothing visible: compacted out CPU-side (no no-op command)
            }
            // DrawElementsIndirectCommand: indexCount, instanceCount, firstIndex,
            // baseVertex, baseInstance.
            buf.putInt(indexCount).putInt(1).putInt(pd.baked.eboWordBase[mode]).putInt(0).putInt(0);
            commands++;
        }
        if (commands == 0) {
            return validEvaluated > 0 ? -1 : 0;
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
            IrisBakeBridge.bindSlimShadowAttributes(packShader, pool.outputSsbo);
        } else if (mode == MODE_MAIN) {
            // One bindIrisExtendedAttributes call for the WHOLE multi-draw: the
            // locations are pack-program-dependent and the pointers snapshot
            // GL_ARRAY_BUFFER, so the pool output is bound as the source first.
            IrisBakeBridge.bindIrisExtendedAttributes(packShader, pool.outputSsbo, OUT_STRIDE_BYTES,
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
            synchronized (BakeVramPoolManager.class) {
                if (BakeVramPoolManager.getCacheBytes() + delta > cacheBudgetBytes()) {
                    evictStaleCaches();
                }
                BakeVramPoolManager.adjustCacheBytes(delta);
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
        if (MAIN_BAKE_QUEUE.isEmpty() && PENDING_MAIN.isEmpty()) {
            return;
        }
        if (!isEnabled() || !RenderSystem.isOnRenderThread()) {
            MAIN_BAKE_QUEUE.clear();
            PENDING_MAIN.clear();
            return;
        }
        ShaderInstance packShader = com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                .getBlockShader(false);
        if (packShader == null) {
            MAIN_BAKE_QUEUE.clear();
            PENDING_MAIN.clear();
            return;
        }
        bakeGlDrainForeign("main multi-draw entry");
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        // *BINDING query pnames - see the note in bakeAndDrawShadow.
        int prevDrawIndirect = GL11.glGetInteger(GL40.GL_DRAW_INDIRECT_BUFFER_BINDING);
        int prevDispatchIndirect = GL11.glGetInteger(GL43.GL_DISPATCH_INDIRECT_BUFFER_BINDING);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousBlockEntityId = com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                .setCurrentRenderedBlockEntity(0);
        try (com.hbm_m.client.render.shader.IrisPhaseGuard guard =
                     com.hbm_m.client.render.shader.IrisPhaseGuard.pushBlockEntities()) {
            // Apply packShader BEFORE the MAIN_BAKE_QUEUE loop so the pack's gbuffer FBO
            // is bound when ensureBakeHiZ(MODE_MAIN) queries GL_DRAW_FRAMEBUFFER depth.
            if (!com.hbm_m.client.render.shader.ShaderBindResync.tryApply(packShader)) {
                com.hbm_m.client.render.shader.IrisExtendedShaderAccess.invalidateShaderCache();
                com.hbm_m.client.render.shader.IrisRenderBatch.invalidateCaches();
                packShader = com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                        .getBlockShader(false);
                if (packShader == null
                        || !com.hbm_m.client.render.shader.ShaderBindResync.tryApply(packShader)) {
                    return;
                }
            }

            if (!MAIN_BAKE_QUEUE.isEmpty()) {
                // Capacity pre-pass over MAIN_BAKE_QUEUE
                boolean needMainRelayout = false;
                for (int i = 0; i < MAIN_BAKE_QUEUE.size(); i++) {
                    MainBakeQueueEntry entry = MAIN_BAKE_QUEUE.get(i);
                    ModeCache mc = entry.baked.main;
                    mc.lastUseFrame = frameStamp();
                    if (!cacheAllocated(mc)) {
                        if (preallocateCacheBuffers(entry.baked, mc, entry.count, MODE_MAIN)) {
                            needMainRelayout = true;
                        }
                    } else {
                        boolean grew = false;
                        if (entry.count > mc.outInstances) {
                            mc.outInstances = roundUpInstanceCapacity(entry.count);
                            grew = true;
                        }
                        if (entry.count > mc.eboInstances) {
                            mc.eboInstances = roundUpInstanceCapacity(entry.count);
                            grew = true;
                        }
                        if (grew) {
                            invalidateBakedOutput(mc);
                            needMainRelayout = true;
                        }
                    }
                }
                if (needMainRelayout) {
                    relayoutPool(MODE_MAIN);
                }

                MainBakeQueueEntry first = MAIN_BAKE_QUEUE.get(0);
                Matrix4f effProj = first.proj != null ? first.proj : projection;
                MAIN_MV.set(first.viewRot).translate(-first.relCamX, -first.relCamY, -first.relCamZ);
                float[] planes = new float[24];
                Matrix4f worldToClip = new Matrix4f(effProj).mul(MAIN_MV);
                IrisBakeBridge.extractFrustumPlanes(worldToClip, planes);
                long frustumStamp = IrisBakeBridge.stampPlanes(planes);

                if (hasBoneMatrices) {
                    ensureBoneBuffer();
                }

                for (int i = 0; i < MAIN_BAKE_QUEUE.size(); i++) {
                    MainBakeQueueEntry item = MAIN_BAKE_QUEUE.get(i);
                    boolean success;
                    if (GlDebugMarkers.ENABLED) {
                        GlDebugMarkers.pushGroup("HBM_BakeMain_" + item.baked.mesh.getDebugName());
                    }
                    try {
                        success = bakeEntry(item.baked, item.records, item.count, MODE_MAIN,
                                planes, frustumStamp, worldToClip, item.mutationEpoch);
                    } finally {
                        if (GlDebugMarkers.ENABLED) {
                            GlDebugMarkers.popGroup();
                        }
                    }
                    if (success) {
                        item.baked.mesh.noteBakeSuccess();
                        PENDING_MAIN.add(new PendingDraw(item.baked, item.baked.main, item.count));
                    }
                }
            }

            if (PENDING_MAIN.isEmpty()) {
                packShader.clear();
                return;
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
                IrisBakeBridge.setMatrices(packShader, projection, MAIN_MV);
                // Explicit unit-0 atlas bind (the bake path never relies on
                // setShaderTexture alone).
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
                RenderSystem.bindTexture(Minecraft.getInstance().getTextureManager()
                        .getTexture(TextureAtlas.LOCATION_BLOCKS).getId());
                if (GlDebugMarkers.ENABLED) {
                    GlDebugMarkers.pushGroup("HBM_PresentMainBatch");
                }
                try {
                    flushPool(POOL_MAIN, MODE_MAIN, packShader, PENDING_MAIN);
                } finally {
                    if (GlDebugMarkers.ENABLED) {
                        GlDebugMarkers.popGroup();
                    }
                    // Restore the pack shader's matrices from RenderSystem so the
                    // iris_ModelViewMat uniform no longer holds our -relCam matrix.
                    // setMatrices uploads via raw glUniformMatrix4fv, bypassing the
                    // RenderSystem cache - without this restore a later hand/weapon
                    // draw whose cached RenderSystem matrix is unchanged would skip
                    // its re-upload and render with our matrix (disappears).
                    IrisBakeBridge.setMatrices(packShader, RenderSystem.getProjectionMatrix(),
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
            MAIN_BAKE_QUEUE.clear();
            PENDING_MAIN.clear();
            com.hbm_m.client.render.shader.IrisExtendedShaderAccess
                    .restoreCurrentRenderedBlockEntity(previousBlockEntityId);
            com.hbm_m.client.render.shader.ShaderBindResync.markForeignProgramBind();
            GL40.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, prevDrawIndirect);
            GL43.glBindBuffer(GL43.GL_DISPATCH_INDIRECT_BUFFER, prevDispatchIndirect);
            com.mojang.blaze3d.platform.GlStateManager._glUseProgram(previousProgram);
            // Force the next vanilla immediate draw to rebind its VAO honestly
            // (mirrors MdiBatchCoordinator's exit-to-foreign-GL pattern).
            com.mojang.blaze3d.vertex.BufferUploader.invalidate();
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
    }

    public static void tickFrameCleanup() {
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }
        long now = frameStamp();
        boolean shadowChanged = false;
        boolean mainChanged = false;
        synchronized (NucleusGpuBaker.class) {
            for (int i = BAKED.size() - 1; i >= 0; i--) {
                BakedMesh bm = BAKED.get(i);
                if (!bm.mesh.isBuilt()) {
                    shadowChanged |= cacheAllocated(bm.shadow);
                    mainChanged |= cacheAllocated(bm.main);
                    releaseBakedMesh(bm);
                    BAKED.remove(i);
                    continue;
                }
                if (cacheAllocated(bm.shadow) && now - bm.shadow.lastUseFrame > LRU_FRAME_LIMIT) {
                    freeModeCache(bm.shadow);
                    shadowChanged = true;
                } else if (cacheAllocated(bm.shadow) && bm.shadow.recordCount > 0
                        && bm.shadow.outInstances > 256 && bm.shadow.recordCount * 4 < bm.shadow.outInstances) {
                    int target = roundUpInstanceCapacity(bm.shadow.recordCount);
                    bm.shadow.outInstances = target;
                    bm.shadow.eboInstances = target;
                    shadowChanged = true;
                }
                if (cacheAllocated(bm.main) && now - bm.main.lastUseFrame > LRU_FRAME_LIMIT) {
                    freeModeCache(bm.main);
                    mainChanged = true;
                } else if (cacheAllocated(bm.main) && bm.main.recordCount > 0
                        && bm.main.outInstances > 256 && bm.main.recordCount * 4 < bm.main.outInstances) {
                    int target = roundUpInstanceCapacity(bm.main.recordCount);
                    bm.main.outInstances = target;
                    bm.main.eboInstances = target;
                    mainChanged = true;
                }
            }
        }
        if (shadowChanged) {
            relayoutPool(MODE_SHADOW);
        }
        if (mainChanged) {
            relayoutPool(MODE_MAIN);
        }
        maybeShrinkPool(POOL_SHADOW, MODE_SHADOW);
        maybeShrinkPool(POOL_MAIN, MODE_MAIN);
    }

    private static BakedMesh getOrCreate(IrisCompanionMesh mesh) {
        synchronized (NucleusGpuBaker.class) {
            for (int i = 0; i < BAKED.size(); i++) {
                if (BAKED.get(i).mesh == mesh) {
                    return BAKED.get(i);
                }
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
            synchronized (NucleusGpuBaker.class) {
                BAKED.add(baked);
            }
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
        return BakeVramPoolManager.createOutputVaoWithEbo(vbo, ebo, slim);
    }

    /** Diagnostics tracking for skipped bake entries (prevents log spam). */
    private static final java.util.HashSet<Integer> SKIP_LOGGED = new java.util.HashSet<>();


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
    public static long shadowDeclineCount() {
        return IrisBakeBridge.shadowDeclineCount();
    }

    private static void logShadowDecline(String reason, ShaderInstance shader) {
        IrisBakeBridge.logShadowDecline(reason, shader);
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
        int clampedCount = Math.min(count, BONE_PALETTE_SIZE);
        int floatsNeeded = clampedCount * 16;
        if (matrices.remaining() < floatsNeeded) {
            hasBoneMatrices = false;
            return;
        }
        boneEpoch++;
        ensureBoneBuffer();
        FloatBuffer slice = matrices.slice();
        slice.limit(floatsNeeded);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, boneSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, slice);
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
            synchronized (BakeVramPoolManager.class) {
                if (BakeVramPoolManager.getCacheBytes() + delta > cacheBudgetBytes()) {
                    evictStaleCaches();
                }
                BakeVramPoolManager.adjustCacheBytes(delta);
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
        synchronized (NucleusGpuBaker.class) {
            for (BakedMesh bm : BAKED) {
                releaseBakedMesh(bm);
            }
            BAKED.clear();
            BakeVramPoolManager.resetCacheBytes();
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
        MAIN_BAKE_QUEUE.clear();
        PENDING_MAIN.clear();
        PENDING_SHADOW.clear();
        // Session-wide diagnostic sets (identity hash codes of renderers/mode
        // caches): dead after a teardown, grew unbounded for the whole session
        // until now - covers the lifecycle hook and this crash path alike.
        SKIP_LOGGED.clear();
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

            foreignShadowMvLogged = false;
            dataFailLogMs = 0L;
        }
    }

    private static void releasePool(PassPool pool) {
        BakeVramPoolManager.releasePool(pool);
    }


    private static String COMPUTE_SOURCE;
    private static String CULL_SOURCE;

    private static void ensureShadersLoaded() {
        if (COMPUTE_SOURCE == null) {
            COMPUTE_SOURCE = GlShaderUtil.loadResource(NucleusGpuBaker.class, "/assets/hbm_m/shaders/bake/bake_expand.comp");
        }
        if (CULL_SOURCE == null) {
            CULL_SOURCE = GlShaderUtil.loadResource(NucleusGpuBaker.class, "/assets/hbm_m/shaders/bake/bake_cull.comp");
        }
    }
}
