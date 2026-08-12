package com.hbm_m.client.render;


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
import com.hbm_m.config.ModClothConfig;
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
 * Instanced Renderer для статических частей (Base/Frame).
 * Без шейдеров рендерит все машины одного типа одним {@code glDrawElementsInstanced}.
 * Под Iris/Oculus переключается на per-machine draw через {@code ExtendedShader}
 * + companion VBO с {@code IrisVertexFormats.ENTITY} layout, что даёт корректный
 * G-buffer / shadow pass / pack uniforms.
 * <p>
 * Flush logic is delegated to {@link VanillaInstancedBatchRenderer} (vanilla path)
 * and {@link IrisInstancedBatchRenderer} (Iris/Oculus path).
 * GL compatibility helpers live in {@link InstancedGlCompat}.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
    // Sliced: lights @14..45 (8 vec4), fade in InstBboxSize.w @ 13
    static final int INSTANCE_ATTRIB_FIRST = 4;
    static final int LIGHT_FLOAT_OFFSET = 14;
    private static final int BASE_INSTANCE_DATA_SIZE = 30;
    private static final int SLICED_INSTANCE_DATA_SIZE = 46;

    final boolean useSlicedLight;
    final boolean storesPerInstancePartBone;
    final int instanceDataSize;
    final int instanceAttribLast;
    final int instanceFadeFloatOffset;
    final int lightFloatCount;

    int instanceCount = 0;
    final int[] instanceCullIndices = new int[maxInstances];
    final long[] instanceOcclusionKeys = new long[maxInstances];
    float batchSkyDarken = -1f;
    private boolean overflowLogged = false;
    static volatile boolean warnedInstancedShaderNullFlush;

    final float[] instanceLightUV = new float[maxInstances * 2];

    final Vector3f posTmp = new Vector3f();
    final Quaternionf rotTmp = new Quaternionf();
    final Matrix4f tmpLocalPose = new Matrix4f();
    final Matrix4f tmpInvViewRot = new Matrix4f();
    final float[] tmpCornerUV;

    int instanceVboId = -1;
    FloatBuffer instanceBuffer;
    private long instanceBufferAddress;

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

    // ── Scratch for addInstanceGpuBones ─────────────────────────────────
    private final Matrix4f tmpInstanceMat = new Matrix4f();

    // ── Constructors ───────────────────────────────────────────────────

    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data) {
        this(data, null, false, false);
    }
    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris) {
        this(data, quadsForIris, false, false);
    }
    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris, boolean useSlicedLight) {
        this(data, quadsForIris, useSlicedLight, false);
    }

    /**
     * @param storesPerInstancePartBone assembler arms: {@link #addInstanceGpuBones} (no MDI atlas).
     */
    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris, boolean useSlicedLight, boolean storesPerInstancePartBone) {
        this.quadsForIris = quadsForIris;
        this.useSlicedLight = useSlicedLight;
        this.storesPerInstancePartBone = storesPerInstancePartBone;
        this.instanceDataSize = useSlicedLight ? SLICED_INSTANCE_DATA_SIZE : BASE_INSTANCE_DATA_SIZE;
        this.instanceAttribLast = useSlicedLight ? 15 : 11;
        this.instanceFadeFloatOffset = 13; // InstBboxSize.w
        this.lightFloatCount = useSlicedLight ? 32 : 16;
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

            if (!useSlicedLight) {
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
            } else {
                for (int a = 0; a < 8; a++) {
                    int loc = 8 + a;
                    GL20.glEnableVertexAttribArray(loc);
                    GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, stride, (LIGHT_FLOAT_OFFSET + a * 4) * 4L);
                    InstancedGlCompat.glVertexAttribDivisorCompat(loc, 1);
                }
            }

            GL30.glBindVertexArray(0);

            instanceBuffer = MemoryUtil.memAllocFloat(maxInstances * instanceDataSize);
            this.instanceBufferAddress = MemoryUtil.memAddress0(instanceBuffer);
            final long bufferAddress = MemoryUtil.memAddress(instanceBuffer);
            instanceBufferCleanable = CLEANER.register(this, () -> {
                try {
                    if (bufferAddress != 0L) {
                        MemoryUtil.nmemFree(bufferAddress);
                    }
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("Failed to free instanceBuffer via Cleaner", t);
                }
            });

            if (!storesPerInstancePartBone && !useSlicedLight && data.byteBuffer != null && data.indices != null
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

            data.close();
            initialized = true;

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize InstancedStaticPartRenderer", e);
            cleanup();
            initialized = false;
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
        }
    }

    // ── Instance data write ────────────────────────────────────────────

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
        MemoryUtil.memPutFloat(a + 52, SingleMeshVboRenderer.getFadeAlpha());
        long lightA = a + (long) LIGHT_FLOAT_OFFSET * 4L;
        for (int i = 0; i < lightFloatCount; i++) {
            MemoryUtil.memPutFloat(lightA + (long) i * 4L, tmpCornerUV[i]);
        }
    }

    void uploadInstanceStreamToBoundVbo() {
        ModClothConfig cfg = ModClothConfig.get();
        if (cfg.instanceVboOrphanBeforeUpload) {
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) maxInstances * instanceDataSize * 4, GL15.GL_STREAM_DRAW);
        }
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);
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
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    RenderHooks.putBulkData(consumer, pose, quad, 1f, 1f, 1f, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        vanillaHelper.renderSingleVanilla(poseStack, packedLight, blockPos, blockEntity, bufferSource);
    }

    // ── addInstance ─────────────────────────────────────────────────────

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
        if (!initialized) return;

        //? if fabric {
        /*if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            if (irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    consumer.putBulkData(pose, quad, fade, fade, fade, packedLight, OverlayTexture.NO_OVERLAY);
                }
            }
            return;
        }
        *///?}

        //? if forge {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            if (irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }
        //?}

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
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }

        Matrix4f mat = poseStack.last().pose();
        mat.getTranslation(posTmp);
        mat.getNormalizedRotation(rotTmp);

        fillInstanceCornerLight(blockEntity, packedLight, blockPos, mat, sharedCornerUV8);

        instanceCullIndices[instanceCount] = -1;
        instanceOcclusionKeys[instanceCount] = OcclusionCullingHelper.occlusionKeyForBlock(blockPos);
        int baseFloat = instanceCount * instanceDataSize;
        memPutInstanceRecordAtBaseFloat(baseFloat);
        instanceCount++;
        ((Buffer) instanceBuffer).position(instanceDataSize * instanceCount);
    }

    // ── addInstanceGpuBones ────────────────────────────────────────────

    public void addInstanceGpuBones(PoseStack baseBlockPose, Matrix4f partLocalToBlock,
                                    int packedLight, BlockPos blockPos,
                                    @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        addInstanceGpuBones(baseBlockPose, partLocalToBlock, packedLight, blockPos, blockEntity, bufferSource, null);
    }

    public void addInstanceGpuBones(PoseStack baseBlockPose, Matrix4f partLocalToBlock,
                                    int packedLight, BlockPos blockPos,
                                    @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                                    @Nullable float[] sharedCornerUV8) {
        if (!initialized || !storesPerInstancePartBone) {
            addInstance(baseBlockPose, packedLight, blockPos, blockEntity, bufferSource, sharedCornerUV8);
            return;
        }

        //? if fabric {
        /*if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            PoseStack composed = new PoseStack();
            composed.pushPose();
            composed.last().pose().set(baseBlockPose.last().pose()).mul(partLocalToBlock);
            try {
                if (irisHelper.drawSingleWithIrisExtended(composed, packedLight, blockPos, blockEntity)) {
                    return;
                }
                if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                    float fade = SingleMeshVboRenderer.getFadeAlpha();
                    VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                    var pose = composed.last();
                    for (BakedQuad quad : quadsForIris) {
                        consumer.putBulkData(pose, quad, fade, fade, fade, packedLight, OverlayTexture.NO_OVERLAY);
                    }
                }
            } finally {
                composed.popPose();
            }
            return;
        }
        *///?}

        //? if forge {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            PoseStack composed = new PoseStack();
            composed.pushPose();
            composed.last().pose().set(baseBlockPose.last().pose()).mul(partLocalToBlock);
            try {
                if (irisHelper.drawSingleWithIrisExtended(composed, packedLight, blockPos, blockEntity)) {
                    return;
                }
                if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                    float fade = SingleMeshVboRenderer.getFadeAlpha();
                    VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                    var pose = composed.last();
                    for (BakedQuad quad : quadsForIris) {
                        consumer.putBulkData(pose, quad, 1f, 1f, 1f, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                    }
                }
            } finally {
                composed.popPose();
            }
            return;
        }
        //?}

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            PoseStack composed = new PoseStack();
            composed.pushPose();
            composed.last().pose().set(baseBlockPose.last().pose()).mul(partLocalToBlock);
            try {
                if (irisHelper.drawSingleWithIrisExtended(composed, packedLight, blockPos, blockEntity)) {
                    return;
                }
                if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                    float fade = SingleMeshVboRenderer.getFadeAlpha();
                    VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                    var pose = composed.last();
                    for (BakedQuad quad : quadsForIris) {
                        //? if forge {
                        consumer.putBulkData(pose, quad, 1f, 1f, 1f, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                        //?}
                        //? if fabric {
                        /*consumer.putBulkData(pose, quad, fade, fade, fade, packedLight, OverlayTexture.NO_OVERLAY);
                        *///?}
                    }
                }
            } finally {
                composed.popPose();
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
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }

        Matrix4f mat = baseBlockPose.last().pose();
        tmpInstanceMat.set(mat).mul(partLocalToBlock);
        tmpInstanceMat.getTranslation(posTmp);
        tmpInstanceMat.getNormalizedRotation(rotTmp);

        fillInstanceCornerLight(blockEntity, packedLight, blockPos, mat, sharedCornerUV8);

        int slot = instanceCount;
        int baseFloat = slot * instanceDataSize;
        memPutInstanceRecordAtBaseFloat(baseFloat);
        instanceCount++;
        ((Buffer) instanceBuffer).position(instanceDataSize * instanceCount);
    }

    public boolean usesGpuPartBonePath() {
        return storesPerInstancePartBone && isInitialized();
    }

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
            /*tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation()).invert();
            *///?}
            tmpLocalPose.set(tmpInvViewRot).mul(worldPose);
            tmpLocalPose.m30(tmpLocalPose.m30() - (float) (blockPos.getX() - cam.x));
            tmpLocalPose.m31(tmpLocalPose.m31() - (float) (blockPos.getY() - cam.y));
            tmpLocalPose.m32(tmpLocalPose.m32() - (float) (blockPos.getZ() - cam.z));
        }

        long partHash = System.identityHashCode(this);
        if (useSlicedLight) {
            LightSampleCache.getOrSample16(blockEntity, partHash, objBbox, blockPos, tmpLocalPose,
                    packedLight, tmpCornerUV);
        } else {
            LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, blockPos, tmpLocalPose,
                    packedLight, tmpCornerUV);
        }
    }

    // ── Flush ──────────────────────────────────────────────────────────

    @Override
    public void flush() {
        flush(RenderSystem.getProjectionMatrix());
    }

    //? if forge {
    public void flush(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        flush(event.getProjectionMatrix());
    }
    //?}
    //? if fabric {
    /*public void flush(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext event) {
        flush(event.projectionMatrix());
    }
    *///?}

    /**
     * Обязательный re-bind atlas + lightmap после {@link ShaderInstance#apply()} и перед glDraw*.
     * <p>
     * <b>РЕГРЕССИЯ-СТОП:</b> без этого instanced машины белые (Sampler2 читает unit 0 = atlas).
     * Делегат — {@link SingleMeshVboRenderer#bindBlockLitSamplerTextures}; не дублировать логику здесь.
     */
    static void bindBlockLitTexturesBeforeDraw(ShaderInstance shader) {
        SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);
    }

    /**
     * Вызывается из {@link com.hbm_m.client.render.culling.InstancedRenderFrame#presentAfterBlockEntities}
     * в том же кадре, что addInstance — не откладывать flush на конец уровня.
     */
    @Override
    public void flush(Matrix4f projectionMatrix) {
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

        final int instanceVboToDelete = this.instanceVboId;
        final Cleaner.Cleanable bufferCleanable = this.instanceBufferCleanable;
        final java.nio.ByteBuffer atlasVbToFree = this.atlasVertexBytesRetained;
        final java.nio.IntBuffer atlasIbToFree = this.atlasIndicesRetained;
        this.atlasVertexBytesRetained = null;
        this.atlasIndicesRetained = null;
        this.atlasIndexCountRetained = 0;

        this.instanceVboId = -1;
        this.instanceBuffer = null;
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
