package com.hbm_m.client.render;

/**
 * Representation of a mesh registered and baked in the Tier-1 GPU bake cache.
 */
public final class BakedMesh {
    public final IrisCompanionMesh mesh;
    public final int vertCount;
    public final int idxCount;
    public final int[] baseIndices;
    /** Base mesh indices as an SSBO for the cull program's EBO-build pass. */
    public int baseIdxSsbo = -1;
    // P3: one-time aligned relayout of the input mesh; -1 = bind the mesh VBO directly
    public int inputSsbo = -1;
    public int inputStride;
    public final ModeCache shadow = new ModeCache();
    public final ModeCache main = new ModeCache();
    // Global-batch placement: vertex offset and EBO word offset per mode (0 = shadow, 1 = main)
    public final int[] vertBase = { -1, -1 };
    public final int[] eboWordBase = { -1, -1 };

    public BakedMesh(IrisCompanionMesh mesh, int vertCount, int idxCount, int[] baseIndices) {
        this.mesh = mesh;
        this.vertCount = vertCount;
        this.idxCount = idxCount;
        this.baseIndices = baseIndices;
    }

    public String debugTag() {
        if (mesh != null) {
            String name = mesh.getDebugName();
            if (name != null && !name.isEmpty() && !"unknown".equals(name)) {
                return name;
            }
        }
        return "mesh#" + System.identityHashCode(this);
    }

    public ModeCache cache(int mode) {
        return mode == NucleusGpuBaker.MODE_SHADOW ? shadow : main;
    }

    /** Mode index of one of this mesh's caches (0 = shadow, 1 = main). */
    public int modeOf(ModeCache mc) {
        return mc == main ? NucleusGpuBaker.MODE_MAIN : NucleusGpuBaker.MODE_SHADOW;
    }
}
