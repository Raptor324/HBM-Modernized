package com.hbm_m.client.render.weapon;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.LoaderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.obj.ObjModel;

/**
 * Central registry of pre-compiled VBO renderers for weapon parts.
 *
 * <p>Populated at {@code ModelEvent.ModifyBakingResult} via {@link #bindBakedModels(Map)}
 * or lazily on demand during the first draw call. For each {@link WeaponMultipartBakedModel},
 * parts are compiled into native off-heap {@link com.hbm_m.client.render.SingleMeshVboRenderer.VboData}
 * with normalized UVs, wrapped in {@link WeaponPartVboRenderer}, and cached for direct GPU rendering.
 */
@OnlyIn(Dist.CLIENT)
public final class WeaponRenderRegistry {

    public static final String ALL_PARTS_KEY = "__ALL__";
    private static final Map<ResourceLocation, WeaponMultipartBakedModel> BAKED_MODELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Map<String, WeaponPartVboRenderer>> REGISTRY = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> REGISTERED_WEAPON_MODELS = new CopyOnWriteArrayList<>();
    private static final Set<String> WARNED_MISSING = ConcurrentHashMap.newKeySet();
    /** Quads with atlas UVs un-mapped to [0,1] for the Iris/shadow fallback (identity-keyed). */
    private static final Map<BakedQuad, BakedQuad> NORMALIZED_QUADS = new ConcurrentHashMap<>();
    /** Locations whose last on-demand bake failed; retried at most once per window to avoid log spam. */
    private static final Map<ResourceLocation, Long> FAILED_BAKES = new ConcurrentHashMap<>();
    private static final long BAKE_RETRY_MS = 5000L;

    static {
        // Register single invalidation hook with RenderCacheManager (executes on render thread)
        RenderCacheManager.register(reason -> clearAll());
        initDefaults();
    }

    private WeaponRenderRegistry() { }

    /** Ensures all weapon model constants in WeaponResources are loaded into REGISTERED_WEAPON_MODELS. */
    public static void initDefaults() {
        try {
            WeaponResources.init();
        } catch (Throwable ignored) { }
    }

    /** Registers a weapon model ResourceLocation for pre-baking. */
    public static void registerModelLocation(ResourceLocation location) {
        if (location != null && !REGISTERED_WEAPON_MODELS.contains(location)) {
            REGISTERED_WEAPON_MODELS.add(location);
        }
    }

    public static List<ResourceLocation> getRegisteredModelLocations() {
        return Collections.unmodifiableList(REGISTERED_WEAPON_MODELS);
    }

    public static WeaponJointRig getRig(ResourceLocation location) {
        return WeaponJointSpecs.getRig(location);
    }

    public static void registerRig(WeaponJointRig rig) {
        WeaponJointSpecs.registerRig(rig);
    }

    public static Optional<WeaponMultipartBakedModel> getBakedModel(ResourceLocation location) {
        if (location == null) return Optional.empty();
        WeaponMultipartBakedModel model = BAKED_MODELS.get(location);
        if (model == null) {
            model = getOrBakeModel(location);
        }
        return Optional.ofNullable(model);
    }

    public static Optional<SingleMeshVboRenderer.VboData> getVboData(ResourceLocation location, String partName) {
        if (location == null || partName == null) return Optional.empty();
        WeaponMultipartBakedModel model = getOrBakeModel(location);
        if (model == null) return Optional.empty();
        BakedModel part = model.getPart(partName);
        if (part == null) {
            for (Map.Entry<String, BakedModel> entry : model.getParts().entrySet()) {
                if (entry.getKey().equalsIgnoreCase(partName)) {
                    part = entry.getValue();
                    break;
                }
            }
        }
        if (part == null) return Optional.empty();
        // Compile a fresh, unconsumed VboData on demand: VboData is a one-shot
        // off-heap transfer object freed by initVbo(), never safe to cache.
        return Optional.ofNullable(PartGeometry.compile(part, partName, true).toVboDataNormalized(partName));
    }

    /**
     * Retrieves the VBO renderer for a specific part of a weapon model, baking on-demand if necessary.
     */
    @Nullable
    public static WeaponPartVboRenderer getPart(ResourceLocation modelLocation, String partName) {
        if (modelLocation == null || partName == null) return null;
        Map<String, WeaponPartVboRenderer> parts = REGISTRY.get(modelLocation);
        if (parts == null) {
            getOrBakeModel(modelLocation);
            parts = REGISTRY.get(modelLocation);
        }
        if (parts == null) {
            handleMissingPart(modelLocation, partName);
            return null;
        }

        WeaponPartVboRenderer renderer = parts.get(partName);
        if (renderer == null) {
            // Case-insensitive fallback
            for (Map.Entry<String, WeaponPartVboRenderer> entry : parts.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(partName)) {
                    renderer = entry.getValue();
                    break;
                }
            }
            if (renderer == null) {
                handleMissingPart(modelLocation, partName);
            }
        }
        return renderer;
    }

    public static Optional<WeaponPartVboRenderer> getPartRenderer(ResourceLocation modelLocation, String partName) {
        return Optional.ofNullable(getPart(modelLocation, partName));
    }

    /**
     * Checks if a weapon model is registered or bakeable.
     */
    public static boolean hasModel(ResourceLocation modelLocation) {
        if (modelLocation == null) return false;
        if (REGISTRY.containsKey(modelLocation) || BAKED_MODELS.containsKey(modelLocation)) {
            return true;
        }
        return getOrBakeModel(modelLocation) != null;
    }

    /**
     * Checks if a specific part of a weapon model has a registered VBO renderer.
     */
    public static boolean hasPartRenderer(ResourceLocation modelLocation, String partName) {
        if (modelLocation == null || partName == null) return false;
        return getPart(modelLocation, partName) != null;
    }

    public static Set<String> getPartNames(ResourceLocation location) {
        if (location == null) return Collections.emptySet();
        WeaponMultipartBakedModel model = BAKED_MODELS.get(location);
        if (model == null) {
            model = getOrBakeModel(location);
        }
        if (model != null) {
            return model.getPartNameSet();
        }
        Map<String, WeaponPartVboRenderer> renderers = REGISTRY.get(location);
        if (renderers != null) {
            return renderers.keySet();
        }
        return Collections.emptySet();
    }

    /**
     * Hook called from ClientSetup on ModelEvent.RegisterAdditional.
     * Registers all known weapon models so ModelBakery bakes them.
     */
    public static void onModelRegisterAdditional(Object event) {
        initDefaults();
        for (ResourceLocation loc : REGISTERED_WEAPON_MODELS) {
            LoaderHooks.registerAdditionalModel(event, loc);
        }
        MainRegistry.LOGGER.info("[WeaponRenderRegistry] Registered {} weapon models for loading",
                REGISTERED_WEAPON_MODELS.size());
    }

    /**
     * Hook called from ClientSetup on ModelEvent.ModifyBakingResult.
     * Scans baked models, registers WeaponMultipartBakedModel instances, and pre-compiles VboData.
     */
    public static void onModelBake(Map<?, BakedModel> models) {
        bindBakedModels(models);
    }

    /**
     * Binds baked weapon models from the ModelManager bake map and pre-bakes all registered weapon OBJ assets.
     * Called during {@code ModelEvent.ModifyBakingResult}.
     */
    public static void bindBakedModels(Map<?, BakedModel> models) {
        clearAll();
        initDefaults();
        int weaponCount = 0;
        int partCount = 0;

        if (models != null) {
            for (Map.Entry<?, BakedModel> entry : models.entrySet()) {
                if (!(entry.getValue() instanceof WeaponMultipartBakedModel weaponModel)) {
                    continue;
                }

                ResourceLocation modelKey = LoaderHooks.getModelId(entry.getKey());
                ResourceLocation targetKey = weaponModel.getModelLocation() != null
                        ? weaponModel.getModelLocation()
                        : modelKey;

                BAKED_MODELS.put(targetKey, weaponModel);
                if (modelKey != null && !targetKey.equals(modelKey)) {
                    BAKED_MODELS.put(modelKey, weaponModel);
                }

                int compiled = compileAndStoreModel(targetKey, weaponModel);
                if (modelKey != null && !targetKey.equals(modelKey)) {
                    Map<String, WeaponPartVboRenderer> targetRenderers = REGISTRY.get(targetKey);
                    if (targetRenderers != null) {
                        REGISTRY.put(modelKey, targetRenderers);
                    }
                }

                partCount += compiled;
                weaponCount++;
                MainRegistry.LOGGER.debug("WeaponRenderRegistry: registered baked model '{}' with {} parts",
                        targetKey, compiled);
            }
        }

        // Pre-bake all standalone weapon models registered in WeaponResources
        Function<Material, TextureAtlasSprite> spriteGetter = getDefaultSpriteGetter();
        for (ResourceLocation loc : REGISTERED_WEAPON_MODELS) {
            if (!BAKED_MODELS.containsKey(loc)) {
                try {
                    WeaponMultipartBakedModel model = bakeObjDirect(loc, null, spriteGetter);
                    if (model != null) {
                        weaponCount++;
                        partCount += model.getParts().size();
                    }
                } catch (Exception e) {
                    MainRegistry.LOGGER.warn("WeaponRenderRegistry: Failed to pre-bake weapon model '{}': {}", loc, e.getMessage());
                }
            }
        }

        MainRegistry.LOGGER.info("WeaponRenderRegistry: bound {} weapon models ({} total VBO parts)",
                weaponCount, partCount);
    }

    /**
     * Resolves or bakes a WeaponMultipartBakedModel on-demand.
     */
    public static WeaponMultipartBakedModel getOrBakeModel(ResourceLocation location) {
        if (location == null) return null;
        WeaponMultipartBakedModel existing = BAKED_MODELS.get(location);
        if (existing != null) {
            return existing;
        }
        Long failedAt = FAILED_BAKES.get(location);
        if (failedAt != null && System.currentTimeMillis() - failedAt < BAKE_RETRY_MS) {
            return null;
        }
        synchronized (WeaponRenderRegistry.class) {
            existing = BAKED_MODELS.get(location);
            if (existing != null) {
                return existing;
            }
            try {
                MainRegistry.LOGGER.debug("WeaponRenderRegistry: On-demand baking weapon model '{}'", location);
                return bakeObjDirect(location, null, getDefaultSpriteGetter());
            } catch (Exception | OutOfMemoryError e) {
                FAILED_BAKES.put(location, System.currentTimeMillis());
                MainRegistry.LOGGER.error("WeaponRenderRegistry: Failed on-demand baking for '{}' (retry in {} ms)", location, BAKE_RETRY_MS, e);
                return null;
            }
        }
    }

    /**
     * Compiles all parts of a WeaponMultipartBakedModel into native VboData with normalized UVs
     * and wraps each in a WeaponPartVboRenderer.
     */
    public static int compileAndStoreModel(ResourceLocation modelId, WeaponMultipartBakedModel model) {
        Map<String, WeaponPartVboRenderer> partRenderers = new ConcurrentHashMap<>();
        List<BakedQuad> allRawQuads = new java.util.ArrayList<>();
        List<BakedQuad> allNormQuads = new java.util.ArrayList<>();

        for (Map.Entry<String, BakedModel> partEntry : model.getParts().entrySet()) {
            String partName = partEntry.getKey();
            BakedModel partBaked = partEntry.getValue();
            if (partBaked == null) continue;

            PartGeometry geo = PartGeometry.compile(partBaked, partName, true);
            SingleMeshVboRenderer.VboData vboData = geo.toVboDataNormalized(partName);
            if (vboData != null) {
                List<BakedQuad> rawQuads = geo.solidQuads();
                List<BakedQuad> normQuads = new java.util.ArrayList<>(rawQuads.size());
                for (BakedQuad q : rawQuads) {
                    normQuads.add(normalizeQuadUv(q));
                }
                allRawQuads.addAll(rawQuads);
                allNormQuads.addAll(normQuads);
                // VboData ownership is fully handed to the renderer; initVbo()
                // consumes and frees it - it must never be retained here.
                partRenderers.put(partName, new WeaponPartVboRenderer(vboData, List.copyOf(normQuads)));
            }
        }

        if (!allRawQuads.isEmpty() && !partRenderers.containsKey(ALL_PARTS_KEY)) {
            SingleMeshVboRenderer.VboData allVbo = PartGeometry.buildVboDataFromQuadsNormalized(allRawQuads, ALL_PARTS_KEY);
            if (allVbo != null) {
                partRenderers.put(ALL_PARTS_KEY, new WeaponPartVboRenderer(allVbo, List.copyOf(allNormQuads)));
            }
        }

        REGISTRY.put(modelId, partRenderers);
        return model.getParts().size();
    }

    /**
     * Helper to compile baked quads into normalized VboData.
     */
    public static SingleMeshVboRenderer.VboData compileWeaponVboData(List<BakedQuad> quads, String partName) {
        return PartGeometry.buildVboDataFromQuadsNormalized(quads, partName);
    }

    /**
     * Renders a specific weapon part using the registered WeaponPartVboRenderer.
     * Passes current GunGL pose, texture, light, overlay, and color tint.
     */
    public static void renderPart(ResourceLocation modelLocation, String partName, PoseStack pose) {
        renderPart(modelLocation, partName, pose, GunGL.light(), GunGL.overlay(),
                   GunGL.red(), GunGL.green(), GunGL.blue(), GunGL.alpha());
    }

    /**
     * Extended renderPart with explicit color, light, and overlay parameters.
     */
    public static void renderPart(ResourceLocation modelLocation, String partName, PoseStack pose,
                                  int light, int overlay, float r, float g, float b, float a) {
        if (!com.hbm_m.client.render.ClientRenderFlags.forceVanillaImmediate()) {
            WeaponPartVboRenderer renderer = getPart(modelLocation, partName);
            if (renderer != null && renderer.renderPart(pose, GunGL.texture(), light, overlay, r, g, b, a)) {
                return;
            }
        }
        // Fallback to immediate quads only if forceVanillaImmediate is enabled or VBO/companion failed
        if (ShaderCompatibilityDetector.isRenderingShadowPass()
                && (GunGL.isBlend() && GunGL.isAdditive())) {
            return;
        }
        boolean unshaded = (GunGL.isBlend() && GunGL.isAdditive()) || GunGL.fullbright() || !GunGL.lighting();
        int effectiveLight = unshaded ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : light;
        renderPartQuads(modelLocation, partName, pose, GunGL.buffer(), effectiveLight, overlay, r, g, b, a);
    }

    /**
     * Handles missing model or part gracefully with throttled, informative logging (once per missing key).
     */
    private static void handleMissingPart(ResourceLocation modelLocation, String partName) {
        String key = (modelLocation != null ? modelLocation.toString() : "null") + "#" + (partName != null ? partName : "null");
        if (WARNED_MISSING.add(key)) {
            if (modelLocation == null) {
                MainRegistry.LOGGER.warn("WeaponRenderRegistry: Attempted to render part '{}' with a null model ResourceLocation!", partName);
            } else if (!hasModel(modelLocation)) {
                MainRegistry.LOGGER.warn("WeaponRenderRegistry: Weapon model '{}' is not registered or failed to bake when attempting to render part '{}'. (Registered models: {})",
                        modelLocation, partName, BAKED_MODELS.size());
            } else {
                Set<String> available = getPartNames(modelLocation);
                MainRegistry.LOGGER.warn("WeaponRenderRegistry: Weapon model '{}' is missing part '{}'. Available parts in this model: {}",
                        modelLocation, partName, available);
            }
        }
    }

    /**
     * Renders all parts of the specified weapon model with the current GunGL pose and texture.
     */
    public static void renderAll(ResourceLocation modelLocation, PoseStack pose) {
        if (modelLocation == null) return;
        if (com.hbm_m.client.render.ClientRenderFlags.forceVanillaImmediate()) {
            renderAllQuadsFallback(modelLocation, pose, null);
            return;
        }
        Map<String, WeaponPartVboRenderer> parts = REGISTRY.get(modelLocation);
        if (parts == null) {
            getOrBakeModel(modelLocation);
            parts = REGISTRY.get(modelLocation);
        }
        if (parts != null) {
            WeaponPartVboRenderer merged = parts.get(ALL_PARTS_KEY);
            if (merged != null && merged.renderPart(pose, GunGL.texture(), GunGL.light(), GunGL.overlay(),
                    GunGL.red(), GunGL.green(), GunGL.blue(), GunGL.alpha())) {
                return;
            }
            for (String partName : parts.keySet()) {
                if (!ALL_PARTS_KEY.equals(partName)) {
                    renderPart(modelLocation, partName, pose);
                }
            }
        } else {
            handleMissingPart(modelLocation, "ALL");
        }
    }

    /**
     * Renders all parts of the specified weapon model except the excluded part names.
     */
    public static void renderAllExcept(ResourceLocation modelLocation, PoseStack pose, String... except) {
        if (modelLocation == null) return;
        if (com.hbm_m.client.render.ClientRenderFlags.forceVanillaImmediate()) {
            renderAllQuadsFallback(modelLocation, pose, except);
            return;
        }
        Map<String, WeaponPartVboRenderer> parts = REGISTRY.get(modelLocation);
        if (parts == null) {
            getOrBakeModel(modelLocation);
            parts = REGISTRY.get(modelLocation);
        }
        if (parts != null) {
            Set<String> skip = Set.of(except);
            for (String partName : parts.keySet()) {
                if (ALL_PARTS_KEY.equals(partName)) continue;
                boolean match = skip.contains(partName);
                if (!match) {
                    for (String ex : except) {
                        if (ex.equalsIgnoreCase(partName)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) {
                    renderPart(modelLocation, partName, pose);
                }
            }
        } else {
            handleMissingPart(modelLocation, "ALL_EXCEPT");
        }
    }

    /**
     * Iris/shadow-pass variant of {@link #renderAll}: emits every part's baked quads into the
     * current GunGL buffer instead of applying the weapon_lit shader. Applying a mod-owned
     * {@code ShaderInstance} here trips Oculus's MixinShaderInstance.onTail, which locks
     * {@code DepthColorStorage} (all subsequent depthMask/colorMask calls cancelled) until our
     * watchdog resets it - the world pass then renders with wrong write masks and disappears.
     */
    private static void renderAllQuadsFallback(ResourceLocation modelLocation, PoseStack pose,
                                               @org.jetbrains.annotations.Nullable String[] except) {
        WeaponMultipartBakedModel model = BAKED_MODELS.get(modelLocation);
        if (model == null) {
            model = getOrBakeModel(modelLocation);
        }
        if (model == null) {
            handleMissingPart(modelLocation, "ALL");
            return;
        }
        var vc = GunGL.buffer();
        int light = GunGL.light();
        int overlay = GunGL.overlay();
        float r = GunGL.red(), g = GunGL.green(), b = GunGL.blue(), a = GunGL.alpha();
        for (String partName : model.getParts().keySet()) {
            if (except != null && except.length > 0) {
                boolean match = false;
                for (String ex : except) {
                    if (ex.equalsIgnoreCase(partName)) {
                        match = true;
                        break;
                    }
                }
                if (match) continue;
            }
            renderPartQuads(modelLocation, partName, pose, vc, light, overlay, r, g, b, a);
        }
    }

    /**
     * Emits baked quads of a weapon part into a VertexConsumer (used for glint overlays, clip planes, and Iris fallback).
     */
    public static void renderPartQuads(ResourceLocation modelLocation, String partName, PoseStack pose,
                                       VertexConsumer vc, int light, int overlay,
                                       float r, float g, float b, float a) {
        if (modelLocation == null || partName == null || vc == null) return;
        WeaponMultipartBakedModel model = BAKED_MODELS.get(modelLocation);
        if (model == null) {
            model = getOrBakeModel(modelLocation);
        }
        if (model != null) {
            BakedModel partBaked = model.getPart(partName);
            if (partBaked == null) {
                for (Map.Entry<String, BakedModel> entry : model.getParts().entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(partName)) {
                        partBaked = entry.getValue();
                        break;
                    }
                }
            }
            if (partBaked != null) {
                var rand = net.minecraft.util.RandomSource.create();
                boolean guiLighting = com.hbm_m.client.weapon.GunGL.isGuiLighting();
                boolean unshaded = (GunGL.isBlend() && GunGL.isAdditive()) || GunGL.fullbright() || !GunGL.lighting();
                int effectiveLight = unshaded ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : light;
                for (Direction d : Direction.values()) {
                    float shade = unshaded ? 1.0f : vanillaFaceShade(d, guiLighting);
                    for (BakedQuad quad : partBaked.getQuads(null, d, rand)) {
                        vc.putBulkData(pose.last(), normalizeQuadUv(quad), r * shade, g * shade, b * shade, a, effectiveLight, overlay, true);
                    }
                }
                for (BakedQuad quad : partBaked.getQuads(null, null, rand)) {
                    vc.putBulkData(pose.last(), normalizeQuadUv(quad), r, g, b, a, effectiveLight, overlay, true);
                }
            }
        }
    }

    /**
     * Vanilla {@code Level.getShade} face brightness (1.0 UP, 0.5 DOWN, 0.8 N/S, 0.6 E/W).
     * GUI-INVENTORY draws flip the model's Y axis, so UP and DOWN swap there.
     */
    private static float vanillaFaceShade(Direction direction, boolean guiLighting) {
        if (guiLighting) {
            if (direction == Direction.UP) return 0.5f;
            if (direction == Direction.DOWN) return 1.0f;
        }
        return switch (direction) {
            case DOWN -> 0.5f;
            case NORTH, SOUTH -> 0.8f;
            case WEST, EAST -> 0.6f;
            default -> 1.0f;
        };
    }

    /**
     * Un-maps a baked quad's UVs from its atlas sprite region back to [0,1] so the
     * Iris/shadow-pass fallback can sample the weapon's standalone PNG texture
     * (the VBO path does the same math in {@code PartGeometry.buildVboDataFromQuads}).
     * Identity-keyed cache: the same BakedQuad instances are re-emitted every frame.
     */
    private static BakedQuad normalizeQuadUv(BakedQuad quad) {
        TextureAtlasSprite sprite = quad.getSprite();
        if (sprite == null) return quad;
        return NORMALIZED_QUADS.computeIfAbsent(quad, q -> {
            float u0 = sprite.getU0(), v0 = sprite.getV0();
            float du = sprite.getU1() - u0, dv = sprite.getV1() - v0;
            if (Math.abs(du) < 1e-7f || Math.abs(dv) < 1e-7f) return q;
            int[] src = q.getVertices();
            int[] dst = src.clone();
            for (int i = 0; i < 4; i++) {
                int base = i * 8;
                float u = Float.intBitsToFloat(dst[base + 4]);
                float v = Float.intBitsToFloat(dst[base + 5]);
                dst[base + 4] = Float.floatToIntBits((u - u0) / du);
                dst[base + 5] = Float.floatToIntBits((v - v0) / dv);
            }
            return new BakedQuad(dst, q.getTintIndex(), q.getDirection(), sprite, q.isShade());
        });
    }

    public static Function<Material, TextureAtlasSprite> getDefaultSpriteGetter() {
        return mat -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.getModelManager() != null) {
                    var atlas = mc.getModelManager().getAtlas(mat.atlasLocation());
                    if (atlas != null) {
                        TextureAtlasSprite sprite = atlas.getSprite(mat.texture());
                        if (sprite != null) return sprite;
                    }
                }
            } catch (Throwable ignored) { }
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    return mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                            .apply(MissingTextureAtlasSprite.getLocation());
                }
            } catch (Throwable ignored) { }
            return null;
        };
    }

    /**
     * Programmatic baking helper to bake an OBJ model directly into WeaponMultipartBakedModel
     * without requiring an explicit JSON file.
     */
    public static WeaponMultipartBakedModel bakeObjDirect(ResourceLocation objLocation,
                                                          ModelBaker baker,
                                                          Function<Material, TextureAtlasSprite> spriteGetter) {
        ObjModel objModel = LoaderHooks.loadObjModel(objLocation, true);
        Map<String, BakedModel> bakedParts = new ConcurrentHashMap<>();
        ModelState identityState = new ModelState() {
            @Override
            public Transformation getRotation() {
                return Transformation.identity();
            }
        };

        final Function<Material, TextureAtlasSprite> effectiveSpriteGetter = (spriteGetter != null)
                ? spriteGetter
                : getDefaultSpriteGetter();

        Set<String> rootParts = objModel.getRootComponentNames();
        Set<String> targetParts = (rootParts == null || rootParts.isEmpty()) ? Set.of("default", "") : rootParts;

        for (String partName : targetParts) {
            IGeometryBakingContext partContext = new IGeometryBakingContext() {
                @Override public String getModelName() { return objLocation.toString(); }
                @Override public boolean isGui3d() { return true; }
                @Override public boolean useBlockLight() { return true; }
                @Override public boolean useAmbientOcclusion() { return true; }
                @Override public ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }
                @Override public Transformation getRootTransform() { return Transformation.identity(); }
                @Override public ResourceLocation getRenderTypeHint() { return null; }
                @Override public boolean isComponentVisible(String component, boolean fallback) {
                    return component.equals(partName) || component.startsWith(partName + "/");
                }
                @Override public boolean hasMaterial(String name) { return true; }
                @Override public Material getMaterial(String name) {
                    return new Material(TextureAtlas.LOCATION_BLOCKS, objLocation);
                }
            };
            BakedModel baked = LoaderHooks.bakeObjModel(objModel, partContext, baker, effectiveSpriteGetter, identityState, ItemOverrides.EMPTY, objLocation);
            if (baked != null) {
                bakedParts.put(partName, baked);
            }
        }

        WeaponMultipartBakedModel model = new WeaponMultipartBakedModel(bakedParts, ItemTransforms.NO_TRANSFORMS, objLocation);
        BAKED_MODELS.put(objLocation, model);
        compileAndStoreModel(objLocation, model);
        return model;
    }

    /**
     * Clears all cached renderers and frees OpenGL resources and off-heap memory.
     * Safe for resource reload (F3+T) and session disconnect.
     */
    public static void clearAll() {
        for (Map<String, WeaponPartVboRenderer> partMap : REGISTRY.values()) {
            for (WeaponPartVboRenderer renderer : partMap.values()) {
                if (renderer != null) {
                    renderer.cleanup();
                }
            }
        }
        REGISTRY.clear();

        BAKED_MODELS.clear();
        WARNED_MISSING.clear();
        NORMALIZED_QUADS.clear();
        FAILED_BAKES.clear();

        MainRegistry.LOGGER.debug("[WeaponRenderRegistry] Cleared all weapon baked models and VBO renderers");
    }

    /** Alias for {@link #clearAll()} */
    public static void clear() {
        clearAll();
    }
}
