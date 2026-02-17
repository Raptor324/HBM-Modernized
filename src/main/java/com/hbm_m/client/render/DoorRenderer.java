package com.hbm_m.client.render;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.block.entity.custom.doors.DoorDecl;
import com.hbm_m.client.loader.ColladaAnimationData;
import com.hbm_m.client.loader.ColladaAnimationParser;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class DoorRenderer extends AbstractPartBasedRenderer<DoorBlockEntity, DoorBakedModel> {

    // Instanced рендерер для статической части frame
    private static final ConcurrentHashMap<String, InstancedStaticPartRenderer> instancedFrameCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> frameInitializationFlags = new ConcurrentHashMap<>();
    // Instanced рендереры для анимированных частей (door, doorLeft, doorRight и т.д.)
    private static final ConcurrentHashMap<String, InstancedStaticPartRenderer> instancedPartCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> partInitializationFlags = new ConcurrentHashMap<>();

    // Переиспользуемые буферы для трансформаций (legacy режим)
    private final float[] translation = new float[3];
    private final float[] origin = new float[3];
    private final float[] rotation = new float[3];

    /** Части без геометрии — не пытаемся рендерить и не спамим лог */
    private static final Set<String> PARTS_WITHOUT_GEOMETRY = ConcurrentHashMap.newKeySet();

    public DoorRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    // Инициализация instanced рендерера для frame
    private synchronized void initializeFrameInstancerForType(DoorBakedModel model,
                                                            String doorType,
                                                            String framePartName,
                                                            String frameKey) {
        if (frameInitializationFlags.getOrDefault(frameKey, false)) return;

        try {
            BakedModel frameModel = model.getPart(framePartName);
            if (frameModel != null) {
                var frameData = ObjModelVboBuilder.buildSinglePart(frameModel, framePartName);
                if (frameData != null) {
                    var frameQuads = GlobalMeshCache.getOrCompile(frameKey, frameModel);
                    InstancedStaticPartRenderer frameRenderer = new InstancedStaticPartRenderer(frameData, frameQuads);
                    instancedFrameCache.put(frameKey, frameRenderer);
                    frameInitializationFlags.put(frameKey, true);
                    MainRegistry.LOGGER.debug("DoorRenderer: Frame instancer created for '{}' (key: {})", framePartName, frameKey);
                } else {
                    frameInitializationFlags.put(frameKey, true);
                    MainRegistry.LOGGER.debug("DoorRenderer: Frame part '{}' has no geometry (doorType: {}), using fallback", framePartName, doorType);
                }
            } else {
                frameInitializationFlags.put(frameKey, true);
                MainRegistry.LOGGER.debug("DoorRenderer: Frame model is null for '{}' (doorType: {})", framePartName, doorType);
            }
        } catch (Exception e) {
            frameInitializationFlags.put(frameKey, true);
            MainRegistry.LOGGER.debug("DoorRenderer: Failed to init frame for {}: {}", doorType, e.getMessage());
        }
    }

    private synchronized void initializePartInstancerFor(DoorBakedModel model, String doorType,
            String partName, DoorModelSelection selection, String cacheKey) {
        if (partInitializationFlags.getOrDefault(cacheKey, false)) return;

        try {
            BakedModel partModel = model.getPart(partName);
            if (partModel != null) {
                var data = ObjModelVboBuilder.buildSinglePart(partModel, partName);
                if (data != null) {
                    var partQuads = GlobalMeshCache.getOrCompile(cacheKey, partModel);
                    InstancedStaticPartRenderer renderer = new InstancedStaticPartRenderer(data, partQuads);
                    instancedPartCache.put(cacheKey, renderer);
                    partInitializationFlags.put(cacheKey, true);
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.debug("DoorRenderer: Failed to init part instancer for '{}': {}", partName, e.getMessage());
        }
    }

    private static String getPartCacheKey(String doorType, String partName, DoorModelSelection selection) {
        return "anim_" + doorType + "_" + selection.getModelType().getId()
                + "_" + selection.getSkin().getId() + "_" + partName;
    }

    /** Проверяет, есть ли у части геометрия (квады). Результат кэшируется. */
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
    protected DoorBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof DoorBakedModel model ? model : null;
    }

    @Override
    protected Direction getFacing(DoorBlockEntity blockEntity) {
        return blockEntity.getFacing();
    }

    @Override
    protected void renderParts(DoorBlockEntity be, DoorBakedModel model, LegacyAnimator animator,
                             float partialTick, int packedLight, int packedOverlay,
                             PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!be.isController()) return;
        DoorDecl doorDecl = be.getDoorDecl();
        if (doorDecl == null) return;

        BlockPos blockPos = be.getBlockPos();
        
        var minecraft = Minecraft.getInstance();
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        float openTicks = be.getOpenProgress(partialTick) * doorDecl.getOpenTime();
        boolean isOpen = be.isOpen();
        
        // Проверяем наличие стороннего шейдера (Iris/Oculus)
        boolean shaderActive = ShaderCompatibilityDetector.isExternalShaderActive();
        // Дверь движется (state 2=закрывается, 3=открывается)
        boolean isMoving = be.isMoving();
        
        // Шейдер активен + дверь НЕ движется → всё в BakedModel, BER не рендерит
        if (shaderActive && !isMoving) {
            return;
        }

        // Применяем базовое смещение модели
        doorDecl.doOffsetTransform(animator);

        renderWithVBO(be, model, doorDecl, openTicks, isOpen, poseStack, packedLight, blockPos, bufferSource);
    }

    /**
     * VBO рендеринг для оптимальной производительности
     */
    private void renderWithVBO(DoorBlockEntity be, DoorBakedModel model, DoorDecl doorDecl,
                            float openTicks, boolean isOpen, PoseStack poseStack,
                            int packedLight, BlockPos blockPos, MultiBufferSource bufferSource) {
        try {
            String doorType = getDoorTypeKey(doorDecl);
            String frameKey = "frame_" + doorType;
            String[] partNames = model.getPartNames();
            String staticFramePart = detectFramePart(partNames);
            
            // Проверяем наличие стороннего шейдера
            boolean shaderActive = ShaderCompatibilityDetector.isExternalShaderActive();
            boolean isMoving = be.isMoving();

            // Load animation data once (можно отключить в конфиге при проблемах)
            ColladaAnimationData animData = null;
            if (ModClothConfig.get().useColladaDoorAnimations && doorDecl.getColladaAnimationSource() != null) {
                animData = ColladaAnimationData.getOrLoad(Minecraft.getInstance().getResourceManager(), doorDecl.getColladaAnimationSource());
            }

            // Конвертация Z-up (Blender) → Y-up (Minecraft) для всей модели
            if (ModClothConfig.get().useColladaZUpConversion && animData != null && animData.isZUp()) {
                poseStack.mulPoseMatrix(ColladaAnimationParser.Z_UP_TO_Y_UP);
            }

            // IRIS/OCULUS PATH: При активном шейдере и движущейся двери
            // Frame уже отрендерен через BakedModel - рендерим только анимированные части
            if (shaderActive && isMoving) {
                // Пропускаем shadow pass - Iris сам сделает тени от основной геометрии
                if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
                    return;
                }
                
                // Рендерим только анимированные части через putBulkData
                for (String partName : partNames) {
                    if (staticFramePart != null && staticFramePart.equals(partName)) {
                        continue; // Frame уже в BakedModel
                    }
                    if (isChildInDae(partName, doorDecl, animData, partNames)) {
                        continue; // Skip, will be rendered as child
                    }
                    
                    renderAnimatedPartForIris(partName, doorType, be, model, doorDecl, openTicks, 
                                             poseStack, packedLight, animData, bufferSource, false);
                }
                return;
            }

            // VANILLA VBO PATH: Нет шейдера - полный VBO рендер
            
            // 1. Instanced Frame Render
            if (staticFramePart != null) {
                if (!frameInitializationFlags.getOrDefault(frameKey, false)) {
                    initializeFrameInstancerForType(model, doorType, staticFramePart, frameKey);
                }
                
                InstancedStaticPartRenderer frameRenderer = instancedFrameCache.get(frameKey);
                boolean useInstancedFrame = frameRenderer != null && frameRenderer.isInitialized();
                
                if (useInstancedFrame) {
                    poseStack.pushPose();
                    boolean useBatching = ModClothConfig.useInstancedBatching();
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
                            String fallbackKey = "door_" + doorType + "_" + staticFramePart;
                            var fallbackRenderer = GlobalMeshCache.getOrCreateRenderer(fallbackKey, frameModel);
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

            // 2. Рисуем ВСЕ части из JSON, кроме той, что ушла в instanced-frame
            for (String partName : partNames) {
                if (staticFramePart != null && staticFramePart.equals(partName)) {
                    continue; // рамку уже нарисовали
                }

                // Check if this part is a child in DAE
                if (isChildInDae(partName, doorDecl, animData, partNames)) {
                    continue; // Skip, will be rendered as child
                }

                renderHierarchyVbo(partName, false, be, model, doorDecl,
                                openTicks, poseStack, packedLight, blockPos, animData, bufferSource);
            }

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error in VBO door render", e);
        }
    }
    
    /**
     * Рендер анимированной части через putBulkData для Iris/Oculus пути.
     * doorType в ключе кэша — иначе qe_containment_door показывал бы створку fire_door.
     * @param child true при рекурсивном вызове для дочерних частей (water_door: spinny_upper/lower)
     */
    private void renderAnimatedPartForIris(String partName, String doorType, DoorBlockEntity be, DoorBakedModel model,
                                           DoorDecl doorDecl, float openTicks, PoseStack poseStack,
                                           int packedLight, ColladaAnimationData animData,
                                           MultiBufferSource bufferSource, boolean child) {
        if (!doorDecl.doesRender(partName, child)) return;
        
        poseStack.pushPose();
        doPartTransform(poseStack, doorDecl, partName, openTicks, child, animData);
        
        BakedModel partModel = model.getPart(partName);
        if (partModel != null) {
            var quads = GlobalMeshCache.getOrCompile("door_anim_" + doorType + "_" + partName, partModel);
            if (quads != null && !quads.isEmpty() && bufferSource != null) {
                float brightness = ModClothConfig.get().doorAnimatedPartBrightness / 100f;
                var consumer = bufferSource.getBuffer(RenderType.solid());
                var pose = poseStack.last();
                for (var quad : quads) {
                    consumer.putBulkData(pose, quad, brightness, brightness, brightness, 1f, packedLight, 
                                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, false);
                }
            }
        }
        
        // Рекурсивно дети из DoorDecl (child=true — water_door.doesRender("spinny_*", true) вернёт true)
        for (String c : doorDecl.getChildren(partName)) {
            renderAnimatedPartForIris(c, doorType, be, model, doorDecl, openTicks, poseStack, 
                                     packedLight, animData, bufferSource, true);
        }

        // Рекурсивно дети из DAE (как в renderHierarchyVbo)
        if (animData != null) {
            String daeName = doorDecl.getDaeObjectName(partName);
            List<String> daeChildren = animData.getChildren(daeName);
            for (String childDaeName : daeChildren) {
                for (String potentialChild : model.getPartNames()) {
                    if (doorDecl.getDaeObjectName(potentialChild).equals(childDaeName)) {
                        boolean alreadyHandled = false;
                        for (String c : doorDecl.getChildren(partName)) {
                            if (c.equals(potentialChild)) { alreadyHandled = true; break; }
                        }
                        if (!alreadyHandled) {
                            renderAnimatedPartForIris(potentialChild, doorType, be, model, doorDecl, openTicks,
                                    poseStack, packedLight, animData, bufferSource, true);
                        }
                    }
                }
            }
        }
        
        poseStack.popPose();
    }

    private boolean isChildInDae(String partName, DoorDecl doorDecl, ColladaAnimationData animData, String[] allPartNames) {
        if (animData == null) return false;
        String daeName = doorDecl.getDaeObjectName(partName);
        String parentDaeName = animData.getParent(daeName);
        if (parentDaeName == null) return false;
        
        // Check if parent exists in our model parts
        for (String p : allPartNames) {
            if (p.equals(partName)) continue;
            if (doorDecl.getDaeObjectName(p).equals(parentDaeName)) {
                return true;
            }
        }
        return false;
    }

    private void renderHierarchyVbo(String partName, boolean child, DoorBlockEntity be, 
                                    DoorBakedModel model, DoorDecl doorDecl,
                                    float openTicks, PoseStack poseStack, int packedLight, 
                                    BlockPos blockPos, ColladaAnimationData animData,
                                    MultiBufferSource bufferSource) {
        if (!doorDecl.doesRender(partName, child)) return;
        
        poseStack.pushPose();
        doPartTransform(poseStack, doorDecl, partName, openTicks, child, animData);
        
        // Рендерим текущую часть ТОЛЬКО если у неё есть mesh
        boolean isStaticPart = "frame".equalsIgnoreCase(partName) || 
                               "DoorFrame".equals(partName) ||
                               "base".equalsIgnoreCase(partName);
        if (!isStaticPart) {
            BakedModel partModel = model.getPart(partName);
            String doorType = getDoorTypeKey(doorDecl);
            String geomCacheKey = "geom_" + doorType + "_" + partName;
            if (partModel != null && partHasGeometry(partModel, partName, geomCacheKey)) {
                DoorModelSelection selection = be.getModelSelection();
                String partCacheKey = getPartCacheKey(doorType, partName, selection);

                boolean useBatching = ModClothConfig.useInstancedBatching();
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
        
        // Рекурсивно дети с child=true (from DoorDecl)
        for (String c : doorDecl.getChildren(partName)) {
            renderHierarchyVbo(c, true, be, model, doorDecl, openTicks, poseStack, packedLight, blockPos, animData, bufferSource);
        }

        // Recurse children from DAE
        if (animData != null) {
            String daeName = doorDecl.getDaeObjectName(partName);
            List<String> daeChildren = animData.getChildren(daeName);
            for (String childDaeName : daeChildren) {
                // Find corresponding OBJ part name
                for (String potentialChild : model.getPartNames()) {
                    if (doorDecl.getDaeObjectName(potentialChild).equals(childDaeName)) {
                        // Avoid double rendering if it was already in DoorDecl children
                        boolean alreadyHandled = false;
                        for (String c : doorDecl.getChildren(partName)) {
                            if (c.equals(potentialChild)) {
                                alreadyHandled = true;
                                break;
                            }
                        }
                        if (!alreadyHandled) {
                            renderHierarchyVbo(potentialChild, true, be, model, doorDecl, openTicks, poseStack, packedLight, blockPos, animData, bufferSource);
                        }
                    }
                }
            }
        }
        
        poseStack.popPose();
    }

    private String getDoorTypeKey(DoorDecl doorDecl) {
        if (doorDecl == DoorDecl.LARGE_VEHICLE_DOOR) {
            return "large_vehicle_door";
        } else if (doorDecl == DoorDecl.ROUND_AIRLOCK_DOOR) {
            return "round_airlock_door";
        } else if (doorDecl == DoorDecl.TRANSITION_SEAL) {
            return "transition_seal";
        } else if (doorDecl == DoorDecl.FIRE_DOOR) {
            return "fire_door";
        } else if (doorDecl == DoorDecl.SLIDE_DOOR) {
            return "sliding_blast_door";
        } else if (doorDecl == DoorDecl.SLIDING_SEAL_DOOR) {
            return "sliding_seal_door";
        } else if (doorDecl == DoorDecl.SECURE_ACCESS_DOOR) {
            return "secure_access_door";
        } else if (doorDecl == DoorDecl.QE_SLIDING) {
            return "qe_sliding_door";
        } else if (doorDecl == DoorDecl.QE_CONTAINMENT) {
            return "qe_containment_door";
        } else if (doorDecl == DoorDecl.WATER_DOOR) {
            return "water_door";
        } else if (doorDecl == DoorDecl.SILO_HATCH) {
            return "silo_hatch";
        } else if (doorDecl == DoorDecl.SILO_HATCH_LARGE) {
            return "silo_hatch_large";
        }
        throw new IllegalStateException("Unknown door type: " + doorDecl.getClass().getName());
    }

    private void renderPartViaDoorVbo(DoorBakedModel model, String partName, String doorType,
            DoorModelSelection selection, PoseStack poseStack, int packedLight, BlockPos blockPos,
            DoorBlockEntity be, DoorDecl doorDecl, float openTicks, boolean child,
            MultiBufferSource bufferSource) {
        try {
            DoorVboRenderer partRenderer = DoorVboRenderer.getOrCreate(model, partName, doorType, selection);
            partRenderer.renderPart(poseStack, packedLight, blockPos, be, doorDecl, openTicks, child, bufferSource);
        } catch (IllegalStateException e) {
            MainRegistry.LOGGER.debug("No mesh for part {}, skipping render: {}", partName, e.getMessage());
        }
    }

    private String detectFramePart(String[] partNames) {
        for (String p : partNames) {
            if ("frame".equals(p)) return "frame";
        }
        for (String p : partNames) {
            if ("Frame".equals(p)) return "Frame";
        }
        for (String p : partNames) {
            if ("DoorFrame".equals(p)) return "DoorFrame";
        }
        for (String p : partNames) {
            if ("base".equals(p)) return "base";
        }
        for (String p : partNames) {
            if ("Base".equals(p)) return "Base";
        }
        return null;
    }


    /**
     * Применяет трансформации части двери.
     * ВАЖНО: При наличии DAE-анимации применяются И процедурная трансформация (getOrigin, getRotation, getTranslation),
     * И полная матрица DAE (включая translation) — оба источника нужны для корректных пивотов и смещений.
     */
    private void doPartTransform(PoseStack poseStack, DoorDecl doorDecl,
                                String partName, float openTicks, boolean child, ColladaAnimationData animData) {
        
        // 1. Процедурная трансформация (origin → rotation → translation)
        doorDecl.getOrigin(partName, origin);
        doorDecl.getRotation(partName, openTicks, rotation);
        
        // Смещаемся к Pivot Point
        poseStack.translate(origin[0], origin[1], origin[2]);
        
        // 1.1 Процедурный поворот
        if (rotation[0] != 0) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotation[0]));
        if (rotation[1] != 0) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation[1]));
        if (rotation[2] != 0) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation[2]));
        
        // 1.2 Процедурное перемещение
        doorDecl.getTranslation(partName, openTicks, child, translation);
        poseStack.translate(-origin[0] + translation[0], -origin[1] + translation[1], -origin[2] + translation[2]);

        // 2. Полная матрица DAE (rotation + translation) — НЕ обнуляем m30,m31,m32, пивоты в DAE важны
        if (animData != null) {
            String daeObjectName = doorDecl.getDaeObjectName(partName);
            float normProgress = Math.min(1f, openTicks / doorDecl.getOpenTime());
            float timeSec = doorDecl.isColladaAnimationInverted()
                ? (1f - normProgress) * animData.getDurationSeconds()
                : normProgress * animData.getDurationSeconds();
            
            Matrix4f matrix = animData.getTransformMatrix(daeObjectName, timeSec);
            if (matrix != null) {
                poseStack.mulPoseMatrix(matrix);
            }
        }
    }

    @Override 
    public boolean shouldRenderOffScreen(DoorBlockEntity be) {
        // Во время shadow pass не включаем двери в global list — избегаем краша
        // Oculus/Embeddium (ReferenceOpenHashSet.wrapped is null при итерации)
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return false;
        }
        return true;
    }

    /**
     * ВАЖНО: Вызывать в конце рендера ВСЕХ дверей для флаша батчей.
     * При useInstancedBatching использует матрицы из события.
     */
    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (ModClothConfig.get().enableDebugLogging) {
            for (var e : instancedFrameCache.entrySet()) {
                var r = e.getValue();
                if (r != null && r.getInstanceCount() > 0) {
                    MainRegistry.LOGGER.debug("DoorRenderer flush frame '{}': {} instances", e.getKey(), r.getInstanceCount());
                }
            }
            for (var e : instancedPartCache.entrySet()) {
                var r = e.getValue();
                if (r != null && r.getInstanceCount() > 0) {
                    MainRegistry.LOGGER.debug("DoorRenderer flush part '{}': {} instances", e.getKey(), r.getInstanceCount());
                }
            }
        }
        for (InstancedStaticPartRenderer renderer : instancedFrameCache.values()) {
            if (renderer != null) renderer.flush(event);
        }
        for (InstancedStaticPartRenderer renderer : instancedPartCache.values()) {
            if (renderer != null) renderer.flush(event);
        }
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    /**
     * Вызывается при перезагрузке ресурсов
     */
    public void onResourceManagerReload() {
        // Очищаем instanced кэш
        clearAllCaches();
    }

    /**
     * Статический метод для глобальной очистки (вызывается из регистрации событий)
     */
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

        // Очищаем VBO кэш
        DoorVboRenderer.clearCache();

        // Очищаем глобальный кэш мешей
        GlobalMeshCache.clearAll();

        // Очищаем кэш Collada-анимаций
        ColladaAnimationData.clearCache();

        // Сбрасываем кэш частей без геометрии (при смене моделей)
        PARTS_WITHOUT_GEOMETRY.clear();

        MainRegistry.LOGGER.debug("Door renderer caches cleared");
    }
}