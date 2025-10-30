package com.hbm_m.client.render;

import com.hbm_m.block.entity.DoorBlockEntity;
import com.hbm_m.block.entity.DoorDecl;
import com.hbm_m.client.model.DoorBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class DoorVboRenderer extends AbstractGpuVboRenderer {

    private final DoorBakedModel model;
    private final String partName;
    private final String cacheKey;

    // Кэш VBO рендереров для каждой анимированной части двери (НЕ включая frame)
    private static final ConcurrentHashMap<String, DoorVboRenderer> partRenderers = new ConcurrentHashMap<>();

    // Переиспользуемые буферы для трансформаций (потокобезопасно через ThreadLocal)
    private static final ThreadLocal<float[]> translationBuffer = ThreadLocal.withInitial(() -> new float[3]);
    private static final ThreadLocal<float[]> originBuffer = ThreadLocal.withInitial(() -> new float[3]);
    private static final ThreadLocal<float[]> rotationBuffer = ThreadLocal.withInitial(() -> new float[3]);

    private DoorVboRenderer(DoorBakedModel model, String partName) {
        this.model = model;
        this.partName = partName;
        this.cacheKey = "door_" + partName;
    }

    /**
     * Получает или создает VBO рендерер для указанной анимированной части двери
     * ВАЖНО: НЕ используется для статической части "frame"!
     */
    public static DoorVboRenderer getOrCreate(DoorBakedModel model, String partName) {
        // Защита от создания VBO рендерера для статических частей
        if ("frame".equals(partName)) {
            throw new IllegalArgumentException("Frame part should use InstancedStaticPartRenderer, not DoorVboRenderer!");
        }
        
        // ИСПРАВЛЕНИЕ: Также пропускаем пустую часть "Base"
        if ("Base".equals(partName)) {
            throw new IllegalArgumentException("Base part is empty and should not be rendered!");
        }
    
        String key = "door_" + partName + "_" + System.identityHashCode(model);
        return partRenderers.computeIfAbsent(key, k -> new DoorVboRenderer(model, partName));
    }

    @Override
    protected VboData buildVboData() {
        BakedModel partModel = model.getPart(partName);
        if (partModel == null) {
            throw new IllegalStateException("Part model not found: " + partName);
        }

        // ИСПРАВЛЕНИЕ: Проверяем что модель содержит геометрию
        VboData vboData = ObjModelVboBuilder.buildSinglePart(partModel);
        if (vboData == null) {
            throw new IllegalStateException("No geometry found in part: " + partName);
        }
        
        return vboData;
    }

    /**
     * Рендерит анимированную часть двери с трансформациями
     */
    public void renderPart(PoseStack poseStack, int packedLight, BlockPos blockPos,
                          @Nullable BlockEntity blockEntity, DoorDecl doorDecl,
                          float openTicks, boolean isOpen) {
        
        if (!doorDecl.doesRender(partName, isOpen)) {
            return;
        }

        if (blockEntity instanceof DoorBlockEntity doorBE) {
            AABB renderBounds = doorBE.getRenderBoundingBox();
            if (!OcclusionCullingHelper.shouldRender(blockPos, 
                blockEntity.getLevel(), renderBounds)) {
                return;
            }
        }

        // Используем ThreadLocal буферы для потокобезопасности
        float[] translation = translationBuffer.get();
        float[] origin = originBuffer.get();
        float[] rotation = rotationBuffer.get();

        // Получаем трансформации для текущего состояния двери
        doorDecl.getTranslation(partName, openTicks, isOpen, translation);
        doorDecl.getOrigin(partName, origin);
        doorDecl.getRotation(partName, openTicks, rotation);

        // Применяем трансформации через матрицу
        poseStack.pushPose();

        // Смещение к точке вращения
        poseStack.translate(origin[0], origin[1], origin[2]);

        // Применяем повороты (оптимизированные условные проверки)
        if (rotation[0] != 0) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotation[0]));
        if (rotation[1] != 0) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation[1]));
        if (rotation[2] != 0) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation[2]));

        // Применяем смещение с учетом трансляции
        poseStack.translate(-origin[0] + translation[0], -origin[1] + translation[1], -origin[2] + translation[2]);

        // Рендерим с VBO
        super.render(poseStack, packedLight, blockPos, blockEntity);

        poseStack.popPose();
    }

    /**
     * Очищает кэш VBO рендереров при перезагрузке ресурсов
     */
    public static void clearCache() {
        partRenderers.values().forEach(DoorVboRenderer::cleanup);
        partRenderers.clear();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        
        // Очищаем ThreadLocal буферы при необходимости
        translationBuffer.remove();
        originBuffer.remove();
        rotationBuffer.remove();
    }
}
