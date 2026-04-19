package com.hbm_m.client.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

/**
 * Per-frame cache for the smoothed lightmap UV pair sampled at the 6 face
 * centres of a BlockEntity's render bounding box.
 * <p>
 * <b>Why this exists.</b> {@code InstancedStaticPartRenderer.sampleSmoothLightUV}
 * is the most expensive call on the per-machine path under shaders - the
 * profiler attributed roughly 17% of frame time to it on a small Advanced
 * Assembler farm. The work itself (6 × {@code Level.getBlockState} + 6 ×
 * {@code LevelRenderer.getLightColor}) is fundamentally chunky, but it was
 * being repeated <i>per part renderer</i> for the same BlockEntity:
 *
 * <ul>
 *   <li>Advanced Assembler has 11 parts (Base, Frame, 3× Ring, Arm[Bone1..6]).
 *   For each visible machine the BlockEntityRenderer dispatches to 11 different
 *   {@code InstancedStaticPartRenderer.addInstance(...)} calls, each of which
 *   used to sample the lightmap independently for the SAME world-space bbox.</li>
 *   <li>Both the main pass and the shadow pass repeat the dispatch - so the 11×
 *   redundancy doubles to 22× per machine per frame.</li>
 *   <li>For 6 visible machines that adds up to ~792 lightmap lookups per frame
 *   for a single multiblock type, plus the bbox call (which itself can be
 *   non-trivial for dynamic-bbox BEs).</li>
 * </ul>
 *
 * Sharing a single (blockU, skyV) pair across all parts of one BE for the
 * lifetime of one frame collapses that to ~6 lookups per machine per frame -
 * an 11–22× reduction.
 *
 * <p><b>Invalidation strategy.</b> The cache uses a render-frame counter that
 * is bumped from {@code ClientModEvents.onRenderLevelStage(AFTER_BLOCK_ENTITIES)}
 * - i.e. ONCE per fully-rendered frame, after both the shadow pass and the
 * main pass have drained their block-entity dispatches. So a sample taken
 * during the shadow pass is reused during the main pass of the same frame
 * (correct - the world hasn't changed between them) and a fresh sample is
 * taken on the next frame. There is no need for a stronger TTL because the
 * lightmap only changes over multiple ticks anyway.
 *
 * <p>Periodic pruning every 600 frames (~10 seconds) drops entries belonging
 * to BlockEntities that haven't been rendered recently - typical
 * "I walked past a chunk and now it's behind me" case - keeping the cache
 * bounded in long sessions.
 *
 * <p>Single-threaded: lives entirely on the render thread.
 */
@OnlyIn(Dist.CLIENT)
public final class LightSampleCache {

    private static final Long2ObjectOpenHashMap<Entry> CACHE = new Long2ObjectOpenHashMap<>();
    private static long currentFrame = 0L;

    /**
     * Single-slot fast path. Stores the most recently sampled BE so the typical
     * call pattern - Advanced Assembler dispatching {@link #getOrSample} 11
     * times in a row for the same {@code BlockEntity} (one per part renderer)
     * - collapses to a {@code ==} reference compare and two field reads,
     * skipping {@code BlockEntity.getBlockPos().asLong()} and the hash map
     * lookup entirely. The {@code BlockEntity} reference is captured as a
     * weak-ish identity by holding it directly: BEs are render-thread-owned
     * during the dispatch window, so the reference is always live.
     * <p>
     * Cleared by {@link #onFrameStart} (so a new frame can't read a stale
     * entry whose contents would no longer match {@code currentFrame}) and by
     * {@link #invalidateAll} (level swap).
     */
    private static BlockEntity lastQueriedBE = null;
    private static float lastBlockU = 0f;
    private static float lastSkyV = 0f;
    private static long lastFastFrame = -1L;

    /** Reusable scratch position to avoid allocating per sample. */
    private static final BlockPos.MutableBlockPos SAMPLE_POS = new BlockPos.MutableBlockPos();

    /** Reusable face-offsets table (avoids per-call allocation). */
    private static final int[][] FACE_OFFSETS = new int[6][3];

    private static final int PRUNE_EVERY = 600;
    private static final int STALE_AFTER_FRAMES = 60;

    private LightSampleCache() {}

    private static final class Entry {
        float blockU;
        float skyV;
        long lastFrame;
    }

    /**
     * Bumps the frame counter; subsequent {@link #getOrSample} calls will
     * resample once per BlockEntity rather than reusing the previous frame's
     * value. Periodically prunes stale entries.
     */
    public static void onFrameStart() {
        currentFrame++;
        // Drop the fast-path slot - its frame stamp is now stale, and a new
        // frame must re-sample anyway. Keeping the BE reference live across
        // frames would also pin disposed BEs in memory.
        lastQueriedBE = null;
        lastFastFrame = -1L;
        if ((currentFrame % PRUNE_EVERY) == 0) {
            CACHE.long2ObjectEntrySet().removeIf(e ->
                e.getValue().lastFrame < currentFrame - STALE_AFTER_FRAMES);
        }
    }

    /**
     * Returns the smoothed lightmap UV pair for the supplied BlockEntity,
     * sampling lazily on the first call within a frame and reusing the cached
     * value for all subsequent calls within the same frame.
     *
     * <p>Falls back to {@code packedLightFallback} when:
     * <ul>
     *   <li>{@code be} is null or detached from a level;</li>
     *   <li>the bounding box is non-finite (some BEs return AABB.INFINITE);</li>
     *   <li>all 6 face positions are themselves opaque (machine fully buried).</li>
     * </ul>
     *
     * @param be the BlockEntity providing world + bbox
     * @param packedLightFallback vanilla packed light to use if sampling fails
     * @param outUV destination array, written as {@code outUV[outBase] = blockU,
     *              outUV[outBase+1] = skyV} on the same 0..240 scale that
     *              {@code BufferBuilder.uv2} writes
     * @param outBase offset into {@code outUV}
     */
    public static void getOrSample(@Nullable BlockEntity be, int packedLightFallback,
                                   float[] outUV, int outBase) {
        if (be == null) {
            outUV[outBase]     = (float) (packedLightFallback & 0xFFFF);
            outUV[outBase + 1] = (float) ((packedLightFallback >>> 16) & 0xFFFF);
            return;
        }

        // Fast path: same BE as the previous call within the same frame.
        // Hits ~10/11 times for the Advanced Assembler's per-part dispatch
        // and ~5/6 times for Chemical Plant - that's the bulk of the
        // calls into this cache. We avoid `getBlockPos().asLong()` AND the
        // hashmap lookup AND the Entry field reads.
        if (be == lastQueriedBE && lastFastFrame == currentFrame) {
            outUV[outBase]     = lastBlockU;
            outUV[outBase + 1] = lastSkyV;
            return;
        }

        long key = be.getBlockPos().asLong();
        Entry cached = CACHE.get(key);
        if (cached != null && cached.lastFrame == currentFrame) {
            outUV[outBase]     = cached.blockU;
            outUV[outBase + 1] = cached.skyV;
            // Promote into fast-path slot for the next 10× burst.
            lastQueriedBE = be;
            lastBlockU = cached.blockU;
            lastSkyV = cached.skyV;
            lastFastFrame = currentFrame;
            return;
        }

        // Sample now and store back into the cache (re-using the existing
        // Entry slot when present to skip the allocation).
        sampleSmoothLightUV(be, packedLightFallback, outUV, outBase);

        if (cached == null) {
            cached = new Entry();
            CACHE.put(key, cached);
        }
        cached.blockU = outUV[outBase];
        cached.skyV   = outUV[outBase + 1];
        cached.lastFrame = currentFrame;

        // Same promotion - and since we sampled fresh, the fast-path slot
        // matches `cached` exactly.
        lastQueriedBE = be;
        lastBlockU = cached.blockU;
        lastSkyV = cached.skyV;
        lastFastFrame = currentFrame;
    }

    /**
     * The expensive sampling routine - extracted from
     * {@code InstancedStaticPartRenderer.sampleSmoothLightUV} so that all call
     * sites (instanced batch path, single-instance Iris path, future
     * non-instanced fallbacks) share one implementation and benefit from this
     * cache uniformly.
     */
    private static void sampleSmoothLightUV(BlockEntity be, int packedLightFallback,
                                            float[] outUV, int outBase) {
        Level level = be.getLevel();
        if (level == null) {
            outUV[outBase]     = (float) (packedLightFallback & 0xFFFF);
            outUV[outBase + 1] = (float) ((packedLightFallback >>> 16) & 0xFFFF);
            return;
        }

        AABB bounds;
        try {
            bounds = be.getRenderBoundingBox();
        } catch (Throwable t) {
            bounds = null;
        }
        if (bounds == null || !Double.isFinite(bounds.minX) || !Double.isFinite(bounds.maxX)) {
            outUV[outBase]     = (float) (packedLightFallback & 0xFFFF);
            outUV[outBase + 1] = (float) ((packedLightFallback >>> 16) & 0xFFFF);
            return;
        }

        // One block past the bounding box in each cardinal direction (±X, ±Y, ±Z).
        // The mid* coordinates sit at the structure centre on the orthogonal axes
        // so the sample lies in front of the geometric face, not at one of the
        // bbox edges.
        int xLo = Mth.floor(bounds.minX) - 1;
        int xHi = Mth.floor(bounds.maxX - 1.0E-7) + 1;
        int yLo = Mth.floor(bounds.minY) - 1;
        int yHi = Mth.floor(bounds.maxY - 1.0E-7) + 1;
        int zLo = Mth.floor(bounds.minZ) - 1;
        int zHi = Mth.floor(bounds.maxZ - 1.0E-7) + 1;
        int xMid = (xLo + xHi) >> 1;
        int yMid = (yLo + yHi) >> 1;
        int zMid = (zLo + zHi) >> 1;

        FACE_OFFSETS[0][0] = xLo;  FACE_OFFSETS[0][1] = yMid; FACE_OFFSETS[0][2] = zMid;
        FACE_OFFSETS[1][0] = xHi;  FACE_OFFSETS[1][1] = yMid; FACE_OFFSETS[1][2] = zMid;
        FACE_OFFSETS[2][0] = xMid; FACE_OFFSETS[2][1] = yLo;  FACE_OFFSETS[2][2] = zMid;
        FACE_OFFSETS[3][0] = xMid; FACE_OFFSETS[3][1] = yHi;  FACE_OFFSETS[3][2] = zMid;
        FACE_OFFSETS[4][0] = xMid; FACE_OFFSETS[4][1] = yMid; FACE_OFFSETS[4][2] = zLo;
        FACE_OFFSETS[5][0] = xMid; FACE_OFFSETS[5][1] = yMid; FACE_OFFSETS[5][2] = zHi;

        long totalBlock = 0;
        long totalSky = 0;
        int n = 0;

        for (int i = 0; i < 6; i++) {
            int wx = FACE_OFFSETS[i][0];
            int wy = FACE_OFFSETS[i][1];
            int wz = FACE_OFFSETS[i][2];
            SAMPLE_POS.set(wx, wy, wz);

            // Skip face positions inside opaque blocks: light cannot propagate
            // into them so the sample would be 0 and would only drag the average
            // down. This is precisely the case where a multiblock is stacked on
            // top of another, buried in dirt, or sandwiched between walls - and
            // it is the difference between "lower machine pitch black" and
            // "lower machine matches its neighbours".
            BlockState state;
            try {
                state = level.getBlockState(SAMPLE_POS);
            } catch (Throwable t) {
                continue;
            }
            if (state.isSolidRender(level, SAMPLE_POS)) continue;

            int packed = LevelRenderer.getLightColor(level, SAMPLE_POS);
            totalBlock += (packed & 0xFFFF);
            totalSky   += ((packed >>> 16) & 0xFFFF);
            n++;
        }

        if (n == 0) {
            outUV[outBase]     = (float) (packedLightFallback & 0xFFFF);
            outUV[outBase + 1] = (float) ((packedLightFallback >>> 16) & 0xFFFF);
            return;
        }

        outUV[outBase]     = (float) totalBlock / (float) n;
        outUV[outBase + 1] = (float) totalSky   / (float) n;
    }

    /**
     * Drops every cached entry. Call when the level changes (dimension switch,
     * world unload) so we don't return a stale UV taken from a now-invalid
     * Level reference. Cheap - the map is typically a few dozen entries.
     */
    public static void invalidateAll() {
        CACHE.clear();
        lastQueriedBE = null;
        lastFastFrame = -1L;
    }
}
