package com.hbm_m.client.loader.dae;

import java.util.ArrayList;
import java.util.List;

/**
 * Triangle soup parsed from a single COLLADA geometry element, fully tessellated.
 * Each triangle is an int[9]: [pos0, normal0, uv0, pos1, normal1, uv1, pos2, normal2, uv2],
 * indices reference the arrays below. Normal/uv indices may be -1 if absent.
 */
public class DaeMesh {

    public float[] positions;
    public float[] normals;
    public float[] uvs;
    public final List<int[]> tris = new ArrayList<>();

    public int getTriangleCount() {
        return tris.size();
    }
}
