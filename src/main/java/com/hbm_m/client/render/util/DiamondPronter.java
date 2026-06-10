package com.hbm_m.client.render.util;

import com.hbm_m.inventory.fluid.FluidHazardSymbol;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Renders NFPA-style hazard diamonds (1.7.10 {@code com.hbm.render.util.DiamondPronter}).
 */
public final class DiamondPronter {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/misc/danger_diamond.png");

    private DiamondPronter() {}

    public static void pront(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int poison,
            int flammability,
            int reactivity,
            FluidHazardSymbol symbol,
            int packedLight,
            int packedOverlay
    ) {
        float p = 1F / 256F;
        float s = 1F / 139F;

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

        consumer.vertex(matrix, 0.0F, 0.5F, -0.5F)
                .color(255, 255, 255, 255)
                .uv(p * 144, p * 45)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(matrix, 0.0F, 0.5F, 0.5F)
                .color(255, 255, 255, 255)
                .uv(p * 5, p * 45)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(matrix, 0.0F, -0.5F, 0.5F)
                .color(255, 255, 255, 255)
                .uv(p * 5, p * 184)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(matrix, 0.0F, -0.5F, -0.5F)
                .color(255, 255, 255, 255)
                .uv(p * 144, p * 184)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        float width = 10F * s;
        float height = 14F * s;

        if (poison >= 0 && poison < 6) {
            float oY = 0;
            float oZ = 33 * s;

            int x = 5 + (poison - 1) * 24;
            int y = 5;

            if (poison == 0) {
                x = 125;
            }

            addSegment(consumer, matrix, height, oY, width, oZ, x, y, p, packedLight, packedOverlay);
        }

        if (flammability >= 0 && flammability < 6) {
            float oY = 33 * s;
            float oZ = 0;

            int x = 5 + (flammability - 1) * 24;
            int y = 5;

            if (flammability == 0) {
                x = 125;
            }

            addSegment(consumer, matrix, height, oY, width, oZ, x, y, p, packedLight, packedOverlay);
        }

        if (reactivity >= 0 && reactivity < 6) {
            float oY = 0;
            float oZ = -33 * s;

            int x = 5 + (reactivity - 1) * 24;
            int y = 5;

            if (reactivity == 0) {
                x = 125;
            }

            addSegment(consumer, matrix, height, oY, width, oZ, x, y, p, packedLight, packedOverlay);
        }

        float symSize = 59F / 2F * s;

        if (symbol != FluidHazardSymbol.NONE) {
            float oY = -33 * s;
            float oZ = 0;

            int x = symbol.x;
            int y = symbol.y;

            consumer.vertex(matrix, 0.01F, symSize + oY, -symSize + oZ)
                    .color(255, 255, 255, 255)
                    .uv((x + 59) * p, y * p)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
            consumer.vertex(matrix, 0.01F, symSize + oY, symSize + oZ)
                    .color(255, 255, 255, 255)
                    .uv(x * p, y * p)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
            consumer.vertex(matrix, 0.01F, -symSize + oY, symSize + oZ)
                    .color(255, 255, 255, 255)
                    .uv(x * p, (y + 59) * p)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
            consumer.vertex(matrix, 0.01F, -symSize + oY, -symSize + oZ)
                    .color(255, 255, 255, 255)
                    .uv((x + 59) * p, (y + 59) * p)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
        }
    }

    private static void addSegment(
            VertexConsumer consumer,
            Matrix4f matrix,
            float height,
            float oY,
            float width,
            float oZ,
            int x,
            int y,
            float p,
            int packedLight,
            int packedOverlay
    ) {
        consumer.vertex(matrix, 0.01F, height + oY, -width + oZ)
                .color(255, 255, 255, 255)
                .uv((x + 20) * p, y * p)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(matrix, 0.01F, height + oY, width + oZ)
                .color(255, 255, 255, 255)
                .uv(x * p, y * p)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(matrix, 0.01F, -height + oY, width + oZ)
                .color(255, 255, 255, 255)
                .uv(x * p, (y + 28) * p)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(matrix, 0.01F, -height + oY, -width + oZ)
                .color(255, 255, 255, 255)
                .uv((x + 20) * p, (y + 28) * p)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
