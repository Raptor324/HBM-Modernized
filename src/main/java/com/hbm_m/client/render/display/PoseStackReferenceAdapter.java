package com.hbm_m.client.render.display;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Forge/NeoForge PoseStack behind {@link DisplayReference.PoseStackAdapter}. The rotations spell
 * out as vanilla does them: {@code Axis.*P.rotationDegrees} for the single axes, {@code rotationXYZ}
 * for the display triples ({@code ItemTransform.apply} builds the same quaternion), and an axis
 * rotation for the guiSteps spin.
 */
public final class PoseStackReferenceAdapter implements DisplayReference.PoseStackAdapter {

    private final PoseStack pose;

    public PoseStackReferenceAdapter(PoseStack pose) {
        this.pose = pose;
    }

    @Override
    public void pushPose() {
        pose.pushPose();
    }

    @Override
    public void translate(float x, float y, float z) {
        pose.translate(x, y, z);
    }

    @Override
    public void rotateXYZ(float xDegrees, float yDegrees, float zDegrees) {
        pose.mulPose(new Quaternionf().rotationXYZ(
                Mth.DEG_TO_RAD * xDegrees, Mth.DEG_TO_RAD * yDegrees, Mth.DEG_TO_RAD * zDegrees));
    }

    @Override
    public void rotateX(float degrees) {
        pose.mulPose(Axis.XP.rotationDegrees(degrees));
    }

    @Override
    public void rotateY(float degrees) {
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
    }

    @Override
    public void rotateZ(float degrees) {
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
    }

    @Override
    public void rotateAxis(float degrees, Vector3f axis) {
        pose.mulPose(new Quaternionf().rotationAxis(Mth.DEG_TO_RAD * degrees, axis.x, axis.y, axis.z));
    }

    @Override
    public void scale(float x, float y, float z) {
        pose.scale(x, y, z);
    }
}
