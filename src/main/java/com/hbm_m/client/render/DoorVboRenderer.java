package com.hbm_m.client.render;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.custom.doors.DoorDecl;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DoorVboRenderer extends AbstractGpuVboRenderer {

    private final DoorBakedModel model;
    private final String partName;
    private final String quadCacheKey;

    // Кэш VBO рендереров для каждой анимированной части двери (НЕ включая frame)
    private static final ConcurrentHashMap<String, DoorVboRenderer> partRenderers = new ConcurrentHashMap<>();

    // Переиспользуемые буферы для трансформаций (потокобезопасно через ThreadLocal)
    private static final ThreadLocal<float[]> translationBuffer = ThreadLocal.withInitial(() -> new float[3]);
    private static final ThreadLocal<float[]> originBuffer = ThreadLocal.withInitial(() -> new float[3]);
    private static final ThreadLocal<float[]> rotationBuffer = ThreadLocal.withInitial(() -> new float[3]);

    private DoorVboRenderer(DoorBakedModel model, String partName, String quadCacheKey) {
        this.model = model;
        this.partName = partName;
        this.quadCacheKey = quadCacheKey;
    }

    /**
     * Получает или создает VBO рендерер для указанной анимированной части двери
     * ВАЖНО: НЕ используется для статической части "frame"!
     */
    public static DoorVboRenderer getOrCreate(DoorBakedModel model, String partName, String doorType) {
        // Защита от создания VBO рендерера для статических частей
        // ИСПРАВЛЕНИЕ: Проверяем все варианты регистра
        if (isStaticPart(partName)) {
            throw new IllegalArgumentException("Static part '" + partName + "' should use InstancedStaticPartRenderer, not DoorVboRenderer!");
        }
    
        String key = "door_" + doorType + "_" + partName;
        return partRenderers.computeIfAbsent(key, k -> new DoorVboRenderer(model, partName, key));
    }

    /**
     * Получает или создает VBO рендерер с поддержкой выбора модели.
     * Включает тип модели и скин в ключ кэша.
     */
    public static DoorVboRenderer getOrCreate(DoorBakedModel model, String partName, 
                                              String doorType, DoorModelSelection selection) {
        // Защита от создания VBO рендерера для статических частей
        // ИСПРАВЛЕНИЕ: Проверяем все варианты регистра
        if (isStaticPart(partName)) {
            throw new IllegalArgumentException("Static part '" + partName + "' should use InstancedStaticPartRenderer!");
        }
    
        String key = "door_" + doorType + "_" + selection.getModelType().getId() + 
                     "_" + selection.getSkin().getId() + "_" + partName;
        return partRenderers.computeIfAbsent(key, k -> new DoorVboRenderer(model, partName, key));
    }
    
    /**
     * Проверяет, является ли часть статической (не должна рендериться через DoorVboRenderer)
     */
    public static boolean isStaticPart(String partName) {
        return "frame".equalsIgnoreCase(partName) || 
               "DoorFrame".equals(partName) ||
               "Base".equals(partName);
    }

    public static DoorVboRenderer getOrCreate(DoorBakedModel model, String partName) {
        return getOrCreate(model, partName, "unknown");
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

    @Override
    protected List<BakedQuad> getQuadsForIrisPath() {
        BakedModel partModel = model.getPart(partName);
        if (partModel == null) return null;
        return GlobalMeshCache.getOrCompile(quadCacheKey, partModel);
    }

    // Рендер геометрии в текущей позе.
    public void renderPart(PoseStack poseStack, int packedLight, BlockPos blockPos,
                        @Nullable BlockEntity blockEntity, DoorDecl doorDecl,
                        float openTicks, boolean child, @Nullable MultiBufferSource bufferSource) {
        // Всё ещё уважаем doesRender, чтобы не рисовать скрытые части
        if (!doorDecl.doesRender(partName, child)) return;

        // НИКАКИХ дополнительных трансформаций: PoseStack уже содержит
        // origin/rotation/translation из DoorRenderer.renderHierarchyVbo(...)
        super.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
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
