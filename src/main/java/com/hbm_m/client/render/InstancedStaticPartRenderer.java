package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}


import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Instanced renderer for static parts (Base/Frame).
 * Without shaders it renders all machines of one type with a single {@code glDrawElementsInstanced}.
 * Under Iris/Oculus it switches to per-machine draws via {@code ExtendedShader}
 * + a companion VBO with the {@code IrisVertexFormats.ENTITY} layout, which yields correct
 * G-buffer / shadow pass / pack uniforms.
 * <p>
 * Flush logic is delegated to {@link VanillaInstancedBatchRenderer} (vanilla path)
 * and {@link IrisInstancedBatchRenderer} (Iris/Oculus path).
 * GL compatibility helpers live in {@link InstancedGlCompat}.
 */
@OnlyIn(Dist.CLIENT)
public class InstancedStaticPartRenderer extends AbstractGpuMesh
        implements VanillaInstancedMeshRenderer, IrisCompanionMeshRenderer {

    /** Per-part instance cap (one renderer = one mesh part, e.g. ChemPlant/Base). */
    final int maxInstances = ClientRenderFlags.maxInstances();
    private static final java.util.concurrent.atomic.AtomicInteger OVERFLOW_ADD_COUNT =
            new java.util.concurrent.atomic.AtomicInteger();

    /** Diagnostics: consumed by {@link com.hbm_m.client.render.culling.InstancedRenderStats}. */
    public static int drainOverflowAddCount() {
        return OVERFLOW_ADD_COUNT.getAndSet(0);
    }

    // Per-instance layout (floats):
    //   InstPos       vec3 (loc 4) @ 0
    //   InstRot       vec4 (loc 5) @ 3
    //   InstBboxMin   vec3 (loc 6) @ 7
    //   InstBboxSize  vec4 (loc 7) @ 10 — xyz extent, w = fade
    //   InstLightC01  vec4 (loc 8) @ 14   -- c0.uv, c1.uv
    //   InstLightC23  vec4 (loc 9) @ 18   -- c2.uv, c3.uv
    //   InstLightC45  vec4 (loc 10) @ 22   -- c4.uv, c5.uv
    //   InstLightC67  vec4 (loc 11) @ 26  -- c6.uv, c7.uv
    //   InstUvRect    vec4 (loc 12) @ 30  -- uv0.x, uv0.y, du, dv (sprite-local VBO → atlas;
    //                                     identity 0,0,1,1 for regular machines, see UVRECT_FLOAT_OFFSET)
    //   InstColor     vec4 (loc 13) @ 34  -- per-instance RGBA tint (part heat/glow; RGB
    //                                     may be > 1 - overbright), white = passthrough
    //   GradParams    vec4 (loc 14) @ 38  -- spatial falloff of the tint in MODEL coordinates:
    //                                     x = axis (0/1/2, <0 = off), y = full-tint
    //                                     coordinate (source), z = zero coordinate; smooth
    //                                     smoothstep between them (block_lit_instanced.vsh)
    //   AnimParams    vec4 (loc 15) @ 42  -- parametric GPU animation (see MachineSpecBuilder
    //                                     .parametricPart): xyz = joint parameters (angle in degrees
    //                                     / distance), w = joint index in NucleusJointSpecs
    //                                     (global RGBA32F spec texture: 2 texels per
    //                                     joint - kind+axis, pivot). w < 0 = part without parametrics.
    static final int INSTANCE_ATTRIB_FIRST = 4;
    static final int LIGHT_FLOAT_OFFSET = 14;
    static final int UVRECT_FLOAT_OFFSET = 30;
    static final int TINT_FLOAT_OFFSET = 34;
    static final int GRAD_FLOAT_OFFSET = 38;
    static final int ANIM_FLOAT_OFFSET = 42;
    /** public: read by the culling pipeline (NucleusGpuCuller) and modding artifacts. */
    public static final int INSTANCE_DATA_SIZE = 46;

    final int instanceDataSize;
    final int instanceAttribLast;
    final int instanceFadeFloatOffset;
    final int lightFloatCount;

    int instanceCount = 0;
    final int[] instanceCullIndices = new int[maxInstances];
    final long[] instanceOcclusionKeys = new long[maxInstances];
    float batchSkyDarken = -1f;
    private boolean overflowLogged = false;
    /** During this phase at least one actual write to instanceBuffer happened (skip-write failed at least once). */
    boolean mdiRecordWriteHappened = false;
    /**
     * The instanceBuffer contents are in sync with the MDI coordinator's snapshot:
     * the last flush went out as an accepted submit and no writes happened since.
     * Reset by the direct path (partition with reordering), renderSingle, and failed
     * submits; being true lets {@code submitClean} reuse the snapshot.
     */
    boolean mdiBufferSynced = false;
    static volatile boolean warnedInstancedShaderNullFlush;

    /**
     * Phase-2 deferred fading of the direct path (MDI fallback): {@code flush()} on the
     * direct path draws only opaque instances,
     * while fading ones are copied into {@link #fadingSnapshot}; they are picked up by
     * {@link #flushFading(Matrix4f)} AFTER the MDI multi-draw (see
     * InstancedRenderFrame). Otherwise translucent geometry drawn earlier than the
     * opaque MDI base (it goes to the coordinator and is drawn at the end of the flush)
     * writes depth and depth-rejects the base - the chunk shows through instead of it.
     */
    int deferredFadingCount = 0;
    FloatBuffer fadingSnapshot;

    final float[] instanceLightUV = new float[maxInstances * 2];

    /**
     * Current record uvRect ({u0, v0, du, dv}); identity {0,0,1,1} for regular machines.
     * Filled by {@link #addInstance} (the new 7-arg overload with uvRect) before writing
     * the record and reading {@code recordMatchesBuffer}; the shadow batch writes the same into its records.
     */
    final float[] tmpUvRect = {0f, 0f, 1f, 1f};
    private static final float[] IDENTITY_UV_RECT = {0f, 0f, 1f, 1f};

    /** Active uvRect (for shadow-batch records); always length 4. */
    float[] getActiveUvRect() {
        return tmpUvRect;
    }

    /**
     * Current record RGBA tint; RGB white = passthrough, A = emission strength (heat 0..1) -
     * shaders push the lightmap toward fullbright by tint*gradient (glow follows the tint).
     * Filled by {@link #addInstance} before writing the record; the shadow batch writes the same
     * into its records (the shadow FB does not use color - only the stride alignment matters).
     */
    final float[] tmpTint = {1f, 1f, 1f, 0f};
    private static final float[] WHITE_TINT = {1f, 1f, 1f, 0f};

    /**
     * Current spatial tint falloff {axis, fullCoord, zeroCoord, pad};
     * axis &lt; 0 = off (multiplier 1). The config is static per part
     * ({@code MachineSpecBuilder.tintFalloff}), but is written into the instance record -
     * the shader computes the gradient from the vertex's model coordinates.
     */
    final float[] tmpGrad = {-1f, 0f, 0f, 0f};

    /**
     * Current parametric joint parameters of the record (attrib 15 AnimParams):
     * xyz = parameters, w = joint index (&lt; 0 = no parametrics).
     */
    final float[] tmpAnimParams = {0f, 0f, 0f, -1f};

    // Pending parameters (static, like setFadeAlpha): MachineBer sets them
    // BEFORE renderer.enqueue of a parametric part; addInstance reads and
    // resets them. -1 = a regular (CPU-animated/static) part.
    private static float pendingAnimP0, pendingAnimP1, pendingAnimP2;
    private static int pendingJointIndex = -1;

    /** Set joint parameters for the next {@code addInstance} (parametric GPU animation). */
    public static void setPendingAnimParams(float p0, float p1, float p2, int jointIndex) {
        pendingAnimP0 = p0;
        pendingAnimP1 = p1;
        pendingAnimP2 = p2;
        pendingJointIndex = jointIndex;
    }

    /** Active falloff (for shadow-batch records); always length 4. */
    float[] getActiveGrad() {
        return tmpGrad;
    }

    /** Active tint (for shadow-batch records); always length 4. */
    float[] getActiveTint() {
        return tmpTint;
    }

    final Vector3f posTmp = new Vector3f();
    final Quaternionf rotTmp = new Quaternionf();
    final Matrix4f tmpLocalPose = new Matrix4f();
    final Matrix4f tmpInvViewRot = new Matrix4f();
    /** Scratch for converting composed(view) to a world transform (see {@link #convertToWorldRecord}). */
    private final Matrix4f tmpWorldMat = new Matrix4f();
    final float[] tmpCornerUV;

    int instanceVboId = -1;
    FloatBuffer instanceBuffer;
    private long instanceBufferAddress;

    // -- Direct-path VBO shadow (span-diff uploads) ----------------------
    // CPU copy of the instanceVboId contents: drawInstanceRange/flushFadingVanilla/
    // renderSingleVanilla upload only changed spans (see GpuSpanUploader).
    // Lazy alloc: only renderers that actually take the direct path (not MDI).
    private FloatBuffer vboShadow;
    private int vboShadowFloats = 0;
    private boolean vboShadowValid = false;
    private static long activeAnchorGen = -1L;
    private long lastAnchorGen = -1L;

    public static void onRenderOriginChanged() {
        activeAnchorGen = FrameViewState.anchorGeneration();
        // Records are anchor-relative: an anchor shift stales them all - the fast-path
        // roster assert must report a worldGen miss and fall back to a full rebuild.
        NucleusRenderVersion.bump();
    }
    /** VRAM estimate of this renderer (vertices+indices+instance VBO); 0 = not accounted. */
    private long vramBytes = 0;

    java.nio.ByteBuffer atlasVertexBytesRetained;
    java.nio.IntBuffer atlasIndicesRetained;
    int atlasIndexCountRetained;
    @Nullable
    private String mdiTraceTag;

    private static final Cleaner CLEANER = Cleaner.create();
    private Cleaner.Cleanable instanceBufferCleanable;

    final List<BakedQuad> quadsForIris;

    // ── Delegate helpers ───────────────────────────────────────────────
    final VanillaInstancedBatchRenderer vanillaHelper;
    private final IrisInstancedBatchRenderer irisHelper;

    // ── Scratch ─────────────────────────────────────────────────────────
    private final Matrix4f tmpCompositeMat = new Matrix4f();

    // ── Constructors ───────────────────────────────────────────────────

    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data) {
        this(data, null);
    }

    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris) {
        this.quadsForIris = quadsForIris;
        this.instanceDataSize = INSTANCE_DATA_SIZE;
        this.instanceAttribLast = 15;
        this.instanceFadeFloatOffset = 13; // InstBboxSize.w
        this.lightFloatCount = 16;
        this.tmpCornerUV = new float[lightFloatCount];

        this.vanillaHelper = new VanillaInstancedBatchRenderer(this);
        this.irisHelper = new IrisInstancedBatchRenderer(this);

        if (data == null) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer: Received NULL VboData! Cannot create renderer.");
            initialized = false;
            return;
        }
        if (!RenderSystem.isOnRenderThread()) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: Skipping initialization because this is not render thread.");
            data.close();
            initialized = false;
            return;
        }
        if (GLFW.glfwGetCurrentContext() == 0L) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: No current GLFW OpenGL context; falling back to non-instanced render path.");
            data.close();
            initialized = false;
            return;
        }
        if (!InstancedGlCompat.supportsInstancedAttributeDivisor()) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: Instancing entrypoints unavailable. Falling back to non-instanced render path.");
            data.close();
            initialized = false;
            return;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        try {
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();

            if (vaoId == 0 || vboId == 0) {
                throw new IllegalStateException("Failed to generate VAO/VBO!");
            }

            indexCount = data.indices != null ? data.indices.remaining() : 0;
            setObjBboxFrom(data);

            if (data.bytesPerVertex != SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES) {
                throw new IllegalStateException("InstancedStaticPartRenderer expects VboData.bytesPerVertex="
                        + SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES + " got " + data.bytesPerVertex);
            }
            int meshStride = data.bytesPerVertex;

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, meshStride, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, meshStride, 12);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, meshStride, 24);
            GL30.glEnableVertexAttribArray(3);
            GL30.glVertexAttribIPointer(3, 1, GL11.GL_INT, meshStride, 32);

            if (data.indices != null && data.indices.remaining() > 0) {
                eboId = GL15.glGenBuffers();
                if (eboId == 0) {
                    throw new IllegalStateException("Failed to generate EBO!");
                }
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
            }

            instanceVboId = GL15.glGenBuffers();
            if (instanceVboId == 0) {
                throw new IllegalStateException("Failed to generate instance VBO!");
            }

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) maxInstances * instanceDataSize * 4, GL15.GL_STREAM_DRAW);

            int stride = instanceDataSize * 4;

            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, 0);
            InstancedGlCompat.glVertexAttribDivisorCompat(4, 1);

            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, stride, 3 * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(5, 1);

            GL20.glEnableVertexAttribArray(6);
            GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, 7 * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(6, 1);

            GL20.glEnableVertexAttribArray(7);
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, 10 * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(7, 1);

            GL20.glEnableVertexAttribArray(8);
            GL20.glVertexAttribPointer(8, 4, GL11.GL_FLOAT, false, stride, LIGHT_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(8, 1);
            GL20.glEnableVertexAttribArray(9);
            GL20.glVertexAttribPointer(9, 4, GL11.GL_FLOAT, false, stride, (LIGHT_FLOAT_OFFSET + 4) * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(9, 1);
            GL20.glEnableVertexAttribArray(10);
            GL20.glVertexAttribPointer(10, 4, GL11.GL_FLOAT, false, stride, (LIGHT_FLOAT_OFFSET + 8) * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(10, 1);
            GL20.glEnableVertexAttribArray(11);
            GL20.glVertexAttribPointer(11, 4, GL11.GL_FLOAT, false, stride, (LIGHT_FLOAT_OFFSET + 12) * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(11, 1);

            // InstUvRect vec4 (loc 12): sprite-rect remap from normalized VBO to atlas.
            GL20.glEnableVertexAttribArray(12);
            GL20.glVertexAttribPointer(12, 4, GL11.GL_FLOAT, false, stride, UVRECT_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(12, 1);

            // InstColor vec4 (loc 13): per-instance RGBA tint (white = passthrough).
            GL20.glEnableVertexAttribArray(13);
            GL20.glVertexAttribPointer(13, 4, GL11.GL_FLOAT, false, stride, TINT_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(13, 1);

            // GradParams vec4 (loc 14): spatial tint falloff (axis<0 = off).
            GL20.glEnableVertexAttribArray(14);
            GL20.glVertexAttribPointer(14, 4, GL11.GL_FLOAT, false, stride, GRAD_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(14, 1);

            // AnimParams vec4 (loc 15): parametric GPU animation (xyz = parameters,
            // w = joint index; w < 0 = no parametrics - the shader skips the joint delta).
            GL20.glEnableVertexAttribArray(15);
            GL20.glVertexAttribPointer(15, 4, GL11.GL_FLOAT, false, stride, ANIM_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(15, 1);

            GL30.glBindVertexArray(0);

            instanceBuffer = MemoryUtil.memAllocFloat(maxInstances * instanceDataSize);
            fadingSnapshot = MemoryUtil.memAllocFloat(maxInstances * instanceDataSize);
            this.instanceBufferAddress = MemoryUtil.memAddress0(instanceBuffer);
            final long bufferAddress = MemoryUtil.memAddress(instanceBuffer);
            final long fadingAddress = MemoryUtil.memAddress(fadingSnapshot);
            instanceBufferCleanable = CLEANER.register(this, () -> {
                try {
                    if (bufferAddress != 0L) {
                        MemoryUtil.nmemFree(bufferAddress);
                    }
                    if (fadingAddress != 0L) {
                        MemoryUtil.nmemFree(fadingAddress);
                    }
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("Failed to free instanceBuffer via Cleaner", t);
                }
            });

            if (data.byteBuffer != null && data.indices != null
                    && data.indices.remaining() > 0) {
                try {
                    java.nio.ByteBuffer srcVb = data.byteBuffer.duplicate();
                    atlasVertexBytesRetained = MemoryUtil.memAlloc(srcVb.remaining());
                    atlasVertexBytesRetained.put(srcVb);
                    atlasVertexBytesRetained.flip();

                    java.nio.IntBuffer srcIb = data.indices.duplicate();
                    atlasIndexCountRetained = srcIb.remaining();
                    atlasIndicesRetained = MemoryUtil.memAllocInt(atlasIndexCountRetained);
                    atlasIndicesRetained.put(srcIb);
                    atlasIndicesRetained.flip();
                } catch (Throwable t) {
                    MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: failed to retain MDI atlas copy ({}); MDI path will skip this renderer", t.toString());
                    if (atlasVertexBytesRetained != null) {
                        MemoryUtil.memFree(atlasVertexBytesRetained);
                        atlasVertexBytesRetained = null;
                    }
                    if (atlasIndicesRetained != null) {
                        MemoryUtil.memFree(atlasIndicesRetained);
                        atlasIndicesRetained = null;
                    }
                    atlasIndexCountRetained = 0;
                }
            }

            long vram = (data.byteBuffer != null ? data.byteBuffer.remaining() : 0L)
                    + ((long) indexCount * 4L)
                    + (long) maxInstances * instanceDataSize * 4L;

            data.close();
            initialized = true;
            vramBytes = vram;
            NucleusDebug.addRendererVram(vram);

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize InstancedStaticPartRenderer", e);
            // super.cleanup() (AbstractGpuMesh) exits early on !initialized - and in
            // the constructor initialized is still false. Explicitly delete the
            // already-generated GL objects, otherwise an exception between glGen* and
            // the end of try leaks VAO/vertex VBO/EBO (instanceVboId is cleaned in cleanup() below).
            if (vaoId != -1) {
                try { GL30.glDeleteVertexArrays(vaoId); } catch (Throwable ignored) {}
                vaoId = -1;
            }
            if (vboId != -1) {
                try { GL15.glDeleteBuffers(vboId); } catch (Throwable ignored) {}
                vboId = -1;
            }
            if (eboId != -1) {
                try { GL15.glDeleteBuffers(eboId); } catch (Throwable ignored) {}
                eboId = -1;
            }
            cleanup();
            initialized = false;
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
        }
    }

    // ── Instance data write ────────────────────────────────────────────

    /**
     * Converts a composed view-space transform (R_cam*T(rel)*local, the contract is
     * identical on 1.20.1 and 1.21.1 - see {@link FrameViewState}) into world-space posTmp/rotTmp.
     * World instead of camera in the record => camera motion does not "dirty" the instances
     * (GpuSpanUploader gives zero upload for a static scene); the camera is applied in the vsh via ModelViewMat.
     */
    void convertToWorldRecord(Matrix4f composed) {
        tmpWorldMat.set(FrameViewState.inverseViewRotation()).mul(composed);
        tmpWorldMat.getTranslation(posTmp);
        posTmp.add(FrameViewState.relCamX(), FrameViewState.relCamY(), FrameViewState.relCamZ());
        tmpWorldMat.getNormalizedRotation(rotTmp);
        rotTmp.normalize();
    }

    void memPutInstanceRecordAtBaseFloat(int baseFloatIndex) {
        long a = instanceBufferAddress + (long) baseFloatIndex * 4L;
        MemoryUtil.memPutFloat(a, posTmp.x);
        MemoryUtil.memPutFloat(a + 4, posTmp.y);
        MemoryUtil.memPutFloat(a + 8, posTmp.z);
        MemoryUtil.memPutFloat(a + 12, rotTmp.x);
        MemoryUtil.memPutFloat(a + 16, rotTmp.y);
        MemoryUtil.memPutFloat(a + 20, rotTmp.z);
        MemoryUtil.memPutFloat(a + 24, rotTmp.w);
        MemoryUtil.memPutFloat(a + 28, objBbox[0]);
        MemoryUtil.memPutFloat(a + 32, objBbox[1]);
        MemoryUtil.memPutFloat(a + 36, objBbox[2]);
        float sx = objBbox[3] - objBbox[0];
        float sy = objBbox[4] - objBbox[1];
        float sz = objBbox[5] - objBbox[2];
        MemoryUtil.memPutFloat(a + 40, sx);
        MemoryUtil.memPutFloat(a + 44, sy);
        MemoryUtil.memPutFloat(a + 48, sz);
        // fade is quantized to 1/255: otherwise the smooth distance-based fade ramp changes
        // low bits every frame and the GpuSpanUploader span-diff never converges
        // to zero while the camera moves. 8 bits of alpha are visually indistinguishable.
        float fade = quantizeFade(SingleMeshVboRenderer.getFadeAlpha());
        MemoryUtil.memPutFloat(a + 52, fade);
        long lightA = a + (long) LIGHT_FLOAT_OFFSET * 4L;
        for (int i = 0; i < lightFloatCount; i++) {
            MemoryUtil.memPutFloat(lightA + (long) i * 4L, tmpCornerUV[i]);
        }
        long uvA = a + (long) UVRECT_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(uvA, tmpUvRect[0]);
        MemoryUtil.memPutFloat(uvA + 4, tmpUvRect[1]);
        MemoryUtil.memPutFloat(uvA + 8, tmpUvRect[2]);
        MemoryUtil.memPutFloat(uvA + 12, tmpUvRect[3]);
        long tintA = a + (long) TINT_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(tintA, tmpTint[0]);
        MemoryUtil.memPutFloat(tintA + 4, tmpTint[1]);
        MemoryUtil.memPutFloat(tintA + 8, tmpTint[2]);
        MemoryUtil.memPutFloat(tintA + 12, tmpTint[3]);
        long gradA = a + (long) GRAD_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(gradA, tmpGrad[0]);
        MemoryUtil.memPutFloat(gradA + 4, tmpGrad[1]);
        MemoryUtil.memPutFloat(gradA + 8, tmpGrad[2]);
        MemoryUtil.memPutFloat(gradA + 12, tmpGrad[3]);
        long animA = a + (long) ANIM_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(animA, tmpAnimParams[0]);
        MemoryUtil.memPutFloat(animA + 4, tmpAnimParams[1]);
        MemoryUtil.memPutFloat(animA + 8, tmpAnimParams[2]);
        MemoryUtil.memPutFloat(animA + 12, tmpAnimParams[3]);
    }

    /**
     * Compares the new record's components (posTmp/rotTmp/objBbox/fade/tmpCornerUV)
     * against the instanceBuffer contents at slot {@code baseFloat}. Records of the previous
     * flush persist in the buffer (clear() only resets position), so a match means
     * "the machine has not changed since the last flush" - the memPut can be skipped.
     *
     * <p>The comparison uses a tolerance (see {@link #recEq}), not exact equality: the world
     * record is produced via R^-1*(R_cam*T*local), and when the camera ROTATES the product
     * R^-1*R_cam ~= I introduces ulp jitter in the last bits - exact equality would mark
     * every machine dirty on each camera turn. The tolerances are orders of magnitude below
     * the visible threshold (~a pixel on screen ~= 2 mm at meter distances).
     */
    private boolean recordMatchesBuffer(int baseFloatIndex) {
        long a = instanceBufferAddress + (long) baseFloatIndex * 4L;
        if (!recEq(MemoryUtil.memGetFloat(a), posTmp.x, POS_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 4), posTmp.y, POS_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 8), posTmp.z, POS_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 12), rotTmp.x, ROT_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 16), rotTmp.y, ROT_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 20), rotTmp.z, ROT_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 24), rotTmp.w, ROT_EPS)) return false;
        if (MemoryUtil.memGetFloat(a + 28) != objBbox[0]) return false;
        if (MemoryUtil.memGetFloat(a + 32) != objBbox[1]) return false;
        if (MemoryUtil.memGetFloat(a + 36) != objBbox[2]) return false;
        float sx = objBbox[3] - objBbox[0];
        float sy = objBbox[4] - objBbox[1];
        float sz = objBbox[5] - objBbox[2];
        if (MemoryUtil.memGetFloat(a + 40) != sx) return false;
        if (MemoryUtil.memGetFloat(a + 44) != sy) return false;
        if (MemoryUtil.memGetFloat(a + 48) != sz) return false;
        float fade = quantizeFade(SingleMeshVboRenderer.getFadeAlpha());
        if (MemoryUtil.memGetFloat(a + 52) != fade) return false;
        long lightA = a + (long) LIGHT_FLOAT_OFFSET * 4L;
        for (int i = 0; i < lightFloatCount; i++) {
            if (!recEq(MemoryUtil.memGetFloat(lightA + (long) i * 4L), tmpCornerUV[i], LIGHT_EPS)) return false;
        }
        long uvA = a + (long) UVRECT_FLOAT_OFFSET * 4L;
        // Exact equality: rects come from stable sprite bounds and change
        // only on a skin change - then the record must be rewritten.
        if (MemoryUtil.memGetFloat(uvA) != tmpUvRect[0]) return false;
        if (MemoryUtil.memGetFloat(uvA + 4) != tmpUvRect[1]) return false;
        if (MemoryUtil.memGetFloat(uvA + 8) != tmpUvRect[2]) return false;
        if (MemoryUtil.memGetFloat(uvA + 12) != tmpUvRect[3]) return false;
        long tintA = a + (long) TINT_FLOAT_OFFSET * 4L;
        // Exact equality, as with uvRect: the tint resolver must converge to stable
        // values (otherwise skip-write degrades into per-frame writes - not a bug, but
        // cheaper to fix in the resolver itself).
        if (MemoryUtil.memGetFloat(tintA) != tmpTint[0]) return false;
        if (MemoryUtil.memGetFloat(tintA + 4) != tmpTint[1]) return false;
        if (MemoryUtil.memGetFloat(tintA + 8) != tmpTint[2]) return false;
        if (MemoryUtil.memGetFloat(tintA + 12) != tmpTint[3]) return false;
        long gradA = a + (long) GRAD_FLOAT_OFFSET * 4L;
        // Falloff is static per-part config: values are stable, comparison is exact.
        if (MemoryUtil.memGetFloat(gradA) != tmpGrad[0]) return false;
        if (MemoryUtil.memGetFloat(gradA + 4) != tmpGrad[1]) return false;
        if (MemoryUtil.memGetFloat(gradA + 8) != tmpGrad[2]) return false;
        if (MemoryUtil.memGetFloat(gradA + 12) != tmpGrad[3]) return false;
        long animA = a + (long) ANIM_FLOAT_OFFSET * 4L;
        // Joint parameters use exact equality (like uvRect/tint): the parameter resolver
        // must converge to stable values, otherwise a frozen part would rewrite the
        // record tail every frame (the span-diff would never converge to zero).
        if (MemoryUtil.memGetFloat(animA) != tmpAnimParams[0]) return false;
        if (MemoryUtil.memGetFloat(animA + 4) != tmpAnimParams[1]) return false;
        if (MemoryUtil.memGetFloat(animA + 8) != tmpAnimParams[2]) return false;
        if (MemoryUtil.memGetFloat(animA + 12) != tmpAnimParams[3]) return false;
        return true;
    }

    /** |a-b| <= eps (NaN equals nothing - conservatively writes the record). */
    private static boolean recEq(float a, float b, float eps) {
        return Math.abs(a - b) <= eps;
    }

    /** Tolerance for record world coordinates (~1 mm - subpixel even up close). */
    private static final float POS_EPS = 1.0e-3f;
    /** Tolerance for the record quaternion (~1e-4 rad ~= 0.006 deg - invisible). */
    private static final float ROT_EPS = 1.0e-5f;
    /** Tolerance for light angles (trilinear weights also pass through the camera-relative pose). */
    private static final float LIGHT_EPS = 1.0e-4f;

    /**
     * Whether the renderer is ready for {@code MdiBatchCoordinator.submitClean}: during this
     * phase not a single write to the buffer happened, and the buffer is in sync with the coordinator's snapshot.
     */
    boolean canSubmitMdiClean() {
        return ClientRenderFlags.mdiCleanFrameReuse() && !mdiRecordWriteHappened && mdiBufferSynced;
    }

    /** The buffer matches the coordinator's snapshot again (accepted submit / clean re-assert). */
    void noteMdiDispatched() {
        mdiBufferSynced = true;
    }

    /** Sync lost: direct path, partition reordering, renderSingle, submit refusal. */
    void noteMdiDispatchLost() {
        mdiBufferSynced = false;
    }

    /**
     * Span upload of the data window into the direct-path instance VBO (offset 0):
     * compares against the shadow copy and uploads only changed ranges.
     * Orphaning removed: when spans are skipped the GPU buffer must KEEP the old
     * content; glBufferData(orphan) would destroy it.
     */
    void uploadToInstanceVboSpanned(FloatBuffer src, int srcFloatOffset, int destFloatOffset, int floats) {
        if (floats <= 0 || instanceVboId <= 0) {
            return;
        }
        if (this.lastAnchorGen != activeAnchorGen) {
            this.lastAnchorGen = activeAnchorGen;
            this.vboShadowValid = false;
            this.mdiBufferSynced = false;
        }
        int need = 64;
        while (need < floats) {
            need <<= 1;
        }
        if (vboShadow == null || vboShadowFloats < floats) {
            if (vboShadow != null) {
                MemoryUtil.memFree(vboShadow);
            }
            vboShadow = MemoryUtil.memAllocFloat(need);
            vboShadowFloats = need;
            vboShadowValid = false;
        }
        if (vboShadowValid) {
            GpuSpanUploader.diffUpload(vboShadow, src, srcFloatOffset, destFloatOffset, floats, instanceVboId);
        } else {
            GpuSpanUploader.fullUpload(vboShadow, destFloatOffset, src, srcFloatOffset, floats, instanceVboId);
            vboShadowValid = true;
        }
    }

    void uploadInstanceStreamToBoundVbo() {
        // instanceBuffer is already flipped: valid records are [0, remaining).
        uploadToInstanceVboSpanned(instanceBuffer, 0, 0, instanceBuffer.remaining());
    }

    protected List<BakedQuad> getQuadsForIrisPath() {
        return quadsForIris;
    }

    // ── renderSingle ───────────────────────────────────────────────────

    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity) {
        renderSingle(poseStack, packedLight, blockPos, blockEntity, null);
    }

    @Override
    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                             @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!initialized || vaoId <= 0 || eboId <= 0 || indexCount <= 0 || instanceVboId <= 0 || instanceBuffer == null) return;

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            if (irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                // Fallback goes to bufferSource (drawn at endBatch) - the companion VAO
                // of the shadow batch must be detached (per-part release in shadow is disabled).
                com.hbm_m.client.render.shader.IrisRenderBatch.detachCompanionVaoForVanillaWork();
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    RenderHooks.putBulkDataShaded(consumer, pose, quad, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        vanillaHelper.renderSingleVanilla(poseStack, packedLight, blockPos, blockEntity, bufferSource);
    }

    // ── addInstance ─────────────────────────────────────────────────────

    /**
     * Fast-path dirty-skip: can slot {@code instanceCount} be confirmed by a roster
     * assert for machine {@code posKey} without rebuilding the record?
     * Roster keys live in {@link #instanceOcclusionKeys} (= pos.asLong(), written both
     * in addInstance and in the assert) - the buffer contents match slot k as long as
     * no record in [0, k] has been reordered/overwritten by another machine.
     * Read-only - safe to call during the checking phase (before commit).
     */
    public boolean canAssertInstance(long posKey) {
        return initialized && instanceBuffer != null
                && instanceCount < maxInstances
                && instanceOcclusionKeys[instanceCount] == posKey;
    }

    /**
     * Fade quantum of instance-buffer records: 1/255 (see {@link #memPutInstanceRecordAtBaseFloat}).
     * This is the single quantization point - the write, the skip-write comparison, and the
     * roster assert must all check the same quantum.
     */
    public static float quantizeFade(float fade) {
        return Math.round(fade * 255.0f) * (1.0f / 255.0f);
    }

    /**
     * Fast-path dirty-skip with a fade check: slot {@code instanceCount} is confirmed only
     * when both the roster key AND the quantized fade of the record in the buffer match.
     * <p>
     * Distance fade is a pure function of camera position and changes every frame of motion
     * WITHOUT a dirty event; a key-only assert freezes the record's alpha until a random
     * full rebuild (a pop instead of dissolving when moving away, a "stuck" translucent
     * when approaching). The check is a single float read from the buffer; machines with
     * fade=1 (the vast majority) pass right here.
     */
    public boolean canAssertInstance(long posKey, float expectedQuantizedFade) {
        if (!canAssertInstance(posKey)) {
            return false;
        }
        long fadeAddr = instanceBufferAddress
                + ((long) instanceCount * instanceDataSize + instanceFadeFloatOffset) * 4L;
        return MemoryUtil.memGetFloat(fadeAddr) == expectedQuantizedFade;
    }

    /**
     * Commits a roster assert for machine {@code blockPos}: the buffer already holds
     * last frame's record (clear() after a flush only resets position), so the instance
     * merely extends its presence - no matrices, no light, no float comparisons.
     * Call ONLY after {@link #canAssertInstance}.
     */
    public void assertCleanInstance(BlockPos blockPos) {
        if (instanceCount >= maxInstances) {
            return;
        }
        long key = OcclusionCullingHelper.occlusionKeyForBlock(blockPos);
        if (instanceOcclusionKeys[instanceCount] != key) {
            return; // defensive: the caller must check canAssertInstance
        }
        if (instanceCount == 0) {
            overflowLogged = false;
            mdiRecordWriteHappened = false;
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }
        instanceCullIndices[instanceCount] = -1;
        instanceCount++;
        ((Buffer) instanceBuffer).position(instanceDataSize * instanceCount);
    }

    /**
     * The instance-buffer contents no longer match the roster keys
     * (renderSingle overwrote slot 0). All canAssertInstance calls will start
     * missing -> machines fall back to a full rebuild.
     */
    void invalidateRoster() {
        if (instanceOcclusionKeys.length > 0) {
            instanceOcclusionKeys[0] = Long.MIN_VALUE;
        }
    }

    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, null);
    }

    @Override
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, null);
    }

    /**
     * Like {@link #addInstance(PoseStack, int, BlockPos, BlockEntity, MultiBufferSource)} but
     * reuses {@code sharedCornerUV8} (16 floats from {@link LightSampleCache#getOrSample8}) for all
     * parts of one machine in the same frame — avoids repeated spatial sampling at farm scale.
     */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            @Nullable float[] sharedCornerUV8) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, sharedCornerUV8, null);
    }

    /**
     * Full form: {@code uvRect} = {u0, v0, du, dv} remapping a sprite-local VBO into the atlas
     * (texture-variant door parts; null/identity - regular machines with atlas UVs);
     * {@code tint} = {r,g,b,a} per-instance color multiplier (null/white - passthrough;
     * RGB may be &gt; 1 - overbright heat, see block_lit InstColor).
     */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            @Nullable float[] sharedCornerUV8, @Nullable float[] uvRect) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, sharedCornerUV8, uvRect, null);
    }

    /** Full form with a per-instance tint; see the 7-arg overload for the other parameters. */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            @Nullable float[] sharedCornerUV8, @Nullable float[] uvRect,
                            @Nullable float[] tint) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, sharedCornerUV8, uvRect, tint, null);
    }

    /**
     * Full form with tint and falloff. {@code grad} = {axis (0/1/2, &lt;0 = off),
     * fullCoord, zeroCoord, pad} - per-part config ({@code MachineSpecBuilder.tintFalloff});
     * the falloff is applied in the shader from the vertex's model coordinates.
     */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            @Nullable float[] sharedCornerUV8, @Nullable float[] uvRect,
                            @Nullable float[] tint, @Nullable float[] grad) {
        if (!initialized) return;

        if (uvRect != null && uvRect.length >= 4) {
            tmpUvRect[0] = uvRect[0];
            tmpUvRect[1] = uvRect[1];
            tmpUvRect[2] = uvRect[2];
            tmpUvRect[3] = uvRect[3];
        } else {
            tmpUvRect[0] = 0f;
            tmpUvRect[1] = 0f;
            tmpUvRect[2] = 1f;
            tmpUvRect[3] = 1f;
        }
        if (tint != null && tint.length >= 4) {
            tmpTint[0] = tint[0];
            tmpTint[1] = tint[1];
            tmpTint[2] = tint[2];
            tmpTint[3] = tint[3];
        } else {
            tmpTint[0] = 1f;
            tmpTint[1] = 1f;
            tmpTint[2] = 1f;
            tmpTint[3] = 0f;
        }
        if (grad != null && grad.length >= 4 && grad[0] >= 0f) {
            tmpGrad[0] = grad[0];
            tmpGrad[1] = grad[1];
            tmpGrad[2] = grad[2];
            tmpGrad[3] = grad[3];
        } else {
            tmpGrad[0] = -1f;
            tmpGrad[1] = 0f;
            tmpGrad[2] = 0f;
            tmpGrad[3] = 0f;
        }
        // Parametric joint (attrib 15): read the pending values and reset them -
        // one write per enqueue. Not set (regular part) -> w = -1, the vertex
        // shader skips the joint delta.
        tmpAnimParams[0] = pendingAnimP0;
        tmpAnimParams[1] = pendingAnimP1;
        tmpAnimParams[2] = pendingAnimP2;
        tmpAnimParams[3] = (float) pendingJointIndex;
        pendingJointIndex = -1;

        // Shadow pass (both platforms): immediate drawing through the ACTIVE per-BE
        // batch (fast path, see IrisRenderBatch.begin). This block used to be forge-only:
        // on neoforge 1.21.1 instances accumulated in shadow with shadow matrices and were
        // then flushed in the main pass. Fallback without a batch - putBulkData via
        // bufferSource (SHADOW_BLOCK at endBatch).
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            // Global shadow batch: record instead of an immediate draw - one flush
            // for the whole BE phase (Iris mixin in ShadowRenderer.renderShadows). With
            // the batch disabled/failed - the previous immediate path.
            if (irisHelper.tryRecordShadowInstance(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                // Fallback to bufferSource with the shadow batch open - see the detach comment in renderSingle.
                // The tint is read from tmpTint (filled earlier in this addInstance) - shadows do
                // not use color, but alpha/overbright must not be lost on the fallback path.
                com.hbm_m.client.render.shader.IrisRenderBatch.detachCompanionVaoForVanillaWork();
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                float tr = tmpTint[0], tg = tmpTint[1], tb = tmpTint[2];
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    float shade = RenderHooks.quadShade(quad.getDirection());
                    RenderHooks.putBulkData(consumer, pose, quad,
                            shade * tr, shade * tg, shade * tb, fade, packedLight,
                            OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        if (instanceCount >= maxInstances) {
            OVERFLOW_ADD_COUNT.incrementAndGet();
            if (!overflowLogged) {
                overflowLogged = true;
                MainRegistry.LOGGER.warn(
                        "InstancedStaticPartRenderer overflow: maxInstances={} reached for tag={}, skipping extra instances until next flush",
                        maxInstances, mdiTraceTag);
            }
            return;
        }
        if (instanceCount == 0) {
            overflowLogged = false;
            mdiRecordWriteHappened = false;
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }

        Matrix4f mat = poseStack.last().pose();
        // VERDICT FROM VANILLA SOURCES 1.21.1 (GameRenderer.renderLevel + LevelRenderer.renderLevel):
        // R_cam (frustumMatrix) is passed SEPARATELY from the projection and pushed onto RenderSystem.getModelViewStack()
        // before the BE loop - that is, RenderSystem.getModelViewMatrix() == R_cam on BOTH versions,
        // while poseStack carries only T(blockPos - cam) * local. The previous "pose already contains the camera"
        // branch was wrong and dropped R_cam -> models flew across the screen. Stripping cancelled.
        // (Clarification 2026-09-12 from fresh sources: on 1.20.1 it is the reverse - R_cam is baked into the poseStack,
        // and modelViewStack is identity until AFTER_BLOCK_ENTITIES; composed = mvm*pose matches on both.)
        tmpCompositeMat.set(RenderSystem.getModelViewMatrix()).mul(mat);
        convertToWorldRecord(tmpCompositeMat);

        fillInstanceCornerLight(blockEntity, packedLight, blockPos, poseStack.last().pose(), sharedCornerUV8);

        instanceCullIndices[instanceCount] = -1;
        instanceOcclusionKeys[instanceCount] = OcclusionCullingHelper.occlusionKeyForBlock(blockPos);
        int baseFloat = instanceCount * instanceDataSize;
        // Skip-write: clear() after a flush only resets position - records of the previous
        // flush remain in the buffer. If the new record matches byte-for-byte (a static
        // machine: same pos/rot, quantized fade, same light), the memPut is skipped,
        // and the coordinator can reuse its snapshot in full (submitClean).
        if (!recordMatchesBuffer(baseFloat)) {
            memPutInstanceRecordAtBaseFloat(baseFloat);
            mdiRecordWriteHappened = true;
        }
        instanceCount++;
        ((Buffer) instanceBuffer).position(instanceDataSize * instanceCount);
    }

    /** Companion mesh for the global shadow batch ({@link IrisShadowBatchCollector}); built lazily. */
    @Nullable
    IrisCompanionMesh getOrBuildCompanionForShadowBatch() {
        return irisHelper.getOrBuildIrisCompanion();
    }

    /**
     * Instanced draw of shadow records: our ExtendedShader (vanilla-parity FSH - the
     * shadow FB needs no pack format), the parent VAO, span-diff of records into the instance
     * VBO, ONE {@code glDrawElementsInstanced} per part-renderer.
     * <p>
     * Records are shadow-space poses (exact decomposition of the BER PoseStack's T*R),
     * so ModelViewMat is HARD identity: iris_ModelViewMat from RenderSystem
     * must not be used (on Oculus 1.8 the stack in shadow may carry a shadow-MV - that was
     * the "crawling shadows" of the early variant). Clip = P_shadow * pose, exactly
     * the same contract as the pack-program drawCompanion flush.
     * {@code records} - position at the end of writing; the method flips/clears it itself.
     */
    boolean drawShadowInstances(java.nio.FloatBuffer records, int count,
                                ShaderInstance shader, Matrix4f proj) {
        if (!initialized || vaoId <= 0 || instanceVboId <= 0 || instanceBuffer == null || count <= 0) {
            return false;
        }
        try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            records.flip();
            uploadToInstanceVboSpanned(records, 0, 0, count * instanceDataSize);
            vanillaHelper.enableVertexAttribsForDraw();

            vanillaHelper.useIrisProgram(shader, proj, IDENTITY_MV);
            SingleMeshVboRenderer.primeIrisInstancedSamplerMap(shader, Minecraft.getInstance());
            // Pack shadow distortion - must match the pack's sampler
            // (IrisShadowDistortion extracts the mode/constants from the pack source).
            // ALL uniforms are declared "float" - set(int) on a float uniform throws
            // NPE (Uniform.intValues == null, debug.log 0913 23:41, BSL).
            com.mojang.blaze3d.shaders.Uniform uMode = shader.getUniform("hbmShadowDistortionMode");
            if (uMode != null) { uMode.set((float) com.hbm_m.client.render.shader.IrisShadowDistortion.mode()); uMode.upload(); }
            com.mojang.blaze3d.shaders.Uniform uDist = shader.getUniform("hbmShadowDistortion");
            if (uDist != null) { uDist.set(com.hbm_m.client.render.shader.IrisShadowDistortion.distortion()); uDist.upload(); }
            com.mojang.blaze3d.shaders.Uniform uScale = shader.getUniform("hbmShadowDepthScale");
            if (uScale != null) { uScale.set(com.hbm_m.client.render.shader.IrisShadowDistortion.depthScale()); uScale.upload(); }

            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, indexCount,
                    GL11.GL_UNSIGNED_INT, 0, count);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Iris shadow instanced draw failed", e);
            records.clear();
            return false;
        }
        records.clear();
        return true;
    }

    /** Identity ModelViewMat for the shadow instanced draw (records are already in shadow-space). */
    private static final Matrix4f IDENTITY_MV = new Matrix4f();

    /**
     * Fills {@link #tmpCornerUV} for the instanced VBO and, when Iris flush needs it,
     * {@link #instanceLightUV} for the current slot.
     *
     * <p>When {@code sharedCornerUV8} is supplied (one 8-corner sample per machine per frame),
     * vanilla path skips {@link LightSampleCache#getOrSample} and {@code tmpLocalPose} work.
     */
    private void fillInstanceCornerLight(@Nullable BlockEntity blockEntity, int packedLight,
                                         BlockPos blockPos, Matrix4f worldPose,
                                         @Nullable float[] sharedCornerUV8) {
        boolean hasSharedCorners = sharedCornerUV8 != null && sharedCornerUV8.length >= 16;
        boolean needsIrisInstanceLight = ShaderCompatibilityDetector.canUseIrisExtendedShader()
                || ShaderCompatibilityDetector.isExternalShaderActive();

        if (needsIrisInstanceLight) {
            int sampleBase = instanceCount * 2;
            LightSampleCache.getOrSample(blockEntity, packedLight, instanceLightUV, sampleBase);
        }

        if (hasSharedCorners) {
            System.arraycopy(sharedCornerUV8, 0, tmpCornerUV, 0, 16);
            return;
        }

        if (LightSampleCache.BASE_POSE_SET.get()) {
            tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(worldPose);
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            //? if < 1.21.1 {
            tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
            //?} else {
            /*// rotation(camera.rotation()) = R_cam^-1 (vanilla frustumMatrix =
            // rotation(rotation().conjugate()) = R_cam; the quaternion already carries
            // the INVERSE view rotation - as in FrameViewState.capture; an early
            // extra .invert() rotated the light sample points by R_cam^2 and
            // the light "crawled" over the machine on camera rotation.
            tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            *///?}
            tmpLocalPose.set(tmpInvViewRot).mul(worldPose);
            tmpLocalPose.m30(tmpLocalPose.m30() - (float) (blockPos.getX() - cam.x));
            tmpLocalPose.m31(tmpLocalPose.m31() - (float) (blockPos.getY() - cam.y));
            tmpLocalPose.m32(tmpLocalPose.m32() - (float) (blockPos.getZ() - cam.z));
        }

        long partHash = System.identityHashCode(this);
        LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, blockPos, tmpLocalPose,
                packedLight, tmpCornerUV);
    }

    // ── Flush ──────────────────────────────────────────────────────────

    @Override
    public void flush() {
        flush(RenderSystem.getProjectionMatrix());
    }


    /**
     * Mandatory re-bind of atlas + lightmap after {@link ShaderInstance#apply()} and before glDraw*.
     * <p>
     * <b>REGRESSION STOP:</b> without this, instanced machines render white (Sampler2 reads unit 0 = atlas).
     * Delegates to {@link SingleMeshVboRenderer#bindBlockLitSamplerTextures}; do not duplicate the logic here.
     */
    static void bindBlockLitTexturesBeforeDraw(ShaderInstance shader) {
        SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);
    }

    /**
     * Called from {@link com.hbm_m.client.render.culling.InstancedRenderFrame#presentAfterBlockEntities}
     * in the same frame as addInstance - do not defer the flush to the end of the level.
     */
    @Override
    public void flush(Matrix4f projectionMatrix) {
        // Last phase's deferred fading was either already drawn by flushFading(),
        // or staled (a second AFTER_BLOCK_ENTITIES pass per frame) - reset.
        deferredFadingCount = 0;
        if (instanceCount == 0) return;

        if (!initialized || vaoId <= 0 || eboId <= 0 || instanceVboId <= 0 || instanceBuffer == null) {
            instanceCount = 0;
            if (instanceBuffer != null) {
                instanceBuffer.clear();
            }
            return;
        }

        boolean useIrisFlush = ShaderCompatibilityDetector.canUseIrisExtendedShader();
        if (useIrisFlush) {
            irisHelper.flushBatchIris(projectionMatrix);
        } else if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // Iris/Oculus owns the GL program; vanilla instanced shaders would draw with
            // "No active program". Callers must fall back to per-BE VBO / putBulkData.
        } else {
            vanillaHelper.flushBatchVanilla(projectionMatrix);
        }

        instanceCount = 0;
        instanceBuffer.clear();
        overflowLogged = false;
    }

    /**
     * Phase 2 (called from InstancedRenderFrame AFTER the MDI dispatch): picks up the
     * fading instances deferred by the direct path in {@link #deferFading}.
     * Unconditionally clears the deferred state - skipping the call cannot
     * "leak" into the next frame. Under Iris there is never anything deferred
     * (flushBatchVanilla is not called there) - a natural no-op.
     */
    public void flushFading(Matrix4f projectionMatrix) {
        int count = deferredFadingCount;
        deferredFadingCount = 0;
        if (count <= 0) return;
        if (!initialized || vaoId <= 0 || eboId <= 0 || instanceVboId <= 0 || fadingSnapshot == null) return;
        if (ShaderCompatibilityDetector.isExternalShaderActive()) return;
        vanillaHelper.flushFadingVanilla(projectionMatrix, count);
    }

    /**
     * Copies the fading records [firstRecord, instanceCount) of the main buffer into the
     * phase-2 snapshot. Called from {@code flushBatchVanilla} after the partition;
     * instanceBuffer is at position=0 at that moment (after flip), with content already
     * reordered [opaque | fading].
     */
    void deferFading(int firstRecord, int count) {
        deferredFadingCount = count;
        if (count <= 0 || fadingSnapshot == null) return;
        long srcAddr = MemoryUtil.memAddress(instanceBuffer)
                + (long) firstRecord * instanceDataSize * 4L;
        MemoryUtil.memCopy(srcAddr, MemoryUtil.memAddress(fadingSnapshot),
                (long) count * instanceDataSize * 4L);
    }

    /**
     * Sort key for direct-path fading windows: the distSq of the FARTHEST fading instance
     * (record 0 of the snapshot - back-to-front within the renderer). -1 = no fade.
     * Used by MachineSpec.flushFading for the global back-to-front order of windows.
     */
    public float fadingSortKeyDistSq() {
        if (deferredFadingCount <= 0 || fadingSnapshot == null) return -1f;
        float dx = fadingSnapshot.get(0) - FrameViewState.relCamX();
        float dy = fadingSnapshot.get(1) - FrameViewState.relCamY();
        float dz = fadingSnapshot.get(2) - FrameViewState.relCamZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /** Fade below the threshold counts as fading (consistent with the blending minFade threshold). */
    static final float OPAQUE_FADE_THRESHOLD = 0.99f;

    /**
     * Reorders the instance-buffer records into [opaque in original order |
     * fading back-to-front by |InstPos|^2] and returns the number of opaque records.
     * Buffer: position 0, valid records [0, instanceCount*stride).
     * <p>
     * Shared phase-order G mechanism for MDI snapshots
     * ({@code MdiBatchCoordinator.partitionOpaqueFirst}) and the direct path
     * ({@code flushBatchVanilla}): "opaque before fading" must hold
     * identically across ALL render paths.
     *
     * @param parallelKeys optional parallel array of roster keys
     *                     (machine per slot): reordered in sync with the records so
     *                     the "key <-> record in slot" correspondence survives
     *                     the partition (fast-path dirty-skip). null - leave untouched.
     */
    /** Partition scratch: a growing persistent buffer instead of memAllocFloat/Free on every dirty frame. */
    private static FloatBuffer partitionScratch = null;
    private static long[] partitionScratchKeys = null;
    private static int[] partitionFadeIdx = null;
    private static float[] partitionFadeDistSq = null;

    private static FloatBuffer partitionScratch(int floats) {
        FloatBuffer buf = partitionScratch;
        if (buf == null || buf.capacity() < floats) {
            if (buf != null) {
                MemoryUtil.memFree(buf);
            }
            buf = MemoryUtil.memAllocFloat(Math.max(floats, 4096));
            partitionScratch = buf;
        }
        return buf;
    }

    static int partitionInstancesOpaqueFirst(FloatBuffer buf, int instanceCount, int floatsPerInstance, int fadeOffset,
                                             long[] parallelKeys) {
        int n = instanceCount;
        if (buf == null || n <= 0) return 0;

        int fadeTotal = 0;
        for (int i = 0; i < n; i++) {
            if (buf.get(i * floatsPerInstance + fadeOffset) < OPAQUE_FADE_THRESHOLD) {
                fadeTotal++;
            }
        }
        if (fadeTotal == 0) return n;

        FloatBuffer tmp = partitionScratch(n * floatsPerInstance);
        long[] tmpKeys = parallelKeys != null
                ? (partitionScratchKeys != null && partitionScratchKeys.length >= n
                        ? partitionScratchKeys
                        : (partitionScratchKeys = new long[n]))
                : null;
        int[] fadeIdx = partitionFadeIdx != null && partitionFadeIdx.length >= fadeTotal
                ? partitionFadeIdx : (partitionFadeIdx = new int[fadeTotal]);
        float[] fadeDistSq = partitionFadeDistSq != null && partitionFadeDistSq.length >= fadeTotal
                ? partitionFadeDistSq : (partitionFadeDistSq = new float[fadeTotal]);
        try {
            int opaqueWrote = 0;
            int k = 0;
            for (int i = 0; i < n; i++) {
                int recBase = i * floatsPerInstance;
                if (buf.get(recBase + fadeOffset) < OPAQUE_FADE_THRESHOLD) {
                    fadeIdx[k] = i;
                    float x = buf.get(recBase);
                    float y = buf.get(recBase + 1);
                    float z = buf.get(recBase + 2);
                    fadeDistSq[k] = x * x + y * y + z * z;
                    k++;
                } else {
                    copyInstanceRecord(buf, recBase, tmp, opaqueWrote * floatsPerInstance, floatsPerInstance);
                    if (tmpKeys != null) {
                        tmpKeys[opaqueWrote] = parallelKeys[i];
                    }
                    opaqueWrote++;
                }
            }
            // insertion sort by descending distSq (back-to-front); fading instances are usually few
            for (int a = 1; a < fadeTotal; a++) {
                int idx = fadeIdx[a];
                float d = fadeDistSq[a];
                int b = a - 1;
                while (b >= 0 && fadeDistSq[b] < d) {
                    fadeIdx[b + 1] = fadeIdx[b];
                    fadeDistSq[b + 1] = fadeDistSq[b];
                    b--;
                }
                fadeIdx[b + 1] = idx;
                fadeDistSq[b + 1] = d;
            }
            int dst = opaqueWrote;
            for (int a = 0; a < fadeTotal; a++, dst++) {
                copyInstanceRecord(buf, fadeIdx[a] * floatsPerInstance, tmp, dst * floatsPerInstance, floatsPerInstance);
                if (tmpKeys != null) {
                    tmpKeys[dst] = parallelKeys[fadeIdx[a]];
                }
            }
            MemoryUtil.memCopy(MemoryUtil.memAddress(tmp), MemoryUtil.memAddress(buf),
                    (long) n * floatsPerInstance * 4L);
            if (tmpKeys != null) {
                System.arraycopy(tmpKeys, 0, parallelKeys, 0, n);
            }
            return n - fadeTotal;
        } finally {
            // Scratch is persistent - do not free.
        }
    }

    private static void copyInstanceRecord(FloatBuffer src, int srcRecBase, FloatBuffer dst, int dstRecBase, int floats) {
        for (int f = 0; f < floats; f++) {
            dst.put(dstRecBase + f, src.get(srcRecBase + f));
        }
    }

    // ── IrisCompanionMeshRenderer interface ────────────────────────────

    @Override
    public void flushBatchIris(Matrix4f projectionMatrix) {
        irisHelper.flushBatchIris(projectionMatrix);
    }

    @Override
    public boolean drawSingleWithIrisExtended(PoseStack poseStack, int packedLight,
                                              BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        return irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity);
    }

    // ── State queries ──────────────────────────────────────────────────

    @Override
    public boolean isInitialized() {
        return initialized && vaoId > 0 && vboId > 0 && eboId > 0;
    }

    @Override
    public int getInstanceCount() {
        return instanceCount;
    }

    public void setMdiTraceTag(@Nullable String tag) {
        this.mdiTraceTag = tag;
    }

    @Nullable
    public String getMdiTraceTag() {
        return mdiTraceTag;
    }

    // ── Cleanup ────────────────────────────────────────────────────────

    @Override
    public void cleanup() {
        super.cleanup();

        // The coordinator's retained record for this renderer is no longer valid:
        // evictRendererIfRegistered will tear down the atlas slot, and the flags forbid submitClean.
        this.mdiBufferSynced = false;
        this.mdiRecordWriteHappened = false;

        final long vramToRelease = this.vramBytes;
        this.vramBytes = 0L;
        if (vramToRelease != 0L) {
            NucleusDebug.removeRendererVram(vramToRelease);
        }

        final int instanceVboToDelete = this.instanceVboId;
        final Cleaner.Cleanable bufferCleanable = this.instanceBufferCleanable;
        final FloatBuffer shadowToFree = this.vboShadow;
        this.vboShadow = null;
        this.vboShadowFloats = 0;
        this.vboShadowValid = false;
        final java.nio.ByteBuffer atlasVbToFree = this.atlasVertexBytesRetained;
        final java.nio.IntBuffer atlasIbToFree = this.atlasIndicesRetained;
        this.atlasVertexBytesRetained = null;
        this.atlasIndicesRetained = null;
        this.atlasIndexCountRetained = 0;

        this.instanceVboId = -1;
        this.instanceBuffer = null;
        this.fadingSnapshot = null;
        this.instanceBufferCleanable = null;
        vanillaHelper.invalidateShaderCache();

        irisHelper.cleanup();

        final InstancedStaticPartRenderer mdiEvictTarget = this;
        RenderSystem.recordRenderCall(() -> {
            try {
                com.hbm_m.client.render.MdiGeometryAtlas.evictRendererIfRegistered(mdiEvictTarget);
                if (instanceVboToDelete != -1) {
                    GL15.glDeleteBuffers(instanceVboToDelete);
                }
                if (bufferCleanable != null) {
                    bufferCleanable.clean();
                }
                if (shadowToFree != null) {
                    MemoryUtil.memFree(shadowToFree);
                }
                if (atlasVbToFree != null) {
                    MemoryUtil.memFree(atlasVbToFree);
                }
                if (atlasIbToFree != null) {
                    MemoryUtil.memFree(atlasIbToFree);
                }
            } catch (Exception e) {
                MainRegistry.LOGGER.error("InstancedStaticPartRenderer.cleanup failed", e);
            }
        });
    }
}
