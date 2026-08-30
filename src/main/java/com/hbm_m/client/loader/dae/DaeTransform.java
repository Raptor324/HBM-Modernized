package com.hbm_m.client.loader.dae;

/**
 * A single transform primitive of a COLLADA node, preserving the document order in
 * which the transforms were declared.
 */
public class DaeTransform {

    public enum Type {
        TRANSLATE,
        ROTATE,
        SCALE,
        MATRIX
    }

    public final Type type;
    public final String sid;
    /** TRANSLATE/SCALE: [x, y, z], ROTATE: [axisX, axisY, axisZ, angleDeg], MATRIX: 16 values, column-major */
    public final float[] data;

    public DaeTransform(Type type, String sid, float[] data) {
        this.type = type;
        this.sid = sid != null ? sid : "";
        this.data = data;
    }
}
