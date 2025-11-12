package com.hbm_m.client.render;

import com.hbm_m.block.entity.DoorBlockEntity;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.block.entity.DoorDecl;
import com.hbm_m.client.render.shader.RenderPathManager;
import com.hbm_m.client.render.shader.ImmediateFallbackRenderer;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class DoorRenderer extends AbstractPartBasedRenderer<DoorBlockEntity, DoorBakedModel> {

    // Кэш fallback рендереров для совместимости с шейдерами
    private static final ConcurrentHashMap<String, ImmediateFallbackRenderer> fallbackRenderers = new ConcurrentHashMap<>();

    // НОВОЕ: Instanced рендерер для статической части frame
    private static final ConcurrentHashMap<String, InstancedStaticPartRenderer> instancedFrameCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> frameInitializationFlags = new ConcurrentHashMap<>();

    // ОПТИМИЗАЦИЯ: Переиспользуемые буферы для трансформаций (legacy режим)
    private final float[] translation = new float[3];
    private final float[] origin = new float[3];
    private final float[] rotation = new float[3];

    public DoorRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    // Инициализация instanced рендерера для frame
    private synchronized void initializeFrameInstancerForType(DoorBakedModel model, String doorType) {
        String frameKey = "frame_" + doorType;
        
        if (frameInitializationFlags.getOrDefault(frameKey, false)) return;
        
        try {
            BakedModel frameModel = model.getPart("frame");
            if (frameModel != null) {
                var frameData = ObjModelVboBuilder.buildSinglePart(frameModel);
                InstancedStaticPartRenderer frameRenderer = new InstancedStaticPartRenderer(frameData);
                instancedFrameCache.put(frameKey, frameRenderer);
            }
            frameInitializationFlags.put(frameKey, true);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize door frame instanced renderer for type: " + doorType, e);
            frameInitializationFlags.put(frameKey, false);
        }
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
        
        // ИСПРАВЛЕНО: Используем правильный вызов OcclusionCullingHelper
        var minecraft = Minecraft.getInstance();
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        float openTicks = be.getOpenProgress(partialTick) * doorDecl.getOpenTime();
        boolean isOpen = be.isOpen();

        // Применяем базовое смещение модели
        doorDecl.doOffsetTransform(animator);

        // КРИТИЧНО: Проверяем shadow pass
        boolean isShadowPass = isShadowPassActive();
        
        if (isShadowPass || RenderPathManager.shouldUseFallback()) {
            // В shadow pass ВСЕГДА используем fallback
            renderWithFallback(be, model, doorDecl, openTicks, isOpen, poseStack, packedLight);
        } else {
            // VBO рендер только в обычном режиме
            renderWithVBO(be, model, doorDecl, openTicks, isOpen, poseStack, packedLight, blockPos);
        }
    }

    /**
     * VBO рендеринг для оптимальной производительности
     */
    private void renderWithVBO(DoorBlockEntity be, DoorBakedModel model, DoorDecl doorDecl,
                            float openTicks, boolean isOpen, PoseStack poseStack,
                            int packedLight, BlockPos blockPos) {
        try {
            // ИСПРАВЛЕНИЕ: Генерируем уникальный ключ на основе типа двери
            String doorType = getDoorTypeKey(doorDecl);
            String frameKey = "frame_" + doorType;
            
            // Инициализируем frame рендерер для конкретного типа двери
            if (!frameInitializationFlags.getOrDefault(frameKey, false)) {
                initializeFrameInstancerForType(model, doorType);
            }

            String[] partNames = model.getPartNames();
            Level level = be.getLevel();
            
            for (String partName : partNames) {
                if (!doorDecl.doesRender(partName, isOpen)) continue;

                if ("frame".equals(partName)) {
                    InstancedStaticPartRenderer frameRenderer = instancedFrameCache.get(frameKey);
                    if (frameRenderer != null) {
                        frameRenderer.addInstance(poseStack, packedLight, blockPos, be);
                    } else {
                        renderFallbackPart(partName, model.getPart(partName), poseStack, packedLight, blockPos, level, be);
                    }
                } else if (!"Base".equals(partName)) {
                    try {
                        // ИСПРАВЛЕНИЕ: Передаем тип двери в VBO рендерер
                        DoorVboRenderer partRenderer = DoorVboRenderer.getOrCreate(model, partName, doorType);
                        partRenderer.renderPart(poseStack, packedLight, blockPos, be, doorDecl, openTicks, isOpen);
                    } catch (IllegalArgumentException e) {
                        MainRegistry.LOGGER.warn("Cannot create VBO renderer for part {}: {}", partName, e.getMessage());
                        renderFallbackPart(partName, model.getPart(partName), poseStack, packedLight, blockPos, level, be);
                    }
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error in VBO door render", e);
            renderWithFallback(be, model, doorDecl, openTicks, isOpen, poseStack, packedLight);
        }
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
        // Fallback для неизвестных типов
        throw new IllegalStateException("Unknown door type: " + doorDecl.getClass().getName());
    }
    

    /**
     * Fallback рендеринг для совместимости с шейдерами
     */
    private void renderWithFallback(DoorBlockEntity be, DoorBakedModel model, DoorDecl doorDecl,
                                float openTicks, boolean isOpen, PoseStack poseStack, int packedLight) {
        
        // Используем кэшированный массив из DoorBakedModel
        String[] partNames = model.getPartNames();
        BlockPos blockPos = be.getBlockPos();
        Level level = be.getLevel();
        
        for (String partName : partNames) {
            if (!doorDecl.doesRender(partName, isOpen)) continue;
            
            poseStack.pushPose();
            
            // Применяем трансформации части
            doPartTransform(poseStack, doorDecl, partName, openTicks, isOpen);
            
            // ИСПРАВЛЕНО: Рендерим через fallback рендерер с culling поддержкой
            renderFallbackPart(partName, model.getPart(partName), poseStack, packedLight, blockPos, level, be);
            
            poseStack.popPose();
        }
        
        // КРИТИЧНО: Завершаем batch ПОСЛЕ всех частей, а не после каждой
        ImmediateFallbackRenderer.endBatch();
    }

    /**
     * Применяет трансформации части двери (legacy режим)
     */
    private void doPartTransform(PoseStack poseStack, DoorDecl doorDecl,
                                String partName, float openTicks, boolean isOpen) {
        // ВАЖНО: DoorDecl методы НЕ создают новые массивы, а заполняют переданные
        doorDecl.getTranslation(partName, openTicks, isOpen, translation);
        doorDecl.getOrigin(partName, origin);
        doorDecl.getRotation(partName, openTicks, rotation);

        poseStack.translate(origin[0], origin[1], origin[2]);

        // Условные проверки оптимальны для CPU branch prediction
        if (rotation[0] != 0) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotation[0]));
        if (rotation[1] != 0) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation[1]));
        if (rotation[2] != 0) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation[2]));

        poseStack.translate(-origin[0] + translation[0], -origin[1] + translation[1], -origin[2] + translation[2]);
    }

    /**
     * Рендерит часть модели через fallback рендерер
     */
    private void renderFallbackPart(String partName, BakedModel partModel, PoseStack poseStack,
                                int packedLight, BlockPos blockPos, Level level, BlockEntity blockEntity) {
        if (partModel == null) return;
        
        try {
            String doorType = getDoorTypeKey(((DoorBlockEntity) blockEntity).getDoorDecl());
            
            // ИСПРАВЛЕНИЕ: Включаем тип двери в ключ кэша fallback рендерера
            ImmediateFallbackRenderer renderer = fallbackRenderers.computeIfAbsent(
                "door_" + doorType + "_" + partName,
                k -> new ImmediateFallbackRenderer(partModel)
            );
            
            renderer.render(poseStack, packedLight, null, blockPos, level, blockEntity);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error rendering fallback door part {}: {}", partName, e.getMessage());
            // Удаляем проблемный рендерер из кэша для повторного создания
            String doorType = getDoorTypeKey(((DoorBlockEntity) blockEntity).getDoorDecl());
            fallbackRenderers.remove("door_" + doorType + "_" + partName);
        }
    }

    @Override  
    public boolean shouldRender(DoorBlockEntity blockEntity, Vec3 cameraPos) {
        // Проверяем shadow pass через Oculus API
        if (isShadowPassActive()) {
            // В shadow pass рендерим всё в пределах shadow distance
            return blockEntity.getBlockPos().distSqr(new BlockPos((int)cameraPos.x, (int)cameraPos.y, (int)cameraPos.z)) <= 
                   getShadowRenderDistance() * getShadowRenderDistance();
        }
        
        // Обычная проверка
        return super.shouldRender(blockEntity, cameraPos);
    }

    @Override 
    public boolean shouldRenderOffScreen(DoorBlockEntity be) { 
        return true; 
    }

    private static double getShadowRenderDistance() {
        try {
            Class<?> shadowRenderingClass = Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState");
            var method = shadowRenderingClass.getDeclaredMethod("getRenderDistance");
            method.setAccessible(true);
            Integer distance = (Integer) method.invoke(null);
            return distance != null ? distance : 128;
        } catch (Exception e) {
            return 128; // Fallback distance
        }
    }

    private static boolean isShadowPassActive() {
        try {
            Class<?> shadowRenderingClass = Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState");
            var method = shadowRenderingClass.getDeclaredMethod("areShadowsCurrentlyBeingRendered");
            method.setAccessible(true);
            Boolean result = (Boolean) method.invoke(null);
            return result != null && result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ВАЖНО: Вызывать в конце рендера ВСЕХ дверей для флаша батчей
     */
    public static void flushInstancedBatches() {
        for (InstancedStaticPartRenderer renderer : instancedFrameCache.values()) {
            if (renderer != null) {
                renderer.flush();
            }
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
        // Очищаем instanced кэш для всех типов дверей
        for (InstancedStaticPartRenderer renderer : instancedFrameCache.values()) {
            if (renderer != null) {
                renderer.cleanup();
            }
        }
        instancedFrameCache.clear();
        frameInitializationFlags.clear();

        // Очищаем VBO кэш
        DoorVboRenderer.clearCache();

        // Очищаем fallback кэш
        fallbackRenderers.clear();

        // Принудительно сбрасываем Tesselator
        ImmediateFallbackRenderer.clearGlobalCache();
        ImmediateFallbackRenderer.endFrame();

        // Очищаем глобальный кэш мешей
        GlobalMeshCache.clearAll();

        MainRegistry.LOGGER.debug("Door renderer caches cleared");
    }
}
