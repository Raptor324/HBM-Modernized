package com.hbm_m.client.render;

import com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Bridge between Tier-1 GPU compute bake and Iris shaderpack pipeline.
 * Handles Iris extended vertex attributes, shadow slim-format attribute re-binding,
 * matrix uniform uploads, frustum plane extraction, and shadow decline telemetry.
 */
public final class IrisBakeBridge {

    private IrisBakeBridge() {}

    // Shadow record layout constants are owned by BakeVramPoolManager (single
    // source of truth shared with the compute bake and the pool VAOs).

    // Extended-attribute location cache, keyed per (ShaderInstance, pipeline generation)
    private static ShaderInstance extAttribShader;
    private static long extAttribPipelineGen = -1L;
    private static int cachedEntLoc = -2, cachedMidTexLoc = -2,
            cachedTangentLoc = -2, cachedMidBlockLoc = -2;

    public static void bindIrisExtendedAttributes(ShaderInstance shader, int outputSsbo,
                                                  int strideBytes, int offMidTex, int offTangent,
                                                  int offMidBlock) {
        if (shader == null || outputSsbo == -1) {
            return;
        }
        long gen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (extAttribShader != shader || extAttribPipelineGen != gen) {
            extAttribShader = shader;
            extAttribPipelineGen = gen;
            int program = shader.getId();
            cachedEntLoc = GL20.glGetAttribLocation(program, "iris_Entity");
            cachedMidTexLoc = GL20.glGetAttribLocation(program, "mc_midTexCoord");
            cachedTangentLoc = GL20.glGetAttribLocation(program, "at_tangent");
            cachedMidBlockLoc = GL20.glGetAttribLocation(program, "at_midBlock");
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, outputSsbo);
        // Supply constant NO_OVERLAY (0, 10) on location 3 (UV1) since machines have no entity hurt/flash overlay.
        GL20.glDisableVertexAttribArray(3);
        GL30.glVertexAttribI2i(3, 0, 10);
        if (cachedEntLoc >= 0) {
            // Supply constant 0 for entity ID since multiblock machines are not entity-specific.
            GL20.glDisableVertexAttribArray(cachedEntLoc);
            GL30.glVertexAttribI2i(cachedEntLoc, 0, 0);
        }
        if (cachedMidTexLoc >= 0) {
            GL20.glEnableVertexAttribArray(cachedMidTexLoc);
            GL20.glVertexAttribPointer(cachedMidTexLoc, 2, GL11.GL_FLOAT, false, strideBytes, offMidTex);
        }
        if (cachedTangentLoc >= 0) {
            GL20.glEnableVertexAttribArray(cachedTangentLoc);
            GL20.glVertexAttribPointer(cachedTangentLoc, 4, GL11.GL_BYTE, true, strideBytes, offTangent);
        }
        if (cachedMidBlockLoc >= 0) {
            GL20.glEnableVertexAttribArray(cachedMidBlockLoc);
            GL20.glVertexAttribPointer(cachedMidBlockLoc, 4, GL11.GL_BYTE, false, strideBytes, offMidBlock);
        }
    }

    private static ShaderInstance slimAttribShader;
    private static long slimAttribPipelineGen = -1L;
    private static int slimLocPos = -2, slimLocUv = -2, slimLocNormal = -2;
    private static boolean slimAttribMismatchLogged = false;

    public static void bindSlimShadowAttributes(ShaderInstance shader, int outputSsbo) {
        bindIrisExtendedAttributes(shader, outputSsbo,
                BakeVramPoolManager.OUT_STRIDE_BYTES,
                BakeVramPoolManager.OUT_OFF_MIDTEX,
                BakeVramPoolManager.OUT_OFF_TANGENT,
                BakeVramPoolManager.OUT_OFF_MIDBLOCK);
    }

    public static int firstAttrib(int program, String... names) {
        for (String name : names) {
            int loc = GL20.glGetAttribLocation(program, name);
            if (loc >= 0) {
                return loc;
            }
        }
        return -1;
    }

    private static ShaderInstance matrixShader;
    private static long matrixPipelineGen = -1L;
    private static IrisDerivedMatrixUniforms.Locations matrixLocs =
            IrisDerivedMatrixUniforms.Locations.NONE;
    private static int projLoc = -2;

    public static boolean setMatrices(ShaderInstance shader, Matrix4f proj, Matrix4f modelView) {
        long gen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (matrixShader != shader || matrixPipelineGen != gen) {
            matrixShader = shader;
            matrixPipelineGen = gen;
            matrixLocs = IrisDerivedMatrixUniforms.resolve(shader);
            int program = shader.getId();
            projLoc = GL20.glGetUniformLocation(program, "iris_ProjMat");
            if (projLoc < 0) {
                projLoc = GL20.glGetUniformLocation(program, "ProjMat");
            }
        }
        IrisDerivedMatrixUniforms.uploadDerivedMatrices(matrixLocs, modelView);
        if (projLoc >= 0) {
            proj.get(PROJ_FLOATS);
            GL20.glUniformMatrix4fv(projLoc, false, PROJ_FLOATS);
        }
        return matrixLocs.modelView() >= 0 && projLoc >= 0;
    }

    private static final float[] PROJ_FLOATS = new float[16];

    public static void extractFrustumPlanes(Matrix4f clip, float[] out24) {
        com.hbm_m.client.render.culling.CpuFrustumCuller.extractPlanes(clip, out24, false);
    }

    public static long stampPlanes(float[] planes) {
        return com.hbm_m.client.render.culling.CpuFrustumCuller.stampPlanes(planes);
    }

    private static volatile long shadowDeclines = 0L;
    private static long shadowDeclineLogMs = Long.MIN_VALUE;

    public static void invalidateCaches() {
        extAttribShader = null;
        extAttribPipelineGen = -1L;
        cachedEntLoc = -2;
        cachedMidTexLoc = -2;
        cachedTangentLoc = -2;
        cachedMidBlockLoc = -2;
        slimAttribShader = null;
        slimAttribPipelineGen = -1L;
        slimLocPos = -2;
        slimLocUv = -2;
        slimLocNormal = -2;
        slimAttribMismatchLogged = false;
        matrixShader = null;
        matrixPipelineGen = -1L;
        matrixLocs = IrisDerivedMatrixUniforms.Locations.NONE;
        projLoc = -2;
    }

    public static long shadowDeclineCount() {
        return shadowDeclines;
    }

    public static void logShadowDecline(String reason, ShaderInstance shader) {
        shadowDeclines++;
        long now = System.currentTimeMillis();
        if (now - shadowDeclineLogMs < 30000L) {
            return;
        }
        shadowDeclineLogMs = now;
        String program = "(unknown)";
        try {
            program = shader != null ? shader.getName() + "#" + shader.getId() : "(no shader)";
        } catch (Throwable ignored) {
        }
        MainRegistry.LOGGER.info(
                "[HBM-M] NucleusGpuBaker: Tier 1 shadow bake declined - falling back to Tier 2/3 "
                        + "(reason: {}, program: {}) [{} declines this session]",
                reason, program, shadowDeclines);
    }
}
