package com.hbm_m.client.render.culling;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.slf4j.Logger;

import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.MdiGeometryAtlas;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.logging.LogUtils;

/**
 * GPU compute shader occlusion culler for Nucleus MDI batches.
 * <p>
 * Dispatches {@code nucleus_cull.comp} to perform 6-plane view-frustum culling,
 * bounding sphere screen projection, Forward-Z Hi-Z depth footprint testing,
 * and atomic stream compaction directly into the compacted instance VBO and
 * MDI indirect command buffer.
 *
 * @credit CrankShaft / fewizz / Jdb100
 * Inspired by and adapted from the CrankShaft / Flywheel GPU culling compute pipeline.
 */
@OnlyIn(Dist.CLIENT)
public final class NucleusGpuCuller {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static int programId = 0;
    private static int uNumCommandsLoc = -1;
    private static int uTotalInstancesLoc = -1;
    private static int uPyramidLevelsLoc = -1;
    private static int uViewSizeLoc = -1;
    private static int uMip0SizeLoc = -1;
    private static int uViewMatLoc = -1;
    private static int uZNearLoc = -1;
    private static int uZFarLoc = -1;
    private static int uP00Loc = -1;
    private static int uP11Loc = -1;
    private static int uFrustumPlanesLoc = -1;
    private static int uFrustumOnlyLoc = -1;
    private static boolean initialized = false;

    private static final float[] FRUSTUM_PLANES = new float[24];
    private static final float[] MAT_FLOATS = new float[16];
    private static long lastDispatchLogTimeMs = 0L;

    /** GL_KHR_shader_subgroup query: size of a subgroup in invocations (LWJGL has no constant). */
    private static final int GL_SUBGROUP_SIZE_KHR = 0x9532;
    /** Default workgroup size; extended to 64 when hardware executes 64-thread wavefronts (AMD). */
    private static int subgroupSize = 32;
    private static Boolean subgroupBallotCapable = null;

    private NucleusGpuCuller() {}

    /**
     * CrankShaft pattern: subgroup ballot compaction is compiled in only when
     * GL_KHR_shader_subgroup is present AND the queried subgroup size matches the
     * workgroup (32 or 64). The shader additionally runtime-verifies workgroup == one
     * subgroup and falls back to atomic/shared compaction per workgroup.
     */
    private static boolean hasSubgroupBallot() {
        Boolean cached = subgroupBallotCapable;
        if (cached != null) {
            return cached;
        }
        boolean capable = false;
        try {
            GLCapabilities caps = GL.getCapabilities();
            if (caps.GL_KHR_shader_subgroup) {
                int queriedSize = GL11.glGetInteger(GL_SUBGROUP_SIZE_KHR);
                if (queriedSize == 32 || queriedSize == 64) {
                    subgroupSize = queriedSize;
                    capable = true;
                }
            }
        } catch (Throwable t) {
            capable = false;
        }
        subgroupBallotCapable = capable;
        return capable;
    }

    public static int getWorkgroupSize() {
        hasSubgroupBallot();
        return subgroupSize;
    }

    private static synchronized boolean ensureProgram() {
        if (initialized) {
            return programId > 0;
        }
        initialized = true;
        try {
            programId = compileComputeShader("/assets/hbm_m/shaders/cull/nucleus_cull.comp");
            uNumCommandsLoc = GL20.glGetUniformLocation(programId, "uNumCommands");
            uTotalInstancesLoc = GL20.glGetUniformLocation(programId, "uTotalInstances");
            uPyramidLevelsLoc = GL20.glGetUniformLocation(programId, "uPyramidLevels");
            uViewSizeLoc = GL20.glGetUniformLocation(programId, "uViewSize");
            uMip0SizeLoc = GL20.glGetUniformLocation(programId, "uMip0Size");
            uViewMatLoc = GL20.glGetUniformLocation(programId, "uViewMat");
            uZNearLoc = GL20.glGetUniformLocation(programId, "uZNear");
            uZFarLoc = GL20.glGetUniformLocation(programId, "uZFar");
            uP00Loc = GL20.glGetUniformLocation(programId, "uP00");
            uP11Loc = GL20.glGetUniformLocation(programId, "uP11");
            uFrustumPlanesLoc = GL20.glGetUniformLocation(programId, "uFrustumPlanes");
            uFrustumOnlyLoc = GL20.glGetUniformLocation(programId, "uFrustumOnly");

            LOGGER.info("[HBM-M] NucleusGpuCuller compute program initialized (programId={}, compaction={}, subgroupSize={})",
                    programId, hasSubgroupBallot() ? "subgroup-ballot" : "atomic-fallback", getWorkgroupSize());
            return true;
        } catch (Throwable t) {
            LOGGER.error("[HBM-M] Failed to initialize NucleusGpuCuller compute program", t);
            destroy();
            return false;
        }
    }

    private static String loadShaderSource(String resourcePath) {
        try (InputStream in = NucleusGpuCuller.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Shader resource not found: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read shader " + resourcePath, e);
        }
    }

    /**
     * Replaces the {@code #define NAME <fallback>u} marker lines for every
     * (name, wordOffset) pair with the Java-owned value. Fails loud on a missing
     * marker (the shader drifted from the injected contract).
     */
    private static String injectRecordDefines(String source, String resourcePath, Object... nameValuePairs) {
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            String name = (String) nameValuePairs[i];
            String define = "#define " + name + " " + nameValuePairs[i + 1] + "u";
            if (!source.contains(define)) {
                if (!source.contains("#define " + name)) {
                    throw new IllegalStateException(resourcePath + " is missing the " + name + " define marker");
                }
                source = source.replaceFirst("(?m)^#define " + name + " \\d+u$", define);
            }
        }
        return source;
    }

    private static int compileComputeShader(String resourcePath) {
        String source = loadShaderSource(resourcePath);
        // The instance record layout is owned by the InstancedStaticPartRenderer offset
        // constants and injected at load: hardcoded GLSL literals drifted before (34 vs
        // 38 after the tint attrib; then a stale 42u INST_STRIDE that survived only
        // because the loader reinjected it) and the compaction shredded every GPU-culled
        // batch. Fail loud on a missing marker so ensureProgram()'s catch-all drops us
        // to the CPU path instead of silently culling against a stale layout.
        source = injectRecordDefines(source, resourcePath,
                "INST_STRIDE", InstancedStaticPartRenderer.INSTANCE_DATA_SIZE,
                "INST_POS_WORD", InstancedStaticPartRenderer.POS_FLOAT_OFFSET,
                "INST_QUAT_WORD", InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET,
                "INST_BMIN_WORD", InstancedStaticPartRenderer.BBOX_MIN_FLOAT_OFFSET,
                "INST_BSIZE_WORD", InstancedStaticPartRenderer.BBOX_SIZE_FLOAT_OFFSET);
        // Subgroup ballot compaction (#extension must precede all non-preprocessor
        // tokens, so inject right after #version). Only injected when the extension
        // is advertised — with ": enable" an absent extension would break compilation
        // of the subgroup calls themselves.
        if (hasSubgroupBallot()) {
            source = source.replaceFirst("(?m)^#version 430 core$",
                    "#version 430 core\n"
                            + "#extension GL_KHR_shader_subgroup_basic : enable\n"
                            + "#extension GL_KHR_shader_subgroup_ballot : enable\n"
                            + "#define NUCLEUS_BALLOT 1\n"
                            + "#define SUBGROUP_SIZE " + getWorkgroupSize());
        }
        int shader = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        com.hbm_m.platform.RenderHooks.safeShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Failed to compile compute shader " + resourcePath + ": " + log);
        }
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(shader);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 4096);
            GL20.glDeleteProgram(program);
            throw new IllegalStateException("Failed to link compute program for " + resourcePath + ": " + log);
        }
        return program;
    }

    private static void computeFrustumPlanes(Matrix4f m, float[] out) {
        float m00 = m.m00(), m01 = m.m01(), m02 = m.m02(), m03 = m.m03();
        float m10 = m.m10(), m11 = m.m11(), m12 = m.m12(), m13 = m.m13();
        float m20 = m.m20(), m21 = m.m21(), m22 = m.m22(), m23 = m.m23();
        float m30 = m.m30(), m31 = m.m31(), m32 = m.m32(), m33 = m.m33();

        // Left: row 3 + row 0
        setNormalizedPlane(out, 0, m03 + m00, m13 + m10, m23 + m20, m33 + m30);
        // Right: row 3 - row 0
        setNormalizedPlane(out, 4, m03 - m00, m13 - m10, m23 - m20, m33 - m30);
        // Bottom: row 3 + row 1
        setNormalizedPlane(out, 8, m03 + m01, m13 + m11, m23 + m21, m33 + m31);
        // Top: row 3 - row 1
        setNormalizedPlane(out, 12, m03 - m01, m13 - m11, m23 - m21, m33 - m31);
        // Near: row 3 + row 2
        setNormalizedPlane(out, 16, m03 + m02, m13 + m12, m23 + m22, m33 + m32);
        // Far: row 3 - row 2
        setNormalizedPlane(out, 20, m03 - m02, m13 - m12, m23 - m22, m33 - m32);
    }

    private static void setNormalizedPlane(float[] out, int offset, float x, float y, float z, float w) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 1e-6f) {
            float inv = 1.0f / len;
            out[offset] = x * inv;
            out[offset + 1] = y * inv;
            out[offset + 2] = z * inv;
            out[offset + 3] = w * inv;
        } else {
            out[offset] = x;
            out[offset + 1] = y;
            out[offset + 2] = z;
            out[offset + 3] = w;
        }
    }

    /**
     * Executes GPU occlusion culling and stream compaction for all submitted MDI commands.
     *
     * @param frustumOnly true = skip Hi-Z sampling (frustum-only mode; pyramid not
     *                    regenerated this frame — adaptive low-population gate)
     * @return true if culling succeeded; false if unsupported or failed.
     */
    public static boolean cull(
            MdiGeometryAtlas atlas,
            int numCommands,
            int totalInstances,
            HiZDepthPyramid depthPyramid,
            Matrix4f viewMatrix,
            Matrix4f projMatrix,
            float zNear,
            float zFar,
            int viewWidth,
            int viewHeight,
            boolean frustumOnly) {

        if (atlas == null || !atlas.isReady() || numCommands <= 0 || totalInstances <= 0 || depthPyramid == null) {
            return false;
        }

        return cull(
                atlas.getInstanceVboId(),
                atlas.getCompactedInstanceVboId(),
                atlas.getIndirectBufferId(),
                numCommands,
                totalInstances,
                depthPyramid,
                viewMatrix,
                projMatrix,
                zNear,
                zFar,
                viewWidth,
                viewHeight,
                frustumOnly
        );
    }

    /**
     * Executes GPU occlusion culling and stream compaction on specific buffer handles.
     */
    public static boolean cull(
            int inInstanceVbo,
            int outInstanceVbo,
            int indirectBufferId,
            int numCommands,
            int totalInstances,
            HiZDepthPyramid depthPyramid,
            Matrix4f viewMatrix,
            Matrix4f projMatrix,
            float zNear,
            float zFar,
            int viewWidth,
            int viewHeight,
            boolean frustumOnly) {

        if (inInstanceVbo <= 0 || outInstanceVbo <= 0 || indirectBufferId <= 0 || numCommands <= 0 || totalInstances <= 0) {
            return false;
        }

        if (!ensureProgram()) {
            return false;
        }

        // Save/restore the caller's program: the exit used to bind 0, which left
        // Iris's ExtendedShader.lastApplied skip-gate inconsistent with the real
        // state for every pack-shader apply after this pass (parts disappearing /
        // uniform errors each frame the gate ran).
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GlStateManager._glUseProgram(programId);

        // Bind SSBOs
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, inInstanceVbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, outInstanceVbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, indirectBufferId);

        // Bind Hi-Z Depth Pyramid to texture unit 10 through GlStateManager (cache-coherent):
        // raw binds desync the per-unit cache — CrankShaft 1.5.2 stale-pyramid bug pattern.
        GlStateManager._activeTexture(GL13.GL_TEXTURE10);
        try {
            GlStateManager._bindTexture(depthPyramid.getPyramidTextureId());

            // Set uniforms
            if (uNumCommandsLoc >= 0) {
                GL30.glUniform1ui(uNumCommandsLoc, numCommands);
            }
            if (uTotalInstancesLoc >= 0) {
                GL30.glUniform1ui(uTotalInstancesLoc, totalInstances);
            }
            if (uPyramidLevelsLoc >= 0) {
                GL20.glUniform1i(uPyramidLevelsLoc, depthPyramid.getMipLevels());
            }
            if (uViewSizeLoc >= 0) {
                GL20.glUniform2f(uViewSizeLoc, (float) viewWidth, (float) viewHeight);
            }
            if (uMip0SizeLoc >= 0) {
                // The pyramid mip-0 is the pow2-snapped lower half of the screen
                // (HiZDepthPyramid.mip0Size) - the cull shader maps projected
                // rects against the ACTUAL mip-0 texel grid, not a fixed >>1.
                GL20.glUniform2f(uMip0SizeLoc,
                        (float) HiZDepthPyramid.mip0Size(viewWidth),
                        (float) HiZDepthPyramid.mip0Size(viewHeight));
            }
            if (uViewMatLoc >= 0) {
                viewMatrix.get(MAT_FLOATS);
                GL20.glUniformMatrix4fv(uViewMatLoc, false, MAT_FLOATS);
            }
            if (uZNearLoc >= 0) {
                GL20.glUniform1f(uZNearLoc, zNear);
            }
            if (uZFarLoc >= 0) {
                GL20.glUniform1f(uZFarLoc, zFar);
            }
            if (uP00Loc >= 0) {
                GL20.glUniform1f(uP00Loc, projMatrix.m00());
            }
            if (uP11Loc >= 0) {
                GL20.glUniform1f(uP11Loc, projMatrix.m11());
            }
            if (uFrustumPlanesLoc >= 0) {
                computeFrustumPlanes(projMatrix, FRUSTUM_PLANES);
                GL20.glUniform4fv(uFrustumPlanesLoc, FRUSTUM_PLANES);
            }
            if (uFrustumOnlyLoc >= 0) {
                GL30.glUniform1ui(uFrustumOnlyLoc, frustumOnly ? 1 : 0);
            }

            // Decoupled 1D instance tiling: dispatch flat tiles covering all instances
            int wgSize = getWorkgroupSize();
            int numWorkgroups = (totalInstances + wgSize - 1) / wgSize;
            GL43.glDispatchCompute(numWorkgroups, 1, 1);

            long now = System.currentTimeMillis();
            if (now - lastDispatchLogTimeMs >= 2000L) {
                lastDispatchLogTimeMs = now;
                LOGGER.info("[HBM-M GPU Cull] Compute dispatched: cmds={}, totalInstances={}, workgroups={}, pyramidMips={}, viewSize={}x{}, zNear={}, zFar={}, P00={}, P11={}",
                        numCommands, totalInstances, numWorkgroups, depthPyramid.getMipLevels(), viewWidth, viewHeight, zNear, zFar, projMatrix.m00(), projMatrix.m11());
            }

            // Memory barrier ensuring indirect buffer commands and compacted instance VBO are ready for drawing
            GL42.glMemoryBarrier(GL42.GL_COMMAND_BARRIER_BIT | GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT | GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
        } finally {
            // Unbind SSBOs
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);

            // Explicitly unbind Hi-Z pyramid texture from unit 10 to eliminate zombie texture bindings
            GlStateManager._activeTexture(GL13.GL_TEXTURE10);
            GlStateManager._bindTexture(0);

            GlStateManager._glUseProgram(previousProgram);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            // Compute dispatch left program 0 bound: force an honest shader rebind
            // on the next ensureFreshBind (recycled-id-safe).
            com.hbm_m.client.render.shader.ShaderBindResync.markForeignProgramBind();
        }

        return true;
    }

    public static void destroy() {
        if (programId > 0) {
            GL20.glDeleteProgram(programId);
            programId = 0;
        }
        initialized = false;
    }
}
