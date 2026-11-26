package com.hbm_m.client.render.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.shaders.Uniform;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Resolves {@code ModelViewMat} / inverse / normal matrix uniform locations for
 * Iris {@code ExtendedShader} programs with GL type validation.
 * <p>
 * {@code ExtendedShader} uses Mojang names ({@code ModelViewMatInverse},
 * {@code NormalMat}). Some packs also declare {@code iris_ModelViewMat*}; a
 * stale or wrong {@code glGetUniformLocation("iris_ModelViewMatInverse")} can
 * point at a non-matrix uniform and spam
 * {@code GL_INVALID_OPERATION: Uniform must be a matrix type}.
 */
public final class IrisDerivedMatrixUniforms {

    private static final int GL_FLOAT_MAT3 = 0x8B5B;
    private static final int GL_FLOAT_MAT4 = 0x8B5C;

    private static long cachedPipelineGeneration = -1L;
    private static final Int2ObjectMap<Locations> LOCATIONS_CACHE = new Int2ObjectOpenHashMap<>();

    private IrisDerivedMatrixUniforms() {}

    public record Locations(int modelView, int modelViewInverse, int normalMat) {
        public static final Locations NONE = new Locations(-1, -1, -1);
    }

    public static Locations resolve(ShaderInstance shader) {
        if (shader == null) return Locations.NONE;
        int programId = shader.getId();
        if (programId <= 0) return Locations.NONE;

        long currentGen = IrisExtendedShaderAccess.getPipelineGeneration();
        synchronized (LOCATIONS_CACHE) {
            if (currentGen != cachedPipelineGeneration) {
                LOCATIONS_CACHE.clear();
                cachedPipelineGeneration = currentGen;
            }
            Locations cached = LOCATIONS_CACHE.get(programId);
            if (cached != null) {
                return cached;
            }
            Locations resolved = resolveInternal(shader, programId);
            LOCATIONS_CACHE.put(programId, resolved);
            return resolved;
        }
    }

    public static void clearCache() {
        synchronized (LOCATIONS_CACHE) {
            LOCATIONS_CACHE.clear();
            cachedPipelineGeneration = -1L;
        }
    }

    private static Locations resolveInternal(ShaderInstance shader, int programId) {
        int locModelView = firstMat4(programId, "ModelViewMat", "iris_ModelViewMat");
        if (locModelView < 0) {
            Uniform u = shader.getUniform("ModelViewMat");
            if (u != null) {
                int loc = u.getLocation();
                if (loc >= 0 && getUniformType(programId, "ModelViewMat") == GL_FLOAT_MAT4) {
                    locModelView = loc;
                }
            }
        }

        int locInverse = firstMat4(programId, "ModelViewMatInverse", "iris_ModelViewMatInverse");
        int locNormal = firstMat3(programId, "NormalMat", "iris_NormalMat");
        return new Locations(locModelView, locInverse, locNormal);
    }

    private static int firstMat4(int programId, String... names) {
        for (String name : names) {
            int loc = GL20.glGetUniformLocation(programId, name);
            if (loc >= 0 && getUniformType(programId, name) == GL_FLOAT_MAT4) return loc;
        }
        return -1;
    }

    private static int firstMat3(int programId, String... names) {
        for (String name : names) {
            int loc = GL20.glGetUniformLocation(programId, name);
            if (loc >= 0 && getUniformType(programId, name) == GL_FLOAT_MAT3) return loc;
        }
        return -1;
    }

    private static int getUniformType(int programId, String name) {
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            return -1;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var indices = stack.mallocInt(1);
            GL31.glGetUniformIndices(programId, new CharSequence[]{name}, indices);
            int index = indices.get(0);
            if (index == -1 || index == GL31.GL_INVALID_INDEX) {
                return -1;
            }
            return GL31.glGetActiveUniformsi(programId, index, GL31.GL_UNIFORM_TYPE);
        }
    }

    // Shared zero-allocation scratch buffers (guaranteed render-thread only)
    private static final float[] MV_FLOATS = new float[16];
    private static final float[] INV_FLOATS = new float[16];
    private static final float[] NORM_FLOATS = new float[9];
    private static final org.joml.Matrix4f INV_SCRATCH = new org.joml.Matrix4f();
    private static final org.joml.Matrix3f NORM_SCRATCH = new org.joml.Matrix3f();

    /**
     * Uploads ModelViewMat, ModelViewMatInverse (if present), and NormalMat (if present)
     * using zero-allocation internal scratch buffers.
     */
    public static void uploadDerivedMatrices(Locations locs, org.joml.Matrix4fc modelView) {
        if (locs == null) return;

        if (locs.modelView() >= 0) {
            modelView.get(MV_FLOATS);
            GL20.glUniformMatrix4fv(locs.modelView(), false, MV_FLOATS);
        }
        if (locs.modelViewInverse() >= 0) {
            INV_SCRATCH.set(modelView).invertAffine();
            INV_SCRATCH.get(INV_FLOATS);
            GL20.glUniformMatrix4fv(locs.modelViewInverse(), false, INV_FLOATS);
        }
        if (locs.normalMat() >= 0) {
            if (locs.modelViewInverse() >= 0) {
                NORM_SCRATCH.set(INV_SCRATCH).transpose();
            } else {
                NORM_SCRATCH.set(INV_SCRATCH.set(modelView).invertAffine()).transpose();
            }
            NORM_SCRATCH.get(NORM_FLOATS);
            GL20.glUniformMatrix3fv(locs.normalMat(), false, NORM_FLOATS);
        }
    }
}
