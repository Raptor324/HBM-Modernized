package com.hbm_m.client.model.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalMeshCache {

    private static final Map<String, List<BakedQuad>> COMPILED_QUADS = new ConcurrentHashMap<>();
    private static final Map<String, VertexBuffer> GPU_BUFFERS = new ConcurrentHashMap<>();
    private static final RandomSource RANDOM_SOURCE = RandomSource.create(0);

    public static List<BakedQuad> getOrCompile(String cacheKey, BakedModel modelPart) {
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

    // ==================== GPU MESH БЕЗ запечённого света! ====================

    public static VertexBuffer getOrCreateGPUBuffer(String cacheKey, BakedModel modelPart) {
        return GPU_BUFFERS.computeIfAbsent(cacheKey, k -> {
            List<BakedQuad> quads = getOrCompile(cacheKey, modelPart);
            return uploadToGPU(quads);
        });
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
}
