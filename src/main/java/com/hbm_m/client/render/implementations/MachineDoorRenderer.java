package com.hbm_m.client.render.implementations;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.machines.TransitionSealBlock;
import com.hbm_m.blockentity.ModBlockEntities;
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
 * Двери на фабрике {@link MachineRenderers} (замена DoorRenderer/DoorVboRenderer/
 * TransitionSealRenderer). Две спеки:
 * <ul>
 *   <li><b>door</b> — все 13 {@link DoorDecl} на одном BE-типе. Части — объединение
 *       getPartNames()+getChildren() всех деклараций; часть, отсутствующая в конкретной
 *       модели, скипается движком (partModel == null). Все части объявлены как
 *       staticPart(transform) — трансформ из DoorDecl считается аниматором, но части
 *       НЕ гейтятся анимационной дистанцией (двери — крупная статика).
 *       DAE-скины — по {@code dynamicPart} на ноду DAE-модели: квады резолвятся
 *       по скину BE, аниматор воспроизводит цепочку {@code localMatrix} от корня.
 *       Ноды уходят в общий инстансинг/MDI-пайплайн станков.</li>
 *   <li><b>transition_seal</b> — DAE-модель 26×24, те же dynamicPart-ноды, клип
 *       "animation" (24 c).</li>
 * </ul>
 * DAE-модели загружаются на register() (обычный XML из jar); если загрузка не
 * удалась, соответствующая дверь рендерится фолбэк-хуком с ленивой загрузкой.
 * Item-рендер дверей (скины, DAE item-модели) живёт в DoorBakedModel и фабрикой
 * не трогается (dynamicPart в deriveItemParts не попадают).
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
    };

    /** Нода DAE-сцены с её цепочкой родителей (для компоновки localMatrix в аниматоре). */
    private record DaeNodePath(String name, DaeNode node, List<DaeNode> chain) {}

    /** Загруженные на register() DAE-модели (двери по decl, seal отдельно). */
    private static final ConcurrentHashMap<ResourceLocation, DaeModel> DAE_MODELS = new ConcurrentHashMap<>();
    /** Декларации, чья DAE-модель не загрузилась на register() → фолбэк-хук. */
    private static final Set<DoorDecl> DAE_REGISTER_FAILED = ConcurrentHashMap.newKeySet();
    /** Пути, чья загрузка уже ошиблась (анти-спам: не ретраим каждый кадр). */
    private static final Set<ResourceLocation> DAE_LOAD_FAILED = ConcurrentHashMap.newKeySet();

    // Фолбэк-хук: кэш рендереров нод (прямой SingleMeshVboRenderer-путь).
    private static final ConcurrentHashMap<String, SingleMeshVboRenderer> DAE_RENDERER_CACHE = new ConcurrentHashMap<>();

    // Transition seal: модель + разрешённые пути для debug-оверлея.
    private static final ResourceLocation SEAL_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/doors/transition_seal");
    private static final ResourceLocation SEAL_TEX =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/doors/transition_seal");
    private static volatile DaeModel sealModel;
    private static volatile boolean sealModelFailed;
    private static ResourceLocation sealResolvedModelFile;
    private static ResourceLocation sealResolvedTexture;

    // Scratch для doPartTransform (рендер однопоточный, вызов раз в кадр на часть
    // благодаря anim-кэшу MachineBer).
    private static final float[] translation = new float[3];
    private static final float[] origin = new float[3];
    private static final float[] rotation = new float[3];

    private MachineDoorRenderer() {}

    // ==================== РЕГИСТРАЦИЯ ====================

    public static void register() {
        Set<String> allParts = collectAllPartNames();

        MachineSpecBuilder<DoorBlockEntity> door =
                MachineRenderers.machine("door", ModBlockEntities.DOOR_ENTITY.get(), DoorBlockEntity.class)
                        .model(MachineDoorRenderer::resolveModel)
                        .facing(DoorBlockEntity::getFacing);
        for (String part : allParts) {
            final boolean child = isChildPart(part);
            // ВАЖНО: OBJ-части дверей объявлены как dynamicPart, НЕ staticPart —
            // ключ VBO-кэша спеки обязан включать тип двери и скин (одно имя части
            // "frame"/"door" у разных дверей = РАЗНАЯ геометрия; кэш по имени части
            // отдавал бы всем дверям меш первой отрисованной).
            door.dynamicPart(part,
                    be -> resolveDoorPartQuads(be, part),
                    MachineDoorRenderer::doorPartCacheKey,
                    (be, partialTick, gameTime, pose) ->
                            applyDoorPartTransform(be, partialTick, pose, part, child));
        }
        // DAE-ноды дверей: по части на ноду, инстансинг/MDI общий со станками.
        registerDoorDaeParts(door);
        door.hook(MachineDoorRenderer::renderDoorDaeFallbackHook)
                .register();

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
     * Объединение частей дверей. ИСТОЧНИК ИСТИНЫ — имена "parts" в JSON-моделях
     * doors/*_modern.json (старый DoorRenderer итерировал model.getPartNames(), а
     * НЕ DoorDecl.getPartNames(): например, у 9 моделей створки называются
     * doorLeft/doorRight, у QE — leftDoor/rightDoor, у water door есть bolt —
     * в DoorDecl.getPartNames() этих имён НЕТ). Плюс замыкание getChildren() из
     * DoorDecl (water door: door → spinny_*). Часть, которой нет в конкретной
     * модели, даёт пустой квад-лист и скипается — супермножество безопасно.
     * При добавлении новой дверной модели с новой частью — дополнить список.
     */
    private static final String[] EXTRA_JSON_PARTS = {
            "doorLeft", "doorRight",          // large_vehicle_door, round_airlock_door
            "leftDoor", "rightDoor",          // qe_sliding_door
            "DoorTop", "DoorBot",             // cargo_door
            "Hatch",                          // silo_hatch, silo_hatch_large (створка люка)
            "base",                           // secure_access_door (рама называется base, не frame!)
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
        // Единственная иерархия в DoorDecl: "door" -> spinny_lower/spinny_upper
        return partName.startsWith("spinny");
    }

    // ==================== DAE: ЧАСТИ СПЕКИ ====================

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
                door.dynamicPart(
                        "dae/" + np.name(),
                        be -> resolveDaeNodeQuads(be, declF, modelF, npF.node()),
                        be -> daeCacheKey(be, declF),
                        (be, partialTick, gameTime, pose) ->
                                animateDaeNode(be, partialTick, pose, declF, modelF, npF.chain()));
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

    /** Загрузка DAE c кэшем; null при ошибке (ошибка запоминается, не ретраим). */
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

    /** DFS по сцене: все ноды с геометрией + цепочка родителей для localMatrix. */
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

    // ==================== DOOR: МОДЕЛЬ И ТРАНСФОРМЫ ====================

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
        BakedModel selectionModel = com.hbm_m.platform.PlatformHooks.getModel(modelManager, modelPath);
        if (selectionModel == null || selectionModel == modelManager.getMissingModel()) {
            return blockstateModel(be);
        }
        return selectionModel;
    }

    private static BakedModel blockstateModel(DoorBlockEntity be) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
    }

    /** DAE-скин активен для этого BE: модель — НЕ OBJ-multipart (это DAE-baked модель). */
    private static boolean isDaeModelActive(DoorBlockEntity be) {
        return !(resolveModel(be) instanceof DoorBakedModel);
    }

    /** Ключ VBO-кэша DAE-ноды: дверь + скин (текстура зависит от скина). */
    private static String daeCacheKey(DoorBlockEntity be, DoorDecl decl) {
        if (!isDaeModelActive(be)) return "off";
        return decl.getBlockId().getPath() + ":" + be.getModelSelection().getSkin().getId();
    }

    /**
     * Обогащённый (texturePath из реестра) выбор скина: NBT-синхронизация
     * reconstructs DoorSkin только по id, из-за чего resolveDaeTexture не находит
     * текстуру для variant1/variant2. Не меняет modelType, только обогащает skin.
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
        throw new IllegalStateException("Unknown door type: " + doorDecl.getClass().getName());
    }

    /** Прогресс открытия 0..openTime с учётом контрапшена (ContraptionDoorAnimCache.chase). */
    private static float openTicks(DoorBlockEntity be, float partialTick, DoorDecl doorDecl) {
        if (ContraptionRenderCompat.isContraptionRender(be)) {
            // Create VirtualRenderWorld (не ClientLevel): BE пересоздаётся из замороженного
            // NBT и не тикает — анимацию ведёт chase-кеш, питаемый Contraption-пакетами.
            // Sable sublevel — это НАСТОЯЩИЙ клиентский BE в основном ClientLevel (plot-grid):
            // он тикает и синкается сам, а chase-кеш для него никто не заполняет → дверь
            // навсегда «закрыта». Поэтому chase — только для виртуальных миров.
            if (ContraptionRenderCompat.isContraptionRenderLevel(be.getLevel())) {
                float progress = ContraptionDoorAnimCache.chase(be, doorDecl.getOpenTime());
                return progress * doorDecl.getOpenTime();
            }
        }
        return be.getOpenProgress(partialTick) * doorDecl.getOpenTime();
    }

    /** Ключ VBO-кэша OBJ-части: тип двери + тип модели + скин (геометрия части уникальна на triple). */
    private static String doorPartCacheKey(DoorBlockEntity be) {
        DoorDecl decl = be.getDoorDecl();
        String doorType = decl == null ? "null" : getDoorTypeKey(decl);
        DoorModelSelection selection = be.getModelSelection();
        return doorType + "_" + selection.getModelType().getId() + "_" + selection.getSkin().getId();
    }

    /** Квады OBJ-части из DoorBakedModel; пусто, если части нет/скин не OBJ. */
    private static List<BakedQuad> resolveDoorPartQuads(DoorBlockEntity be, String partName) {
        BakedModel model = resolveModel(be);
        if (!(model instanceof DoorBakedModel doorModel)) return List.of();
        BakedModel partModel = doorModel.getPart(partName);
        if (partModel == null) return List.of();

        // Точная копия PartGeometry.collectSolidQuads: общий seed на каждый вызов.
        List<BakedQuad> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            RandomSource rand = RandomSource.create(42L);
            out.addAll(RenderHooks.getPartQuads(partModel, null, d, rand));
        }
        out.addAll(RenderHooks.getPartQuads(partModel, null, null, RandomSource.create(42L)));
        return out;
    }

    private static boolean applyDoorPartTransform(DoorBlockEntity be, float partialTick,
                                                  PoseStack pose, String partName, boolean child) {
        if (!be.isController() && !ContraptionRenderCompat.isContraptionRender(be)) return false;
        DoorDecl doorDecl = be.getDoorDecl();
        if (doorDecl == null) return false;
        if (!doorDecl.doesRender(partName, child)) return false;

        // Рама (staticFrame) рисовалась в блочной позе БЕЗ трансформа DoorDecl.
        if (isStaticFramePart(partName)) return true;

        DoorModelSelection selection = be.getModelSelection();
        float open = openTicks(be, partialTick, doorDecl);

        if (child) {
            // Иерархия: дочерняя часть живёт в позе родителя (старый pushPose-обход).
            if (!doorDecl.doesRender("door", false)) return false;
            doPartTransform(pose, doorDecl, "door", open, false, selection);
        }
        doPartTransform(pose, doorDecl, partName, open, child, selection);
        return true;
    }

    /** Рама: в старом DoorRenderer выносилась из анимированного обхода и не трансформировалась. */
    private static boolean isStaticFramePart(String partName) {
        return "frame".equalsIgnoreCase(partName)
                || "base".equalsIgnoreCase(partName)
                || "DoorFrame".equals(partName);
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

    // ==================== DOOR: DAE АНИМАТОР/РЕЗОЛВЕР ====================

    private static boolean animateDaeNode(DoorBlockEntity be, float partialTick, PoseStack pose,
                                          DoorDecl doorDecl, DaeModel model, List<DaeNode> chain) {
        if (!be.isController() && !ContraptionRenderCompat.isContraptionRender(be)) return false;
        if (!isDaeModelActive(be)) return false;

        // openTicks уже несёт корректный прогресс (0..openTime) с учётом контрапшена
        // (ContraptionDoorAnimCache.chase) ИЛИ обычного BE.getOpenProgress.
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

    /** Offset-повороты DAE-рамки из DoorDecl (getBakedModelRotationOffsetY). */
    private static void applyDaeRootOffset(PoseStack poseStack, DoorDecl doorDecl) {
        int offset = doorDecl.getBakedModelRotationOffsetY();
        if (offset == 0) return;
        poseStack.translate(0.5f, 0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(offset));
        poseStack.translate(-0.5f, 0f, -0.5f);

        // DAE-геометрия рамки DoorFrame хранится по X ∈ [-3.5,+3.5], Z ∈ [-0.5,+0.5]
        // с pivot 0 = corner блока контроллера, а не его центр. Сдвиг на -1 по
        // local-Z компенсирует "1 блок к игроку" симметрично для всех facing'ов.
        poseStack.translate(0f, 0f, 1f);
    }

    /** Композиция localMatrix цепочки root→node в текущий pose. */
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
        try {
            ResourceLocation rawTexture = resolveDaeTexture(node, selection, doorDecl);

            // Очищаем путь от "textures/" и ".png", чтобы атлас мог найти спрайт
            String cleanPath = rawTexture.getPath();
            if (cleanPath.startsWith("textures/")) {
                cleanPath = cleanPath.substring("textures/".length());
            }
            if (cleanPath.endsWith(".png")) {
                cleanPath = cleanPath.substring(0, cleanPath.length() - 4);
            }
            ResourceLocation spriteLocation =
                    ResourceLocation.fromNamespaceAndPath(rawTexture.getNamespace(), cleanPath);

            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(spriteLocation);

            if (sprite == null) {
                MainRegistry.LOGGER.error("MachineDoorRenderer: Sprite '{}' not found in block atlas!", spriteLocation);
                return List.of();
            }

            List<BakedQuad> quads = DaeQuadBaker.bakeNodeQuads(node.mesh, new Matrix4f(), sprite);
            return quads != null ? quads : List.of();
        } catch (Exception e) {
            MainRegistry.LOGGER.error("MachineDoorRenderer: failed to bake DAE node '{}'", node.name, e);
            return List.of();
        }
    }

    private static ResourceLocation resolveDaeTexture(DaeNode node, DoorModelSelection selection,
                                                      DoorDecl doorDecl) {
        DoorSkin skin = selection.getSkin();
        ResourceLocation tex = skin.getTextureForPart(node.name);
        if (tex != null && !tex.equals(skin.getTexturePath())) return tex;
        if (skin.getTexturePath() != null) return skin.getTexturePath();

        // Скин без texturePath: для LEGACY — «old» текстура, для MODERN default —
        // обычная. Этот fallback ВАЖНЕЕ node.texture: в .dae часто стоит битая
        // ссылка <init_from>door0.png</init_from> (Blender-экспорт).
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

    // ==================== DAE ФОЛБЭК-ХУКИ ====================
    // Используются ТОЛЬКО если DAE-модель не загрузилась на register() (в норме
    // все ноды идут через dynamicPart и хуки бездействуют). Прямой
    // SingleMeshVboRenderer-путь без MDI.

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

    /** Debug-оверлей (бывш. TransitionSealRenderer.getDebugInfo). */
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

    // ==================== ИНВАЛИДАЦИЯ КЭШЕЙ ====================

    public static void clearDaeCaches() {
        for (String key : DAE_RENDERER_CACHE.keySet()) {
            MeshRenderCache.removeRenderer(key);
        }
        DAE_RENDERER_CACHE.values().forEach(SingleMeshVboRenderer::cleanup);
        DAE_RENDERER_CACHE.clear();
        // DAE_MODELS не чистим: ноды захвачены в PartDef-лямбдах спек, объекты
        // переиспользуются (reload-листенер DaeModel.allModels перепарсит их на месте).
        DAE_REGISTER_FAILED.clear();
        MainRegistry.LOGGER.debug("MachineDoorRenderer DAE caches cleared");
    }
}
