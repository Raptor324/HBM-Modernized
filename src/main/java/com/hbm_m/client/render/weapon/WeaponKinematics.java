package com.hbm_m.client.render.weapon;

import java.util.function.Consumer;

import org.joml.Vector3f;

import com.hbm_m.client.render.NucleusJointSpecs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Leak-free PoseStack evaluator for weapon kinematic joints with strict RAII push/pop balance
 * and zero OpenGL state leaks.
 */
@OnlyIn(Dist.CLIENT)
public final class WeaponKinematics {

    private WeaponKinematics() { }

    /**
     * Applies a single joint transformation to PoseStack.
     * For KIND_ROTATE: translates to pivot, rotates around normalized axis, translates back.
     * For KIND_TRANSLATE / KIND_TRANSLATE_AXIS: translates along axis scaled by param.
     */
    public static void applyJoint(PoseStack ps, WeaponJointRig.JointDef joint, float value) {
        if (joint == null) return;
        if (joint.kind() == NucleusJointSpecs.KIND_ROTATE) {
            ps.translate(joint.pivotX(), joint.pivotY(), joint.pivotZ());
            ps.mulPose(Axis.of(new Vector3f(joint.axisX(), joint.axisY(), joint.axisZ())).rotationDegrees(value));
            ps.translate(-joint.pivotX(), -joint.pivotY(), -joint.pivotZ());
        } else if (joint.kind() == NucleusJointSpecs.KIND_TRANSLATE_AXIS || joint.kind() == NucleusJointSpecs.KIND_TRANSLATE) {
            ps.translate(joint.axisX() * value, joint.axisY() * value, joint.axisZ() * value);
        }
    }

    /**
     * Applies a 3D vector translation joint.
     */
    public static void applyVectorTranslate(PoseStack ps, float x, float y, float z) {
        ps.translate(x, y, z);
    }

    /**
     * Applies a translation along a fixed axis.
     */
    public static void applyTranslateAxis(PoseStack ps, float distance, float ax, float ay, float az) {
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len < 1e-6f) len = 1f;
        ps.translate((ax / len) * distance, (ay / len) * distance, (az / len) * distance);
    }

    /**
     * Applies a rotation around a specific axis and pivot point.
     */
    public static void applyRotate(PoseStack ps, float angleDeg, float ax, float ay, float az, float px, float py, float pz) {
        if (angleDeg == 0f) return;
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len < 1e-6f) len = 1f;
        ps.translate(px, py, pz);
        ps.mulPose(Axis.of(new Vector3f(ax / len, ay / len, az / len)).rotationDegrees(angleDeg));
        ps.translate(-px, -py, -pz);
    }

    /**
     * Applies a multi-node kinematic chain sequentially to PoseStack.
     */
    public static void applyChain(PoseStack ps, WeaponJointRig.ChainDef chain, float[] valuesPerNode) {
        if (chain == null || valuesPerNode == null) return;
        for (int i = 0; i < chain.nodeCount() && i < valuesPerNode.length; i++) {
            int kind = chain.kinds()[i];
            float ax = chain.axes()[i * 3], ay = chain.axes()[i * 3 + 1], az = chain.axes()[i * 3 + 2];
            float px = chain.pivots()[i * 3], py = chain.pivots()[i * 3 + 1], pz = chain.pivots()[i * 3 + 2];
            float val = valuesPerNode[i];

            if (kind == NucleusJointSpecs.KIND_ROTATE) {
                ps.translate(px, py, pz);
                ps.mulPose(Axis.of(new Vector3f(ax, ay, az)).rotationDegrees(val));
                ps.translate(-px, -py, -pz);
            } else {
                ps.translate(ax * val, ay * val, az * val);
            }
        }
    }

    // ─── RAII Scoped Execution Helpers ──────────────────────────────────────────

    /**
     * Scoped transform application: pushes PoseStack, runs the transform, and guarantees pop.
     */
    public static void scoped(PoseStack ps, Consumer<PoseStack> action) {
        ps.pushPose();
        try {
            action.accept(ps);
        } finally {
            ps.popPose();
        }
    }

    /**
     * Renders a part with a custom transform inside a safe push/pop scope.
     */
    public static void renderPartAnimated(ResourceLocation modelLocation, String partName, PoseStack ps, Consumer<PoseStack> transform) {
        ps.pushPose();
        try {
            if (transform != null) {
                transform.accept(ps);
            }
            WeaponRenderRegistry.renderPart(modelLocation, partName, ps);
        } finally {
            ps.popPose();
        }
    }

    /**
     * Renders a part with its registered joint transformation applied inside a safe push/pop scope.
     */
    public static void renderPartWithJoint(WeaponJointRig rig, String partName, PoseStack ps, float value) {
        ps.pushPose();
        try {
            if (rig != null) {
                WeaponJointRig.JointDef joint = rig.getJoint(partName);
                if (joint != null) {
                    applyJoint(ps, joint, value);
                }
                WeaponRenderRegistry.renderPart(rig.getModelLocation(), partName, ps);
            }
        } finally {
            ps.popPose();
        }
    }

    /**
     * Renders a part with a 3D translation vector applied inside a safe push/pop scope.
     */
    public static void renderPartWithVector(WeaponJointRig rig, String partName, PoseStack ps, float x, float y, float z) {
        ps.pushPose();
        try {
            applyVectorTranslate(ps, x, y, z);
            if (rig != null) {
                WeaponRenderRegistry.renderPart(rig.getModelLocation(), partName, ps);
            }
        } finally {
            ps.popPose();
        }
    }

    /**
     * Renders a part with its registered kinematic chain applied inside a safe push/pop scope.
     */
    public static void renderPartWithChain(WeaponJointRig rig, String partName, PoseStack ps, float[] values) {
        ps.pushPose();
        try {
            if (rig != null) {
                WeaponJointRig.ChainDef chain = rig.getChain(partName);
                if (chain != null) {
                    applyChain(ps, chain, values);
                }
                WeaponRenderRegistry.renderPart(rig.getModelLocation(), partName, ps);
            }
        } finally {
            ps.popPose();
        }
    }

    // ─── 6 Core Kinematic Category Implementations ─────────────────────────────

    // Category A: Reciprocating slides & bolts
    public static void applySlideReciprocation(PoseStack ps, float stroke) {
        ps.translate(0, 0, stroke);
    }

    // Category B: Revolver cylinder step rotation & hammer
    public static void applyCylinderStepRotation(PoseStack ps, float drumIndex, float pivotY) {
        ps.translate(0, pivotY, 0);
        ps.mulPose(Axis.XP.rotationDegrees(drumIndex * -60.0f));
        ps.translate(0, -pivotY, 0);
    }

    public static void applyRevolverHammer(PoseStack ps, float hammerProgress, float px, float py, float pz) {
        ps.translate(px, py, pz);
        ps.mulPose(Axis.ZP.rotationDegrees(-30.0f + 30.0f * hammerProgress));
        ps.translate(-px, -py, -pz);
    }

    public static void applyCylinderFlip(PoseStack ps, float flipAngle) {
        ps.mulPose(Axis.XP.rotationDegrees(flipAngle));
    }

    // Category C: Minigun continuous rotating barrels
    public static void applyMinigunBarrelSpin(PoseStack ps, float angleDeg, int handFactor) {
        ps.mulPose(Axis.ZP.rotationDegrees(angleDeg * handFactor));
    }

    // Category D: Chainsaw cyclic 20-tooth racetrack loop
    public static void renderChainsawTeeth(ResourceLocation modelLocation, PoseStack ps, boolean canOperate, long timeMs) {
        double run = canOperate ? (timeMs % 100D) * 0.25D / 100D : 0.0625D;
        float arcLimit = (float) (0.25 * Math.PI); // 45 deg in radians

        for (int i = 0; i < 20; i++) {
            double forward = i * 0.25 + run - 2.0625;
            ps.pushPose();
            try {
                ps.translate(0, 0, 1.9375);
                ps.translate(0, 0.375, 0.5625);
                double angle = Mth.clamp(forward, 0, 0.25 * Math.PI);
                ps.mulPose(Axis.XP.rotationDegrees((float) (angle * 180D / (Math.PI * 0.25))));
                ps.translate(0, -0.375, -0.5625);
                if (forward < 0) {
                    ps.translate(0, 0, forward);
                }
                if (forward > Math.PI * 0.25) {
                    ps.translate(0, 0, forward - Math.PI * 0.25);
                }
                WeaponRenderRegistry.renderPart(modelLocation, "Tooth", ps);
            } finally {
                ps.popPose();
            }
        }
    }

    // Category E: Lever action & hammer
    public static void applyLeverRotation(PoseStack ps, float angleDeg, float px, float py, float pz) {
        ps.translate(px, py, pz);
        ps.mulPose(Axis.XP.rotationDegrees(angleDeg));
        ps.translate(-px, -py, -pz);
    }

    public static void applyHammerRotation(PoseStack ps, float angleDeg, float px, float py, float pz) {
        ps.translate(px, py, pz);
        ps.mulPose(Axis.XP.rotationDegrees(angleDeg));
        ps.translate(-px, -py, -pz);
    }

    // Category F: Akimbo dual-wield mirroring
    public static void applyAkimboTransforms(PoseStack ps, int handIndex, float turnRoll, float recoilX, float recoilY, float recoilZ) {
        int i = (handIndex == 0 ? -1 : 1);
        ps.translate(recoilX * i, recoilY, recoilZ);
        ps.mulPose(Axis.ZP.rotationDegrees(turnRoll * i));
    }

    public static String getAkimboReceiverPart(WeaponJointRig rig, int handIndex) {
        if (handIndex == 0 && rig != null && rig.hasLeftPart()) {
            return rig.getLeftPartName();
        }
        return "Gun";
    }

    /**
     * Renders the receiver part (automatically selecting left/right variant for Akimbo)
     * plus all declared rest-pose parts of the rig, and optional conditional attachments.
     */
    public static void renderRestParts(WeaponJointRig rig, int handIndex, PoseStack ps, String... extraParts) {
        if (rig == null) return;
        ResourceLocation loc = rig.getModelLocation();
        WeaponRenderRegistry.renderPart(loc, getAkimboReceiverPart(rig, handIndex), ps);
        for (String part : rig.getRestParts()) {
            WeaponRenderRegistry.renderPart(loc, part, ps);
        }
        if (extraParts != null) {
            for (String extra : extraParts) {
                if (extra != null && !extra.isEmpty()) {
                    WeaponRenderRegistry.renderPart(loc, extra, ps);
                }
            }
        }
    }
}
