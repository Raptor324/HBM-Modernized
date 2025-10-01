package com.hbm_m.client.model.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Слой совместимости для рендера анимаций в стиле GL11 (1.7.10) с использованием PoseStack (1.20.1).
 * Позволяет переносить старый код с минимальными изменениями.
 */

public class LegacyAnimator {

    private final PoseStack poseStack;
    private final MultiBufferSource bufferSource;
    private final BlockRenderDispatcher blockRenderer;
    private final int packedLight;
    private final int packedOverlay;

    public LegacyAnimator(PoseStack poseStack, MultiBufferSource bufferSource, BlockRenderDispatcher blockRenderer, int packedLight, int packedOverlay) {
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.blockRenderer = blockRenderer;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
    }

    /**
     * Выполняет стандартную настройку для блока: поворот по Direction и смещение в центр блока (X/Z).
     * Этот метод инкапсулирует ПРАВИЛЬНЫЙ ПОРЯДОК трансформаций (сначала поворот, потом смещение).
     * @param facing Направление, куда смотрит блок.
     */
    public void setupBlockTransform(Direction facing) {
        // --- ШАГ 1: Смещаемся в центр блока, чтобы установить правильную точку вращения. ---
        // Это соответствует старому GL11.glTranslated(x + 0.5, y, z + 0.5);
        this.translate(0.5, 0.0, 0.5);

        // --- ШАГ 2: Применяем ПОСТОЯННЫЙ поворот-коррекцию из старого рендера. ---
        // Это соответствует старому GL11.glRotated(90, 0, 1, 0);
        // Эта строка исправляет изначальную ориентацию модели в .obj файле.
        this.rotate(90, 0, 1, 0);

        // --- ШАГ 3: Применяем поворот, зависящий от направления установки блока. ---
        // Эта логика в точности воспроизводит switch-case из оригинального кода 1.7.10.

        float directionalRotation = 0.0F;
        switch (facing) {
            case NORTH:
                directionalRotation = 0.0F;
                break;
            case SOUTH:
                directionalRotation = 180.0F;
                break;
            case WEST:
                directionalRotation = 90.0F;
                break;
            case EAST:
                directionalRotation = 270.0F;
                break;
            default:
                // Для вертикальных направлений, если они возможны. По умолчанию 0.
                break;
        }
        
        this.rotate(directionalRotation, 0, 1, 0);
    }

    /** Имитация glPushMatrix() */
    public void push() {
        poseStack.pushPose();
    }

    /** Имитация glPopMatrix() */
    public void pop() {
        poseStack.popPose();
    }

    /** Имитация glTranslated() */
    public void translate(double x, double y, double z) {
        poseStack.translate(x, y, z);
    }

    /** Имитация glRotated() */
    public void rotate(double angle, double x, double y, double z) {
        if (x == 1.0 && y == 0 && z == 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees((float) angle));
        } else if (x == 0 && y == 1.0 && z == 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees((float) angle));
        } else if (x == 0 && y == 0 && z == 1.0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) angle));
        } else {
            // Общий случай для произвольной оси, если понадобится
            poseStack.mulPose(Axis.of(new org.joml.Vector3f((float)x, (float)y, (float)z)).rotationDegrees((float)angle));
        }
    }

    /** Метод для рендера части модели */
    public void renderPart(BakedModel modelPart) {
        if (modelPart != null) {
            blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                null, modelPart, 1.0f, 1.0f, 1.0f,
                packedLight, packedOverlay, ModelData.EMPTY, null
            );
        }
    }
}