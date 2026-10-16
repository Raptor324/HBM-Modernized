//? if fabric {
/*package com.hbm_m.client.model.loading;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.math.Transformation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.ModelState;

@Environment(EnvType.CLIENT)
public final class ModelStateTransforms {

    private ModelStateTransforms() {}

    /^*
     * Returns the raw modelState rotation. Does NOT add an extra pivot layer,
     * because BlockModelRotation.getRotation() already contains the pivot internally.
     *
     * Forge equivalent: just uses modelTransform.getRotation() directly.
     ^/
    public static Transformation getModelStateRotation(ModelState state) {
        if (state == null) return Transformation.identity();
        Transformation rot = state.getRotation();
        if (rot == null || isIdentity(rot)) return Transformation.identity();
        return rot;
    }

    /^*
     * Composes modelState rotation and rootTransform following Forge's exact pipeline:
     * <pre>
     *   combined = modelState.compose(rootTransform)
     *   final = combined.blockCenterToCorner()
     *         = T(0.5) * combined * T(-0.5)
     *         = T(0.5) * modelState * rootTransform * T(-0.5)
     * </pre>
     *
     * This is the matrix actually applied to OBJ vertex positions.
     ^/
    public static Transformation composeForObjBaking(ModelState state, Transformation rootTransform) {
        Transformation modelRot = getModelStateRotation(state);

        Transformation combined;
        if (isIdentity(rootTransform)) {
            combined = modelRot;
        } else if (isIdentity(modelRot)) {
            combined = rootTransform;
        } else {
            // Forge: modelTransform.getRotation().compose(rootTransform)
            // compose = this.matrix * other.matrix
            Matrix4f m = new Matrix4f(modelRot.getMatrix());
            m.mul(rootTransform.getMatrix());
            combined = new Transformation(m);
        }

        if (isIdentity(combined)) return Transformation.identity();

        // Forge: transform.blockCenterToCorner() = applyOrigin(0.5, 0.5, 0.5)
        return blockCenterToCorner(combined);
    }

    /^*
     * Replicates Forge's Transformation.blockCenterToCorner():
     * <pre>
     *   result = T(origin) * transform * T(-origin)
     * </pre>
     * where origin = (0.5, 0.5, 0.5).
     *
     * This converts from a "center of block" reference to a "corner of block" reference.
     ^/
    public static Transformation blockCenterToCorner(Transformation transform) {
        if (isIdentity(transform)) return Transformation.identity();
        return applyOrigin(transform, new Vector3f(0.5f, 0.5f, 0.5f));
    }

    /^*
     * Replicates Forge's IForgeTransformation.applyOrigin():
     * <pre>
     *   Matrix4f ret = transform.getMatrix();
     *   Matrix4f tmp = new Matrix4f().translation(origin);
     *   tmp.mul(ret, ret);       // ret = T(origin) * transform
     *   tmp.translation(-origin);
     *   ret.mul(tmp);            // ret = T(origin) * transform * T(-origin)
     * </pre>
     ^/
    public static Transformation applyOrigin(Transformation transform, Vector3f origin) {
        if (isIdentity(transform)) return Transformation.identity();

        Matrix4f ret = new Matrix4f(transform.getMatrix());
        Matrix4f tmp = new Matrix4f().translation(origin.x(), origin.y(), origin.z());
        tmp.mul(ret, ret);  // ret = tmp * ret = T(origin) * transform
        tmp.translation(-origin.x(), -origin.y(), -origin.z());
        ret.mul(tmp);       // ret = ret * T(-origin) = T(origin) * transform * T(-origin)
        return new Transformation(ret);
    }

    public static boolean isIdentity(Transformation t) {
        if (t == null) return true;
        if (t.equals(Transformation.identity())) return true;
        Matrix4f m = new Matrix4f(t.getMatrix());
        Matrix4f identity = new Matrix4f().identity();
        return m.equals(identity, 1e-5f);
    }

    public static class PivotedModelState implements ModelState {
        private final Transformation rotation;
        private final boolean uvLocked;

        public PivotedModelState(Transformation rotation, boolean uvLocked) {
            this.rotation = rotation;
            this.uvLocked = uvLocked;
        }

        @Override
        public Transformation getRotation() {
            return rotation;
        }

        @Override
        public boolean isUvLocked() {
            return uvLocked;
        }
    }
}
*///?}

