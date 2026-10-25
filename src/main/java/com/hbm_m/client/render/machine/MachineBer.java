package com.hbm_m.client.render.machine;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.IrisShadowBatchCollector;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.PartEnqueueRequest;
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
    // The per-machine delta arrays live in {@link MachineRenderState}, keyed by
    // posKey in the same map as the roster/epoch/param state - no slot ring, no
    // collisions by construction (the 32768-slot ring see-sawed owners every
    // frame on a 5000-machine farm, spark 2026-10-07).
    // Scratch (rendering is single-threaded): S/Sinv of the current BE for the
    // duration of the parts pass.
    private final Matrix4f animBase = new Matrix4f();
    private final Matrix4f animBaseInv = new Matrix4f();
    private final Matrix4f animComposed = new Matrix4f();
    // Scratch matrices to eliminate PoseStack.pushPose() / popPose() heap allocations:
    private final Matrix4f machineBasePoseScratch = new Matrix4f();
    private final Matrix3f machineBaseNormalScratch = new Matrix3f();
    private final Matrix4f partPoseScratch = new Matrix4f();
    private final Matrix3f partNormalScratch = new Matrix3f();

    private void animStore(MachineRenderState state, int partIdx, int partCount, long timeKey,
                           Matrix4f baseInv, Matrix4f finalPose, boolean draw) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) STORE_SHADOW++;
        else STORE_MAIN++;
        Matrix4f[] deltas = state.animDelta(partCount);
        if (deltas[partIdx] == null) {
            deltas[partIdx] = new Matrix4f();
        }
        // Misses happen on the first pass of every BE of every frame (and after
        // the timeKey changes on a tick) - alloc-free: matrices are reused.
        deltas[partIdx].set(baseInv).mul(finalPose);
        state.animSkip[partIdx] = !draw;
        state.animPartTime[partIdx] = timeKey;
        state.animFrameKey = timeKey;
    }

    // Hit/miss counters of the animation cache (diagnosing "cache not hitting",
    // profile 0914).
    private static volatile long ANIM_HITS = 0;
    private static volatile long ANIM_MISSES = 0;
    private static volatile long MISS_OFF = 0, MISS_STATE = 0, MISS_FRAME = 0, MISS_DELTA = 0;
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
                        "[HBM-M] anim cache: hits={} misses={} ({}% hit) | off={} noState={} frame={} "
                                + "delta={} (deltaShadow={} deltaMain={}) | stores: shadow={} main={}",
                        ANIM_HITS, ANIM_MISSES,
                        (ANIM_HITS + ANIM_MISSES) == 0 ? 0
                                : ANIM_HITS * 100 / (ANIM_HITS + ANIM_MISSES),
                        MISS_OFF, MISS_STATE, MISS_FRAME, MISS_DELTA,
                        DELTA_MISS_SHADOW, DELTA_MISS_MAIN, STORE_SHADOW, STORE_MAIN);
            }
            ANIM_HITS = 0;
            ANIM_MISSES = 0;
            MISS_OFF = 0;
            MISS_STATE = 0;
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
    private BlockEntity frameDistBE;
    private long frameDistStamp = -1L;
    private double frameDistSq;

    // -- Cross-frame render-bounds cache ----------------------------------
    // Centralized in RenderBoundsCache (shared with the Embeddium/Sodium call
    // site via SodiumRenderBoundsMixin) - per-renderer caches duplicated the
    // invalidation logic and each covered only their own callers. Each get()
    // is a map lookup + staleness checks, and MachineBer fetched the bounds
    // up to 3x per machine per pass (frustum pre-gate, culling/fade, shared
    // light) plus the fast-assert pre-gate - the single-slot memo below (the
    // frameDistSq pattern) reduces it to one get() per (machine, frame).
    private BlockEntity frameBoundsBE;
    private long frameBoundsStamp = -1L;
    private net.minecraft.world.phys.AABB frameBoundsMemo;
    /** Second slot: the collect loop interleaves two machines (assert + collect),
     * so a single-entry memo missed on EVERY call (3 RenderBoundsCache lookups
     * per machine per frame, ~1.7% of the frame on the fleet). */
    private BlockEntity frameBoundsBE2;
    private long frameBoundsStamp2 = -1L;
    private net.minecraft.world.phys.AABB frameBoundsMemo2;
    private boolean frameBoundsSlotToggle;

    private net.minecraft.world.phys.AABB frameBounds(BlockEntity be) {
        long frame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (frameBoundsBE == be && frameBoundsStamp == frame) {
            return frameBoundsMemo;
        }
        if (frameBoundsBE2 == be && frameBoundsStamp2 == frame) {
            return frameBoundsMemo2;
        }
        net.minecraft.world.phys.AABB box = com.hbm_m.client.render.RenderBoundsCache.get(be);
        if (frameBoundsSlotToggle) {
            frameBoundsBE = be;
            frameBoundsStamp = frame;
            frameBoundsMemo = box;
        } else {
            frameBoundsBE2 = be;
            frameBoundsStamp2 = frame;
            frameBoundsMemo2 = box;
        }
        frameBoundsSlotToggle = !frameBoundsSlotToggle;
        return box;
    }

    /** Level game time, memoized per render frame (was a per-machine virtual call in hot loops). */
    private static long frameGameTimeStamp = -1L;
    private static long frameGameTimeMemo = 0L;

    /**
     * Distance throttle for animated content (CrankShaft BandedPrimeLimiter analog):
     * beyond the first 64-block band the animated parts re-record every Nth frame
     * (N = 3..8 by distance: the first throttled band 64-128 blocks re-records
     * every 3rd frame, each further band adds one, capped at 8); in between the
     * roster assert keeps the last pose. Band size 64 blocks, cap N=8
     * (~7.5 updates/s at 400+ blocks).
     */
    private boolean isAnimationThrottled(T be) {
        double distSq = frameDistSq(be);
        int band = (int) (distSq / 4096.0);
        if (band <= 0) {
            return false;
        }
        int divisor = Math.min(2 + band, 8);
        return (int) (com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame() % divisor) != 0;
    }

    private static long frameGameTime() {
        long frame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (frameGameTimeStamp != frame) {
            var lvl = net.minecraft.client.Minecraft.getInstance().level;
            frameGameTimeMemo = lvl != null ? lvl.getGameTime() : 0L;
            frameGameTimeStamp = frame;
        }
        return frameGameTimeMemo;
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
    private InstancedStaticPartRenderer[] fastAssertScratch = new InstancedStaticPartRenderer[32];
    private int fastAssertScratchCount = 0;
    private int[] shadowPreCountsScratch = new int[32];

    // -- Frozen roster walk (partial fast path without hooks) -------------
    // A partial-asserted machine still pays the FULL part loop every frame:
    // per-part model lookups (HashMap), renderer resolution, pushPose/popPose,
    // conditionals - only to reach the roster assert (profile 2000: renderAll
    // self 10% for machines whose static parts are all asserted). The roster
    // caches, per machine, which parts are structurally static-assertable and
    // their renderers: on a partial frame those parts are confirmed with two
    // tight array passes (validate all -> commit all), and the part loop runs
    // ONLY for rebuild parts (animated/dynamic/parametric). Structural facts
    // (part list is immutable per spec; static part geometry is stable; a
    // renderer cache wipe fails canAssertInstance and drops the entry), so the
    // cache is self-healing. Hooks are compatible: when hooks exist, the frozen
    // walk preamble (top of renderAll) supplies the hook base pose for every
    // ASSERT part - the shared block-transform basis, saved via saveTransform
    // without recomputing anything.
    // -- Per-machine state map ---------------------------------------------
    // One MachineRenderState per position (roster + epoch + param words + anim
    // delta) replaces the old rosterCache / paramWordCache maps and the epoch
    // (65536-slot) and anim (32768-slot) ring arrays: no slot hashing, no
    // collision workarounds - the F3 "Frozen: 0/100%" see-saw from two
    // machines sharing a ring slot is gone by construction.
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<MachineRenderState> states =
            new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private long statesWorldGen = -1L;

    /** Get-or-create the state of a position (render thread only). */
    private MachineRenderState stateFor(long posKey) {
        MachineRenderState s = states.get(posKey);
        if (s == null) {
            s = new MachineRenderState();
            states.put(posKey, s);
        }
        return s;
    }

    static {
        // Central teardown: a removed machine's roster entry survives until a
        // worldGen bump, and BER_FAST_CACHE keeps serving the pre-reload BER
        // instance, so stale rosters (with hollowed renderer refs after the
        // renderer-cache wipe) would otherwise survive into the next session
        // (audit 2026-10-05 §3.24). Snapshot first, then clearBerCache so
        // collectMain re-fetches the fresh instances via the dispatcher
        // fallback.
        com.hbm_m.client.render.cache.RenderCacheManager.register(reason -> {
            for (MachineBer<?> ber : com.hbm_m.client.render.NucleusDispatcherBypass.snapshotBers()) {
                ber.clearRosterCache();
            }
            com.hbm_m.client.render.NucleusDispatcherBypass.clearBerCache();
        });
    }

    /** Teardown of per-machine state; runs on the render thread via RenderCacheManager. */
    public void clearRosterCache() {
        states.clear();
        statesWorldGen = -1L;
    }

    /**
     * Frozen walk: confirms every cached static-assertable part of the machine.
     * Validates ALL renderers first - a mixed assert+rebuild must never happen
     * (a failed assert after a committed one would double-extend the roster).
     * {@code epochParts} gates the ASSERT_EPOCH category (parametric parts under
     * a stable animation epoch); when false those parts are left to the regular
     * parts loop without invalidating the statics' assert.
     *
     * @return true if every selected part was asserted; false = caller must fall
     *         back to the full part loop (and drop the entry).
     */
    private boolean tryFrozenRosterAssert(MachineRenderState roster, BlockPos blockPos, long rosterKey,
                                          boolean epochParts) {
        // Slot-indexed fast path: the per-part slot is cached in the state and
        // verified with one direct array read (instanceOcclusionKeys[slot] ==
        // rosterKey) - zero hash lookups, zero monitor locks. The cached slot is
        // dropped as soon as the ownership check fails (free/reorder/remap).
        byte[] cats = roster.rosterCategory;
        MachinePartRenderer[] rends = roster.rosterRenderers;
        int[] slots = roster.rosterSlots;
        int n = cats.length;
        for (int i = 0; i < n; i++) {
            byte cat = cats[i];
            if (cat != MachineRenderState.ASSERT && !(cat == MachineRenderState.ASSERT_EPOCH && epochParts)) continue;
            InstancedStaticPartRenderer inst = rends[i].instanced();
            if (inst == null) return false;
            int slot = slots != null ? slots[i] : -1;
            if (slot < 0) {
                slot = inst.getSlot(rosterKey);
                if (slot < 0) return false;
                if (slots != null) slots[i] = slot;
            }
            if (!inst.canAssertSlot(slot, rosterKey)) {
                if (slots != null) slots[i] = -1;
                return false;
            }
        }
        for (int i = 0; i < n; i++) {
            byte cat = cats[i];
            if (cat != MachineRenderState.ASSERT && !(cat == MachineRenderState.ASSERT_EPOCH && epochParts)) continue;
            if (!rends[i].instanced().assertCleanSlot(slots[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fast in-place update for actively animating parametric machines whose parts are
     * all in {@code roster} (static ASSERT + parametric ASSERT_EPOCH) and whose hooks
     * are quiet. Validates all cached slots first, then asserts static slots and
     * updates ONLY the 4-float AnimParams tail of parametric slots directly in the
     * native instance buffer - the full renderAll loop (bounds, light sampling,
     * part models, addInstance) never runs for these machines.
     */
    private boolean tryFastParametricRosterUpdate(T blockEntity, MachineRenderState roster, long rosterKey,
                                                  long gameTime, float partialTick) {
        if (!parametricAnimOn() || ShaderCompatibilityDetector.isExternalShaderActive()) {
            return false;
        }
        byte[] cats = roster.rosterCategory;
        MachinePartRenderer[] rends = roster.rosterRenderers;
        int[] slots = roster.rosterSlots;
        int n = cats.length;
        for (int i = 0; i < n; i++) {
            InstancedStaticPartRenderer inst = rends[i] != null ? rends[i].instanced() : null;
            if (inst == null) return false;
            int slot = slots != null ? slots[i] : -1;
            if (slot < 0) {
                slot = inst.getSlot(rosterKey);
                if (slot < 0) return false;
                if (slots != null) slots[i] = slot;
            }
            if (!inst.canAssertSlot(slot, rosterKey)) {
                if (slots != null) slots[i] = -1;
                return false;
            }
        }
        boolean throttled = isAnimationThrottled(blockEntity);
        java.util.List<MachineSpec.PartDef<T>> parts = spec.parts();
        float[] pr = paramScratch;
        float[] enc = encodedScratch;
        for (int i = 0; i < n; i++) {
            byte cat = cats[i];
            InstancedStaticPartRenderer inst = rends[i].instanced();
            int slot = slots[i];
            if (cat == MachineRenderState.ASSERT || throttled) {
                inst.assertCleanSlot(slot);
                continue;
            }
            // cat == MachineRenderState.ASSERT_EPOCH (GPU-parametric part)
            MachineSpec.PartDef<T> part = parts.get(i);
            MachineSpec.ParametricRig pa = part.parametric();
            if (pa == null || part.parametricParams() == null) {
                return false;
            }
            java.util.Arrays.fill(pr, 0f);
            boolean drawP;
            try {
                drawP = part.parametricParams().params(blockEntity, partialTick, gameTime, pr);
            } catch (Throwable t) {
                drawP = false;
            }
            if (!drawP) {
                inst.updateCleanSlotAnimParams(slot, 0f, 0f, 0f, 0f,
                        InstancedStaticPartRenderer.FADE_MODE_INVISIBLE);
                continue;
            }
            pa.encodeAnimParams(pr, enc);
            float fadeMode = (part.isConditional() && !part.isVisible(blockEntity))
                    ? InstancedStaticPartRenderer.FADE_MODE_INVISIBLE
                    : InstancedStaticPartRenderer.FADE_MODE_MIN_ANIM;
            inst.updateCleanSlotAnimParams(slot, enc[0], enc[1], enc[2], enc[3], fadeMode);
        }
        epochStoreAfterFullAnim(blockEntity, gameTime, rosterKey);
        com.hbm_m.client.render.NucleusDebug.recordMachineRendered();
        return true;
    }

    // -- Animation-epoch: full roster-assert of "frozen" animated machines --
    // Spec contract - MachineSpecBuilder.animationEpoch: mixes prev+curr of ALL
    // animator inputs, so epoch equality means the pose is identical at any
    // partialTick (lerp is degenerate). The machine's MachineRenderState stores
    // (epoch, gameTick) of the last FULL rebuild of the animated parts; stable =
    // the epoch is unchanged AND the rebuild was not in the current tick (within
    // a tick the lerp moves the pose between frames - an assert would freeze
    // movement at 20 Hz). Keyed by the state map's posKey - no slot ring.
    // Per-frame memo: the provider is called once per (machine, frame); renderAll
    // and tryFastAssertRender share the result.
    private BlockEntity epochMemoBE;
    private long epochMemoFrame = -1L;
    private boolean epochMemoStable;

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
        MachineRenderState s = states.get(posKey);
        if (s != null && s.epochStored && s.epochTick != gameTick) {
            long cur = currentEpoch(be);
            stable = cur != Long.MIN_VALUE && cur == s.epochValue;
        }
        com.hbm_m.client.render.NucleusDebug.recordEpochCheck(stable);
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
        MachineRenderState s = stateFor(posKey);
        // Store ONLY on a real change: re-storing an unchanged epoch bumps
        // epochTick to the current tick, and the epochTick != gameTick guard
        // then reports "unstable" for the REST of that tick - the next frame
        // full-pathed again and stored again, and the fleet oscillated
        // stable/unstable frame by frame (the F3 "Frozen" line flickering
        // 0/100, spark 2026-10-08) with the parametric branch never
        // converging into the frozen walk. A stale tick with a matching value
        // is exactly the converged state the walk needs.
        if (s.epochStored && s.epochValue == cur) {
            return;
        }
        s.epochStored = true;
        s.epochValue = cur;
        s.epochTick = gameTick;
    }

    // -- Parametric GPU animation (attrib 15, MachineSpecBuilder.parametricPart) --
    /** Config nucleusParametricAnim; the JVM flag -Dhbm.parametricAnim=false is an emergency override. */
    private static final boolean PARAMETRIC_ANIM_FLAG =
            !"false".equalsIgnoreCase(System.getProperty("hbm.parametricAnim", "true"));
    private static boolean parametricAnimOn() {
        return com.hbm_m.config.ModClothConfig.get().nucleusParametricAnim && PARAMETRIC_ANIM_FLAG;
    }
    /** Joint parameter scratch (single-threaded rendering). */
    private final float[] paramScratch = new float[MachineSpec.CHAIN_PARAM_BUDGET];
    /** Encoded (packed binary16 words) AnimParams scratch for the record. */
    private final float[] encodedScratch = new float[4];
    /** Lazy joint indices per partIdx; -1 = not yet registered. */
    private int[] partJointIdx;
    /** Whether the lazy index array has been allocated to the size of parts. */
    private boolean partJointIdxSized;

    // -- Frozen parametric eval-skip: epoch-keyed AnimParams word cache ----
    // The params() chain lambdas (~5.4% of the frame on assembler farms)
    // re-run every frame even while the machine's animation epoch is
    // unchanged. Per the epoch contract (MachineSpecBuilder.animationEpoch)
    // an epoch unchanged ACROSS TICKS means the pose/params are identical at
    // any partialTick; within the storage tick the lerp still moves the pose
    // between frames (the same reason epochStable carries an epochTick
    // guard), so those frames re-evaluate. Keyed by (blockPos.asLong(),
    // partIdx): one map entry per machine position, per-part arrays inside -
    // there are no slot collisions by construction, a replaced machine
    // re-stores through the same key and the epoch comparison governs
    // correctness (epoch equality <=> identical animator inputs). Populated
    // only for specs that provide an epoch (explicit animationEpoch or the
    // builder's deduceAnimationEpoch - both land in spec.animationEpoch());
    // entries are dropped on the render-world change that resets the roster
    // cache. The LOGICAL params are cached alongside the packed words - the
    // lazy hook path (saveParametricTransform) copies them for partTransform
    // queries.
    // Frozen parametric eval-skip: the per-machine AnimParams word cache lives
    // in MachineRenderState (paramEpoch/paramTick + per-part drawn/logical/
    // words), keyed by posKey in the shared states map - one entry per machine
    // position, no slot collisions by construction.

    /**
     * BE-removal hook (drained from
     * {@link com.hbm_m.client.render.NucleusDispatcherBypass#onMachineRemoved}):
     * drop the per-position state of a broken machine so it does not stay
     * pinned until the next worldGen bump / render-world change. A replaced
     * machine re-stores through the same key - correctness is governed by the
     * store path, so dropping early is safe.
     */
    public void dropPerPositionCaches(long posKey) {
        states.remove(posKey);
        if (spec != null) {
            spec.freeMachineSlots(posKey);
        }
    }

    /**
     * Parametrics is active on the vanilla MDI path ONLY: no Iris main-pass
     * shader applies the joint delta (attrib 15) - the packed encoder's vsh
     * has no joint block and every other pack layout runs the companion path
     * through the pack's own program - so under an active shaderpack
     * parametrics is always false and the CPU animator (the regular
     * animated-part branch in renderAll) drives the pose instead. Tier-1
     * compute bake likewise reads the record's pos/quat/bbox/light words
     * only, never the joint words.
     */
    private boolean isParametricPath(MachinePartRenderer renderer) {
        if (!parametricAnimOn()
                || ShaderCompatibilityDetector.isRenderingShadowPass()
                || ClientRenderFlags.forceVanillaImmediate()
                || ShaderCompatibilityDetector.isExternalShaderActive()) {
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
            // Single joint or joint chain - the rig registers its own texels.
            idx = part.parametric().registerSpecs();
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

    /** Memoized: any part declares a parametric (GPU joint) rig. */
    private boolean hasParametricParts() {
        if (!parametricPartsResolved) {
            boolean any = false;
            for (MachineSpec.PartDef<T> part : spec.parts()) {
                if (part.parametric() != null) {
                    any = true;
                    break;
                }
            }
            parametricPartsCache = any;
            parametricPartsResolved = true;
        }
        return parametricPartsCache;
    }

    private boolean parametricPartsResolved;
    private boolean parametricPartsCache;

    /** Part transform stamped with the owning frame (see FrameCtx). */
    private static final class StampedPose {
        final Matrix4f pose = new Matrix4f();
        long stamp;
        @Nullable MachineSpec.ParametricRig lazyRig;
        final float[] lazyParams = new float[MachineSpec.CHAIN_PARAM_BUDGET];
        boolean lazyPending;
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
        // Block-transform basis inverse (B^-1), stamped per machine frame exactly
        // like the transforms above: MachineBer is one instance per BE TYPE and
        // several machines of that type render within one frame, so a basis
        // captured for machine A must never convert machine B's poses. captureBasis
        // is called right after beginEntry(), before any saveTransform of the same
        // entry (renderAll runs strictly later); basisStamp records the owning
        // entry so the invariant is auditable.
        private final Matrix4f basisInv = new Matrix4f();
        private long basisStamp = -1L;
        /** Whether ANY hook reported content for THIS machine this frame -
         * gates the basis capture and every transform save (an idle assembler
         * fleet's recipe-icon hook is empty: the capture invert + the per-part
         * saves were 3% of the frame for transforms nobody would read). */
        boolean hooksDrawContent;

        void beginEntry() {
            stamp++;
        }

        /**
         * Captures B^-1 once per machine per frame. Call with
         * {@code poseStack.last().pose()} right after {@link #beginEntry()} - at
         * that point the stack carries the block-transform basis B (the view base
         * plus {@code setupBlockTransform}).
         * <p>
         * B is provably rigid for EVERY spec (view base = rotations +
         * translations only; block transform goes through the LegacyAnimator
         * facade, which offers no scale operation), but JOML's Matrix4f has no
         * rigid-specialized inverse (verified: javap on joml 1.10.5/1.10.8 -
         * only invert/invertAffine/invertOrtho/... exist), so {@code
         * invertAffine()} is used: it is exact for rigid B and would stay exact
         * if a future block transform ever gained scale.
         */
        void captureBasis(Matrix4f basis) {
            basisInv.set(basis).invertAffine();
            basisStamp = stamp;
        }

        @Override public float fadeAlpha() { return fadeAlpha; }
        @Override public BlockPos blockPos() { return blockPos; }
        @Override public @Nullable Matrix4f partTransform(String partName) {
            // Live instance WITHOUT a defensive copy: mutating the returned
            // matrix corrupts the slot for every later hook of the same frame
            // (saveTransform rewrites it only on the next machine frame). Copy
            // on the caller side if the value must be held or changed.
            // The matrix is the BLOCK-RELATIVE delta (B^-1 * pose, see
            // captureBasis/saveTransform): apply it with mulPose right after
            // your own pushPose, on a stack already carrying the block
            // transform.
            StampedPose e = transforms.get(partName);
            if (e == null || e.stamp != stamp) {
                return null;
            }
            if (e.lazyPending && e.lazyRig != null) {
                // First query of this machine frame: compose the joint chain on
                // top of the saved record statics (e.pose = B^-1 * statics;
                // applyToPose right-multiplies the joint ops, so the result is
                // exactly the eager saveTransform value).
                e.lazyRig.applyToPose(e.pose, e.lazyParams);
                e.lazyPending = false;
            }
            return e.pose;
        }

        void saveTransform(String partName, Matrix4f pose) {
            StampedPose e = transforms.computeIfAbsent(partName, k -> new StampedPose());
            // Strip the per-frame basis: e.pose = B^-1 * pose. For static/roster
            // sites (pose == B) this is identity; for animator-driven parts it
            // is exactly the animator's delta; for parametric parts it is
            // recordStatics * jointDelta - what applyToPose composes.
            basisInv.mul(pose, e.pose);
            e.stamp = stamp;
            e.lazyPending = false;
        }

        /**
         * Lazy variant for parametric parts: saves the RECORD STATICS pose
         * (cheap, no forward kinematics) plus the rig and its logical per-frame
         * parameters; the joint delta is composed on the first
         * {@link #partTransform} query (see StampedPose). {@code pr} is copied -
         * the caller's array is scratch shared by all parts.
         */
        void saveParametricTransform(String partName, Matrix4f staticPose,
                                     MachineSpec.ParametricRig rig, float[] pr) {
            StampedPose e = transforms.computeIfAbsent(partName, k -> new StampedPose());
            basisInv.mul(staticPose, e.pose);
            e.lazyRig = rig;
            System.arraycopy(pr, 0, e.lazyParams, 0, Math.min(pr.length, e.lazyParams.length));
            e.lazyPending = true;
            e.stamp = stamp;
        }
    }

    public MachineBer(MachineSpec<T> spec) {
        this.spec = spec;
        com.hbm_m.client.render.NucleusDispatcherBypass.registerBer(spec.type(), this);
    }

    @Override
    protected BakedModel getModelType(BakedModel rawModel) {
        return rawModel;
    }

    @Override
    protected BakedModel getModel(T blockEntity) {
        return spec.modelResolver().apply(blockEntity);
    }

    private boolean isMachineInViewFrustum(T blockEntity, net.minecraft.world.phys.AABB bounds) {
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(blockEntity)) {
            return true;
        }
        if (com.hbm_m.client.render.culling.CpuFrustumCuller.isReady()) {
            return com.hbm_m.client.render.culling.CpuFrustumCuller.INSTANCE.isWorldBoxVisible(bounds);
        }
        return isInViewFrustum(blockEntity, bounds);
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
                tracker.onRenderCollected(frameGameTime(),
                        com.hbm_m.client.render.NucleusRenderVersion.worldGen());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doCollectRender(BlockEntity be, float partialTick, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight, boolean applyFrustum) {
        T blockEntity = (T) be;
        // Двери: per-frame детект flip'а share-состояния (Iris on/off, конфиг-тумблер,
        // момент загрузки шейдера) -> shareEpoch++ и полная инвалидация PART/VBO/dae
        // кешей ДО чтения любых кеш-ключей этого кадра (детерминизм share, рев.2).
        if (blockEntity instanceof com.hbm_m.block.entity.doors.DoorBlockEntity) {
            com.hbm_m.client.render.implementations.MachineDoorRenderer.syncShareState();
        }
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            SHADOW_BER_INVOCATIONS++;
        } else if (applyFrustum && !(com.hbm_m.client.render.culling.GpuCullCoverage.isActive()
                || (ShaderCompatibilityDetector.isExternalShaderActive()
                    && com.hbm_m.client.render.NucleusGpuBaker.isEnabled()))
                && !isMachineInViewFrustum(blockEntity, frameBounds(blockEntity))) {
            // GPU cull active: skip the CPU frustum re-test - the machine stays
            // in the MDI batch and the GPU frustum/Hi-Z compaction culls it
            // (same contract as the finally block in collectRender).
            return;
        }
        currentModelViewMatrix.set(poseStack.last().pose());

        BakedModel rawModel = getModel(blockEntity);
        rawModel = unwrapFabricForwardingModels(rawModel);
        BakedModel model = getModelType(rawModel);
        if (model == null) return;

        LegacyAnimator animator = LegacyAnimator.create(poseStack);
        com.hbm_m.client.render.LightSampleCache.BASE_POSE.get().set(poseStack.last().pose());
        // Arm the fast local light-matrix path (same contract as
        // AbstractPartBasedRenderer): consumers (InstancedStaticPartRenderer etc.)
        // only read BASE_POSE while this flag is true. Reset in the finally below.
        com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(true);
        machineBasePoseScratch.set(poseStack.last().pose());
        machineBaseNormalScratch.set(poseStack.last().normal());
        try {
            setupBlockTransform(animator, blockEntity);
            renderParts(blockEntity, model, animator, partialTick, packedLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource);
        } finally {
            poseStack.last().pose().set(machineBasePoseScratch);
            poseStack.last().normal().set(machineBaseNormalScratch);
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
        // Двери: per-frame sync share-состояния ДО фазы 1 (roster-assert читает кеш-ключи,
        // зависящие от shareEpoch). Frame-stamped -- один расчёт на кадр.
        if (beRaw instanceof com.hbm_m.block.entity.doors.DoorBlockEntity) {
            com.hbm_m.client.render.implementations.MachineDoorRenderer.syncShareState();
        }

        if (spec.isPurelyStatic()
                && com.hbm_m.client.render.retained.RetainedInstanceGraph.get().isMachineStaticAndRetained(beRaw)) {
            // The retained graph has no TTL of its own: its light slices refresh
            // only on render-dirty events and light-network section dirt. The
            // tracker's TTL horizon is the safety net for events that miss both
            // (same contract as the streamed fast path below) - honor it here by
            // requesting a sparse LIGHT slice re-sample. The machine STAYS
            // retained (falling through would double-draw it against the GPU
            // graph); only the light/fade record is rebuilt. The tracker is then
            // re-stamped so the refresh fires once per TTL window, not per frame.
            if (beRaw instanceof com.hbm_m.api.render.RenderDirtyTracker tracker
                    && tracker.isRenderStale(gameTick, com.hbm_m.client.render.NucleusRenderVersion.worldGen())) {
                com.hbm_m.client.render.retained.RetainedInstanceGraph.get()
                        .onMachineDirty(beRaw, com.hbm_m.client.render.retained.DirtyMask.LIGHT);
                tracker.onRenderCollected(gameTick, com.hbm_m.client.render.NucleusRenderVersion.worldGen());
            }
            com.hbm_m.client.render.NucleusDebug.recordMachineRendered();
            return true;
        }
        // Dynamic-animator parts (Frame quads, door panels: the pose follows
        // state that advances without a render-dirty event) are handled in
        // phase 1 below - they fall back to the PARTIAL fast path, so their
        // animator keeps running in the parts loop while the instanced parts
        // around them assert. (This used to be a spec-level veto that
        // disabled the fast path for EVERY machine of the spec - the
        // 5000-idle-assembler profile, spark 2026-10-07: the spec's constant
        // -animator Frame part alone cost the fleet the whole roster assert.)
        if (ShaderCompatibilityDetector.isRenderingShadowPass()
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
        // Visibility pre-gate: with the GPU culler active it owns visibility -
        // the CPU gate is a redundant frustum test on the same memoized AABB
        // (a retained instance outside the view stays in the batch for the GPU
        // frustum/Hi-Z compaction). Without GPU culling (GPU unsupported, OFF,
        // external shader, vanilla-immediate fallback) the CPU gate remains:
        // the GPU culler only saves raster work - without a CPU gate the
        // collection (roster asserts, record rebuilds, light) runs for every
        // machine in range even when looking away from the farm. One
        // cubeInFrustum on the cached bounds; out-of-frustum machines are
        // dropped from the batch and re-collected the frame they re-enter
        // (newcomer path).
        if (!(com.hbm_m.client.render.culling.GpuCullCoverage.isActive()
                || (ShaderCompatibilityDetector.isExternalShaderActive()
                    && com.hbm_m.client.render.NucleusGpuBaker.isEnabled()))
                && !isMachineInViewFrustum((T) beRaw, frameBounds(beRaw))) {
            return false;
        }
        if (!(beRaw instanceof com.hbm_m.api.render.RenderDirtyTracker tracker)) {
            return false;
        }
        if (tracker.isRenderStale(gameTick, com.hbm_m.client.render.NucleusRenderVersion.worldGen())) {
            return false;
        }
        // Specs with per-state dynamic geometry or tinted parts can never be
        // FULLY fast-asserted (their draw lives in the parts loop) - go
        // straight to the partial path instead of re-resolving every part
        // renderer per machine per frame (that walk was 7% of the frame on the
        // 5000-machine farm, spark 2026-10-08). The frozen walk in renderAll
        // asserts everything assertable; the parts loop draws the rest.
        if (spec.hasNonAssertableParts()) {
            partialAssertBE = beRaw;
            return false;
        }
        // Animation zone: the full path is required only for per-frame content -
        // the animators of animated parts and immediate hooks (fluids/items/
        // diamonds). A machine without them takes the full fast path even inside
        // the zone; otherwise - partial: static parts are confirmed by the roster
        // in renderAll (no matrices/light/float comparison), removing most of the
        // per-frame cost of the zone.
        // Exception - frozen animation (epoch): animated parts are confirmed too,
        // no full collection needed at all. Since 2026-10-07 a QUIET hook (one
        // reporting no content via MachineRenderHook.mayDrawContent) no longer
        // vetoes the full assert: on a 5000-machine idle assembler farm the
        // recipe-icon hook forced the full path for every machine inside the
        // zone (56% of the frame in collectMain, spark 2026-10-07). The epoch
        // covers the content state (contract in mayDrawContent), so the frame
        // content appears the epoch flips and the full path draws it.
        double distSq = frameDistSq(beRaw);
        float staticFade = RenderDistanceHelper.computeStaticFade(beRaw, distSq);
        if (staticFade < 0) {
            return false;
        }

        BlockPos pos = beRaw.getBlockPos();
        long posKey = pos.asLong();
        MachineRenderState cachedState =
                (statesWorldGen == com.hbm_m.client.render.NucleusRenderVersion.worldGen())
                        ? states.get(posKey) : null;

        double animDist = RenderDistanceHelper.getAnimatedDistanceBlocks();
        boolean epochFullAssert = false;
        if (animDist > 0 && distSq <= animDist * animDist) {
            if (!spec.hooks().isEmpty() || hasAnimatedParts() || hasParametricParts()) {
                boolean hooksQuiet = true;
                for (int hi = 0; hi < spec.hooks().size(); hi++) {
                    if (spec.hooks().get(hi).mayDrawContent((T) beRaw)) {
                        hooksQuiet = false;
                        break;
                    }
                }
                if (!hooksQuiet) {
                    partialAssertBE = beRaw;
                    return false;
                }
                if (epochStable(beRaw, gameTick, posKey)) {
                    epochFullAssert = true;
                } else if (cachedState != null && cachedState.allAsserted(true)) {
                    // Actively animating parametric machine (epoch rolling) with
                    // quiet hooks: every part is static (ASSERT) or GPU-parametric
                    // (ASSERT_EPOCH) - static pose/bbox/light are already in the
                    // buffer, so only the 4-float AnimParams tail changes per
                    // frame. Update it in place per cached slot: NO collectRender,
                    // setupBlockTransform, light sampling, getPartModels or
                    // addInstance (that loop was 10.62 ms/frame on the animated
                    // 5000-assembler farm, spark 2026-10-15).
                    float partialTick = com.hbm_m.platform.RenderHooks.getPartialTick();
                    if (tryFastParametricRosterUpdate((T) beRaw, cachedState, posKey, gameTick, partialTick)) {
                        return true;
                    }
                    partialAssertBE = beRaw;
                    return false;
                } else {
                    partialAssertBE = beRaw;
                    return false;
                }
            }
        }

        // Fast path: a frozen MachineRenderState roster from the regular walk
        // (renderAll) encodes exactly the classification phase 1 below re-derives
        // per part. Entries survive until the worldGen bump (invalidated in
        // renderAll), teardown hook or a failed frozen walk - the same guards the
        // walk itself trusts. Machines with any non-assertable part never get
        // allAsserted() and keep the loop path. (cachedState was resolved above,
        // before the anim-zone branch - the fast parametric update uses it too.)
        if (cachedState != null && cachedState.allAsserted(epochFullAssert)
                && tryFrozenRosterAssert(cachedState, pos, posKey, epochFullAssert)) {
            com.hbm_m.client.render.NucleusDebug.recordMachineRendered();
            return true;
        }

        // Phase 1 (read-only): resolve renderers + roster check. Any part that
        // cannot be confirmed falls back to the PARTIAL fast path (the roster
        // walk in renderAll asserts the confirmable parts, the parts loop
        // rebuilds the rest). A bare refusal would rebuild EVERY part of the
        // machine every frame - on the idle assembler farm one non-instanced
        // Frame part did exactly that to all 5000 machines (spark 2026-10-07).
        fastAssertScratchCount = 0;
        boolean partialFallback = false;
        java.util.List<MachineSpec.PartDef<T>> specParts = spec.parts();
        for (int pIdx = 0; pIdx < specParts.size(); pIdx++) {
            MachineSpec.PartDef<T> part = specParts.get(pIdx);
            // Without an epoch, animated parts are skipped, exactly as in renderAll.
            if (part.animated() && !epochFullAssert) continue;
            // Parametric parts: their record words are provably stable ONLY under
            // the epoch gate (epochFullAssert). Outside it a segment edge would
            // never reach the buffer - a crafting ring/arm rig would keep the
            // stale segment phase forever (latent freeze, 2026-10-07).
            if (part.parametric() != null && !epochFullAssert) {
                partialFallback = true;
                break;
            }
            // Time-varying dynamic animators (door panels, per-state quads):
            // their draw lives in the parts loop - the partial path runs it
            // while the instanced parts around them assert.
            if (part.animator() != null && part.dynamic() && !epochFullAssert) {
                partialFallback = true;
                break;
            }
            // Tint-bearing parts are never assertable (see the roster build):
            // a tint change without a consumed dirty event (turbofan glow decay)
            // would otherwise stay frozen in the confirmed buffer record. The
            // parts loop re-resolves the tint; an unchanged tint is a skip-write.
            if (part.tintOverride() != null) {
                partialFallback = true;
                break;
            }
            String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, (T) beRaw) : null;
            MachinePartRenderer r = spec.findExistingRenderer(pIdx, part, dynKey);
            if (r == null) {
                partialFallback = true; // renderer cache wiped - the loop rebuilds it
                break;
            }
            InstancedStaticPartRenderer inst = r.instanced();
            if (inst == null) {
                if (r.hasGeometry()) {
                    partialFallback = true; // single-VBO/immediate part - drawn in the loop
                    break;
                }
                continue; // empty part - renderAll skips it too
            }
            if (!inst.isInitialized() || !inst.canAssertInstance(posKey)) {
                partialFallback = true; // the loop's rebuild repairs the slot sequence
                break;
            }
            if (fastAssertScratchCount >= fastAssertScratch.length) {
                fastAssertScratch = java.util.Arrays.copyOf(fastAssertScratch, fastAssertScratch.length * 2);
            }
            fastAssertScratch[fastAssertScratchCount++] = inst;
        }
        if (partialFallback) {
            partialAssertBE = beRaw;
            return false;
        }

        // Phase 2 (commit): state did not change between phases (single-threaded
        // rendering) - the assert cannot fail.
        for (int i = 0; i < fastAssertScratchCount; i++) {
            fastAssertScratch[i].assertCleanInstance(pos);
        }

        if (cachedState == null || cachedState.rosterCategory == null) {
            if (statesWorldGen != com.hbm_m.client.render.NucleusRenderVersion.worldGen()) {
                states.clear();
                statesWorldGen = com.hbm_m.client.render.NucleusRenderVersion.worldGen();
            }
            MachineRenderState st = stateFor(posKey);
            int pSize = specParts.size();
            st.rosterCategory = new byte[pSize];
            st.rosterRenderers = new MachinePartRenderer[pSize];
            st.rosterSlots = new int[pSize];
            java.util.Arrays.fill(st.rosterSlots, -1);
            for (int pIdx = 0; pIdx < pSize; pIdx++) {
                MachineSpec.PartDef<T> part = specParts.get(pIdx);
                if (part.animated() && part.parametric() == null) continue;
                String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, (T) beRaw) : null;
                MachinePartRenderer r = spec.findExistingRenderer(pIdx, part, dynKey);
                if (r != null && r.instanced() != null) {
                    st.rosterCategory[pIdx] = part.parametric() != null
                            ? MachineRenderState.ASSERT_EPOCH
                            : MachineRenderState.ASSERT;
                    st.rosterRenderers[pIdx] = r;
                    st.rosterSlots[pIdx] = r.instanced().getSlot(posKey);
                }
            }
            st.computeRosterFlags();
        }
        // THE TRACKER STAMP IS NOT UPDATED HERE: onRenderCollected from every
        // successful assert would keep the stamp fresh every frame, and the
        // TTL rebuild (the only owner of light/fade updates without an event
        // model) would never fire. Light/fade are refreshed by a full collection
        // on TTL (isRenderStale), worldGen change, dirty flag, or fade quantum
        // change (checked above).
        com.hbm_m.client.render.NucleusDebug.recordMachineRendered();
        return true;
    }

    /**
     * SHADOW-EPOCH (Iris shadow pass): consume the machine's main-pass instance
     * records as prebaked shadow records instead of re-running the full CPU
     * collection. GENERIC: the decision is purely spec-driven - no per-machine-type
     * branches. FAIL-CLOSED: any uncertainty (hooks present, a drawn part without an
     * initialized instanced renderer, incomplete/stale record coverage, any throw)
     * returns false and the caller runs the normal shadow BER, whose records also
     * calibrate the prebake ({@code IrisShadowBatchCollector}, SHADOW-EPOCH section).
     */
    @SuppressWarnings("unchecked")
    public boolean tryConsumePrebakedShadow(BlockEntity beRaw) {
        try {
            if (beRaw instanceof com.hbm_m.block.entity.doors.DoorBlockEntity) {
                com.hbm_m.client.render.implementations.MachineDoorRenderer.syncShareState();
            }
            if ((!IrisShadowBatchCollector.prebakeCalibrated() && !IrisShadowBatchCollector.isAnchorSpaceShadowActive())
                    || ClientRenderFlags.forceVanillaImmediate()
                    || !ClientRenderFlags.useInstancedBatching()) {
                return false;
            }
            // A just-broken machine must not cast its prebaked shadow: the snapshot
            // published by the previous main pass is still epoch-fresh during this
            // shadow pass (the epoch bumps only at the next main-pass frame start),
            // and nothing else here gates on removal. The dispatcher should not call
            // a removed BE at all - belt and braces, fail closed.
            if (beRaw.isRemoved()) {
                return false;
            }
            if (IrisShadowBatchCollector.isAnchorSpaceShadowActive()) {
                if (!spec.hasDynamicParts() && beRaw instanceof com.hbm_m.client.render.NucleusTrackedBlockEntity tracked && tracked.hbm_m$isNucleusRegistered()) {
                    long pk = beRaw.getBlockPos().asLong();
                    MachineRenderState st = (statesWorldGen == com.hbm_m.client.render.NucleusRenderVersion.worldGen())
                            ? states.get(pk) : null;
                    if (st != null && st.rosterRenderers != null && st.rosterRenderers.length > 0) {
                        for (MachinePartRenderer r : st.rosterRenderers) {
                            if (r != null) {
                                InstancedStaticPartRenderer inst = r.instanced();
                                if (inst != null && IrisShadowBatchCollector.hasFreshPrebake(inst, pk)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            T be = (T) beRaw;
            BlockPos pos = beRaw.getBlockPos();
            long posKey = pos.asLong();
            // Same animated-part gate renderAll applies in the shadow pass: animated
            // parts beyond the animation zone draw in neither path - no record needed.
            boolean animatedVisible =
                    RenderDistanceHelper.computeAnimatedFade(beRaw, frameDistSq(beRaw)) >= 0;
            MachineRenderState state = (statesWorldGen == com.hbm_m.client.render.NucleusRenderVersion.worldGen())
                    ? states.get(posKey) : null;
            MachinePartRenderer[] rosterRenderers = (state != null && state.rosterRenderers != null)
                    ? state.rosterRenderers : null;

            BakedModel model = null;
            BakedModel[] partModels = null;
            boolean modelsResolved = false;

            java.util.List<MachineSpec.PartDef<T>> parts = spec.parts();
            // fastAssertScratch is free here: tryFastAssertRender is gated off in the
            // shadow pass, and the render thread owns both call sites.
            fastAssertScratchCount = 0;
            for (int i = 0; i < parts.size(); i++) {
                MachineSpec.PartDef<T> part = parts.get(i);
                MachinePartRenderer r = (rosterRenderers != null && i < rosterRenderers.length && !part.dynamic())
                        ? rosterRenderers[i] : null;
                if (r == null) {
                    if (!modelsResolved) {
                        model = spec.modelResolver().apply(be);
                        partModels = spec.getPartModels(model);
                        modelsResolved = true;
                    }
                    // Mirror renderAll's skip: a part without a resolved model (and not
                    // dynamic) draws nothing in either pass.
                    BakedModel partModel = partModels != null ? partModels[i] : spec.partModel(part, model);
                    if (partModel == null && !part.dynamic()) continue;
                    String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, be) : null;
                    r = spec.findExistingRenderer(i, part, dynKey);
                    if (r == null) {
                        return false;
                    }
                }
                if (!r.hasGeometry()) continue;
                InstancedStaticPartRenderer inst = r.instanced();
                if (inst == null || !inst.isInitialized()) {
                    // single-VBO / immediate part: real shadow content the prebake
                    // cannot represent - run the normal shadow BER.
                    return false;
                }
                if (part.animated() && !animatedVisible) continue;
                if (!IrisShadowBatchCollector.hasFreshPrebake(inst, posKey)) {
                    return false;
                }
                if (fastAssertScratchCount >= fastAssertScratch.length) {
                    fastAssertScratch = java.util.Arrays.copyOf(fastAssertScratch, fastAssertScratch.length * 2);
                }
                fastAssertScratch[fastAssertScratchCount++] = inst;
            }
            if (IrisShadowBatchCollector.isAnchorSpaceShadowActive()) {
                return true;
            }
            // Every drawn part is provably covered - convert. consumePrebaked re-checks
            // its own preconditions before the first append; after the loop above they
            // hold for every renderer (single render thread, no state change between).
            // Cross-renderer atomicity is enforced, not assumed: consumePrebaked rolls
            // back only ITS renderer, so a later failure would leave earlier renderers'
            // records doubled by the fallback re-record - capture the pre-consume entry
            // counts and truncate them back on any failure.
            if (shadowPreCountsScratch.length < fastAssertScratchCount) {
                shadowPreCountsScratch = new int[Math.max(fastAssertScratchCount, shadowPreCountsScratch.length * 2)];
            }
            for (int i = 0; i < fastAssertScratchCount; i++) {
                shadowPreCountsScratch[i] = IrisShadowBatchCollector.entryCountFor(fastAssertScratch[i]);
            }
            for (int i = 0; i < fastAssertScratchCount; i++) {
                if (!IrisShadowBatchCollector.consumePrebaked(fastAssertScratch[i], posKey, pos)) {
                    for (int j = 0; j < i; j++) {
                        IrisShadowBatchCollector.truncateEntryTo(fastAssertScratch[j], shadowPreCountsScratch[j]);
                    }
                    return false;
                }
            }
            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[MachineRenderers:{}] prebaked shadow consume failed", spec.id(), t);
            return false;
        }
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
        // Basis for hook part transforms (B^-1): the stack here carries the
        // block-transform basis - setupBlockTransform ran in doCollectRender /
        // AbstractPartBasedRenderer.render before renderParts. Captured once per
        // machine per frame (per pass: shadow and main bases differ, but B^-1
        // cancels whichever base the saved poses carry). The only consumers are
        // saveTransform/saveParametricTransform and every call site of those is
        // gated on a hook actually reporting content this frame - without
        // content (the idle assembler fleet's empty recipe icon) the capture
        // (an invertAffine per machine per frame) buys nothing, so it is
        // skipped.
        ctx.hooksDrawContent = false;
        for (int hi = 0; hi < spec.hooks().size(); hi++) {
            if (spec.hooks().get(hi).mayDrawContent(blockEntity)) {
                ctx.hooksDrawContent = true;
                break;
            }
        }
        if (ctx.hooksDrawContent) {
            ctx.captureBasis(poseStack.last().pose());
        }

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
        // Create contraptions bypass the distance fade entirely (parity with
        // RenderDistanceHelper.computeFade) - their records get FADE_MODE_NONE.
        boolean contraptionRender = com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(blockEntity);
        long gameTime = frameGameTime();
        // Animated-part roster asserts (epoch/throttle below) are MAIN-pass,
        // instancing-only machinery:
        //  * in the Iris shadow pass assertCleanInstance would bump the MAIN-pass
        //    instance buffer (count 0->1) while emitting NO shadow collector
        //    record - the part drops out of the shadow map for that frame and the
        //    shifted slot layout poisons the main pass with a ghost record
        //    (double-image/z-fight, camera-rotation dependent);
        //  * under forceVanillaImmediatePath the assert resurrects last frame's
        //    instanced record, so animated parts escape the immediate fallback.
        // Gated parts always fall through to the animator + enqueue, in both passes.
        boolean rosterAssertsAllowed = !ShaderCompatibilityDetector.isRenderingShadowPass()
                && !ClientRenderFlags.forceVanillaImmediate();
        // Animation-epoch: frozen animation - animated parts are confirmed by the
        // roster without running the animators (spec contract: epoch equality
        // means the pose is identical at any partialTick).
        boolean epochStableNow = rosterAssertsAllowed && epochStable(blockEntity, gameTime, rosterKey);
        boolean anyAnimatedRebuilt = false;
        // No distance throttle on contraption renders: a Sable ship's BE sits in
        // plot-grid chunks ~160k+ blocks out, so the band math would throttle the
        // animated parts to every 8th frame while the player stands next to them.
        boolean animationThrottled = rosterAssertsAllowed && !contraptionRender && isAnimationThrottled(blockEntity);
        // -- Frozen roster walk setup (see the MachineRenderState javadoc) ---------
        long worldGenNow = com.hbm_m.client.render.NucleusRenderVersion.worldGen();
        // TTL-driven full rebuild (light/fade refresh): animated parts must rebuild
        // TOGETHER with the statics this frame. Asserting them here would freeze
        // their records at the previous pose/light while the statics take the fresh
        // light - the machine flips between two mixed states every TTL phase
        // (periodic wobble of idle far machines). isRenderStale is still true
        // during renderAll: the tracker stamp is consumed in collectRender's
        // finally, after the parts loop.
        boolean ttlRebuild = rosterAssertsAllowed
                && blockEntity instanceof com.hbm_m.api.render.RenderDirtyTracker ttlTracker
                && ttlTracker.isRenderStale(gameTime, worldGenNow);
        if (ttlRebuild) {
            epochStableNow = false;
            animationThrottled = false;
        }
        if (statesWorldGen != worldGenNow) {
            // Chunk reload / render-origin shift: drop stale per-machine state
            // (after the shift the same positions may belong to different
            // chunks/machines).
            states.clear();
            statesWorldGen = worldGenNow;
        }
        MachineRenderState state = states.get(rosterKey);
        MachineRenderState roster = partialAssert ? state : null;
        if (roster != null && ttlRebuild) {
            // TTL frame: the whole point is a fresh light/fade rebuild - a
            // frozen walk here would confirm the stale-light records instead
            // (the statics would keep last TTL's light for another cycle).
            // Only the roster section drops - the epoch/param/anim sections
            // stay valid (the same scope the old rosterCache.remove had).
            state.dropRoster();
            roster = null;
        }
        boolean staticsFrozen = false;
        if (roster != null) {
            staticsFrozen = tryFrozenRosterAssert(roster, blockPos, rosterKey, epochStableNow);
            com.hbm_m.client.render.NucleusDebug.recordWalkAttempt(staticsFrozen);
            if (!staticsFrozen) {
                state.dropRoster();
                roster = null;
            }
        }
        boolean buildRoster = roster == null;
        if (buildRoster && state == null) {
            state = stateFor(rosterKey);
        }
        if (buildRoster) {
            int pSize = spec.parts().size();
            state.rosterCategory = new byte[pSize];
            state.rosterRenderers = new MachinePartRenderer[pSize];
            state.rosterSlots = new int[pSize];
            java.util.Arrays.fill(state.rosterSlots, -1);
        }

        // Shared light: when batching, one 8-corner sample per machine per frame;
        // all parts reuse it (saves getLightColor calls on farms).
        // In the shadow pass with the global batch on, light is not needed at all
        // (shadow FB - depth/plain color): skip the sample entirely.
        float[] sharedLight = null;
        boolean shadowNoLight = ShaderCompatibilityDetector.isRenderingShadowPass()
                && IrisShadowBatchCollector.isBatchingEnabled();
        if (!shadowNoLight && ClientRenderFlags.useInstancedBatching()) {
            net.minecraft.world.phys.AABB localBox = com.hbm_m.client.render.RenderBoundsCache.getLocalPhysicalBounds(blockEntity);
            if (localBox != null && localBox.maxX > localBox.minX && localBox.maxY > localBox.minY && localBox.maxZ > localBox.minZ) {
                sharedLightBbox[0] = (float) localBox.minX;
                sharedLightBbox[1] = (float) localBox.minY;
                sharedLightBbox[2] = (float) localBox.minZ;
                sharedLightBbox[3] = (float) localBox.maxX;
                sharedLightBbox[4] = (float) localBox.maxY;
                sharedLightBbox[5] = (float) localBox.maxZ;
            } else {
                sharedLightBbox[0] = 0f;
                sharedLightBbox[1] = 0f;
                sharedLightBbox[2] = 0f;
                sharedLightBbox[3] = 1f;
                sharedLightBbox[4] = 1f;
                sharedLightBbox[5] = 1f;
            }
            machineBasePoseScratch.invert(sharedLightPose).mul(poseStack.last().pose());
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
        long timeKey = 0L;
        boolean timeKeyReady = false;
        boolean baseReady = false;
        // Frozen parametric eval-skip: the machine's current epoch for this
        // renderAll pass, computed lazily on the first parametric part (the
        // provider is cheap but not free; machines without parametric parts
        // never pay it). MIN_VALUE = no epoch or provider error: the word
        // cache is bypassed entirely.
        long paramEpoch = Long.MIN_VALUE;
        boolean paramEpochReady = false;
        java.util.List<MachineSpec.PartDef<T>> parts = spec.parts();
        BakedModel[] resolvedPartModels = spec.getPartModels(model);

        if (staticsFrozen && !ctx.hooksDrawContent && roster.allAsserted(epochStableNow)) {
            return;
        }

        // Hooks consume the per-part base pose (saveTransform); for asserted parts
        // that pose is the shared block-transform base (the static animator is
        // constant and is not run) - save it per part without recomputing anything.
        // Must run before the loop mutates the stack (REBUILD parts pushPose).
        if (staticsFrozen && ctx.hooksDrawContent) {
            var basePose = poseStack.last().pose();
            for (int i = 0; i < roster.rosterCategory.length; i++) {
                byte cat = roster.rosterCategory[i];
                if (cat == MachineRenderState.ASSERT || (cat == MachineRenderState.ASSERT_EPOCH && epochStableNow)) {
                    ctx.saveTransform(parts.get(i).name(), basePose);
                }
            }
        }
        for (int partIdx = 0; partIdx < parts.size(); partIdx++) {
            MachineSpec.PartDef<T> part = parts.get(partIdx);
            // Frozen roster walk: this part was already confirmed this frame (two
            // array passes before the loop) - skip it entirely. ASSERT_EPOCH
            // parts (parametric under a stable epoch) participate only while the
            // epoch is stable; an unstable epoch rebuilds them below.
            if (staticsFrozen
                    && (roster.rosterCategory[partIdx] == MachineRenderState.ASSERT
                        || (epochStableNow && roster.rosterCategory[partIdx] == MachineRenderState.ASSERT_EPOCH))) {
                continue;
            }
            // Only animated CONTENT is skipped; static parts with an "animator"
            // transform (legacy offsets) live up to the static cutoff.
            if (!animatedVisible && part.animated()) continue;
            BakedModel partModel = resolvedPartModels != null ? resolvedPartModels[partIdx] : spec.partModel(part, model);
            // Lazy build: dynQuads are computed only if the VBO is not built yet
            // (previously the resolver was invoked every frame for every BE - see
            // MachineSpec.partRendererLazy).
            if (partModel == null && !part.dynamic()) continue;
            String dynKey = part.dynamic() ? spec.dynamicCacheKeyValue(part, blockEntity) : null;

            MachinePartRenderer renderer = spec.partRendererLazy(partIdx, part, partModel, blockEntity, dynKey);
            if (part.dynamic()) {
                spec.freeDynamicPartSlots(part, rosterKey, dynKey);
            }
            if (!renderer.hasGeometry()) {
                if (renderer.instanced() != null && renderer.instanced().hasSlot(rosterKey)) {
                    renderer.instanced().freeSlot(rosterKey);
                }
                continue;
            }

            // Roster build: classify the part structurally. A static part with an
            // instanced renderer is frozen-roster-assertable; a PARAMETRIC part of
            // an epoch-providing spec is assertable WHILE THE EPOCH IS STABLE
            // (ASSERT_EPOCH - the record words are provably unchanged and the VSH
            // animates them from uAnimTime; the epoch gate in the frozen walk
            // hands them back to this branch the frame the epoch flips).
            // Everything else (animated, dynamic-animator, parametric without an
            // epoch, immediate) rebuilds in the full loop on partial frames.
            // Parts with a per-instance tintOverride are NEVER assertable: the
            // roster assert confirms the record without resolving the tint, so a
            // heat/tint change with no consumed dirty event (turbofan nozzle glow
            // fading to zero) would freeze the old tint in the buffer. The rebuild
            // path costs a 46-float compare with skip-write when unchanged.
            boolean canAssertParametric = part.parametric() != null && spec.animationEpoch() != null;
            if (buildRoster
                    && (!part.animated() || canAssertParametric)
                    && !(part.animator() != null && part.dynamic() && !canAssertParametric)
                    && (part.parametric() == null || spec.animationEpoch() != null)
                    && part.tintOverride() == null
                    && part.clipPlanesResolver() == null
                    && renderer.instanced() != null) {
                state.rosterCategory[partIdx] =
                        part.parametric() != null ? MachineRenderState.ASSERT_EPOCH : MachineRenderState.ASSERT;
                state.rosterRenderers[partIdx] = renderer;
            }

            // -- Parametric GPU animation: a jointed part bypasses the PoseStack
            // animator. Parameters are written to attrib 15 (4 trailing floats of
            // the record), the vertex shader moves the geometry; a frozen joint =
            // skip-write = zero upload. Outside the vanilla MDI path (degradation /
            // Iris / shadow / kill-switch) - the regular CPU animator from the
            // descriptor (legacy fallback below).
            if (part.parametric() != null && isParametricPath(renderer) && part.clipPlanesResolver() == null) {
                MachineSpec.ParametricRig pa = part.parametric();
                // Frozen per-part assert (the roster walk's in-loop fallback):
                // the walk above can fail for the WHOLE machine (stale roster,
                // slot-sequence churn) while THIS part's record words are
                // provably unchanged (epoch contract) - confirm the instance
                // and skip the branch for exactly those parts. The walk's
                // parts-loop skip covers the same case when the walk succeeds.
                if (epochStableNow) {
                    InstancedStaticPartRenderer inst = renderer.instanced();
                    if (inst != null && inst.isInitialized() && inst.canAssertInstance(rosterKey)) {
                        if (inst.assertCleanInstance(blockPos)) {
                            continue;
                        }
                    }
                }
                com.hbm_m.client.render.NucleusDebug.recordParametricBranch();
                // Frozen parametric eval-skip: resolve the machine's current
                // epoch once per pass (MIN_VALUE = no epoch or provider error,
                // never cached).
                if (!paramEpochReady) {
                    paramEpoch = spec.animationEpoch() != null ? currentEpoch(blockEntity) : Long.MIN_VALUE;
                    paramEpochReady = true;
                }
                // Zero the scratch first: the params fns fill LOGICAL values that
                // the rig then packs into the record words - stale values (from
                // other joints or previous frames) would leak into the packed
                // record and break the skip-write convergence. (Skipped on a
                // frozen hit: pr then aliases the cached logical words.)
                float[] pr = paramScratch;
                boolean drawP;
                MachineRenderState pe = paramEpoch != Long.MIN_VALUE ? state : null;
                if (pe != null && pe.hasParamWords(partIdx, paramEpoch, gameTime)) {
                    // Frozen hit: the epoch is unchanged in a later tick - the
                    // contract guarantees the same pose/params at any
                    // partialTick. Reuse the cached words; the params chain and
                    // the binary16 packer are skipped.
                    drawP = pe.paramDrawn(partIdx);
                    if (drawP) {
                        System.arraycopy(pe.paramWords(partIdx), 0, encodedScratch, 0, 4);
                    }
                    pr = pe.paramLogical(partIdx);
                } else {
                    java.util.Arrays.fill(pr, 0f);
                    try {
                        drawP = part.parametricParams().params(blockEntity, partialTick, gameTime, pr);
                    } catch (Throwable t) {
                        com.hbm_m.main.MainRegistry.LOGGER.error(
                                "[MachineRenderers:{}] parametric part '{}' params failed", spec.id(), part.name(), t);
                        drawP = false;
                    }
                    // Record params: logical floats -> packed binary16 words (the
                    // layout the VSH decodes with halfDecode bit math). The lazy
                    // hook entry below copies the LOGICAL pr values.
                    if (drawP) {
                        pa.encodeAnimParams(pr, encodedScratch);
                    }
                    // Cache for the frozen frames (specs with an epoch only; the
                    // provider-error sentinel stays uncached).
                    if (paramEpoch != Long.MIN_VALUE) {
                        MachineRenderState st = state != null ? state : stateFor(rosterKey);
                        st.storeParamWords(partIdx, parts.size(), paramEpoch, gameTime, drawP, pr, encodedScratch);
                    }
                }
                if (drawP) {
                    // Record statics (legacy bake offset, static pre-rotation):
                    // the joint delta is applied by the VSH in the geometry's
                    // local coordinates, and the record must carry the animator's
                    // static transforms MINUS the joint itself - otherwise the
                    // part is displaced by the missing offset. Rigs apply the
                    // baseYaw group + baseOffset ONLY; an animated mount
                    // (turntable) is NOT applied here - per the chain contract it
                    // travels through the record's AnimParams as chain node 0
                    // (chainWithTurntable), so the record statics stay fully
                    // static while the mount turns.
                    boolean hasStaticRigTransform = pa.hasBaseYaw() || pa.hasBaseOffset();
                    if (hasStaticRigTransform) {
                        partPoseScratch.set(poseStack.last().pose());
                        partNormalScratch.set(poseStack.last().normal());
                        pa.applyRecordStatic(poseStack, blockEntity, partialTick);
                    }
                    // The record statics are frame-invariant for both rig kinds:
                    // applyRecordStatic reads only final record constants
                    // (baseYawDegrees/baseOffset - ParametricAnim and
                    // ParametricChain are the only implementers) and the
                    // turntable mount rides as chain node 0. Lets addInstance
                    // skip the posQuat convert + compare prefix.
                    // Pose for hooks = record statics + joint delta, saved LAZILY
                    // (no eager applyToPose): the CPU forward kinematics runs only
                    // if a hook actually queries partTransform for THIS part -
                    // hooks that never ask (the assembler recipe icon draws at a
                    // fixed stanchion point) cost zero. Without hooks - nothing at
                    // all (same as the static paths below).
                    if (ctx.hooksDrawContent) {
                        ctx.saveParametricTransform(part.name(), poseStack.last().pose(), pa, pr);
                    }
                    SingleMeshVboRenderer.setFadeAlpha((part.isConditional() && !part.isVisible(blockEntity)) ? 0.0f : fade);
                    PartEnqueueRequest rq = PartEnqueueRequest.obtain();
                    rq.poseStack = poseStack;
                    rq.blockPos = blockPos;
                    rq.blockEntity = blockEntity;
                    rq.bufferSource = bufferSource;
                    if (part.isConditional() && !part.isVisible(blockEntity)) {
                        rq.fadeMode = InstancedStaticPartRenderer.FADE_MODE_INVISIBLE;
                    } else if (contraptionRender) {
                        rq.fadeMode = InstancedStaticPartRenderer.FADE_MODE_NONE;
                    } else {
                        rq.fadeMode = InstancedStaticPartRenderer.FADE_MODE_MIN_ANIM;
                    }
                    rq.staticPose = pa instanceof MachineSpec.ParametricAnim
                            || pa instanceof MachineSpec.ParametricChain;
                    // The record params reflect the current state (recomputed or
                    // frozen-hit reused) - the epoch slot can be refreshed at
                    // the end of renderAll.
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
                    rq.packedLight = partLight;
                    rq.sharedCornerUV8 = partSharedLight;
                    // The parametric branch must pair the shared 8-corner light with
                    // the SAME bbox the light was sampled at (the non-parametric
                    // branch below does this); without it addInstance paired
                    // sharedLight with objBbox - wrong lightmap interpolation and a
                    // permanent recordMatchesBuffer mismatch vs SparseUpdateQueue.
                    if (partSharedLight != null) {
                        rq.lightBbox = sharedLightBbox;
                    }
                    rq.setAnimParams(
                            encodedScratch[0], encodedScratch[1], encodedScratch[2], encodedScratch[3],
                            jointIndexFor(partIdx, part));
                    try {
                        rq.uvRect = spec.resolveUvRect(part, blockEntity);
                        rq.tint = spec.resolveTint(part, blockEntity);
                        rq.grad = part.tintFalloff();
                        rq.clipPlanes = spec.resolveClipPlanes(part, blockEntity, partialTick);
                        renderer.enqueue(rq);
                    } finally {
                        // A throw in resolveUvRect/resolveTint (evaluated before
                        // enqueue is entered) leaves the request unread - the next
                        // obtain() resets it, nothing leaks into an unrelated
                        // record (the teleport bug class, 10-09).
                        if (hasStaticRigTransform) {
                            poseStack.last().pose().set(partPoseScratch);
                            poseStack.last().normal().set(partNormalScratch);
                        }
                    }
                }
                continue;
            }

            // Animation cache: initialize on the first part with an animator.
            // Key = per-frame counter (shared between shadow/main of one frame).
            // The deltas live in the machine's own MachineRenderState - there is
            // no slot to hash into, so no collision invalidation and no
            // directional mixing of the position key.
            if (part.animator() != null && animCacheOn && !timeKeyReady) {
                timeKey = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
                timeKeyReady = true;
            }

            boolean hasPartTransform = part.animator() != null;
            if (hasPartTransform) {
                partPoseScratch.set(poseStack.last().pose());
                partNormalScratch.set(poseStack.last().normal());
            }
            try {
                boolean draw = true;
                // Partial fast path (machine inside the animation zone): the static
                // part is already in last frame's instance buffer - confirm it with
                // a roster-assert instead of a full rebuild (matrix/light/30-float
                // comparison). The record's fade slot holds a static MODE now (the
                // distance ramp lives on the GPU), so no per-frame fade guard is
                // needed - the record cannot go stale while the camera moves.
                // Pose semantics - as with the full fast path beyond the zone: the
                // static part's animator (legacy offsets, constant pose) is not
                // run, the buffer record is unchanged.
                boolean fastAsserted = false;
                if (partialAssert && !part.animated()
                        && !(part.animator() != null && part.dynamic())
                        && part.tintOverride() == null
                        && part.clipPlanesResolver() == null) {
                    InstancedStaticPartRenderer inst = renderer.instanced();
                    if (inst != null && inst.isInitialized()
                            && inst.canAssertInstance(rosterKey)) {
                        fastAsserted = inst.assertCleanInstance(blockPos);
                    }
                }
                // Frozen animation: the animated part is confirmed by the roster -
                // the pose in the buffer matches the current one (epoch contract).
                // PARAMETRIC parts are exempt: that contract only holds while the
                // VSH advances them from uAnimTime (the parametric branch above).
                // On the CPU fallback path (Iris / shadow / immediate) their pose
                // is recomputed from the clock every frame - asserting under a
                // stable epoch froze the tracked rig mid-leg and snapped it
                // forward when the epoch flipped (the jerky assembler ring).
                if (!fastAsserted && epochStableNow && part.animated() && part.parametric() == null
                        && part.tintOverride() == null
                        && part.clipPlanesResolver() == null) {
                    InstancedStaticPartRenderer inst = renderer.instanced();
                    if (inst != null && inst.isInitialized()
                            && inst.canAssertInstance(rosterKey)) {
                        fastAsserted = inst.assertCleanInstance(blockPos);
                    }
                }
                // Distance throttle (CrankShaft BandedPrimeLimiter analog): far
                // machines re-record animated parts only every Nth frame - in
                // between the roster assert keeps the last pose (a brief freeze
                // at distance, accepted by the reference implementation too).
                // Parametric parts are exempt - see the epoch assert above.
                if (!fastAsserted && part.animated() && part.parametric() == null
                        && animationThrottled && part.tintOverride() == null
                        && part.clipPlanesResolver() == null) {
                    InstancedStaticPartRenderer inst = renderer.instanced();
                    if (inst != null && inst.isInitialized()
                            && inst.canAssertInstance(rosterKey)) {
                        fastAsserted = inst.assertCleanInstance(blockPos);
                    }
                }
                if (!fastAsserted && part.animated()) {
                    // The part went through the animator - the pose was recomputed
                    // this frame, the epoch can be stored at the end of renderAll.
                    anyAnimatedRebuilt = true;
                }
                if (!fastAsserted && part.animator() != null) {
                    boolean hit;
                    if (!animCacheOn) {
                        hit = false;
                        MISS_OFF++;
                    } else if (state.animDelta == null) {
                        hit = false;
                        MISS_STATE++;
                    } else if (state.animFrameKey != timeKey) {
                        // The state was last stamped in an earlier frame.
                        hit = false;
                        MISS_FRAME++;
                    } else if (state.animDelta[partIdx] == null || state.animPartTime[partIdx] != timeKey) {
                        // Stale per-part delta: the slot was re-stamped this frame by
                        // an earlier animator part, but THIS part has not run its
                        // animator yet - it must not reuse the previous frame's pose.
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
                        if (state.animSkip[partIdx]) {
                            draw = false;
                        } else {
                            // F' = S * delta - the current pass's basis on top of
                            // the cached local animation.
                            animComposed.set(animBase).mul(state.animDelta[partIdx]);
                            poseStack.last().pose().set(animComposed);
                        }
                    } else {
                        ANIM_MISSES++;
                        maybeLogAnimCache();
                        draw = part.animator().animate(blockEntity, partialTick, gameTime, poseStack);
                        if (animCacheOn) {
                            animStore(state, partIdx, parts.size(), timeKey,
                                    animBaseInv, poseStack.last().pose(), draw);
                        }
                    }
                }
                if (part.isConditional() && !part.isVisible(blockEntity) && renderer.instanced() == null) {
                    draw = false;
                }
                if (fastAsserted) {
                    // The part's pose is needed by hooks (MachineRenderApi.partTransform)
                    // even when the rebuild is skipped - the base pose (the static
                    // part's animator is constant); save without enqueue.
                    if (ctx.hooksDrawContent) {
                        ctx.saveTransform(part.name(), poseStack.last().pose());
                    }
                } else if (draw) {
                    // Matrices are only needed by hooks (MachineRenderApi.partTransform);
                    // without hooks nothing is allocated.
                    if (ctx.hooksDrawContent) {
                        ctx.saveTransform(part.name(), poseStack.last().pose());
                    }
                    // Per-part fade: fade is written into per-instance data at
                    // addInstance time - statics are faded only by the static
                    // zone, animated parts additionally by the animation zone.
                    // The record stores a static MODE (the ramp itself is GPU-side,
                    // see InstancedStaticPartRenderer.FADE_MODE_*); Create
                    // contraptions bypass the distance fade entirely.
                    SingleMeshVboRenderer.setFadeAlpha((part.isConditional() && !part.isVisible(blockEntity))
                            ? 0.0f
                            : (part.animated() ? fade : staticFade));
                    PartEnqueueRequest rq = PartEnqueueRequest.obtain();
                    rq.poseStack = poseStack;
                    rq.blockPos = blockPos;
                    rq.blockEntity = blockEntity;
                    rq.bufferSource = bufferSource;
                    if (part.isConditional() && !part.isVisible(blockEntity)) {
                        rq.fadeMode = InstancedStaticPartRenderer.FADE_MODE_INVISIBLE;
                    } else if (contraptionRender) {
                        rq.fadeMode = InstancedStaticPartRenderer.FADE_MODE_NONE;
                    } else {
                        rq.fadeMode = part.animated()
                                ? InstancedStaticPartRenderer.FADE_MODE_MIN_ANIM
                                : InstancedStaticPartRenderer.FADE_MODE_STATIC;
                    }
                    // Static-pose skip-put (10-09): mirror the partial fast-path's
                    // frame-invariance condition - lets addInstance skip the
                    // convertToWorldRecord multiply + posQuat compare for every
                    // static part (previously declared only for parametric records).
                    rq.staticPose = !part.animated() && !(part.animator() != null && part.dynamic());
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
                    rq.packedLight = partLight;
                    rq.sharedCornerUV8 = partSharedLight;
                    if (partSharedLight != null) {
                        rq.lightBbox = sharedLightBbox;
                    }
                    // A throw in resolveUvRect/resolveTint leaves the request
                    // unread - the next obtain() resets it, nothing leaks into an
                    // unrelated record (the teleport bug class, 10-09).
                    rq.uvRect = spec.resolveUvRect(part, blockEntity);
                    rq.tint = spec.resolveTint(part, blockEntity);
                    rq.grad = part.tintFalloff();
                    rq.clipPlanes = spec.resolveClipPlanes(part, blockEntity, partialTick);
                    renderer.enqueue(rq);
                } else {
                    if (renderer.instanced() != null && renderer.instanced().hasSlot(rosterKey)) {
                        renderer.instanced().freeSlot(rosterKey);
                    }
                }
            } catch (Throwable t) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] part '{}' render failed",
                        spec.id(), part.name(), t);
            } finally {
                if (hasPartTransform) {
                    poseStack.last().pose().set(partPoseScratch);
                    poseStack.last().normal().set(partNormalScratch);
                }
            }
        }

        // Animation-epoch: after rebuilding the animated parts, store (epoch, tick)
        // - the buffer records now correspond to this pose.
        if (anyAnimatedRebuilt) {
            epochStoreAfterFullAnim(blockEntity, gameTime, rosterKey);
        }

        if (buildRoster) {
            // Pre-resolve slots and the allAsserted flags once per build so the
            // frozen assert path stays hash-lookup-free for the roster's lifetime.
            for (int i = 0; i < state.rosterCategory.length; i++) {
                if (state.rosterCategory[i] != MachineRenderState.REBUILD && state.rosterRenderers[i] != null) {
                    InstancedStaticPartRenderer inst = state.rosterRenderers[i].instanced();
                    if (inst != null) {
                        state.rosterSlots[i] = inst.getSlot(rosterKey);
                    }
                }
            }
            state.computeRosterFlags();
        }

        // -- Hooks: fluids/items/diamonds (immediate); not drawn beyond the
        // animation distance (icons/diamonds are animation cosmetics) ------
        if (!animatedVisible || !ctx.hooksDrawContent) return;
        for (MachineRenderHook<T> hook : spec.hooks()) {
            try {
                hook.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, ctx);
            } catch (Throwable t) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] hook render failed", spec.id(), t);
            }
        }
    }
}
