//? if fabric {
package com.hbm_m.client.model.loading;

import org.joml.Matrix4f;

import com.mojang.math.Transformation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.ModelState;

@Environment(EnvType.CLIENT)
public final class ModelStateTransforms {

    private ModelStateTransforms() {}

    /**
     * Applies the Vanilla block center pivot (0.5, 0.5, 0.5) ONLY to the BlockState rotation.
     * Extracts it safely so it doesn't double-pivot if already processed.
     */
    public static Transformation resolveAndPivot(ModelState state) {
        if (state == null) return Transformation.identity();

        // If it's already processed by a parent composite model, use it directly
        if (state instanceof PivotedModelState) {
            return state.getRotation();
        }

        Transformation rot = state.getRotation();
        if (rot == null || rot.equals(Transformation.identity())) {
            return Transformation.identity();
        }

        Matrix4f m = new Matrix4f(rot.getMatrix());
        Matrix4f pivoted = new Matrix4f()
                .translation(0.5f, 0.5f, 0.5f)
                .mul(m)
                .translate(-0.5f, -0.5f, -0.5f);

        return new Transformation(pivoted);
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
//?}

