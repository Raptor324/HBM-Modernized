package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.client.loader.dae.DaeAnimation;
import com.hbm_m.client.loader.dae.DaeModel;
import com.hbm_m.client.loader.dae.DaeNode;
import com.hbm_m.client.loader.dae.DaeQuadBaker;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.ObjModelVboBuilder;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.block.model.BakedQuad;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

//? if forge {
import net.minecraftforge.client.model.data.ModelData;
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*import net.neoforged.neoforge.client.model.data.ModelData;
@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class DoorRenderer extends AbstractPartBasedRenderer<DoorBlockEntity, BakedModel> {

    private static final ConcurrentHashMap<String, InstancedStaticPartRenderer> instancedFrameCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> frameInitializationFlags = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, InstancedStaticPartRenderer> instancedPartCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> partInitializationFlags = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SingleMeshVboRenderer> DAE_RENDERER_CACHE = new ConcurrentHashMap<>();

    // === ИСПРАВЛЕНИЕ УТЕЧКИ ПАМЯТИ: Кэш загруженных DAE моделей ===
    private static final ConcurrentHashMap<ResourceLocation, DaeModel> DAE_MODELS_CACHE = new ConcurrentHashMap<>();

    private final float[] translation = new float[3];
    private final float[] origin = new float[3];
    private final float[] rotation = new float[3];

    private static final Set<String> PARTS_WITHOUT_GEOMETRY = ConcurrentHashMap.newKeySet();

    public DoorRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    private synchronized void initializeFrameInstancerForType(DoorBakedModel model, String doorType, String framePartName, String frameKey) {
        if (frameInitializationFlags.getOrDefault(frameKey, false)) return;
        try {
            BakedModel frameModel = model.getPart(framePartName);
            if (frameModel != null) {
                var frameData = ObjModelVboBuilder.buildSinglePart(frameModel, framePartName);
                if (frameData != null) {
                    var frameQuads = MeshRenderCache.getOrCompile(frameKey, frameModel);
                    InstancedStaticPartRenderer frameRenderer = new InstancedStaticPartRenderer(frameData, frameQuads);
                    frameRenderer.setMdiTraceTag("Door/frame:" + frameKey);
                    instancedFrameCache.put(frameKey, frameRenderer);
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.debug("DoorRenderer: Failed to init frame for {}: {}", doorType, e.getMessage());
        } finally {
            frameInitializationFlags.put(frameKey, true);
        }
    }

    private synchronized void initializePartInstancerFor(DoorBakedModel model, String doorType, String partName, DoorModelSelection selection, String cacheKey) {
        if (partInitializationFlags.getOrDefault(cacheKey, false)) return;
        try {
            BakedModel partModel = model.getPart(partName);
            if (partModel != null) {
                var data = ObjModelVboBuilder.buildSinglePart(partModel, partName);
                if (data != null) {
                    var partQuads = MeshRenderCache.getOrCompile(cacheKey, partModel);
                    InstancedStaticPartRenderer renderer = new InstancedStaticPartRenderer(data, partQuads);
                    renderer.setMdiTraceTag("Door/part:" + partName + ":" + cacheKey);
                    instancedPartCache.put(cacheKey, renderer);
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.debug("DoorRenderer: Failed to init part instancer for '{}': {}", partName, e.getMessage());
        } finally {
            partInitializationFlags.put(cacheKey, true);
        }
    }

    private static String getPartCacheKey(String doorType, String partName, DoorModelSelection selection) {
        return "anim_" + doorType + "_" + selection.getModelType().getId() + "_" + selection.getSkin().getId() + "_" + partName;
    }

    private static String getSelectionCacheKey(DoorModelSelection selection) {
        return selection.getModelType().getId() + "_" + selection.getSkin().getId();
    }

    private static boolean partHasGeometry(BakedModel partModel, String partName, String cacheKey) {
        if (partModel == null) return false;
        if (PARTS_WITHOUT_GEOMETRY.contains(cacheKey)) return false;
        int count = 0;
        var rand = RandomSource.create(42);
        for (Direction d : Direction.values()) {
            count += partModel.getQuads(null, d, rand, ModelData.EMPTY, RenderType.solid()).size();
        }
        count += partModel.getQuads(null, null, rand, ModelData.EMPTY, RenderType.solid()).size();
        if (count == 0) {
            PARTS_WITHOUT_GEOMETRY.add(cacheKey);
            return false;
        }
        return true;
    }

    @Override
    protected BakedModel getModel(DoorBlockEntity blockEntity) {
        DoorDecl doorDecl = blockEntity.getDoorDecl();
        if (doorDecl == null) return super.getModel(blockEntity);
        String doorType = getDoorTypeKey(doorDecl);
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        if (!registry.isRegistered(doorType)) return super.getModel(blockEntity);
        
        var selection = blockEntity.getModelSelection();
        ResourceLocation modelPath = registry.getModelPath(doorType, selection);
        if (modelPath == null) return super.getModel(blockEntity);
        
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel selectionModel = PlatformHooks.getModel(modelManager, modelPath);
        if (selectionModel == null || selectionModel == modelManager.getMissingModel()) {
            return super.getModel(blockEntity);
        }
        return selectionModel;
    }

    @Override
    protected BakedModel getModelType(BakedModel rawModel) {
        return rawModel;
    }

    @Override
    protected Direction getFacing(DoorBlockEntity blockEntity) {
        return blockEntity.getFacing();
    }

    @Override
    protected void renderParts(DoorBlockEntity be, BakedModel model, LegacyAnimator animator,
                             float partialTick, int packedLight, int packedOverlay,
                             PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!be.isController() && !com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) return;
        DoorDecl doorDecl = be.getDoorDecl();
        if (doorDecl == null) return;

        BlockPos blockPos = be.getBlockPos();
        // Куллинг + fade: в контрапшене Create shouldRender() пропускает
        // frustum/ray-march кулинг.
        float doorFade = applyCullingAndStaticFade(be);
        if (doorFade < 0) return;

        boolean onContraption = com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be);
        float openTicks;
        boolean isOpen;
        if (onContraption) {
            float progress = com.hbm_m.client.compat.create.ContraptionDoorAnimCache.chase(be, doorDecl.getOpenTime());
            openTicks = progress * doorDecl.getOpenTime();
            isOpen = progress > 0.5f;
        } else {
            openTicks = be.getOpenProgress(partialTick) * doorDecl.getOpenTime();
            isOpen = be.isOpen();
        }
        
        doorDecl.doOffsetTransform(animator);

        if (model instanceof DoorBakedModel doorModel) {
            renderWithVBO(be, doorModel, doorDecl, openTicks, isOpen, poseStack, packedLight, blockPos, bufferSource);
        } else {
            ResourceLocation daePath = doorDecl.getColladaAnimationSource();
            if (daePath != null) {
                // ВАЖНО: передаём уже вычисленный openTicks (с учётом onContraption через
                // ContraptionDoorAnimCache.chase), а не partialTick. Раньше DAE-путь сам
                // брал be.getOpenProgress(partialTick), который на контрапшене не обновляется
                // (BE пересоздаётся из frozen NBT на reset) → анимация DAE-двери на поезде
                // застывала в позиции сборки, хотя OBJ-путь той же двери работал.
                renderDaeModel(be, daePath, doorDecl, openTicks, poseStack, packedLight, bufferSource);
            }
        }
    }

    // ================= DAE RENDER PIPELINE =================
    
    private void renderDaeModel(DoorBlockEntity be, ResourceLocation daePath, DoorDecl doorDecl,
                                float openTicks, PoseStack poseStack, int packedLight,
                                MultiBufferSource bufferSource) {
                                     
        DaeModel model = DAE_MODELS_CACHE.computeIfAbsent(daePath, path -> {
            try {
                return DaeModel.load(path);
            } catch (Exception e) {
                MainRegistry.LOGGER.error("DoorRenderer: failed to load DAE model {}", path, e);
                return null;
            }
        });

        if (model == null) return;

        DaeAnimation clip = model.animations.get("animation");
        if (clip == null && !model.animations.isEmpty()) {
            clip = model.animations.values().iterator().next();
        }
        
        // openTicks уже несёт корректный прогресс (0..openTime) с учётом контрапшена
        // (ContraptionDoorAnimCache.chase) ИЛИ обычного BE.getOpenProgress. Раньше тут
        // было be.getOpenProgress(partialTick) * openTime — на контрапшене это всегда 0
        // (BE frozen), что и держало DAE-дверь статичной на поезде.
        float time = openTicks / 20.0f;
        DoorModelSelection selection = be.getModelSelection();

        // NBT-синхронизация reconstructs DoorSkin только по id (texturePath=null),
        // из-за чего resolveDaeTexture не находит текстуру для variant1/variant2 и
        // fallback-ает на block/doors/<doorId> (modern), который не совпадает с
        // DAE UV-разметкой → "missing texture". Резолвим полный DoorSkin (с
        // texturePath и textureMap) из DoorModelRegistry — там загружены данные
        // из <door>_config.json. Это не меняет modelType, только обогащает skin.
        DoorSkin fullSkin = selection.getSkin();
        if (fullSkin.getTexturePath() == null && !fullSkin.isDefault()) {
            String doorId = doorDecl.getBlockId().getPath();
            DoorModelRegistry registry = DoorModelRegistry.getInstance();
            DoorSkin resolved = registry.getSkin(doorId, fullSkin.getId());
            if (resolved != null && resolved.getTexturePath() != null) {
                selection = new DoorModelSelection(selection.getModelType(), resolved);
            }
        }

        poseStack.pushPose();
        
        int offset = doorDecl.getBakedModelRotationOffsetY();
        if (offset != 0) {
            poseStack.translate(0.5f, 0f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(offset));
            poseStack.translate(-0.5f, 0f, -0.5f);
        }

        // DAE-геометрия рамки DoorFrame хранится по X ∈ [-3.5,+3.5], Z ∈ [-0.5,+0.5]
        // с pivot 0 = corner блока контроллера, а не его центр. После setupBlockTransform
        // (T(0.5,0,0.5)+R(90+facing) — центр блока controller + поворот по facing) и
        // offset -90° выше, в локальном фрейме PoseStack ось -Z указывает "от игрока
        // в стену" равномерно для всех 4 facing'ов (проверено: NORTH R=0° → -Z→world -Z
        // = player's "looking North"; SOUTH R=180° → -Z→+Z; WEST R=90° → -Z→+X; EAST
        // R=270° → -Z→-X). Сдвиг на -1 по local-Z компенсирует "1 блок к игроку"
        // симметрично для всех facing'ов.
        if (offset != 0) {
            poseStack.translate(0f, 0f, 1f);
        }

        boolean iris = ShaderCompatibilityDetector.isExternalShaderActive();
        if (iris) {
            boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                renderDaeNodes(model.sceneRoots, clip, time, poseStack, packedLight, be, bufferSource, selection, doorDecl);
            }
        } else {
            renderDaeNodes(model.sceneRoots, clip, time, poseStack, packedLight, be, bufferSource, selection, doorDecl);
        }
        
        poseStack.popPose();
    }

    private void renderDaeNodes(List<DaeNode> nodes, DaeAnimation clip, float time,
                                PoseStack poseStack, int packedLight,
                                DoorBlockEntity be, MultiBufferSource bufferSource,
                                DoorModelSelection selection, DoorDecl doorDecl) {
        for (DaeNode node : nodes) {
            poseStack.pushPose();
            //? if < 1.21.1 {
            poseStack.mulPoseMatrix(node.localMatrix(time, clip));
             //?} else {
            /*poseStack.last().pose().mul(node.localMatrix(time, clip));
            *///?}
            if (node.mesh != null) {
                SingleMeshVboRenderer renderer = getDaeRendererForNode(node, selection, doorDecl);
                if (renderer != null) {
                    renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
                }
            }
            renderDaeNodes(node.children, clip, time, poseStack, packedLight, be, bufferSource, selection, doorDecl);
            poseStack.popPose();
        }
    }

    private SingleMeshVboRenderer getDaeRendererForNode(DaeNode node, DoorModelSelection selection, DoorDecl doorDecl) {
        String skinId = selection.getSkin().getId();
        String key = "dae_door:" + doorDecl.getBlockId().getPath() + ":" + node.name + ":" + skinId;

        return DAE_RENDERER_CACHE.computeIfAbsent(key, k -> {
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
                ResourceLocation spriteLocation = ResourceLocation.fromNamespaceAndPath(rawTexture.getNamespace(), cleanPath);

                TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                        .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .getSprite(spriteLocation);
                
                if (sprite == null) {
                    MainRegistry.LOGGER.error("DoorRenderer: Sprite '{}' not found in block atlas!", spriteLocation);
                    return null;
                }

                List<BakedQuad> quads = DaeQuadBaker.bakeNodeQuads(node.mesh, new Matrix4f(), sprite);
                if (quads.isEmpty()) return null;
                return MeshRenderCache.getOrCreateRendererFromQuadList(key, quads);
            } catch (Exception e) {
                MainRegistry.LOGGER.error("DoorRenderer: failed to bake DAE node '{}'", node.name, e);
                return null;
            }
        });
    }

    private ResourceLocation resolveDaeTexture(DaeNode node, DoorModelSelection selection, DoorDecl doorDecl) {
        DoorSkin skin = selection.getSkin();
        ResourceLocation tex = skin.getTextureForPart(node.name);
        if (tex != null && !tex.equals(skin.getTexturePath())) return tex;
        if (skin.getTexturePath() != null) return skin.getTexturePath();

        // Скин без texturePath (DoorSkin.DEFAULT — LEGACY default, либо скин из NBT
        // без обогащения через DoorModelRegistry). DoorDecl знает doorId, по нему
        // строим корректный путь: для LEGACY — «old» текстура (sliding_blast_door_old),
        // для MODERN default — обычная (sliding_blast_door). Этот fallback ВАЖНЕЕ
        // node.texture: в .dae часто стоит битая ссылка <init_from>door0.png</init_from>
        // (Blender-экспорт), которая ведёт на несуществующий ресурс и даёт missing tex.
        String basePath = doorDecl.getBlockId().getPath();
        //? if fabric && < 1.21.1 {
        /*return new ResourceLocation(RefStrings.MODID,
                "block/doors/" + basePath + (selection.isLegacy() ? "_old" : ""));
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID,
                "block/doors/" + basePath + (selection.isLegacy() ? "_old" : ""));
        //?}
    }

    // ================= OBJ RENDER PIPELINE =================

    private void renderWithVBO(DoorBlockEntity be, DoorBakedModel model, DoorDecl doorDecl,
                            float openTicks, boolean isOpen, PoseStack poseStack,
                            int packedLight, BlockPos blockPos, MultiBufferSource bufferSource) {
        try {
            String doorType = getDoorTypeKey(doorDecl);
            DoorModelSelection selection = be.getModelSelection();
            String frameKey = "frame_" + doorType + "_" + getSelectionCacheKey(selection);
            String[] partNames = model.getPartNames();
            String staticFramePart = detectFramePart(partNames);
            
            boolean useBatchingNow = ClientRenderFlags.useInstancedBatching();
            boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
            boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive() && (!useBatchingNow || shadowPass);

            if (useIrisBatch) {
                try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                    renderDoorVboParts(be, model, doorDecl, partNames, staticFramePart, doorType,
                            selection, frameKey, openTicks, poseStack, packedLight, blockPos, bufferSource);
                }
            } else {
                renderDoorVboParts(be, model, doorDecl, partNames, staticFramePart, doorType,
                        selection, frameKey, openTicks, poseStack, packedLight, blockPos, bufferSource);
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error in VBO door render", e);
        }
    }

    private void renderDoorVboParts(DoorBlockEntity be, DoorBakedModel model, DoorDecl doorDecl,
                                    String[] partNames, String staticFramePart, String doorType,
                                    DoorModelSelection selection, String frameKey, float openTicks,
                                    PoseStack poseStack, int packedLight, BlockPos blockPos,
                                    MultiBufferSource bufferSource) {
        if (staticFramePart != null) {
            if (!frameInitializationFlags.getOrDefault(frameKey, false)) {
                initializeFrameInstancerForType(model, doorType, staticFramePart, frameKey);
            }

            InstancedStaticPartRenderer frameRenderer = instancedFrameCache.get(frameKey);
            boolean useInstancedFrame = frameRenderer != null && frameRenderer.isInitialized();

            if (useInstancedFrame) {
                poseStack.pushPose();
                boolean useBatching = ClientRenderFlags.useInstancedBatching();
                boolean inShadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
                if (useBatching && !inShadowPass) {
                    frameRenderer.addInstance(poseStack, packedLight, blockPos, be, bufferSource);
                } else {
                    frameRenderer.renderSingle(poseStack, packedLight, blockPos, be, bufferSource);
                }
                poseStack.popPose();
            } else {
                BakedModel frameModel = model.getPart(staticFramePart);
                if (frameModel != null) {
                    poseStack.pushPose();
                    try {
                        String fallbackKey = "door_" + doorType + "_" + getSelectionCacheKey(selection) + "_" + staticFramePart;
                        var fallbackRenderer = MeshRenderCache.getOrCreateRenderer(fallbackKey, frameModel);
                        if (fallbackRenderer != null) {
                            fallbackRenderer.render(poseStack, packedLight, blockPos, be, bufferSource);
                        }
                    } catch (Exception e) {
                        MainRegistry.LOGGER.debug("DoorRenderer: Fallback frame render failed: {}", e.getMessage());
                    }
                    poseStack.popPose();
                }
            }
        }

        for (String partName : partNames) {
            if (staticFramePart != null && staticFramePart.equals(partName)) continue;
            renderHierarchyVbo(partName, false, be, model, doorDecl, openTicks, poseStack, packedLight, blockPos, bufferSource);
        }
    }

    private void renderHierarchyVbo(String partName, boolean child, DoorBlockEntity be, 
                                    DoorBakedModel model, DoorDecl doorDecl,
                                    float openTicks, PoseStack poseStack, int packedLight, 
                                    BlockPos blockPos, MultiBufferSource bufferSource) {
        if (!doorDecl.doesRender(partName, child)) return;
        
        DoorModelSelection selection = be.getModelSelection();
        poseStack.pushPose();
        doPartTransform(poseStack, doorDecl, partName, openTicks, child, selection);
        
        boolean isStaticPart = "frame".equalsIgnoreCase(partName) || 
                               "Frame".equals(partName) ||
                               "DoorFrame".equals(partName) ||
                               "base".equalsIgnoreCase(partName);
        if (!isStaticPart) {
            BakedModel partModel = model.getPart(partName);
            String doorType = getDoorTypeKey(doorDecl);
            String geomCacheKey = "geom_" + doorType + "_" + getSelectionCacheKey(selection) + "_" + partName;
            if (partModel != null && partHasGeometry(partModel, partName, geomCacheKey)) {
                String partCacheKey = getPartCacheKey(doorType, partName, selection);

                boolean useBatching = ClientRenderFlags.useInstancedBatching();
                boolean inShadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
                if (useBatching && !inShadowPass) {
                    if (!partInitializationFlags.getOrDefault(partCacheKey, false)) {
                        initializePartInstancerFor(model, doorType, partName, selection, partCacheKey);
                    }
                    InstancedStaticPartRenderer partRenderer = instancedPartCache.get(partCacheKey);
                    if (partRenderer != null && partRenderer.isInitialized()) {
                        partRenderer.addInstance(poseStack, packedLight, blockPos, be, bufferSource);
                    } else {
                        renderPartViaDoorVbo(model, partName, doorType, selection, poseStack, packedLight, blockPos, be, doorDecl, openTicks, child, bufferSource);
                    }
                } else {
                    renderPartViaDoorVbo(model, partName, doorType, selection, poseStack, packedLight, blockPos, be, doorDecl, openTicks, child, bufferSource);
                }
            }
        }
        
        for (String c : doorDecl.getChildren(partName, selection)) {
            renderHierarchyVbo(c, true, be, model, doorDecl, openTicks, poseStack, packedLight, blockPos, bufferSource);
        }
        
        poseStack.popPose();
    }

    private String getDoorTypeKey(DoorDecl doorDecl) {
        if (doorDecl == DoorDecl.LARGE_VEHICLE_DOOR) return "large_vehicle_door";
        if (doorDecl == DoorDecl.ROUND_AIRLOCK_DOOR) return "round_airlock_door";
        if (doorDecl == DoorDecl.FIRE_DOOR) return "fire_door";
        if (doorDecl == DoorDecl.SLIDING_SEAL_DOOR) return "sliding_seal_door";
        if (doorDecl == DoorDecl.SECURE_ACCESS_DOOR) return "secure_access_door";
        if (doorDecl == DoorDecl.QE_SLIDING) return "qe_sliding_door";
        if (doorDecl == DoorDecl.QE_CONTAINMENT) return "qe_containment_door";
        if (doorDecl == DoorDecl.WATER_DOOR) return "water_door";
        if (doorDecl == DoorDecl.SILO_HATCH) return "silo_hatch";
        if (doorDecl == DoorDecl.SILO_HATCH_LARGE) return "silo_hatch_large";
        if (doorDecl == DoorDecl.VAULT_DOOR) return "vault_door";
        if (doorDecl == DoorDecl.CARGO_DOOR) return "cargo_door";
        if (doorDecl.getBlockId().getPath().equals("sliding_blast_door")) return "sliding_blast_door";
        throw new IllegalStateException("Unknown door type: " + doorDecl.getClass().getName());
    }

    private void renderPartViaDoorVbo(DoorBakedModel model, String partName, String doorType,
            DoorModelSelection selection, PoseStack poseStack, int packedLight, BlockPos blockPos,
            DoorBlockEntity be, DoorDecl doorDecl, float openTicks, boolean child,
            MultiBufferSource bufferSource) {
        try {
            com.hbm_m.client.render.implementations.DoorVboRenderer partRenderer = 
                com.hbm_m.client.render.implementations.DoorVboRenderer.getOrCreate(model, partName, doorType, selection);
            partRenderer.renderPart(poseStack, packedLight, blockPos, be, doorDecl, openTicks, child, bufferSource);
        } catch (IllegalStateException e) {
            MainRegistry.LOGGER.debug("No mesh for part {}, skipping render: {}", partName, e.getMessage());
        }
    }

    private String detectFramePart(String[] partNames) {
        for (String p : partNames) if ("frame".equals(p)) return "frame";
        for (String p : partNames) if ("Frame".equals(p)) return "Frame";
        for (String p : partNames) if ("DoorFrame".equals(p)) return "DoorFrame";
        for (String p : partNames) if ("base".equals(p)) return "base";
        for (String p : partNames) if ("Base".equals(p)) return "Base";
        return null;
    }

    private void doPartTransform(PoseStack poseStack, DoorDecl doorDecl,
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

    @Override 
    public boolean shouldRenderOffScreen(DoorBlockEntity be) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return false;
        }
        return true;
    }

    public static void flushInstancedBatches(Matrix4f projectionMatrix) {
        ArrayList<String> frameKeys = new ArrayList<>(instancedFrameCache.keySet());
        Collections.sort(frameKeys);
        for (String key : frameKeys) {
            InstancedStaticPartRenderer renderer = instancedFrameCache.get(key);
            if (renderer != null) renderer.flush(projectionMatrix);
        }
        ArrayList<String> partKeys = new ArrayList<>(instancedPartCache.keySet());
        Collections.sort(partKeys);
        for (String key : partKeys) {
            InstancedStaticPartRenderer renderer = instancedPartCache.get(key);
            if (renderer != null) renderer.flush(projectionMatrix);
        }
    }

    public void onResourceManagerReload() {
        clearAllCaches();
    }

    public static void clearAllCaches() {
        for (InstancedStaticPartRenderer renderer : instancedFrameCache.values()) {
            if (renderer != null) renderer.cleanup();
        }
        instancedFrameCache.clear();
        frameInitializationFlags.clear();

        for (InstancedStaticPartRenderer renderer : instancedPartCache.values()) {
            if (renderer != null) renderer.cleanup();
        }
        instancedPartCache.clear();
        partInitializationFlags.clear();

        com.hbm_m.client.render.implementations.DoorVboRenderer.clearCache();

        // Очищаем рендереры из общего кэша MeshRenderCache перед удалением
        for (String key : DAE_RENDERER_CACHE.keySet()) {
            MeshRenderCache.removeRenderer(key);
        }
        DAE_RENDERER_CACHE.values().forEach(SingleMeshVboRenderer::cleanup);
        DAE_RENDERER_CACHE.clear();
        DAE_MODELS_CACHE.clear();

        PARTS_WITHOUT_GEOMETRY.clear();
        
        MainRegistry.LOGGER.debug("Door renderer caches cleared");
    }
}