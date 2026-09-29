package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import org.joml.Matrix4f;

import com.hbm_m.interfaces.IDoorAnimator;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;

@OnlyIn(Dist.CLIENT)
/**
 * Transform facade (ported from 1.7.10): a PoseStack wrapper for canonical block
 * transforms and door offsets ({@link IDoorAnimator}).
 * Immediate quad rendering was removed - all geometry goes through the VBO pipeline
 * ({@link com.hbm_m.client.render.machine.MachineRenderers} factory) or engine fallbacks.
 */
public class LegacyAnimator implements IDoorAnimator {

    private PoseStack poseStack;

    public LegacyAnimator(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    /**
     * Reusable instance on the render thread (previously a new instance per BE per
     * pass; the object only holds a PoseStack reference). Not reentrant: create()
     * lives within a single BE's collect, animation hooks never see the animator.
     */
    private static final ThreadLocal<LegacyAnimator> REUSE =
            ThreadLocal.withInitial(() -> new LegacyAnimator(null));

    public static LegacyAnimator create(PoseStack poseStack) {
        LegacyAnimator animator = REUSE.get();
        animator.poseStack = poseStack;
        return animator;
    }

    // ===== Transformations =====
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
     * Chemical plant: a single source of truth - the canonical chunk angle from
     * {@link MultipartFacingTransforms#chemicalPlantCanonicalRotationY}, converted to the PoseStack convention.
     */
    public void setupChemicalPlantBlockTransform(Direction facing) {
        translate(0.5, 0.0, 0.5);
        rotate(MultipartFacingTransforms.chemicalPlantPoseRotationY(facing), 0, 1, 0);
    }

    public Matrix4f currentMatrix() {
        return new Matrix4f(poseStack.last().pose());
    }
}
