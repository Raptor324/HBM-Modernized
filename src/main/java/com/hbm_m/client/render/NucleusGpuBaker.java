package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;

@OnlyIn(Dist.CLIENT)
/**
 * <b>Tier 1: GPU Compute Bake</b> — Bakes multiblock part instances into a
 * BLOCK-format vertex buffer using an OpenGL 4.3 compute shader, followed by
 * a single {@code glDrawElements} invocation using the active shaderpack's
 * native program (pack shadow or gbuffers).
 * <p>
 * Under this architecture, the shaderpack applies its own shadow distortion and gbuffer
 * encoding natively without requiring hardcoded static schemas or runtime AST regex
 * rewriting, ensuring full compatibility across shaderpacks (BSL, Complementary, Photon, etc.).
 * <p>
 * <b>Shadow Contract:</b> Shadow programs execute a round-trip projection sequence:
 * {@code P * M * (P^-1 * M^-1 * ftransform)}. For shadows, vertices are transformed
 * into shadow-space (recorded by {@link IrisShadowBatchCollector}), ModelViewMat is set
 * to identity, and ProjMat is set to P_shadow.
 * <p>
 * <b>Compute Pipeline:</b>
 * <ul>
 *   <li>SSBO 0: Companion mesh vertices (Iris ENTITY format, bound directly without CPU copying).</li>
 *   <li>SSBO 1: Instance records (46 floats per instance: pos+quat+bbox+light+uvRect+tint+grad+anim).</li>
 *   <li>SSBO 2: Output vertex buffer (verts * instances in a packed 52-byte layout).</li>
 *   <li>SSBO 3: Bone palette SSBO (up to 64 4x4 bone matrices for dynamic skeletal animations).</li>
 * </ul>
 * Indices are dynamically expanded on the CPU once per batch capacity and cached. A memory
 * barrier ({@code GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT}) synchronizes compute writes before draw submission.
 * <p>
 * <b>Fallback:</b> If GL < 4.3 compute shaders are unsupported or an error occurs,
 * {@link #isEnabled()} returns false and execution falls back to Tier 2 (ExtendedShader)
 * or Tier 3 (vanilla instancing).
 *
 * @credit CrankShaft / Flywheel / Iris
 */
public final class NucleusGpuBaker {

    private static final int LOCAL_SIZE = 64;
    private static final boolean KILL_SWITCH =
            !"false".equalsIgnoreCase(System.getProperty("hbm.gpuBake", "true"));

    private static boolean checked = false;
    private static boolean available = false;
    private static boolean logged = false;
    private static int programId = -1;
    /** Latches an initialization failure (e.g. GLSL compile error): without it every flush would retry compilation and spam the log with stack traces. */
    private static boolean initFailed = false;
    private static int uMeshStride, uInstStride, uOutStride, uVertCount, uInstCount;
    private static int uOffPos, uOffColor, uOffNormal, uOffUv2;
    private static int uOffUv0, uOffUv1, uOffMidTex, uOffTangent;
    private static int uBakeMode, uCamPos, uOffLight;
    private static int uTransformMode = -1;
    private static int uHasBones = -1;
    private static int uOffBoneId = -1;

    private static final int BONE_PALETTE_SIZE = 64;
    private static final int BONE_PALETTE_BYTES = BONE_PALETTE_SIZE * 16 * 4;
    private static int boneSsbo = -1;
    private static boolean hasBoneMatrices = false;

    // ── Dense OUTPUT bake layout (52 bytes, 4-byte aligned) ───────────
    // The input companion mesh uses IrisVertexFormats.ENTITY, whose stride
    // on 1.21.1 is 54 bytes (unaligned: invisible padding after normal).
    // Addressing it via floats caused an off-by-2 byte stride misalignment
    // and exploding triangles on 1.21.1/Iris (on 1.20.1/Oculus stride 56 is
    // 4-byte aligned). The compute shader reads inputs by byte offsets
    // using bitfield unpacking and writes to this dense 52-byte layout,
    // matching the attribute order of Iris ENTITY formats.
    private static final int OUT_STRIDE_BYTES = 52;
    private static final int OUT_OFF_POS = 0;      // 3F
    private static final int OUT_OFF_COLOR = 12;   // 4UB
    private static final int OUT_OFF_UV0 = 16;     // 2F
    private static final int OUT_OFF_UV1 = 24;     // 2S (integer pipeline)
    private static final int OUT_OFF_UV2 = 28;     // 2S (integer pipeline)
    private static final int OUT_OFF_NORMAL = 32;  // 3B + 1 pad
    private static final int OUT_OFF_ENTITY = 36;  // 2US
    private static final int OUT_OFF_MIDTEX = 40;  // 2F
    private static final int OUT_OFF_TANGENT = 48; // 4B

    private static int instanceSsbo = -1;
    private static long instanceSsboBytes = -1;
    private static int outputSsbo = -1;
    private static long outputSsboBytes = -1;

    private static final java.util.ArrayList<BakedMesh> BAKED = new java.util.ArrayList<>(64);

    /** GPU bake state for a single part renderer (associated with its companion mesh). */
    private static final class BakedMesh {
        final IrisCompanionMesh mesh;
        final int vaoId;
        final int eboId;
        final int[] baseIndices;
        final int vertCount;
        final int idxCount;
        int filledInstances = -1;

        BakedMesh(IrisCompanionMesh mesh, int vaoId, int eboId, int[] baseIndices,
                  int vertCount, int idxCount) {
            this.mesh = mesh;
            this.vaoId = vaoId;
            this.eboId = eboId;
            this.baseIndices = baseIndices;
            this.vertCount = vertCount;
            this.idxCount = idxCount;
        }
    }

    private NucleusGpuBaker() {}

    /** Returns true if Tier 1 compute bake is supported and enabled. Cheap after first call. */
    public static boolean isEnabled() {
        // nucleusGpuBake config is the primary source; -Dhbm.gpuBake=false is the emergency override.
        if (!com.hbm_m.config.ModClothConfig.get().nucleusGpuBake || !KILL_SWITCH) {
            return false;
        }
        if (!checked) {
            checked = true;
            try {
                var caps = org.lwjgl.opengl.GL.getCapabilities();
                // Baking needs compute + SSBO as a pair: on exotic hardware with
                // ARB_compute_shader but without ARB_shader_storage_buffer_object,
                // glBindBufferBase(SSBO) throws a GL error - cut that off here, ahead
                // of the catch-all in bake (which remains the last line of defense:
                // any dispatch failure disables Tier 1 permanently).
                available = (caps.OpenGL43 || caps.GL_ARB_compute_shader)
                        && (caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object);
                if (!available && !logged) {
                    logged = true;
                    com.hbm_m.main.MainRegistry.LOGGER.info(
                            "[HBM-M] NucleusGpuBaker: no compute shaders (GL<4.3, no ARB) - "
                                    + "Tier 1 disabled, Tier 2/3 stays active");
                }
            } catch (Throwable t) {
                available = false;
            }
        }
        return available;
    }

    /**
     * Tier 1 shadow flush: bakes shadow collector instance records and submits
     * a single draw call per part renderer using the active pack shadow shader.
     * The shader program must already be applied with its framebuffer bound.
     *
     * @return true if the shadow pass was successfully baked and drawn via Tier 1;
     *         false if falling back to Tier 2/3.
     */
    public static boolean bakeAndDrawShadow(List<IrisShadowBatchCollector.Entry> entries,
                                            ShaderInstance shader, Matrix4f shadowProj) {
        if (!isEnabled() || !RenderSystem.isOnRenderThread() || shader == null) {
            return false;
        }
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            if (programId == -1 && !initProgram()) {
                return false;
            }
            ensureSharedBuffers();
            // Draw state mirrors Tier 2 drawShadowInstances: multiblock machinery has
            // mixed winding order; culling is disabled to prevent missing faces.
            try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();

                for (int i = 0; i < entries.size(); i++) {
                IrisShadowBatchCollector.Entry e = entries.get(i);
                if (e.count <= 0) {
                    continue;
                }
                IrisCompanionMesh mesh = e.getCompanionMesh();
                if (mesh == null || !mesh.isBuilt()) {
                    bakeSkipLog(e, "companion unbuilt/failed");
                    continue;
                }
                if (mesh.getMeshVertexCount() <= 0 || mesh.getMeshStrideBytes() <= 0
                        || mesh.getMeshIndices() == null || mesh.getMeshIndices().length == 0) {
                    bakeSkipLog(e, "companion empty mesh (verts=" + mesh.getMeshVertexCount() + ")");
                    continue;
                }
                BakedMesh baked = getOrCreate(mesh);
                if (baked == null) {
                    continue;
                }
                FloatBuffer records = e.data.duplicate();
                records.flip();
                if (!dispatchBake(baked, records, e.count, MODE_SHADOW, 0f, 0f, 0f)) {
                    continue;
                }

                // ── Draw: activate pack shader program and set projection/model-view matrices.
                // Pack program must be active to avoid sending uniforms to compute program.
                GL20.glUseProgram(shader.getId());
                setMatrices(shader, shadowProj, IDENTITY_MV);
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                GL30.glBindVertexArray(baked.vaoId);
                GL11.glDrawElements(GL11.GL_TRIANGLES, baked.idxCount * e.count,
                        GL11.GL_UNSIGNED_INT, 0L);
                NucleusDebug.recordDraw(1, e.count, "GPU bake shadow");
            }

                // Pack program is active; restore previous VAO and VBO state cleanly.
                GL20.glUseProgram(shader.getId());
                com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            }
            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[HBM-M] NucleusGpuBaker shadow bake failed - "
                    + "falling back to Tier 2", t);
            available = false;
            releaseResourcesInternal();
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            return false;
        }
    }

    private static final int MODE_SHADOW = 0;
    private static final int MODE_MAIN = 1;

    /**
     * Core bake execution: uploads instance records, dispatches compute shader, and issues memory barriers.
     * After successful execution, outputSsbo contains verts * count vertices in BLOCK layout
     * (camera-relative for main pass), and the bake VAO is ready for a single glDrawElements call.
     */
    private static boolean dispatchBake(BakedMesh baked, FloatBuffer records, int count,
                                        int mode, float camX, float camY, float camZ) {
        if (!uploadInstances(records, count)) {
            return false;
        }
        // All input byte offsets must resolve: ldWord with -1 would cause out-of-bounds reads.
        int offPos = baked.mesh.getMeshOffset("position", -1);
        int offColor = baked.mesh.getMeshOffset("color", -1);
        int offNormal = baked.mesh.getMeshOffset("normal", -1);
        int offUv0 = baked.mesh.getMeshOffset("uv", 0);
        int offUv1 = baked.mesh.getMeshOffset("uv", 1);
        int offUv2 = baked.mesh.getMeshOffset("uv", 2);
        int offMidTex = baked.mesh.getMeshAttribByteOffsetOr("mc_midTexCoord",
                baked.mesh.getMeshOffset("genericFloat2", -1));
        int offTangent = baked.mesh.getMeshAttribByteOffsetOr("at_tangent",
                baked.mesh.getMeshOffset("genericByte4", -1));
        if (mode == MODE_SHADOW) {
            // Shadow passes write only depth/shadowcolor; position and UV0 are strictly required.
            if (offPos < 0 || offUv0 < 0) {
                return false;
            }
        } else {
            if ((offPos | offColor | offNormal | offUv0 | offUv1 | offUv2 | offMidTex | offTangent) < 0) {
                return false;
            }
        }

        int offBoneId = baked.mesh.getMeshAttribByteOffsetOr("bone_id",
                baked.mesh.getMeshAttribByteOffsetOr("boneId", -1));
        if (offBoneId < 0 && baked.mesh.getMeshStrideBytes() == 36) {
            offBoneId = 32;
        }

        expandIndices(baked, count);

        // ── Dispatch: activate compute program and set uniforms/SSBO bindings ──
        GL20.glUseProgram(programId);
        // uMeshStride: input mesh byte stride (e.g. 54 on 1.21.1, byte-assembled via ldWord).
        GL20.glUniform1i(uMeshStride, baked.mesh.getMeshStrideBytes());
        GL20.glUniform1i(uInstStride, InstancedStaticPartRenderer.INSTANCE_DATA_SIZE);
        GL20.glUniform1i(uOutStride, OUT_STRIDE_BYTES / 4);
        GL20.glUniform1i(uVertCount, baked.vertCount);
        GL20.glUniform1i(uInstCount, count);
        GL20.glUniform1i(uOffPos, offPos);
        GL20.glUniform1i(uOffColor, offColor);
        GL20.glUniform1i(uOffNormal, offNormal);
        GL20.glUniform1i(uOffUv0, offUv0);
        GL20.glUniform1i(uOffUv1, offUv1);
        GL20.glUniform1i(uOffUv2, offUv2);
        GL20.glUniform1i(uOffMidTex, offMidTex);
        GL20.glUniform1i(uOffTangent, offTangent);
        GL20.glUniform1i(uBakeMode, mode);
        GL20.glUniform1i(uOffLight, InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET);
        if (uTransformMode >= 0) {
            // Both main and shadow records are pos+quat now (IrisShadowBatchCollector
            // decomposes the affine pose, the record layout matches the shared VAO).
            GL20.glUniform1i(uTransformMode, 0);
        }
        if (uHasBones >= 0) {
            GL20.glUniform1i(uHasBones, hasBoneMatrices ? 1 : 0);
        }
        if (uOffBoneId >= 0) {
            GL20.glUniform1i(uOffBoneId, offBoneId);
        }
        if (uCamPos >= 0) {
            GL20.glUniform3f(uCamPos, camX, camY, camZ);
        }

        ensureOutputBuffer((long) baked.vertCount * count * OUT_STRIDE_BYTES);
        ensureBoneBuffer();
        // WAR synchronization with previous draw call: ensures shader storage and vertex attribute
        // arrays are synchronized before compute write.
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT
                | GL43.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, baked.mesh.getMeshVboId());
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, instanceSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, outputSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, boneSsbo);
        int total = baked.vertCount * count;
        GL43.glDispatchCompute((total + LOCAL_SIZE - 1) / LOCAL_SIZE, 1, 1);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, 0);
        GL42.glMemoryBarrier(GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        return true;
    }

    /**
     * Tier 1 main pass: bakes part instances into camera-relative vertices and draws with
     * a single glDrawElements using the active pack gbuffers shader.
     *
     * @param records instance records buffer (flip already performed by caller)
     * @param count number of instances
     * @param mesh companion mesh
     * @param packShader applied pack shader program
     * @param proj projection matrix
     * @param viewRot camera rotation matrix R_cam
     * @param camX camera X
     * @param camY camera Y
     * @param camZ camera Z
     * @return true if drawn via Tier 1; false if falling back to companion path
     */
    public static boolean bakeAndDrawMain(FloatBuffer records, int count, IrisCompanionMesh mesh,
                                          ShaderInstance packShader, Matrix4f proj, Matrix4f viewRot,
                                          float camX, float camY, float camZ) {
        if (!isEnabled() || !RenderSystem.isOnRenderThread() || packShader == null
                || mesh == null || !mesh.isBuilt() || count <= 0 || records == null) {
            return false;
        }
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            if (programId == -1 && !initProgram()) {
                return false;
            }
            BakedMesh baked = getOrCreate(mesh);
            if (baked == null || !dispatchBake(baked, records, count, MODE_MAIN, camX, camY, camZ)) {
                return false;
            }
            try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();

                GL20.glUseProgram(packShader.getId());
                setMatrices(packShader, proj, viewRot);
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                GL30.glBindVertexArray(baked.vaoId);
                bindIrisExtendedAttributes(packShader, baked);
                GL11.glDrawElements(GL11.GL_TRIANGLES, baked.idxCount * count,
                        GL11.GL_UNSIGNED_INT, 0L);
                NucleusDebug.recordDraw(1, count, "GPU bake main");
            }
            // Restore previous VAO and VBO state cleanly.
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[HBM-M] NucleusGpuBaker main bake failed - "
                    + "falling back to companion path", t);
            available = false;
            releaseResourcesInternal();
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            return false;
        }
    }

    /**
     * Binds Iris extended vertex attributes (mc_midTexCoord, at_tangent, iris_Entity) on the pack program.
     * Attributes are read from the dense 52-byte output layout.
     */
    private static void bindIrisExtendedAttributes(ShaderInstance shader, BakedMesh baked) {
        int program = shader.getId();
        int entLoc = GL20.glGetAttribLocation(program, "iris_Entity");
        if (entLoc >= 0) {
            // Supply constant 0 for entity ID since multiblock machines are not entity-specific.
            if (attribBound(entLoc)) {
                GL20.glDisableVertexAttribArray(entLoc);
            }
            GL30.glVertexAttribI2i(entLoc, 0, 0);
        }
        int midTexLoc = GL20.glGetAttribLocation(program, "mc_midTexCoord");
        if (midTexLoc >= 0 && !attribBound(midTexLoc)) {
            GL20.glEnableVertexAttribArray(midTexLoc);
            GL20.glVertexAttribPointer(midTexLoc, 2, GL11.GL_FLOAT, false,
                    OUT_STRIDE_BYTES, OUT_OFF_MIDTEX);
        }
        int tangentLoc = GL20.glGetAttribLocation(program, "at_tangent");
        if (tangentLoc >= 0 && !attribBound(tangentLoc)) {
            GL20.glEnableVertexAttribArray(tangentLoc);
            GL20.glVertexAttribPointer(tangentLoc, 4, GL11.GL_BYTE, true,
                    OUT_STRIDE_BYTES, OUT_OFF_TANGENT);
        }
    }

    private static boolean attribBound(int loc) {
        return GL20.glGetVertexAttribi(loc, GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED) == GL11.GL_TRUE;
    }

    /**
     * Sets ModelViewMat / ProjMat on the active pack program using raw GL uniform locations.
     * Raw locations avoid overhead and type mismatch issues with Minecraft's Uniform wrappers.
     */
    private static final Matrix4f IDENTITY_MV = new Matrix4f();
    private static final float[] MV_FLOATS = new float[16];
    private static final float[] PROJ_FLOATS = new float[16];
    private static ShaderInstance matrixShader;
    private static com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.Locations matrixLocs =
            com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.Locations.NONE;
    private static int projLoc = -2;

    private static void setMatrices(ShaderInstance shader, Matrix4f proj, Matrix4f modelView) {
        if (matrixShader != shader) {
            matrixShader = shader;
            matrixLocs = com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.resolve(shader);
            int program = shader.getId();
            projLoc = GL20.glGetUniformLocation(program, "iris_ProjMat");
            if (projLoc < 0) {
                projLoc = GL20.glGetUniformLocation(program, "ProjMat");
            }
        }
        modelView.get(MV_FLOATS);
        int locModelView = matrixLocs.modelView();
        if (locModelView >= 0) {
            GL20.glUniformMatrix4fv(locModelView, false, MV_FLOATS);
        }
        if (projLoc >= 0) {
            proj.get(PROJ_FLOATS);
            GL20.glUniformMatrix4fv(projLoc, false, PROJ_FLOATS);
        }
        // Derived matrices: required for shaderpacks (e.g. Photon) calculating world-space
        // normals via inverse model-view or normal matrices.
        int locInverse = matrixLocs.modelViewInverse();
        if (locInverse >= 0) {
            INVERSE_SCRATCH.set(modelView).invertAffine();
            INVERSE_SCRATCH.get(INV_FLOATS);
            GL20.glUniformMatrix4fv(locInverse, false, INV_FLOATS);
        }
        int locNormalMat = matrixLocs.normalMat();
        if (locNormalMat >= 0) {
            NORMAL_TMP.set(modelView);
            NORMAL_TMP.get(NORM_FLOATS);
            GL20.glUniformMatrix3fv(locNormalMat, false, NORM_FLOATS);
        }
    }

    private static final Matrix4f INVERSE_SCRATCH = new Matrix4f();
    private static final float[] INV_FLOATS = new float[16];
    private static final org.joml.Matrix3f NORMAL_TMP = new org.joml.Matrix3f();
    private static final float[] NORM_FLOATS = new float[9];

    // ── Initialization & Resources ─────────────────────────────────────

    private static boolean initProgram() {
        if (initFailed) {
            return false;
        }
        try {
            int p = GL20.glCreateProgram();
            int cs = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
            com.hbm_m.platform.RenderHooks.safeShaderSource(cs, COMPUTE_SOURCE);
            GL20.glCompileShader(cs);
            if (GL20.glGetShaderi(cs, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("compute compile: " + GL20.glGetShaderInfoLog(cs, 4096));
            }
            GL20.glAttachShader(p, cs);
            GL20.glLinkProgram(p);
            GL20.glDeleteShader(cs);
            if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("compute link: " + GL20.glGetProgramInfoLog(p, 4096));
            }
            programId = p;
            uMeshStride = GL20.glGetUniformLocation(p, "uMeshStride");
            uInstStride = GL20.glGetUniformLocation(p, "uInstStride");
            uOutStride = GL20.glGetUniformLocation(p, "uOutStride");
            uVertCount = GL20.glGetUniformLocation(p, "uVertCount");
            uInstCount = GL20.glGetUniformLocation(p, "uInstCount");
            uOffPos = GL20.glGetUniformLocation(p, "uOffPos");
            uOffColor = GL20.glGetUniformLocation(p, "uOffColor");
            uOffNormal = GL20.glGetUniformLocation(p, "uOffNormal");
            uOffUv2 = GL20.glGetUniformLocation(p, "uOffUv2");
            uOffUv0 = GL20.glGetUniformLocation(p, "uOffUv0");
            uOffUv1 = GL20.glGetUniformLocation(p, "uOffUv1");
            uOffMidTex = GL20.glGetUniformLocation(p, "uOffMidTex");
            uOffTangent = GL20.glGetUniformLocation(p, "uOffTangent");
            uBakeMode = GL20.glGetUniformLocation(p, "uBakeMode");
            uCamPos = GL20.glGetUniformLocation(p, "uCamPos");
            uOffLight = GL20.glGetUniformLocation(p, "uOffLight");
            uTransformMode = GL20.glGetUniformLocation(p, "uTransformMode");
            uHasBones = GL20.glGetUniformLocation(p, "uHasBones");
            uOffBoneId = GL20.glGetUniformLocation(p, "uOffBoneId");
            instanceSsbo = GL15.glGenBuffers();
            outputSsbo = GL15.glGenBuffers();
            ensureBoneBuffer();
            if (!logged) {
                logged = true;
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] NucleusGpuBaker: Tier 1 ACTIVE (programId={})", programId);
            }
            return true;
        } catch (Throwable t) {
            initFailed = true;
            com.hbm_m.main.MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker init failed - Tier 1 disabled", t);
            return false;
        }
    }

    private static void ensureSharedBuffers() {
        if (instanceSsboBytes == -1) {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, instanceSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, 4L << 20, GL15.GL_DYNAMIC_COPY);
            instanceSsboBytes = 4L << 20;
        }
        if (outputSsboBytes == -1) {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, 16L << 20, GL15.GL_DYNAMIC_COPY);
            outputSsboBytes = 16L << 20;
        }
        ensureBoneBuffer();
    }

    private static void ensureOutputBuffer(long bytes) {
        if (bytes > outputSsboBytes) {
            long newBytes = Math.max(bytes, outputSsboBytes * 2);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            outputSsboBytes = newBytes;
        }
    }

    private static boolean uploadInstances(FloatBuffer records, int count) {
        int floats = count * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        if (records == null || records.remaining() < floats) {
            return false;
        }
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, instanceSsbo);
        long bytes = (long) floats * 4L;
        if (bytes > instanceSsboBytes) {
            long newBytes = Math.max(bytes, instanceSsboBytes * 2);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            instanceSsboBytes = newBytes;
        }
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, records);
        return true;
    }

    /** CPU expansion of index buffer up to {@code count} instances (cached). */
    private static void expandIndices(BakedMesh baked, int count) {
        if (baked.filledInstances >= count) {
            return;
        }
        // GL_ELEMENT_ARRAY_BUFFER is part of VAO state: bind with our VAO active
        // to avoid overwriting external EBO bindings.
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        GL30.glBindVertexArray(baked.vaoId);
        IntBuffer data = MemoryUtil.memAllocInt(baked.idxCount * count);
        try {
            for (int i = 0; i < count; i++) {
                int base = i * baked.vertCount;
                for (int k = 0; k < baked.idxCount; k++) {
                    data.put(baked.baseIndices[k] + base);
                }
            }
            data.flip();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, baked.eboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, GL15.GL_DYNAMIC_DRAW);
            baked.filledInstances = count;
        } finally {
            MemoryUtil.memFree(data);
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(prevVao);
        }
    }

    private static BakedMesh getOrCreate(IrisCompanionMesh mesh) {
        for (int i = 0; i < BAKED.size(); i++) {
            if (BAKED.get(i).mesh == mesh) {
                return BAKED.get(i);
            }
        }
        try {
            int vertCount = mesh.getMeshVertexCount();
            int stride = mesh.getMeshStrideBytes();
            int[] base = mesh.getMeshIndices();
            var format = mesh.getMeshFormat();
            if (vertCount <= 0 || stride == 0 || base == null || base.length == 0 || format == null) {
                return null;
            }
            int vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);
            // Attributes point into outputSsbo written by compute shader.
            // Layout matches our dense 52-byte format (OUT_* offsets):
            // Locations 0..5 = Position/Color/UV0/UV1/UV2/Normal.
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, outputSsbo);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, OUT_STRIDE_BYTES, OUT_OFF_POS);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, OUT_STRIDE_BYTES, OUT_OFF_COLOR);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, OUT_STRIDE_BYTES, OUT_OFF_UV0);
            // UV1/UV2: 16-bit short integers bound via glVertexAttribIPointer.
            GL20.glEnableVertexAttribArray(3);
            GL30.glVertexAttribIPointer(3, 2, GL11.GL_SHORT, OUT_STRIDE_BYTES, OUT_OFF_UV1);
            GL20.glEnableVertexAttribArray(4);
            GL30.glVertexAttribIPointer(4, 2, GL11.GL_SHORT, OUT_STRIDE_BYTES, OUT_OFF_UV2);
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 3, GL11.GL_BYTE, true, OUT_STRIDE_BYTES, OUT_OFF_NORMAL);
            int ebo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL30.glBindVertexArray(0);
            BakedMesh baked = new BakedMesh(mesh, vao, ebo, base, vertCount, base.length);
            BAKED.add(baked);
            return baked;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker: baked mesh init failed", t);
            return null;
        }
    }

    /** Diagnostics tracking for skipped bake entries (prevents log spam). */
    private static final java.util.HashSet<Integer> SKIP_LOGGED = new java.util.HashSet<>();

    private static void bakeSkipLog(IrisShadowBatchCollector.Entry e, String reason) {
        int key = e.renderer != null ? System.identityHashCode(e.renderer) : System.identityHashCode(e.customMesh);
        if (SKIP_LOGGED.add(key)) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[HBM-M] NucleusGpuBaker: entry {} skipped in shadow bake ({}), instances={}",
                    key, reason, e.count);
        }
    }

    public static void setBoneMatrices(org.joml.Matrix4f[] bones) {
        if (!isEnabled()) return;
        if (bones == null || bones.length == 0) {
            hasBoneMatrices = false;
            return;
        }
        ensureBoneBuffer();
        int count = Math.min(bones.length, BONE_PALETTE_SIZE);
        FloatBuffer buf = MemoryUtil.memAllocFloat(count * 16);
        try {
            for (int i = 0; i < count; i++) {
                org.joml.Matrix4f m = bones[i];
                if (m != null) {
                    m.get(buf);
                    buf.position((i + 1) * 16);
                } else {
                    for (int r = 0; r < 4; r++) {
                        for (int c = 0; c < 4; c++) {
                            buf.put(r == c ? 1.0f : 0.0f);
                        }
                    }
                }
            }
            buf.flip();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, boneSsbo);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, buf);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            hasBoneMatrices = true;
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    public static void uploadBonePalette(FloatBuffer matrices, int count) {
        if (!isEnabled() || matrices == null || count <= 0) {
            hasBoneMatrices = false;
            return;
        }
        ensureBoneBuffer();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, boneSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, matrices);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        hasBoneMatrices = true;
    }

    private static void ensureBoneBuffer() {
        if (boneSsbo == -1) {
            boneSsbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, boneSsbo);
            FloatBuffer defaultBones = MemoryUtil.memAllocFloat(BONE_PALETTE_SIZE * 16);
            try {
                for (int i = 0; i < BONE_PALETTE_SIZE; i++) {
                    for (int r = 0; r < 4; r++) {
                        for (int c = 0; c < 4; c++) {
                            defaultBones.put(r == c ? 1.0f : 0.0f);
                        }
                    }
                }
                defaultBones.flip();
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, defaultBones, GL15.GL_DYNAMIC_DRAW);
            } finally {
                MemoryUtil.memFree(defaultBones);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            }
        }
    }

    private static void releaseResourcesInternal() {
        for (BakedMesh bm : BAKED) {
            GL30.glDeleteVertexArrays(bm.vaoId);
            GL15.glDeleteBuffers(bm.eboId);
        }
        BAKED.clear();
        if (programId != -1) {
            GL20.glDeleteProgram(programId);
            programId = -1;
        }
        if (instanceSsbo != -1) {
            GL15.glDeleteBuffers(instanceSsbo);
            instanceSsbo = -1;
            instanceSsboBytes = -1;
        }
        if (outputSsbo != -1) {
            GL15.glDeleteBuffers(outputSsbo);
            outputSsbo = -1;
            outputSsboBytes = -1;
        }
        if (boneSsbo != -1) {
            GL15.glDeleteBuffers(boneSsbo);
            boneSsbo = -1;
            hasBoneMatrices = false;
        }
    }

    // ── Compute Shader: one invocation per output vertex ───────────────
    // MODE_SHADOW (0): vertices in shadow-space, UV2 set to full light.
    // MODE_MAIN (1): vertices camera-relative (world - uCamPos), UV2 computed via trilinear interpolation.
    //
    // INPUT (SSBO 0): companion mesh in IrisVertexFormats.ENTITY format,
    // whose stride may not be a multiple of 4 (1.21.1 = 54 bytes). Therefore,
    // input data is addressed at word boundaries with byte-level extraction
    // (ldWord/ldFloat by byte offsets) to prevent misalignment.
    // OUTPUT (SSBO 2): dense 52-byte aligned vertex layout (divisible by 4):
    // pos 3f@0, color 4ub@12, uv0 2f@16, uv1 2s@24, uv2 2s@28,
    // normal 4b@32, iris_Entity 2us@36, mc_midTexCoord 2f@40, at_tangent 4b@48.

    private static final String COMPUTE_SOURCE = """
            #version 430 core
            layout(local_size_x = 64) in;

            layout(std430, binding = 0) readonly restrict buffer MeshBuf { uint meshW[]; };
            layout(std430, binding = 1) readonly restrict buffer InstBuf { float instData[]; };
            layout(std430, binding = 2) restrict buffer OutBuf { float outData[]; };
            layout(std430, binding = 3) readonly restrict buffer BonePaletteSSBO { mat4 uBoneMatrices[64]; };

            uniform int uMeshStride; // Input mesh byte stride (may be non-multiple of 4)
            uniform int uInstStride; // Instance record float stride (46: pose+bbox+light+uvRect+tint+grad+animParams)
            uniform int uOutStride;  // Output vertex float stride (13 floats = 52 bytes)
            uniform int uVertCount;
            uniform int uInstCount;
            uniform int uOffPos;     // Input byte offsets
            uniform int uOffColor;
            uniform int uOffNormal;
            uniform int uOffUv0;
            uniform int uOffUv1;
            uniform int uOffUv2;
            uniform int uOffMidTex;
            uniform int uOffTangent;
            uniform int uBakeMode;   // 0 shadow, 1 main
            uniform int uOffLight;   // Float offset of 8-corner light data (14)
            uniform int uTransformMode; // 0 pos+quat, 1 3x4 affine matrix
            uniform int uHasBones;      // 0 no bones, 1 bone palette active
            uniform int uOffBoneId;     // Byte offset of bone id in mesh (-1 if none)
            uniform vec3 uCamPos;

            uint ldWord(uint base, int byteOff) {
                if (byteOff < 0) return 0u;
                uint a = base + uint(byteOff);
                uint w0 = meshW[a >> 2u];
                uint sh = (a & 3u) * 8u;
                if (sh == 0u) return w0;
                uint w1 = meshW[(a >> 2u) + 1u];
                return (w0 >> sh) | (w1 << (32u - sh));
            }

            float ldFloat(uint base, int byteOff) {
                if (byteOff < 0) return 0.0;
                return uintBitsToFloat(ldWord(base, byteOff));
            }

            vec3 quatRotate(vec4 q, vec3 v) {
                return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);
            }

            void main() {
                uint gid = gl_GlobalInvocationID.x;
                uint total = uint(uVertCount) * uint(uInstCount);
                if (gid >= total) return;
                uint inst = gid / uint(uVertCount);
                uint lv = gid - inst * uint(uVertCount);

                uint mBase = lv * uint(uMeshStride);   // Input mesh byte offset
                uint iBase = uint(inst * uInstStride); // Instance record float offset
                uint o = gid * uint(uOutStride);       // Output vertex float offset

                vec3 pos = vec3(ldFloat(mBase, uOffPos),
                                ldFloat(mBase, uOffPos + 4),
                                ldFloat(mBase, uOffPos + 8));

                // Normal decode
                vec3 nrm;
                uint nRaw = 0u;
                if (uOffNormal >= 0) {
                    nRaw = ldWord(mBase, uOffNormal);
                    nrm = vec3(float(bitfieldExtract(int(nRaw), 0, 8)) / 127.0,
                               float(bitfieldExtract(int(nRaw), 8, 8)) / 127.0,
                               float(bitfieldExtract(int(nRaw), 16, 8)) / 127.0);
                } else {
                    nrm = vec3(0.0, 1.0, 0.0);
                }

                // Dynamic skeletal bone animation support
                vec4 localPos = vec4(pos, 1.0);
                if (uHasBones > 0 && uOffBoneId >= 0) {
                    int bone = int(ldWord(mBase, uOffBoneId));
                    if (bone >= 0 && bone < 64) {
                        localPos = uBoneMatrices[bone] * localPos;
                        nrm = mat3(uBoneMatrices[bone]) * nrm;
                    }
                }

                vec3 outPos;
                vec3 nOut;
                if (uTransformMode == 1) {
                    // Full 12-float 3x4 affine transform matrix (row0, row1, row2)
                    vec4 row0 = vec4(instData[iBase + 0u], instData[iBase + 1u], instData[iBase + 2u], instData[iBase + 3u]);
                    vec4 row1 = vec4(instData[iBase + 4u], instData[iBase + 5u], instData[iBase + 6u], instData[iBase + 7u]);
                    vec4 row2 = vec4(instData[iBase + 8u], instData[iBase + 9u], instData[iBase + 10u], instData[iBase + 11u]);

                    outPos = vec3(dot(row0, localPos), dot(row1, localPos), dot(row2, localPos));
                    if (uBakeMode == 1) {
                        outPos -= uCamPos;
                    }

                    // Cofactor / adjoint normal matrix for arbitrary affine transforms (transposed for GLSL column-major order)
                    mat3 normalMat = transpose(mat3(
                        cross(row1.xyz, row2.xyz),
                        cross(row2.xyz, row0.xyz),
                        cross(row0.xyz, row1.xyz)
                    ));
                    float nLenSq = dot(normalMat * nrm, normalMat * nrm);
                    nOut = (nLenSq > 1e-8) ? normalize(normalMat * nrm) : nrm;
                } else {
                    // Legacy pos(3) + quat(4)
                    vec4 rot = vec4(instData[iBase + 3u], instData[iBase + 4u],
                                    instData[iBase + 5u], instData[iBase + 6u]);
                    outPos = quatRotate(rot, localPos.xyz)
                            + vec3(instData[iBase + 0u], instData[iBase + 1u], instData[iBase + 2u]);
                    if (uBakeMode == 1) {
                        outPos -= uCamPos;
                    }
                    nOut = normalize(quatRotate(rot, nrm));
                }

                outData[o + 0u] = outPos.x;
                outData[o + 1u] = outPos.y;
                outData[o + 2u] = outPos.z;

                // Color: mesh color x per-instance tint (records 34..37) with a spatial
                // falloff (records 38..41). The gradient is evaluated on the MODEL-space
                // vertex position (pos, before the instance transform); the
                // mix(white, tint, smoothstep) semantics match block_lit_instanced.vsh
                // exactly. The 4UB channel clamps overbright (>1); glow brightness under
                // shader packs comes from uv2 (lightmap, see lightOverride).
                uint cRaw = (uOffColor >= 0) ? ldWord(mBase, uOffColor) : 0xFFFFFFFFu;
                float fade = clamp(instData[iBase + 13u], 0.0, 1.0);
                float tA = instData[iBase + 37u]; // emission strength (heat 0..1), NOT alpha
                float gAxis = instData[iBase + 38u];
                vec3 tCol = vec3(instData[iBase + 34u], instData[iBase + 35u], instData[iBase + 36u]);
                vec3 colMul;
                float gt = 0.0;
                if (gAxis >= 0.0) {
                    float gCoord = gAxis < 0.5 ? pos.x : (gAxis < 1.5 ? pos.y : pos.z);
                    gt = clamp((gCoord - instData[iBase + 40u])
                             / (instData[iBase + 39u] - instData[iBase + 40u]), 0.0, 1.0);
                    gt = gt * gt * (3.0 - 2.0 * gt);
                    colMul = mix(vec3(1.0), tCol, gt);
                } else {
                    colMul = tCol;
                }
                uint r8 = uint(clamp(float((cRaw >>  0) & 0xFFu) * colMul.r, 0.0, 255.0));
                uint g8 = uint(clamp(float((cRaw >>  8) & 0xFFu) * colMul.g, 0.0, 255.0));
                uint b8 = uint(clamp(float((cRaw >> 16) & 0xFFu) * colMul.b, 0.0, 255.0));
                uint a8 = uint(clamp(float((cRaw >> 24) & 0xFFu) * fade, 0.0, 255.0));
                outData[o + 3u] = uintBitsToFloat(r8 | (g8 << 8) | (b8 << 16) | (a8 << 24));

                // UV0 — 2 floats
                outData[o + 4u] = ldFloat(mBase, uOffUv0);
                outData[o + 5u] = ldFloat(mBase, uOffUv0 + 4);

                // UV1 (overlay) — 2 shorts packed into one word
                outData[o + 6u] = (uOffUv1 >= 0) ? uintBitsToFloat(ldWord(mBase, uOffUv1)) : 0.0;

                // UV2 (lightmap) — 2 ushorts packed into one word
                uint uv2Bits;
                if (uBakeMode == 0) {
                    uv2Bits = uint(240 | (240 << 16));
                } else {
                    vec3 bmin = vec3(instData[iBase + 7u], instData[iBase + 8u], instData[iBase + 9u]);
                    vec3 bsize = max(vec3(instData[iBase + 10u], instData[iBase + 11u],
                                          instData[iBase + 12u]), vec3(1e-4));
                    vec3 w = clamp((pos - bmin) / bsize, 0.0, 1.0);
                    uint lBase = iBase + uint(uOffLight);
                    vec2 c0 = vec2(instData[lBase + 0u], instData[lBase + 1u]);
                    vec2 c1 = vec2(instData[lBase + 2u], instData[lBase + 3u]);
                    vec2 c2 = vec2(instData[lBase + 4u], instData[lBase + 5u]);
                    vec2 c3 = vec2(instData[lBase + 6u], instData[lBase + 7u]);
                    vec2 c4 = vec2(instData[lBase + 8u], instData[lBase + 9u]);
                    vec2 c5 = vec2(instData[lBase + 10u], instData[lBase + 11u]);
                    vec2 c6 = vec2(instData[lBase + 12u], instData[lBase + 13u]);
                    vec2 c7 = vec2(instData[lBase + 14u], instData[lBase + 15u]);
                    vec2 x00 = mix(c0, c1, w.x);
                    vec2 x10 = mix(c2, c3, w.x);
                    vec2 x01 = mix(c4, c5, w.x);
                    vec2 x11 = mix(c6, c7, w.x);
                    vec2 y0 = mix(x00, x10, w.y);
                    vec2 y1 = mix(x01, x11, w.y);
                    vec2 lm = mix(y0, y1, w.z);
                    // Emission: push the lightmap toward fullbright by heat*gradient so
                    // the glow follows the tint (hot metal), not just lightOverride parts.
                    float emit = clamp(tA * gt, 0.0, 1.0);
                    lm = mix(lm, vec2(240.0), emit);
                    int bu = int(clamp(lm.x, 0.0, 240.0));
                    int sv = int(clamp(lm.y, 0.0, 240.0));
                    uv2Bits = uint(bu | (sv << 16));
                }
                outData[o + 7u] = uintBitsToFloat(uv2Bits);

                // Normal: packed 4 GLbytes
                uint nPacked = uint(int(round(clamp(nOut.x, -1.0, 1.0) * 127.0)) & 255)
                        | (uint(int(round(clamp(nOut.y, -1.0, 1.0) * 127.0)) & 255) << 8)
                        | (uint(int(round(clamp(nOut.z, -1.0, 1.0) * 127.0)) & 255) << 16)
                        | (nRaw & 0xFF000000u);
                outData[o + 8u] = uintBitsToFloat(nPacked);

                // iris_Entity — constant 0
                outData[o + 9u] = uintBitsToFloat(0u);

                // mc_midTexCoord — 2 floats
                outData[o + 10u] = (uOffMidTex >= 0) ? ldFloat(mBase, uOffMidTex) : 0.0;
                outData[o + 11u] = (uOffMidTex >= 0) ? ldFloat(mBase, uOffMidTex + 4) : 0.0;

                // at_tangent — 4 bytes packed into one word
                outData[o + 12u] = (uOffTangent >= 0) ? uintBitsToFloat(ldWord(mBase, uOffTangent)) : 0.0;
            }
            """;
}
