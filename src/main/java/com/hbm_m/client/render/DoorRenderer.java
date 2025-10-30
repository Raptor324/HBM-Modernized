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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class DoorRenderer extends AbstractPartBasedRenderer<DoorBlockEntity, DoorBakedModel> {

    // Кэш fallback рендереров для совместимости с шейдерами
    private static final ConcurrentHashMap<String, ImmediateFallbackRenderer> fallbackRenderers = new ConcurrentHashMap<>();

    // НОВОЕ: Instanced рендерер для статической части frame
    private static volatile InstancedStaticPartRenderer instancedFrame;
    private static volatile boolean frameInstancerInitialized = false;

    // ОПТИМИЗАЦИЯ: Переиспользуемые буферы для трансформаций (legacy режим)
    private final float[] translation = new float[3];
    private final float[] origin = new float[3];
    private final float[] rotation = new float[3];

    public DoorRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    // Инициализация instanced рендерера для frame
    private static synchronized void initializeFrameInstancerSync(DoorBakedModel model) {
        if (frameInstancerInitialized) return;

        try {
            BakedModel frameModel = model.getPart("frame");
            InstancedStaticPartRenderer tempFrame = null;
            
            if (frameModel != null) {
                var frameData = ObjModelVboBuilder.buildSinglePart(frameModel);
                tempFrame = new InstancedStaticPartRenderer(frameData);
            }

            // КРИТИЧНО: Устанавливаем поля ДО флага
            instancedFrame = tempFrame;
            
            // Memory barrier: все записи видны после этого
            frameInstancerInitialized = true;

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize door frame instanced renderer", e);
            instancedFrame = null;
        }
    }

    // Wrapper с double-check locking
    private void initializeFrameInstancer(DoorBakedModel model) {
        if (!frameInstancerInitialized) { // Первая проверка без лока
            initializeFrameInstancerSync(model);
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
        // MainRegistry.LOGGER.debug("DoorRenderer.renderParts called for controller: {}", be.isController());
        
        if (!be.isController()) return;
        
        DoorDecl doorDecl = be.getDoorDecl();
        // MainRegistry.LOGGER.debug("DoorDecl: {}", doorDecl != null ? "found" : "null");
        
        if (doorDecl == null) return;
        
        // String[] partNames = model.getPartNames();
        // MainRegistry.LOGGER.debug("Part names: {}", Arrays.toString(partNames));

        BlockPos blockPos = be.getBlockPos();

        // Occlusion culling check
        var minecraft = Minecraft.getInstance();
        AABB renderBounds = be.getRenderBoundingBox();
        if (!shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        float openTicks = be.getOpenProgress(partialTick) * doorDecl.getOpenTime();
        boolean isOpen = be.isOpen();

        // Применяем базовое смещение модели
        doorDecl.doOffsetTransform(animator);

        // Определяем путь рендеринга на основе совместимости
        boolean useFallback = RenderPathManager.shouldUseFallback();
        
        if (useFallback) {
            // ═══════════════════════════════════════════════════════
            // FALLBACK PATH: Immediate mode рендер (совместим с шейдерами)
            // ═══════════════════════════════════════════════════════
            renderWithFallback(be, model, doorDecl, openTicks, isOpen, poseStack, packedLight);
        } else {
            // ═══════════════════════════════════════════════════════
            // OPTIMIZED PATH: VBO рендер с кастомным шейдером
            // ═══════════════════════════════════════════════════════
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
            // Инициализируем instanced рендерер для frame если нужно
            if (!frameInstancerInitialized) {
                initializeFrameInstancer(model);
            }

            // Получаем кэшированный массив имён частей из модели
            String[] partNames = model.getPartNames();
            Level level = be.getLevel();
            
            for (String partName : partNames) {
                if (!doorDecl.doesRender(partName, isOpen)) continue;

                // Статическая часть frame рендерится через InstancedStaticPartRenderer
                if ("frame".equals(partName)) {
                    if (instancedFrame != null) {
                        instancedFrame.addInstance(poseStack, packedLight, blockPos, be);
                    } else {
                        // Fallback на immediate рендерер если instanced не удалось создать
                        renderFallbackPart(partName, model.getPart(partName), poseStack, packedLight, blockPos, level, be);
                    }
                } else if (!"Base".equals(partName)) { 
                    // Подвижные части (doorLeft, doorRight) рендерятся через обычный DoorVboRenderer
                    try {
                        DoorVboRenderer partRenderer = DoorVboRenderer.getOrCreate(model, partName);
                        partRenderer.renderPart(poseStack, packedLight, blockPos, be, doorDecl, openTicks, isOpen);
                    } catch (IllegalArgumentException e) {
                        MainRegistry.LOGGER.warn("Cannot create VBO renderer for part {}: {}", partName, e.getMessage());
                        // Fallback на immediate режим для этой части
                        renderFallbackPart(partName, model.getPart(partName), poseStack, packedLight, blockPos, level, be);
                    }
                }
            }

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error in VBO door render", e);
            // Fallback на immediate режим при ошибке
            renderWithFallback(be, model, doorDecl, openTicks, isOpen, poseStack, packedLight);
        }
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
            ImmediateFallbackRenderer.endBatch();
        }
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
            // Получаем/создаем fallback рендерер для этой части
            ImmediateFallbackRenderer renderer = fallbackRenderers.computeIfAbsent(
                    "door_" + partName,
                    k -> new ImmediateFallbackRenderer(partModel)
            );

            // ИСПРАВЛЕНО: Рендерим с occlusion culling поддержкой
            renderer.render(poseStack, packedLight, null, blockPos, level, blockEntity);

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error rendering fallback door part {}: {}", partName, e.getMessage());
            // Удаляем проблемный рендерер из кэша для повторного создания
            fallbackRenderers.remove("door_" + partName);
        }
    }

    private boolean shouldRender(BlockPos blockPos, net.minecraft.world.level.Level level, AABB renderBounds) {
        // Сначала используем продвинутый occlusion culling
        if (!OcclusionCullingHelper.shouldRender(blockPos, level, renderBounds)) {
            return false;
        }
        
        // Дополнительная проверка по дистанции как fallback
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double distanceSq = camera.getPosition().distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        double maxDistance = getViewDistance();
        return distanceSq <= maxDistance * maxDistance;
    }

    /**
     * ВАЖНО: Вызывать в конце рендера ВСЕХ дверей для флаша батчей
     */
    public static void flushInstancedBatches() {
        if (instancedFrame != null) instancedFrame.flush();
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
        if (instancedFrame != null) {
            instancedFrame.cleanup();
            instancedFrame = null;
        }
        frameInstancerInitialized = false;

        // Очищаем VBO кэш
        DoorVboRenderer.clearCache();

        // ИСПРАВЛЕНО: Правильная очистка fallback кэша
        // ImmediateFallbackRenderer не имеет метода cleanup() - просто очищаем карту
        fallbackRenderers.clear();
        ImmediateFallbackRenderer.clearGlobalCache(); // вместо ImmediateFallbackRenderer.clearCache()
        ImmediateFallbackRenderer.endFrame();

        // Принудительно сбрасываем глобальный Tesselator если нужно
        ImmediateFallbackRenderer.ensureBatchClosed();

        // Сбрасываем путь рендеринга
        RenderPathManager.reset();
    }

    /**
     * Статический метод для глобальной очистки (вызывается из регистрации событий)
     */
    public static void clearAllCaches() {
        // Очищаем instanced кэш
        if (instancedFrame != null) {
            instancedFrame.cleanup();
            instancedFrame = null;
        }
        frameInstancerInitialized = false;

        // Очищаем VBO кэш
        DoorVboRenderer.clearCache();

        // Очищаем fallback кэш
        fallbackRenderers.clear();

        // Принудительно сбрасываем Tesselator
        ImmediateFallbackRenderer.clearGlobalCache();
        ImmediateFallbackRenderer.endFrame();
    }
}
