package com.hbm_m.client.render.machine;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.IrisShadowBatchCollector;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * BER produced by the {@link MachineRenderers} factory. Contains the entire
 * common pipeline (culling / fade / Iris batching / path degradation); only the
 * spec is machine-specific: parts + animators + hooks.
 */
@OnlyIn(Dist.CLIENT)
public final class MachineBer<T extends BlockEntity> extends AbstractPartBasedRenderer<T, BakedModel> {

    private final MachineSpec<T> spec;

    // -- Animation delta cache (Step 1: the animator runs once per frame) --
    //
    // A part's pose depends only on (BE state, gameTime, partialTick), and the
    // shadow and main passes of one frame receive IDENTICAL values of all three -
    // the lambdas (lerp+trig+translate chains, ~10-12% CPU on assembler farms per
    // the profile) run on the first pass; the second reuses the ready delta. The
    // DELTA S^-1*F is cached, not the final matrix: the pass bases differ (shadow
    // is shadowMV, main is R_cam*T(be-cam)); on a hit F' = S*delta - one 4x4 mul.
    // Animation delta cache: config (nucleusAnimCache) is the primary source, the
    // JVM flag -Dhbm.animCache=false is an emergency override. Read once per
    // renderAll - live toggling from the config screen without a restart.
    private static final boolean ANIM_CACHE_FLAG =
            !"false".equalsIgnoreCase(System.getProperty("hbm.animCache", "true"));
    private static boolean animCacheOn() {
        return com.hbm_m.config.ModClothConfig.get().nucleusAnimCache && ANIM_CACHE_FLAG;
    }
    private static final int ANIM_SLOTS = 4096;
    private static final int ANIM_SLOT_MASK = ANIM_SLOTS - 1;
    private final long[] animPosKey = new long[ANIM_SLOTS];
    private final long[] animTimeKey = new long[ANIM_SLOTS];
    private final Matrix4f[][] animDelta = new Matrix4f[ANIM_SLOTS][];
    private final boolean[][] animSkip = new boolean[ANIM_SLOTS][];
    // Scratch (rendering is single-threaded): S/Sinv of the current BE for the
    // duration of the parts pass.
    private final Matrix4f animBase = new Matrix4f();
    private final Matrix4f animBaseInv = new Matrix4f();
    private final Matrix4f animComposed = new Matrix4f();

    private void animStore(int slot, int partIdx, int partCount, long posKey, long timeKey,
                           Matrix4f baseInv, Matrix4f finalPose, boolean draw) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) STORE_SHADOW++;
        else STORE_MAIN++;
        if (animDelta[slot] == null || animDelta[slot].length != partCount) {
            animDelta[slot] = new Matrix4f[partCount];
            animSkip[slot] = new boolean[partCount];
        }
        if (animDelta[slot][partIdx] == null) {
            animDelta[slot][partIdx] = new Matrix4f();
        }
        // Misses happen on the first pass of every BE of every frame (and after
        // the timeKey changes on a tick) - alloc-free: matrices are reused.
        animDelta[slot][partIdx].set(baseInv).mul(finalPose);
        animSkip[slot][partIdx] = !draw;
        animPosKey[slot] = posKey;
        animTimeKey[slot] = timeKey;
    }

    private void animInvalidate(int slot) {
        if (animDelta[slot] != null) {
            java.util.Arrays.fill(animDelta[slot], null);
        }
    }

    // Hit/miss counters of the animation cache (diagnosing "cache not hitting",
    // profile 0914).
    private static volatile long ANIM_HITS = 0;
    private static volatile long ANIM_MISSES = 0;
    private static volatile long MISS_OFF = 0, MISS_NO_SLOT = 0, MISS_POS = 0, MISS_FRAME = 0, MISS_DELTA = 0;
    // Split by pass: where we write and where we lose (log 0914 03:31: delta misses
    // 90% with live keys - the writes are missing; need to know WHOSE store never landed).
    private static volatile long STORE_SHADOW = 0, STORE_MAIN = 0;
    private static volatile long DELTA_MISS_SHADOW = 0, DELTA_MISS_MAIN = 0;
    /** Config debugAnimCacheLog; the JVM flag -Dhbm.debugAnimCache forces ON for dev runs. */
    private static final boolean DEBUG_ANIM_CACHE_FLAG = Boolean.getBoolean("hbm.debugAnimCache");
    private static boolean debugAnimCacheOn() {
        return DEBUG_ANIM_CACHE_FLAG || com.hbm_m.config.ModClothConfig.get().debugAnimCacheLog;
    }
    private static volatile long ANIM_LAST_LOG = 0L;

    private static void maybeLogAnimCache() {
        long now = System.currentTimeMillis();
        if (now - ANIM_LAST_LOG > 5000L) {
            ANIM_LAST_LOG = now;
            // Animation cache diagnostics - only via the explicit flag
            // -Dhbm.debugAnimCache=true (the 0914 investigation is closed; an
            // autobrief every 5s under shadows pollutes the log).
            if (debugAnimCacheOn()) {
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] anim cache: hits={} misses={} ({}% hit) | off={} noSlot={} pos={} frame={} "
                                + "delta={} (deltaShadow={} deltaMain={}) | stores: shadow={} main={}",
                        ANIM_HITS, ANIM_MISSES,
                        (ANIM_HITS + ANIM_MISSES) == 0 ? 0
                                : ANIM_HITS * 100 / (ANIM_HITS + ANIM_MISSES),
                        MISS_OFF, MISS_NO_SLOT, MISS_POS, MISS_FRAME, MISS_DELTA,
                        DELTA_MISS_SHADOW, DELTA_MISS_MAIN, STORE_SHADOW, STORE_MAIN);
            }
            ANIM_HITS = 0;
            ANIM_MISSES = 0;
            MISS_OFF = 0;
            MISS_NO_SLOT = 0;
            MISS_POS = 0;
            MISS_FRAME = 0;
            MISS_DELTA = 0;
            STORE_SHADOW = 0;
            STORE_MAIN = 0;
            DELTA_MISS_SHADOW = 0;
            DELTA_MISS_MAIN = 0;
        }
    }

    // Shared light: one 8-corner sample per machine per frame (instead of per part).
    private final float[] sharedLight8 = new float[16];
    private final float[] sharedLightBbox = new float[6];
    private final Matrix4f sharedLightPose = new Matrix4f();

    // Reusable per-frame context for hooks (rendering is single-threaded).
    private final FrameCtx frameCtx = new FrameCtx();

    // -- Per-frame cache (bbox, distance): one computation per (machine, frame) --
    // MachineBer is one instance per type - the key is identity BE + a per-frame
    // counter (shared between shadow/main: the second pass takes the first's
    // cache). Previously getRenderBoundingBox was computed 2x per frame (frustum +
    // shared light) and distanceSqToCamera 3-4x (fastpath, fade, LOD) with a
    // camera fetch every time.
    private BlockEntity frameBoundsBE;
    private long frameBoundsStamp = -1L;
    private net.minecraft.world.phys.AABB frameBounds;
    private BlockEntity frameDistBE;
    private long frameDistStamp = -1L;
    private double frameDistSq;

    private net.minecraft.world.phys.AABB frameBounds(BlockEntity be) {
        long frame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (frameBoundsBE != be || frameBoundsStamp != frame) {
            frameBounds = com.hbm_m.platform.RenderHooks.getRenderBoundingBox(be);
            frameBoundsBE = be;
            frameBoundsStamp = frame;
        }
        return frameBounds;
    }

    private double frameDistSq(BlockEntity be) {
        long frame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (frameDistBE != be || frameDistStamp != frame) {
            frameDistSq = RenderDistanceHelper.distanceSqToCamera(be.getBlockPos());
            frameDistBE = be;
            frameDistStamp = frame;
        }
        return frameDistSq;
    }

    /** Scratch for the fast-path assert (phase 1 -> phase 2); single-threaded rendering. */
    private final java.util.ArrayList<InstancedStaticPartRenderer> fastAssertScratch = new java.util.ArrayList<>();

    // -- Animation-epoch: full roster-assert of "frozen" animated machines --
    // Spec contract - MachineSpecBuilder.animationEpoch: mixes prev+curr of ALL
    // animator inputs, so epoch equality means the pose is identical at any
    // partialTick (lerp is degenerate). The slot stores (epoch, gameTick) of the
    // last FULL rebuild of the animated parts; stable = the epoch is unchanged AND
    // the rebuild was not in the current tick (within a tick the lerp moves the
    // pose between frames - an assert would freeze movement at 20 Hz).
    private static final int EPOCH_SLOT_BITS = 12;
    private final long[] epochPosKey = new long[1 << EPOCH_SLOT_BITS];
    private final long[] epochValue = new long[1 << EPOCH_SLOT_BITS];
    private final long[] epochTick = new long[1 << EPOCH_SLOT_BITS];
    // Per-frame memo: the provider is called once per (machine, frame); renderAll
    // and tryFastAssertRender share the result.
    private BlockEntity epochMemoBE;
    private long epochMemoFrame = -1L;
    private boolean epochMemoStable;

    private static int epochSlot(long posKey) {
        return (int) ((posKey * 0x9E3779B97F4A7C15L) >>> (64 - EPOCH_SLOT_BITS)) & (ANIM_SLOT_MASK);
    }

    /** Current machine epoch; Long.MIN_VALUE - provider error (always "unstable"). */
    private long currentEpoch(BlockEntity be) {
        try {
            return spec.animationEpoch().applyAsLong((T) be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] animationEpoch failed", spec.id(), t);
            return Long.MIN_VALUE;
        }
    }

    /**
     * Whether the machine's animation is frozen (the epoch matches the one stored at
     * the last full rebuild, and the rebuild was not in this tick). At most 1
     * computation per (machine, frame).
     */
    private boolean epochStable(BlockEntity be, long gameTick, long posKey) {
        if (spec.animationEpoch() == null) {
            return false;
        }
        long frame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (epochMemoBE == be && epochMemoFrame == frame) {
            return epochMemoStable;
        }
        boolean stable = false;
        int slot = epochSlot(posKey);
        if (epochPosKey[slot] == posKey && epochTick[slot] != gameTick) {
            long cur = currentEpoch(be);
            stable = cur != Long.MIN_VALUE && cur == epochValue[slot];
        }
        epochMemoBE = be;
        epochMemoFrame = frame;
        epochMemoStable = stable;
        return stable;
    }

    /** Stores the epoch after a full rebuild of the animated parts (records = current pose). */
    private void epochStoreAfterFullAnim(BlockEntity be, long gameTick, long posKey) {
        if (spec.animationEpoch() == null) {
            return;
        }
        long cur = currentEpoch(be);
        if (cur == Long.MIN_VALUE) {
            return;
        }
        int slot = epochSlot(posKey);
        epochPosKey[slot] = posKey;
        epochValue[slot] = cur;
        epochTick[slot] = gameTick;
    }

    // -- Parametric GPU animation (attrib 15, MachineSpecBuilder.parametricPart) --
    /** Config nucleusParametricAnim; the JVM flag -Dhbm.parametricAnim=false is an emergency override. */
    private static final boolean PARAMETRIC_ANIM_FLAG =
            !"false".equalsIgnoreCase(System.getProperty("hbm.parametricAnim", "true"));
    private static boolean parametricAnimOn() {
        return com.hbm_m.config.ModClothConfig.get().nucleusParametricAnim && PARAMETRIC_ANIM_FLAG;
    }
    /** Joint parameter scratch (single-threaded rendering). */
    private final float[] paramScratch = new float[4];
    /** Lazy joint indices per partIdx; -1 = not yet registered. */
    private int[] partJointIdx;
    /** Whether the lazy index array has been allocated to the size of parts. */
    private boolean partJointIdxSized;

    /**
     * Parametrics is active only on the vanilla MDI path: Iris/Tier1 (vertex
     * bakes via compute) and immediate degradation read the CPU animator.
     */
    private boolean isParametricPath(MachinePartRenderer renderer) {
        if (!parametricAnimOn()
                || ShaderCompatibilityDetector.isExternalShaderActive()
                || ShaderCompatibilityDetector.canUseIrisExtendedShader()
                || ShaderCompatibilityDetector.isRenderingShadowPass()
                || ClientRenderFlags.forceVanillaImmediate()) {
            return false;
        }
        InstancedStaticPartRenderer inst = renderer.instanced();
        return inst != null && inst.isInitialized();
    }

    private int jointIndexFor(int partIdx, MachineSpec.PartDef<T> part) {
        if (!partJointIdxSized || partJointIdx.length < spec.parts().size()) {
            partJointIdx = new int[spec.parts().size()];
            java.util.Arrays.fill(partJointIdx, -1);
            partJointIdxSized = true;
        }
        int idx = partJointIdx[partIdx];
        if (idx < 0) {
            MachineSpec.ParametricAnim p = part.parametric();
            idx = com.hbm_m.client.render.NucleusJointSpecs.register(
                    p.kind(), p.axisX(), p.axisY(), p.axisZ(), p.pivotX(), p.pivotY(), p.pivotZ());
            partJointIdx[partIdx] = idx;
        }
        return idx;
    }

    /**
     * Partial fast path (machine inside the animation zone): static parts are
     * confirmed by the roster right in {@link #renderAll}, while animated parts and
     * hooks take the full path. Set in {@link #tryFastAssertRender}, consumed by the
     * first {@code collectRender} (reset in its finally - early frustum/culling
     * exits must not leak onto a neighboring BE).
     */
    private BlockEntity partialAssertBE;
    private boolean animatedPartsResolved;
    private boolean hasAnimatedPartsCache;

    private boolean hasAnimatedParts() {
        if (!animatedPartsResolved) {
            boolean any = false;
            for (MachineSpec.PartDef<T> part : spec.parts()) {
                if (part.animated()) {
                    any = true;
                    break;
                }
            }
            hasAnimatedPartsCache = any;
            animatedPartsResolved = true;
        }
        return hasAnimatedPartsCache;
    }

    /** Part transform stamped with the owning frame (see FrameCtx). */
    private static final class StampedPose {
        final Matrix4f pose = new Matrix4f();
        long stamp;
    }

    private final class FrameCtx implements MachineRenderApi {
        private float fadeAlpha = 1f;
        private BlockPos blockPos = BlockPos.ZERO;
        // Part transforms for hooks: clear()+re-put per machine allocated a HashMap
        // node and a matrix for every part every frame. Entries are persistent
        // (size <= the number of spec part names); freshness is the stamp match:
        // the stamp increments per machine, foreign/stale entries read as null -
        // same semantics as the old clear().
        private final Map<String, StampedPose> transforms = new HashMap<>();
        private long stamp;

        void beginEntry() {
            stamp++;
        }

        @Override public float fadeAlpha() { return fadeAlpha; }
        @Override public BlockPos blockPos() { return blockPos; }
        @Override public @Nullable Matrix4f partTransform(String partName) {
            // Live instance without a defensive copy: the matrix is mutated via
            // saveTransform once per frame, hooks live within the same frame.
            StampedPose e = transforms.get(partName);
            return (e != null && e.stamp == stamp) ? e.pose : null;
        }

        void saveTransform(String partName, Matrix4f pose) {
            StampedPose e = transforms.computeIfAbsent(partName, k -> new StampedPose());
            e.pose.set(pose);
            e.stamp = stamp;
        }
    }

    public MachineBer(MachineSpec<T> spec) {
        this.spec = spec;
    }

    @Override
    protected BakedModel getModelType(BakedModel rawModel) {
        return rawModel;
    }

    @Override
    protected BakedModel getModel(T blockEntity) {
        return spec.modelResolver().apply(blockEntity);
    }

    /**
     * Machine collection WITHOUT the vanilla dispatcher - invoked by the flat walk
     * of {@link com.hbm_m.client.render.NucleusDispatcherBypass} (bypassing
     * Sodium/Iris dispatch). poseStack already carries base*T(be-cam) - the same
     * contract the dispatcher provided. {@code applyFrustum} - main pass only (the
     * shadow pass is not clipped by the main camera frustum).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void collectRender(BlockEntity be, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight, boolean applyFrustum) {
        try {
            doCollectRender(be, partialTick, poseStack, bufferSource, packedLight, applyFrustum);
        } finally {
            // The partial fast path lives only within one collectRender: early
            // exits (frustum/CPU occlusion) must not carry the flag to a
            // neighboring BE.
            partialAssertBE = null;
            // Dirty tracker: collection happened (including early frustum/culling
            // exits - the machine stays in the MDI batch and the GPU-culler catches
            // it, no repeated CPU walk needed until TTL).
            if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker) {
                var lvl = be.getLevel();
                tracker.onRenderCollected(lvl != null ? lvl.getGameTime() : 0L,
                        com.hbm_m.client.render.NucleusRenderVersion.worldGen());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doCollectRender(BlockEntity be, float partialTick, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight, boolean applyFrustum) {
        T blockEntity = (T) be;
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            SHADOW_BER_INVOCATIONS++;
        } else if (applyFrustum && !isInViewFrustum(blockEntity, frameBounds(blockEntity))) {
            return;
        }
        currentModelViewMatrix.set(poseStack.last().pose());

        BakedModel rawModel = getModel(blockEntity);
        rawModel = unwrapFabricForwardingModels(rawModel);
        BakedModel model = getModelType(rawModel);
        if (model == null) return;

        LegacyAnimator animator = LegacyAnimator.create(poseStack);
        com.hbm_m.client.render.LightSampleCache.BASE_POSE.get().set(poseStack.last().pose());
        com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(true);
        poseStack.pushPose();
        try {
            setupBlockTransform(animator, blockEntity);
            renderParts(blockEntity, model, animator, partialTick, packedLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource);
        } finally {
            poseStack.popPose();
            com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(false);
        }
    }

    /**
     * Fast-path dirty-skip: a clean machine, WITHOUT rebuilding, confirms its
     * presence in the part renderers (roster-assert). Returning true means all
     * static parts are confirmed and {@code collectRender} need not be called -
     * CPU cost per machine: a distance check + long-key comparison per part.
     * <p>
     * Gates: Iris off, occlusion mode not CPU (see the visibility block below),
     * tracker not stale (dirty flag, worldGen, light/fade TTL). Beyond the animation
     * zone (and for machines without animated parts/hooks - including inside it) a
     * clean machine confirms ALL static parts via roster-assert - the full path is
     * not needed. Inside the zone, with animated parts or hooks present, the partial
     * fast path engages ({@link #partialAssertBE}): static parts are confirmed in
     * renderAll, per-frame content takes the full path.
     * <p>
     * Safety: phase 1 is read-only (renderer lookup + roster check); on any miss -
     * false, state untouched, the caller performs the full {@code collectRender}.
     */
    @SuppressWarnings("unchecked")
    public boolean tryFastAssertRender(BlockEntity beRaw, long gameTick) {
        if (!ClientRenderFlags.nucleusDirtySkip()) {
            return false;
        }
        // Specs with time-varying dynamic parts (doors: animated=false, but the
        // animator depends on openTicks, which advances the client tick WITHOUT
        // render-dirty) - a roster-assert would freeze the pose ("binary"
        // animation). Such parts always take the full path with the animator run.
        if (spec.hasDynamicAnimators()) {
            return false;
        }
        if (ShaderCompatibilityDetector.isExternalShaderActive()
                || ShaderCompatibilityDetector.canUseIrisExtendedShader()
                || ShaderCompatibilityDetector.isRenderingShadowPass()
                || ClientRenderFlags.forceVanillaImmediate()) {
            return false;
        }
        // Visibility on the assert path:
        //  * GPU+compute - the CPU frustum is skipped, visibility is handled by
        //    the GPU-culler (instance compaction in the compute dispatch);
        //  * CPU occlusion - fastpath FORBIDDEN: the DDA raycast is the active
        //    culler, a roster-assert would freeze the visibility result (a machine
        //    hidden by a wall after collect would stay in the batch);
        //  * GPU without compute support or OFF - there is no occlusion in the full
        //    collection either (OcclusionCullingHelper.shouldRender returns true
        //    outside CPU mode), visibility = frustum + distance. A fastpath with a
        //    cheap CPU frustum gate on the machine AABB gives exactly the same
        //    semantics without a full rebuild: outside the frustum - false, the
        //    full collection will early-exit, the window will remove
        //    dropUnassertedRetained (parity with current frustum culling).
        ModClothConfig.OcclusionCullingMode occlusionMode = ModClothConfig.get().getEffectiveOcclusionCullingMode();
        if (occlusionMode == ModClothConfig.OcclusionCullingMode.CPU) {
            return false;
        }
        if (occlusionMode == ModClothConfig.OcclusionCullingMode.GPU
                && com.hbm_m.client.render.culling.GpuCullingCapability.isSupported()) {
            // no frustum needed - cull happens in compute
        } else if (!isInViewFrustum((T) beRaw, frameBounds(beRaw))) {
            return false;
        }
        if (!(beRaw instanceof com.hbm_m.api.render.RenderDirtyTracker tracker)) {
            return false;
        }
        if (tracker.isRenderStale(gameTick, com.hbm_m.client.render.NucleusRenderVersion.worldGen())) {
            return false;
        }
        // Animation zone: the full path is required only for per-frame content -
        // the animators of animated parts and immediate hooks (fluids/items/
        // diamonds). A machine without them takes the full fast path even inside
        // the zone; otherwise - partial: static parts are confirmed by the roster
        // in renderAll (no matrices/light/float comparison), removing most of the
        // per-frame cost of the zone.
        // Exception - frozen animation (epoch) without hooks: animated parts are
        // confirmed too, no full collection needed at all.
        double animDist = RenderDistanceHelper.getAnimatedDistanceBlocks();
        boolean epochFullAssert = false;
        if (animDist > 0) {
            double distSq = frameDistSq(beRaw);
            if (distSq <= animDist * animDist) {
                if (!spec.hooks().isEmpty() || hasAnimatedParts()) {
                    if (!spec.hooks().isEmpty() || !hasAnimatedParts()
                            || !epochStable(beRaw, gameTick, beRaw.getBlockPos().asLong())) {
                        partialAssertBE = beRaw;
                        return false;
                    }
                    epochFullAssert = true;
                }
            }
        }

        // Distance fade - a pure function of camera position that changes every
        // frame of movement without any dirty event: the roster-assert must verify
        // the quantized fade of the buffer record (canAssertInstance(posKey, fade)),
        // otherwise a machine in the fade ring would "freeze" its alpha until a
        // random full rebuild - a pop instead of dissolving when moving away, a
        // translucent train when approaching. Beyond the cutoff (fade<0) there is
        // nothing to confirm - full collection (the same early fade exit as always).
        float staticFade = RenderDistanceHelper.computeStaticFade(beRaw, frameDistSq(beRaw));
        if (staticFade < 0) {
            return false;
        }
        float quantizedFade = InstancedStaticPartRenderer.quantizeFade(staticFade);
        // Animated parts carry fade = min(static, anim) in the record: their roster
        // must be verified against the same quantized value.
        float quantizedAnimFade = quantizedFade;
        if (epochFullAssert) {
            float animFade = RenderDistanceHelper.computeAnimatedFade(beRaw, frameDistSq(beRaw));
            float fade = animFade >= 0 ? Math.min(staticFade, animFade) : staticFade;
            quantizedAnimFade = InstancedStaticPartRenderer.quantizeFade(fade);
        }

        BlockPos pos = beRaw.getBlockPos();
        long posKey = pos.asLong();

        // Phase 1 (read-only): resolve renderers + roster check.
        fastAssertScratch.clear();
        for (MachineSpec.PartDef<T> part : spec.parts()) {
            // Without an epoch, animated parts are skipped, exactly as in renderAll.
            if (part.animated() && !epochFullAssert) continue;
            String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, (T) beRaw) : null;
            MachinePartRenderer r = spec.findExistingRenderer(part, dynKey);
            if (r == null) {
                return false; // no renderer (cache wiped) - full collection will build it
            }
            InstancedStaticPartRenderer inst = r.instanced();
            if (inst == null) {
                if (r.hasGeometry()) {
                    return false; // single-VBO/immediate part - not instanced
                }
                continue; // empty part - renderAll skips it too
            }
            float partQuantizedFade = part.animated() ? quantizedAnimFade : quantizedFade;
            if (!inst.isInitialized() || !inst.canAssertInstance(posKey, partQuantizedFade)) {
                return false;
            }
            fastAssertScratch.add(inst);
        }

        // Phase 2 (commit): state did not change between phases (single-threaded
        // rendering) - the assert cannot fail.
        for (InstancedStaticPartRenderer inst : fastAssertScratch) {
            inst.assertCleanInstance(pos);
        }
        // THE TRACKER STAMP IS NOT UPDATED HERE: onRenderCollected from every
        // successful assert would keep the stamp fresh every frame, and the
        // TTL rebuild (the only owner of light/fade updates without an event
        // model) would never fire. Light/fade are refreshed by a full collection
        // on TTL (isRenderStale), worldGen change, dirty flag, or fade quantum
        // change (checked above).
        return true;
    }

    @Override
    protected Direction getFacing(T blockEntity) {
        return spec.facingResolver().apply(blockEntity);
    }

    @Override
    protected void setupBlockTransform(LegacyAnimator animator, T blockEntity) {
        var custom = spec.blockTransform();
        if (custom != null) {
            custom.apply(blockEntity, animator);
            return;
        }
        super.setupBlockTransform(animator, blockEntity);
    }

    @Override
    public int getViewDistance() {
        return spec.viewDistance() >= 0 ? spec.viewDistance() : RenderDistanceHelper.getStaticViewDistanceBlocks();
    }

    @Override
    protected void renderParts(T blockEntity, BakedModel model, LegacyAnimator animator, float partialTick,
                               int packedLight, int packedOverlay, PoseStack poseStack,
                               MultiBufferSource bufferSource) {
        // -- Static fade isolation (SingleMeshVboRenderer.currentFadeAlpha) --
        // renderParts is the SINGLE point through which machines set fade
        // (applyCullingAndStaticFade + per-part set in renderAll), and it is called
        // from TWO paths: the regular dispatcher (AbstractPartBasedRenderer.render)
        // and the dispatcher bypass (NucleusDispatcherBypass.collectOne ->
        // collectRender -> doCollectRender). Without restoration, the value of the
        // last collected machine leaks past the collection: on AFTER_ENTITIES the
        // bypass collects all machines in a row, and the fade of a farther machine
        // (static fade zone, alpha < 1) stays in the static for the whole BER phase.
        // The first BER without its own fade set - a rocket on a launch pad - is
        // drawn with blend + depthMask(false), does not write depth, and the MDI
        // flush of machines (AFTER_BLOCK_ENTITIES) draws on top of it ("rocket
        // behind models that should be behind it"; flickering because the last
        // collected machine changes frame to frame: fast-assert sets no fade at
        // all, full collection sets the farther machine's fade).
        // Restoring the input value in finally covers both paths and all early
        // return exits for ANY machine automatically.
        float prevFadeAlpha = SingleMeshVboRenderer.getFadeAlpha();
        try {
            renderPartsInner(blockEntity, model, animator, partialTick, packedLight, packedOverlay,
                    poseStack, bufferSource);
        } finally {
            SingleMeshVboRenderer.setFadeAlpha(prevFadeAlpha);
        }
    }

    private void renderPartsInner(T blockEntity, BakedModel model, LegacyAnimator animator, float partialTick,
                                  int packedLight, int packedOverlay, PoseStack poseStack,
                                  MultiBufferSource bufferSource) {
        // -- Culling + fade (automatic) ----------------------------------
        // Caveat: BE.getLevel() is a VirtualRenderWorld; shouldRender() skips
        // frustum/ray-march culling (see AbstractPartBasedRenderer).
        net.minecraft.world.phys.AABB bounds = frameBounds(blockEntity);
        double distSq = frameDistSq(blockEntity);
        float staticFade = applyCullingAndStaticFade(blockEntity, bounds, distSq);
        if (staticFade < 0) return;
        // Beyond the animation distance (modelUpdateDistance) only statics are shown.
        float animFade = RenderDistanceHelper.computeAnimatedFade(blockEntity, distSq);
        boolean animatedVisible = animFade >= 0;
        // fade for animated content (min of both zones); statics fade out ONLY by
        // the static zone - purely static machines must not flicker in the
        // animation fade ring and come back after it (fade = staticFade = 1).
        float fade = animatedVisible ? Math.min(staticFade, animFade) : staticFade;

        BlockPos blockPos = blockEntity.getBlockPos();

        FrameCtx ctx = frameCtx;
        ctx.fadeAlpha = fade;
        ctx.blockPos = blockPos;
        ctx.beginEntry();

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
            // In shadow, begin(true)+close = apply()/clear() of the pack ON EVERY
            // BER - on assembler farms that is ~14% of the frame. When the global
            // shadow batch is on (all parts go into it as records, see
            // addInstance), the immediate companion draw is not needed and the
            // batch wrapper is not opened. Parts that fell out of the batch
            // degrade correctly to putBulkData (bufferSource) or to a record -
            // both paths work without an active batch.
            if (shadowPass && IrisShadowBatchCollector.isBatchingEnabled()) {
                renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                        poseStack, bufferSource, blockPos, ctx, animatedVisible, staticFade, fade);
            } else {
                try (IrisRenderBatch ignored = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                    renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                            poseStack, bufferSource, blockPos, ctx, animatedVisible, staticFade, fade);
                }
            }
        } else {
            renderAll(blockEntity, model, partialTick, packedLight, packedOverlay,
                    poseStack, bufferSource, blockPos, ctx, animatedVisible, staticFade, fade);
        }
    }

    private void renderAll(T blockEntity, BakedModel model, float partialTick,
                           int packedLight, int packedOverlay, PoseStack poseStack,
                           MultiBufferSource bufferSource, BlockPos blockPos,
                           FrameCtx ctx, boolean animatedVisible, float staticFade, float fade) {
        // Partial fast path: confirmed in tryFastAssertRender for THIS BE.
        // Read-with-consume - a repeated renderAll must not double the assert.
        boolean partialAssert = partialAssertBE == blockEntity;
        partialAssertBE = null;
        long rosterKey = blockPos.asLong();
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        // Animation-epoch: frozen animation - animated parts are confirmed by the
        // roster without running the animators (spec contract: epoch equality
        // means the pose is identical at any partialTick).
        boolean epochStableNow = epochStable(blockEntity, gameTime, rosterKey);
        boolean anyAnimatedRebuilt = false;

        // Shared light: when batching, one 8-corner sample per machine per frame;
        // all parts reuse it (saves getLightColor calls on farms).
        // In the shadow pass with the global batch on, light is not needed at all
        // (shadow FB - depth/plain color): skip the sample entirely.
        float[] sharedLight = null;
        boolean shadowNoLight = ShaderCompatibilityDetector.isRenderingShadowPass()
                && IrisShadowBatchCollector.isBatchingEnabled();
        if (!shadowNoLight && ClientRenderFlags.useInstancedBatching()) {
            net.minecraft.world.phys.AABB bb = frameBounds(blockEntity);
            if (bb != null && bb.maxX > bb.minX && bb.maxY > bb.minY && bb.maxZ > bb.minZ) {
                sharedLightBbox[0] = (float) (bb.minX - blockPos.getX());
                sharedLightBbox[1] = (float) (bb.minY - blockPos.getY());
                sharedLightBbox[2] = (float) (bb.minZ - blockPos.getZ());
                sharedLightBbox[3] = (float) (bb.maxX - blockPos.getX());
                sharedLightBbox[4] = (float) (bb.maxY - blockPos.getY());
                sharedLightBbox[5] = (float) (bb.maxZ - blockPos.getZ());
            } else {
                sharedLightBbox[0] = -0.5f;
                sharedLightBbox[1] = 0f;
                sharedLightBbox[2] = -0.5f;
                sharedLightBbox[3] = 1.5f;
                sharedLightBbox[4] = 2f;
                sharedLightBbox[5] = 1.5f;
            }
            sharedLightPose.identity();
            com.hbm_m.client.render.LightSampleCache.getOrSample8Lod(blockEntity, spec.lightSampleKey,
                    sharedLightBbox, blockPos, sharedLightPose, packedLight, sharedLight8,
                    frameDistSq(blockEntity));
            sharedLight = sharedLight8;
        }

        // -- Parts: static and animated via the VBO pipeline --------------
        // The cache is ALWAYS ON (no external gate): the isExternalShaderActive()
        // detector is unstable during Iris shadow re-entry - with the gate the
        // shadow pass did not write the cache, and every first main read was a
        // delta miss (hits 3.07M ~ the "second" main draws, delta 3.5M ~ the
        // "first" ones, log 0914 03:08). Without duplicate passes the write is
        // simply never read (keyed by frame) - negligible overhead, correctness
        // does not depend on detector state.
        boolean animCacheOn = animCacheOn();
        int cacheSlot = -1;
        long timeKey = 0L;
        boolean baseReady = false;
        java.util.List<MachineSpec.PartDef<T>> parts = spec.parts();
        for (int partIdx = 0; partIdx < parts.size(); partIdx++) {
            MachineSpec.PartDef<T> part = parts.get(partIdx);
            // Only animated CONTENT is skipped; static parts with an "animator"
            // transform (legacy offsets) live up to the static cutoff.
            if (!animatedVisible && part.animated()) continue;
            BakedModel partModel = spec.partModel(part, model);
            // Lazy build: dynQuads are computed only if the VBO is not built yet
            // (previously the resolver was invoked every frame for every BE - see
            // MachineSpec.partRendererLazy).
            if (partModel == null && !part.dynamic()) continue;
            String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, blockEntity) : null;

            MachinePartRenderer renderer = spec.partRendererLazy(part, partModel, blockEntity, dynKey);
            if (!renderer.hasGeometry()) continue;

            // -- Parametric GPU animation: a jointed part bypasses the PoseStack
            // animator. Parameters are written to attrib 15 (4 trailing floats of
            // the record), the vertex shader moves the geometry; a frozen joint =
            // skip-write = zero upload. Outside the vanilla MDI path (degradation /
            // Iris / shadow / kill-switch) - the regular CPU animator from the
            // descriptor (legacy fallback below).
            if (part.parametric() != null && isParametricPath(renderer)) {
                float[] pr = paramScratch;
                boolean drawP;
                try {
                    drawP = part.parametricParams().params(blockEntity, partialTick, gameTime, pr);
                } catch (Throwable t) {
                    com.hbm_m.main.MainRegistry.LOGGER.error(
                            "[MachineRenderers:{}] parametric part '{}' params failed", spec.id(), part.name(), t);
                    drawP = false;
                }
                if (drawP) {
                    // Base record offset (legacy geometry centering): the joint delta
                    // is applied by the VSH in the geometry's local coordinates, and
                    // the record must carry the animator's static transforms MINUS the
                    // joint itself - otherwise the part is displaced by the missing
                    // offset (ntm-next solves this with a base translate(+0.5,0,+0.5)
                    // in front of all geometry).
                    MachineSpec.ParametricAnim pa = part.parametric();
                    boolean shifted = pa.hasBaseOffset();
                    if (shifted) {
                        poseStack.pushPose();
                        poseStack.translate(pa.baseOffsetX(), pa.baseOffsetY(), pa.baseOffsetZ());
                    }
                    // Pose = block transform (+ base offset) - parametrics lives
                    // INSIDE the geometry, the local matrix is untouched.
                    if (!spec.hooks().isEmpty()) {
                        ctx.saveTransform(part.name(), poseStack.last().pose());
                    }
                    SingleMeshVboRenderer.setFadeAlpha(fade);
                    // Parameters were recomputed this frame - the records match
                    // the current state, the epoch can be updated at the end of renderAll.
                    anyAnimatedRebuilt = true;
                    int partLight = packedLight;
                    float[] partSharedLight = sharedLight;
                    if (part.lightOverride() != null) {
                        Integer forced = part.lightOverride().apply(blockEntity);
                        if (forced != null && forced >= 0) {
                            partLight = forced;
                            partSharedLight = null;
                        }
                    }
                    InstancedStaticPartRenderer.setPendingAnimParams(pr[0], pr[1], pr[2],
                            jointIndexFor(partIdx, part));
                    renderer.enqueue(poseStack, partLight, blockPos, blockEntity, bufferSource,
                            partSharedLight, spec.resolveUvRect(part, blockEntity),
                            spec.resolveTint(part, blockEntity), part.tintFalloff());
                    if (shifted) {
                        poseStack.popPose();
                    }
                }
                continue;
            }

            // Animation cache: initialize on the first part with an animator.
            // Key = per-frame counter (shared between shadow/main of one frame).
            // SLOT: asLong() holds X in bits 0..25, Z in 26..51 - masking the low
            // bits saw ONLY X (a row of machines along Z sharing X = full
            // collision, 90% delta misses, log 0914 03:08). Mix by multiplication
            // and take the HIGH bits - uniform distribution over X/Y/Z.
            if (part.animator() != null && animCacheOn && cacheSlot < 0) {
                long posKey0 = blockPos == null ? 0L : blockPos.asLong();
                long mixed = posKey0 * 0x9E3779B97F4A7C15L;
                cacheSlot = (int) (mixed >>> (64 - 12)) & ANIM_SLOT_MASK;
                timeKey = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
                if (animPosKey[cacheSlot] != posKey0 || animTimeKey[cacheSlot] != timeKey) {
                    animInvalidate(cacheSlot);
                }
            }

            poseStack.pushPose();
            try {
                boolean draw = true;
                // Partial fast path (machine inside the animation zone): the static
                // part is already in last frame's instance buffer - confirm it with
                // a roster-assert instead of a full rebuild (matrix/light/30-float
                // comparison). Fade guard: the buffer record's fade quantum must
                // equal the current staticFade quantum: a fading part is rebuilt
                // only when the alpha quantum (1/255) changes, symmetric to the
                // full fast path beyond the zone. Pose semantics - as with the
                // full fast path beyond the zone: the static part's animator
                // (legacy offsets, constant pose) is not run, the buffer record
                // is unchanged.
                boolean fastAsserted = false;
                if (partialAssert && !part.animated()
                        && !(part.animator() != null && part.dynamic())) {
                    InstancedStaticPartRenderer inst = renderer.instanced();
                    if (inst != null && inst.isInitialized()
                            && inst.canAssertInstance(rosterKey,
                                    InstancedStaticPartRenderer.quantizeFade(staticFade))) {
                        int beforeCount = inst.getInstanceCount();
                        inst.assertCleanInstance(blockPos);
                        fastAsserted = inst.getInstanceCount() > beforeCount;
                    }
                }
                // Frozen animation: the animated part is confirmed by the roster -
                // the pose in the buffer matches the current one (epoch contract);
                // the fade check uses the animated content's fade quantum
                // (fade = min(static, anim)).
                if (!fastAsserted && epochStableNow && part.animated()) {
                    InstancedStaticPartRenderer inst = renderer.instanced();
                    if (inst != null && inst.isInitialized()
                            && inst.canAssertInstance(rosterKey,
                                    InstancedStaticPartRenderer.quantizeFade(fade))) {
                        int beforeCount = inst.getInstanceCount();
                        inst.assertCleanInstance(blockPos);
                        fastAsserted = inst.getInstanceCount() > beforeCount;
                    }
                }
                if (!fastAsserted && part.animated()) {
                    // The part went through the animator - the pose was recomputed
                    // this frame, the epoch can be stored at the end of renderAll.
                    anyAnimatedRebuilt = true;
                }
                if (!fastAsserted && part.animator() != null) {
                    long posKey = blockPos == null ? 0L : blockPos.asLong();
                    boolean hit;
                    if (!animCacheOn) {
                        hit = false;
                        MISS_OFF++;
                    } else if (cacheSlot < 0) {
                        hit = false;
                        MISS_NO_SLOT++;
                    } else if (animPosKey[cacheSlot] != posKey) {
                        hit = false;
                        MISS_POS++;
                    } else if (animTimeKey[cacheSlot] != timeKey) {
                        hit = false;
                        MISS_FRAME++;
                    } else if (animDelta[cacheSlot] == null || animDelta[cacheSlot][partIdx] == null) {
                        hit = false;
                        MISS_DELTA++;
                        if (ShaderCompatibilityDetector.isRenderingShadowPass()) DELTA_MISS_SHADOW++;
                        else DELTA_MISS_MAIN++;
                    } else {
                        hit = true;
                    }
                    if (!baseReady) {
                        animBase.set(poseStack.last().pose());
                        animBaseInv.set(animBase).invert();
                        baseReady = true;
                    }
                    if (hit) {
                        ANIM_HITS++;
                        if (animSkip[cacheSlot][partIdx]) {
                            draw = false;
                        } else {
                            // F' = S * delta - the current pass's basis on top of
                            // the cached local animation.
                            animComposed.set(animBase).mul(animDelta[cacheSlot][partIdx]);
                            poseStack.last().pose().set(animComposed);
                        }
                    } else {
                        ANIM_MISSES++;
                        maybeLogAnimCache();
                        draw = part.animator().animate(blockEntity, partialTick, gameTime, poseStack);
                        if (animCacheOn && cacheSlot >= 0) {
                            animStore(cacheSlot, partIdx, parts.size(), posKey, timeKey,
                                    animBaseInv, poseStack.last().pose(), draw);
                        }
                    }
                }
                if (fastAsserted) {
                    // The part's pose is needed by hooks (MachineRenderApi.partTransform)
                    // even when the rebuild is skipped - the base pose (the static
                    // part's animator is constant); save without enqueue.
                    if (!spec.hooks().isEmpty()) {
                        ctx.saveTransform(part.name(), poseStack.last().pose());
                    }
                } else if (draw) {
                    // Matrices are only needed by hooks (MachineRenderApi.partTransform);
                    // without hooks nothing is allocated.
                    if (!spec.hooks().isEmpty()) {
                        ctx.saveTransform(part.name(), poseStack.last().pose());
                    }
                    // Per-part fade: fade is written into per-instance data at
                    // addInstance time - statics are faded only by the static
                    // zone, animated parts additionally by the animation zone.
                    SingleMeshVboRenderer.setFadeAlpha(part.animated() ? fade : staticFade);
                    // Forced part light (fullbright port: InnerBurning furnaces, lightmap 240/240)
                    int partLight = packedLight;
                    float[] partSharedLight = sharedLight;
                    if (part.lightOverride() != null) {
                        Integer forced = part.lightOverride().apply(blockEntity);
                        if (forced != null && forced >= 0) {
                            partLight = forced;
                            // The machine's shared 8-corner sample would overwrite
                            // the forced light (crucible melt darkened at night) -
                            // do not pass it on fullbright parts.
                            partSharedLight = null;
                        }
                    }
                    renderer.enqueue(poseStack, partLight, blockPos,
                            blockEntity, bufferSource, partSharedLight,
                            spec.resolveUvRect(part, blockEntity),
                            spec.resolveTint(part, blockEntity),
                            part.tintFalloff());
                }
            } catch (Throwable t) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] part '{}' render failed",
                        spec.id(), part.name(), t);
            } finally {
                poseStack.popPose();
            }
        }

        // Animation-epoch: after rebuilding the animated parts, store (epoch, tick)
        // - the buffer records now correspond to this pose.
        if (anyAnimatedRebuilt) {
            epochStoreAfterFullAnim(blockEntity, gameTime, rosterKey);
        }

        // -- Hooks: fluids/items/diamonds (immediate); not drawn beyond the
        // animation distance (icons/diamonds are animation cosmetics) ------
        if (!animatedVisible) return;
        for (MachineRenderHook<T> hook : spec.hooks()) {
            try {
                hook.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, ctx);
            } catch (Throwable t) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] hook render failed", spec.id(), t);
            }
        }
    }
}
