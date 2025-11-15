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


@OnlyIn(Dist.CLIENT)
public class ImmediateFallbackRenderer {
    
    // ═══ ДАННЫЕ И КЭШИРОВАНИЕ ═══
    private final BakedModel model;
    private final List<OptimizedQuad> optimizedQuads;
    private final int quadCount;
    private final int modelHash;

    // ═══ ПОДДЕРЖКА ТЕНЕЙ (TODO) ═══
    private static final ThreadLocal<Boolean> RENDERING_SHADOWS = ThreadLocal.withInitial(() -> false);


    // ═══ BATCHING СИСТЕМА ═══
    private static final ThreadLocal<BatchRenderer> THREAD_BATCH = ThreadLocal.withInitial(BatchRenderer::new);

    // ═══ КЭШИРОВАНИЕ ═══
    private static final ThreadLocal<RandomSource> RANDOM_CACHE = ThreadLocal.withInitial(() -> {
        RandomSource random = RandomSource.create();
        random.setSeed(42L);
        return random;
    });
    
    private static final ConcurrentHashMap<Integer, List<OptimizedQuad>> QUAD_CACHE = new ConcurrentHashMap<>();

    // ═══ ОПТИМИЗИРОВАННЫЙ QUAD ═══
    private static class OptimizedQuad {
        final BakedQuad quad;
        final int[] vertexData;
        final int estimatedCost;

        OptimizedQuad(BakedQuad quad) {
            this.quad = quad;
            this.vertexData = quad.getVertices();
            this.estimatedCost = vertexData.length / 8;
        }
    }

    // ═══ BATCH РЕНДЕРЕР С ПОДДЕРЖКОЙ ТЕНЕЙ ═══
    private static class BatchRenderer {
        private final List<RenderTask> pendingTasks = new ArrayList<>(64);

        

        private static class RenderTask {
            final List<OptimizedQuad> quads;
            final PoseStack.Pose pose;
            final int packedLight;

            RenderTask(List<OptimizedQuad> quads, PoseStack.Pose pose, int packedLight,
                      BlockPos blockPos, AABB bounds) {
                this.quads = quads;
                this.pose = pose;
                this.packedLight = packedLight;
            }
        }

        void addTask(List<OptimizedQuad> quads, PoseStack.Pose pose, int packedLight,
                    BlockPos blockPos, AABB bounds) {
            pendingTasks.add(new RenderTask(quads, pose, packedLight, blockPos, bounds));
            
            if (pendingTasks.size() >= 16) {
                flushBatch();
            }
        }

        void flushBatch() {
            if (pendingTasks.isEmpty()) return;

            try {
                // всегда обновляем shadow state
                updateShadowState();
        
                // ВСЕГДА настраиваем GL-состояние перед отрисовкой
                if (isShadowPass()) {
                    setupShadowRenderState();
                } else {
                    setupOculusCompatibleRenderState();
                }
        
                renderBatchedTasks();
            } catch (Exception e) {
                MainRegistry.LOGGER.warn("Batch render error: {}", e.getMessage());
                safeResetTesselator();
            } finally {
                restoreRenderState();
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
                    // УПРОЩЕНО: Одинаковые настройки для всех режимов
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
            return false;
        }

        // КРИТИЧНОЕ ИСПРАВЛЕНИЕ: Настройка для shadow pass
        private void setupShadowRenderState() {
            // Используем более быстрые настройки для shadow pass
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            
            // КРИТИЧНО для теней: настройки блендинга
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 771); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
            
            // Быстрые depth настройки
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515); // GL_LEQUAL
            RenderSystem.depthMask(true);
            
            RenderSystem.disableCull();
            RenderSystem.activeTexture(33984); // GL_TEXTURE0
            RenderSystem.colorMask(true, true, true, true);
        }

        private void setupOculusCompatibleRenderState() {
            // Для дверей с прозрачными частями лучше использовать translucent‑шейдер
            // Можно оставить и solid, но главное — включить blending.
            RenderSystem.setShader(GameRenderer::getRendertypeTranslucentShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        
            // КРИТИЧНО: включаем альфа‑смешивание
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc(); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
        
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515); // GL_LEQUAL
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.activeTexture(33984); // GL_TEXTURE0
            RenderSystem.colorMask(true, true, true, true);
        }
        
        private void restoreRenderState() {
            RenderSystem.enableCull();
        
            // После нашего батча возвращаемся к "чистому" состоянию:
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        
            RenderSystem.depthFunc(513); // GL_LESS
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

    // ═══ SHADOW DETECTION МЕТОДЫ ═══
    
    /**
     * Проверяем, активен ли shadow pass через Oculus API
     */
    private static boolean isShadowPass() {
        // КРИТИЧНО: Всегда проверяем актуальное состояние
        return detectOculusShadowPass() || RENDERING_SHADOWS.get();
    }

    public static void onShaderReload() {
        // Очищаем все кэши при смене шейдера
        RENDERING_SHADOWS.set(false);
        
        // Очищаем renderer кэши
        clearGlobalCache();
        
        MainRegistry.LOGGER.info("ImmediateFallbackRenderer: Shader reload detected, caches cleared");
    }

    /**
     * КРИТИЧНО: Детекция shadow pass через Oculus reflection API
     */
    private static boolean detectOculusShadowPass() {
        try {
            Class<?> shadowRendererClass = Class.forName("net.irisshaders.iris.shadows.ShadowRenderer");
            var activeField = shadowRendererClass.getDeclaredField("ACTIVE");
            activeField.setAccessible(true);
            Boolean isActive = (Boolean) activeField.get(null);
            
            boolean currentlyActive = isActive != null && isActive;
            
            return currentlyActive;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Обновляем состояние shadow mode
     */
    private static void updateShadowState() {
        // НЕ используем интервальные проверки - проверяем каждый раз
        detectOculusShadowPass();
    }

    /**
     * НОВЫЙ API: Установить shadow mode (вызывается из hook'ов)
     */
    public static void setShadowMode(boolean shadowMode) {
        RENDERING_SHADOWS.set(shadowMode);
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
            
            // Получаем общие квады
            var generalQuads = model.getQuads(null, null, random, ModelData.EMPTY, null);
            for (BakedQuad quad : generalQuads) {
                quads.add(new OptimizedQuad(quad));
            }
            
            // Получаем квады для каждого направления
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

    // ═══ МЕТОДЫ РЕНДЕРИНГА ═══
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
            
            BatchRenderer batchRenderer = THREAD_BATCH.get();
            batchRenderer.addTask(optimizedQuads, poseStack.last(), packedLight, blockPos, renderBounds);
            
        } finally {
            poseStack.popPose();
        }
    }

    // ПЕРЕГРУЗКИ для совместимости
    public void render(PoseStack poseStack, int packedLight) {
        render(poseStack, packedLight, null, null, null, null);
    }

    public void render(PoseStack poseStack, int packedLight, @Nullable Matrix4f additionalTransform) {
        render(poseStack, packedLight, additionalTransform, null, null, null);
    }

    // ═══ СТАТИЧЕСКИЕ МЕТОДЫ ═══
    public static void endBatch() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        batchRenderer.flushBatch();
    }

    public static void endFrame() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        batchRenderer.flushBatch();
        RENDERING_SHADOWS.set(false); // Сбрасываем shadow state в конце кадра
    }

    public static void forceReset() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        batchRenderer.safeResetTesselator();
        batchRenderer.pendingTasks.clear();
        RENDERING_SHADOWS.set(false);
    }

    public static void ensureBatchClosed() {
        endBatch();
    }

    public static void clearGlobalCache() {
        QUAD_CACHE.clear();
        THREAD_BATCH.get().pendingTasks.clear();
        RENDERING_SHADOWS.set(false);
    }

    private static long getCurrentFrameId() {
        return System.nanoTime() / 16_666_666L;
    }

    // ═══ ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ═══
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

    // ═══ ИНФОРМАЦИОННЫЕ МЕТОДЫ ═══
    public int getQuadCount() {
        return quadCount;
    }

    public int getModelHash() {
        return modelHash;
    }

    public static void printCacheStats() {
        BatchRenderer batchRenderer = THREAD_BATCH.get();
        MainRegistry.LOGGER.info("ImmediateFallbackRenderer stats: {} models cached, {} pending tasks, shadow mode: {}",
                QUAD_CACHE.size(), batchRenderer.pendingTasks.size(), isShadowPass());
    }

    // ═══ SHADOW PASS API ═══
    
    /**
     * НОВЫЙ API: Уведомление о начале shadow pass
     */
    public static void beginShadowPass() {
        setShadowMode(true);
        MainRegistry.LOGGER.debug("ImmediateFallbackRenderer: Shadow pass started");
    }

    /**
     * НОВЫЙ API: Уведомление об окончании shadow pass  
     */
    public static void endShadowPass() {
        endBatch(); // Завершаем текущий batch
        setShadowMode(false);
        MainRegistry.LOGGER.debug("ImmediateFallbackRenderer: Shadow pass ended");
    }

    /**
     * Проверить, активен ли shadow pass
     */
    public static boolean isShadowPassActive() {
        return isShadowPass();
    }
}
