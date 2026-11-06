package com.hbm_m.client.render.machine;

import org.joml.Matrix4f;

/**
 * Per-machine render state, keyed by {@code blockPos.asLong()} in one map per
 * {@link MachineBer}. This replaces five parallel staleness mechanisms that
 * used to live in MachineBer - the roster cache, the param-word cache, the
 * animation-epoch ring arrays and the animation-delta ring arrays (both rings
 * were slot-collision prone and carried F3 "Frozen: 0/100%" see-saw bugs) -
 * with one object per position: no slot hashing, no collision invalidation,
 * and a removed machine drops via one map remove.
 * <p>
 * Sections:
 * <ul>
 *   <li><b>Roster</b> - the frozen-walk classification per part (which parts
 *   are static-assertable and their cached renderer/slot). Structural facts
 *   (immutable part list, stable static geometry), self-healing on renderer
 *   cache wipes.</li>
 *   <li><b>Epoch</b> - (epoch, gameTick) of the last FULL rebuild of the
 *   animated parts; stable = epoch unchanged AND rebuild not in the current
 *   tick (within a tick the lerp moves the pose between frames).</li>
 *   <li><b>Param words</b> - the 4 packed binary16 AnimParams words (plus the
 *   logical floats for lazy hook transforms) per parametric part, valid while
 *   the epoch is unchanged across ticks.</li>
 *   <li><b>Anim delta</b> - per-part {@code S^-1 * F} matrices reused between
 *   the shadow and main passes of one frame; per-part frame stamps make a
 *   delta unhittable outside its frame.</li>
 * </ul>
 */
final class MachineRenderState {

    long worldGen = -1L;
    long berEpoch = -1L;

    // -- Roster (frozen walk) ---------------------------------------------

    static final byte REBUILD = 0;
    static final byte ASSERT = 1;
    /** Parametric part assertable ONLY while the machine's animation epoch is
     * stable (the record words are then provably unchanged; the VSH animates
     * them from uAnimTime). Asserted by the frozen walk when epochStableNow,
     * rebuilt through the regular parametric branch otherwise. */
    static final byte ASSERT_EPOCH = 2;

    byte[] rosterCategory;
    MachinePartRenderer[] rosterRenderers;
    /** Cached resolved slot per part (-1 = not resolved yet); lets the frozen
     * assert skip both Long2IntOpenHashMap lookups and monitor locks. */
    int[] rosterSlots;

    private boolean rosterFlagsComputed;
    private boolean allStaticAsserted;
    private boolean allEpochAsserted;

    void computeRosterFlags() {
        if (rosterCategory == null || rosterCategory.length == 0 || rosterRenderers == null) {
            allStaticAsserted = false;
            allEpochAsserted = false;
            rosterFlagsComputed = true;
            return;
        }
        boolean stat = true;
        boolean ep = true;
        for (int i = 0; i < rosterCategory.length; i++) {
            byte cat = rosterCategory[i];
            if (cat == ASSERT) {
                if (rosterRenderers[i] == null) {
                    stat = false;
                    ep = false;
                    break;
                }
            } else if (cat == ASSERT_EPOCH) {
                if (rosterRenderers[i] == null) {
                    ep = false;
                }
            } else {
                stat = false;
                ep = false;
                break;
            }
        }
        allStaticAsserted = stat;
        allEpochAsserted = ep;
        rosterFlagsComputed = true;
    }

    boolean allAsserted(boolean epochParts) {
        if (!rosterFlagsComputed) {
            computeRosterFlags();
        }
        return epochParts ? allEpochAsserted : allStaticAsserted;
    }

    /**
     * Drops only the roster section (TTL frame / failed frozen walk); the
     * epoch, param words and anim delta stay - exactly the scope the old
     * {@code rosterCache.remove} had.
     */
    void dropRoster() {
        rosterCategory = null;
        rosterRenderers = null;
        rosterSlots = null;
        rosterFlagsComputed = false;
    }

    // -- Animation epoch ----------------------------------------------------

    /** Sentinel until the first store: {@code currentEpoch} never returns a
     * stored value for an un-stored machine. */
    boolean epochStored;
    long epochValue;
    long epochTick = -1L;

    // -- Frozen parametric eval-skip: epoch-keyed AnimParams word cache ------
    // Keyed by the map's posKey (no per-part keys): one entry per machine
    // position, per-part arrays inside. Populated only for specs that provide
    // an epoch; the LOGICAL params are cached alongside the packed words - the
    // lazy hook path (saveParametricTransform) copies them for partTransform
    // queries.

    /** Epoch the words were stored under; MIN_VALUE = nothing stored yet. */
    long paramEpoch = Long.MIN_VALUE;
    /** Game tick of the store (the within-tick lerp invalidates the reuse). */
    long paramTick = -1L;
    private boolean[] paramDrawn;
    private float[][] paramLogical;
    private float[][] paramWords;

    void storeParamWords(int partIdx, int partCount, long epochIn, long tickIn, boolean drawnIn,
                         float[] pr, float[] enc) {
        paramEpoch = epochIn;
        paramTick = tickIn;
        if (paramDrawn == null || paramDrawn.length != partCount) {
            paramDrawn = new boolean[partCount];
            paramLogical = new float[partCount][];
            paramWords = new float[partCount][];
        }
        paramDrawn[partIdx] = drawnIn;
        float[] dstL = paramLogical[partIdx];
        if (dstL == null) {
            dstL = new float[pr.length];
            paramLogical[partIdx] = dstL;
        }
        System.arraycopy(pr, 0, dstL, 0, Math.min(pr.length, dstL.length));
        if (drawnIn) {
            float[] dstW = paramWords[partIdx];
            if (dstW == null) {
                dstW = new float[4];
                paramWords[partIdx] = dstW;
            }
            System.arraycopy(enc, 0, dstW, 0, 4);
        }
    }

    /** Whether a frozen hit exists for (partIdx, epoch, tick). */
    boolean hasParamWords(int partIdx, long epoch, long gameTick) {
        return paramEpoch == epoch && paramTick != gameTick
                && paramLogical != null && paramLogical[partIdx] != null;
    }

    boolean paramDrawn(int partIdx) {
        return paramDrawn[partIdx];
    }

    /** The 4 packed binary16 record words of a drawn part. */
    float[] paramWords(int partIdx) {
        return paramWords[partIdx];
    }

    /** The logical parameter floats (for lazy hook transforms). */
    float[] paramLogical(int partIdx) {
        return paramLogical[partIdx];
    }

    // -- Animation delta (shadow/main pass reuse) -----------------------------

    /** Render frame the deltas were stored in (one machine per state - no
     * slot key needed). */
    long animFrameKey;
    Matrix4f[] animDelta;
    long[] animPartTime;
    boolean[] animSkip;

    /** Lazily sizes the per-part delta arrays; returns the delta array. */
    Matrix4f[] animDelta(int partCount) {
        if (animDelta == null || animDelta.length != partCount) {
            animDelta = new Matrix4f[partCount];
            animPartTime = new long[partCount];
            animSkip = new boolean[partCount];
        }
        return animDelta;
    }
}
