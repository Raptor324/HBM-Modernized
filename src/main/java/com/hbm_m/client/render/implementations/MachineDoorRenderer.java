package com.hbm_m.client.render.implementations;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

import com.hbm_m.block.entity.doors.BlastDoorDecl;
import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.machines.TransitionSealBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.render.machine.DoorPartClipper;
import com.hbm_m.blockentity.machines.TransitionSealBlockEntity;
import com.hbm_m.client.compat.create.ContraptionDoorAnimCache;
import com.hbm_m.client.loader.dae.DaeAnimation;
import com.hbm_m.client.loader.dae.DaeModel;
import com.hbm_m.client.loader.dae.DaeNode;
import com.hbm_m.client.loader.dae.DaeQuadBaker;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.compat.ContraptionRenderCompat;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Doors on the {@link MachineRenderers} factory (replacement for DoorRenderer/DoorVboRenderer/
 * TransitionSealRenderer). Two specs:
 * <ul>
 *   <li><b>door</b> -- all 13 {@link DoorDecl} on a single BE type. Parts are the union
 *       of getPartNames()+getChildren() of all declarations; a part missing from a specific
 *       model is skipped by the engine (partModel == null). All parts are declared as
 *       staticPart(transform) -- the DoorDecl transform acts as the animator, but parts
 *       are NOT gated by the animation distance (doors are large statics).
 *       DAE skins go through {@code dynamicPart} on the DAE model's node: quads are resolved
 *       per the BE's skin, the animator reproduces the {@code localMatrix} chain from the root.
 *       The nodes join the machines' shared instancing/MDI pipeline.</li>
 *   <li><b>transition_seal</b> -- a 26x24 DAE model, the same dynamicPart nodes, the "animation"
 *       clip (24 s).</li>
 * </ul>
 * DAE models load at register() (plain XML from the jar); if loading fails, the corresponding
 * door renders via a lazily-loading fallback hook.
 * Door item rendering (skins, DAE item models) lives in DoorBakedModel and is untouched
 * by the factory (dynamicParts do not reach deriveItemParts).
 */
@OnlyIn(Dist.CLIENT)
public final class MachineDoorRenderer {

    private static final DoorDecl[] DOOR_DECLS = {
            DoorDecl.LARGE_VEHICLE_DOOR,
            DoorDecl.ROUND_AIRLOCK_DOOR,
            DoorDecl.FIRE_DOOR,
            DoorDecl.SLIDING_BLAST_DOOR,
            DoorDecl.SLIDING_SEAL_DOOR,
            DoorDecl.SECURE_ACCESS_DOOR,
            DoorDecl.QE_SLIDING,
            DoorDecl.QE_CONTAINMENT,
            DoorDecl.WATER_DOOR,
            DoorDecl.SILO_HATCH,
            DoorDecl.SILO_HATCH_LARGE,
            DoorDecl.CARGO_DOOR,
            DoorDecl.VAULT_DOOR,
            BlastDoorDecl.INSTANCE,
    };

    /** The nucleusDoorSkinSharing config is the primary source; -Dhbm.doorSkinSharing=false is an emergency override. */
    private static final boolean SKIN_SHARING_FLAG =
            !"false".equalsIgnoreCase(System.getProperty("hbm.doorSkinSharing", "true"));
    /** The doorPartCulling config is the primary source; -Dhbm.doorPartCulling=false is an emergency override. */
    private static final boolean DOOR_PART_CULLING_FLAG =
            !"false".equalsIgnoreCase(System.getProperty("hbm.doorPartCulling", "true"));

    // ==================== DOOR CLIP DEBUG (-Dhbm.debugDoorClip=true) ====================
    // One-time dump for diagnostics: (a) why resolver returned/omitted planes,
    // (b) which sprites/UVs arrive at clip tier. Logged once per key.

    /** Door clip debug dump enabled via doorClipDebug config. */
    static boolean debugDoorClip() {
        return com.hbm_m.config.ModClothConfig.get().doorClipDebug;
    }
    /** Keys already logged by clip debug. */
    private static final java.util.Set<String> DEBUG_CLIP_LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** One-time clip debug log: '[HBM-M][DoorClipDebug] <key>: <msg>'. */
    static void debugClipLog(String key, java.util.function.Supplier<String> msg) {
        if (!debugDoorClip()) return;
        if (!DEBUG_CLIP_LOGGED.add(key)) return;
        com.hbm_m.main.MainRegistry.LOGGER.info("[HBM-M][DoorClipDebug] {}: {}", key, msg.get());
    }
    private static boolean skinSharing() {
        // Share pipeline restored to contract:
        // cache holds per-quad normalized UVs (normalizeQuadUvsPerQuad), remapping is per-quad
        // in clipper. Per-part uvRect copies (not shared scratch) are required: rect lives
        // until instance batch flush; shared scratch corrupted all part textures.
        return com.hbm_m.config.ModClothConfig.get().nucleusDoorSkinSharing && SKIN_SHARING_FLAG;
    }

    /**
     * Pure query (no latch): does the loaded block_lit_instanced shader actually have
     * the InstUvRect attribute. Guards against "fresh classes, stale resources"
     * (running without processResources): with the old shader (texCoord = UV0
     * passthrough) the normalized VBO would map the WHOLE atlas onto the model --
     * fall back to per-skin mode. Memoized per shader instance (a GL query per
     * shader, not per call); a shader reload produces a new instance and the memo
     * follows. The OLD lazy latch ("checked once, stuck false when the shader was
     * not loaded yet") is gone: syncShareState re-evaluates shareSkins() every frame
     * and a late-loaded shader is just a desired-state flip.
     */
    private static Object uvRectShaderMemo;
    private static boolean uvRectShaderMemoVal;

    private static boolean uvRectAttribSupported() {
        var shader = com.hbm_m.client.render.shader.ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            return false; // shader not loaded yet -- doors are not rendered, nothing to share
        }
        if (uvRectShaderMemo != shader) {
            uvRectShaderMemo = shader;
            uvRectShaderMemoVal = org.lwjgl.opengl.GL20.glGetAttribLocation(shader.getId(), "InstUvRect") >= 0;
            if (!uvRectShaderMemoVal) {
                MainRegistry.LOGGER.warn("[HBM-M] block_lit_instanced without InstUvRect -- stale shader resources; "
                        + "door skin sharing disabled (rebuild/restart with processResources)");
            }
        }
        return uvRectShaderMemoVal;
    }

    /**
     * VBO sharing between texture skins: the cache key omits the skin, the VBO holds
     * normalized sprite-local UVs, and the skin goes into a per-instance uvRect (remapped
     * in the vertex shader). ONLY outside Iris: companion/GPU-bake/shadows under shader packs
     * keep atlas quads per-skin -- under Iris the keys remain per-skin (behavior as before).
     */
    private static boolean shareSkins() {
        return skinSharing()
                && !com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()
                && uvRectAttribSupported();
    }

    // ==================== SHARE-STATE: per-frame desired!=current + epoch ====================
    // shareSkins() depends on dynamic inputs: Iris on/off (ShaderCompatibilityDetector),
    // config toggle nucleusDoorSkinSharing, and the moment block_lit_instanced finishes loading.
    // Determinism mechanism: each frame desired is compared to current; on mismatch,
    // shareEpoch++, current=desired and simultaneously invalidates everything built in the prior mode:
    // clipper memo-cache (epoch is in its key), PART/VBO door cache (epoch in doorPartCacheKey/daeCacheKey),
    // and full clear of MachineSpec.partRenderers (global registry MachineRenderRegistry.clearAll()
    // to prevent leaking old renderers referencing stale VBOs). Both cache-key resolvers and both
    // uvRect resolvers read shareSkins()/shareEpoch() atomically between syncShareState() and
    // key usage on the single render thread.

    private static boolean shareStateCurrent;
    private static boolean shareCullCurrent;
    private static boolean shareStateInitialized;
    private static long shareEpoch;
    private static long shareStateFrame = -1L;

    /** Current share epoch; included in all door cache keys (PART/VBO/dae/clipper-memo). */
    public static long shareEpoch() {
        return shareEpoch;
    }

    /**
     * Per-frame sync of the share state (called once per frame from the MachineBer
     * door loop -- tryFastAssertRender / doCollectRender, both door-BE gated):
     * computes desired=shareSkins() and, on a change, bumps shareEpoch and
     * invalidates every cache built in the previous mode (see the block comment
     * above). idempotent per frame (frame-stamped).
     */
    public static void syncShareState() {
        long frame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (shareStateFrame == frame) {
            return;
        }
        shareStateFrame = frame;
        boolean desired = shareSkins();
        boolean desiredCull = com.hbm_m.config.ModClothConfig.get().doorPartCulling && DOOR_PART_CULLING_FLAG;
        if (shareStateInitialized && desired == shareStateCurrent && desiredCull == shareCullCurrent) {
            return;
        }
        String prev = shareStateInitialized ? Boolean.toString(shareStateCurrent) : "init";
        String prevCull = shareStateInitialized ? Boolean.toString(shareCullCurrent) : "init";
        shareEpoch++;
        shareStateInitialized = true;
        shareStateCurrent = desired;
        shareCullCurrent = desiredCull;
        MainRegistry.LOGGER.info("[HBM-M] Door share state (skinSharing {} -> {}, doorPartCulling {} -> {}, shareEpoch={}) -- invalidating door VBO/dae/clip caches",
                prev, desired, prevCull, desiredCull, shareEpoch);
        // Full clear of partRenderers in global registry + machine GPU caches; dae-cache
        // and part canonical sprite cache are door-specific; clipper memo-cache is keyed
        // by epoch and expires on its own (plus quad lists change identity).
        com.hbm_m.client.render.machine.MachineRenderRegistry.clearAll();
        clearDaeCaches();
        clearPartSpriteCache();
    }

    /** A DAE scene node with its chain of parents (for composing localMatrix in the animator). */
    private record DaeNodePath(String name, DaeNode node, List<DaeNode> chain) {}

    /** DAE models loaded at register() (doors per decl, the seal separately). */
    private static final ConcurrentHashMap<ResourceLocation, DaeModel> DAE_MODELS = new ConcurrentHashMap<>();
    /** Declarations whose DAE model failed to load at register() -> fallback hook. */
    private static final Set<DoorDecl> DAE_REGISTER_FAILED = ConcurrentHashMap.newKeySet();
    /** Paths whose load already failed (anti-spam: do not retry every frame). */
    private static final Set<ResourceLocation> DAE_LOAD_FAILED = ConcurrentHashMap.newKeySet();

    // Fallback hook: node renderer cache (direct SingleMeshVboRenderer path).
    private static final ConcurrentHashMap<String, SingleMeshVboRenderer> DAE_RENDERER_CACHE = new ConcurrentHashMap<>();

    // Transition seal: model + resolved paths for the debug overlay.
    private static final ResourceLocation SEAL_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/doors/transition_seal");
    private static final ResourceLocation SEAL_TEX =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/transition_seal");
    private static volatile DaeModel sealModel;
    private static volatile boolean sealModelFailed;
    private static ResourceLocation sealResolvedModelFile;
    private static ResourceLocation sealResolvedTexture;

    // Scratch for doPartTransform (rendering is single-threaded, called once per frame per part
    // thanks to MachineBer's anim cache).
    private static final float[] translation = new float[3];
    private static final float[] origin = new float[3];
    private static final float[] rotation = new float[3];

    private MachineDoorRenderer() {}

    // ==================== REGISTRATION ====================

    public static void register() {
        Set<String> allParts = collectAllPartNames();

        registerDoorSpec("door", ModBlockEntities.DOOR_ENTITY.get(), allParts);
        // Separate BE type for blast door (hbm_m:blast_door) - BlastDoorBlockEntity class
        // persists across reloads under its own ID, so a spec is needed for it as well.
        registerDoorSpec("blast_door", ModBlockEntities.BLAST_DOOR_ENTITY.get(), allParts);

        MachineSpecBuilder<TransitionSealBlockEntity> seal =
                MachineRenderers.machine("transition_seal", ModBlockEntities.TRANSITION_SEAL_BE.get(),
                                TransitionSealBlockEntity.class)
                        .facing(be -> be.getBlockState().getValue(TransitionSealBlock.FACING))
                        .blockTransform(MachineDoorRenderer::sealBlockTransform);
        registerSealDaeParts(seal);
        seal.hook(MachineDoorRenderer::renderSealDaeFallbackHook)
                .register();
    }

    /**
     * The same set of door parts is registered to both door BE types
     * (base DOOR_ENTITY and BLAST_DOOR_ENTITY). The VBO cache already accounts for
     * door type and skin in the key (doorPartCacheKey), avoiding conflicts between specs.
     */
    private static void registerDoorSpec(String id, BlockEntityType<? extends DoorBlockEntity> type, Set<String> allParts) {
        MachineSpecBuilder<DoorBlockEntity> door =
                MachineRenderers.machine(id, (BlockEntityType<DoorBlockEntity>) (BlockEntityType<?>) type, DoorBlockEntity.class)
                        .model(MachineDoorRenderer::resolveModel)
                        .facing(DoorBlockEntity::getFacing);
        for (String part : allParts) {
            final boolean child = isChildPart(part);
            final String partF = part;
            // IMPORTANT: door OBJ parts are declared as dynamicPart, NOT staticPart --
            // the spec's VBO cache key must include the door type and skin (the same part
            // name "frame"/"door" on different doors = DIFFERENT geometry; a cache keyed
            // by part name would hand every door the mesh of the first one drawn).
            // Per-part CPU clipping (door "pocket" clipping). IMPORTANT: call BEFORE declaring part -
            // builder captures resolver eagerly during dynamicPart; reverse order leaves
            // resolver null making clipping dead code. DAE parts below have NO resolver
            // (matching RenderDoorGeneric).
            if (!isStaticFramePart(part)) {
                door.clipPlanes(part, (be, partialTick) ->
                        doorPartClipPlanes(be, partialTick, partF, child));
            }
            door.dynamicPart(part,
                    be -> resolveDoorPartQuads(be, part),
                    MachineDoorRenderer::doorPartCacheKey,
                    (be, partialTick, gameTime, pose) ->
                            applyDoorPartTransform(be, partialTick, pose, part, child),
                    be -> doorUvRect(be, part));
        }
        // Door DAE nodes: one part per node, instancing/MDI shared with the machines.
        registerDoorDaeParts(door);
        door.hook(MachineDoorRenderer::renderDoorDaeFallbackHook)
                .register();
    }

    /**
     * Union of door parts. SOURCE OF TRUTH -- the "parts" names in the JSON models
     * doors/*_modern.json (the old DoorRenderer iterated model.getPartNames(), NOT
     * DoorDecl.getPartNames(): for example, 9 models name the leaves doorLeft/doorRight,
     * QE uses leftDoor/rightDoor, and the water door has a bolt -- none of these names
     * exist in DoorDecl.getPartNames()). Plus the closure of getChildren() from
     * DoorDecl (water door: door -> spinny_*). A part missing from a specific
     * model yields an empty quad list and is skipped -- a superset is safe.
     * When adding a new door model with a new part -- extend this list.
     */
    private static final String[] EXTRA_JSON_PARTS = {
            "doorLeft", "doorRight",          // large_vehicle_door, round_airlock_door
            "leftDoor", "rightDoor",          // qe_sliding_door
            "DoorTop", "DoorBot",             // cargo_door
            "Hatch",                          // silo_hatch, silo_hatch_large (hatch leaf)
            "base",                           // secure_access_door (the frame is called base, not frame!)
            "Door", "Label",                  // vault_door
            "decal",                          // qe_containment_door
            "bolt",                           // water_door
    };

    private static Set<String> collectAllPartNames() {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (DoorDecl decl : DOOR_DECLS) {
            for (String name : decl.getPartNames()) {
                parts.add(name);
                collectChildren(decl, name, parts);
            }
        }
        for (String name : EXTRA_JSON_PARTS) {
            parts.add(name);
        }
        return parts;
    }

    private static void collectChildren(DoorDecl decl, String partName, Set<String> out) {
        for (String c : decl.getChildren(partName)) {
            if (out.add(c)) {
                collectChildren(decl, c, out);
            }
        }
    }

    private static boolean isChildPart(String partName) {
        // The only hierarchy in DoorDecl: "door" -> spinny_lower/spinny_upper
        return partName.startsWith("spinny");
    }

    // ==================== DAE: SPEC PARTS ====================

    private static void registerDoorDaeParts(MachineSpecBuilder<DoorBlockEntity> door) {
        for (DoorDecl decl : DOOR_DECLS) {
            ResourceLocation daePath = decl.getColladaAnimationSource();
            if (daePath == null) continue;
            DaeModel model = loadDaeModel(daePath);
            if (model == null) {
                DAE_REGISTER_FAILED.add(decl);
                continue;
            }
            for (DaeNodePath np : collectNodePaths(model.sceneRoots)) {
                final DoorDecl declF = decl;
                final DaeModel modelF = model;
                final DaeNodePath npF = np;
                // Per-part CPU clipping of DAE nodes. IMPORTANT: call BEFORE declaring part - builder
                // captures resolver eagerly during dynamicPart. Without resolver, DAE skins
                // did not hide door leaves in "pockets" (RenderDoorGeneric parity was violated).
                // Resolver is DAE counterpart to doorPartClipPlanes: same pose-space convention
                // of DoorDecl, inverting DAE animator delta (applyDaeRootOffset + localMatrix chain)
                // instead of doPartTransform.
                door.clipPlanes("dae/" + np.name(), (be, partialTick) ->
                        doorDaeClipPlanes(be, partialTick, declF, modelF, npF.chain(), npF.name()));
                door.dynamicPart(
                        "dae/" + np.name(),
                        be -> resolveDaeNodeQuads(be, declF, modelF, npF.node()),
                        be -> daeCacheKey(be, declF),
                        (be, partialTick, gameTime, pose) ->
                                animateDaeNode(be, partialTick, pose, declF, modelF, npF.chain()),
                        be -> daeUvRect(be, declF, npF.node()));
            }
        }
    }

    private static void registerSealDaeParts(MachineSpecBuilder<TransitionSealBlockEntity> seal) {
        DaeModel model = loadDaeModel(SEAL_MODEL_ID);
        if (model == null) {
            sealModelFailed = true;
            MainRegistry.LOGGER.error("MachineDoorRenderer: failed to load seal DAE model at register");
            return;
        }
        sealModel = model;
        sealResolvedModelFile = ResourceLocation.fromNamespaceAndPath(
                model.resource.getNamespace(), model.resource.getPath() + ".dae");
        sealResolvedTexture = findFirstTexture(model.sceneRoots);
        if (sealResolvedTexture == null && !model.textures.isEmpty()) {
            sealResolvedTexture = model.textures.values().iterator().next();
        }
        if (sealResolvedTexture == null) {
            sealResolvedTexture = SEAL_TEX;
        }
        for (DaeNodePath np : collectNodePaths(model.sceneRoots)) {
            final DaeModel modelF = model;
            final DaeNodePath npF = np;
            seal.dynamicPart(
                    "dae/" + np.name(),
                    be -> resolveSealNodeQuads(modelF, npF.node()),
                    be -> "seal",
                    (be, partialTick, gameTime, pose) ->
                            animateSealNode(be, partialTick, pose, modelF, npF.chain()));
        }
    }

    /** Loads a DAE with caching; null on error (the error is remembered, no retries). */
    private static DaeModel loadDaeModel(ResourceLocation path) {
        if (DAE_LOAD_FAILED.contains(path)) return null;
        return DAE_MODELS.computeIfAbsent(path, p -> {
            try {
                return DaeModel.load(p);
            } catch (Exception e) {
                DAE_LOAD_FAILED.add(p);
                MainRegistry.LOGGER.error("MachineDoorRenderer: failed to load DAE model {}", p, e);
                return null;
            }
        });
    }

    /** DFS over the scene: all nodes with geometry + the parent chain for localMatrix. */
    private static List<DaeNodePath> collectNodePaths(List<DaeNode> roots) {
        List<DaeNodePath> out = new ArrayList<>();
        Set<String> usedNames = ConcurrentHashMap.newKeySet();
        collectNodePaths(roots, new ArrayList<>(), out, usedNames);
        return out;
    }

    private static void collectNodePaths(List<DaeNode> nodes, List<DaeNode> parentChain,
                                         List<DaeNodePath> out, Set<String> usedNames) {
        for (DaeNode node : nodes) {
            parentChain.add(node);
            if (node.mesh != null) {
                String name = node.name;
                if (!usedNames.add(name)) {
                    name = name + "_" + usedNames.size();
                    usedNames.add(name);
                }
                out.add(new DaeNodePath(name, node, new ArrayList<>(parentChain)));
            }
            collectNodePaths(node.children, parentChain, out, usedNames);
            parentChain.remove(parentChain.size() - 1);
        }
    }

    // ==================== DOOR: MODEL AND TRANSFORMS ====================

    private static BakedModel resolveModel(DoorBlockEntity be) {
        DoorDecl doorDecl = be.getDoorDecl();
        if (doorDecl == null) return blockstateModel(be);
        String doorType = getDoorTypeKey(doorDecl);
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        if (!registry.isRegistered(doorType)) return blockstateModel(be);

        DoorModelSelection selection = be.getModelSelection();
        ResourceLocation modelPath = registry.getModelPath(doorType, selection);
        if (modelPath == null) return blockstateModel(be);

        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel selectionModel = com.hbm_m.platform.LoaderHooks.getModel(modelManager, modelPath);
        if (selectionModel == null || selectionModel == modelManager.getMissingModel()) {
            return blockstateModel(be);
        }
        return selectionModel;
    }

    private static BakedModel blockstateModel(DoorBlockEntity be) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
    }

    /** A DAE skin is active for this BE: the model is NOT OBJ-multipart (it is a DAE-baked model). */
    private static boolean isDaeModelActive(DoorBlockEntity be) {
        return !(resolveModel(be) instanceof DoorBakedModel);
    }

    /** DAE node VBO cache key. When sharing -- the door without a skin (nodes share the normalized VBO).
     *  shareEpoch is mixed in: a share-state flip changes the VBO's UV space (normalized vs atlas),
     *  stale entries of the previous mode must not be reused. */
    private static String daeCacheKey(DoorBlockEntity be, DoorDecl decl) {
        if (!isDaeModelActive(be)) return "off";
        String epoch = "@e" + shareEpoch();
        if (shareSkins()) return decl.getBlockId().getPath() + epoch;
        return decl.getBlockId().getPath() + ":" + be.getModelSelection().getSkin().getId() + epoch;
    }

    /** DAE node uvRect resolver (the skin texture's sprite) or null outside sharing. */
    private static float[] daeUvRect(DoorBlockEntity be, DoorDecl decl, DaeNode node) {
        if (!shareSkins() || !isDaeModelActive(be)) return null;
        TextureAtlasSprite s = daeNodeSprite(node, enrichedSelection(be), decl);
        if (s == null) return null;
        return cachedUvRect(s);
    }

    /**
     * Enriched (texturePath from the registry) skin selection: NBT sync
     * reconstructs DoorSkin by id only, so resolveDaeTexture cannot find the texture
     * for variant1/variant2. Does not change modelType, only enriches the skin.
     */
    private static DoorModelSelection enrichedSelection(DoorBlockEntity be) {
        DoorModelSelection selection = be.getModelSelection();
        DoorSkin fullSkin = selection.getSkin();
        if (fullSkin.getTexturePath() == null && !fullSkin.isDefault()) {
            DoorModelRegistry registry = DoorModelRegistry.getInstance();
            DoorSkin resolved = registry.getSkin(be.getDoorDecl().getBlockId().getPath(), fullSkin.getId());
            if (resolved != null && resolved.getTexturePath() != null) {
                return new DoorModelSelection(selection.getModelType(), resolved);
            }
        }
        return selection;
    }

    private static String getDoorTypeKey(DoorDecl doorDecl) {
        if (doorDecl == DoorDecl.LARGE_VEHICLE_DOOR) return "large_vehicle_door";
        if (doorDecl == DoorDecl.ROUND_AIRLOCK_DOOR) return "round_airlock_door";
        if (doorDecl == DoorDecl.FIRE_DOOR) return "fire_door";
        if (doorDecl == DoorDecl.SLIDING_BLAST_DOOR) return "sliding_blast_door";
        if (doorDecl == DoorDecl.SLIDING_SEAL_DOOR) return "sliding_seal_door";
        if (doorDecl == DoorDecl.SECURE_ACCESS_DOOR) return "secure_access_door";
        if (doorDecl == DoorDecl.QE_SLIDING) return "qe_sliding_door";
        if (doorDecl == DoorDecl.QE_CONTAINMENT) return "qe_containment_door";
        if (doorDecl == DoorDecl.WATER_DOOR) return "water_door";
        if (doorDecl == DoorDecl.SILO_HATCH) return "silo_hatch";
        if (doorDecl == DoorDecl.SILO_HATCH_LARGE) return "silo_hatch_large";
        if (doorDecl == DoorDecl.VAULT_DOOR) return "vault_door";
        if (doorDecl == DoorDecl.CARGO_DOOR) return "cargo_door";
        if (doorDecl == BlastDoorDecl.INSTANCE) return "blast_door";
        throw new IllegalStateException("Unknown door type: " + doorDecl.getClass().getName());
    }

    /** Open progress 0..openTime accounting for contraptions (ContraptionDoorAnimCache.chase). */
    private static float openTicks(DoorBlockEntity be, float partialTick, DoorDecl doorDecl) {
        if (ContraptionRenderCompat.isContraptionRender(be)) {
            // Create VirtualRenderWorld (not ClientLevel): the BE is recreated from frozen
            // NBT and does not tick -- the animation is driven by the chase cache, fed by
            // Contraption packets. The Sable sublevel is a REAL client BE in the main
            // ClientLevel (plot-grid): it ticks and syncs itself, and nobody fills the
            // chase cache for it -> the door would stay "closed" forever. Hence chase
            // is for virtual worlds only.
            if (ContraptionRenderCompat.isContraptionRenderLevel(be.getLevel())) {
                float progress = ContraptionDoorAnimCache.chase(be, doorDecl.getOpenTime());
                return progress * doorDecl.getOpenTime();
            }
        }
        return be.getOpenProgress(partialTick) * doorDecl.getOpenTime();
    }

    /**
     * OBJ part VBO cache key.
     * Sharing (shareSkins): door + skin geometry token -- texture skins of the same
     * geometry share a VBO (its UVs are normalized, the skin goes into a per-instance uvRect).
     * Token = the canonical instance of the model's first part: skins with DIFFERENT geometry
     * (large_vehicle_door: base-obj vs _clean.obj) get different tokens -- sharing will not kick in.
     * Without sharing (Iris / kill-switch): the old door+model+skin key.
     */
    private static String doorPartCacheKey(DoorBlockEntity be) {
        DoorDecl decl = be.getDoorDecl();
        String doorType = decl == null ? "null" : getDoorTypeKey(decl);
        // shareEpoch is mixed into BOTH branches: a share-state flip changes the VBO's
        // UV space (normalized sprite-local vs atlas) -- stale entries of the previous
        // mode must never be handed out (the old +-2.3 tile-uv desync).
        String epoch = "@e" + shareEpoch();
        if (!shareSkins()) {
            DoorModelSelection selection = be.getModelSelection();
            return doorType + "_" + selection.getModelType().getId() + "_" + selection.getSkin().getId() + epoch;
        }
        if (resolveModel(be) instanceof DoorBakedModel dm) {
            return be.cachedModelKey(doorType) + "@" + System.identityHashCode(dm.geometryToken()) + epoch;
        }
        return be.cachedModelKey(doorType) + epoch;
    }

    /** Part sprite: for a skin wrapper -- the target sprite, for canonical -- the first quad's sprite (cached). */
    private static final ConcurrentHashMap<BakedModel, TextureAtlasSprite> PART_SPRITES = new ConcurrentHashMap<>();

    /** Clears the canonical part sprite cache (resource reload -- old models go stale). */
    public static void clearPartSpriteCache() {
        PART_SPRITES.clear();
        UV_RECT_COPIES.clear();
    }

    private static TextureAtlasSprite partSprite(BakedModel part) {
        if (part instanceof com.hbm_m.client.loader.RemappedPartModel r) {
            return r.toSprite();
        }
        TextureAtlasSprite cached = PART_SPRITES.get(part);
        if (cached != null || PART_SPRITES.containsKey(part)) {
            return cached;
        }
        // canonical part: UVs are baked by its own sprite -- take it from the first quad.
        TextureAtlasSprite found = null;
        var rand = RandomSource.create(42L);
        for (Direction d : Direction.values()) {
            var q = RenderHooks.getPartQuads(part, null, d, rand);
            if (!q.isEmpty()) {
                found = q.get(0).getSprite();
                break;
            }
        }
        if (found == null) {
            var q = RenderHooks.getPartQuads(part, null, null, RandomSource.create(42L));
            found = q.isEmpty() ? null : q.get(0).getSprite();
        }
        if (found != null) {
            PART_SPRITES.put(part, found);
        }
        return found;
    }

    private static final float[] UV_RECT_SCRATCH = new float[4];

    /**
     * OBJ part uvRect resolver: {u0,v0,du,dv} of the skin's target sprite for remapping
     * the normalized VBO into the atlas. Called every frame -- stays on map lookups.
     * <p>
     * KNOWN LIMITATION (out of scope, canonical design rev.2 item 7): the clip-remap is
     * PART-level -- this resolver (like daeUvRect) returns the rect of the FIRST sprite
     * of the part; a multi-sprite part in the clip tier would be remapped by one rect.
     * Not a regression: the non-clip paths are built the same way (InstUvRect is one
     * rect per part). The return value is the shared UV_RECT_SCRATCH -- every consumer
     * MUST snapshot the values in the same frame (the DoorPartClipper memo key does).
     */
    /** Per-part uvRect copies per sprite (avoids string allocations and skin collisions). */
    private static final java.util.concurrent.ConcurrentHashMap<TextureAtlasSprite, float[]> UV_RECT_COPIES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Snapshot of atlas sprite rect into reusable per-part array. */
    private static float[] cachedUvRect(TextureAtlasSprite s) {
        return UV_RECT_COPIES.computeIfAbsent(s, spr -> new float[] {
                spr.getU0(),
                spr.getV0(),
                spr.getU1() - spr.getU0(),
                spr.getV1() - spr.getV0()
        });
    }
    private static float[] doorUvRect(DoorBlockEntity be, String partName) {
        if (!shareSkins()) return null;
        if (!(resolveModel(be) instanceof DoorBakedModel dm)) return null;
        BakedModel part = dm.getPart(partName);
        if (part == null) return null;
        TextureAtlasSprite s = partSprite(part);
        if (s == null) return null;
        // Per-part COPY of rect (not shared UV_RECT_SCRATCH): rect lives until
        // instance batch flush; shared scratch assigned the last part's rect to all parts -
        // corrupting door textures in share mode.
        return cachedUvRect(s);
    }

    /** OBJ part quads from DoorBakedModel; empty if the part is missing / the skin is not OBJ.
     *  When sharing -- normalized (sprite-local [0..1]) UVs of the canonical geometry. */
    private static List<BakedQuad> resolveDoorPartQuads(DoorBlockEntity be, String partName) {
        BakedModel model = resolveModel(be);
        if (!(model instanceof DoorBakedModel doorModel)) return List.of();
        BakedModel partModel = doorModel.getPart(partName);
        if (partModel == null) return List.of();

        boolean share = shareSkins();
        if (share && partModel instanceof com.hbm_m.client.loader.RemappedPartModel remapped) {
            // Canonical geometry (shared by all texture skins) instead of the
            // remap into this skin's sprite.
            partModel = remapped.canonical();
        }

        // Exact copy of PartGeometry.collectSolidQuads: a fresh seed per call.
        List<BakedQuad> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            RandomSource rand = RandomSource.create(42L);
            out.addAll(RenderHooks.getPartQuads(partModel, null, d, rand));
        }
        out.addAll(RenderHooks.getPartQuads(partModel, null, null, RandomSource.create(42L)));

        if (isStaticFramePart(partName) && com.hbm_m.config.ModClothConfig.get().doorPartCulling && DOOR_PART_CULLING_FLAG) {
            DoorDecl decl = be.getDoorDecl();
            if (decl != null) {
                double[][] planes = decl.getClippingPlanes(partName, 0f, be.getModelSelection());
                if (planes != null && planes.length > 0) {
                    float[][] fPlanes = new float[planes.length][4];
                    for (int pIdx = 0; pIdx < planes.length; pIdx++) {
                        fPlanes[pIdx][0] = (float) planes[pIdx][0];
                        fPlanes[pIdx][1] = (float) planes[pIdx][1];
                        fPlanes[pIdx][2] = (float) planes[pIdx][2];
                        fPlanes[pIdx][3] = (float) planes[pIdx][3];
                    }
                    // One-time bake-time clipping of static frame before VBO compilation
                    out = DoorPartClipper.clipQuads(out, fPlanes, null);
                }
            }
        }

        if (share) {
            // Composition tile-split -> frac -> sprite normalization. At this
            // call site input is atlas-mapped (DoorBakedModel/RemappedPartModel quads),
            // split phase is no-op, equivalent to normalizeQuadUvsPerQuad.
            out = com.hbm_m.client.model.ModelHelper.normalizeQuadUvsPerQuad(out);
        }
        return out;
    }

    private static boolean applyDoorPartTransform(DoorBlockEntity be, float partialTick,
                                                  PoseStack pose, String partName, boolean child) {
        if (!be.isController() && !ContraptionRenderCompat.isContraptionRender(be)) return false;
        DoorDecl doorDecl = be.getDoorDecl();
        if (doorDecl == null) return false;
        DoorModelSelection selection = be.getModelSelection();
        float open = openTicks(be, partialTick, doorDecl);
        // Part culling on opening (be-aware doesRender: blast door leaf segments
        // vanish into housing). Toggle doorPartCulling disables per-frame hiding
        // across all doors; static rules (water_door: hide spinny valves via
        // DoorDecl.doesRender(partName, child)) always apply for permanent decorative parts.
        if (com.hbm_m.config.ModClothConfig.get().doorPartCulling && DOOR_PART_CULLING_FLAG
                && !doorDecl.doesRender(be, partName, open, child)) return false;
        if (!doorDecl.doesRender(partName, child)) return false;

        // The frame (staticFrame) was drawn in block pose WITHOUT the DoorDecl transform.
        if (isStaticFramePart(partName)) return true;

        if (child) {
            // Hierarchy: the child part lives in the parent's pose (the old pushPose traversal).
            if (!doorDecl.doesRender("door", false)) return false;
            doPartTransform(pose, doorDecl, "door", open, false, selection);
        }
        doPartTransform(pose, doorDecl, partName, open, child, selection);
        return true;
    }

    /** Frame: in the old DoorRenderer it was pulled out of the animated traversal and not transformed. */
    private static boolean isStaticFramePart(String partName) {
        return "frame".equalsIgnoreCase(partName)
                || "base".equalsIgnoreCase(partName)
                || "DoorFrame".equals(partName);
    }

    // ==================== DOOR: PER-PART CLIP PLANES ====================

    /** Scratch stack for inverting pose delta of doPartTransform (rendering is single-threaded). */
    private static final PoseStack CLIP_POSE = new PoseStack();
    /** Reusable result scratch (door parts have at most 2 planes). */
    private static final float[][] CLIP_SCRATCH = new float[2][4];

    /**
     * Resolver of per-part clip planes for door OBJ parts (planes {A,B,C,D} in
     * DoorDecl "+D" convention - inside is {@code n·x + D >= 0}). Contract is null
     * if: (1) toggle is disabled ({@code ModClothConfig.doorPartCulling &&
     * DOOR_PART_CULLING_FLAG} - single gate point: OFF = entire geometry,
     * including blast door); (2) {@code resolveModel(be)} is not DoorBakedModel (DAE skins
     * without clip, matching RenderDoorGeneric); (3) DoorDecl plane array is empty (otherwise
     * frames of all 14 doors would degrade to immediate tier).
     * <p>
     * Otherwise: {@code decl.getClippingPlanes(partName, open, selection)} in pose space, then
     * inverts pose delta of doPartTransform ({@code p = R·v + c}, {@code c = R·t + (I-R)·o})
     * and converts to "+D" convention: {@code {R^T·n, n·c + D}}. For pure translation -
     * {@code d_local = d - n·t}.
     * <p>
     * Child parts require compositing the parent's delta ("door") - currently dead
     * (spinny_* have no planes), returning null for them.
     */
    private static float[][] doorPartClipPlanes(DoorBlockEntity be, float partialTick,
                                                String partName, boolean child) {
        boolean cfg = com.hbm_m.config.ModClothConfig.get().doorPartCulling && DOOR_PART_CULLING_FLAG;
        boolean isDbm = resolveModel(be) instanceof DoorBakedModel;
        DoorDecl doorDecl = be.getDoorDecl();
        if (!cfg || child || !isDbm || doorDecl == null) {
            debugClipLog("obj-gate/" + partName, () ->
                    "cfg=" + cfg + " child=" + child + " isDoorBakedModel=" + isDbm + " decl=" + doorDecl);
            return null;
        }
        DoorModelSelection selection = be.getModelSelection();
        float open = openTicks(be, partialTick, doorDecl);
        double[][] planes = doorDecl.getClippingPlanes(partName, open, selection);
        debugClipLog("obj-resolve/" + partName, () ->
                "decl=" + doorDecl.getBlockId() + " sel=" + selection + " open=" + open
                + " planes=" + (planes == null ? "null" : java.util.Arrays.deepToString(planes)));
        if (planes == null || planes.length == 0) return null;

        // Pose delta of doPartTransform reproduced in scratch stack:
        // pose = T(o)·R·T(-o+t), R = Rx·Ry·Rz. c = pose·(0,0,0) = R·t + (I-R)·o.
        CLIP_POSE.pushPose();
        try {
            doPartTransform(CLIP_POSE, doorDecl, partName, open, false, selection);
            return invertPosePlanes(planes);
        } finally {
            CLIP_POSE.popPose();
        }
    }

    /**
     * Shared inversion of pose delta (called with pose assembled in {@link #CLIP_POSE}):
     * converts planes from pose space to local geometry space of the part quad.
     * p = R^T·(p' - c) => {n, D}_local = {R^T·n, n·c + D} ("+D" convention preserved;
     * inverse operation to resolver - decls return planes in pose space).
     */
    private static float[][] invertPosePlanes(double[][] planes) {
        org.joml.Matrix4f m = CLIP_POSE.last().pose();
        float m00 = m.m00(), m01 = m.m01(), m02 = m.m02();
        float m10 = m.m10(), m11 = m.m11(), m12 = m.m12();
        float m20 = m.m20(), m21 = m.m21(), m22 = m.m22();
        float cx = m.m30(), cy = m.m31(), cz = m.m32();

        float[][] out = planes.length <= CLIP_SCRATCH.length ? CLIP_SCRATCH
                : new float[planes.length][];
        for (int i = 0; i < planes.length; i++) {
            double[] p = planes[i];
            float nx = (float) p[0], ny = (float) p[1], nz = (float) p[2];
            float lx = m00 * nx + m10 * ny + m20 * nz;
            float ly = m01 * nx + m11 * ny + m21 * nz;
            float lz = m02 * nx + m12 * ny + m22 * nz;
            float dLocal = (float) (nx * cx + ny * cy + nz * cz + p[3]);
            float[] row = out[i] != null && out[i].length == 4 ? out[i] : new float[4];
            row[0] = lx; row[1] = ly; row[2] = lz; row[3] = dLocal;
            out[i] = row;
        }
        return out;
    }

    /**
     * Resolver of per-part clip planes for DAE parts (skin dynamicPart "dae/&lt;node&gt;").
     * Counterpart of {@link #doorPartClipPlanes} for DAE pipeline: gate is inverted (only
     * when DAE model is active), and pose matches node animator composition
     * ({@code applyDaeRootOffset} + localMatrix chain from {@code mulPoseChain} with same
     * time). Planes are retrieved from decl hook {@link DoorDecl#getDaeClippingPlanes} -
     * NOT from {@code getClippingPlanes(partName, ...)}, which matches doPartTransform names
     * ("LeftLock") rather than "dae/LeftLock". The hook returns planes in LOCAL space of the
     * .dae node; inversion to rest geometry local space uses applyDaeRootOffset + mulPoseChain +
     * invertPosePlanes. Log "dae-invert" restores observability of inverted planes in debug dumps.
     */
    private static float[][] doorDaeClipPlanes(DoorBlockEntity be, float partialTick, DoorDecl decl,
                                               DaeModel model, List<DaeNode> chain, String partName) {
        boolean cfg = com.hbm_m.config.ModClothConfig.get().doorPartCulling && DOOR_PART_CULLING_FLAG;
        boolean daeActive = isDaeModelActive(be);
        float open = openTicks(be, partialTick, decl);
        float time = open / 20.0f;
        DaeNode node = chain.get(chain.size() - 1);
        // Strip "dae/" prefix: decl hooks operate on .dae node names.
        String nodeName = partName.startsWith("dae/") ? partName.substring(4) : partName;
        double[][] planes = (cfg && daeActive)
                ? decl.getDaeClippingPlanes(nodeName, node, time) : null;
        debugClipLog("dae-resolve/" + partName, () ->
                "cfg=" + cfg + " daeActive=" + daeActive + " decl=" + decl.getBlockId()
                + " node=" + nodeName + " open=" + open
                + " planes=" + (planes == null ? "null" : java.util.Arrays.deepToString(planes)));
        if (planes == null || planes.length == 0) return null;

        CLIP_POSE.pushPose();
        try {
            // Same pose as node animator (animateDaeNode), with same time.
            applyDaeRootOffset(CLIP_POSE, decl);
            mulPoseChain(CLIP_POSE, model, chain, time);
            float[][] inverted = invertPosePlanes(planes);
            if (debugDoorClip()) {
                final org.joml.Vector3f dbgC = CLIP_POSE.last().pose().transformPosition(new org.joml.Vector3f());
                final org.joml.Matrix3f dbgM = new org.joml.Matrix3f(CLIP_POSE.last().pose());
                debugClipLog("dae-invert/" + partName, () ->
                        "src=" + java.util.Arrays.deepToString(planes)
                        + " inverted=" + java.util.Arrays.deepToString(inverted)
                        + " poseC=" + dbgC + " poseCols=" + dbgM.getColumn(0, new org.joml.Vector3f()) + "|" + dbgM.getColumn(1, new org.joml.Vector3f()) + "|" + dbgM.getColumn(2, new org.joml.Vector3f()));
            }
            return inverted;
        } finally {
            CLIP_POSE.popPose();
        }
    }

    private static void doPartTransform(PoseStack poseStack, DoorDecl doorDecl,
                                        String partName, float openTicks, boolean child,
                                        DoorModelSelection selection) {
        doorDecl.getOrigin(partName, origin, selection);
        doorDecl.getRotation(partName, openTicks, rotation, selection);

        poseStack.translate(origin[0], origin[1], origin[2]);
        if (rotation[0] != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0]));
        if (rotation[1] != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotation[1]));
        if (rotation[2] != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[2]));

        doorDecl.getTranslation(partName, openTicks, child, translation, selection);
        poseStack.translate(-origin[0] + translation[0], -origin[1] + translation[1], -origin[2] + translation[2]);
    }

    // ==================== DOOR: DAE ANIMATOR/RESOLVER ====================

    private static boolean animateDaeNode(DoorBlockEntity be, float partialTick, PoseStack pose,
                                          DoorDecl doorDecl, DaeModel model, List<DaeNode> chain) {
        if (!be.isController() && !ContraptionRenderCompat.isContraptionRender(be)) return false;
        if (!isDaeModelActive(be)) return false;

        // openTicks already carries the correct progress (0..openTime) accounting for the
        // contraption (ContraptionDoorAnimCache.chase) OR the regular BE.getOpenProgress.
        float time = openTicks(be, partialTick, doorDecl) / 20.0f;
        applyDaeRootOffset(pose, doorDecl);
        mulPoseChain(pose, model, chain, time);
        return true;
    }

    private static List<BakedQuad> resolveDaeNodeQuads(DoorBlockEntity be, DoorDecl doorDecl,
                                                       DaeModel model, DaeNode node) {
        if (!isDaeModelActive(be)) return List.of();
        DoorModelSelection selection = enrichedSelection(be);
        return bakeDaeNodeQuads(node, selection, doorDecl);
    }

    /** Offset rotations of the DAE frame from DoorDecl (getBakedModelRotationOffsetY). */
    private static void applyDaeRootOffset(PoseStack poseStack, DoorDecl doorDecl) {
        int offset = doorDecl.getBakedModelRotationOffsetY();
        if (offset == 0) return;
        poseStack.translate(0.5f, 0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(offset));
        poseStack.translate(-0.5f, 0f, -0.5f);

        // The DoorFrame's DAE geometry is stored with X in [-3.5,+3.5], Z in [-0.5,+0.5]
        // and pivot 0 = the controller block's corner, not its center. A -1 shift along
        // local-Z compensates "1 block toward the player" symmetrically for all facings.
        poseStack.translate(0f, 0f, 1f);
    }

    /** Composes the localMatrix chain root->node into the current pose. */
    private static void mulPoseChain(PoseStack poseStack, DaeModel model,
                                     List<DaeNode> chain, float time) {
        DaeAnimation clip = model.animations.get("animation");
        if (clip == null && !model.animations.isEmpty()) {
            clip = model.animations.values().iterator().next();
        }
        for (DaeNode node : chain) {
            RenderHooks.mulPoseMatrix(poseStack, node.localMatrix(time, clip));
        }
    }

    private static List<BakedQuad> bakeDaeNodeQuads(DaeNode node, DoorModelSelection selection,
                                                    DoorDecl doorDecl) {
        return bakeDaeNodeQuads(node, selection, doorDecl, shareSkins());
    }

    /** @param normalize normalize UVs (VBO sharing); the fallback hook draws directly -- false. */
    private static List<BakedQuad> bakeDaeNodeQuads(DaeNode node, DoorModelSelection selection,
                                                    DoorDecl doorDecl, boolean normalize) {
        try {
            TextureAtlasSprite sprite = daeNodeSprite(node, selection, doorDecl);
            if (sprite == null) {
                MainRegistry.LOGGER.error("MachineDoorRenderer: sprite not found in block atlas for DAE node '{}'", node.name);
                return List.of();
            }

            List<BakedQuad> quads = DaeQuadBaker.bakeNodeQuads(node.mesh, new Matrix4f(), sprite);
            if (quads == null) return List.of();
            // Skin sharing: UVs are normalized to sprite-local -- the sprite goes into
            // a per-instance uvRect, the node VBO is shared by all skins of the door.
            // Composition tile-split -> frac -> sprite normalization: input here
            // is atlas-mapped (DaeQuadBaker lerps raw UV into sprite rect), split phase is no-op.
            if (normalize) {
                quads = com.hbm_m.client.model.ModelHelper.normalizeQuadUvsPerQuad(quads);
            }
            return quads;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("MachineDoorRenderer: failed to bake DAE node '{}'", node.name, e);
            return List.of();
        }
    }

    private static TextureAtlasSprite daeNodeSprite(DaeNode node, DoorModelSelection selection,
                                                    DoorDecl doorDecl) {
        ResourceLocation rawTexture = resolveDaeTexture(node, selection, doorDecl);

        // Strip "textures/" and ".png" so the atlas can find the sprite
        String cleanPath = rawTexture.getPath();
        if (cleanPath.startsWith("textures/")) {
            cleanPath = cleanPath.substring("textures/".length());
        }
        if (cleanPath.endsWith(".png")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 4);
        }
        ResourceLocation spriteLocation =
                ResourceLocation.fromNamespaceAndPath(rawTexture.getNamespace(), cleanPath);

        return Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(spriteLocation);
    }

    private static ResourceLocation resolveDaeTexture(DaeNode node, DoorModelSelection selection,
                                                      DoorDecl doorDecl) {
        DoorSkin skin = selection.getSkin();
        ResourceLocation tex = skin.getTextureForPart(node.name);
        if (tex != null && !tex.equals(skin.getTexturePath())) return tex;
        if (skin.getTexturePath() != null) return skin.getTexturePath();

        // Skin without texturePath: for LEGACY -- the "old" texture, for MODERN default --
        // the regular one. This fallback takes precedence over node.texture: .dae files often
        // carry a broken <init_from>door0.png</init_from> reference (Blender export).
        String basePath = doorDecl.getBlockId().getPath();
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID,
                "block/doors/" + basePath + (selection.isLegacy() ? "_old" : ""));
    }

    // ==================== TRANSITION SEAL ====================

    private static void sealBlockTransform(TransitionSealBlockEntity be, LegacyAnimator animator) {
        Direction facing = be.getBlockState().getValue(TransitionSealBlock.FACING);
        animator.translate(0.5F, 0F, 0.5F);
        switch (facing) {
            case NORTH -> animator.rotate(90F, 0, 1, 0);
            case SOUTH -> animator.rotate(270F, 0, 1, 0);
            case WEST -> animator.rotate(180F, 0, 1, 0);
            default -> { }
        }
        animator.translate(0F, 0F, 0.5F);
    }

    private static boolean animateSealNode(TransitionSealBlockEntity be, float partialTick, PoseStack pose,
                                           DaeModel model, List<DaeNode> chain) {
        float time = be.getAnimationTime(partialTick);
        mulPoseChain(pose, model, chain, time);
        return true;
    }

    private static List<BakedQuad> resolveSealNodeQuads(DaeModel model, DaeNode node) {
        try {
            ResourceLocation texture = node.texture != null ? node.texture : SEAL_TEX;
            if (texture != null) {
                sealResolvedTexture = texture;
            }
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(texture);
            if (sprite == null) {
                return List.of();
            }
            List<BakedQuad> quads = DaeQuadBaker.bakeNodeQuads(node.mesh, new Matrix4f(), sprite);
            return quads != null ? quads : List.of();
        } catch (Exception e) {
            MainRegistry.LOGGER.error("MachineDoorRenderer: failed to bake seal node '{}'", node.name, e);
            return List.of();
        }
    }

    // ==================== DAE FALLBACK HOOKS ====================
    // Used ONLY if a DAE model failed to load at register() (normally all nodes go
    // through dynamicPart and the hooks sit idle). Direct SingleMeshVboRenderer
    // path without MDI.

    private static void renderDoorDaeFallbackHook(DoorBlockEntity be, float partialTick,
                                                  PoseStack poseStack, MultiBufferSource bufferSource,
                                                  int packedLight, int packedOverlay, MachineRenderApi api) {
        DoorDecl doorDecl = be.getDoorDecl();
        if (doorDecl == null || !DAE_REGISTER_FAILED.contains(doorDecl)) return;
        if (!be.isController() && !ContraptionRenderCompat.isContraptionRender(be)) return;
        if (resolveModel(be) instanceof DoorBakedModel) return;

        DaeModel dae = loadDaeModel(doorDecl.getColladaAnimationSource());
        if (dae == null) return;
        DaeAnimation clip = getClip(dae);

        float time = openTicks(be, partialTick, doorDecl) / 20.0f;
        DoorModelSelection selection = enrichedSelection(be);

        poseStack.pushPose();
        applyDaeRootOffset(poseStack, doorDecl);
        renderDaeNodesDirect(dae.sceneRoots, clip, time, poseStack, packedLight, be, bufferSource, selection, doorDecl);
        poseStack.popPose();
    }

    private static void renderSealDaeFallbackHook(TransitionSealBlockEntity be, float partialTick,
                                                  PoseStack poseStack, MultiBufferSource bufferSource,
                                                  int packedLight, int packedOverlay, MachineRenderApi api) {
        if (!sealModelFailed) return;
        DaeModel dae = loadDaeModel(SEAL_MODEL_ID);
        if (dae == null) return;
        DaeAnimation clip = getClip(dae);
        float time = be.getAnimationTime(partialTick);
        renderSealNodesDirect(dae.sceneRoots, clip, time, poseStack, packedLight, be, bufferSource);
    }

    private static DaeAnimation getClip(DaeModel model) {
        DaeAnimation clip = model.animations.get("animation");
        if (clip == null && !model.animations.isEmpty()) {
            clip = model.animations.values().iterator().next();
        }
        return clip;
    }

    private static void renderDaeNodesDirect(List<DaeNode> nodes, DaeAnimation clip, float time,
                                             PoseStack poseStack, int packedLight,
                                             DoorBlockEntity be, MultiBufferSource bufferSource,
                                             DoorModelSelection selection, DoorDecl doorDecl) {
        for (DaeNode node : nodes) {
            poseStack.pushPose();
            RenderHooks.mulPoseMatrix(poseStack, node.localMatrix(time, clip));
            if (node.mesh != null) {
                SingleMeshVboRenderer renderer = getDaeRendererForNodeDirect(node, selection, doorDecl);
                if (renderer != null) {
                    renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
                }
            }
            renderDaeNodesDirect(node.children, clip, time, poseStack, packedLight, be, bufferSource, selection, doorDecl);
            poseStack.popPose();
        }
    }

    private static SingleMeshVboRenderer getDaeRendererForNodeDirect(DaeNode node, DoorModelSelection selection,
                                                                     DoorDecl doorDecl) {
        String skinId = selection.getSkin().getId();
        String key = "dae_door:" + doorDecl.getBlockId().getPath() + ":" + node.name + ":" + skinId;

        return DAE_RENDERER_CACHE.computeIfAbsent(key, k -> {
            List<BakedQuad> quads = bakeDaeNodeQuads(node, selection, doorDecl);
            if (quads.isEmpty()) return null;
            return MeshRenderCache.getOrCreateRendererFromQuadList(key, quads);
        });
    }

    private static void renderSealNodesDirect(List<DaeNode> nodes, DaeAnimation clip, float time,
                                              PoseStack poseStack, int packedLight,
                                              TransitionSealBlockEntity be, MultiBufferSource bufferSource) {
        for (DaeNode node : nodes) {
            poseStack.pushPose();
            RenderHooks.mulPoseMatrix(poseStack, node.localMatrix(time, clip));
            if (node.mesh != null) {
                SingleMeshVboRenderer renderer = getSealRendererForNodeDirect(node);
                if (renderer != null) {
                    renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
                }
            }
            renderSealNodesDirect(node.children, clip, time, poseStack, packedLight, be, bufferSource);
            poseStack.popPose();
        }
    }

    private static SingleMeshVboRenderer getSealRendererForNodeDirect(DaeNode node) {
        String key = "transition_seal:" + node.name;
        return DAE_RENDERER_CACHE.computeIfAbsent(key, k -> {
            List<BakedQuad> quads = resolveSealNodeQuads(null, node);
            if (quads.isEmpty()) return null;
            return MeshRenderCache.getOrCreateRendererFromQuadList(key, quads);
        });
    }

    private static ResourceLocation findFirstTexture(List<DaeNode> nodes) {
        for (DaeNode node : nodes) {
            if (node.texture != null) {
                return node.texture;
            }
            ResourceLocation found = findFirstTexture(node.children);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Debug overlay (formerly TransitionSealRenderer.getDebugInfo). */
    public static String getSealDebugInfo() {
        StringBuilder sb = new StringBuilder("Transition seal debug\n");
        sb.append("Model file: ").append(sealResolvedModelFile != null ? sealResolvedModelFile : "<not loaded>");
        sb.append("\nTexture: ").append(sealResolvedTexture != null ? sealResolvedTexture : "<not loaded>");
        if (Minecraft.getInstance() != null && sealResolvedTexture != null) {
            try {
                TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                        .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .getSprite(sealResolvedTexture);
                sb.append("\nSprite present: ").append(sprite != null);
                if (sprite != null) {
                    var contents = sprite.contents();
                    sb.append("\nSprite size: ").append(contents.width()).append("x").append(contents.height());
                    sb.append("\nSprite UV: ").append(sprite.getU0()).append("..").append(sprite.getU1())
                            .append(" / ").append(sprite.getV0()).append("..").append(sprite.getV1());
                }
            } catch (Exception e) {
                sb.append("\nSprite lookup failed: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            }
        }
        if (sealModel != null) {
            sb.append("\nDAE resource: ").append(sealModel.resource);
        }
        if (sealModelFailed) {
            sb.append("\nStatus: load failed");
        }
        return sb.toString();
    }

    // ==================== CACHE INVALIDATION ====================

    public static void resetUvRectAttribCheck() {
        uvRectShaderMemo = null;
        uvRectShaderMemoVal = false;
        clearPartSpriteCache();
    }

    public static void clearDaeCaches() {
        resetUvRectAttribCheck();
        for (String key : DAE_RENDERER_CACHE.keySet()) {
            MeshRenderCache.removeRenderer(key);
        }
        DAE_RENDERER_CACHE.values().forEach(SingleMeshVboRenderer::cleanup);
        DAE_RENDERER_CACHE.clear();
        // DAE_MODELS is not cleared: the nodes are captured in the specs' PartDef lambdas, the
        // objects are reused (the DaeModel.allModels reload listener re-parses them in place).
        DAE_REGISTER_FAILED.clear();
        MainRegistry.LOGGER.debug("MachineDoorRenderer DAE caches cleared");
    }
}
