package com.hbm_m.client.render.shader;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import com.hbm_m.main.MainRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ ИСПРАВЛЕН: Memory-safe immediate рендер
 * 
 * КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ:
 * - Переиспользование глобального Tesselator
 * - Правильное освобождение нативной памяти
 * - Защита от memory leak
 * - Минимальные аллокации
 */
@OnlyIn(Dist.CLIENT)
public class ImmediateFallbackRenderer {
    
    private final BakedModel model;
    private final List<BakedQuad> cachedQuads;
    private final int quadCount;
    
    // ✅ КРИТИЧНО: Используем ОДИН глобальный Tesselator вместо создания новых
    private static final Object TESSELATOR_LOCK = new Object();
    
    public ImmediateFallbackRenderer(BakedModel model) {
        this.model = model;
        this.cachedQuads = new ArrayList<>();
        
        if (model != null) {
            // Кэшируем все квады один раз при создании
            RandomSource random = RandomSource.create();
            random.setSeed(42L);
            
            // Добавляем квады для всех сторон
            this.cachedQuads.addAll(model.getQuads(null, null, random, ModelData.EMPTY, null));
            
            for (Direction direction : Direction.values()) {
                random.setSeed(42L);
                this.cachedQuads.addAll(model.getQuads(null, direction, random, ModelData.EMPTY, null));
            }
        }
        
        this.quadCount = cachedQuads.size();
    }
    
    /**
     * ✅ ОСНОВНОЙ РЕНДЕР - максимально простой
     */
    public void render(PoseStack poseStack, int packedLight) {
        render(poseStack, packedLight, null);
    }
    
    /**
     * ✅ РЕНДЕР С ДОПОЛНИТЕЛЬНОЙ ТРАНСФОРМАЦИЕЙ
     */
    public void render(PoseStack poseStack, int packedLight, Matrix4f additionalTransform) {
        if (model == null || quadCount == 0 || cachedQuads.isEmpty()) {
            return;
        }
        
        // Применяем трансформацию если есть
        poseStack.pushPose();
        try {
            if (additionalTransform != null) {
                poseStack.last().pose().mul(additionalTransform);
            }
            
            // ✅ MEMORY-SAFE рендер
            renderMemorySafe(poseStack.last(), packedLight);
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error in immediate fallback render", e);
        } finally {
            poseStack.popPose();
        }
    }
    
    /**
     * ✅ КРИТИЧНО: Memory-safe рендер без утечек памяти
     */
    private void renderMemorySafe(PoseStack.Pose pose, int packedLight) {
        // ✅ ИСПОЛЬЗУЕМ ГЛОБАЛЬНЫЙ Tesselator - НЕ создаем новый!
        synchronized (TESSELATOR_LOCK) {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            
            try {
                // ✅ Минимальная настройка GL состояния
                setupSimpleRenderState();
                
                // ✅ КРИТИЧНО: Сначала проверяем что буфер свободен
                if (buffer.building()) {
                    MainRegistry.LOGGER.warn("Tesselator already building! Force ending...");
                    try {
                        // Принудительно завершаем предыдущий рендер
                        BufferUploader.drawWithShader(buffer.end());
                    } catch (Exception e) {
                        MainRegistry.LOGGER.error("Error force-ending buffer", e);
                    }
                }
                
                // Начинаем рендер
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                
                // Добавляем все квады
                for (BakedQuad quad : cachedQuads) {
                    buffer.putBulkData(
                        pose,
                        quad,
                        1.0f, 1.0f, 1.0f, 1.0f,      // Белый цвет
                        packedLight,                  // Освещение
                        OverlayTexture.NO_OVERLAY,    // Без overlay
                        true                          // hasOverlay
                    );
                }
                
                // Рендерим буфер
                var builtBuffer = buffer.end();
                if (builtBuffer.drawState().vertexCount() > 0) {
                    BufferUploader.drawWithShader(builtBuffer);
                }
                
            } catch (IllegalStateException e) {
                MainRegistry.LOGGER.warn("BufferBuilder conflict: {}", e.getMessage());
                // ✅ При конфликте пытаемся сбросить состояние
                try {
                    if (buffer.building()) {
                        buffer.discard();
                    }
                } catch (Exception resetError) {
                    MainRegistry.LOGGER.error("Error resetting buffer", resetError);
                }
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Unexpected error in memory-safe render", e);
            }
        }
    }
    
    /**
     * ✅ МИНИМАЛЬНАЯ НАСТРОЙКА GL СОСТОЯНИЯ
     */
    private void setupSimpleRenderState() {
        // Устанавливаем только НЕОБХОДИМОЕ для блоков
        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        
        // Базовые настройки для правильного рендера блоков
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }
    
    public int getQuadCount() {
        return quadCount;
    }
    
    // ═══ STUB МЕТОДЫ ДЛЯ СОВМЕСТИМОСТИ ═══
    
    public static boolean beginBatch() {
        return false;
    }
    
    public static void endBatch() {
        // Ничего не делаем
    }
    
    public static boolean isBatchActive() {
        return false;
    }
    
    public static void forceReset() {
        // ✅ ДОБАВЛЕНО: Принудительный сброс глобального Tesselator
        synchronized (TESSELATOR_LOCK) {
            try {
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder buffer = tesselator.getBuilder();
                if (buffer.building()) {
                    MainRegistry.LOGGER.warn("Force resetting global Tesselator");
                    buffer.discard();
                }
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Error in force reset", e);
            }
        }
    }
    
    public static void ensureBatchClosed() {
        // ✅ ДОБАВЛЕНО: Убеждаемся что глобальный Tesselator не занят
        forceReset();
    }
    
    public static int getBatchedQuadCount() {
        return 0;
    }
}
