package com.hbm_m.client.render;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class LegacyAnimator {

    protected final PoseStack poseStack;
    final VertexConsumer buffer;
    private final int packedLight;
    private final int packedOverlay;

    public LegacyAnimator(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        this.poseStack = poseStack;
        this.buffer = buffer;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
    }

    public static LegacyAnimator create(PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());
        return new LegacyAnimator(poseStack, buffer, packedLight, packedOverlay);
    }

    // ===== Трансформации =====
    public void push() { poseStack.pushPose(); }
    public void pop()  { poseStack.popPose(); }

    public void translate(double x, double y, double z) { poseStack.translate(x, y, z); }

    public void rotate(float degrees, float x, float y, float z) {
        if (degrees == 0) return;
        if (x != 0) poseStack.mulPose(Axis.XP.rotationDegrees(degrees));
        if (y != 0) poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        if (z != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(degrees));
    }

    public void setupBlockTransform(Direction facing) {
        translate(0.5, 0.0, 0.5);
        rotate(90, 0, 1, 0);
        float facingRotation = switch (facing) {
            case SOUTH -> 180.0F;
            case WEST  -> 90.0F;
            case EAST  -> 270.0F;
            default    -> 0.0F;
        };
        rotate(facingRotation, 0, 1, 0);
    }

    // ===== CPU рендер =====
    public void renderQuads(List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) return;
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            buffer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay, false);
        }
    }

    @Deprecated
    public void renderPart(BakedModel modelPart) {
        if (modelPart == null) return;
        String tempKey = "Temp:" + System.identityHashCode(modelPart);
        List<BakedQuad> quads = GlobalMeshCache.getOrCompile(tempKey, modelPart);
        renderQuads(quads);
    }

    public Matrix4f currentMatrix() {
        return new Matrix4f(poseStack.last().pose());
    }
}
