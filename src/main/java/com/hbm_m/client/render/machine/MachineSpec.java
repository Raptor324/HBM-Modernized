package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

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
    record PartDef<T extends BlockEntity>(
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
            @Nullable ParametricAnim parametric, // null = CPU animator; else GPU joint (attrib 15)
            @Nullable ParametricParams<T> parametricParams // per-frame joint parameters
    ) {
        boolean dynamic() { return dynamicQuads != null; }
    }

    /** Kind for {@link ParametricAnim} - rotation around an axis (parameter = angle in degrees). */
    public static final int KIND_ROTATE = 0;
    /** Kind for {@link ParametricAnim} - translation along an axis (parameter = distance in blocks). */
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
     */
    public record ParametricAnim(int kind, float axisX, float axisY, float axisZ,
                                 float pivotX, float pivotY, float pivotZ,
                                 float baseOffsetX, float baseOffsetY, float baseOffsetZ) {
        public boolean hasBaseOffset() {
            return baseOffsetX != 0f || baseOffsetY != 0f || baseOffsetZ != 0f;
        }
    }

    /**
     * Per-frame joint parameters: writes up to 3 values into {@code out4}
     * (angle/distance in [0]; [1]/[2] reserved). Contract: values must CONVERGE to
     * stable ones when the machine is idle (otherwise skip-write degrades into a
     * per-frame tail write). {@code return false} = the part is not drawn this frame.
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
        this.hooks = List.copyOf(hooks);
        this.viewDistance = viewDistance;
        boolean anyAnimated = false;
        boolean anyDynamicAnimator = false;
        for (PartDef<T> p : parts) {
            if (p.animated()) {
                anyAnimated = true;
            }
            if (p.animator() != null && p.dynamic()) {
                anyDynamicAnimator = true;
            }
        }
        this.hasAnimatedParts = anyAnimated;
        this.hasDynamicAnimators = anyDynamicAnimator;
        this.lightSampleKey = (0x4D4143484C534B4FL) ^ (id.hashCode() * 0x9E3779B97F4A7C15L);
        this.blockTransform = blockTransform;
        this.animationEpoch = animationEpoch;
        this.itemParts = itemParts == null ? null : List.copyOf(itemParts);
        this.itemExcept = itemExcept == null ? null : List.copyOf(itemExcept);
        this.chunkRenderTypes = chunkRenderTypes == null ? null : List.copyOf(chunkRenderTypes);
    }

    net.minecraft.world.level.block.entity.BlockEntityType<T> type() { return type; }
    @Nullable List<String> itemParts() { return itemParts; }
    @Nullable List<String> itemExcept() { return itemExcept; }
    @Nullable List<net.minecraft.client.renderer.RenderType> chunkRenderTypes() { return chunkRenderTypes; }

    /**
     * Final part list for item rendering. An explicit {@code itemParts} wins;
     * by default - the spec's NON-dynamic parts (what the BER draws as static and
     * animated), minus {@code itemExcept}. Dynamic parts (per-BE geometry: fluid
     * levels, conditional frame) and undeclared parts altogether (junk OBJ groups)
     * never make it into the item render.
     */
    List<String> deriveItemParts() {
        if (itemParts != null) {
            return itemParts;
        }
        List<String> out = new ArrayList<>();
        for (PartDef<?> p : parts) {
            if (!p.dynamic() && !out.contains(p.modelPartName())) {
                out.add(p.modelPartName());
            }
        }
        if (itemExcept != null) {
            out.removeIf(itemExcept::contains);
        }
        return out;
    }

    @Nullable MachineSpecBuilder.BlockTransform<T> blockTransform() { return blockTransform; }

    /** Machine animation-epoch or null (see {@link MachineSpecBuilder#animationEpoch}). */
    @Nullable java.util.function.ToLongFunction<T> animationEpoch() { return animationEpoch; }

    String id() { return id; }
    List<PartDef<T>> parts() { return parts; }
    /** Whether there are animated (fading out by modelUpdateDistance) parts. */
    boolean hasAnimatedParts() { return hasAnimatedParts; }
    /** Dynamic parts with an animator (doors/seal) - fast-path dirty-skip is forbidden, see field. */
    boolean hasDynamicAnimators() { return hasDynamicAnimators; }
    List<MachineRenderHook<T>> hooks() { return hooks; }
    Function<T, BakedModel> modelResolver() { return modelResolver; }
    Function<T, Direction> facingResolver() { return facingResolver; }
    int viewDistance() { return viewDistance; }

    /** Quads/model of the part for this BE (or null if the part is absent from the model). */
    @Nullable BakedModel partModel(PartDef<T> part, BakedModel multipartModel) {
        if (part.dynamic()) return null;
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
    @Nullable float[] resolveTint(PartDef<T> part, T be) {
        if (part.tintOverride() == null) return null;
        try {
            return part.tintOverride().apply(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] part '{}' tint failed", id, part.name(), t);
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
        if (!part.dynamic()) {
            return partRenderers.get(part.staticCacheKey());
        }
        ConcurrentHashMap<String, MachinePartRenderer> byDynKey = dynPartRenderers.get(part.name());
        return byDynKey == null ? null : byDynKey.get(dynMapKey(dynamicKey));
    }

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
     * is called ONLY when the renderer has not been built yet. Otherwise (VBO already
     * cached) a mountain of temporary BakedQuads would be created every frame for
     * nothing - the profiler showed ~75% of frame time in
     * retextureAndFixUV/BakedQuad.&lt;init&gt; (tanks with fluid) plus a
     * markSpriteActive storm on Embeddium.
     */
    MachinePartRenderer partRendererLazy(PartDef<T> part, @Nullable BakedModel partModel,
                                         T be, @Nullable String dynamicKey) {
        MachinePartRenderer existing = findExistingRenderer(part, dynamicKey);
        if (existing != null) {
            // matches() is not needed: the per-part map guarantees the holder was
            // created for this same (part, dynKey) - the composite key matches by construction.
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
            if (!raced.isAttempted()) {
                raced.ensureBuilt(partModel, dynamicQuads(part, be));
            }
            return raced;
        }
        registerRenderer(part, dynamicKey, created);
        created.ensureBuilt(partModel, dynamicQuads(part, be));
        return created;
    }

    void flush(Matrix4f projection) {
        for (MachinePartRenderer r : partRenderers.values()) {
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
    }

    List<MachinePartRenderer> partRenderersSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(partRenderers.values()));
    }
}
