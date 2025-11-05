package com.hbm_m.client.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class GlobalMeshCache {

    // ИСПРАВЛЕНИЕ: Ограничиваем размер кэша
    private static final int MAX_CACHE_SIZE = 256;
    
    private static final ConcurrentHashMap<String, WeakReference<AbstractGpuVboRenderer>> PART_RENDERERS = new ConcurrentHashMap<>();
    private static final Map<String, List<BakedQuad>> COMPILED_QUADS = new ConcurrentHashMap<>();
    private static final Map<String, VertexBuffer> GPU_BUFFERS = new ConcurrentHashMap<>();
    private static final RandomSource RANDOM_SOURCE = RandomSource.create(0);

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

    private static List<BakedQuad> compileMesh(BakedModel modelPart) {
        if (modelPart == null) return Collections.emptyList();
        
        List<BakedQuad> quads = new ArrayList<>();
        quads.addAll(modelPart.getQuads(null, null, RANDOM_SOURCE, ModelData.EMPTY, null));
        
        for (Direction direction : Direction.values()) {
            quads.addAll(modelPart.getQuads(null, direction, RANDOM_SOURCE, ModelData.EMPTY, null));
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

    // ==================== GPU MESH БЕЗ запечённого света! ====================
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
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

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

    public static void clear() {
        COMPILED_QUADS.clear();
        GPU_BUFFERS.values().forEach(vb -> { if (vb != null) vb.close(); });
        GPU_BUFFERS.clear();
    }

    public static AbstractGpuVboRenderer getOrCreateRenderer(String partKey, BakedModel model) {
        // ИСПРАВЛЕНИЕ: Проверяем размер кэша рендереров
        if (PART_RENDERERS.size() > MAX_CACHE_SIZE) {
            cleanupDeadRenderers();
        }
        
        return PART_RENDERERS.compute(partKey, (key, existingRef) -> {
            AbstractGpuVboRenderer renderer = (existingRef != null) ? existingRef.get() : null;
            
            if (renderer == null) {
                renderer = createRendererForPart(model);
                return new WeakReference<>(renderer);
            }
            
            return existingRef;
        }).get();
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

    private static AbstractGpuVboRenderer createRendererForPart(BakedModel model) {
        return new AbstractGpuVboRenderer() {
            private VboData cachedData;

            @Override
            protected VboData buildVboData() {
                if (cachedData == null) {
                    cachedData = ObjModelVboBuilder.buildSinglePart(model);
                }
                return cachedData;
            }
        };
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
        clear(); // Очищаем также quads и buffers
    }
}
