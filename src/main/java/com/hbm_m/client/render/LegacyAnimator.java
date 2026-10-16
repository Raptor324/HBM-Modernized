package com.hbm_m.client.render;

import org.joml.Matrix4f;

import com.hbm_m.interfaces.IDoorAnimator;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
/**
 * Transform-фасад (портирован из 1.7.10): обёртка над PoseStack для
 * канонических блочных трансформов и дверных оффсетов ({@link IDoorAnimator}).
 * Немедленный рендер квадов вырезан — вся геометрия идёт через VBO-пайплайн
 * (фабрика {@link com.hbm_m.client.render.machine.MachineRenderers}) или фолбэки движка.
 */
public class LegacyAnimator implements IDoorAnimator {

    protected final PoseStack poseStack;

    public LegacyAnimator(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    public static LegacyAnimator create(PoseStack poseStack) {
        return new LegacyAnimator(poseStack);
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

    public Matrix4f currentMatrix() {
        return new Matrix4f(poseStack.last().pose());
    }
}
