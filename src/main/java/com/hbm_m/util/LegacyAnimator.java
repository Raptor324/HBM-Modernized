package com.hbm_m.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.List;

public class LegacyAnimator {
    private final PoseStack poseStack;
    private final MultiBufferSource bufferSource;
    private final BlockRenderDispatcher blockRenderer;
    private final int packedLight;
    private final int packedOverlay;
    
    // Диапазон clipping в world space (по оси Z для LARGE_VEHICLE_DOOR)
    private double clipMin = Double.NEGATIVE_INFINITY;
    private double clipMax = Double.POSITIVE_INFINITY;

    public LegacyAnimator(PoseStack poseStack, MultiBufferSource bufferSource, 
                         BlockRenderDispatcher blockRenderer, int packedLight, int packedOverlay) {
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.blockRenderer = blockRenderer;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
    }
    
    /**
     * НОВОЕ: Устанавливает диапазон clipping по оси Z (world space)
     * Створки за пределами [clipMin, clipMax] становятся прозрачными
     */
    public void setClippingRange(double min, double max) {
        this.clipMin = min;
        this.clipMax = max;
    }

    public void push() {
        poseStack.pushPose();
    }

    public void pop() {
        poseStack.popPose();
    }

    public void translate(double x, double y, double z) {
        poseStack.translate(x, y, z);
    }

    public void rotate(double angle, double x, double y, double z) {
        if (x == 1.0 && y == 0 && z == 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees((float) angle));
        } else if (x == 0 && y == 1.0 && z == 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees((float) angle));
        } else if (x == 0 && y == 0 && z == 1.0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) angle));
        }
    }

    public void setupBlockTransform(Direction facing) {
        this.translate(0.5, 0.0, 0.5);
        this.rotate(90, 0, 1, 0);
        
        float directionalRotation = switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
        
        this.rotate(directionalRotation, 0, 1, 0);
    }

    /**
     * ИЗМЕНЕНО: Рендер с динамической альфой на основе позиции вершин
     */
    public void renderPart(BakedModel modelPart) {
        if (modelPart == null) return;
        
        // Если clipping отключен - используем стандартный рендер
        if (clipMin == Double.NEGATIVE_INFINITY && clipMax == Double.POSITIVE_INFINITY) {
            blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.translucent()),
                null, modelPart, 1.0f, 1.0f, 1.0f,
                packedLight, packedOverlay, ModelData.EMPTY, null
            );
            return;
        }
        
        // Рендер с динамической альфой
        renderPartWithAlphaClipping(modelPart);
    }
    
    /**
     * НОВОЕ: Рендер квадов с плавным затуханием альфы за границами clipping
     */
    private void renderPartWithAlphaClipping(BakedModel modelPart) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        List<BakedQuad> quads = modelPart.getQuads(null, null, RandomSource.create(), ModelData.EMPTY, null);
        
        PoseStack.Pose pose = poseStack.last();
        
        for (BakedQuad quad : quads) {
            // Вычисляем среднюю альфу для квада
            float alpha = calculateQuadAlpha(quad, pose);
            
            // Пропускаем полностью прозрачные квады
            if (alpha < 0.01f) continue;
            
            // Рендерим квад с вычисленной альфой
            consumer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, alpha, 
                packedLight, packedOverlay, false);
        }
    }
    
    /**
     * НОВОЕ: Вычисляет альфу квада на основе положения вершин
     */
    private float calculateQuadAlpha(BakedQuad quad, PoseStack.Pose pose) {
        int[] vertexData = quad.getVertices();
        float totalAlpha = 0;
        
        // Проверяем все 4 вершины квада
        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            
            float x = Float.intBitsToFloat(vertexData[offset]);
            float y = Float.intBitsToFloat(vertexData[offset + 1]);
            float z = Float.intBitsToFloat(vertexData[offset + 2]);
            
            // Трансформируем вершину
            Vector3f transformed = new Vector3f(x, y, z);
            transformed.mulPosition(pose.pose());
            
            // Вычисляем альфу для этой вершины
            float vertexAlpha = calculateVertexAlpha(transformed.z());
            totalAlpha += vertexAlpha;
        }
        
        // Средняя альфа по 4 вершинам
        return totalAlpha / 4.0f;
    }
    
    /**
     * НОВОЕ: Вычисляет альфу вершины с плавным переходом
     */
    private float calculateVertexAlpha(float z) {
        double fadeDistance = 0.5; // Расстояние плавного затухания (0.5 блока)
        
        if (z < clipMin) {
            // За нижней границей - плавное затухание
            double distance = clipMin - z;
            return (float) Math.max(0, 1.0 - distance / fadeDistance);
        } else if (z > clipMax) {
            // За верхней границей - плавное затухание
            double distance = z - clipMax;
            return (float) Math.max(0, 1.0 - distance / fadeDistance);
        }
        
        // Внутри диапазона - полностью видна
        return 1.0f;
    }
}
