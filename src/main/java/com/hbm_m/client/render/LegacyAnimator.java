package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import com.hbm_m.interfaces.IDoorAnimator;
import com.mojang.blaze3d.vertex.PoseStack;

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

    /**
     * 1.7.10 {@code glRotatef} semantics: a single rotation of {@code degrees}
     * around the given axis (right-hand rule - the SIGN of each component is
     * significant; the axis is normalized, its magnitude is irrelevant, as in
     * the original GL call). A reusable quaternion instance is safe: mulPose
     * copies the values into the pose stack.
     */
    private static final org.joml.Quaternionf ROTATE_SCRATCH = new org.joml.Quaternionf();

    public void rotate(float degrees, float x, float y, float z) {
        if (degrees == 0) return;
        poseStack.mulPose(ROTATE_SCRATCH.rotationAxis((float) Math.toRadians(degrees), x, y, z));
    }

    public void setupBlockTransform(Direction facing) {
        translate(0.5, 0.0, 0.5);
        if (!CardinalOrientation.isIdentity(facing)) {
            poseStack.mulPose(CardinalOrientation.get(facing));
        }
    }

    /**
     * Chemical plant: a single source of truth - precomputed cardinal quaternion from
     * CardinalOrientation.
     */
    public void setupChemicalPlantBlockTransform(Direction facing) {
        translate(0.5, 0.0, 0.5);
        poseStack.mulPose(CardinalOrientation.getChemicalPlant(facing));
    }

    public Matrix4f currentMatrix() {
        return new Matrix4f(poseStack.last().pose());
    }
}
