package com.hbm_m.client.render;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.client.render.shader.IrisBufferHelper;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class GlobalMeshCache {

    // ИСПРАВЛЕНИЕ: Ограничиваем размер кэша
    private static final int MAX_CACHE_SIZE = 256;
    
    // Отдельные кэши для разных типов контента
    private static final ConcurrentHashMap<String, WeakReference<AbstractGpuVboRenderer>> PART_RENDERERS = new ConcurrentHashMap<>();
    private static final Map<String, List<BakedQuad>> COMPILED_QUADS = new ConcurrentHashMap<>();
    private static final Map<String, VertexBuffer> GPU_BUFFERS = new ConcurrentHashMap<>();
    private static final RandomSource RANDOM_SOURCE = RandomSource.create(0);

    // ==================== ОБРАТНАЯ СОВМЕСТИМОСТЬ: Старые методы ====================
    
    /**
     * УСТАРЕВШИЕ МЕТОДЫ для обратной совместимости с существующими рендерерами
     * Автоматически генерируют простые ключи без типов
     */
    
    public static List<BakedQuad> getOrCompile(String cacheKey, BakedModel modelPart) {
        // ИСПРАВЛЕНИЕ: Проверяем размер кэша
        if (COMPILED_QUADS.size() > MAX_CACHE_SIZE) {
            clearOldestQuads();
        }
        
        return COMPILED_QUADS.computeIfAbsent(cacheKey, k -> compileMesh(modelPart));
    }

    public static List<BakedQuad> getOrCompile(Class<?> modelClass, String partName, BakedModel modelPart) {
        String cacheKey = modelClass.getSimpleName() + ":" + partName;
        return getOrCompile(cacheKey, modelPart);
    }

    public static VertexBuffer getOrCreateGPUBuffer(String cacheKey, BakedModel modelPart) {
        // ИСПРАВЛЕНИЕ: Проверяем размер кэша
        if (GPU_BUFFERS.size() > MAX_CACHE_SIZE) {
            clearOldestBuffers();
        }
        
        return GPU_BUFFERS.computeIfAbsent(cacheKey, k -> {
            List<BakedQuad> quads = getOrCompile(cacheKey, modelPart);
            return uploadToGPU(quads);
        });
    }

    public static AbstractGpuVboRenderer getOrCreateRenderer(String partKey, BakedModel model) {
        if (FAILED_RENDERER_KEYS.contains(partKey)) return null;
        if (PART_RENDERERS.size() > MAX_CACHE_SIZE) {
            cleanupDeadRenderers();
        }
        
        WeakReference<AbstractGpuVboRenderer> ref = PART_RENDERERS.compute(partKey, (key, existingRef) -> {
            AbstractGpuVboRenderer renderer = (existingRef != null) ? existingRef.get() : null;
            if (renderer == null) {
                String partName = key.contains(":") ? key.substring(key.lastIndexOf(":") + 1) : key;
                renderer = createRendererForPart(model, partName);
                if (renderer == null) {
                    FAILED_RENDERER_KEYS.add(key);
                    return existingRef;
                }
                return new WeakReference<>(renderer);
            }
            return existingRef;
        });
        return (ref != null) ? ref.get() : null;
    }

    // ==================== НОВЫЕ МЕТОДЫ: Поддержка типов для дверей ====================
    
    /**
     * НОВЫЕ МЕТОДЫ с поддержкой типов для систем, которые требуют разделения кэша
     * (например, двери с разными типами)
     */
    
    public static List<BakedQuad> getOrCompile(String entityType, String partName, BakedModel modelPart) {
        String cacheKey = entityType + ":" + partName;
        return getOrCompile(cacheKey, modelPart);
    }

    public static VertexBuffer getOrCreateGPUBuffer(String entityType, String partName, BakedModel modelPart) {
        String cacheKey = entityType + ":" + partName;
        return getOrCreateGPUBuffer(cacheKey, modelPart);
    }

    public static AbstractGpuVboRenderer getOrCreateRenderer(String entityType, String partName, BakedModel model) {
        String partKey = entityType + ":" + partName;
        return getOrCreateRenderer(partKey, model);
    }

    // ==================== ВНУТРЕННИЕ МЕТОДЫ ====================
    
    private static List<BakedQuad> compileMesh(BakedModel modelPart) {
        if (modelPart == null) return Collections.emptyList();
        
        List<BakedQuad> quads = new ArrayList<>();
        quads.addAll(modelPart.getQuads(null, null, RANDOM_SOURCE, ModelData.EMPTY, RenderType.solid()));
        
        for (Direction direction : Direction.values()) {
            quads.addAll(modelPart.getQuads(null, direction, RANDOM_SOURCE, ModelData.EMPTY, RenderType.solid()));
        }
        
        return Collections.unmodifiableList(quads);
    }

    // ИСПРАВЛЕНИЕ: Добавлена очистка старых квадов
    private static void clearOldestQuads() {
        if (COMPILED_QUADS.isEmpty()) return;
        
        // Удаляем первые 25% кэша
        int toRemove = Math.max(1, COMPILED_QUADS.size() / 4);
        Iterator<String> it = COMPILED_QUADS.keySet().iterator();
        
        for (int i = 0; i < toRemove && it.hasNext(); i++) {
            String key = it.next();
            it.remove();
            
            // Также удаляем связанный GPU буфер если есть
            VertexBuffer vb = GPU_BUFFERS.remove(key);
            if (vb != null) {
                vb.close();
            }
        }
    }

    // ИСПРАВЛЕНИЕ: Добавлена очистка старых буферов
    private static void clearOldestBuffers() {
        if (GPU_BUFFERS.isEmpty()) return;
        
        int toRemove = Math.max(1, GPU_BUFFERS.size() / 4);
        Iterator<Map.Entry<String, VertexBuffer>> it = GPU_BUFFERS.entrySet().iterator();
        
        for (int i = 0; i < toRemove && it.hasNext(); i++) {
            Map.Entry<String, VertexBuffer> entry = it.next();
            VertexBuffer vb = entry.getValue();
            if (vb != null) {
                vb.close();
            }
            it.remove();
        }
    }

    private static VertexBuffer uploadToGPU(List<BakedQuad> quads) {
        if (quads.isEmpty()) return null;

        BufferBuilder builder = new BufferBuilder(quads.size() * 32);
        IrisBufferHelper.begin(builder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        PoseStack.Pose neutralPose = new PoseStack().last();
        float r = 1.0F, g = 1.0F, b = 1.0F, a = 1.0F;
        int neutralLight = 0;

        for (BakedQuad quad : quads) {
            builder.putBulkData(neutralPose, quad, r, g, b, a, neutralLight, OverlayTexture.NO_OVERLAY, false);
        }

        BufferBuilder.RenderedBuffer renderedBuffer = builder.end();
        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vbo.bind();
        vbo.upload(renderedBuffer);
        VertexBuffer.unbind();
        
        return vbo;
    }

    // ИСПРАВЛЕНИЕ: Добавлена очистка мёртвых WeakReference
    private static void cleanupDeadRenderers() {
        PART_RENDERERS.entrySet().removeIf(entry -> {
            AbstractGpuVboRenderer renderer = entry.getValue().get();
            if (renderer == null) {
                return true; // Удаляем мёртвые ссылки
            }
            return false;
        });
    }

    private static final java.util.Set<String> FAILED_RENDERER_KEYS = ConcurrentHashMap.newKeySet();

    private static AbstractGpuVboRenderer createRendererForPart(BakedModel model) {
        return createRendererForPart(model, "unknown");
    }
    
    private static AbstractGpuVboRenderer createRendererForPart(BakedModel model, String partName) {
        // ПРЕДВАРИТЕЛЬНАЯ проверка: есть ли квады в модели
        List<BakedQuad> quads = compileMesh(model);
        if (quads.isEmpty()) {
            return null;
        }
        
        // Создаем рендерер с ПРЕДВАРИТЕЛЬНО построенными данными
        final AbstractGpuVboRenderer.VboData prebuiltData = ObjModelVboBuilder.buildSinglePart(model, partName);
        if (prebuiltData == null) {
            return null;
        }
        
        MainRegistry.LOGGER.debug("GlobalMeshCache: Successfully created renderer for part '{}' with {} vertices", 
            partName, prebuiltData.byteBuffer.remaining() / 32);
        
        final List<BakedQuad> quadsForIris = quads;
        return new AbstractGpuVboRenderer() {
            private boolean dataInitialized = false;

            @Override
            protected AbstractGpuVboRenderer.VboData buildVboData() {
                if (!dataInitialized) {
                    dataInitialized = true;
                }
                return prebuiltData;
            }

            @Override
            protected List<BakedQuad> getQuadsForIrisPath() {
                return quadsForIris;
            }
        };
    }

    // ==================== ОЧИСТКА КЭША ====================
    
    public static void clear() {
        COMPILED_QUADS.clear();
        GPU_BUFFERS.values().forEach(vb -> { if (vb != null) vb.close(); });
        GPU_BUFFERS.clear();
    }

    /**
     * Очистить весь кэш (вызывать при reload ресурсов)
     */
    public static void clearAll() {
        for (WeakReference<AbstractGpuVboRenderer> ref : PART_RENDERERS.values()) {
            AbstractGpuVboRenderer renderer = ref.get();
            if (renderer != null) {
                renderer.cleanup();
            }
        }
        PART_RENDERERS.clear();
        FAILED_RENDERER_KEYS.clear();
        clear(); // Очищаем также quads и buffers
    }

    // ==================== МЕТОДЫ ДЛЯ СТАТИСТИКИ И ОТЛАДКИ ====================
    
    public static int getCachedQuadsCount() {
        return COMPILED_QUADS.size();
    }

    public static int getCachedBuffersCount() {
        return GPU_BUFFERS.size();
    }

    public static int getCachedRenderersCount() {
        return PART_RENDERERS.size();
    }

    public static void logCacheStats() {
        System.out.println("GlobalMeshCache stats:");
        System.out.println("  Compiled quads: " + getCachedQuadsCount());
        System.out.println("  GPU buffers: " + getCachedBuffersCount());
        System.out.println("  Renderers: " + getCachedRenderersCount());
    }
}
