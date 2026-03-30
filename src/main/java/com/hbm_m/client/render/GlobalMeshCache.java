package com.hbm_m.client.render;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.client.render.shader.IrisBufferHelper;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlobalMeshCache {

    private static final int MAX_CACHE_SIZE = 256;
    private static final ConcurrentHashMap<String, WeakReference<SingleMeshVboRenderer>> PART_RENDERERS = new ConcurrentHashMap<>();
    private static final java.util.Set<String> FAILED_RENDERER_KEYS = ConcurrentHashMap.newKeySet();

    private static final Map<String, PartGeometry> COMPILED_GEOMETRY = Collections.synchronizedMap(
        new LinkedHashMap<String, PartGeometry>(MAX_CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, PartGeometry> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        }
    );

    private static final Map<String, VertexBuffer> GPU_BUFFERS = Collections.synchronizedMap(
        new LinkedHashMap<String, VertexBuffer>(MAX_CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, VertexBuffer> eldest) {
                if (size() > MAX_CACHE_SIZE) {
                    VertexBuffer vb = eldest.getValue();
                    if (vb != null) vb.close();
                    return true;
                }
                return false;
            }
        }
    );

    public static List<BakedQuad> getOrCompile(String cacheKey, BakedModel modelPart) {
        return getOrCompilePartGeometry(cacheKey, modelPart).solidQuads();
    }

    public static List<BakedQuad> getOrCompile(Class<?> modelClass, String partName, BakedModel modelPart) {
        String cacheKey = modelClass.getSimpleName() + ":" + partName;
        return getOrCompile(cacheKey, modelPart);
    }

    /**
     * Один кэшированный проход getQuads + общий список для Iris/VBO.
     */
    public static PartGeometry getOrCompilePartGeometry(String cacheKey, BakedModel modelPart) {
        if (modelPart == null) {
            return PartGeometry.EMPTY;
        }
        return COMPILED_GEOMETRY.computeIfAbsent(cacheKey,
            k -> PartGeometry.compile(modelPart, partNameFromKey(cacheKey)));
    }

    public static VertexBuffer getOrCreateGPUBuffer(String cacheKey, BakedModel modelPart) {
        RenderSystem.assertOnRenderThread();

        return GPU_BUFFERS.computeIfAbsent(cacheKey, k -> {
            List<BakedQuad> quads = getOrCompile(cacheKey, modelPart);
            return uploadToGPU(quads);
        });
    }

    public static SingleMeshVboRenderer getOrCreateRenderer(String partKey, BakedModel model) {
        if (FAILED_RENDERER_KEYS.contains(partKey)) return null;
        if (PART_RENDERERS.size() > MAX_CACHE_SIZE) {
            cleanupDeadRenderers();
        }

        WeakReference<SingleMeshVboRenderer> ref = PART_RENDERERS.compute(partKey, (key, existingRef) -> {
            SingleMeshVboRenderer renderer = (existingRef != null) ? existingRef.get() : null;
            if (renderer == null) {
                renderer = createRendererForPart(key, model);
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

    public static List<BakedQuad> getOrCompile(String entityType, String partName, BakedModel modelPart) {
        String cacheKey = entityType + ":" + partName;
        return getOrCompile(cacheKey, modelPart);
    }

    public static PartGeometry getOrCompilePartGeometry(String entityType, String partName, BakedModel modelPart) {
        return getOrCompilePartGeometry(entityType + ":" + partName, modelPart);
    }

    public static VertexBuffer getOrCreateGPUBuffer(String entityType, String partName, BakedModel modelPart) {
        String cacheKey = entityType + ":" + partName;
        return getOrCreateGPUBuffer(cacheKey, modelPart);
    }

    public static SingleMeshVboRenderer getOrCreateRenderer(String entityType, String partName, BakedModel model) {
        String partKey = entityType + ":" + partName;
        return getOrCreateRenderer(partKey, model);
    }

    private static String partNameFromKey(String cacheKey) {
        return cacheKey.contains(":") ? cacheKey.substring(cacheKey.lastIndexOf(":") + 1) : cacheKey;
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

    private static void cleanupDeadRenderers() {
        PART_RENDERERS.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }

    private static SingleMeshVboRenderer createRendererForPart(String partKey, BakedModel model) {
        PartGeometry geo = getOrCompilePartGeometry(partKey, model);
        if (geo.isEmpty()) {
            return null;
        }

        String partName = partNameFromKey(partKey);
        final SingleMeshVboRenderer.VboData prebuiltData = geo.toVboData(partName);
        if (prebuiltData == null) {
            return null;
        }

        MainRegistry.LOGGER.debug("GlobalMeshCache: renderer for part '{}', {} vertices",
            partName, prebuiltData.byteBuffer.remaining() / 32);

        final List<BakedQuad> quadsForIris = geo.solidQuads();
        return new SingleMeshVboRenderer() {
            private boolean dataInitialized = false;

            @Override
            protected SingleMeshVboRenderer.VboData buildVboData() {
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

    public static void clear() {
        COMPILED_GEOMETRY.clear();
        GPU_BUFFERS.values().forEach(vb -> { if (vb != null) vb.close(); });
        GPU_BUFFERS.clear();
    }

    public static void clearAll() {
        for (WeakReference<SingleMeshVboRenderer> ref : PART_RENDERERS.values()) {
            SingleMeshVboRenderer renderer = ref.get();
            if (renderer != null) {
                renderer.cleanup();
            }
        }
        PART_RENDERERS.clear();
        FAILED_RENDERER_KEYS.clear();
        clear();
    }

    public static int getCachedQuadsCount() {
        return COMPILED_GEOMETRY.size();
    }

    public static int getCachedBuffersCount() {
        return GPU_BUFFERS.size();
    }

    public static int getCachedRenderersCount() {
        return PART_RENDERERS.size();
    }

    public static void logCacheStats() {
        MainRegistry.LOGGER.debug("GlobalMeshCache stats:");
        MainRegistry.LOGGER.debug("  Compiled part geometry entries: " + getCachedQuadsCount());
        MainRegistry.LOGGER.debug("  GPU buffers: " + getCachedBuffersCount());
        MainRegistry.LOGGER.debug("  Renderers: " + getCachedRenderersCount());
    }
}
