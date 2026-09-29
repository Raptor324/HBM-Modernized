package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

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
 * <p><b>Invalidation strategy.</b> Entries are stamped with the client game
 * time (tick counter) and stay valid for {@link #LIGHT_TTL_TICKS} ticks.
 * The lightmap UV coordinates stored here are raw sky/block light levels —
 * they do NOT change with the day/night cycle (the lightmap texture applies
 * that at draw time), so the only real change driver is block updates around
 * the machine. Resampling each machine's 6+8 positions EVERY frame was pure
 * CPU waste; a ≤0.75 s staleness after placing/breaking a nearby block is the
 * deliberate trade-off. A sample taken during the shadow pass is reused by
 * the main pass of the same frame (and by subsequent frames within the TTL).
 *
 * <p>Periodic pruning drops entries older than the TTL - typical
 * "I walked past a chunk and now it's behind me" case - keeping the cache
 * bounded in long sessions.
 *
 * <p>Single-threaded: lives entirely on the render thread.
 */

@OnlyIn(Dist.CLIENT)
public final class LightSampleCache {

    private static final Long2ObjectOpenHashMap<Entry> CACHE = new Long2ObjectOpenHashMap<>();

    /**
     * Record validity horizon in game ticks. Light around a standing machine
     * changes only when nearby blocks change; resampling 6+8 positions every
     * frame is a pure CPU cost (6.8% of the frame on the farm). 15 ticks = 0.75 s -
     * a trade-off between "perceptible delay ~= none" and "a frame without lightmap work".
     */
    private static final int LIGHT_TTL_TICKS = 15;

    /**
     * Client-side game time (tick counter), updated once per frame in {@link #onFrameStart}.
     * 0, not MIN_VALUE: the difference {@code currentTick - lastTick} must not overflow,
     * otherwise records set before the first onFrameStart would be considered valid forever.
     */
    private static long currentTick = 0L;

    public static final ThreadLocal<Matrix4f> BASE_POSE = ThreadLocal.withInitial(Matrix4f::new);
    public static final ThreadLocal<Boolean> BASE_POSE_SET = ThreadLocal.withInitial(() -> false);

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
     * Valid until it expires by tick TTL (see {@link #LIGHT_TTL_TICKS}) -
     * it survives frames while the light has not actually changed. Cleared by
     * {@link #invalidateAll} (level change).
     */
    private static BlockEntity lastQueriedBE = null;
    private static float lastBlockU = 0f;
    private static float lastSkyV = 0f;
    private static long lastFastTick = Long.MIN_VALUE;

    /** Reusable scratch position to avoid allocating per sample. */
    private static final BlockPos.MutableBlockPos SAMPLE_POS = new BlockPos.MutableBlockPos();

    /** Reusable face-offsets table (avoids per-call allocation). */
    private static final int[][] FACE_OFFSETS = new int[6][3];

    /**
     * Scratch 2-float buffer for the adaptive spatial-sampling gate.
     * Single-threaded: render thread only.
     */
    private static final float[] PRECHECK_UV = new float[2];

    private static final int PRUNE_EVERY = 600;

    private LightSampleCache() {}

    private static final class Entry {
        float blockU;
        float skyV;
        long lastTick;
    }

    /**
     * Preserves the old fast path of skipping expensive spatial sampling when the
     * BlockEntity's own packed light reports no block light, but makes it adaptive:
     * for very large machines a torch may light a remote part while the controller
     * position remains at blockLight=0. In that case we must sample anyway.
     */
    private static boolean shouldSkipSpatialSampling(@Nullable BlockEntity be, int packedLightFallback) {
        int blockLight = (packedLightFallback & 0xFFFF);
        if (blockLight != 0) return false;
        if (be == null) return true;
        getOrSample(be, packedLightFallback, PRECHECK_UV, 0);
        return Math.round(PRECHECK_UV[0]) <= 0;
    }

    /**
     * Refreshes the tick stamp used for entry validity; runs once per rendered
     * frame. Periodically prunes entries older than the TTL.
     */
    public static void onFrameStart() {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            currentTick = level.getGameTime();
        }
        if ((currentTick % PRUNE_EVERY) == 0) {
            CACHE.long2ObjectEntrySet().removeIf(e ->
                currentTick - e.getValue().lastTick > LIGHT_TTL_TICKS);
            // CACHE8 keys include the renderer's identityHashCode, which changes
            // on every reload/disconnect - without pruning, old entries became
            // permanently unreachable garbage in the map (unbounded growth).
            CACHE8.long2ObjectEntrySet().removeIf(e ->
                currentTick - e.getValue().lastTick > LIGHT_TTL_TICKS);
            PACKED_CACHE.long2LongEntrySet().removeIf(e ->
                currentTick - (e.getLongValue() >>> 32) > LIGHT_TTL_TICKS);
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
        // Forced fullbright (part.lightOverride): ignore the world sample,
        // otherwise glowing parts (crucible melt, InnerBurning) darken at night.
        if (packedLightFallback == net.minecraft.client.renderer.LightTexture.FULL_BRIGHT) {
            outUV[outBase]     = (float) (packedLightFallback & 0xFFFF);
            outUV[outBase + 1] = (float) ((packedLightFallback >>> 16) & 0xFFFF);
            return;
        }
        if (be == null) {
            outUV[outBase]     = (float) (packedLightFallback & 0xFFFF);
            outUV[outBase + 1] = (float) ((packedLightFallback >>> 16) & 0xFFFF);
            return;
        }

        // Fast path: same BE as the previous call, entry not expired.
        // Hits for the whole per-part dispatch burst of one machine and,
        // with the tick TTL, for repeated frames while nothing changed.
        if (be == lastQueriedBE && currentTick - lastFastTick <= LIGHT_TTL_TICKS) {
            outUV[outBase]     = lastBlockU;
            outUV[outBase + 1] = lastSkyV;
            return;
        }

        long key = be.getBlockPos().asLong();
        Entry cached = CACHE.get(key);
        if (cached != null && currentTick - cached.lastTick <= LIGHT_TTL_TICKS) {
            outUV[outBase]     = cached.blockU;
            outUV[outBase + 1] = cached.skyV;
            // Promote into fast-path slot for the next burst.
            lastQueriedBE = be;
            lastBlockU = cached.blockU;
            lastSkyV = cached.skyV;
            lastFastTick = currentTick;
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
        cached.lastTick = currentTick;

        // Same promotion - and since we sampled fresh, the fast-path slot
        // matches `cached` exactly.
        lastQueriedBE = be;
        lastBlockU = cached.blockU;
        lastSkyV = cached.skyV;
        lastFastTick = currentTick;
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
            bounds = com.hbm_m.platform.RenderHooks.getRenderBoundingBox(be);
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
        CACHE8.clear();
        PACKED_CACHE.clear();
        lastQueriedBE = null;
        lastFastTick = Long.MIN_VALUE;
    }

    // ========================================================================
    // 8-corner trilinear sampling (for per-vertex gradient inside machine mesh)
    // ========================================================================

    /**
     * Per-(BE, part-identity) cache of the 8 corner lightmap UVs. Keyed by a
     * 64-bit pack of {@code (beBlockPos.asLong() ^ partIdentity)} so that the 11
     * parts of one Advanced Assembler each get their own entry while still
     * sharing ONE entry across the shadow+main pass of a single frame.
     */
    private static final class Entry8 {
        final float[] cornerUV = new float[16]; // 8 x (blockU, skyV) interleaved
        long lastTick;
    }

    private static final Long2ObjectOpenHashMap<Entry8> CACHE8 = new Long2ObjectOpenHashMap<>();
    private static final BlockPos.MutableBlockPos SAMPLE_POS_8 = new BlockPos.MutableBlockPos();
    private static final Vector4f CORNER_TMP = new Vector4f();

    /**
     * Per-BE cache of vanilla packedLight ({@code LevelRenderer.getLightColor}) with the
     * same tick TTL. The value is packed as {@code (tick << 32) | packed} - no
     * per-record object allocation. The dispatcher (NucleusDispatcherBypass.collectOne)
     * used to query it for every machine every frame.
     */
    private static final Long2LongOpenHashMap PACKED_CACHE = new Long2LongOpenHashMap();
    private static final long PACKED_MISS = Long.MIN_VALUE;

    static {
        PACKED_CACHE.defaultReturnValue(PACKED_MISS);
    }

    /**
     * Vanilla packedLight at the BE position with a tick TTL. null/detached BE -
     * no caching (caller's fallback).
     */
    public static int getOrSamplePacked(@Nullable BlockEntity be, int packedLightFallback) {
        Level level = (be != null) ? be.getLevel() : null;
        if (level == null) {
            return packedLightFallback;
        }
        long key = be.getBlockPos().asLong();
        long v = PACKED_CACHE.get(key);
        if (v != PACKED_MISS && currentTick - (v >>> 32) <= LIGHT_TTL_TICKS) {
            return (int) v;
        }
        int fresh = LevelRenderer.getLightColor(level, be.getBlockState(), be.getBlockPos());
        PACKED_CACHE.put(key, (currentTick << 32) | (fresh & 0xFFFFFFFFL));
        return fresh;
    }

    /**
     * Distance to pull each corner sample toward the bbox interior before
     * flooring to a block index.
     * <p>
     * <b>Why this exists.</b> OBJ multiblock models routinely have bbox
     * extents on exact integer grid points (e.g. {@code minX = 0.0},
     * {@code maxX = 2.0}) because the meshes are modelled in whole-block
     * units. After {@code localPose.transform(corner)} the resulting world
     * offset lands <i>exactly</i> on an integer boundary, and any sub-1e-5
     * float32 drift from the {@code invViewRot * mat} composition (which
     * scales with camera offset distance) pushes the floored index between
     * two adjacent blocks from frame to frame. If the block on one side
     * happens to contain a torch and the block on the other side is air, the
     * corner's sampled light jumps between ~0 and ~15 every time the camera
     * so much as twitches - exactly the "models shimmer near a torch when I
     * move my mouse" symptom.
     * <p>
     * Pulling each corner {@code SAMPLE_INSET} units toward the model's
     * centre:
     * <ul>
     *   <li>moves the floored index well clear of the boundary, absorbing
     *       the entire plausible float drift budget (even at cam ~10^5);</li>
     *   <li>makes the sample land inside the block the model <i>actually
     *       occupies</i> on that side (for a 2-block-wide model with
     *       {@code maxX = 2.0}, floor(2.0) = 2 picks the empty block past
     *       the model; floor(2 - eps) = 1 picks the last occupied block,
     *       which is the one we want to read light from);</li>
     *   <li>at 1/64 of a block (~1.5 cm) is well under the resolution of
     *       the lightmap (1 block) and produces no visible bias in the
     *       trilinear interpolation.</li>
     * </ul>
     * Clamped per-axis to half the bbox span so zero-thickness parts don't
     * invert their corner ordering.
     */
    private static final float SAMPLE_INSET = 1.0f / 64.0f;

    /**
     * Samples lightmap UV pairs at the 8 world-space corners of the given
     * object-space bbox. Writes into {@code out16} as
     * {@code [c0.blockU, c0.skyV, c1.blockU, c1.skyV, ...]}.
     *
     * <p>Corner index encoding: bit 0 = x, bit 1 = y, bit 2 = z; bit set means
     * "take max side on that axis, else min side". This order must match the
     * shader's {@code w.x / w.y / w.z} weight indexing in {@code trilinearBright}.
     *
     * <p><b>Coordinate system.</b> The 8 world block positions are resolved as
     * {@code blockPos + floor(localPose * objCorner)}, where:
     * <ul>
     *   <li>{@code blockPos} is the BE's integer world position (exact);</li>
     *   <li>{@code localPose} is the per-BE local transform (rotation, scale,
     *       and any small sub-block translation applied inside the BER)
     *       <b>without</b> the camera's view rotation and <b>without</b> the
     *       {@code blockPos - cameraPos} offset baked in by LevelRenderer.</li>
     * </ul>
     * This avoids composing a full absolute world pose in {@code Matrix4f},
     * which would lose precision at large camera offsets (&ge; ~10k blocks
     * from origin) and cause the floored block index to flicker between
     * adjacent blocks as the camera moves sub-block distances - producing
     * the "models shimmer between bright and dark when you move near a torch"
     * symptom. Float32 has ~7 decimal digits of precision, so
     * {@code (float)cameraPos + (float)(blockPos - cameraPos)} at
     * {@code cameraPos ~ 10^6} rounds inconsistently across frames.
     *
     * <p>Cache key combines {@code BE.getBlockPos().asLong()} with a
     * {@code partIdentityHash} so separate parts of one BE don't collide.
     *
     * @param be                   owning BlockEntity (for world lookup + cache key)
     * @param partIdentityHash     stable hash identifying which part of the BE
     *                             this sample set belongs to (e.g. renderer identity)
     * @param objBbox              {minX, minY, minZ, maxX, maxY, maxZ}
     * @param blockPos             BE world position (int, exact)
     * @param localPose            object-space -&gt; block-relative transform
     *                             (per-BE rot/scale/local trans; no view rot;
     *                             no camera-relative offset)
     * @param packedLightFallback  vanilla packed light when sampling isn't possible
     * @param out16                destination, 16 floats (must be non-null)
     */
    public static void getOrSample8(@Nullable BlockEntity be, long partIdentityHash,
                                    float[] objBbox, BlockPos blockPos, Matrix4f localPose,
                                    int packedLightFallback, float[] out16) {

        // Forced fullbright (part.lightOverride): no 8-corner sample,
        // otherwise glowing parts (crucible melt, InnerBurning) darken at night.
        if (packedLightFallback == net.minecraft.client.renderer.LightTexture.FULL_BRIGHT) {
            fillFallback8(packedLightFallback, out16);
            return;
        }

        if (com.hbm_m.client.render.SingleMeshVboRenderer.isWorldMissileOverlayDraw()) {
            fillFallback8(packedLightFallback, out16);
            return;
        }

        // Preserve the cheap “no block light -> no spatial sampling” fast path,
        // but make it adaptive for very large machines where remote block light
        // can exist even if the controller position is dark.
        if (shouldSkipSpatialSampling(be, packedLightFallback)) {
            fillFallback8(packedLightFallback, out16);
            return;
        }
        
        Level level = (be != null) ? be.getLevel() : null;
        if (level == null || blockPos == null || localPose == null || objBbox == null) {
            fillFallback8(packedLightFallback, out16);
            return;
        }

        long key = be.getBlockPos().asLong() ^ partIdentityHash;
        Entry8 cached = CACHE8.get(key);
        if (cached != null && currentTick - cached.lastTick <= LIGHT_TTL_TICKS) {
            System.arraycopy(cached.cornerUV, 0, out16, 0, 16);
            return;
        }

        sample8(level, objBbox, blockPos, localPose, packedLightFallback, out16);

        if (cached == null) {
            cached = new Entry8();
            CACHE8.put(key, cached);
        }
        System.arraycopy(out16, 0, cached.cornerUV, 0, 16);
        cached.lastTick = currentTick;
    }

    /**
     * Like {@link #getOrSample8} but skips expensive 8-block spatial sampling when
     * {@code distSqToCamera} exceeds {@link RenderDistanceHelper#getLightCornerDetailDistanceSq()}.
     */
    public static void getOrSample8Lod(@Nullable BlockEntity be, long partIdentityHash,
                                       float[] objBbox, BlockPos blockPos, Matrix4f localPose,
                                       int packedLightFallback, float[] out16, double distSqToCamera) {
        if (distSqToCamera > RenderDistanceHelper.getLightCornerDetailDistanceSq()) {
            fillFallback8(packedLightFallback, out16);
            return;
        }
        getOrSample8(be, partIdentityHash, objBbox, blockPos, localPose, packedLightFallback, out16);
    }

    private static void sample8(Level level, float[] objBbox, BlockPos blockPos,
                                Matrix4f localPose, int packedLightFallback, float[] out16) {
        final float minX = objBbox[0], minY = objBbox[1], minZ = objBbox[2];
        final float maxX = objBbox[3], maxY = objBbox[4], maxZ = objBbox[5];
        final int bpx = blockPos.getX();
        final int bpy = blockPos.getY();
        final int bpz = blockPos.getZ();

        // Pull each corner toward the bbox centre by SAMPLE_INSET so the
        // floored block index is stable against float32 drift and lands in
        // the block the model actually occupies on that side. See the constant's
        // javadoc for the full rationale. Clamp to half-span so very thin
        // bboxes don't produce insets that cross the centre.
        float insetX = Math.min(SAMPLE_INSET, (maxX - minX) * 0.5f);
        float insetY = Math.min(SAMPLE_INSET, (maxY - minY) * 0.5f);
        float insetZ = Math.min(SAMPLE_INSET, (maxZ - minZ) * 0.5f);
        final float minXs = minX + insetX, maxXs = maxX - insetX;
        final float minYs = minY + insetY, maxYs = maxY - insetY;
        final float minZs = minZ + insetZ, maxZs = maxZ - insetZ;

        for (int i = 0; i < 8; i++) {
            float ox = ((i & 1) == 0) ? minXs : maxXs;
            float oy = ((i & 2) == 0) ? minYs : maxYs;
            float oz = ((i & 4) == 0) ? minZs : maxZs;

            // Transform the object-space corner through the BE-local pose.
            // Result is small floats (typically within [-1..+N] for a BE of
            // size N blocks), so float precision is more than sufficient and
            // the floored value is stable frame-to-frame regardless of where
            // the player stands in the world.
            CORNER_TMP.set(ox, oy, oz, 1f);
            localPose.transform(CORNER_TMP);

            int wx = bpx + Mth.floor(CORNER_TMP.x);
            int wy = bpy + Mth.floor(CORNER_TMP.y);
            int wz = bpz + Mth.floor(CORNER_TMP.z);
            SAMPLE_POS_8.set(wx, wy, wz);

            int packed;
            try {
                BlockState state = level.getBlockState(SAMPLE_POS_8);
                if (state.isSolidRender(level, SAMPLE_POS_8)) {
                    // Inside an opaque block: sampling there returns 0 which
                    // would punch a dark corner into an otherwise lit model.
                    // Use the fallback (BE's own packed light) instead.
                    packed = packedLightFallback;
                } else {
                    packed = LevelRenderer.getLightColor(level, SAMPLE_POS_8);
                }
            } catch (Throwable t) {
                packed = packedLightFallback;
            }

            // Keep UV2 RAW here. Iris/Embeddium shader packs feed vaUV2 into the
            // vanilla lightmap sampler, which already applies day/night sky
            // attenuation. Pre-darkening sky light here makes the pack apply that
            // attenuation twice, producing the "machines are too dark with shaders"
            // regression. Our own block_lit shader now applies the sky scale
            // explicitly on its path instead.
            out16[i * 2]     = (float) (packed & 0xFFFF);
            out16[i * 2 + 1] = (float) ((packed >>> 16) & 0xFFFF);
        }
    }

    private static void fillFallback8(int packedLightFallback, float[] out16) {
        float bu = (float) (packedLightFallback & 0xFFFF);
        float sv = (float) ((packedLightFallback >>> 16) & 0xFFFF);
        for (int i = 0; i < 8; i++) {
            out16[i * 2]     = bu;
            out16[i * 2 + 1] = sv;
        }
    }
}
