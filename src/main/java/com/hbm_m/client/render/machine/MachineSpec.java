package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Immutable render description for a machine: the set of parts (static /
 * animated / dynamic), hooks, model resolvers and facing. Created via
 * {@link MachineSpecBuilder}; runtime VBO/instancer caches live here too and
 * are invalidated through {@link #clear()} (from RenderCacheManager).
 */
public final class MachineSpec<T extends BlockEntity> {

    /**
     * Description of a single part. {@code name} is a unique key; {@code modelPartName}
     * is the model part's name.
     *
     * <p>{@code animated} marks ANIMATED CONTENT (arms, gears, sliders): such parts
     * fade out / are skipped beyond {@code modelUpdateDistance}. An animator without
     * animation (legacy bake offsets) is NOT an animated part: it must live up to the
     * static cutoff, see {@link MachineSpecBuilder#staticPart}.
     */
    public record PartDef<T extends BlockEntity>(
            String name,
            String modelPartName,
            @Nullable PartAnimator<T> animator,      // null = no transform needed
            @Nullable QuadResolver<T> dynamicQuads,  // null = take the model part by name
            @Nullable Function<T, String> dynamicCacheKey,
            boolean animated,                         // gated by modelUpdateDistance
            String staticCacheKey,                    // precomputed "id/name" - no String allocations in the hot path
            @Nullable Function<T, Integer> lightOverride, // null/-1 = world light; >=0 = forced packedLight (fullbright)
            @Nullable Function<T, float[]> uvRectResolver, // null = atlas UV; else {u0,v0,du,dv} remap of sprite-local VBO -> atlas
            @Nullable Function<T, float[]> tintOverride, // null/white = no tint; else {r,g,b,a} per-instance color multiplier
            @Nullable float[] tintFalloff, // null = no falloff; else {axis(0/1/2), fullCoord, zeroCoord} in model coordinates
            @Nullable ParametricRig parametric, // null = CPU animator; else GPU rig: single joint or joint chain (attrib 15)
            @Nullable ParametricParams<T> parametricParams, // per-frame joint parameters
            @Nullable java.util.function.Predicate<T> visibilityPredicate, // null = always visible; else conditional part
            @Nullable java.util.function.BiFunction<T, Float, float[][]> clipPlanesResolver // null = no per-part CPU clip
    ) {
        public PartDef(
                String name,
                String modelPartName,
                @Nullable PartAnimator<T> animator,
                @Nullable QuadResolver<T> dynamicQuads,
                @Nullable Function<T, String> dynamicCacheKey,
                boolean animated,
                String staticCacheKey,
                @Nullable Function<T, Integer> lightOverride,
                @Nullable Function<T, float[]> uvRectResolver,
                @Nullable Function<T, float[]> tintOverride,
                @Nullable float[] tintFalloff,
                @Nullable ParametricRig parametric,
                @Nullable ParametricParams<T> parametricParams
        ) {
            this(name, modelPartName, animator, dynamicQuads, dynamicCacheKey, animated, staticCacheKey,
                    lightOverride, uvRectResolver, tintOverride, tintFalloff, parametric, parametricParams, null, null);
        }

        public PartDef(
                String name,
                String modelPartName,
                @Nullable PartAnimator<T> animator,
                @Nullable QuadResolver<T> dynamicQuads,
                @Nullable Function<T, String> dynamicCacheKey,
                boolean animated,
                String staticCacheKey,
                @Nullable Function<T, Integer> lightOverride,
                @Nullable Function<T, float[]> uvRectResolver,
                @Nullable Function<T, float[]> tintOverride,
                @Nullable float[] tintFalloff,
                @Nullable ParametricRig parametric,
                @Nullable ParametricParams<T> parametricParams,
                @Nullable java.util.function.BiFunction<T, Float, float[][]> clipPlanesResolver
        ) {
            this(name, modelPartName, animator, dynamicQuads, dynamicCacheKey, animated, staticCacheKey,
                    lightOverride, uvRectResolver, tintOverride, tintFalloff, parametric, parametricParams, null, clipPlanesResolver);
        }

        public boolean isConditional() {
            return visibilityPredicate != null;
        }

        public boolean isVisible(T be) {
            return visibilityPredicate == null || visibilityPredicate.test(be);
        }

        boolean dynamic() { return dynamicQuads != null; }
    }

    /** Kind for {@link ParametricAnim} - rotation around an axis (parameter = angle in degrees). */
    public static final int KIND_ROTATE = 0;
    /**
     * Kind for {@link ParametricAnim} - translation. The record's AnimParams.xyz IS
     * the translation vector in part-local coordinates (blocks); the spec axis is
     * informational (helpers still fill it). Supports multi-axis motion (e.g. an X
     * ride plus a Z sway on one part).
     */
    public static final int KIND_TRANSLATE = 1;

    /**
     * Descriptor of a parametric joint (PART constants): animation kind, axis
     * (normalized by the registry) and pivot in the part geometry's local
     * coordinates. Written to the {@link com.hbm_m.client.render.NucleusJointSpecs}
     * texture once; per-frame values go through attrib 15 AnimParams. The VSH moves
     * the geometry - the CPU does not rebuild the record (see
     * {@link MachineSpecBuilder#parametricPart}).
     * <p>
     * {@code baseOffset*} is a static translation applied by MachineBer to the
     * record pose (block transform) before enqueue. The joint delta is applied by
     * the VSH in the part geometry's local coordinates; parts whose geometry lives
     * in the legacy bake space (model centered at (0.5, ., 0.5), the offset carried
     * by the legacy animator) need the inverse offset on the record so the
     * GPU-animating record matches the CPU-animator result:
     * record·(pivot + R·(v - pivot)) == blockTransform·R·(v - pivot) + record-only
     * shift cancels exactly when the record carries T(-pivot) — the legacy bake
     * offset. Zero for geometry already centered at the origin.
     * <p>
     * {@code baseYawDegrees} is the static PRE-ROTATION the legacy animator applies
     * BEFORE the joint (the legacy yaw group: rotate by the given angle around
     * the vertical axis through the block center, T(0.5,0,0.5)·R_y(yaw)·T(-0.5,0,-0.5)).
     * The joint operates on raw part vertices, so a static transform that precedes it
     * must live in the record pose (MachineBer applies it right before baseOffset) —
     * the same rule that sends the post-joint tail translation into baseOffset.
     * 0 = none.
     */
    public record ParametricAnim(int kind, float axisX, float axisY, float axisZ,
                                 float pivotX, float pivotY, float pivotZ,
                                 float baseOffsetX, float baseOffsetY, float baseOffsetZ,
                                 float baseYawDegrees,
                                 @Nullable Track track) implements ParametricRig {
        public ParametricAnim {
            if (track != null && kind == KIND_TRANSLATE) {
                // The VSH renders a tracked KIND_TRANSLATE unposed (a scalar track
                // has no 3-word semantic) - fail fast at declaration instead.
                throw new IllegalStateException(
                        "[MachineRenderers] a tracked joint cannot be KIND_TRANSLATE (use KIND_TRANSLATE_AXIS)");
            }
        }

        public boolean hasBaseOffset() {
            return baseOffsetX != 0f || baseOffsetY != 0f || baseOffsetZ != 0f;
        }

        public boolean hasBaseYaw() {
            return baseYawDegrees != 0f;
        }

        @Override
        public int registerSpecs() {
            com.hbm_m.client.render.NucleusJointSpecs.TrackRef ref = null;
            int flags = 0;
            if (track != null) {
                ref = com.hbm_m.client.render.NucleusJointSpecs.registerTrack(
                        track.times(), track.values(), track.cycle(), track.rateScale());
                flags = (track.cycle() ? com.hbm_m.client.render.NucleusJointSpecs.FLAG_CYCLE : 0)
                        | (track.hermite() ? com.hbm_m.client.render.NucleusJointSpecs.FLAG_HERMITE : 0);
            }
            return com.hbm_m.client.render.NucleusJointSpecs.register(
                    kind(), axisX(), axisY(), axisZ(), pivotX(), pivotY(), pivotZ(), ref, flags);
        }

        @Override
        public void applyToPose(Matrix4f pose, float[] pr) {
            if (track != null) {
                // Tracked rig: the CPU mirror sampled the node value into the
                // first slot after the 3-float header (no streamed words on a
                // single-node rig) - see TRACKED_HEADER_SLOTS.
                float value = pr[TRACKED_HEADER_SLOTS];
                if (kind() == KIND_TRANSLATE_AXIS) {
                    pose.translate(axisX() * value, axisY() * value, axisZ() * value);
                } else if (kind() == KIND_ROTATE) {
                    pose.translate(pivotX(), pivotY(), pivotZ());
                    pose.rotate((float) Math.toRadians(value), axisX(), axisY(), axisZ());
                    pose.translate(-pivotX(), -pivotY(), -pivotZ());
                }
                return;
            }
            if (kind() == KIND_TRANSLATE) {
                // pr[0..2] IS the translation vector (single-axis producers write
                // it pre-scaled by the axis) - identical to the VSH joint delta.
                pose.translate(pr[0], pr[1], pr[2]);
            } else if (kind() == KIND_ROTATE) {
                pose.translate(pivotX(), pivotY(), pivotZ());
                pose.rotate((float) Math.toRadians(pr[0]), axisX(), axisY(), axisZ());
                pose.translate(-pivotX(), -pivotY(), -pivotZ());
            }
        }

        @Override
        public void encodeAnimParams(float[] pr, float[] out4) {
            if (track != null) {
                // Tracked rig: floats 28/29 carry the segment header (t0 float32,
                // u0/durationTicks halfwords), no streamed words follow - words
                // 4..7 are zeroed so MachineBer's reuse of encodedScratch cannot
                // leak another part's words into the skip-write compare (same
                // zero-tail contract as ParametricChain.encodeAnimParams).
                encodeTrackedHeader(pr, out4);
                out4[2] = 0f;
                out4[3] = 0f;
                return;
            }
            if (kind() == KIND_TRANSLATE) {
                // Words: w0..w2 = the translation vector, w3.. pad.
                out4[0] = com.hbm_m.client.render.HalfFloat.pack2(pr[0], pr[1]);
                out4[1] = com.hbm_m.client.render.HalfFloat.pack2(pr[2], 0f);
                out4[2] = 0f;
                out4[3] = 0f;
            } else if (kind() == KIND_TRANSLATE_AXIS) {
                // Words: w0 = the distance in blocks - a raw halfword, NOT
                // wrapped as an angle (matches applyToPose's axis*value and the
                // VSH's specA.x >= 1.5 branch, axis * word).
                out4[0] = com.hbm_m.client.render.HalfFloat.pack2(pr[0], 0f);
                out4[1] = 0f;
                out4[2] = 0f;
                out4[3] = 0f;
            } else {
                // KIND_ROTATE: one word - the angle wrapped to (-180, 180].
                out4[0] = com.hbm_m.client.render.HalfFloat.pack2(
                        com.hbm_m.client.render.HalfFloat.wrapDegrees(pr[0]), 0f);
                out4[1] = 0f;
                out4[2] = 0f;
                out4[3] = 0f;
            }
        }

        @Override
        public void applyRecordStatic(PoseStack pose, BlockEntity be, float partialTick) {
            if (baseYawDegrees != 0f) {
                pose.translate(0.5f, 0f, 0.5f);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(baseYawDegrees));
                pose.translate(-0.5f, 0f, -0.5f);
            }
            if (baseOffsetX != 0f || baseOffsetY != 0f || baseOffsetZ != 0f) {
                pose.translate(baseOffsetX, baseOffsetY, baseOffsetZ);
            }
        }
    }

    /** Shader loop bound: a joint chain may not exceed this many nodes. */
    public static final int MAX_CHAIN_NODES = 8;
    /**
     * Per-frame parameter budget of the record's AnimParams vec4 (attrib 15),
     * in BINARY16 WORDS: the 4 record floats pack 8 half-precision words (2 per
     * float, low first - decoded in the VSH by GLSL bit math (halfDecode), encoded
     * by {@code com.hbm_m.client.render.HalfFloat}). A rotate node costs 1 word
     * (the angle in degrees, wrapped to (-180, 180] - ulp &lt;= 0.125), an axis
     * translate costs 1 (the distance in blocks, ulp ~1 mm), a vector translate
     * costs 3 (the full vector). A declaration exceeding the budget fails fast
     * in {@link MachineSpecBuilder#jointChainPart}.
     * <p>
     * TRACKED rig (any node with a {@link Track}): the first two record floats
     * carry the segment header (t0 float32 + u0/duration halfwords), so the
     * streamed nodes are bounded by {@link #TRACKED_STREAM_BUDGET} (4) instead;
     * additionally the CPU mirror keeps one logical slot per tracked node after
     * the streamed words, all inside this same 8-float record array (streamed +
     * tracked &lt;= 5) - also validated at declaration.
     */
    public static final int CHAIN_PARAM_BUDGET = 8;
    /**
     * Chain node kind: translation along the node's (normalized) axis, costing
     * ONE parameter - the distance in blocks (negative = opposite). The 3-float
     * vector form ({@link #KIND_TRANSLATE}) exists only for the pre-chain
     * single-joint records; an arbitrary vector translate in a chain is three
     * axis nodes at the same total cost.
     */
    public static final int KIND_TRANSLATE_AXIS = 2;

    /**
     * Keyframe track of a tracked rig node (ANIM TRACK FORMAT v2): ordered
     * (t01, value) pairs, t01 strictly ascending within [0,1], plus the wrap
     * mode - {@code cycle} = looping ({@code u = fract(u)}), otherwise one-shot
     * ({@code u} clamped to [0,1]). The track is a PART constant: it is baked
     * into the {@link com.hbm_m.client.render.NucleusJointSpecs} keyframe pool
     * once at registration and sampled by the VSH; the values are absolute node
     * parameters (degrees for rotations, blocks for translates).
     * <p>
     * "Linear" motion is CPU sugar: {@link #linear} bakes it as a 2-point
     * keyframe track. The instance record carries no per-frame node value for a
     * tracked node - only the segment header (t0, u0, durationTicks, see
     * {@link ParametricRig#encodeAnimParams}); the VSH and the CPU mirror
     * ({@link #sampleTracked}) advance u from {@code uAnimTime} themselves.
     */
    public record Track(float[] times, float[] values, boolean cycle, boolean hermite, float rateScale) {
        public Track(float[] times, float[] values, boolean cycle) {
            this(times, values, cycle, false, 1.0f);
        }

        public Track(float[] times, float[] values, boolean cycle, boolean hermite) {
            this(times, values, cycle, hermite, 1.0f);
        }

        public Track {
            times = times.clone();
            values = values.clone();
            // Validate rateScale FIRST (before the keyframe checks): a NaN/<=0
            // scale would either freeze or rewind the tracked clock in both the
            // VSH and this CPU mirror.
            if (!Float.isFinite(rateScale) || rateScale <= 0f) {
                throw new IllegalArgumentException(
                        "[MachineRenderers] track rateScale must be finite and > 0: " + rateScale);
            }
            if (times.length != values.length || times.length < 2) {
                throw new IllegalArgumentException(
                        "[MachineRenderers] track needs equal times/values arrays of >= 2 keyframes");
            }
            for (int i = 0; i < times.length; i++) {
                if (!Float.isFinite(times[i]) || times[i] < 0f || times[i] > 1f) {
                    throw new IllegalArgumentException("[MachineRenderers] track t01 out of [0,1]: " + times[i]);
                }
                if (i > 0 && !(times[i] > times[i - 1])) {
                    throw new IllegalArgumentException("[MachineRenderers] track t01 must ascend strictly");
                }
                if (!Float.isFinite(values[i])) {
                    throw new IllegalArgumentException("[MachineRenderers] track value must be finite");
                }
            }
        }

        /**
         * A 2-point CYCLE track covering one motion cycle: value = phase +
         * pointsPerCycle * u (e.g. {@code linear(360, 0)} = one full revolution
         * per cycle - the cycle wrap erases the 360 seam). The segment duration
         * (ticks per cycle) rides the record header, not the track.
         */
        public static Track linear(float pointsPerCycle, float phaseDegrees) {
            return new Track(new float[] {0f, 1f},
                    new float[] {phaseDegrees, phaseDegrees + pointsPerCycle}, true, false, 1.0f);
        }

        /** A baked curve ({@code samplesOf(new float[]{...}, new float[]{...}, cycle)}). */
        public static Track samplesOf(float[] times, float[] values, boolean cycle) {
            return new Track(times, values, cycle, false, 1.0f);
        }

        /** A baked curve with Hermite reciprocating stroke spline evaluation. */
        public static Track hermite(float[] times, float[] values, boolean cycle) {
            return new Track(times, values, cycle, true, 1.0f);
        }

        /** A copy of this track with the given playback-rate multiplier (see {@link #rateScale()}). */
        public Track withRateScale(float scale) {
            return new Track(times, values, cycle, hermite, scale);
        }

        /**
         * CPU mirror of the VSH {@code trackSample}: segment lookup with a
         * bounded scan; u outside the key range pins to the first/last value.
         * No wrap here - the caller wraps/clamps u first (same as the VSH's
         * {@code trackedValue}).
         */
        public float sample(float u) {
            int n = times.length;
            if (u <= times[0]) {
                return values[0];
            }
            if (u >= times[n - 1]) {
                return values[n - 1];
            }
            for (int i = 1; i < n; i++) {
                if (u <= times[i]) {
                    float span = times[i] - times[i - 1];
                    float t = span > 0f ? (u - times[i - 1]) / span : 0f;
                    // GLSL mix(x, y, t) = x*(1-t) + y*t - the exact op order of
                    // the VSH trackSample, so the mirror lands on the same float
                    // (the algebraic a + (b-a)*t differs by an ulp).
                    return values[i - 1] * (1f - t) + values[i] * t;
                }
            }
            return values[n - 1];
        }
    }

    /**
     * Logical pr slots of a TRACKED rig (the record's AnimParams float array
     * layout, mirroring the VSH decode): [0] = t0 (segment start tick, float32 -
     * game ticks, exact), [1] = u0 (curve position at t0), [2] = durationTicks
     * (decoded binary16 == 0 means FROZEN: u stays u0). The STREAMED halfwords
     * of trackKind == 0 nodes start at slot 3 (the VSH decodes them from record
     * words 4..7) and the CPU mirror's per-tracked-node sample values follow
     * after them - both budgets are validated at declaration.
     */
    public static final int TRACKED_HEADER_SLOTS = 3;
    /**
     * Streamed halfword budget of a TRACKED rig: the header occupies record
     * floats 28/29 (t0 float32 + u0/duration halfwords), floats 30/31 carry at
     * most 4 streamed binary16 words (VSH word base 4).
     */
    public static final int TRACKED_STREAM_BUDGET = 4;

    /** Whether ANY node of the rig carries a {@link Track} (per the v2 format: per-RIG layout). */
    public static boolean isTrackedRig(java.util.List<? extends JointNode<?>> nodes) {
        for (JointNode<?> node : nodes) {
            if (node.track() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * The CPU mirror of the VSH {@code trackedValue}: advances the curve
     * position from the SAME half-rounded u0/duration the shader decodes
     * ({@link com.hbm_m.client.render.HalfFloat#pack} on the way in,
     * {@link #halfUnpack} on the way out) and the same split animation clock
     * the uniform carries ({@link com.hbm_m.client.render.FrameViewState
     * #animTicks()} / {@link com.hbm_m.client.render.FrameViewState
     * #animPartialTick()}, which already applies the pilots' -1-tick prev/curr
     * convention), then
     * wraps/clamps and samples the track. GPU and CPU land on the SAME u.
     */
    public static float sampleTracked(Track track, float t0, float u0, float durationTicks) {
        float u0h = halfUnpack(com.hbm_m.client.render.HalfFloat.pack(u0));
        float durh = halfUnpack(com.hbm_m.client.render.HalfFloat.pack(durationTicks));
        // Same split-time arithmetic as the VSH's trackedValue: (ticks - t0) is
        // exact (both are integer floats), the partial tick rides separately;
        // rateScale multiplies the advance rate (VSH: ((ticks - t0) + partial) / dur * rateScale).
        float dt = (com.hbm_m.client.render.FrameViewState.animTicks() - t0)
                + com.hbm_m.client.render.FrameViewState.animPartialTick();
        float u = durh == 0f ? u0h : u0h + (dt / durh) * track.rateScale();
        u = track.cycle() ? u - (float) Math.floor(u) : Math.max(0.0f, Math.min(1.0f, u));
        if (track.hermite()) {
            float stroke = u < 0.5f ? (4.0f * u * u * (3.0f - 4.0f * u))
                                   : (4.0f * (1.0f - u) * (1.0f - u) * (3.0f - 4.0f * (1.0f - u)));
            return track.sample(stroke);
        }
        return track.sample(u);
    }

    /**
     * binary16 decode mirroring the VSH {@code halfDecode} / the inverse of
     * {@link com.hbm_m.client.render.HalfFloat#pack} (the decode is exact -
     * RNE rounding is lost on the encode side only). Lives here because
     * HalfFloat is encode-only by contract (Mojang-free, unit-tested).
     */
    private static float halfUnpack(int h) {
        int s = (h & 0x8000) << 16;
        int e = (h >>> 10) & 0x1F;
        int m = h & 0x3FF;
        if (e == 0) {
            float val = (float) m * 5.9604645e-8f;
            return (h & 0x8000) != 0 ? -val : val;
        }
        if (e == 31) {
            return Float.intBitsToFloat(s | 0x7F800000 | (m << 13));
        }
        return Float.intBitsToFloat(s | ((e + 112) << 23) | (m << 13));
    }

    /**
     * Cross-rig contract shared by the single joint ({@link ParametricAnim}) and
     * the rigid joint chain ({@link ParametricChain}): MachineBer treats both
     * uniformly - base record statics (baseYaw/baseOffset), spec-texture
     * registration and the CPU-side composition for hook transforms. The rig is
     * a PART constant; per-frame values travel through the record's AnimParams.
     */
    public interface ParametricRig {

        boolean hasBaseOffset();

        boolean hasBaseYaw();

        default float baseOffsetX() { return 0f; }

        default float baseOffsetY() { return 0f; }

        default float baseOffsetZ() { return 0f; }

        default float baseYawDegrees() { return 0f; }

        /**
         * Registers the rig's joints in the {@link com.hbm_m.client.render.NucleusJointSpecs}
         * texture (idempotent) and returns the base joint index.
         */
        int registerSpecs();

    /**
     * Composes the per-frame joint delta into a CPU-side pose (hook
     * transforms): nodes in DECLARATION order - the same matrix product the
     * VSH applies in reverse vertex order. {@code pr} = the 4 record
     * AnimParams floats (LOGICAL values: degrees / blocks - not the packed
     * record words).
     */
    void applyToPose(Matrix4f pose, float[] pr);

    /**
     * Encodes the logical per-frame parameters ({@code pr}, the output of the
     * params fns) into the record's 4 AnimParams floats. UNTRACKED rig
     * (every node trackKind == 0): up to 8 binary16 words, 2 per float (low
     * first), one word per node parameter in declaration order - the exact
     * layout the VSH decodes with the VSH's {@code halfDecode}. Rotate angles
     * are wrapped to (-180, 180] so the half precision stays uniform; translate
     * distances are physically bounded. The encoding is a step function of the
     * logical value - sub-ulp jitter vanishes and the record skip-write
     * converges better than raw float32.
     * <p>
     * TRACKED rig (any node trackKind != 0): float 28 = t0 (segment start tick,
     * float32 game ticks, exact), float 29 = (u0 low half, durationTicks high
     * half; decoded 0 = FROZEN), floats 30/31 = the streamed halfwords of
     * trackKind == 0 nodes ONLY (declaration order, at most
     * {@link #TRACKED_STREAM_BUDGET}); tracked nodes carry no words - the VSH
     * samples their keyframe-pool track from t0/u0/duration itself. The
     * encoding must be a PURE function of (epochValue, rig constants) - no
     * per-frame inputs - so the epoch-keyed word cache in MachineBer hits and
     * the CPU skips eval while the segment runs.
     */
    void encodeAnimParams(float[] pr, float[] out4);

    /**
     * Per-frame record statics: applied by MachineBer to the record pose
     * before enqueue (replaces the old static baseYaw/baseOffset block).
     * Static transforms only (baseYaw group + baseOffset) - an ANIMATED mount
     * (turntable) is NOT applied here: it travels through the record's
     * AnimParams as chain node 0 (see {@link #chainWithTurntable}), keeping the
     * record fully static. {@code be} is guaranteed to be the owning spec's
     * BlockEntity type.
     */
    void applyRecordStatic(PoseStack pose, BlockEntity be, float partialTick);
}

    /**
     * Per-frame parameters of ONE chain node
     * ({@link MachineSpecBuilder#jointChainPart}). The engine zero-fills the
     * 4-float record array before the call and hands the node its parameter
     * offset: KIND_ROTATE writes {@code out[offset]} (angle in degrees),
     * KIND_TRANSLATE_AXIS writes {@code out[offset]} (distance in blocks),
     * KIND_TRANSLATE writes {@code out[offset..offset+2]} (the translation
     * vector in part-local blocks). {@code return false} = the part is not
     * drawn this frame.
     */
    @FunctionalInterface
    public interface NodeParams<T extends BlockEntity> {
        boolean params(T be, float partialTick, long gameTime, float[] out, int offset);
    }

    /**
     * One rigid node of a {@link ParametricChain}: a rotation about a fixed
     * axis through a fixed pivot ({@link #KIND_ROTATE}, parameter cost 1 - the
     * angle in degrees), a translation along a fixed axis ({@link #KIND_TRANSLATE_AXIS},
     * cost 1 - the distance in blocks) or a full vector translation
     * ({@link #KIND_TRANSLATE}, cost 3). Use the {@link #rotate} /
     * {@link #translate} / {@link #translateVector} factories (they normalize
     * the axis).
     * <p>
     * An optional {@link Track} turns the node into a KEYFRAMED node (ANIM
     * TRACK FORMAT v2): its parameter is not streamed - the record carries only
     * the rig's segment header (t0/u0/durationTicks) and the VSH (plus the CPU
     * mirror) sample the track itself; {@link #paramsFn} is never called for a
     * tracked node. Tracked KIND_TRANSLATE nodes are invalid (a scalar track
     * has no 3-word semantic) and rejected at declaration.
     */
    public record JointNode<T extends BlockEntity>(int kind, float axisX, float axisY, float axisZ,
                                                   float pivotX, float pivotY, float pivotZ,
                                                   NodeParams<T> paramsFn,
                                                   @Nullable Track track) {

        /** Record parameter cost: rotate 1, axis translate 1, vector translate 3. */
        public int cost() {
            return kind == KIND_TRANSLATE ? 3 : 1;
        }

        /** Tracked KIND_ROTATE: the angle in degrees comes from {@code track}. */
        public static <B extends BlockEntity> JointNode<B> rotate(float axisX, float axisY, float axisZ,
                                                                  float pivotX, float pivotY, float pivotZ,
                                                                  NodeParams<B> paramsFn,
                                                                  @Nullable Track track) {
            float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
            if (len < 1.0e-6f) len = 1f;
            return new JointNode<>(KIND_ROTATE, axisX / len, axisY / len, axisZ / len,
                    pivotX, pivotY, pivotZ, paramsFn, track);
        }

        /** Tracked KIND_TRANSLATE_AXIS: the block distance comes from {@code track}. */
        public static <B extends BlockEntity> JointNode<B> translate(float axisX, float axisY, float axisZ,
                                                                     NodeParams<B> paramsFn,
                                                                     @Nullable Track track) {
            float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
            if (len < 1.0e-6f) len = 1f;
            return new JointNode<>(KIND_TRANSLATE_AXIS, axisX / len, axisY / len, axisZ / len,
                    0f, 0f, 0f, paramsFn, track);
        }

        public static <B extends BlockEntity> JointNode<B> rotate(float axisX, float axisY, float axisZ,
                                                                  float pivotX, float pivotY, float pivotZ,
                                                                  NodeParams<B> paramsFn) {
            return rotate(axisX, axisY, axisZ, pivotX, pivotY, pivotZ, paramsFn, null);
        }

        /**
         * Translation along {@code axis} by the node's single parameter
         * (blocks; negative = opposite direction). The axis is normalized.
         */
        public static <B extends BlockEntity> JointNode<B> translate(float axisX, float axisY, float axisZ,
                                                                     NodeParams<B> paramsFn) {
            return translate(axisX, axisY, axisZ, paramsFn, null);
        }

        /**
         * Full vector translation: the node's THREE parameters are the vector
         * in part-local blocks (the axis is informational, like the single-joint
         * KIND_TRANSLATE). Same cost as three axis nodes.
         */
        public static <B extends BlockEntity> JointNode<B> translateVector(float axisX, float axisY, float axisZ,
                                                                           NodeParams<B> paramsFn) {
            float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
            if (len < 1.0e-6f) len = 1f;
            return new JointNode<>(KIND_TRANSLATE, axisX / len, axisY / len, axisZ / len,
                    0f, 0f, 0f, paramsFn, null);
        }
    }

    /**
     * A rotating mount for a chain (a ring, drum or carousel): a per-frame
     * rotation about a fixed axis through a fixed pivot. Since the packed
     * AnimParams budget (8 words) covers mount + joints, the mount is folded
     * into the chain as node 0 by {@link #chainWithTurntable} - the record
     * statics stay fully static (no per-frame rebuild, the span-diff/skip-write
     * of the whole machine converges) and only the record's AnimParams tail
     * changes while the ring turns.
     */
    public record Turntable(float axisX, float axisY, float axisZ,
                            float pivotX, float pivotY, float pivotZ) {
    }

    /**
     * Folds a {@link Turntable} mount into a chain as node 0 (the OUTERMOST
     * transform - declared first, applied to the vertex last). The node pivot is
     * {@code turntable.pivot - baseOffset}, which makes the composition
     * bit-identical to the legacy record statics order: with the mount in the
     * record, the product was {@code T(p)·R·T(-p)·T(off)}; as node 0 it is
     * {@code T(off)·T(p-off)·R·T(off-p)} - the translates cancel to the same
     * total, so pivots declared for the mount-rotated space keep working
     * unchanged. The per-frame parameter (the mount angle in degrees) is word 0
     * of the record's AnimParams.
     */
    public static <T extends BlockEntity> List<JointNode<T>> chainWithTurntable(
            List<JointNode<T>> nodes, Turntable turntable,
            float baseOffsetX, float baseOffsetY, float baseOffsetZ,
            java.util.function.ToDoubleBiFunction<T, Float> angleFn) {
        JointNode<T> mount = new JointNode<>(KIND_ROTATE,
                turntable.axisX(), turntable.axisY(), turntable.axisZ(),
                turntable.pivotX() - baseOffsetX,
                turntable.pivotY() - baseOffsetY,
                turntable.pivotZ() - baseOffsetZ,
                (be, pt, gt, out, off) -> {
                    out[off] = (float) angleFn.applyAsDouble(be, pt);
                    return true;
                },
                null);
        List<JointNode<T>> out = new ArrayList<>(nodes.size() + 1);
        out.add(mount);
        out.addAll(nodes);
        return List.copyOf(out);
    }

    /**
     * A rigid FK chain of joints applied, in declaration order, in the part's
     * local space BEFORE the record transform - the exact composition the CPU
     * animators write as a PoseStack op sequence (translate·rotate·translate·rotate...:
     * ops declared first are leftmost in the matrix product, so the vertex
     * meets the LAST-declared node first; the VSH walks the chain in that
     * reverse order, the CPU composes in declaration order - both reproduce
     * M = N1·N2·…·Nk). Every existing single joint is the length-1 case.
     * <p>
     * Per-node static offsets are not supported: a rotation pivot covers the
     * rotation center and the record-level baseOffset/baseYaw cover the static
     * transforms around the WHOLE chain (pre-chain rotation / post-chain
     * translation, mirroring ParametricAnim); inter-node static transforms must
     * be folded into the node pivots or the model. An ANIMATED mount (ring)
     * joins the chain as node 0 via {@link #chainWithTurntable}.
     */
    /**
     * Declaration-time constants of a {@link ParametricChain} rig, derived ONCE
     * in the chain constructor: the node-list aggregates the per-frame
     * eval/encode loops need, so the hot path never re-iterates the nodes to
     * re-count streamed words / tracked nodes. {@code kinds}/{@code costs}
     * mirror the node list in declaration order
     * ({@code costs[i] == nodes.get(i).cost()}). Immutable; the arrays are
     * private to the rig's fast paths.
     */
    public static final class RigLayout {
        /** Whether ANY node carries a {@link Track} (the per-RIG v2 layout switch). */
        final boolean tracked;
        /** Sum of all node parameter costs (the untracked word budget). */
        final int totalCost;
        /** Total streamed halfwords of the rig's trackKind==0 nodes (declaration order). */
        final int streamedWords;
        /** Tracked (keyframed) node count - one CPU-mirror slot each. */
        final int trackedNodes;
        final int[] kinds;
        final int[] costs;

        private RigLayout(boolean tracked, int totalCost, int streamedWords, int trackedNodes,
                          int[] kinds, int[] costs) {
            this.tracked = tracked;
            this.totalCost = totalCost;
            this.streamedWords = streamedWords;
            this.trackedNodes = trackedNodes;
            this.kinds = kinds;
            this.costs = costs;
        }

        static <T extends BlockEntity> RigLayout of(List<JointNode<T>> nodes) {
            int n = nodes.size();
            int[] kinds = new int[n];
            int[] costs = new int[n];
            boolean tracked = false;
            int total = 0;
            int streamed = 0;
            int trackedCount = 0;
            for (int i = 0; i < n; i++) {
                JointNode<?> node = nodes.get(i);
                int cost = node.cost();
                kinds[i] = node.kind();
                costs[i] = cost;
                total += cost;
                if (node.track() != null) {
                    tracked = true;
                    trackedCount++;
                } else {
                    streamed += cost;
                }
            }
            return new RigLayout(tracked, total, streamed, trackedCount, kinds, costs);
        }
    }

    public record ParametricChain<T extends BlockEntity>(List<JointNode<T>> nodes,
                                                         float baseOffsetX, float baseOffsetY, float baseOffsetZ,
                                                         float baseYawDegrees,
                                                         RigLayout layout
    ) implements ParametricRig {

        public ParametricChain {
            nodes = List.copyOf(nodes);
            // Rig constants are derived here, once per declaration - the passed
            // layout value is always recomputed from the (copied) nodes.
            layout = RigLayout.of(nodes);
        }

        /** Convenience form: the {@link RigLayout} is derived from the nodes. */
        public ParametricChain(List<JointNode<T>> nodes, float baseOffsetX, float baseOffsetY,
                               float baseOffsetZ, float baseYawDegrees) {
            this(nodes, baseOffsetX, baseOffsetY, baseOffsetZ, baseYawDegrees, null);
        }

        @Override
        public boolean hasBaseOffset() {
            return baseOffsetX != 0f || baseOffsetY != 0f || baseOffsetZ != 0f;
        }

        @Override
        public boolean hasBaseYaw() {
            return baseYawDegrees != 0f;
        }

        @Override
        public int registerSpecs() {
            int n = nodes.size();
            int[] kinds = new int[n];
            float[] axes = new float[n * 3];
            float[] pivots = new float[n * 3];
            com.hbm_m.client.render.NucleusJointSpecs.TrackRef[] refs = null;
            int[] flags = null;
            for (int i = 0; i < n; i++) {
                JointNode<T> node = nodes.get(i);
                kinds[i] = node.kind();
                axes[i * 3] = node.axisX();
                axes[i * 3 + 1] = node.axisY();
                axes[i * 3 + 2] = node.axisZ();
                pivots[i * 3] = node.pivotX();
                pivots[i * 3 + 1] = node.pivotY();
                pivots[i * 3 + 2] = node.pivotZ();
                if (node.track() != null) {
                    if (refs == null) {
                        refs = new com.hbm_m.client.render.NucleusJointSpecs.TrackRef[n];
                        flags = new int[n];
                    }
                    refs[i] = com.hbm_m.client.render.NucleusJointSpecs.registerTrack(
                            node.track().times(), node.track().values(), node.track().cycle(),
                            node.track().rateScale());
                    flags[i] = (node.track().cycle() ? com.hbm_m.client.render.NucleusJointSpecs.FLAG_CYCLE : 0)
                            | (node.track().hermite() ? com.hbm_m.client.render.NucleusJointSpecs.FLAG_HERMITE : 0);
                }
            }
            return com.hbm_m.client.render.NucleusJointSpecs.registerChain(kinds, axes, pivots, refs, flags);
        }

        @Override
        public void applyToPose(Matrix4f pose, float[] pr) {
            if (layout.tracked) {
                composeTrackedNodes(nodes, pr, pose, layout.streamedWords);
                return;
            }
            composeUntrackedNodes(nodes, pr, pose);
        }

        /**
         * Per-frame rig evaluation - the fast path of
         * {@link MachineSpec#evalChainParams(List, BlockEntity, float, long, float[])} on
         * the precomputed {@link #layout}: no node-list re-iteration for the
         * streamed/tracked counts, no per-node {@code cost()} dispatch. Same
         * boolean contract; the out4 contents match the List path in every slot
         * any consumer reads.
         *
         * <p><b>Caller-zero contract</b> (hot-path optimization): out4 must be
         * zeroed by the CALLER before this call - the render path
         * ({@code MachineBer}) zero-fills its whole scratch per part and the
         * epoch sampler zero-fills its scratch; the CPU-fallback animators
         * zero the shared scratch explicitly. This method only WRITES the
         * slots it owns: untracked rigs write the node region
         * {@code [0, totalCost)} (a "not drawn" early return may leave the
         * tail stale, but a false return is never consumed); tracked rigs
         * read the caller-written header out4[0..2] and write
         * {@code [3, 3 + streamed + tracked)}.
         */
        public boolean evalParams(T be, float partialTick, long gameTime, float[] out4) {
            RigLayout L = layout;
            if (!L.tracked) {
                int off = 0;
                List<JointNode<T>> ns = nodes;
                for (int i = 0, n = ns.size(); i < n; i++) {
                    if (!ns.get(i).paramsFn().params(be, partialTick, gameTime, out4, off)) {
                        return false;
                    }
                    off += L.costs[i];
                }
                return true;
            }
            int streamed = L.streamedWords;
            float t0 = out4[0];
            float u0 = out4[1];
            float dur = out4[2];
            int slot = TRACKED_HEADER_SLOTS;
            int valueSlot = TRACKED_HEADER_SLOTS + streamed;
            List<JointNode<T>> ns = nodes;
            for (int i = 0, n = ns.size(); i < n; i++) {
                JointNode<T> node = ns.get(i);
                Track track = node.track();
                if (track == null) {
                    if (!node.paramsFn().params(be, partialTick, gameTime, out4, slot)) {
                        return false;
                    }
                    slot += L.costs[i];
                } else {
                    out4[valueSlot++] = sampleTracked(track, t0, u0, dur);
                }
            }
            return true;
        }

        @Override
        public void encodeAnimParams(float[] pr, float[] out4) {
            RigLayout L = layout;
            if (L.tracked) {
                // Tracked rig: the header (written into pr[0..2] by the
                // builder's params fn) + the streamed halfwords of trackKind==0
                // nodes from pr[3..]; tracked nodes carry no words.
                encodeTrackedHeader(pr, out4);
                int streamed = L.streamedWords;
                for (int w = 0; w < streamed; w++) {
                    PACK_WORDS[w] = com.hbm_m.client.render.HalfFloat.pack(pr[TRACKED_HEADER_SLOTS + w]);
                }
                for (int w = streamed; w < PACK_WORDS.length; w++) {
                    PACK_WORDS[w] = 0;
                }
                out4[2] = com.hbm_m.client.render.HalfFloat.pack2Raw(PACK_WORDS[0], PACK_WORDS[1]);
                out4[3] = com.hbm_m.client.render.HalfFloat.pack2Raw(PACK_WORDS[2], PACK_WORDS[3]);
                return;
            }
            // Word stream: one word per rotate/axis-translate parameter, three
            // per vector translate, in declaration order - the layout the VSH
            // decodes with halfDecode bit math. Kinds/costs come from the
            // precomputed rig layout (no node re-iteration). Static scratch
            // (rendering is single-threaded); the tail is zeroed so reused
            // slots cannot leak into the padding words.
            int off = 0;
            int nw = 0;
            int[] kinds = L.kinds;
            for (int i = 0, n = kinds.length; i < n; i++) {
                int kind = kinds[i];
                if (kind == KIND_TRANSLATE) {
                    PACK_WORDS[nw++] = com.hbm_m.client.render.HalfFloat.pack(pr[off]);
                    PACK_WORDS[nw++] = com.hbm_m.client.render.HalfFloat.pack(pr[off + 1]);
                    PACK_WORDS[nw++] = com.hbm_m.client.render.HalfFloat.pack(pr[off + 2]);
                    off += 3;
                } else if (kind == KIND_TRANSLATE_AXIS) {
                    PACK_WORDS[nw++] = com.hbm_m.client.render.HalfFloat.pack(pr[off]);
                    off += 1;
                } else {
                    PACK_WORDS[nw++] = com.hbm_m.client.render.HalfFloat.pack(
                            com.hbm_m.client.render.HalfFloat.wrapDegrees(pr[off]));
                    off += 1;
                }
            }
            for (int i = nw; i < PACK_WORDS.length; i++) {
                PACK_WORDS[i] = 0;
            }
            out4[0] = com.hbm_m.client.render.HalfFloat.pack2Raw(PACK_WORDS[0], PACK_WORDS[1]);
            out4[1] = com.hbm_m.client.render.HalfFloat.pack2Raw(PACK_WORDS[2], PACK_WORDS[3]);
            out4[2] = com.hbm_m.client.render.HalfFloat.pack2Raw(PACK_WORDS[4], PACK_WORDS[5]);
            out4[3] = com.hbm_m.client.render.HalfFloat.pack2Raw(PACK_WORDS[6], PACK_WORDS[7]);
        }

        @Override
        public void applyRecordStatic(PoseStack pose, BlockEntity be, float partialTick) {
            if (baseYawDegrees != 0f) {
                pose.translate(0.5f, 0f, 0.5f);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(baseYawDegrees));
                pose.translate(-0.5f, 0f, -0.5f);
            }
            if (baseOffsetX != 0f || baseOffsetY != 0f || baseOffsetZ != 0f) {
                pose.translate(baseOffsetX, baseOffsetY, baseOffsetZ);
            }
        }
    }

    /** Scratch for {@link ParametricChain#encodeAnimParams} (single-threaded rendering). */
    private static final int[] PACK_WORDS = new int[MAX_CHAIN_NODES];

    /**
     * Writes the tracked rig's record header: float 28 = pr[0] (t0, float32
     * game ticks - the raw float is carried verbatim, NOT a halfword), float 29
     * = (u0 low half, durationTicks high half; decoded 0 = FROZEN). Words 0..3
     * (floats 28/29) hold nothing else - the streamed halfwords start at record
     * word 4 (float 30), see {@link #TRACKED_STREAM_BUDGET}.
     */
    private static void encodeTrackedHeader(float[] pr, float[] out4) {
        out4[0] = pr[0];
        out4[1] = com.hbm_m.client.render.HalfFloat.pack2Raw(
                com.hbm_m.client.render.HalfFloat.pack(pr[1]),
                com.hbm_m.client.render.HalfFloat.pack(pr[2]));
    }

    /** Total streamed halfwords of the rig's trackKind==0 nodes (declaration order). */
    private static int streamedWordCount(java.util.List<? extends JointNode<?>> nodes) {
        int words = 0;
        for (JointNode<?> node : nodes) {
            if (node.track() == null) {
                words += node.cost();
            }
        }
        return words;
    }

    /** Count of tracked (keyframed) nodes - they occupy one CPU mirror slot each. */
    private static int trackedNodeCount(java.util.List<? extends JointNode<?>> nodes) {
        int n = 0;
        for (JointNode<?> node : nodes) {
            if (node.track() != null) {
                n++;
            }
        }
        return n;
    }

    /**
     * Evaluates the per-frame parameters of a chain into the record's 4-float
     * array. UNTRACKED rig: zero-fills all four first (the skip-write compares
     * them byte-for-byte - stale scratch would degrade it into a per-frame tail
     * write), then walks the nodes in declaration order handing each its
     * parameter offset. TRACKED rig: pr[0..2] must already carry the segment
     * header (t0/u0/duration - written by the builder's params fn BEFORE this
     * call); only the node region [3, 3 + streamed + tracked) is zeroed, the
     * streamed nodes fill their slots via their fns and each tracked node's
     * CPU-mirror sample value lands in the slot after them (for
     * {@link #applyChainNodes} / hook transforms). {@code return false} = the
     * part is not drawn.
     * <p>
     * Per-frame hot path: {@link ParametricChain#evalParams} - same contract on
     * the rig's precomputed {@link RigLayout}, without the per-call node
     * re-iteration this List form performs.
     */
    public static <T extends BlockEntity> boolean evalChainParams(List<JointNode<T>> nodes,
                                                                  T be, float partialTick, long gameTime,
                                                                  float[] out4) {
        if (!isTrackedRig(nodes)) {
            java.util.Arrays.fill(out4, 0f);
            int off = 0;
            for (JointNode<T> node : nodes) {
                if (!node.paramsFn().params(be, partialTick, gameTime, out4, off)) {
                    return false;
                }
                off += node.cost();
            }
            return true;
        }
        return evalTrackedNodes(nodes, be, partialTick, gameTime, out4);
    }

    /**
     * Tracked branch of {@link #evalChainParams}: preserves the pre-written
     * header in pr[0..2], evaluates streamed nodes through their fns at slots
     * from {@link #TRACKED_HEADER_SLOTS} and tracked nodes through the CPU
     * mirror ({@link #sampleTracked}) into the trailing value slots.
     */
    public static <T extends BlockEntity> boolean evalTrackedNodes(List<JointNode<T>> nodes,
                                                                   T be, float partialTick, long gameTime,
                                                                   float[] out4) {
        int streamed = streamedWordCount(nodes);
        int used = TRACKED_HEADER_SLOTS + streamed + trackedNodeCount(nodes);
        for (int i = TRACKED_HEADER_SLOTS; i < used; i++) {
            out4[i] = 0f;
        }
        float t0 = out4[0];
        float u0 = out4[1];
        float dur = out4[2];
        int slot = TRACKED_HEADER_SLOTS;
        int valueSlot = TRACKED_HEADER_SLOTS + streamed;
        for (JointNode<T> node : nodes) {
            Track track = node.track();
            if (track == null) {
                if (!node.paramsFn().params(be, partialTick, gameTime, out4, slot)) {
                    return false;
                }
                slot += node.cost();
            } else {
                out4[valueSlot++] = sampleTracked(track, t0, u0, dur);
            }
        }
        return true;
    }

    /** CPU composition of a chain onto a {@link Matrix4f} (hook transforms). */
    public static <T extends BlockEntity> void applyChainNodes(List<JointNode<T>> nodes, float[] pr, Matrix4f pose) {
        if (isTrackedRig(nodes)) {
            composeTrackedNodes(nodes, pr, pose, streamedWordCount(nodes));
            return;
        }
        composeUntrackedNodes(nodes, pr, pose);
    }

    /** Untracked declaration-order composition onto a {@link Matrix4f} (shared by the static entry and the rig). */
    private static <T extends BlockEntity> void composeUntrackedNodes(List<JointNode<T>> nodes, float[] pr, Matrix4f pose) {
        int off = 0;
        for (JointNode<T> node : nodes) {
            if (node.kind() == KIND_TRANSLATE) {
                pose.translate(pr[off], pr[off + 1], pr[off + 2]);
            } else if (node.kind() == KIND_TRANSLATE_AXIS) {
                pose.translate(node.axisX() * pr[off], node.axisY() * pr[off], node.axisZ() * pr[off]);
            } else {
                pose.translate(node.pivotX(), node.pivotY(), node.pivotZ());
                pose.rotate((float) Math.toRadians(pr[off]), node.axisX(), node.axisY(), node.axisZ());
                pose.translate(-node.pivotX(), -node.pivotY(), -node.pivotZ());
            }
            off += node.cost();
        }
    }

    /**
     * CPU composition of a chain onto a {@link PoseStack} (the auto-derived
     * legacy fallback animator): the same declaration-order op sequence.
     */
    public static <T extends BlockEntity> void applyChainNodes(List<JointNode<T>> nodes, float[] pr, PoseStack pose) {
        if (isTrackedRig(nodes)) {
            composeTrackedNodes(nodes, pr, pose, streamedWordCount(nodes));
            return;
        }
        composeUntrackedNodes(nodes, pr, pose);
    }

    /** Untracked declaration-order composition onto a {@link PoseStack} (shared by the static entry and the rig). */
    private static <T extends BlockEntity> void composeUntrackedNodes(List<JointNode<T>> nodes, float[] pr, PoseStack pose) {
        int off = 0;
        for (JointNode<T> node : nodes) {
            if (node.kind() == KIND_TRANSLATE) {
                pose.translate(pr[off], pr[off + 1], pr[off + 2]);
            } else if (node.kind() == KIND_TRANSLATE_AXIS) {
                pose.translate(node.axisX() * pr[off], node.axisY() * pr[off], node.axisZ() * pr[off]);
            } else {
                pose.translate(node.pivotX(), node.pivotY(), node.pivotZ());
                pose.mulPose(new org.joml.Quaternionf()
                        .rotationAxis((float) Math.toRadians(pr[off]), node.axisX(), node.axisY(), node.axisZ()));
                pose.translate(-node.pivotX(), -node.pivotY(), -node.pivotZ());
            }
            off += node.cost();
        }
    }

    /**
     * Slot cursor over a tracked rig's pr array: streamed nodes read their
     * logical values from pr[3..] (mirroring the VSH's word base 4), tracked
     * nodes read their CPU-mirror sample value from the trailing value slots
     * filled by {@link #evalTrackedNodes}.
     */
    private static final class TrackedCursor {
        private final float[] pr;
        private int slot = TRACKED_HEADER_SLOTS;
        private int valueSlot;

        TrackedCursor(int streamedWords, float[] pr) {
            this.pr = pr;
            this.valueSlot = TRACKED_HEADER_SLOTS + streamedWords;
        }

        TrackedCursor(java.util.List<? extends JointNode<?>> nodes, float[] pr) {
            this(streamedWordCount(nodes), pr);
        }

        float next(JointNode<?> node) {
            if (node.track() != null) {
                return pr[valueSlot++];
            }
            float v = pr[slot];
            slot += node.cost();
            return v;
        }

        /** Vector-translate nodes consume THREE consecutive slots. */
        float x() { return pr[slot]; }

        float y() { return pr[slot + 1]; }

        float z() { return pr[slot + 2]; }

        void advanceVector(JointNode<?> node) {
            slot += node.cost();
        }
    }

    /**
     * Tracked variant of {@link #applyChainNodes(List, float[], Matrix4f)}:
     * same per-node ops, but the values come from the tracked pr layout.
     * {@code streamedWords} is the rig's precomputed count (the List entry
     * passes {@link #streamedWordCount}).
     */
    private static void composeTrackedNodes(List<? extends JointNode<?>> nodes, float[] pr, Matrix4f pose,
                                            int streamedWords) {
        TrackedCursor c = new TrackedCursor(streamedWords, pr);
        for (JointNode<?> node : nodes) {
            if (node.kind() == KIND_TRANSLATE) {
                pose.translate(c.x(), c.y(), c.z());
                c.advanceVector(node);
            } else if (node.kind() == KIND_TRANSLATE_AXIS) {
                float v = c.next(node);
                pose.translate(node.axisX() * v, node.axisY() * v, node.axisZ() * v);
            } else {
                float v = c.next(node);
                pose.translate(node.pivotX(), node.pivotY(), node.pivotZ());
                pose.rotate((float) Math.toRadians(v), node.axisX(), node.axisY(), node.axisZ());
                pose.translate(-node.pivotX(), -node.pivotY(), -node.pivotZ());
            }
        }
    }

    /** Tracked variant of {@link #applyChainNodes(List, float[], PoseStack)}. */
    private static void composeTrackedNodes(List<? extends JointNode<?>> nodes, float[] pr, PoseStack pose,
                                            int streamedWords) {
        TrackedCursor c = new TrackedCursor(streamedWords, pr);
        for (JointNode<?> node : nodes) {
            if (node.kind() == KIND_TRANSLATE) {
                pose.translate(c.x(), c.y(), c.z());
                c.advanceVector(node);
            } else if (node.kind() == KIND_TRANSLATE_AXIS) {
                float v = c.next(node);
                pose.translate(node.axisX() * v, node.axisY() * v, node.axisZ() * v);
            } else {
                float v = c.next(node);
                pose.translate(node.pivotX(), node.pivotY(), node.pivotZ());
                pose.mulPose(new org.joml.Quaternionf()
                        .rotationAxis((float) Math.toRadians(v), node.axisX(), node.axisY(), node.axisZ()));
                pose.translate(-node.pivotX(), -node.pivotY(), -node.pivotZ());
            }
        }
    }

    /**
     * Per-frame joint parameters: MUST write all of {@code out4[0..2]} (the record
     * carries them verbatim and the skip-write compares them byte-for-byte).
     * KIND_ROTATE: [0] = angle in degrees ([1]/[2] = 0). KIND_TRANSLATE: xyz = the
     * translation vector in part-local coordinates (blocks). For joint CHAINS the
     * DSL generates this fn ({@link #evalChainParams}) and all four components may
     * carry node parameters. Contract: values must
     * CONVERGE to stable ones when the machine is idle (otherwise skip-write
     * degrades into a per-frame tail write). {@code return false} = the part is not
     * drawn this frame.
     */
    @FunctionalInterface
    public interface ParametricParams<T extends BlockEntity> {
        boolean params(T be, float partialTick, long gameTime, float[] out4);
    }

    final String id;
    final Class<T> beClass;
    final net.minecraft.world.level.block.entity.BlockEntityType<T> type;
    final Function<T, BakedModel> modelResolver;
    final Function<T, Direction> facingResolver;
    final List<PartDef<T>> parts;
    final List<MachineRenderHook<T>> hooks;
    final int viewDistance; // -1 = default from the static config
    /** Precomputed: whether the spec has animated parts (fast-path dirty-skip gate). */
    final boolean hasAnimatedParts;
    /**
     * Precomputed: whether there are parts with DYNAMIC geometry AND an animator
     * (doors, transition seal). Their pose depends on time (openTicks/animation), but
     * animated=false - such parts MUST be rendered via the full path every frame:
     * a roster-assert fast path would freeze the pose (the door tick does not set
     * render-dirty), producing "binary" animation.
     */
    final boolean hasDynamicAnimators;
    /** Precomputed: whether the spec has dynamic parts (per-BE geometry quads, e.g. fluid levels, electrodes). */
    final boolean hasDynamicParts;
    /** Dynamic-animator or tint-bearing parts - the full fast assert can never confirm them (see {@link #hasNonAssertableParts()}). */
    final boolean hasNonAssertableParts;
    /** Whether there are parts using GPU-parametric rigs (AnimParams). */
    final boolean hasParametricParts;
    /** Stable key for LightSampleCache: one 8-corner sample per machine per frame. */
    final long lightSampleKey;
    @Nullable final MachineSpecBuilder.BlockTransform<T> blockTransform; // null = default setupBlockTransform
    /**
     * Machine animation-epoch (see {@link MachineSpecBuilder#animationEpoch}) -
     * key for the full roster-assert of frozen animated machines.
     * null = the spec provides no epoch (animated parts always take the full path).
     */
    @Nullable final java.util.function.ToLongFunction<T> animationEpoch;

    // Multipart model config (ConfiguredMultipartBakedModel); merged into the
    // instance at the end of model baking (MachineRenderRegistry.bindBakedModels).
    /** Parts for item rendering; null = all model parts. */
    final @Nullable List<String> itemParts;
    /** Exclusions from item rendering; applied after itemParts (when that is null). */
    final @Nullable List<String> itemExcept;
    /** Render types of the chunk pass (forge getRenderTypes); null = default. */
    final @Nullable List<net.minecraft.client.renderer.RenderType> chunkRenderTypes;

    // Runtime: full cache key -> part GPU holder. Cached across frames.
    private final Map<String, MachinePartRenderer> partRenderers = new ConcurrentHashMap<>();
    /** Direct array index cache for static part renderers to eliminate Map query overhead. */
    private final MachinePartRenderer[] staticRenderers;

    /**
     * Runtime: dynKey -> holder, one map per part (key is the unique part name).
     * The full composite key "id/name/dynKey" is built ONLY when creating a holder:
     * on the hot path (findExistingRenderer/partRendererLazy - every frame for every
     * dynamic part of every machine) only one hash of the short dynKey remains, with
     * no String concatenation.
     */
    private final Map<String, ConcurrentHashMap<String, MachinePartRenderer>> dynPartRenderers = new ConcurrentHashMap<>();
    /** Sentinel key for dynamic parts without dynamicCacheKey (ConcurrentHashMap dislikes null). */
    private static final String NULL_DYN_KEY = "\u0000";

    private static String dynMapKey(@Nullable String dynamicKey) {
        return dynamicKey == null ? NULL_DYN_KEY : dynamicKey;
    }

    private ConcurrentHashMap<String, MachinePartRenderer> dynMap(PartDef<T> part) {
        return dynPartRenderers.computeIfAbsent(part.name(), n -> new ConcurrentHashMap<>());
    }

    MachineSpec(String id, Class<T> beClass, net.minecraft.world.level.block.entity.BlockEntityType<T> type,
                Function<T, BakedModel> modelResolver,
                Function<T, Direction> facingResolver, List<PartDef<T>> parts,
                List<MachineRenderHook<T>> hooks, int viewDistance,
                @Nullable MachineSpecBuilder.BlockTransform<T> blockTransform,
                @Nullable List<String> itemParts, @Nullable List<String> itemExcept,
                @Nullable List<net.minecraft.client.renderer.RenderType> chunkRenderTypes,
                @Nullable java.util.function.ToLongFunction<T> animationEpoch) {
        this.id = id;
        this.beClass = beClass;
        this.type = type;
        this.modelResolver = modelResolver;
        this.facingResolver = facingResolver;
        this.parts = List.copyOf(parts);
        this.staticRenderers = new MachinePartRenderer[this.parts.size()];
        this.hooks = List.copyOf(hooks);
        this.viewDistance = viewDistance;
        boolean anyAnimated = false;
        boolean anyDynamicAnimator = false;
        boolean anyDynamic = false;
        for (PartDef<T> p : parts) {
            if (p.animated()) {
                anyAnimated = true;
            }
            // No p.animated() gate: dynamicPart-created parts are animator != null,
            // animated == false, dynamic == true - they must still be treated as
            // dynamic animators, exactly like MachineBer's staticPose test
            // (!animated && !(animator != null && dynamic)) does.
            if (p.animator() != null && p.dynamic()) {
                anyDynamicAnimator = true;
            }
            if (p.dynamic()) {
                anyDynamic = true;
            }
        }
        this.hasAnimatedParts = anyAnimated;
        this.hasDynamicAnimators = anyDynamicAnimator;
        this.hasDynamicParts = anyDynamic;
        boolean anyNonAssertable = false;
        for (PartDef<T> p : parts) {
            if ((p.animator() != null && p.dynamic()) || p.tintOverride() != null || p.clipPlanesResolver() != null) {
                anyNonAssertable = true;
                break;
            }
        }
        this.hasNonAssertableParts = anyNonAssertable;
        boolean anyParametric = false;
        for (PartDef<T> p : parts) {
            if (p.parametric() != null) {
                anyParametric = true;
                break;
            }
        }
        this.hasParametricParts = anyParametric;
        this.lightSampleKey = (0x4D4143484C534B4FL) ^ (id.hashCode() * 0x9E3779B97F4A7C15L);
        this.blockTransform = blockTransform;
        this.animationEpoch = animationEpoch;
        this.itemParts = itemParts == null ? null : List.copyOf(itemParts);
        this.itemExcept = itemExcept == null ? null : List.copyOf(itemExcept);
        this.chunkRenderTypes = chunkRenderTypes == null ? null : List.copyOf(chunkRenderTypes);
    }

    public net.minecraft.world.level.block.entity.BlockEntityType<T> type() { return type; }
    @Nullable List<String> itemParts() { return itemParts; }
    @Nullable List<String> itemExcept() { return itemExcept; }
    @Nullable List<net.minecraft.client.renderer.RenderType> chunkRenderTypes() { return chunkRenderTypes; }

    /**
     * Checks if a part name represents an optional structure frame (e.g. ceiling support).
     */
    public static boolean isFramePart(String name) {
        return name != null && name.equalsIgnoreCase("Frame");
    }

    /**
     * Final part list for item rendering. An explicit {@code itemParts} wins;
     * by default - the spec's NON-dynamic, non-conditional, and non-frame parts
     * (what the BER draws as static and animated), minus {@code itemExcept}.
     * Dynamic parts (per-BE geometry: fluid levels), conditional frame parts,
     * and undeclared parts altogether (junk OBJ groups) never make it into the item render.
     */
    List<String> deriveItemParts() {
        List<String> out = new ArrayList<>();
        if (itemParts != null) {
            out.addAll(itemParts);
        } else {
            for (PartDef<?> p : parts) {
                if (!p.dynamic() && !p.isConditional() && !isFramePart(p.modelPartName()) && !out.contains(p.modelPartName())) {
                    out.add(p.modelPartName());
                }
            }
        }
        if (itemExcept != null) {
            out.removeIf(itemExcept::contains);
        }
        out.removeIf(MachineSpec::isFramePart);
        return out;
    }

    public @Nullable MachineSpecBuilder.BlockTransform<T> blockTransform() { return blockTransform; }

    /** Machine animation-epoch or null (see {@link MachineSpecBuilder#animationEpoch}). */
    public @Nullable java.util.function.ToLongFunction<T> animationEpoch() { return animationEpoch; }

    public String id() { return id; }
    public List<PartDef<T>> parts() { return parts; }
    /** Whether there are animated (fading out by modelUpdateDistance) parts. */
    public boolean hasAnimatedParts() { return hasAnimatedParts; }
    /** Dynamic parts with an animator (doors/seal) - fast-path dirty-skip is forbidden, see field. */
    public boolean hasDynamicAnimators() { return hasDynamicAnimators; }
    /** Whether the spec has dynamic parts (per-BE geometry quads, e.g. fluid levels, electrodes). */
    public boolean hasDynamicParts() { return hasDynamicParts; }

    /**
     * Whether the spec has parts the FULL fast assert can never confirm:
     * dynamic parts with an animator (their draw lives in the parts loop) or
     * tint-bearing parts (never roster-assertable - the tint needs a per-frame
     * re-resolve). For such specs {@code tryFastAssertRender} goes straight to
     * the partial path instead of walking the part list per machine per frame.
     */
    public boolean hasNonAssertableParts() { return hasNonAssertableParts; }
    public boolean hasParametricParts() { return hasParametricParts; }

    /**
     * Returns true if this machine is intrinsically 100% static:
     * no animated parts, no dynamic animators, no parametric rigs, no non-assertable parts,
     * no dynamic quads, no immediate hooks, and no animation epoch.
     */
    public boolean isPurelyStatic() {
        return !hasAnimatedParts && !hasDynamicAnimators && !hasNonAssertableParts
                && !hasParametricParts && !hasDynamicParts && hooks.isEmpty() && animationEpoch == null;
    }

    public List<MachineRenderHook<T>> hooks() { return hooks; }
    Function<T, BakedModel> modelResolver() { return modelResolver; }
    public Function<T, Direction> facingResolver() { return facingResolver; }
    int viewDistance() { return viewDistance; }

    private final Map<BakedModel, BakedModel[]> modelPartArrays = new java.util.concurrent.ConcurrentHashMap<>();

    /** Pre-resolves part models indexed by partIdx to eliminate HashMap.get in hot rendering loops. */
    @Nullable BakedModel[] getPartModels(@Nullable BakedModel multipartModel) {
        if (multipartModel == null) return null;
        return modelPartArrays.computeIfAbsent(multipartModel, m -> {
            BakedModel[] arr = new BakedModel[parts.size()];
            for (int i = 0; i < parts.size(); i++) {
                arr[i] = partModel(parts.get(i), m);
            }
            return arr;
        });
    }

    /** Quads/model of the part for this BE (or null if the part is absent from the model). */
    @Nullable BakedModel partModel(PartDef<T> part, BakedModel multipartModel) {
        if (part.dynamic()) return null;
        multipartModel = com.hbm_m.client.render.AbstractPartBasedRenderer.unwrapFabricForwardingModels(multipartModel);
        return (multipartModel instanceof com.hbm_m.client.model.AbstractMultipartBakedModel mp)
                ? mp.getPart(part.modelPartName()) : null;
    }

    @Nullable List<net.minecraft.client.renderer.block.model.BakedQuad> dynamicQuads(PartDef<T> part, T be) {
        if (!part.dynamic()) return null;
        try {
            return part.dynamicQuads().resolve(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] dynamic part '{}' resolver failed", id, part.name(), t);
            return null;
        }
    }

    @Nullable String dynamicCacheKeyValue(PartDef<T> part, T be) {
        if (!part.dynamic() || part.dynamicCacheKey() == null) return null;
        try {
            return part.dynamicCacheKey().apply(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] dynamic part '{}' cacheKey failed", id, part.name(), t);
            return null;
        }
    }

    /**
     * uvRect of the part for this BE ({u0,v0,du,dv} remap of sprite-local VBO ->
     * atlas) or null. Called every frame per part - the resolver must be cheap
     * (cached sprite).
     */
    @Nullable float[] resolveUvRect(PartDef<T> part, T be) {
        if (part.uvRectResolver() == null) return null;
        try {
            return part.uvRectResolver().apply(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] part '{}' uvRect failed", id, part.name(), t);
            return null;
        }
    }

    /**
     * RGBA tint of the part for this BE ({r,g,b,a} per-instance color multiplier)
     * or null (white RGB = passthrough). Called every frame per part (like
     * {@link #resolveUvRect}) - the resolver must be cheap and CONVERGE to stable
     * values (the skip-write of the instance record compares floats exactly). RGB
     * may be &gt; 1 - overbright glow; alpha = emission strength (heat 0..1), not
     * transparency.
     */
    public @Nullable float[] resolveTint(PartDef<T> part, T be) {
        if (part.tintOverride() == null) return null;
        try {
            return part.tintOverride().apply(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] part '{}' tint failed", id, part.name(), t);
            return null;
        }
    }

    /**
     * Per-part clip-плоскости для этого BE ({@code float[][]} из {A,B,C,D} в конвенции
     * «+D», локальные координаты геометрии) или null (обычный тир). Вызывается каждый
     * кадр для партов с резолвером — резолвер должен быть дешёвым и переиспользовать
     * скратч-массивы.
     */
    @Nullable float[][] resolveClipPlanes(PartDef<T> part, T be, float partialTick) {
        if (part.clipPlanesResolver() == null) return null;
        try {
            return part.clipPlanesResolver().apply(be, partialTick);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] part '{}' clipPlanes failed", id, part.name(), t);
            return null;
        }
    }

    String cacheKey(PartDef<T> part, @Nullable String dynamicKey) {
        return part.dynamic() ? part.staticCacheKey() + "/" + dynamicKey : part.staticCacheKey();
    }

    /**
     * Existing GPU holder of the part WITHOUT creating/building (fast-path
     * dirty-skip). null - the renderer does not exist yet (cache was wiped): the
     * caller must fall back to the full build.
     */
    @Nullable MachinePartRenderer findExistingRenderer(PartDef<T> part, @Nullable String dynamicKey) {
        return findExistingRenderer(-1, part, dynamicKey);
    }

    @Nullable MachinePartRenderer findExistingRenderer(int partIdx, PartDef<T> part, @Nullable String dynamicKey) {
        MachinePartRenderer found;
        if (!part.dynamic()) {
            if (partIdx >= 0 && partIdx < staticRenderers.length) {
                found = staticRenderers[partIdx];
                if (found == null) {
                    found = partRenderers.get(part.staticCacheKey());
                    if (found != null) {
                        staticRenderers[partIdx] = found;
                        staticRendererMemo.put(part, found);
                    }
                }
            } else {
                found = staticRendererMemo.get(part);
                if (found == null) {
                    found = partRenderers.get(part.staticCacheKey());
                    if (found != null) {
                        staticRendererMemo.put(part, found);
                    }
                }
            }
        } else {
            ConcurrentHashMap<String, MachinePartRenderer> byDynKey = dynPartRenderers.get(part.name());
            found = byDynKey == null ? null : byDynKey.get(dynMapKey(dynamicKey));
        }
        if (found != null) {
            found.markUsed(com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame());
        }
        return found;
    }

    /** See the identity memo in {@link #findExistingRenderer}. */
    private final Map<PartDef<T>, MachinePartRenderer> staticRendererMemo = new java.util.IdentityHashMap<>();

    /**
     * Registers the created holder in both maps. Called strictly after a successful
     * putIfAbsent in {@link #partRenderers} - the entries are consistent.
     */
    private void registerRenderer(PartDef<T> part, @Nullable String dynamicKey, MachinePartRenderer renderer) {
        if (part.dynamic()) {
            dynMap(part).put(dynMapKey(dynamicKey), renderer);
        }
    }

    /** GPU holder of the part (lazy, on the render thread). */
    MachinePartRenderer partRenderer(PartDef<T> part, @Nullable BakedModel partModel,
                                     @Nullable List<net.minecraft.client.renderer.block.model.BakedQuad> dynQuads,
                                     @Nullable String dynamicKey) {
        MachinePartRenderer existing = findExistingRenderer(part, dynamicKey);
        if (existing != null) {
            existing.ensureBuilt(partModel, dynQuads);
            return existing;
        }
        String key = cacheKey(part, dynamicKey);
        MachinePartRenderer created = new MachinePartRenderer(key, part.name(), part.dynamic());
        MachinePartRenderer raced = partRenderers.putIfAbsent(key, created);
        if (raced != null) {
            registerRenderer(part, dynamicKey, raced);
            raced.ensureBuilt(partModel, dynQuads);
            return raced;
        }
        registerRenderer(part, dynamicKey, created);
        created.ensureBuilt(partModel, dynQuads);
        return created;
    }

    /**
     * Lazy variant of {@link #partRenderer}: the quad resolver ({@code dynamicQuads})
     * is called ONLY when the renderer has not been built yet.
     */
    MachinePartRenderer partRendererLazy(PartDef<T> part, @Nullable BakedModel partModel,
                                         T be, @Nullable String dynamicKey) {
        return partRendererLazy(-1, part, partModel, be, dynamicKey);
    }

    MachinePartRenderer partRendererLazy(int partIdx, PartDef<T> part, @Nullable BakedModel partModel,
                                         T be, @Nullable String dynamicKey) {
        MachinePartRenderer existing = findExistingRenderer(partIdx, part, dynamicKey);
        if (existing != null) {
            if (!existing.isAttempted()) {
                existing.ensureBuilt(partModel, dynamicQuads(part, be));
            }
            return existing;
        }
        String key = cacheKey(part, dynamicKey);
        MachinePartRenderer created = new MachinePartRenderer(key, part.name(), part.dynamic());
        MachinePartRenderer raced = partRenderers.putIfAbsent(key, created);
        if (raced != null) {
            registerRenderer(part, dynamicKey, raced);
            if (!part.dynamic() && partIdx >= 0 && partIdx < staticRenderers.length) {
                staticRenderers[partIdx] = raced;
                staticRendererMemo.put(part, raced);
            }
            if (!raced.isAttempted()) {
                raced.ensureBuilt(partModel, dynamicQuads(part, be));
            }
            return raced;
        }
        registerRenderer(part, dynamicKey, created);
        if (!part.dynamic() && partIdx >= 0 && partIdx < staticRenderers.length) {
            staticRenderers[partIdx] = created;
            staticRendererMemo.put(part, created);
        }
        created.ensureBuilt(partModel, dynamicQuads(part, be));
        return created;
    }

    /** LRU sweep cadence and idle bound for the dynKey holders (audit 2026-10-05 §3.5). */
    private static final int DYN_RENDERER_SWEEP_EVERY = 128;
    private static final long DYN_RENDERER_IDLE_LIMIT_FRAMES = 600;
    private int flushCountForSweep = 0;

    /**
     * Amortized LRU sweep of the per-dynKey holders (audit 2026-10-05 §3.5):
     * only dynamic keys grow unbounded (a farm of cycling tanks/casters/
     * crucibles converges toward thousands of permanent slots, each pinning up
     * to 4 GL objects plus native VBO staging), while a non-dynamic part holds
     * exactly one bounded holder. Every {@link #DYN_RENDERER_SWEEP_EVERY}th
     * flush, drop the holders no machine has looked up for
     * {@link #DYN_RENDERER_IDLE_LIMIT_FRAMES} frames - far above any pulsing
     * machine's refill window, so a live holder is never evicted (every
     * rendered machine stamps its holders each frame via
     * {@link #findExistingRenderer}), while despawned states release their
     * VRAM without waiting for reload/disconnect. Mirrors the
     * MeshRenderCache eviction precedent, keyed on idle time instead of a
     * size cap.
     */
    private void sweepStaleDynRenderers() {
        if (++flushCountForSweep < DYN_RENDERER_SWEEP_EVERY) {
            return;
        }
        flushCountForSweep = 0;
        long nowFrame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        for (ConcurrentHashMap<String, MachinePartRenderer> byDynKey : dynPartRenderers.values()) {
            for (Map.Entry<String, MachinePartRenderer> e : byDynKey.entrySet()) {
                MachinePartRenderer holder = e.getValue();
                if (nowFrame - holder.lastUseFrame() <= DYN_RENDERER_IDLE_LIMIT_FRAMES) {
                    continue;
                }
                // Frees the instanced renderer (GL deletes ride recordRenderCall,
                // exactly like the reload chain); 'single' stays owned by MeshRenderCache.
                holder.clear();
                byDynKey.remove(e.getKey());
                partRenderers.remove(holder.key());
                // Cleans the SingleMeshVboRenderer + its unconsumed native staging
                // and clears FAILED_RENDERER_KEYS so a future rebuild can retry.
                com.hbm_m.client.render.MeshRenderCache.removeRenderer(holder.key());
            }
        }
    }

    void flush(Matrix4f projection) {
        sweepStaleDynRenderers();
        for (MachinePartRenderer r : partRenderers.values()) {
            if (r.instanced() != null && r.instanced().getBlockEntityType() == null) {
                r.instanced().setOwnerSpec(this);
            }
            r.flush(projection);
        }
    }

    /** Reusable sort buffer for flushFading (rendering is single-threaded). */
    private final List<MachinePartRenderer> fadingSortBuf = new ArrayList<>();

    /** Phase 2 (after MDI): fading instances of the direct paths - see InstancedRenderFrame.
     *  Renderer windows are sorted by the farthest fading instance (back-to-front
     *  globally): fading runs with depth-write, an unsorted window order would
     *  depth-reject farther machines behind nearer ones. */
    void flushFading(Matrix4f projection) {
        if (partRenderers.isEmpty()) {
            return;
        }
        List<MachinePartRenderer> fading = fadingSortBuf;
        fading.clear();
        for (MachinePartRenderer r : partRenderers.values()) {
            if (r.fadingSortKeyDistSq() >= 0f) {
                fading.add(r);
            }
        }
        fading.sort((a, b) -> Float.compare(b.fadingSortKeyDistSq(), a.fadingSortKeyDistSq()));
        for (MachinePartRenderer r : fading) {
            r.flushFading(projection);
        }
    }

    /** Invalidates the GPU caches of this spec (reload/disconnect) - called from RenderCacheManager. */
    void clear() {
        for (MachinePartRenderer r : partRenderers.values()) {
            r.clear();
        }
        partRenderers.clear();
        dynPartRenderers.clear();
        modelPartArrays.clear();
        staticRendererMemo.clear();
        java.util.Arrays.fill(staticRenderers, null);
    }

    public void freeMachineSlots(long posKey) {
        // partRenderers contains every renderer, static and dynamic alike: both
        // creation sites putIfAbsent into partRenderers before registering in
        // dynPartRenderers, so a second pass over dynPartRenderers would be a
        // pure duplicate freeSlot sweep.
        for (MachinePartRenderer r : partRenderers.values()) {
            if (r != null && r.instanced() != null) {
                r.instanced().freeSlot(posKey);
            }
        }
    }

    public void freeDynamicPartSlots(PartDef<T> part, long posKey, @Nullable String exceptDynamicKey) {
        ConcurrentHashMap<String, MachinePartRenderer> dynMap = dynPartRenderers.get(part.name());
        if (dynMap != null && dynMap.size() > 1) {
            String keepKey = dynMapKey(exceptDynamicKey);
            for (Map.Entry<String, MachinePartRenderer> entry : dynMap.entrySet()) {
                if (!entry.getKey().equals(keepKey)) {
                    MachinePartRenderer r = entry.getValue();
                    if (r != null && r.instanced() != null && r.instanced().hasSlot(posKey)) {
                        r.instanced().freeSlot(posKey);
                    }
                }
            }
        }
    }

    /**
     * Ensures all part renderers for this machine spec are instantiated and initialized,
     * associating their owner spec so retained instances can be managed.
     */
    @SuppressWarnings("unchecked")
    public void ensureStaticPartRenderers(T be) {
        if (be == null) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        BakedModel model = null;
        try {
            if (modelResolver != null) {
                model = modelResolver.apply(be);
            }
            if (model == null && mc.getBlockRenderer() != null) {
                model = mc.getBlockRenderer().getBlockModel(be.getBlockState());
            }
        } catch (Throwable ignored) {}
        BakedModel[] resolvedPartModels = (model != null) ? getPartModels(model) : null;
        List<PartDef<T>> specParts = parts();
        for (int i = 0; i < specParts.size(); i++) {
            PartDef<T> part = specParts.get(i);
            BakedModel partModel = (resolvedPartModels != null) ? resolvedPartModels[i] : partModel(part, model);
            if (partModel == null && !part.dynamic()) continue;
            String dynKey = part.dynamic() ? dynamicCacheKeyValue(part, be) : null;
            MachinePartRenderer r = partRendererLazy(i, part, partModel, be, dynKey);
            if (r != null && r.instanced() != null) {
                com.hbm_m.client.render.InstancedStaticPartRenderer inst = r.instanced();
                if (inst.getBlockEntityType() == null) {
                    inst.setOwnerSpec(this);
                }
            }
        }
    }

    /**
     * Synchronizes a baked 32-float instance record for a specific part into its InstancedStaticPartRenderer.
     */
    @SuppressWarnings("unchecked")
    public void syncInstanceRecord(BlockEntity be, long posKey, int partIdx, float[] record32) {
        if (be == null || record32 == null) return;
        List<PartDef<T>> specParts = parts();
        if (specParts == null || partIdx < 0 || partIdx >= specParts.size()) return;
        PartDef<T> part = specParts.get(partIdx);
        String dynKey = part.dynamic() ? dynamicCacheKeyValue(part, (T) be) : null;
        MachinePartRenderer r = findExistingRenderer(partIdx, part, dynKey);
        if (r != null && r.instanced() != null) {
            r.instanced().putInstanceRecord(posKey, record32);
        }
    }

    /**
     * Synchronizes a sub-range (slice) of instance records across all parts of this machine into their InstancedStaticPartRenderers.
     */
    @SuppressWarnings("unchecked")
    public void syncInstanceSlice(BlockEntity be, long posKey, int floatOffset, float[] data) {
        if (be == null || data == null) return;
        List<PartDef<T>> specParts = parts();
        if (specParts == null) return;
        for (int i = 0; i < specParts.size(); i++) {
            syncPartInstanceSlice(be, posKey, i, floatOffset, data);
        }
    }

    /**
     * Synchronizes a sub-range (slice) of an instance record for ONE part index into its
     * InstancedStaticPartRenderer. Per-part entry point for fine-grained updates
     * (SparseUpdateQueue TRANSFORM slices): the old all-parts loop re-wrote every
     * part's transform slot with the LAST part's data.
     */
    @SuppressWarnings("unchecked")
    public void syncPartInstanceSlice(BlockEntity be, long posKey, int partIdx, int floatOffset, float[] data) {
        if (be == null || data == null) return;
        List<PartDef<T>> specParts = parts();
        if (specParts == null || partIdx < 0 || partIdx >= specParts.size()) return;
        PartDef<T> part = specParts.get(partIdx);
        String dynKey = part.dynamic() ? dynamicCacheKeyValue(part, (T) be) : null;
        MachinePartRenderer r = findExistingRenderer(partIdx, part, dynKey);
        if (r != null && r.instanced() != null) {
            r.instanced().putInstanceSlice(posKey, floatOffset, data);
        }
    }

    List<MachinePartRenderer> partRenderersSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(partRenderers.values()));
    }
}
