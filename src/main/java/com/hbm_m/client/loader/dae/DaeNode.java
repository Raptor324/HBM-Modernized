package com.hbm_m.client.loader.dae;

import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A COLLADA scene node. Holds its base transforms, an optional baked geometry buffer
 * and its children. Animation channels are queried from the active {@link DaeAnimation}
 * by the node's COLLADA id, falling back to its display name.
 */
public class DaeNode {

    public final String name;
    /** The COLLADA id attribute; animation channels reference this, not the name. */
    public String colladaId;
    public final List<DaeTransform> transforms = new ArrayList<>();
    public final List<DaeNode> children = new ArrayList<>();

    public DaeMesh mesh;
    public ResourceLocation texture;

    public DaeNode(String name) {
        this.name = name;
    }

    /** Computes the local matrix in rest pose (no animation applied) */
    public Matrix4f localMatrix() {
        return localMatrix(-1F, null);
    }

    /**
     * Computes the local matrix for the given animation time. Channels of the given clip
     * override the base transform values; a {@code "matrix"} channel replaces the node
     * transform entirely. When {@code animation} is null or the node has no channels,
     * the rest pose is returned.
     */
    public Matrix4f localMatrix(float time, DaeAnimation animation) {
        if(animation != null) {
            Map<String, DaeCurve> animChannels = animation.getChannels(this.name);
            if(animChannels == null && colladaId != null && !colladaId.equals(this.name)) {
                animChannels = animation.getChannels(colladaId);
            }
            if(animChannels != null && !animChannels.isEmpty()) {
                Matrix4f result = new Matrix4f();

                DaeCurve matrixCurve = animChannels.get("transform");
                if(matrixCurve != null) {
                    return matrixFromCollada(matrixCurve.sample(time));
                }

                for(DaeTransform t : transforms) {
                    result.mul(transformMatrix(t, time, animChannels));
                }
                return result;
            }
        }

        Matrix4f result = new Matrix4f();
        for(DaeTransform t : transforms) {
            result.mul(transformMatrix(t, -1F, null));
        }
        return result;
    }

    private Matrix4f transformMatrix(DaeTransform t, float time, Map<String, DaeCurve> animChannels) {
        float[] data = t.data.clone();

        if(animChannels != null) {
            switch(t.type) {
                case TRANSLATE -> {
                    DaeCurve whole = animChannels.get(t.sid);
                    if(whole != null) {
                        float[] v = whole.sample(time);
                        data[0] = v[0];
                        data[1] = v[1];
                        data[2] = v[2];
                    }
                    DaeCurve x = animChannels.get(t.sid + ".X");
                    if(x != null) data[0] = x.sample(time)[0];
                    DaeCurve y = animChannels.get(t.sid + ".Y");
                    if(y != null) data[1] = y.sample(time)[0];
                    DaeCurve z = animChannels.get(t.sid + ".Z");
                    if(z != null) data[2] = z.sample(time)[0];
                }
                case ROTATE -> {
                    DaeCurve angle = animChannels.get(t.sid + ".ANGLE");
                    if(angle != null) {
                        data[3] = angle.sample(time)[0];
                    } else {
                        DaeCurve whole = animChannels.get(t.sid);
                        if(whole != null) data[3] = whole.sample(time)[0];
                    }
                }
                case SCALE -> {
                    DaeCurve whole = animChannels.get(t.sid);
                    if(whole != null) {
                        float[] v = whole.sample(time);
                        data[0] = v[0];
                        data[1] = v[1];
                        data[2] = v[2];
                    }
                    DaeCurve x = animChannels.get(t.sid + ".X");
                    if(x != null) data[0] = x.sample(time)[0];
                    DaeCurve y = animChannels.get(t.sid + ".Y");
                    if(y != null) data[1] = y.sample(time)[0];
                    DaeCurve z = animChannels.get(t.sid + ".Z");
                    if(z != null) data[2] = z.sample(time)[0];
                }
                case MATRIX -> {
                    DaeCurve whole = animChannels.get(t.sid);
                    if(whole != null) {
                        data = whole.sample(time);
                    }
                }
            }
        }

        return switch(t.type) {
            case TRANSLATE -> new Matrix4f().translation(data[0], data[1], data[2]);
            case ROTATE -> new Matrix4f().rotation((float) Math.toRadians(data[3]), data[0], data[1], data[2]);
            case SCALE -> new Matrix4f().scale(data[0], data[1], data[2]);
            case MATRIX -> matrixFromCollada(data);
        };
    }

    /**
     * COLLADA {@code <matrix>} elements and full-transform animation outputs written by
     * the Blender exporter store the elements as m{row}{col} in the file, i.e. the
     * translation sits at indices 3, 7 and 11. JOML's {@code Matrix4f} constructor
     * assigns the arguments by name, but transforms are composed in JOML's row-vector
     * convention ({@code v * M}) where the translation lives in the bottom row, so the
     * stored matrix has to be transposed to behave as the file intends.
     */
    private static Matrix4f matrixFromCollada(float[] m) {
        return new Matrix4f(
                m[0], m[4], m[8], m[12],
                m[1], m[5], m[9], m[13],
                m[2], m[6], m[10], m[14],
                m[3], m[7], m[11], m[15]
        );
    }
}
