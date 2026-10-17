package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Builder for a machine spec. Created via {@link MachineRenderers#machine};
 * finished by {@link #register()}, which registers the BER and the spec in the
 * registry.
 *
 * @param <T> the machine's BlockEntity class
 */
public final class MachineSpecBuilder<T extends BlockEntity> {

    private final String id;
    private final Class<T> beClass;
    private final net.minecraft.world.level.block.entity.BlockEntityType<T> type;

    private Function<T, BakedModel> modelResolver = MachineRenderers::blockstateModel;
    private Function<T, Direction> facingResolver = MachineRenderers::defaultFacing;
    @Nullable
    private BlockTransform<T> blockTransform; // null = default (T(0.5,0,0.5)+R(90)+R(legacy facing))
    private final List<MachineSpec.PartDef<T>> parts = new ArrayList<>();
    private final List<MachineRenderHook<T>> hooks = new ArrayList<>();
    private final Map<String, Function<T, Integer>> lightOverrides = new HashMap<>();
    private final Map<String, Function<T, float[]>> tintOverrides = new HashMap<>();
    private final Map<String, float[]> tintFalloffs = new HashMap<>();
    private final Map<String, java.util.function.BiFunction<T, Float, float[][]>> clipPlanesResolvers = new HashMap<>();
    private int viewDistance = -1;
    @Nullable
    private java.util.List<String> itemParts;
    @Nullable
    private java.util.List<String> itemExcept;
    @Nullable
    private java.util.List<net.minecraft.client.renderer.RenderType> chunkRenderTypes;
    @Nullable
    private ToLongFunction<T> animationEpoch;
    private final List<ToDoubleBiFunction<T, Float>> declarativeAnimFns = new ArrayList<>();
    /**
     * Per-part samplers of joint-chain node fns (jointChainPart): hashed into
     * the auto-deduced epoch alongside {@link #declarativeAnimFns}.
     */
    private final List<MachineSpec.ParametricParams<T>> chainEpochSamplers = new ArrayList<>();
    /**
     * Segment-start-tick functions of TRACKED rigs (spinningPart /
     * trackedChainPart, ANIM TRACK FORMAT v2): the epoch contract makes one of
     * these the machine epoch (directly, or hashed with the other sources) so
     * the epoch-keyed AnimParams word cache in MachineBer hits while a segment
     * runs - the words are a pure function of (epochValue, rig constants).
     */
    private final List<ToLongFunction<T>> trackedEpochFns = new ArrayList<>();
    /**
     * Scratch for the auto-derived chain fallback animator and the epoch
     * sampler (rendering is single-threaded - see the MachineBer scratch
     * conventions).
     */
    private static final float[] CHAIN_FALLBACK_SCRATCH = new float[MachineSpec.CHAIN_PARAM_BUDGET];
    private static final float[] EPOCH_SAMPLER_SCRATCH = new float[MachineSpec.CHAIN_PARAM_BUDGET];

    private void recordDeclarativeAnim(ToDoubleBiFunction<T, Float> fn) {
        declarativeAnimFns.add(fn);
    }

    private void recordDeclarativeChain(String name, MachineSpec.ParametricChain<T> chain) {
        // A tracked node must not take the streamed path: this path never
        // writes the segment header (the record's pr[0..2] would carry stale
        // scratch to the GPU) and the epoch sampler below would sample the
        // track at the current animTime (churning the deduced epoch per
        // frame) - fail fast at registration, never at render time.
        for (MachineSpec.JointNode<T> node : chain.nodes()) {
            if (node.track() != null) {
                throw new IllegalStateException("[MachineRenderers:" + id + "] jointChainPart('" + name
                        + "'): a chain node carries a Track - declare the part through trackedChainPart/"
                        + "spinningPart (ANIM TRACK FORMAT v2 wires the segment header + epoch there)");
            }
        }
        // gameTime is fixed at 0 for the epoch sample: the epoch signature
        // cannot see it (same contract as the declarative shortcuts). The
        // scratch is zeroed HERE in full: evalParams holds a caller-zero
        // contract, and the epoch hash below reads all CHAIN_PARAM_BUDGET
        // slots - stale scratch would churn the deduced epoch.
        chainEpochSamplers.add((be, pt, gt, out4) -> {
            java.util.Arrays.fill(out4, 0f);
            return chain.evalParams(be, pt, 0L, out4);
        });
    }

    MachineSpecBuilder(String id, Class<T> beClass, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        this.id = id;
        this.beClass = beClass;
        this.type = type;
    }

    /** Model per BE: door skins, hot/cold of the arc furnace, etc. Default - the blockstate model. */
    public MachineSpecBuilder<T> model(Function<T, BakedModel> resolver) {
        this.modelResolver = resolver;
        return this;
    }

    /** Machine facing. Default - HORIZONTAL_FACING from the blockstate, otherwise NORTH. */
    public MachineSpecBuilder<T> facing(Function<T, Direction> resolver) {
        this.facingResolver = resolver;
        return this;
    }

    /**
     * Custom block transform (rarely needed; default - translate(0.5,0,0.5)
     * + rotate(90) + legacy facing rotation). Use {@code animator.translate/rotate} -
     * it delegates to the PoseStack. Everything is applied BEFORE the part animators.
     */
    public MachineSpecBuilder<T> blockTransform(BlockTransform<T> fn) {
        this.blockTransform = fn;
        return this;
    }

    /** Custom block transform of the spec. */
    @FunctionalInterface
    public interface BlockTransform<T extends BlockEntity> {
        void apply(T blockEntity, com.hbm_m.client.render.LegacyAnimator animator);
    }

    /** Static part (drawn in the block pose, no animation). */
    public MachineSpecBuilder<T> part(String name) {
        parts.add(new MachineSpec.PartDef<>(name, name, null, null, null, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, clipPlanesResolvers.get(name)));
        return this;
    }

    /** Animated part: {@link PartAnimator} sets the transform relative to the block. */
    public MachineSpecBuilder<T> part(String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, null, null, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Animated part referencing a foreign model: several logical parts on top of
     * one OBJ part (e.g. 4 gears from the "Cog" part).
     */
    public MachineSpecBuilder<T> part(String modelPartName, String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, modelPartName, animator, null, null, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Forced part light: the function returns packedLight (>=0) or -1 for world
     * light. Port of the original's fullbright parts (InnerBurning furnaces,
     * lightmap 240/240). Call BEFORE declaring part()/dynamicPart() with that name.
     */
    public MachineSpecBuilder<T> lightOverride(String partName, Function<T, Integer> fn) {
        lightOverrides.put(partName, fn);
        return this;
    }

    /**
     * Per-instance RGBA tint of a part ({r,g,b,a}; null/white RGB = passthrough) -
     * glowing-hot nozzles, lit windows, etc. Written into the instance record
     * (attrib 13 InstColor), changes EVERY FRAME without VBO rebuild; RGB may be
     * &gt; 1 - overbright glow. <b>Alpha = emission strength (heat 0..1), NOT
     * transparency</b>: shaders push the lightmap toward fullbright by
     * alpha*falloff ({@link #tintFalloff}), so the glow follows the tint
     * spatially. Call BEFORE declaring part()/dynamicPart().
     * <p>
     * Update semantics - same as lightOverride: animated parts get a fresh tint
     * every frame; static ones - only on a full rebuild (dirty/TTL) - for a
     * smoothly changing tint on a static part the machine must call
     * {@code markRenderDirty()} when the value changes.
     */
    public MachineSpecBuilder<T> tintOverride(String partName, Function<T, float[]> fn) {
        tintOverrides.put(partName, fn);
        return this;
    }

    /**
     * Spatial tint falloff of a part: smooth decay along a model axis (0=x, 1=y,
     * 2=z) from {@code fullCoord} (full tint, the "source" - e.g. the nozzle cut)
     * to {@code zeroCoord} (zero tint). Evaluated in the vertex shader on the
     * MODEL coordinates of the vertex, so one setting covers the whole part/model
     * with no VBO rebuild. Call BEFORE declaring part()/dynamicPart() with that name.
     */
    public MachineSpecBuilder<T> tintFalloff(String partName, float axis, float fullCoord, float zeroCoord) {
        tintFalloffs.put(partName, new float[] {axis, fullCoord, zeroCoord, 0.0F});
        return this;
    }

    /**
     * Static part with a fixed transform "animator" (legacy bake offsets:
     * T(-0.5,0,-0.5), yaw groups). NOT gated by modelUpdateDistance - lives up to
     * the static cutoff, like regular statics.
     */
    public MachineSpecBuilder<T> staticPart(String name, PartAnimator<T> transform) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, null, null, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Conditional static part gated by a predicate on the BlockEntity (e.g. blockstate properties like FRAME).
     * Rendered via instanced static VBO when visible, or hidden via GPU fade gating (FADE_MODE_INVISIBLE = -1.0f)
     * when invisible. Never falls back to dynamic quad compiling.
     */
    public MachineSpecBuilder<T> conditionalPart(String name, java.util.function.Predicate<T> visibility) {
        parts.add(new MachineSpec.PartDef<>(name, name, null, null, null, false, id + "/" + name,
                lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, visibility, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Conditional static part with transform animator (legacy bake offsets) and visibility predicate.
     */
    public MachineSpecBuilder<T> conditionalPart(String name, PartAnimator<T> transform, java.util.function.Predicate<T> visibility) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, null, null, false, id + "/" + name,
                lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, visibility, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Conditional static part with distinct model part name, transform animator, and visibility predicate.
     */
    public MachineSpecBuilder<T> conditionalPart(String name, String modelPartName, PartAnimator<T> transform, java.util.function.Predicate<T> visibility) {
        parts.add(new MachineSpec.PartDef<>(name, modelPartName, transform, null, null, false, id + "/" + name,
                lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, visibility, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Dynamic part with per-BE geometry (fluid tank walls by fluid, DAE nodes).
     * The VBO is cached keyed on {@code cacheKeyFn}; the returned quad list may be empty.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, null, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, clipPlanesResolvers.get(name)));
        return this;
    }

    /** Dynamic part with per-BE geometry AND animation (e.g. the dish of the large/small radar). */
    public MachineSpecBuilder<T> dynamicPart(String name, PartAnimator<T> animator,
                                             QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, quads, cacheKeyFn, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Dynamic part with per-BE geometry and a fixed transform "animator" (legacy
     * offsets). Content is static (changes with BE state, not time) - NOT gated by
     * modelUpdateDistance.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                             PartAnimator<T> transform) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Dynamic part with a uvRect resolver: the part's VBO stores sprite-local
     * [0..1] UVs, and the resolver supplies a per-BE {u0,v0,du,dv} remap into the
     * atlas (texture-variant door parts: one VBO for geometry, the skin goes into
     * the per-instance uvRect).
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                             PartAnimator<T> transform, Function<T, float[]> uvRectFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), uvRectFn, tintOverrides.get(name), tintFalloffs.get(name), null, null, clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * Per-part CPU-клип плоскостями (обрезка «карманом» двери): резолвер
     * {@code (be, partialTick) -> float[][]} из {A,B,C,D} в конвенции DoorDecl
     * «+D» ({@code A*x+B*y+C*z+D >= 0} — внутри), в ЛОКАЛЬНЫХ координатах геометрии
     * парта, или null (обычный тир). Часть с непустыми плоскостями деградирует в
     * клипуемый immediate-тир (Sutherland-Hodgman по кэшированным квадам,
     * DoorPartClipper) — меши не пересобираются. Call BEFORE declaring
     * part()/dynamicPart() with that name.
     */
    public MachineSpecBuilder<T> clipPlanes(String partName,
                                            java.util.function.BiFunction<T, Float, float[][]> fn) {
        clipPlanesResolvers.put(partName, fn);
        return this;
    }

    /** Extra immediate pass: fluids, NFPA diamonds, item icons. */
    public MachineSpecBuilder<T> hook(MachineRenderHook<T> hook) {
        hooks.add(hook);
        return this;
    }

    /** BER draw distance in blocks. Default - modelStaticRenderDistance. */
    public MachineSpecBuilder<T> viewDistance(int blocks) {
        this.viewDistance = blocks;
        return this;
    }

    /**
     * Explicit list of item-render parts of the multipart model. By default the
     * item shows the spec's NON-dynamic parts (see {@link MachineSpec#deriveItemParts});
     * this method is needed when a dynamic part's base geometry is still wanted in
     * the item (fluid storage tank, foundry plate).
     * World rendering of factory machines is always BER (the chunk mesh is empty) -
     * this follows from the .part() declarations themselves and is merged into the
     * model automatically at {@link #register()} ->
     * {@link MachineRenderRegistry#bindBakedModels}.
     */
    public MachineSpecBuilder<T> itemParts(String... parts) {
        this.itemParts = java.util.List.of(parts);
        return this;
    }

    /** Remove the listed parts from the default item set (the spec's non-dynamic parts). */
    public MachineSpecBuilder<T> itemExcept(String... parts) {
        this.itemExcept = java.util.List.of(parts);
        return this;
    }

    /** Render types of the chunk pass (forge getRenderTypes) when the model is drawn from the chunk mesh. */
    public MachineSpecBuilder<T> chunkRenderTypes(net.minecraft.client.renderer.RenderType... types) {
        this.chunkRenderTypes = java.util.List.of(types);
        return this;
    }

    /**
     * Machine animation-epoch - the key to the full roster-assert of "frozen"
     * animated machines (the fast-path dirty-skip extends to animated parts as
     * long as the animation does not change). The function must mix the <b>prev
     * and curr</b> values of ALL animator inputs (arm angles, ring, phases):
     * <ul>
     *   <li>epoch equality between frames means the pose is identical at ANY
     *       partialTick (prev and curr values match - the lerp is degenerate), and
     *       MachineBer confirms the animated parts via roster-assert without
     *       running the animators;</li>
     *   <li>any movement changes at least one (prev, curr) pair, so the epoch
     *       differs - the machine automatically returns to the full path.</li>
     * </ul>
     * The function's cost is a few field reads: called once per machine per frame.
     * Example - {@code MachineAdvancedAssemblerRenderer}.
     */
    public MachineSpecBuilder<T> animationEpoch(java.util.function.ToLongFunction<T> epochFn) {
        this.animationEpoch = epochFn;
        return this;
    }

    /**
     * Parametric GPU animation of a part: ONE joint (rotation around an axis
     * through a pivot, or translation along an axis), with per-frame values
     * supplied by {@code paramsFn} via attrib 15 AnimParams (4 floats). The vertex
     * shader (block_lit_instanced.vsh) moves the geometry - the CPU neither
     * rebuilds matrices nor uploads pos/quat of the record every frame: a frozen
     * joint gives skip-write and ZERO upload.
     * <p>
     * {@code legacyAnimator} is REQUIRED - it stays for the degradation paths
     * (single-VBO/immediate, Iris/Tier1) and shadows: parametrics is active only
     * on the vanilla MDI path. The axis is normalized; the pivot is in the local
     * coordinates of the part geometry (for in-place rotation - the part's center).
     *
     * @param kind     {@link MachineSpec#KIND_ROTATE} or {@link MachineSpec#KIND_TRANSLATE}
     * @param paramsFn per-frame parameters (out[0] = angle in degrees or distance in blocks;
     *                 return false = do not draw this frame)
     */
    // ==================== DECLARATIVE GPU ANIMATION DSL ====================

    /**
     * First-class declarative shortcut for rotation around an arbitrary axis and pivot.
     * Automatically binds both the GPU joint (AnimParams) and legacy CPU fallback animator.
     */
    public MachineSpecBuilder<T> rotatingPart(String partName, Vector3f axis, Vector3f pivot,
                                              ToDoubleBiFunction<T, Float> angleFn) {
        return rotatingPart(partName, axis.x(), axis.y(), axis.z(), pivot.x(), pivot.y(), pivot.z(), 0f, 0f, 0f, null, null, angleFn);
    }

    public MachineSpecBuilder<T> rotatingPart(String partName, Vector3f axis, Vector3f pivot, Vector3f baseOffset,
                                              ToDoubleBiFunction<T, Float> angleFn) {
        return rotatingPart(partName, axis.x(), axis.y(), axis.z(), pivot.x(), pivot.y(), pivot.z(),
                baseOffset.x(), baseOffset.y(), baseOffset.z(), null, null, angleFn);
    }

    public MachineSpecBuilder<T> rotatingPart(String partName, Vector3f axis, Vector3f pivot,
                                              QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                              ToDoubleBiFunction<T, Float> angleFn) {
        return rotatingPart(partName, axis.x(), axis.y(), axis.z(), pivot.x(), pivot.y(), pivot.z(), 0f, 0f, 0f, quads, cacheKeyFn, angleFn);
    }

    public MachineSpecBuilder<T> rotatingPart(String partName,
                                              float axisX, float axisY, float axisZ,
                                              float pivotX, float pivotY, float pivotZ,
                                              float offX, float offY, float offZ,
                                              ToDoubleBiFunction<T, Float> angleFn) {
        return rotatingPart(partName, axisX, axisY, axisZ, pivotX, pivotY, pivotZ, offX, offY, offZ, null, null, angleFn);
    }

    public MachineSpecBuilder<T> rotatingPart(String partName,
                                              float axisX, float axisY, float axisZ,
                                              float pivotX, float pivotY, float pivotZ,
                                              float offX, float offY, float offZ,
                                              @Nullable QuadResolver<T> quads,
                                              @Nullable Function<T, String> cacheKeyFn,
                                              ToDoubleBiFunction<T, Float> angleFn) {
        float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (len < 1.0e-6f) len = 1f;
        final float ax = axisX / len, ay = axisY / len, az = axisZ / len;

        recordDeclarativeAnim(angleFn);

        // out4[1]/[2] are zeroed: the record carries all three floats verbatim and
        // the skip-write compares them byte-for-byte (paramScratch holds stale values
        // from other joints otherwise).
        MachineSpec.ParametricParams<T> params = (be, pt, gt, out4) -> {
            out4[0] = (float) angleFn.applyAsDouble(be, pt);
            out4[1] = 0f;
            out4[2] = 0f;
            return true;
        };

        PartAnimator<T> legacy = (be, pt, gt, pose) -> {
            float ang = (float) angleFn.applyAsDouble(be, pt);
            if (offX != 0f || offY != 0f || offZ != 0f) {
                pose.translate(offX, offY, offZ);
            }
            pose.translate(pivotX, pivotY, pivotZ);
            pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(ang), ax, ay, az));
            pose.translate(-pivotX, -pivotY, -pivotZ);
            return true;
        };

        return parametricPart(partName, MachineSpec.KIND_ROTATE, ax, ay, az, pivotX, pivotY, pivotZ,
                offX, offY, offZ, quads, cacheKeyFn, params, legacy);
    }

    /**
     * Concise first-class shortcut for rotation around the Y axis through (pivotX, pivotZ).
     */
    public MachineSpecBuilder<T> rotateY(String partName, float pivotX, float pivotZ,
                                         ToDoubleBiFunction<T, Float> angleFn) {
        return rotatingPart(partName, 0f, 1f, 0f, pivotX, 0f, pivotZ, 0f, 0f, 0f, angleFn);
    }

    public MachineSpecBuilder<T> rotateY(String partName, float pivotX, float pivotZ,
                                         float offX, float offZ,
                                         ToDoubleBiFunction<T, Float> angleFn) {
        return rotatingPart(partName, 0f, 1f, 0f, pivotX, 0f, pivotZ, offX, 0f, offZ, angleFn);
    }

    public MachineSpecBuilder<T> rotateY(String partName, float pivotX, float pivotZ,
                                         QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                         ToDoubleBiFunction<T, Float> angleFn) {
        return rotatingPart(partName, 0f, 1f, 0f, pivotX, 0f, pivotZ, 0f, 0f, 0f, quads, cacheKeyFn, angleFn);
    }

    /**
     * First-class declarative shortcut for linear translation along an axis.
     * Automatically binds both the GPU joint (AnimParams) and legacy CPU fallback animator.
     */
    public MachineSpecBuilder<T> translatingPart(String partName, Vector3f axis,
                                                 ToDoubleBiFunction<T, Float> distanceFn) {
        return translatingPart(partName, axis.x(), axis.y(), axis.z(), 0f, 0f, 0f, null, null, distanceFn);
    }

    public MachineSpecBuilder<T> translatingPart(String partName, Vector3f axis, Vector3f baseOffset,
                                                 ToDoubleBiFunction<T, Float> distanceFn) {
        return translatingPart(partName, axis.x(), axis.y(), axis.z(),
                baseOffset.x(), baseOffset.y(), baseOffset.z(), null, null, distanceFn);
    }

    public MachineSpecBuilder<T> translatingPart(String partName, Vector3f axis,
                                                 QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                                 ToDoubleBiFunction<T, Float> distanceFn) {
        return translatingPart(partName, axis.x(), axis.y(), axis.z(), 0f, 0f, 0f, quads, cacheKeyFn, distanceFn);
    }

    public MachineSpecBuilder<T> translatingPart(String partName,
                                                 float axisX, float axisY, float axisZ,
                                                 float offX, float offY, float offZ,
                                                 ToDoubleBiFunction<T, Float> distanceFn) {
        return translatingPart(partName, axisX, axisY, axisZ, offX, offY, offZ, null, null, distanceFn);
    }

    public MachineSpecBuilder<T> translatingPart(String partName,
                                                 float axisX, float axisY, float axisZ,
                                                 float offX, float offY, float offZ,
                                                 @Nullable QuadResolver<T> quads,
                                                 @Nullable Function<T, String> cacheKeyFn,
                                                 ToDoubleBiFunction<T, Float> distanceFn) {
        float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (len < 1.0e-6f) len = 1f;
        final float ax = axisX / len, ay = axisY / len, az = axisZ / len;

        recordDeclarativeAnim(distanceFn);

        // KIND_TRANSLATE consumes the FULL AnimParams.xyz (multi-axis parts): the
        // record's xyz IS the translation vector, so the scalar distance is written
        // out pre-scaled by the axis (GPU parity with the legacy animator below).
        MachineSpec.ParametricParams<T> params = (be, pt, gt, out4) -> {
            float dist = (float) distanceFn.applyAsDouble(be, pt);
            out4[0] = ax * dist;
            out4[1] = ay * dist;
            out4[2] = az * dist;
            return true;
        };

        PartAnimator<T> legacy = (be, pt, gt, pose) -> {
            float dist = (float) distanceFn.applyAsDouble(be, pt);
            if (offX != 0f || offY != 0f || offZ != 0f) {
                pose.translate(offX, offY, offZ);
            }
            pose.translate(ax * dist, ay * dist, az * dist);
            return true;
        };

        return parametricPart(partName, MachineSpec.KIND_TRANSLATE, ax, ay, az, 0f, 0f, 0f,
                offX, offY, offZ, quads, cacheKeyFn, params, legacy);
    }

    /**
     * Parametric GPU animation of a part: ONE joint (rotation around an axis
     * through a pivot, or translation along an axis), with per-frame values
     * supplied by {@code paramsFn} via attrib 15 AnimParams (4 floats). The vertex
     * shader (block_lit_instanced.vsh) moves the geometry - the CPU neither
     * rebuilds matrices nor uploads pos/quat of the record every frame: a frozen
     * joint gives skip-write and ZERO upload.
     * <p>
     * {@code legacyAnimator} is REQUIRED - it stays for the degradation paths
     * (single-VBO/immediate, Iris/Tier1) and shadows: parametrics is active only
     * on the vanilla MDI path. The axis is normalized; the pivot is in the local
     * coordinates of the part geometry (for in-place rotation - the part's center).
     *
     * @param kind     {@link MachineSpec#KIND_ROTATE} or {@link MachineSpec#KIND_TRANSLATE}
     * @param paramsFn per-frame parameters (KIND_ROTATE: out[0] = angle in degrees,
     *                 out[1..2] = 0; KIND_TRANSLATE: xyz = the translation vector in
     *                 part-local blocks; return false = do not draw this frame)
     */
    public MachineSpecBuilder<T> parametricPart(String name, int kind,
                                                float axisX, float axisY, float axisZ,
                                                float pivotX, float pivotY, float pivotZ,
                                                MachineSpec.ParametricParams<T> paramsFn,
                                                PartAnimator<T> legacyAnimator) {
        return parametricPart(name, kind, axisX, axisY, axisZ, pivotX, pivotY, pivotZ,
                0f, 0f, 0f, paramsFn, legacyAnimator);
    }

    /**
     * {@link #parametricPart(String, int, float, float, float, float, float, float,
     * MachineSpec.ParametricParams, PartAnimator)} with a static base offset
     * {@code (offX, offY, offZ)} applied by MachineBer to the record pose before
     * enqueue. Use it when the part geometry lives in the legacy bake space (model
     * centered at (0.5, ., 0.5), the centering carried by the legacy animator): the
     * record must reproduce the animator's static transforms MINUS the joint itself,
     * otherwise the GPU-animated part is displaced by the missing offset. Pass the
     * same translation the legacy animator applies AFTER its joint operation
     * (e.g. the assembler ring: mulPose(R)·T(-0.5,0,-0.5) → offset (-0.5, 0, -0.5)).
     */
    public MachineSpecBuilder<T> parametricPart(String name, int kind,
                                                float axisX, float axisY, float axisZ,
                                                float pivotX, float pivotY, float pivotZ,
                                                float offX, float offY, float offZ,
                                                MachineSpec.ParametricParams<T> paramsFn,
                                                PartAnimator<T> legacyAnimator) {
        return parametricPart(name, kind, axisX, axisY, axisZ, pivotX, pivotY, pivotZ,
                offX, offY, offZ, null, null, paramsFn, legacyAnimator);
    }

    /**
     * {@link #parametricPart(String, int, float, float, float, float, float, float,
     * float, float, float, MachineSpec.ParametricParams, PartAnimator)} with a
     * static PRE-ROTATION {@code baseYawDegrees} - the rotation the legacy animator
     * applies BEFORE the joint (the legacy yaw group around the block center).
     * The joint operates on raw part vertices in the VSH, so static transforms that
     * precede it must go into the record pose (MachineBer applies this yaw right
     * before baseOffset), exactly like the post-joint tail translation goes into
     * baseOffset.
     */
    public MachineSpecBuilder<T> parametricPart(String name, int kind,
                                                float axisX, float axisY, float axisZ,
                                                float pivotX, float pivotY, float pivotZ,
                                                float offX, float offY, float offZ,
                                                float baseYawDegrees,
                                                MachineSpec.ParametricParams<T> paramsFn,
                                                PartAnimator<T> legacyAnimator) {
        return parametricPart(name, name, kind, axisX, axisY, axisZ, pivotX, pivotY, pivotZ,
                offX, offY, offZ, baseYawDegrees, null, null, paramsFn, legacyAnimator);
    }

    /**
     * {@link #parametricPart(String, int, float, float, float, float, float, float,
     * float, float, float, float, MachineSpec.ParametricParams, PartAnimator)} over a
     * FOREIGN model part: several logical parts on top of one OBJ part (the CPU
     * {@code part(modelPartName, name, animator)} pattern, e.g. 4 gears from one
     * "Cog" mesh). {@code name} is the unique logical key (cache/roster), {@code
     * modelPartName} the geometry source; each logical part carries its own joint
     * spec (pivot/baseOffset) and params.
     */
    public MachineSpecBuilder<T> parametricPart(String modelPartName, String name, int kind,
                                                float axisX, float axisY, float axisZ,
                                                float pivotX, float pivotY, float pivotZ,
                                                float offX, float offY, float offZ,
                                                float baseYawDegrees,
                                                MachineSpec.ParametricParams<T> paramsFn,
                                                PartAnimator<T> legacyAnimator) {
        return parametricPart(modelPartName, name, kind, axisX, axisY, axisZ, pivotX, pivotY, pivotZ,
                offX, offY, offZ, baseYawDegrees, null, null, paramsFn, legacyAnimator);
    }

    public MachineSpecBuilder<T> parametricPart(String name, int kind,
                                                float axisX, float axisY, float axisZ,
                                                float pivotX, float pivotY, float pivotZ,
                                                float offX, float offY, float offZ,
                                                @Nullable QuadResolver<T> quads,
                                                @Nullable Function<T, String> cacheKeyFn,
                                                MachineSpec.ParametricParams<T> paramsFn,
                                                PartAnimator<T> legacyAnimator) {
        return parametricPart(name, name, kind, axisX, axisY, axisZ, pivotX, pivotY, pivotZ,
                offX, offY, offZ, 0f, quads, cacheKeyFn, paramsFn, legacyAnimator);
    }

    /** Full form: model part + logical name + base yaw + optional dynamic quads. */
    public MachineSpecBuilder<T> parametricPart(String modelPartName, String name, int kind,
                                                float axisX, float axisY, float axisZ,
                                                float pivotX, float pivotY, float pivotZ,
                                                float offX, float offY, float offZ,
                                                float baseYawDegrees,
                                                @Nullable QuadResolver<T> quads,
                                                @Nullable Function<T, String> cacheKeyFn,
                                                MachineSpec.ParametricParams<T> paramsFn,
                                                PartAnimator<T> legacyAnimator) {
        parts.add(new MachineSpec.PartDef<>(name, modelPartName, legacyAnimator, quads, cacheKeyFn, true,
                id + "/" + name, lightOverrides.get(name), null,
                tintOverrides.get(name), tintFalloffs.get(name),
                new MachineSpec.ParametricAnim(kind, axisX, axisY, axisZ, pivotX, pivotY, pivotZ,
                        offX, offY, offZ, baseYawDegrees, null),
                paramsFn,
                null,
                clipPlanesResolvers.get(name)));
        return this;
    }

    // ==================== HIERARCHICAL JOINT CHAIN DSL ====================

    /**
     * Declarative GPU animation of a part by a rigid FK CHAIN of joints - the
     * generalization of {@link #parametricPart} from one joint to a hierarchy.
     * The chain is evaluated entirely in the vertex shader (attrib 15
     * AnimParams + the NucleusJointSpecs texture): the CPU never rebuilds the
     * record pose, a frozen machine costs zero upload (skip-write).
     *
     * <p><b>What a node is.</b> Each node is one rigid transform, declared in
     * the SAME order the equivalent CPU animator would write its PoseStack ops
     * (root first): {@link MachineSpec.JointNode#rotate} - rotation about a
     * fixed axis through a fixed pivot, costing 1 parameter (the angle in
     * degrees); {@link MachineSpec.JointNode#translate} - translation along a
     * fixed axis, costing 1 parameter (the distance in blocks);
     * {@link MachineSpec.JointNode#translateVector} - full vector translation,
     * costing 3 parameters. Every node carries a {@link MachineSpec.NodeParams} fn
     * that writes ONLY its own scalars into the out array at the given offset
     * (the engine zero-fills the rest, so idle convergence and the record
     * skip-write work automatically).
     *
     * <p><b>Parameter budget (fail fast).</b> The record carries ONE vec4
     * (AnimParams, attrib 15) per part and no vertex attribute slots are free:
     * the 4 floats pack 8 BINARY16 words (2 per float, decoded via
     * {@code halfDecode} (GLSL bit math); rotate angles are wrapped to (-180, 180], so ulp
     * &lt;= 0.125 deg; block distances keep ~1 mm), and the sum of node costs
     * must be &lt;= {@link MachineSpec#CHAIN_PARAM_BUDGET} (8 words) with at
     * most {@link MachineSpec#MAX_CHAIN_NODES} (8) nodes - e.g. a turntable
     * mount + 4 rotations + 3 axis translates, or 2 vector translations + 2
     * rotations. A declaration exceeding
     * the budget throws {@link IllegalStateException} HERE, at registration,
     * naming the part - it never fails silently at render time. A part mounted
     * on an ANIMATED mount (ring/turntable) declares the mount
     * via {@link #turntableChainPart} - it joins the chain as node 0 and costs
     * 1 word.
     *
     * <p><b>Coverage boundary.</b> The system expresses RIGID hierarchies:
     * fixed-axis translates and rotates about fixed pivots - which is what the
     * machine PoseStack animators do. Non-rigid transforms (scale, shear,
     * per-vertex deformation, dynamic/polygon axes) are OUT OF SCOPE: the
     * instance record is pos+quat and cannot carry them. Such animators keep
     * the plain CPU path ({@code #part(name, animator)}).
     *
     * <p><b>Static transforms.</b> Per-node static offsets are not needed (a
     * pivot covers the rotation center). Static transforms around the WHOLE
     * chain use the record-pose hooks: {@code baseYawDegrees} (a pre-chain yaw
     * group around the block center) and {@code (offX, offY, offZ)} (a
     * post-chain translation) - the same baseOffset/baseYaw semantics as
     * {@link #parametricPart}. Inter-node static transforms must be folded into
     * the node pivots or the model.
     *
     * <p><b>CPU fallback (auto-derived).</b> Parts declared as chains keep a
     * legacy CPU animator for the shadow pass, Iris Tier-1/packed/exotic paths
     * and the kill switches ({@code MachineBer.isParametricPath} routes those
     * to it). The fallback is DERIVED from the same node declarations - it
     * composes the identical chain with PoseStack math (baseYaw group, base
     * offset, then the nodes in declaration order), so a degraded path renders
     * the identical pose by construction.
     *
     * <p><b>Animation epoch (auto-deduced).</b> All node param fns are sampled
     * at partialTick 0 and 1 and hashed into the machine epoch (the
     * {@link #animationEpoch} machinery) unless an explicit epoch is set. Node
     * fns may read the BlockEntity and partialTick; fns that depend on
     * {@code gameTime} must set {@link #animationEpoch} explicitly (the epoch
     * signature cannot see it - same contract as the declarative shortcuts).
     */
    @SafeVarargs
    public final MachineSpecBuilder<T> jointChainPart(String name, MachineSpec.JointNode<T>... nodes) {
        return jointChainPart(name, name, 0f, 0f, 0f, 0f, null, null, nodes);
    }

    /**
     * {@link #jointChainPart(String, MachineSpec.JointNode[])} over a FOREIGN
     * model part: several logical chains on top of one OBJ part (the
     * {@code part(modelPartName, name, animator)} pattern).
     */
    @SafeVarargs
    public final MachineSpecBuilder<T> jointChainPart(String modelPartName, String name,
                                                      MachineSpec.JointNode<T>... nodes) {
        return jointChainPart(modelPartName, name, 0f, 0f, 0f, 0f, null, null, nodes);
    }

    /** Full chain form: model part + logical name + record statics + optional dynamic quads. */
    @SafeVarargs
    public final MachineSpecBuilder<T> jointChainPart(String modelPartName, String name,
                                                      float offX, float offY, float offZ,
                                                      float baseYawDegrees,
                                                      @Nullable QuadResolver<T> quads,
                                                      @Nullable Function<T, String> cacheKeyFn,
                                                      MachineSpec.JointNode<T>... nodes) {
        // Fail fast at registration, never at render time.
        List<MachineSpec.JointNode<T>> nodeList = List.of(nodes);
        validateChain(name, nodeList);
        // The rig is built FIRST: the per-frame params fn, the epoch sampler and
        // the fallback animator all run its precomputed-layout fast path
        // (RigLayout is derived once here, never per frame).
        MachineSpec.ParametricChain<T> chain =
                new MachineSpec.ParametricChain<>(nodeList, offX, offY, offZ, baseYawDegrees);
        recordDeclarativeChain(name, chain);

        // Per-frame params: the rig's fast path - the nodes are walked once for
        // the fn calls (rotate/axis-translate consume 1 word, vector translate 3),
        // the counts/kinds come from the precomputed layout.
        MachineSpec.ParametricParams<T> params = (be, pt, gt, out4) ->
                chain.evalParams(be, pt, gt, out4);

        // Auto-derived CPU fallback: same params, same composition, PoseStack math.
        // The shared scratch is zeroed here: evalParams holds a caller-zero
        // contract (the render path zero-fills per part; this degraded path must too).
        PartAnimator<T> legacy = (be, pt, gt, pose) -> {
            java.util.Arrays.fill(CHAIN_FALLBACK_SCRATCH, 0f);
            if (!chain.evalParams(be, pt, gt, CHAIN_FALLBACK_SCRATCH)) {
                return false;
            }
            if (baseYawDegrees != 0f) {
                pose.translate(0.5f, 0f, 0.5f);
                pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(baseYawDegrees), 0f, 1f, 0f));
                pose.translate(-0.5f, 0f, -0.5f);
            }
            if (offX != 0f || offY != 0f || offZ != 0f) {
                pose.translate(offX, offY, offZ);
            }
            MachineSpec.applyChainNodes(nodeList, CHAIN_FALLBACK_SCRATCH, pose);
            return true;
        };

        parts.add(new MachineSpec.PartDef<>(name, modelPartName, legacy, quads, cacheKeyFn, true,
                id + "/" + name, lightOverrides.get(name), null,
                tintOverrides.get(name), tintFalloffs.get(name),
                chain,
                params,
                null,
                clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * A {@link #jointChainPart} variant for parts mounted on a PER-FRAME
     * rotating turntable (a ring, drum or carousel): the mount rotation
     * {@code T(pivot)·R(axis, angleFn)·T(-pivot)} joins the chain as NODE 0
     * (see {@link MachineSpec#chainWithTurntable}) - with the packed AnimParams
     * budget (8 words) the mount + joints fit together, the record statics stay
     * fully static (base offset only) and only the record's AnimParams tail
     * changes while the mount turns. The node pivot is
     * {@code turntable.pivot - baseOffset}: the composition is bit-identical to
     * the legacy "mount in the record" order. The chain nodes then act in the
     * MOUNT-ROTATED space, exactly like the trailing ops of a legacy animator
     * written after its {@code rotationY(ringAngle)} base. The turntable angle
     * fn is an animator input: the folded node fn feeds the auto-deduced
     * animation epoch like any declarative fn.
     *
     * @param axisX axisY axisZ  the mount's rotation axis (normalized here)
     * @param pivotX pivotY pivotZ the mount's rotation center in part coordinates
     * @param turntableAngleFn per-frame mount angle in degrees (must CONVERGE
     *                         when the machine is idle, like the node fns)
     */
    @SafeVarargs
    public final MachineSpecBuilder<T> turntableChainPart(String modelPartName, String name,
                                                          float offX, float offY, float offZ,
                                                          float axisX, float axisY, float axisZ,
                                                          float pivotX, float pivotY, float pivotZ,
                                                          ToDoubleBiFunction<T, Float> turntableAngleFn,
                                                          MachineSpec.JointNode<T>... nodes) {
        float[] axis = normalizeAxis(axisX, axisY, axisZ);
        MachineSpec.Turntable turntable = new MachineSpec.Turntable(
                axis[0], axis[1], axis[2], pivotX, pivotY, pivotZ);
        // Mount rotation folded in as node 0: the per-frame params, the CPU
        // fallback and the epoch sampler all see the SAME augmented chain the
        // rig registers (word 0 = the mount angle).
        List<MachineSpec.JointNode<T>> nodeList =
                MachineSpec.chainWithTurntable(List.of(nodes), turntable, offX, offY, offZ, turntableAngleFn);
        // Fail fast at registration, never at render time (budget check covers
        // the mount node).
        validateChain(name, nodeList);
        // The rig is built FIRST: the per-frame params fn, the epoch sampler and
        // the fallback animator all run its precomputed-layout fast path.
        MachineSpec.ParametricChain<T> chain =
                new MachineSpec.ParametricChain<>(nodeList, offX, offY, offZ, 0f);
        recordDeclarativeChain(name, chain);

        MachineSpec.ParametricParams<T> params = (be, pt, gt, out4) ->
                chain.evalParams(be, pt, gt, out4);

        // Auto-derived CPU fallback: base offset, then the chain (node 0 composes
        // the mount rotation with the shifted pivot - identical matrices).
        // Caller-zero contract: zero the shared scratch before evalParams.
        PartAnimator<T> legacy = (be, pt, gt, pose) -> {
            java.util.Arrays.fill(CHAIN_FALLBACK_SCRATCH, 0f);
            if (!chain.evalParams(be, pt, gt, CHAIN_FALLBACK_SCRATCH)) {
                return false;
            }
            if (offX != 0f || offY != 0f || offZ != 0f) {
                pose.translate(offX, offY, offZ);
            }
            MachineSpec.applyChainNodes(nodeList, CHAIN_FALLBACK_SCRATCH, pose);
            return true;
        };

        parts.add(new MachineSpec.PartDef<>(name, modelPartName, legacy, null, null, true,
                id + "/" + name, lightOverrides.get(name), null,
                tintOverrides.get(name), tintFalloffs.get(name),
                chain,
                params,
                null,
                clipPlanesResolvers.get(name)));
        return this;
    }

    // ==================== TRACKED RIGS (ANIM TRACK FORMAT v2) ====================

    /**
     * Declarative GPU animation of a part by a rig whose nodes may carry
     * {@link MachineSpec.Track}s (ANIM TRACK FORMAT v2): a tracked node's
     * parameter is not streamed per frame - the record's AnimParams carry only
     * the motion SEGMENT HEADER (float 28 = t0, float 29 = u0/durationTicks)
     * plus the streamed halfwords of any trackKind==0 nodes (words 4..7), and
     * the VSH samples the tracked nodes' keyframe-pool tracks from uAnimTime
     * itself. A frozen machine whose segment has not changed costs ZERO params
     * evaluation and ZERO record upload (the epoch-keyed word cache in
     * MachineBer).
     *
     * <p><b>Epoch contract.</b> {@code segmentStartTickFn} returns the START
     * TICK of the current motion segment ({@code level.getGameTime()} domain);
     * the header fns must be pure functions of the BE state that also
     * determines the epoch - no per-frame inputs - so state transitions
     * (craft start/stop, speed change, door open/close) change the epoch and
     * rewrite the words ONCE, while the segment runs the cache hits. Unless
     * the spec sets an explicit {@link #animationEpoch}, this fn BECOMES the
     * machine epoch.
     *
     * <p><b>Streamed nodes of a tracked rig must be SEGMENT-PURE.</b> The
     * MachineBer word cache stores the WHOLE AnimParams vec4 per epoch, so the
     * trackKind == 0 (streamed) nodes' halfwords freeze with the segment too:
     * their params fns must stay constant while the epoch is unchanged, like
     * the header fns. A streamed input that varies per tick within a segment
     * would render frozen - hash it into an explicit {@link #animationEpoch}
     * (which re-keys the cache) or keep the rig untracked.
     *
     * <p><b>Header.</b> {@code u0Fn} = the curve position at t0 (usually 0; a
     * cycle track's phase rides here), {@code durationFn} = the segment
     * duration in ticks (a cycle length for cycle tracks); both are packed as
     * binary16 - an exact decoded duration of 0 means FROZEN (u stays u0). The
     * CPU mirror ({@link MachineSpec#sampleTracked}) consumes the same
     * half-rounded values and the same uAnimTime as the VSH, so the
     * auto-derived fallback animator and the hook transforms land on the same
     * pose by construction.
     *
     * <p><b>Budget (fail fast, see {@link #validateChain}).</b> A tracked node
     * cannot be KIND_TRANSLATE (a scalar track has no 3-word semantic); a
     * tracked rig streams at most {@link MachineSpec#TRACKED_STREAM_BUDGET}
     * halfwords, and the logical record (3 header floats + streamed words +
     * one CPU-mirror slot per tracked node) must fit the
     * {@link MachineSpec#CHAIN_PARAM_BUDGET}-float record array.
     */
    @SafeVarargs
    public final MachineSpecBuilder<T> trackedChainPart(String name,
                                                        ToLongFunction<T> segmentStartTickFn,
                                                        ToDoubleFunction<T> u0Fn,
                                                        ToDoubleFunction<T> durationFn,
                                                        MachineSpec.JointNode<T>... nodes) {
        return trackedChainPart(name, name, 0f, 0f, 0f, 0f, null, null,
                segmentStartTickFn, u0Fn, durationFn, nodes);
    }

    /** Full tracked form: model part + logical name + record statics + optional dynamic quads. */
    @SafeVarargs
    public final MachineSpecBuilder<T> trackedChainPart(String modelPartName, String name,
                                                        float offX, float offY, float offZ,
                                                        float baseYawDegrees,
                                                        @Nullable QuadResolver<T> quads,
                                                        @Nullable Function<T, String> cacheKeyFn,
                                                        ToLongFunction<T> segmentStartTickFn,
                                                        ToDoubleFunction<T> u0Fn,
                                                        ToDoubleFunction<T> durationFn,
                                                        MachineSpec.JointNode<T>... nodes) {
        List<MachineSpec.JointNode<T>> nodeList = List.of(nodes);
        validateChain(name, nodeList);
        // The rig is built FIRST: the per-frame params fn and the fallback
        // animator run its precomputed-layout fast path.
        MachineSpec.ParametricChain<T> chain =
                new MachineSpec.ParametricChain<>(nodeList, offX, offY, offZ, baseYawDegrees);
        // NO chainEpochSamplers for tracked rigs: the tracked nodes' per-frame
        // values live in the keyframe pool - the epoch fn IS their sampler.
        trackedEpochFns.add(segmentStartTickFn);

        // Per-frame params: the segment header (pure in epochValue + rig state;
        // the rig's tracked branch preserves it and fills the nodes).
        MachineSpec.ParametricParams<T> params = (be, pt, gt, out4) -> {
            out4[0] = (float) segmentStartTickFn.applyAsLong(be);
            out4[1] = (float) u0Fn.applyAsDouble(be);
            out4[2] = (float) durationFn.applyAsDouble(be);
            return chain.evalParams(be, pt, gt, out4);
        };

        // Auto-derived CPU fallback: same header, same tracked evaluation (the
        // mirror inside the rig), same composition - a degraded path renders
        // the identical pose by construction.
        // Caller-zero contract: zero the shared scratch before evalParams.
        PartAnimator<T> legacy = (be, pt, gt, pose) -> {
            java.util.Arrays.fill(CHAIN_FALLBACK_SCRATCH, 0f);
            CHAIN_FALLBACK_SCRATCH[0] = (float) segmentStartTickFn.applyAsLong(be);
            CHAIN_FALLBACK_SCRATCH[1] = (float) u0Fn.applyAsDouble(be);
            CHAIN_FALLBACK_SCRATCH[2] = (float) durationFn.applyAsDouble(be);
            if (!chain.evalParams(be, pt, gt, CHAIN_FALLBACK_SCRATCH)) {
                return false;
            }
            if (baseYawDegrees != 0f) {
                pose.translate(0.5f, 0f, 0.5f);
                pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(baseYawDegrees), 0f, 1f, 0f));
                pose.translate(-0.5f, 0f, -0.5f);
            }
            if (offX != 0f || offY != 0f || offZ != 0f) {
                pose.translate(offX, offY, offZ);
            }
            MachineSpec.applyChainNodes(nodeList, CHAIN_FALLBACK_SCRATCH, pose);
            return true;
        };

        parts.add(new MachineSpec.PartDef<>(name, modelPartName, legacy, quads, cacheKeyFn, true,
                id + "/" + name, lightOverrides.get(name), null,
                tintOverrides.get(name), tintFalloffs.get(name),
                chain,
                params,
                null,
                clipPlanesResolvers.get(name)));
        return this;
    }

    /**
     * A crafting-gated SPIN (ring, gear, drum): a rotate node with a baked
     * 2-point cycle track ({@link MachineSpec.Track#linear}(360, 0) - one full
     * revolution per motion cycle), driven by the tracked rig header:
     * {@code ticksPerCycleFn} = ticks per revolution, {@code
     * segmentStartTickFn} = the start tick of the current spinning segment
     * (the epoch - the words are written once per segment), {@code
     * phaseAtSegmentStartFn} = the curve position u0 at t0 in [0,1) (the phase
     * the spin continues from; 0 = track start). One declaration derives the
     * GPU rig, the CPU fallback animator and the machine epoch, so the spin
     * stays in lockstep across the vanilla path and the degradation paths.
     */
    public MachineSpecBuilder<T> spinningPart(String partName,
                                              float axisX, float axisY, float axisZ,
                                              float pivotX, float pivotY, float pivotZ,
                                              float offX, float offY, float offZ,
                                              ToLongFunction<T> segmentStartTickFn,
                                              ToDoubleFunction<T> ticksPerCycleFn,
                                              ToDoubleFunction<T> phaseAtSegmentStartFn) {
        return spinningPart(partName, partName, axisX, axisY, axisZ, pivotX, pivotY, pivotZ,
                offX, offY, offZ, null, null, segmentStartTickFn, ticksPerCycleFn,
                phaseAtSegmentStartFn);
    }

    /** {@link #spinningPart(String, float, float, float, float, float, float, float, float, float, ToLongFunction, ToDoubleFunction, ToDoubleFunction)} from vectors, phase 0, no base offset. */
    public MachineSpecBuilder<T> spinningPart(String partName,
                                              Vector3f axis, Vector3f pivot,
                                              ToLongFunction<T> segmentStartTickFn,
                                              ToDoubleFunction<T> ticksPerCycleFn) {
        return spinningPart(partName, partName, axis.x(), axis.y(), axis.z(),
                pivot.x(), pivot.y(), pivot.z(), 0f, 0f, 0f, null, null,
                segmentStartTickFn, ticksPerCycleFn, be -> 0.0);
    }

    /** Full spinning form (foreign model part + optional dynamic quads). */
    public MachineSpecBuilder<T> spinningPart(String modelPartName, String partName,
                                              float axisX, float axisY, float axisZ,
                                              float pivotX, float pivotY, float pivotZ,
                                              float offX, float offY, float offZ,
                                              @Nullable QuadResolver<T> quads,
                                              @Nullable Function<T, String> cacheKeyFn,
                                              ToLongFunction<T> segmentStartTickFn,
                                              ToDoubleFunction<T> ticksPerCycleFn,
                                              ToDoubleFunction<T> phaseAtSegmentStartFn) {
        float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (len < 1.0e-6f) len = 1f;
        // The revolution is baked as the 2-point cycle track (0 -> 360 deg);
        // the phase rides u0 and the rate rides the segment duration. The
        // node's paramsFn is never called (tracked node).
        MachineSpec.JointNode<T> spinNode = MachineSpec.JointNode.rotate(
                axisX / len, axisY / len, axisZ / len, pivotX, pivotY, pivotZ,
                (be, pt, gt, out, off) -> true, MachineSpec.Track.linear(360f, 0f));
        return trackedChainPart(modelPartName, partName, offX, offY, offZ, 0f, quads, cacheKeyFn,
                segmentStartTickFn, phaseAtSegmentStartFn, ticksPerCycleFn, spinNode);
    }

    /** Shared chain validation (fail fast at registration) + axis normalization helper. */
    private void validateChain(String name, List<MachineSpec.JointNode<T>> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("[MachineRenderers:" + id + "] jointChainPart('" + name
                    + "'): at least one joint node is required");
        }
        if (nodes.size() > MachineSpec.MAX_CHAIN_NODES) {
            throw new IllegalStateException("[MachineRenderers:" + id + "] jointChainPart('" + name
                    + "'): chain of " + nodes.size() + " nodes exceeds the shader bound of "
                    + MachineSpec.MAX_CHAIN_NODES);
        }
        int cost = 0;
        int streamed = 0;
        int trackedCount = 0;
        for (MachineSpec.JointNode<T> node : nodes) {
            cost += node.cost();
            if (node.track() != null) {
                if (node.kind() == MachineSpec.KIND_TRANSLATE) {
                    throw new IllegalStateException("[MachineRenderers:" + id + "] tracked chain part('" + name
                            + "'): a tracked node cannot be KIND_TRANSLATE (a scalar track has no 3-word "
                            + "semantic - use KIND_TRANSLATE_AXIS)");
                }
                trackedCount++;
            } else {
                streamed += node.cost();
            }
        }
        if (MachineSpec.isTrackedRig(nodes)) {
            if (streamed > MachineSpec.TRACKED_STREAM_BUDGET) {
                throw new IllegalStateException("[MachineRenderers:" + id + "] tracked chain part('" + name
                        + "'): a tracked rig's streamed nodes cost " + streamed
                        + " halfwords, the tracked header leaves only "
                        + MachineSpec.TRACKED_STREAM_BUDGET
                        + " (record floats 30/31; rotate = 1, axis translate = 1, vector translate = 3)");
            }
            if (streamed + trackedCount
                    > MachineSpec.CHAIN_PARAM_BUDGET - MachineSpec.TRACKED_HEADER_SLOTS) {
                throw new IllegalStateException("[MachineRenderers:" + id + "] tracked chain part('" + name
                        + "'): tracked rig record overflow - " + streamed + " streamed halfwords + "
                        + trackedCount + " tracked nodes do not fit the "
                        + MachineSpec.CHAIN_PARAM_BUDGET + "-float record (3 header slots)");
            }
        } else if (cost > MachineSpec.CHAIN_PARAM_BUDGET) {
            throw new IllegalStateException("[MachineRenderers:" + id + "] jointChainPart('" + name
                    + "'): per-frame parameter budget exceeded - the nodes cost " + cost
                    + " words, the record's AnimParams vec4 carries "
                    + MachineSpec.CHAIN_PARAM_BUDGET
                    + " binary16 words (rotate = 1, axis translate = 1, vector translate = 3)");
        }
    }

    private static float[] normalizeAxis(float axisX, float axisY, float axisZ) {
        float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (len < 1.0e-6f) len = 1f;
        return new float[] {axisX / len, axisY / len, axisZ / len};
    }

    // ==================== SYSTEMIC AUTO-EPOCH DEDUCTION ====================

    private ToLongFunction<T> deduceAnimationEpoch() {
        if (this.animationEpoch != null) {
            return this.animationEpoch;
        }
        boolean hasTracked = !trackedEpochFns.isEmpty();
        boolean hasStreamed = !declarativeAnimFns.isEmpty() || !chainEpochSamplers.isEmpty();
        if (hasTracked && !hasStreamed) {
            // Tracked rigs only: the epoch IS the segment start tick (a single
            // rig) or a mix of the segments' start ticks - per the tracked-rig
            // contract the words are a pure function of (epochValue, rig
            // constants), so epoch equality still means identical words. The
            // per-frame reflection fallbacks below would CHURN per frame and
            // defeat the word cache - never reached with tracked rigs present.
            if (trackedEpochFns.size() == 1) {
                return trackedEpochFns.get(0);
            }
            final List<ToLongFunction<T>> segFns = List.copyOf(trackedEpochFns);
            return be -> {
                long h = 0xcbf29ce484222325L; // FNV-1a 64-bit basis
                for (int i = 0; i < segFns.size(); i++) {
                    h ^= segFns.get(i).applyAsLong(be);
                    h *= 1099511628211L;
                }
                return h;
            };
        }
        if (hasTracked || hasStreamed) {
            final List<ToDoubleBiFunction<T, Float>> fns = List.copyOf(declarativeAnimFns);
            final List<MachineSpec.ParametricParams<T>> samplers = List.copyOf(chainEpochSamplers);
            final List<ToLongFunction<T>> segFns = List.copyOf(trackedEpochFns);
            return be -> {
                long h = 0xcbf29ce484222325L; // FNV-1a 64-bit basis
                for (int i = 0; i < fns.size(); i++) {
                    double v0 = fns.get(i).applyAsDouble(be, 0.0F);
                    double v1 = fns.get(i).applyAsDouble(be, 1.0F);
                    h ^= Double.doubleToRawLongBits(v0);
                    h *= 1099511628211L;
                    h ^= Double.doubleToRawLongBits(v1);
                    h *= 1099511628211L;
                }
                // Joint-chain nodes: hash every record parameter at both epoch
                // partialTicks (the sampler lambda zero-fills the scratch, so a
                // "not drawn" sample hashes deterministically).
                for (int i = 0; i < samplers.size(); i++) {
                    MachineSpec.ParametricParams<T> s = samplers.get(i);
                    for (int e = 0; e < 2; e++) {
                        s.params(be, e == 0 ? 0.0F : 1.0F, 0L, EPOCH_SAMPLER_SCRATCH);
                        for (int k = 0; k < MachineSpec.CHAIN_PARAM_BUDGET; k++) {
                            h ^= Double.doubleToRawLongBits(EPOCH_SAMPLER_SCRATCH[k]);
                            h *= 1099511628211L;
                        }
                    }
                }
                // Tracked rigs: the segment identity (start tick) rides the
                // same hash - the words must refresh when a segment changes.
                for (int i = 0; i < segFns.size(); i++) {
                    h ^= segFns.get(i).applyAsLong(be);
                    h *= 1099511628211L;
                }
                return h;
            };
        }
        try {
            var mAnim = findMethod(beClass, "getAnim", float.class);
            if (mAnim != null) {
                return be -> {
                    try {
                        float a0 = (float) mAnim.invoke(be, 0.0F);
                        float a1 = (float) mAnim.invoke(be, 1.0F);
                        return ((long) Float.floatToRawIntBits(a0) << 32) | (Float.floatToRawIntBits(a1) & 0xFFFFFFFFL);
                    } catch (Throwable t) { return 0L; }
                };
            }
            var mPress = findMethod(beClass, "getPressAnimationProgress", float.class);
            if (mPress != null) {
                return be -> {
                    try {
                        float p0 = (float) mPress.invoke(be, 0.0F);
                        float p1 = (float) mPress.invoke(be, 1.0F);
                        return ((long) Float.floatToRawIntBits(p0) << 32) | (Float.floatToRawIntBits(p1) & 0xFFFFFFFFL);
                    } catch (Throwable t) { return 0L; }
                };
            }
            var mSpin = findMethod(beClass, "getSpin");
            var mLastSpin = findMethod(beClass, "getLastSpin");
            if (mSpin != null && mLastSpin != null) {
                return be -> {
                    try {
                        float s = (float) mSpin.invoke(be);
                        float ls = (float) mLastSpin.invoke(be);
                        return ((long) Float.floatToRawIntBits(s) << 32) | (Float.floatToRawIntBits(ls) & 0xFFFFFFFFL);
                    } catch (Throwable t) { return 0L; }
                };
            }
            var mCrafting = findMethod(beClass, "isCrafting");
            if (mCrafting != null) {
                return be -> {
                    try {
                        return (boolean) mCrafting.invoke(be) ? (System.currentTimeMillis() / 50L) : 0L;
                    } catch (Throwable t) { return 0L; }
                };
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static java.lang.reflect.Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            var m = clazz.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Registers the BER in the vanilla registry + the spec in {@link MachineRenderRegistry}. */
    public void register() {
        ToLongFunction<T> resolvedEpoch = deduceAnimationEpoch();
        MachineSpec<T> spec = new MachineSpec<>(id, beClass, type, modelResolver, facingResolver,
                parts, hooks, viewDistance, blockTransform, itemParts, itemExcept, chunkRenderTypes,
                resolvedEpoch);
        BlockEntityRenderers.register(type, ctx -> new MachineBer<>(spec));
        MachineRenderRegistry.register(spec);
        // Dispatcher bypass: only MachineRenderers-factory types (Nucleus machines).
        com.hbm_m.client.render.NucleusDispatcherBypass.registerManaged(type);
    }
}
