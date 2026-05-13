package com.hbm_m.client.render;


//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

import java.util.List;

import org.joml.Matrix4f;

import com.hbm_m.interfaces.IDoorAnimator;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class LegacyAnimator implements IDoorAnimator {

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
        rotate(MultipartFacingTransforms.legacyFacingRotationYDegrees(facing), 0, 1, 0);
    }

    /**
     * Химзавод: один источник истины - canonical chunk-угол из
     * {@link MultipartFacingTransforms#chemicalPlantCanonicalRotationY}, переведённый в PoseStack-конвенцию.
     */
    public void setupChemicalPlantBlockTransform(Direction facing) {
        translate(0.5, 0.0, 0.5);
        rotate(MultipartFacingTransforms.chemicalPlantPoseRotationY(facing), 0, 1, 0);
    }

    // ===== CPU рендер =====
    public void renderQuads(List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) return;
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            //? if forge {
            buffer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay, false);
            //?} else {
            /*buffer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
            *///?}
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
