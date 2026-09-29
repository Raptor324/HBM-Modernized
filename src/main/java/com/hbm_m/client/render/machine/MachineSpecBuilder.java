package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

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
    private int viewDistance = -1;
    @Nullable
    private java.util.List<String> itemParts;
    @Nullable
    private java.util.List<String> itemExcept;
    @Nullable
    private java.util.List<net.minecraft.client.renderer.RenderType> chunkRenderTypes;
    @Nullable
    private java.util.function.ToLongFunction<T> animationEpoch;

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
        parts.add(new MachineSpec.PartDef<>(name, name, null, null, null, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null));
        return this;
    }

    /** Animated part: {@link PartAnimator} sets the transform relative to the block. */
    public MachineSpecBuilder<T> part(String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, null, null, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null));
        return this;
    }

    /**
     * Animated part referencing a foreign model: several logical parts on top of
     * one OBJ part (e.g. 4 gears from the "Cog" part).
     */
    public MachineSpecBuilder<T> part(String modelPartName, String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, modelPartName, animator, null, null, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null));
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
        parts.add(new MachineSpec.PartDef<>(name, name, transform, null, null, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null));
        return this;
    }

    /**
     * Dynamic part with per-BE geometry (fluid tank walls by fluid, DAE nodes).
     * The VBO is cached keyed on {@code cacheKeyFn}; the returned quad list may be empty.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, null, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null));
        return this;
    }

    /** Dynamic part with per-BE geometry AND animation (e.g. the dish of the large/small radar). */
    public MachineSpecBuilder<T> dynamicPart(String name, PartAnimator<T> animator,
                                             QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, quads, cacheKeyFn, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null));
        return this;
    }

    /**
     * Dynamic part with per-BE geometry and a fixed transform "animator" (legacy
     * offsets). Content is static (changes with BE state, not time) - NOT gated by
     * modelUpdateDistance.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                             PartAnimator<T> transform) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name), null, null));
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
        parts.add(new MachineSpec.PartDef<>(name, name, transform, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), uvRectFn, tintOverrides.get(name), tintFalloffs.get(name), null, null));
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
        parts.add(new MachineSpec.PartDef<>(name, name, legacyAnimator, null, null, true,
                id + "/" + name, lightOverrides.get(name), null,
                tintOverrides.get(name), tintFalloffs.get(name),
                new MachineSpec.ParametricAnim(kind, axisX, axisY, axisZ, pivotX, pivotY, pivotZ,
                        offX, offY, offZ),
                paramsFn));
        return this;
    }

    /** Registers the BER in the vanilla registry + the spec in {@link MachineRenderRegistry}. */
    public void register() {
        MachineSpec<T> spec = new MachineSpec<>(id, beClass, type, modelResolver, facingResolver,
                parts, hooks, viewDistance, blockTransform, itemParts, itemExcept, chunkRenderTypes,
                animationEpoch);
        BlockEntityRenderers.register(type, ctx -> new MachineBer<>(spec));
        MachineRenderRegistry.register(spec);
        // Dispatcher bypass: only MachineRenderers-factory types (Nucleus machines).
        com.hbm_m.client.render.NucleusDispatcherBypass.registerManaged(type);
    }
}
