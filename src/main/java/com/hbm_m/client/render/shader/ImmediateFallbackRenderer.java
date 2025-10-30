package com.hbm_m.client.render.shader;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.client.render.OcclusionCullingHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * СОВМЕСТИМАЯ И ОПТИМИЗИРОВАННАЯ версия ImmediateFallbackRenderer v3.0
 * 
 * ИСПРАВЛЕНИЯ:
 * - Восстановлена совместимость с существующими API
 * - Добавлены недостающие методы для интеграции
 * - Исправлен lifecycle management для batching
 * - Добавлена поддержка всех сигнатур методов render
 */
@OnlyIn(Dist.CLIENT)
public class ImmediateFallbackRenderer {
    
    // ═══ КЭШИРОВАНИЕ И ДАННЫЕ ═══
    private final BakedModel model;
    private final List<OptimizedQuad> optimizedQuads;
    private final int quadCount;
    private final int modelHash;
    
    // ═══ BATCHING СИСТЕМА ═══
    private static final ThreadLocal<BatchRenderer> THREAD_BATCH = ThreadLocal.withInitial(BatchRenderer::new);
    private static volatile long lastFrameId = -1;
    
    // ═══ КЭШИРОВАНИЕ ═══
    private static final ThreadLocal<RandomSource> RANDOM_CACHE = ThreadLocal.withInitial(() -> {
        RandomSource random = RandomSource.create();
        random.setSeed(42L);
        return random;
    });
    
    private static final ConcurrentHashMap<Integer, List<OptimizedQuad>> QUAD_CACHE = new ConcurrentHashMap<>();
    
    // ═══ ОПТИМИЗИРОВАННЫЙ QUAD С ПРЕДКОМПИЛИРОВАННЫМИ ДАННЫМИ ═══
    private static class OptimizedQuad {
        final BakedQuad quad;
        final int[] vertexData;           
        final boolean hasComplexGeometry;
        final int estimatedCost;
        
        OptimizedQuad(BakedQuad quad) {
            this.quad = quad;
            this.vertexData = quad.getVertices();
            this.hasComplexGeometry = vertexData.length > 32; 
            this.estimatedCost = vertexData.length / 8;
        }
    }
    
    // ═══ THREAD-LOCAL BATCH РЕНДЕРЕР ═══
    private static class BatchRenderer {
        private final List<RenderTask> pendingTasks = new ArrayList<>(64);
        private boolean glStateInitialized = false;
        private long lastFlushFrame = -1;
        
        private static class RenderTask {
            final List<OptimizedQuad> quads;
            final PoseStack.Pose pose;
            final int packedLight;
            final BlockPos blockPos;
            final AABB bounds;
            
            RenderTask(List<OptimizedQuad> quads, PoseStack.Pose pose, int packedLight, 
                      BlockPos blockPos, AABB bounds) {
                this.quads = quads;
                this.pose = pose;
                this.packedLight = packedLight;
                this.blockPos = blockPos;
                this.bounds = bounds;
            }
        }
        
        void addTask(List<OptimizedQuad> quads, PoseStack.Pose pose, int packedLight,
                    BlockPos blockPos, AABB bounds) {
            pendingTasks.add(new RenderTask(quads, pose, packedLight, blockPos, bounds));
            
            // Auto-flush при достижении batch лимита
            if (pendingTasks.size() >= 16) {
                flushBatch();
            }
        }
        
        void flushBatch() {
            if (pendingTasks.isEmpty()) return;
            
            long currentFrame = getCurrentFrameId();
            
            try {
                if (!glStateInitialized || lastFlushFrame != currentFrame) {
                    setupOptimizedRenderState();
                    glStateInitialized = true;
                    lastFlushFrame = currentFrame;
                }
                
                renderBatchedTasks();
                
            } catch (Exception e) {
                MainRegistry.LOGGER.warn("Batch render error: {}", e.getMessage());
                safeResetTesselator();
            } finally {
                pendingTasks.clear();
            }
        }
        
        private void renderBatchedTasks() {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            
            if (buffer.building()) {
                try {
                    BufferUploader.drawWithShader(buffer.end());
                } catch (Exception e) {
                    buffer.discard();
                }
            }
            
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            
            int totalQuads = 0;
            for (RenderTask task : pendingTasks) {
                if (shouldSkipTask(task)) continue;
                
                for (OptimizedQuad quad : task.quads) {
                    buffer.putBulkData(
                        task.pose, quad.quad,
                        1.0f, 1.0f, 1.0f, 1.0f, 
                        task.packedLight,
                        OverlayTexture.NO_OVERLAY,
                        true
                    );
                    totalQuads++;
                }
            }
            
            if (totalQuads > 0) {
                var builtBuffer = buffer.end();
                if (builtBuffer.drawState().vertexCount() > 0) {
                    BufferUploader.drawWithShader(builtBuffer);
                }
            } else {
                buffer.discard();
            }
        }
        
        private boolean shouldSkipTask(RenderTask task) {
            return false; // Простая реализация
        }
        
        private void setupOptimizedRenderState() {
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
        
        private void safeResetTesselator() {
            try {
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder buffer = tesselator.getBuilder();
                if (buffer.building()) {
                    buffer.discard();
                }
            } catch (Exception ignored) {}
        }
    }
    
    // ═══ КОНСТРУКТОР ═══
    public ImmediateFallbackRenderer(BakedModel model) {
        this.model = model;
        this.modelHash = model != null ? model.hashCode() : 0;
        
        if (model != null) {
            this.optimizedQuads = QUAD_CACHE.computeIfAbsent(modelHash, hash -> buildOptimizedQuads(model));
        } else {
            this.optimizedQuads = new ArrayList<>();
        }
        
        this.quadCount = optimizedQuads.size();
    }
    
    private static List<OptimizedQuad> buildOptimizedQuads(BakedModel model) {
        List<OptimizedQuad> quads = new ArrayList<>();
        RandomSource random = RANDOM_CACHE.get();
        
        try {
            random.setSeed(42L);
            var generalQuads = model.getQuads(null, null, random, ModelData.EMPTY, null);
            for (BakedQuad quad : generalQuads) {
                quads.add(new OptimizedQuad(quad));
            }
            
            for (Direction direction : Direction.values()) {
                random.setSeed(42L);
                var directionQuads = model.getQuads(null, direction, random, ModelData.EMPTY, null);
                for (BakedQuad quad : directionQuads) {
                    quads.add(new OptimizedQuad(quad));
                }
            }
            
            quads.sort((a, b) -> Integer.compare(a.estimatedCost, b.estimatedCost));
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error building optimized quads", e);
        }
        
        return quads;
    }
    
    // ═══ СОВМЕСТИМЫЕ МЕТОДЫ РЕНДЕРИНГА ═══
    
    /**
     * ОСНОВНОЙ МЕТОД для совместимости с DoorRenderer
     */
    public void render(PoseStack poseStack, int packedLight, @Nullable Matrix4f additionalTransform,
                      @Nullable BlockPos blockPos, @Nullable Level level, @Nullable BlockEntity blockEntity) {
        if (model == null || quadCount == 0 || optimizedQuads.isEmpty()) {
            return;
        }
        
        // Occlusion culling
        AABB renderBounds = null;
        if (blockPos != null && level != null && blockEntity != null) {
            renderBounds = blockEntity.getRenderBoundingBox();
            if (!OcclusionCullingHelper.shouldRender(blockPos, level, renderBounds)) {
                return;
            }
        }
        
        poseStack.pushPose();
        try {
            if (additionalTransform != null) {
                poseStack.last().pose().mul(additionalTransform);
            }
            
            // КЛЮЧЕВАЯ ОПТИМИЗАЦИЯ: Добавляем в batch
            BatchRenderer batchRenderer = THREAD_BATCH.get();
            batchRenderer.addTask(optimizedQuads, poseStack.last(), packedLight, blockPos, renderBounds);
            
        } finally {
            poseStack.popPose();
        }
    }
    
    // ПЕРЕГРУЗКИ для совместимости с существующим кодом
    public void render(PoseStack poseStack, int packedLight) {
        render(poseStack, packedLight, null, null, null, null);
    }
    
    public void render(PoseStack poseStack, int packedLight, @Nullable Matrix4f additionalTransform) {
        render(poseStack, packedLight, additionalTransform, null, null, null);
    }
    
    // ═══ СТАТИЧЕСКИЕ МЕТОДЫ СОВМЕСТИМОСТИ ═══
    
    /**
     * КРИТИЧНО: Вызывать после рендера всех объектов в кадре
     */
    public static void endBatch() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        batchRenderer.flushBatch();
    }
    
    /**
     * КРИТИЧНО: Вызывать в конце каждого кадра
     */
    public static void endFrame() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        batchRenderer.flushBatch();
        lastFrameId = getCurrentFrameId();
    }
    
    public static void forceReset() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        batchRenderer.safeResetTesselator();
        batchRenderer.pendingTasks.clear();
        batchRenderer.glStateInitialized = false;
    }
    
    public static void ensureBatchClosed() {
        endBatch();
    }
    
    /**
     * СОВМЕСТИМОСТЬ: Метод для очистки кэша - ОБЯЗАТЕЛЬНЫЙ
     */
    public static void clearGlobalCache() {
        QUAD_CACHE.clear();
        THREAD_BATCH.get().pendingTasks.clear();
        THREAD_BATCH.get().glStateInitialized = false;
    }
    
    private static long getCurrentFrameId() {
        return System.nanoTime() / 16_666_666L; 
    }
    
    // ═══ ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ СОВМЕСТИМОСТИ ═══
    public static boolean beginBatch() {
        return false; 
    }
    
    public static boolean isBatchActive() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        return !batchRenderer.pendingTasks.isEmpty();
    }
    
    public static int getBatchedQuadCount() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        return batchRenderer.pendingTasks.stream()
                .mapToInt(task -> task.quads.size())
                .sum();
    }
    
    // ═══ ГЕТТЕРЫ ═══
    public int getQuadCount() {
        return quadCount;
    }
    
    public int getModelHash() {
        return modelHash;
    }
    
    public static void printCacheStats() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        MainRegistry.LOGGER.info("ImmediateFallbackRenderer stats: {} models cached, {} pending tasks", 
                                QUAD_CACHE.size(), batchRenderer.pendingTasks.size());
    }
}
