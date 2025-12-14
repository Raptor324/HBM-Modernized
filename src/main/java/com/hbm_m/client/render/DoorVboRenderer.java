package com.hbm_m.client.render;

import com.hbm_m.block.entity.custom.doors.DoorDecl;
import com.hbm_m.client.model.DoorBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
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
    public static DoorVboRenderer getOrCreate(DoorBakedModel model, String partName, String doorType) {
        // Защита от создания VBO рендерера для статических частей
        if ("frame".equals(partName)) {
            throw new IllegalArgumentException("Frame part should use InstancedStaticPartRenderer, not DoorVboRenderer!");
        }
        
        if ("Base".equals(partName)) {
            throw new IllegalArgumentException("Base part is empty and should not be rendered!");
        }
    
        // ИСПРАВЛЕНИЕ: Включаем тип двери в ключ кэша
        String key = "door_" + doorType + "_" + partName + "_" + System.identityHashCode(model);
        return partRenderers.computeIfAbsent(key, k -> new DoorVboRenderer(model, partName));
    }

    public static DoorVboRenderer getOrCreate(DoorBakedModel model, String partName) {
        // Пытаемся определить тип двери по модели
        String doorType = "unknown_" + System.identityHashCode(model);
        return getOrCreate(model, partName, doorType);
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

    // Рендер геометрии в текущей позе.
    public void renderPart(PoseStack poseStack, int packedLight, BlockPos blockPos,
                        @Nullable BlockEntity blockEntity, DoorDecl doorDecl,
                        float openTicks, boolean child) {
        // Всё ещё уважаем doesRender, чтобы не рисовать скрытые части
        if (!doorDecl.doesRender(partName, child)) return;

        // НИКАКИХ дополнительных трансформаций: PoseStack уже содержит
        // origin/rotation/translation из DoorRenderer.renderHierarchyVbo(...)
        super.render(poseStack, packedLight, blockPos, blockEntity);
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
