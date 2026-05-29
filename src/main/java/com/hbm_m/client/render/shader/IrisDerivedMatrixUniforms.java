package com.hbm_m.client.render.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.shaders.Uniform;
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

    private IrisDerivedMatrixUniforms() {}

    public record Locations(int modelView, int modelViewInverse, int normalMat) {
        public static final Locations NONE = new Locations(-1, -1, -1);
    }

    public static Locations resolve(ShaderInstance shader) {
        if (shader == null) return Locations.NONE;
        int programId = shader.getId();
        if (programId <= 0) return Locations.NONE;

        int locModelView = firstMat4(programId, "ModelViewMat", "iris_ModelViewMat");
        if (locModelView < 0) {
            Uniform u = shader.getUniform("ModelViewMat");
            if (u != null) {
                int loc = u.getLocation();
                if (isMat4(programId, loc)) locModelView = loc;
            }
        }

        int locInverse = firstMat4(programId, "ModelViewMatInverse", "iris_ModelViewMatInverse");
        int locNormal = firstMat3(programId, "NormalMat", "iris_NormalMat");
        return new Locations(locModelView, locInverse, locNormal);
    }

    private static int firstMat4(int programId, String... names) {
        for (String name : names) {
            int loc = GL20.glGetUniformLocation(programId, name);
            if (isMat4(programId, loc)) return loc;
        }
        return -1;
    }

    private static int firstMat3(int programId, String... names) {
        for (String name : names) {
            int loc = GL20.glGetUniformLocation(programId, name);
            if (isMat3(programId, loc)) return loc;
        }
        return -1;
    }

    private static boolean isMat4(int programId, int location) {
        return location >= 0 && uniformType(programId, location) == GL_FLOAT_MAT4;
    }

    private static boolean isMat3(int programId, int location) {
        return location >= 0 && uniformType(programId, location) == GL_FLOAT_MAT3;
    }

    private static int uniformType(int programId, int location) {
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            return -1;
        }
        int count = GL20.glGetProgrami(programId, GL20.GL_ACTIVE_UNIFORMS);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var size = stack.mallocInt(1);
            var type = stack.mallocInt(1);
            for (int i = 0; i < count; i++) {
                String name = GL20.glGetActiveUniform(programId, i, size, type);
                if (name != null && GL20.glGetUniformLocation(programId, name) == location) {
                    return type.get(0);
                }
            }
        }
        return -1;
    }
}
