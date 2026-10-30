package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;

/**
 * Global registry of parametric joint specifications (GPU animation,
 * GradParams.w = joint/chain base index). Joint data are PART constants
 * (kind, axis, pivot), shared by all instances of a variant - therefore they
 * live not in the instance record (which only holds per-frame parameters)
 * but in a single RGBA32F texture that the vertex shader reads via texelFetch
 * ({@code #version 330} forbids dynamic UBO indexing; a texture does not).
 * <p>
 * Layout v2: 3 texels per joint, texel x = idx*3 + {0,1,2} (v1 was 2 texels at
 * idx*2 - the jointIndex -> texel addressing changed with this version):
 * <pre>
 *   A = (kind, axisX, axisY, axisZ)  - kind: 0 = rotation around axis (degrees;
 *                                      AnimParams), 1 = translation (the record's
 *                                      AnimParams.xyz IS the vector; the axis here is
 *                                      informational), 2 = translation along the axis
 *                                      (1 param: the distance; chains only)
 *   B = (pivotX, pivotY, pivotZ, w)  - rotation pivot in part coordinates; for the
 *                                      FIRST node of a chain the free 4th component
 *                                      carries the chain's node count (0 = a legacy
 *                                      single joint - read as 1)
 *   C = (rateScale, flags, trackPtr, trackLen) - x = the track's rateScale
 *                                      (playback-frequency multiplier, > 0):
 *                                      0.0 = STREAMED (the node parameter comes
 *                                      from its AnimParams halfword slot,
 *                                      exactly as v1), > 0 = KEYFRAMED (the
 *                                      parameter is sampled from the keyframe
 *                                      pool by the VSH; the VSH tests
 *                                      specC.x > 0.0). flags bit0 = 0: clamp (u
 *                                      clamped to [0,1], one-shot), 1: cycle
 *                                      (u = fract(u), looping). trackPtr = absolute
 *                                      texel index of the track's first keyframe;
 *                                      trackLen = keyframe count (>= 2); both 0 for
 *                                      streamed joints.
 * </pre>
 * A joint CHAIN ({@link #registerChain}) occupies nodeCount CONSECUTIVE texel
 * triples; the vertex shader walks base..base+count-1 (see
 * MachineSpecBuilder.jointChainPart).
 * KEYFRAME POOL ({@link #registerTrack}): texels appended after the joint
 * region, one per keyframe = (t01, value, 0, 0), t01 ascending within a track.
 * The pool base is {@code jointCount * 3} and SHIFTS whenever joints are
 * appended; every tracked joint's C texel is rewritten with the new absolute
 * ptr on each shift ({@code TrackRef.ptr} handed out by {@link #registerTrack}
 * is only valid until the next joint registration - consume it in the
 * accompanying joint registration call).
 * Registration is idempotent (dedup by value bits, tracks deduped by keyframe
 * content); upload happens only when a new joint or track appears (once per
 * part variant at build). The texture is process-global - recreated only on
 * render re-init.
 */
@OnlyIn(Dist.CLIENT)
public final class NucleusJointSpecs {

    public static final int KIND_ROTATE = 0;
    public static final int KIND_TRANSLATE = 1;
    public static final int KIND_TRANSLATE_AXIS = 2;
    /**
     * Sampler unit for uJointSpecs. Deliberately NOT the "4th declared sampler"
     * index: ShaderInstance.apply() maps samplers by declaration INDEX and drops
     * entries missing from the GLSL (Sampler1 is absent from block_lit.fsh -
     * see the registration warn), shifting every later index - the specs ended
     * up on the lightmap's unit. Binding is explicit (see {@link #applyBinding}),
     * so any unit outside the vanilla 0..2 works; 4 keeps clear of everything.
     */
    public static final int SAMPLER_UNIT = 4;

    private static final int FLOATS_PER_TEXEL = 4;
    /** v2 layout: 3 texels per joint (v1 was 2 - the jointIndex addressing changed). */
    private static final int TEXELS_PER_JOINT = 3;
    /** Texel C x = 0.0: the node parameter streams from its AnimParams halfword slot (STREAMED). */
    public static final int TRACK_STREAMED = 0;
    /**
     * Legacy texel C trackKind for KEYFRAMED nodes (v2.0 wrote the constant 2
     * into component x; the rateScale format now writes the track's rateScale
     * there and the VSH tests specC.x &gt; 0.0 - kept only for documentation).
     */
    public static final int TRACK_KEYFRAMED = 2;
    /** Texel C flags bit0: cycle wrap (u = fract(u), looping); 0 = clamp to [0,1]. */
    public static final int FLAG_CYCLE = 1;
    /** Texel C flags bit1: Hermite reciprocating stroke spline evaluation. */
    public static final int FLAG_HERMITE = 2;
    /** Mask of the meaningful track-flag bits (strips noise bits before writing texel C). */
    private static final int VALID_TRACK_FLAGS = FLAG_CYCLE | FLAG_HERMITE;

    private static int texId = 0;
    private static int widthTexels = 0;
    private static int jointCount = 0;
    private static FloatBuffer cpu;
    private static boolean dirty = false;
    // v2 keyframe pool: deduped tracks stored after the joint region. The pool
    // base is jointCount * TEXELS_PER_JOINT and SHIFTS whenever joints are
    // appended; the shift rewrites every tracked joint's C texel with the new
    // absolute ptr (see appendJoints). One interleaved (t01, value) pair array
    // per deduped track; per-joint parallel (entry, flags) arrays keep every C
    // texel reconstructible after a shift.
    private static final java.util.ArrayList<float[]> poolTracks = new java.util.ArrayList<>();
    private static int poolTexels = 0;
    private static int[] jointTrackEntry = new int[16];
    private static int[] jointTrackFlags = new int[16];
    // Per-joint rateScale of the track (the C texel's x component; 0 = streamed).
    private static float[] jointTrackRate = new float[16];

    private NucleusJointSpecs() {}

    /**
     * Registers a STREAMED joint (idempotently) and returns its index. Call on
     * the render thread (texture upload). The axis is normalized.
     */
    public static synchronized int register(int kind, float axisX, float axisY, float axisZ,
                                            float pivotX, float pivotY, float pivotZ) {
        return register(kind, axisX, axisY, axisZ, pivotX, pivotY, pivotZ, null, 0);
    }

    /**
     * {@link #register(int, float, float, float, float, float, float)} with a
     * keyframed track: {@code track} (from {@link #registerTrack}) turns texel C
     * into (TRACK_KEYFRAMED, flags, trackPtr, trackLen) and the VSH samples the
     * node parameter from the pool instead of an AnimParams halfword. Only
     * {@link #VALID_TRACK_FLAGS} of {@code flags} is meaningful ({@link #FLAG_CYCLE}
     * = the wrap mode, {@link #FLAG_HERMITE} = the stroke spline; the Track-driven
     * declarations pass the track's own cycle flag). The ref is
     * consumed HERE - its absolute {@code ptr} is valid only until the next
     * joint registration shifts the pool (the texel C copy inside the texture
     * is rewritten on every shift, so the stored joints never go stale).
     */
    public static synchronized int register(int kind, float axisX, float axisY, float axisZ,
                                            float pivotX, float pivotY, float pivotZ,
                                            @org.jetbrains.annotations.Nullable TrackRef track, int flags) {
        float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (len < 1.0e-6f) {
            len = 1f;
        }
        final float ax = axisX / len, ay = axisY / len, az = axisZ / len;

        // Candidate texel C, resolved against the CURRENT pool base (the dedup
        // scan below compares it against the stored texels, which the last
        // shift kept in sync with the same base).
        final int entry = track != null ? track.entry : -1;
        final int nodeFlags = track != null ? (flags & VALID_TRACK_FLAGS) : 0;
        final float nodeRate = track != null ? track.rateScale : TRACK_STREAMED;

        // Dedup by raw bits - the index stays stable across frames. The candidate
        // must be a SINGLE joint (0 in the pivot texel's free component) -
        // otherwise a single joint would dedup-alias a chain's node 0 and the
        // shader would walk the whole chain with this part's params (the mirror
        // of registerChain's count check below). Texel C participates in the
        // identity: two joints sharing A/B but carrying different tracks (or
        // streamed vs tracked) never alias.
        // The kind is stored as its FLOAT VALUE (the shader branches on specA.x
        // magnitude: <0.5 rotate, <1.5 vector translate, else axis translate).
        // Float.intBitsToFloat(kind) here stored the raw BIT PATTERN - for kind 1/2
        // that is a ~0 denormal, the shader took every joint for a rotation, and all
        // KIND_TRANSLATE/KIND_TRANSLATE_AXIS parts froze at their rest pose.
        final float kindF = (float) kind;
        for (int i = 0; i < jointCount; i++) {
            int base = i * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
            if (cpu.get(base) == kindF
                    && Float.floatToRawIntBits(cpu.get(base + 1)) == Float.floatToRawIntBits(ax)
                    && Float.floatToRawIntBits(cpu.get(base + 2)) == Float.floatToRawIntBits(ay)
                    && Float.floatToRawIntBits(cpu.get(base + 3)) == Float.floatToRawIntBits(az)
                    && Float.floatToRawIntBits(cpu.get(base + 4)) == Float.floatToRawIntBits(pivotX)
                    && Float.floatToRawIntBits(cpu.get(base + 5)) == Float.floatToRawIntBits(pivotY)
                    && Float.floatToRawIntBits(cpu.get(base + 6)) == Float.floatToRawIntBits(pivotZ)
                    && cpu.get(base + 7) == 0f
                    && matchesCTexel(base + 8, entry, nodeFlags, nodeRate)) {
                return i;
            }
        }

        appendJoints(1, new int[] {entry}, new int[] {nodeFlags}, new float[] {nodeRate});
        int base = (jointCount - 1) * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
        cpu.put(base, kindF);
        cpu.put(base + 1, ax);
        cpu.put(base + 2, ay);
        cpu.put(base + 3, az);
        cpu.put(base + 4, pivotX);
        cpu.put(base + 5, pivotY);
        cpu.put(base + 6, pivotZ);
        cpu.put(base + 7, 0f);
        // Texel C is written by appendJoints' rewrite pass (from the per-joint
        // entry/flags just stored) - nothing to do here.
        return jointCount - 1;
    }

    /** Whether the stored texel C at float offset {@code base} matches (entry, flags, rateScale). */
    private static boolean matchesCTexel(int base, int entry, int flags, float rate) {
        if (entry < 0) {
            return cpu.get(base) == (float) TRACK_STREAMED && cpu.get(base + 1) == 0f
                    && cpu.get(base + 2) == 0f && cpu.get(base + 3) == 0f;
        }
        return cpu.get(base) == rate
                && cpu.get(base + 1) == (float) flags
                && Float.floatToRawIntBits(cpu.get(base + 2)) == Float.floatToRawIntBits(poolPtrTexel(entry))
                && Float.floatToRawIntBits(cpu.get(base + 3)) == Float.floatToRawIntBits(poolLenTexels(entry));
    }

    /**
     * Registers a deduped KEYFRAME TRACK in the pool: {@code times}/{@code
     * values} are equal-length arrays of at least 2 strictly ascending t01
     * positions in [0,1] and their values (the unit depends on the node kind -
     * degrees for rotations, blocks for translates). Returns a ref whose
     * absolute {@code ptr} is valid until the NEXT joint registration (the pool
     * shifts with the joint region; the texture's texel C copies are rewritten
     * on every shift). {@code cycle} rides the JOINT's texel C
     * ({@link #FLAG_CYCLE}), not the pool - it is accepted here so one call
     * describes the whole track; the joint registrations derive their flags
     * from the same value.
     */
    public static synchronized TrackRef registerTrack(float[] times, float[] values, boolean cycle) {
        return registerTrack(times, values, cycle, 1.0f);
    }

    /**
     * {@link #registerTrack(float[], float[], boolean)} with a playback-rate
     * multiplier: the ref carries {@code rateScale} (finite, &gt; 0) into every
     * joint's texel C x component, where the VSH reads it as the tracked
     * clock's frequency multiplier (specC.x &gt; 0.0 = tracked).
     */
    public static synchronized TrackRef registerTrack(float[] times, float[] values, boolean cycle,
                                                      float rateScale) {
        if (!Float.isFinite(rateScale) || rateScale <= 0f) {
            throw new IllegalArgumentException(
                    "[NucleusJointSpecs] track rateScale must be finite and > 0: " + rateScale);
        }
        if (times == null || values == null || times.length != values.length || times.length < 2) {
            throw new IllegalArgumentException(
                    "[NucleusJointSpecs] track needs equal times/values arrays of >= 2 keyframes");
        }
        for (int i = 0; i < times.length; i++) {
            if (!Float.isFinite(times[i]) || times[i] < 0f || times[i] > 1f) {
                throw new IllegalArgumentException("[NucleusJointSpecs] track t01 out of [0,1]: " + times[i]);
            }
            if (i > 0 && !(times[i] > times[i - 1])) {
                throw new IllegalArgumentException("[NucleusJointSpecs] track t01 must ascend strictly");
            }
            if (!Float.isFinite(values[i])) {
                throw new IllegalArgumentException("[NucleusJointSpecs] track value must be finite");
            }
        }
        int entry = internTrack(times, values);
        return new TrackRef(poolPtrTexel(entry), poolLenTexels(entry), entry, rateScale);
    }

    /** Deduped pool track reference: absolute texel of the first keyframe + keyframe count + rateScale. */
    public static final class TrackRef {
        public final int ptr;
        public final int len;
        /** Playback-rate multiplier this track was registered with (the C texel's x component). */
        public final float rateScale;
        final int entry; // pool entry index - the identity that survives pool shifts

        TrackRef(int ptr, int len, int entry, float rateScale) {
            this.ptr = ptr;
            this.len = len;
            this.entry = entry;
            this.rateScale = rateScale;
        }

        public int ptr() { return ptr; }

        public int len() { return len; }

        public float rateScale() { return rateScale; }
    }

    /** Absolute texel of the track entry's first keyframe (against the CURRENT pool base). */
    private static int poolPtrTexel(int entry) {
        return jointCount * TEXELS_PER_JOINT + poolOffsetTexels(entry);
    }

    private static int poolOffsetTexels(int entry) {
        int off = 0;
        for (int e = 0; e < entry; e++) {
            off += poolTracks.get(e).length / 2;
        }
        return off;
    }

    private static int poolLenTexels(int entry) {
        return poolTracks.get(entry).length / 2;
    }

    /** Dedupes the keyframes into the pool; returns the pool ENTRY index (content = raw bits). */
    private static int internTrack(float[] times, float[] values) {
        for (int e = 0; e < poolTracks.size(); e++) {
            float[] t = poolTracks.get(e);
            if (t.length != times.length * 2) {
                continue;
            }
            boolean eq = true;
            for (int i = 0; i < times.length && eq; i++) {
                eq = Float.floatToRawIntBits(t[i * 2]) == Float.floatToRawIntBits(times[i])
                        && Float.floatToRawIntBits(t[i * 2 + 1]) == Float.floatToRawIntBits(values[i]);
            }
            if (eq) {
                return e;
            }
        }
        float[] stored = new float[times.length * 2];
        for (int i = 0; i < times.length; i++) {
            stored[i * 2] = times[i];
            stored[i * 2 + 1] = values[i];
        }
        poolTracks.add(stored);
        ensureTexelCapacity((long) jointCount * TEXELS_PER_JOINT + poolTexels + times.length);
        int base = (jointCount * TEXELS_PER_JOINT + poolTexels) * FLOATS_PER_TEXEL;
        for (int i = 0; i < times.length; i++) {
            cpu.put(base + i * FLOATS_PER_TEXEL, times[i]);
            cpu.put(base + i * FLOATS_PER_TEXEL + 1, values[i]);
            cpu.put(base + i * FLOATS_PER_TEXEL + 2, 0f);
            cpu.put(base + i * FLOATS_PER_TEXEL + 3, 0f);
        }
        poolTexels += times.length;
        dirty = true;
        return poolTracks.size() - 1;
    }

    /**
     * Appends {@code n} joints (per-joint pool entry, -1 = streamed) after the
     * current joint region: grows the buffer, moves the pool right by n texel
     * triples and rewrites every joint's C texel against the new pool base
     * (the callers then fill the new joints' A/B texels - C is already correct).
     */
    private static void appendJoints(int n, int[] entries, int[] flagsPerJoint, float[] ratesPerJoint) {
        ensureTexelCapacity((long) (jointCount + n) * TEXELS_PER_JOINT + poolTexels);
        if (jointTrackEntry.length < jointCount + n) {
            int cap = jointTrackEntry.length;
            while (cap < jointCount + n) {
                cap *= 2;
            }
            jointTrackEntry = java.util.Arrays.copyOf(jointTrackEntry, cap);
            jointTrackFlags = java.util.Arrays.copyOf(jointTrackFlags, cap);
            jointTrackRate = java.util.Arrays.copyOf(jointTrackRate, cap);
        }
        for (int p = 0; p < n; p++) {
            jointTrackEntry[jointCount + p] = entries[p];
            jointTrackFlags[jointCount + p] = entries[p] >= 0 ? (flagsPerJoint[p] & VALID_TRACK_FLAGS) : 0;
            jointTrackRate[jointCount + p] = entries[p] >= 0 ? ratesPerJoint[p] : TRACK_STREAMED;
        }
        // Overlapping move of the pool right by n texel triples - backward
        // float loop (MemoryUtil.memCopy is memcpy: overlapping is undefined).
        // Empty pool: the range is empty and the loop does not run (a full
        // lower bound of poolStartFloat would read below the buffer start on
        // the very first joint append).
        int poolStartFloat = jointCount * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
        int shiftFloats = n * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
        int totalFloats = (jointCount + n) * TEXELS_PER_JOINT * FLOATS_PER_TEXEL
                + poolTexels * FLOATS_PER_TEXEL;
        for (int f = totalFloats - 1; f >= poolStartFloat + shiftFloats; f--) {
            cpu.put(f, cpu.get(f - shiftFloats));
        }
        jointCount += n;
        dirty = true;
        rewriteCTexels();
    }

    /** Rewrites every joint's texel C against the current pool base (call after any shift). */
    private static void rewriteCTexels() {
        for (int i = 0; i < jointCount; i++) {
            int entry = jointTrackEntry[i];
            int b = (i * TEXELS_PER_JOINT + 2) * FLOATS_PER_TEXEL;
            if (entry >= 0) {
                cpu.put(b, jointTrackRate[i]);
                cpu.put(b + 1, (float) jointTrackFlags[i]);
                cpu.put(b + 2, (float) poolPtrTexel(entry));
                cpu.put(b + 3, (float) poolLenTexels(entry));
            } else {
                cpu.put(b, (float) TRACK_STREAMED);
                cpu.put(b + 1, 0f);
                cpu.put(b + 2, 0f);
                cpu.put(b + 3, 0f);
            }
        }
    }

    /** Grows the staging buffer to hold at least {@code texels} texels (positions preserved). */
    private static void ensureTexelCapacity(long texels) {
        long floats = texels * FLOATS_PER_TEXEL;
        if (cpu == null) {
            int init = 64 * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
            cpu = MemoryUtil.memAllocFloat((int) Math.max(init, floats));
            return;
        }
        if (cpu.capacity() < floats) {
            int cap = cpu.capacity();
            while (cap < floats) {
                cap *= 2;
            }
            FloatBuffer bigger = MemoryUtil.memAllocFloat(cap);
            MemoryUtil.memCopy(MemoryUtil.memAddress(cpu), MemoryUtil.memAddress(bigger),
                    (long) cpu.capacity() * 4L);
            MemoryUtil.memFree(cpu);
            cpu = bigger;
        }
    }

    /**
     * Registers a rigid CHAIN of joints (idempotently) and returns the BASE
     * index: the chain occupies {@code kinds.length} consecutive texel triples
     * in the same layout as single joints; the shader walks base..base+n-1. The
     * node count is stored in the free 4th component of the FIRST node's pivot
     * texel (single joints from {@link #register} keep 0 there - read as 1).
     * Dedup treats the count as part of the identity, so a chain never aliases
     * a single joint (or another chain) that merely shares its first node.
     * Call on the render thread (texture upload). Axes are normalized.
     *
     * @param kinds  per node: {@link #KIND_ROTATE} / {@link #KIND_TRANSLATE}
     * @param axes   3 floats per node (normalized here)
     * @param pivots 3 floats per node (part-local rotation pivot; unused for translates)
     */
    public static synchronized int registerChain(int[] kinds, float[] axes, float[] pivots) {
        return registerChain(kinds, axes, pivots, null, null);
    }

    /**
     * {@link #registerChain(int[], float[], float[])} with per-node keyframe
     * tracks: {@code tracks[j]} (from {@link #registerTrack}) turns node j's
     * texel C into (TRACK_KEYFRAMED, flags, trackPtr, trackLen); null = the
     * node streams. {@code flagsPerNode} may be null (no cycle flags). See
     * {@link #register(int, float, float, float, float, float, TrackRef, int)}
     * for the ref-consumption contract.
     */
    public static synchronized int registerChain(int[] kinds, float[] axes, float[] pivots,
                                                 @org.jetbrains.annotations.Nullable TrackRef[] tracks,
                                                 @org.jetbrains.annotations.Nullable int[] flagsPerNode) {
        int n = kinds.length;
        float[] nax = new float[n * 3];
        for (int i = 0; i < n; i++) {
            float ax = axes[i * 3], ay = axes[i * 3 + 1], az = axes[i * 3 + 2];
            float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (len < 1.0e-6f) {
                len = 1f;
            }
            nax[i * 3] = ax / len;
            nax[i * 3 + 1] = ay / len;
            nax[i * 3 + 2] = az / len;
        }
        final int[] entries = new int[n];
        final int[] flags = new int[n];
        final float[] rates = new float[n];
        for (int j = 0; j < n; j++) {
            entries[j] = tracks != null && tracks[j] != null ? tracks[j].entry : -1;
            flags[j] = entries[j] >= 0 && flagsPerNode != null ? (flagsPerNode[j] & VALID_TRACK_FLAGS) : 0;
            rates[j] = entries[j] >= 0 ? tracks[j].rateScale : TRACK_STREAMED;
        }

        // Dedup by raw bits - the base index stays stable across frames. The
        // candidate base must carry the matching node count in its first
        // pivot texel's free component (single joints hold 0 there). Texel C
        // participates in the identity (a tracked node never aliases a
        // streamed one, and two joints alias only on the SAME track content).
        // Kind texels hold FLOAT VALUES (see register) - compare as floats.
        float[] kindF = new float[n];
        for (int j = 0; j < n; j++) {
            kindF[j] = (float) kinds[j];
        }
        scan:
        for (int base = 0; base + n <= jointCount; base++) {
            if (cpu == null || cpu.get(base * TEXELS_PER_JOINT * FLOATS_PER_TEXEL + 7) != (float) n) {
                continue;
            }
            for (int j = 0; j < n; j++) {
                int b = (base + j) * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
                if (cpu.get(b) != kindF[j]
                        || Float.floatToRawIntBits(cpu.get(b + 1)) != Float.floatToRawIntBits(nax[j * 3])
                        || Float.floatToRawIntBits(cpu.get(b + 2)) != Float.floatToRawIntBits(nax[j * 3 + 1])
                        || Float.floatToRawIntBits(cpu.get(b + 3)) != Float.floatToRawIntBits(nax[j * 3 + 2])
                        || Float.floatToRawIntBits(cpu.get(b + 4)) != Float.floatToRawIntBits(pivots[j * 3])
                        || Float.floatToRawIntBits(cpu.get(b + 5)) != Float.floatToRawIntBits(pivots[j * 3 + 1])
                        || Float.floatToRawIntBits(cpu.get(b + 6)) != Float.floatToRawIntBits(pivots[j * 3 + 2])
                        || !matchesCTexel(b + 8, entries[j], flags[j], rates[j])) {
                    continue scan;
                }
            }
            return base;
        }

        int[] allEntries = new int[n];
        int[] allFlags = new int[n];
        float[] allRates = new float[n];
        for (int j = 0; j < n; j++) {
            allEntries[j] = entries[j];
            allFlags[j] = flags[j];
            allRates[j] = rates[j];
        }
        appendJoints(n, allEntries, allFlags, allRates);
        int base0 = (jointCount - n) * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
        for (int j = 0; j < n; j++) {
            int b = base0 + j * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
            cpu.put(b, kindF[j]);
            cpu.put(b + 1, nax[j * 3]);
            cpu.put(b + 2, nax[j * 3 + 1]);
            cpu.put(b + 3, nax[j * 3 + 2]);
            cpu.put(b + 4, pivots[j * 3]);
            cpu.put(b + 5, pivots[j * 3 + 1]);
            cpu.put(b + 6, pivots[j * 3 + 2]);
            cpu.put(b + 7, j == 0 ? (float) n : 0f);
            // Texel C (b + 8..11) was written by appendJoints' rewrite pass.
        }
        return jointCount - n;
    }

    /** GL texture of specs; 0 means no joints registered. */
    public static synchronized int getTextureId() {
        uploadIfDirty();
        return texId;
    }

    public static synchronized int getJointCount() {
        return jointCount;
    }

    /**
     * Fast check whether the joint or chain starting at {@code baseJointIdx} contains
     * any keyframe-tracked node (rateScale > 0 in texel C). Used by BakeRecordDiffer
     * to mark actively moving time-driven instances dirty during compute baking.
     */
    public static synchronized boolean isTrackedChain(int baseJointIdx) {
        if (baseJointIdx < 0 || baseJointIdx >= jointCount || cpu == null) {
            return false;
        }
        int baseFloat = baseJointIdx * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
        float countW = cpu.get(baseFloat + 7);
        int n = countW >= 0.5f ? Math.min((int) countW, 8) : 1;
        for (int i = 0; i < n && (baseJointIdx + i) < jointCount; i++) {
            if (jointTrackEntry[baseJointIdx + i] >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * GPU bytes of the RGBA32F specs texture (widthTexels x 1 x 16 bytes; the
     * process-global texture is never freed until context death, so this is
     * both its used and allocated size). F3 VRAM breakdown only; 0 when no
     * texture was ever uploaded.
     */
    public static synchronized long getVramBytes() {
        return texId != 0 ? (long) widthTexels * FLOATS_PER_TEXEL * 4L : 0L;
    }

    /**
     * Binds the specs texture to unit {@link #SAMPLER_UNIT} (or a dummy when there
     * are no joints - the active sampler must be complete).
     * Call after {@code shader.apply()} of the instanced shader.
     */
    public static void bindSampler() {
        int tex = getTextureId();
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(GL13.GL_TEXTURE0 + SAMPLER_UNIT);
        if (tex != 0) {
            com.mojang.blaze3d.platform.GlStateManager._bindTexture(tex);
        } else {
            com.mojang.blaze3d.platform.GlStateManager._bindTexture(0);
        }
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(GL13.GL_TEXTURE0);
    }

    /**
     * Deterministic sampler binding, independent of ShaderInstance's
     * declaration-index mapping (which shifts when a declared sampler is absent
     * from the GLSL - Sampler1 case): sets the uJointSpecs uniform to
     * {@link #SAMPLER_UNIT} and binds the specs texture to that unit. Call while
     * the instanced program is active, after {@code shader.apply()}.
     */
    public static void applyBinding(int programId) {
        int loc = GL20.glGetUniformLocation(programId, "uJointSpecs");
        if (loc < 0) {
            return;
        }
        GL20.glUniform1i(loc, SAMPLER_UNIT);
        int tex = getTextureId();
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(GL13.GL_TEXTURE0 + SAMPLER_UNIT);
        com.mojang.blaze3d.platform.GlStateManager._bindTexture(tex);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(GL13.GL_TEXTURE0);
    }

    private static void uploadIfDirty() {
        if (!dirty) {
            return;
        }
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            return;
        }
        dirty = false;
        try {
            if (texId == 0) {
                texId = GL11.glGenTextures();
            }
            int needed = jointCount * TEXELS_PER_JOINT + poolTexels;
            if (widthTexels < needed) {
                widthTexels = Math.max(needed, 64);
                GlStateManager._bindTexture(texId);
                try {
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
                    GlVaoSafety.bindVertexArray(0);
                    BufferUploader.invalidate();
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, widthTexels, 1, 0,
                            GL11.GL_RGBA, GL11.GL_FLOAT,
                            (FloatBuffer) cpu.limit(widthTexels * FLOATS_PER_TEXEL).position(0));
                    ((FloatBuffer) cpu).clear();
                } finally {
                    GlStateManager._bindTexture(0);
                }
            } else {
                GlStateManager._bindTexture(texId);
                try {
                    GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, needed, 1,
                            GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) cpu.limit(needed * FLOATS_PER_TEXEL).position(0));
                    ((FloatBuffer) cpu).clear();
                } finally {
                    GlStateManager._bindTexture(0);
                }
            }
        } catch (Throwable t) {
            // The texture is an optimization: on failure the parametric joints simply
            // will not work (w >= 0 but the fetch returns garbage) - disable joints until fixed.
            MainRegistry.LOGGER.warn("[HBM-M] NucleusJointSpecs upload failed: {}", t.toString());
            // Delete the orphaned name before zeroing - a never-deleted texture is
            // reclaimed only at context death (GL 4.6 spec §5.1.1), and the next
            // register() would otherwise gen a fresh name, repeating per failure.
            if (texId != 0) {
                GL11.glDeleteTextures(texId);
            }
            texId = 0;
            dirty = false;
        }
    }
}
