package com.hbm_m.client.render;


import java.lang.ref.Cleaner;
import java.nio.FloatBuffer;
import java.util.List;


import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
*///?}
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//?}
/**
 * Instanced Renderer для статических частей (Base/Frame).
 * Без шейдеров рендерит все машины одного типа одним {@code glDrawElementsInstanced}.
 * Под Iris/Oculus переключается на per-machine draw через {@code ExtendedShader}
 * + companion VBO с {@code IrisVertexFormats.ENTITY} layout, что даёт корректный
 * G-buffer / shadow pass / pack uniforms.
 */
//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)//?}
public class InstancedStaticPartRenderer extends AbstractGpuMesh {

    private static final int MAX_INSTANCES = 1024;
    // Per-instance layout (floats):
    //   InstPos       vec3 (loc 3) @ 0
    //   InstRot       vec4 (loc 4) @ 3
    //   InstBboxMin   vec3 (loc 5) @ 7
    //   InstBboxSize  vec3 (loc 6) @ 10
    //   InstLightC01  vec4 (loc 7) @ 13   -- c0.uv, c1.uv
    //   InstLightC23  vec4 (loc 8) @ 17   -- c2.uv, c3.uv
    //   InstLightC45  vec4 (loc 9) @ 21   -- c4.uv, c5.uv
    //   InstLightC67  vec4 (loc 10) @ 25  -- c6.uv, c7.uv
    private static final int INSTANCE_ATTRIB_FIRST = 3;
    private static final int LIGHT_FLOAT_OFFSET = 13; // first float of light data in instance record
    private static final int BASE_INSTANCE_DATA_SIZE = 29; // legacy: 8 corners = 16 floats
    private static final int SLICED_INSTANCE_DATA_SIZE = 45; // 4 slices * 4 probes * 2 floats = 32 floats

    private final boolean useSlicedLight;
    private final int instanceDataSize;
    private final int instanceAttribLast;
    private final int lightFloatCount;

    private int instanceCount = 0;
    private float batchSkyDarken = -1f; // кэш getSkyDarken на батч
    private boolean overflowLogged = false;
    /** Один WARN на сессию, если instanced-шейдер null при flush батча (раньше на Fabric не вызывали registerFabricShaders). */
    private static volatile boolean warnedInstancedShaderNullFlush;

    /**
     * Per-instance smoothed lightmap UV2 - interleaved {@code (blockU, skyV)}
     * pairs in the same {@code (light << 4)} 0..240 scale that vanilla
     * {@code BufferBuilder.uv2} writes into the per-vertex format. Stored as
     * floats so we can preserve sub-integer averages from the multi-point
     * sampling done in {@link LightSampleCache#getOrSample} - that sub-integer
     * precision is what gives a smooth distance falloff between machines once
     * the bilinear lightmap-texture filter takes over in the pack shader.
     * <p>
     * CPU-only; the Iris draw loop reads from here and supplies the value as a
     * per-draw constant via {@code glVertexAttrib2f}.
     */
    private final float[] instanceLightUV = new float[MAX_INSTANCES * 2];
    /** Per-instance slot index into {@link IrisCompanionMesh}'s per-vertex lightmap VBO. */
    private final int[] instanceLightmapSlot = new int[MAX_INSTANCES];
    /** Scratch quantized probe UV2 for slot hashing (reused). */
    private final short[] tmpCornerShort;

    private final Vector3f posTmp = new Vector3f();
    private final Quaternionf rotTmp = new Quaternionf();
    /**
     * Scratch pose that represents the per-BE local transform (rotation, scale,
     * any small sub-block translation applied inside the BER) <b>without</b>
     * the camera's view rotation and <b>without</b> the {@code blockPos - cameraPos}
     * offset baked in by LevelRenderer. Combined with an explicit
     * integer {@link BlockPos}, this lets {@link LightSampleCache#getOrSample8}
     * derive world block sample positions as
     * {@code blockPos + floor(localPose * objCorner)} and keep the sampled
     * blocks stable as the camera moves. See the long comment in
     * {@link #addInstance}.
     */
    private final Matrix4f tmpLocalPose = new Matrix4f();
    /**
     * Scratch 4x4 holding the inverse of the camera's view rotation, used to
     * strip the view rotation baked into the BER pose stack before computing
     * the per-BE local pose for light sampling. See the long comment in
     * {@link #addInstance}.
     */
    private final Matrix4f tmpInvViewRot = new Matrix4f();
    /** Scratch probe buffers for {@link LightSampleCache} (reused). */
    private final float[] tmpCornerUV;
    /** Reusable quaternion for the Iris per-instance loop (avoid GC pressure). */
    private final Quaternionf irisQuatTmp = new Quaternionf();
    /** Reusable identity matrix for batch-level static uniform uploads. */
    private static final Matrix4f IDENTITY = new Matrix4f();
    
    private int instanceVboId = -1; // VBO для instance attributes
    private FloatBuffer instanceBuffer;

    private static final Cleaner CLEANER = Cleaner.create();
    private Cleaner.Cleanable instanceBufferCleanable;

    private ShaderInstance cachedShader = null;
    private int cachedShaderProgramId = -1;
    /**
     * Pipeline generation this uniform cache was built against - see
     * {@link com.hbm_m.client.render.shader.IrisExtendedShaderAccess#getPipelineGeneration()}.
     * Program IDs alone are unsafe as a cache key because GL drivers recycle
     * deleted IDs on pipeline rebuild; pairing with the generation counter
     * guarantees we re-resolve {@link Uniform} handles whenever the underlying
     * GL program was torn down and re-linked (shader-pack swap, settings-apply,
     * F3+T), preventing {@code GL_INVALID_OPERATION: Uniform must be a matrix
     * type} crashes when a recycled program has a non-matrix uniform at the
     * cached integer location.
     */
    private long cachedPipelineGeneration = -1L;
    private Uniform uProjMat;
    private Uniform uModelView;
    private Uniform uFogStart;
    private Uniform uFogEnd;
    private Uniform uFogColor;
    private Uniform uSampler0;
    private Uniform uBrightness;
    private Uniform uFadeAlpha;

    /**
     * Iris-extended uniform locations cached against {@link #cachedShader}'s
     * program id. {@code glGetUniformLocation} is a synchronous GL roundtrip,
     * and we re-resolve {@code iris_ModelViewMatInverse} / {@code iris_NormalMat}
     * on every {@link #flushBatchIris} - across all part types of all machines
     * that's ~22 redundant queries per frame. Cached lazily inside the flush.
     * Invalidated by {@link #onShaderChanged} when the shader instance pointer
     * actually changes.
     */
    private int cachedLocModelViewInverse = -1;
    private int cachedLocNormalMat = -1;
    private boolean irisLocationsResolved = false;

    private final List<BakedQuad> quadsForIris;
    /** Lazily-built companion mesh in Iris-extended {@code NEW_ENTITY} format. */
    @Nullable
    private IrisCompanionMesh irisCompanion;
    private boolean irisCompanionAttempted;

    private final Matrix4f tmpInstanceMat = new Matrix4f();
    /**
     * Reusable scratch buffers for the Iris-extended path. Held as fields so the
     * per-frame {@code flushBatchIris} loop and the per-call
     * {@code drawSingleWithIrisExtended} fall-through never touch the GC. Sized
     * to the maximum each consumer needs:
     * <ul>
     *   <li>{@code irisMvFloats[16]} - column-major instance ModelView pulled out of {@link #tmpInstanceMat}</li>
     *   <li>{@code irisMvInverseFloats[16]} - its inverse, uploaded to {@code iris_ModelViewMatInverse}</li>
     *   <li>{@code irisNormalMatFloats[9]} - {@code transpose(inverse(MV))} 3×3, uploaded to {@code iris_NormalMat}</li>
     *   <li>{@code irisMvInverseTmp} / {@code irisNormalTmp} - joml scratch matrices</li>
     *   <li>{@code irisSingleUV[2]} - {@link LightSampleCache} output for the single-instance fall-through path</li>
     * </ul>
     * All accessed only on the render thread, so a single set per renderer is safe.
     */
    private final float[] irisMvFloats = new float[16];
    private final float[] irisMvInverseFloats = new float[16];
    private final float[] irisNormalMatFloats = new float[9];
    private final Matrix4f irisMvInverseTmp = new Matrix4f();
    private final org.joml.Matrix3f irisNormalTmp = new org.joml.Matrix3f();
    private final float[] irisSingleUV = new float[2];

    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data) {
        this(data, null, false);
    }
    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris) {
        this(data, quadsForIris, false);
    }
    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris, boolean useSlicedLight) {
        this.quadsForIris = quadsForIris;
        this.useSlicedLight = useSlicedLight;
        this.instanceDataSize = useSlicedLight ? SLICED_INSTANCE_DATA_SIZE : BASE_INSTANCE_DATA_SIZE;
        this.instanceAttribLast = useSlicedLight ? 14 : 10;
        this.lightFloatCount = useSlicedLight ? 32 : 16;
        this.tmpCornerUV = new float[lightFloatCount];
        this.tmpCornerShort = new short[lightFloatCount];
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
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: No current GLFW OpenGL context (e.g. Sodium BE pass); falling back to non-instanced render path.");
            data.close();
            initialized = false;
            return;
        }
        if (!supportsInstancedAttributeDivisor()) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: Instancing entrypoints unavailable (no GL caps / divisor or draw-instanced). Falling back to non-instanced render path.");
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

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 32, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 32, 12);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24);

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
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_INSTANCES * instanceDataSize * 4, GL15.GL_STREAM_DRAW);

            int stride = instanceDataSize * 4;

            // InstPos (vec3) @ 0
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, stride, 0);
            glVertexAttribDivisorCompat(3, 1);

            // InstRot (vec4) @ 3 floats = 12 bytes
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, 3 * 4);
            glVertexAttribDivisorCompat(4, 1);

            // InstBboxMin (vec3) @ 7 floats = 28 bytes
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 3, GL11.GL_FLOAT, false, stride, 7 * 4);
            glVertexAttribDivisorCompat(5, 1);

            // InstBboxSize (vec3) @ 10 floats = 40 bytes
            GL20.glEnableVertexAttribArray(6);
            GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, 10 * 4);
            glVertexAttribDivisorCompat(6, 1);

            if (!useSlicedLight) {
                // InstLightC01 (vec4) @ 13 floats
                GL20.glEnableVertexAttribArray(7);
                GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, 13 * 4);
                glVertexAttribDivisorCompat(7, 1);
                // InstLightC23 (vec4) @ 17 floats
                GL20.glEnableVertexAttribArray(8);
                GL20.glVertexAttribPointer(8, 4, GL11.GL_FLOAT, false, stride, 17 * 4);
                glVertexAttribDivisorCompat(8, 1);
                // InstLightC45 (vec4) @ 21 floats
                GL20.glEnableVertexAttribArray(9);
                GL20.glVertexAttribPointer(9, 4, GL11.GL_FLOAT, false, stride, 21 * 4);
                glVertexAttribDivisorCompat(9, 1);
                // InstLightC67 (vec4) @ 25 floats
                GL20.glEnableVertexAttribArray(10);
                GL20.glVertexAttribPointer(10, 4, GL11.GL_FLOAT, false, stride, 25 * 4);
                glVertexAttribDivisorCompat(10, 1);
            } else {
                // 4 slices * 2 vec4 per slice = 8 vec4 attributes, starting at float offset 13.
                // loc 7  -> +0 floats
                // loc 8  -> +4
                // loc 9  -> +8
                // loc 10 -> +12
                // loc 11 -> +16
                // loc 12 -> +20
                // loc 13 -> +24
                // loc 14 -> +28
                for (int a = 0; a < 8; a++) {
                    int loc = 7 + a;
                    GL20.glEnableVertexAttribArray(loc);
                    GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, stride, (LIGHT_FLOAT_OFFSET + a * 4) * 4L);
                    glVertexAttribDivisorCompat(loc, 1);
                }
            }

            GL30.glBindVertexArray(0);

            instanceBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES * instanceDataSize);
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

    /**
     * Проверка возможности инстансинга и (при инициализации) вызовы divisor/draw должны
     * согласоваться: нельзя принимать только {@code glVertexAttribDivisorARB}, а вызывать
     * {@link GL33#glVertexAttribDivisor} — на части контекстов core entrypoint отсутствует
     * (нативный abort: функция недоступна в текущем контексте).
     * <p>
     * Без привязанного GLFW-контекста {@link GL#getCapabilities()} на потоке не используем —
     * иначе возможны устаревшие TLS capabilities (Sodium/Iris).
     */
    private static boolean supportsInstancedAttributeDivisor() {
        try {
            var caps = resolveGlCapabilities();
            if (caps == null) {
                return false;
            }
            boolean hasDivisor = caps.glVertexAttribDivisor != 0L || caps.glVertexAttribDivisorARB != 0L;
            boolean hasDrawInstanced = caps.glDrawElementsInstanced != 0L || caps.glDrawElementsInstancedARB != 0L;
            return hasDivisor && hasDrawInstanced;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static GLCapabilities resolveGlCapabilities() {
        if (GLFW.glfwGetCurrentContext() == 0L) {
            return null;
        }
        var caps = GL.getCapabilities();
        if (caps != null) {
            return caps;
        }
        try {
            GL.createCapabilities();
        } catch (Throwable ignored) {
            return null;
        }
        return GL.getCapabilities();
    }

    /** Core GL 3.3 divisor, иначе {@link ARBInstancedArrays} (должно совпадать с {@link #supportsInstancedAttributeDivisor}). */
    private static void glVertexAttribDivisorCompat(int index, int divisor) {
        GLCapabilities caps = GL.getCapabilities();
        if (caps != null && caps.glVertexAttribDivisor != 0L) {
            GL33.glVertexAttribDivisor(index, divisor);
        } else {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
        }
    }

    /** Core GL 3.1 draw instanced, иначе {@link ARBDrawInstanced}. */
    private static void glDrawElementsInstancedCompat(int mode, int count, int type, long indices, int primcount) {
        GLCapabilities caps = GL.getCapabilities();
        if (caps != null && caps.glDrawElementsInstanced != 0L) {
            GL31.glDrawElementsInstanced(mode, count, type, indices, primcount);
        } else {
            ARBDrawInstanced.glDrawElementsInstancedARB(mode, count, type, indices, primcount);
        }
    }

    /** Квады для пути совместимого с Iris. */
    protected List<BakedQuad> getQuadsForIrisPath() {
        return quadsForIris;
    }

    /**
     * Returns the lazily-built companion mesh (Iris ENTITY format), or {@code null}
     * if none can be built (e.g. Iris not loaded, no quads, off-thread call).
     */
    @Nullable
    private IrisCompanionMesh getOrBuildIrisCompanion() {
        if (irisCompanion != null && irisCompanion.isBuilt()) return irisCompanion;
        if (irisCompanion != null && irisCompanion.isFailed()) return null;
        if (irisCompanionAttempted && irisCompanion == null) return null;

        if (quadsForIris == null || quadsForIris.isEmpty()) {
            irisCompanionAttempted = true;
            return null;
        }
        if (irisCompanion == null) {
            irisCompanion = new IrisCompanionMesh(quadsForIris);
            irisCompanionAttempted = true;
        }
        return irisCompanion.ensureBuilt() ? irisCompanion : null;
    }

    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity) {
        renderSingle(poseStack, packedLight, blockPos, blockEntity, null);
    }

    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
        @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!initialized || vaoId <= 0 || eboId <= 0 || indexCount <= 0) return;

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // Try the Iris ExtendedShader path through our companion mesh.
            if (ShaderCompatibilityDetector.useNewIrisVboPath()) {
                if (drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                    return;
                }
            }
            // Fallback to the classic putBulkData path that defers to Iris's pipeline.
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    //? if forge {
                    /*consumer.putBulkData(pose, quad, fade, fade, fade, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                    *///?} else {
                    consumer.putBulkData(pose, quad, fade, fade, fade, packedLight, OverlayTexture.NO_OVERLAY);
                    //?}
                }
            }
            return;
        }

        // --- VANILLA VBO PATH ---

        ShaderInstance shader = useSlicedLight ? ModShaders.getBlockLitInstancedSlicedShader()
                                               : ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                PoseStack.Pose pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    //? if forge {
                    /*consumer.putBulkData(pose, quad, fade, fade, fade, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                    *///?} else {
                    consumer.putBulkData(pose, quad, fade, fade, fade, packedLight, OverlayTexture.NO_OVERLAY);
                    //?}
                }
            }
            return;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        try {
            RenderSystem.setShader(() -> shader);
            // Prime a single instance slot with the current pose so the instanced shader path
            // can emit a non-instanced draw via the same VAO layout.
            uploadSingleInstance(poseStack, packedLight, blockEntity);

            applyCommonUniforms(shader, RenderSystem.getProjectionMatrix(), new Matrix4f());
            // Must come BEFORE apply() — see prepareBlockLitSamplers javadoc.
            SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
            shader.apply();

            float fade = SingleMeshVboRenderer.getFadeAlpha();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            GL11.glDisable(GL11.GL_CULL_FACE);
            if (fade < 0.99f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            }

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);
            for (int i = INSTANCE_ATTRIB_FIRST; i <= instanceAttribLast; i++) GL20.glEnableVertexAttribArray(i);

            glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0, 1);

            if (fade < 0.99f) {
                RenderSystem.disableBlend();
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer.renderSingle failed", e);
        } finally {
            instanceBuffer.clear();
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        }
    }

    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, null);
    }

    /**
     * Накапливает инстанс в общий батч. Под vanilla - будет один
     * {@code glDrawElementsInstanced} в {@link #flush()}. Под Iris - серия
     * per-machine draw call’ов через {@code ExtendedShader} в том же flush.
     * <p>
     * <b>Shadow pass:</b> {@link #flush(RenderLevelStageEvent)} is wired into
     * {@code RenderLevelStageEvent.AFTER_BLOCK_ENTITIES}, which fires <i>only
     * for the main pass</i>. Iris's shadow renderer runs its own
     * {@code ShadowRenderingState.renderBlockEntities()} earlier in the frame
     * via the Sodium injector - that triggers our {@code addInstance()}
     * calls but never delivers a flush event. If we let the shadow-pass
     * instances accumulate in {@link #instanceBuffer}, the next main-pass
     * flush draws them with the main-pass projection matrix, producing the
     * "ghost machines floating in the sky" symptom (the shadow camera sits
     * far above the world looking down). At the same time the shadow pass
     * itself draws nothing, so machines fail to cast shadows under shader
     * packs.
     * <p>
     * Fix: when the call originates from the shadow pass, bypass batching and
     * draw the instance immediately through {@link #drawSingleWithIrisExtended}.
     * That call piggy-backs on whatever {@link IrisRenderBatch} the parent
     * BER opened (renderers open one for the shadow pass even when batching
     * is otherwise enabled - see e.g. {@code MachineAdvancedAssemblerRenderer.renderWithVBO}),
     * so the cost stays roughly the same as the batched path.
     * <p>
     * <b>Culling:</b> all current call sites perform a single per-BlockEntity
     * {@code OcclusionCullingHelper.shouldRender(...)} check at the top of
     * {@code renderParts()} before invoking {@code addInstance()} on every part
     * for that BE. The previous per-call {@link #shouldRenderWithCulling}
     * inside this method redundantly recomputed the bbox + occlusion test 11
     * times for an Advanced Assembler - once per part - which the profiler
     * traced to ~6.6% of frame time. We trust the renderer's per-BE check
     * and skip the redundant work here.
     */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!initialized) return;

        //? if fabric {
        // On Fabric, Iris draws MUST happen eagerly during block entity
        // dispatch — not deferred to flush(). Fabric API has no
        // AFTER_BLOCK_ENTITIES event; the flush fires at AFTER_TRANSLUCENT,
        // by which point Iris has already moved past the block entity phase,
        // unbound the shader program, and swapped framebuffers. Deferred
        // flushBatchIris() at that point hits "GL No active program" and
        // draws to screen-space instead of world-space.
        //
        // The BER already opens a persistent IrisRenderBatch around all
        // parts of one machine (see renderWithVBO → useIrisBatch), so
        // drawSingleWithIrisExtended piggybacks on that batch and the
        // per-machine apply()/clear() cost is amortized identically to
        // the Forge instanced path — just without the deferred flush.
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            if (drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
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
        //?}

        //? if forge {
        /*if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            if (drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                MainRegistry.LOGGER.warn("InstancedStaticPartRenderer.addInstance: Fallback to the classic putBulkData!!");
                    consumer.putBulkData(pose, quad, fade, fade, fade, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }
        *///?}

        if (instanceCount >= MAX_INSTANCES) {
            if (!overflowLogged) {
                overflowLogged = true;
                MainRegistry.LOGGER.warn("InstancedStaticPartRenderer overflow: MAX_INSTANCES={} reached, skipping extra instances until next flush", MAX_INSTANCES);
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

        // Keep the legacy 6-face averaged sample populated for the Iris flush path
        // (which still uses a single vaUV2 per draw). Cached per-BE so the 11 parts
        // of one machine share a single lookup.
        int sampleBase = instanceCount * 2;
        LightSampleCache.getOrSample(blockEntity, packedLight, instanceLightUV, sampleBase);

        // Spatial light sampling:
        //  - legacy: 8-corner trilinear (2x2x2)
        //  - sliced: 2x4x2 probe lattice (better mid-height side lighting on tall meshes)
        //
        // Reconstructing the per-BE local pose from `mat` is subtle. Mojang
        // bakes the camera VIEW ROTATION into the pose stack before dispatching
        // block entities: see {@code GameRenderer.renderLevel}, which calls
        //   pPoseStack.mulPose(Z_roll); mulPose(X_pitch); mulPose(Y_yaw+180)
        // BEFORE invoking {@code levelRenderer.renderLevel(pPoseStack, ...)}.
        // {@code LevelRenderer} then only adds {@code translate(blockPos - cameraPos)}
        // (cast to float inside PoseStack) inside its block-entities loop, so
        // by the time we see `mat` here:
        //
        //   mat = viewRot * T( (float)(blockPos - cameraPos) ) * perBELocal
        //
        // For on-screen rendering this cancels out (the composed transform is
        // what the shader rebuilds from InstPos + InstRot). For light sampling
        // it absolutely does NOT cancel in two ways:
        //
        //  (a) If we leave viewRot in the composed pose, the 8 sampled corners
        //      rotate with the camera and drift into opaque blocks /
        //      underground / sky - the "models darken from the bottom up as
        //      the camera pitches up" symptom.
        //
        //  (b) If we compose a full absolute world pose in a Matrix4f
        //      (i.e. T(cameraPos) * invViewRot * mat), the translation column
        //      is computed as (float)cameraPos + (float)(blockPos - cameraPos)
        //      in float32. At cameraPos magnitudes > ~10^4 this sum no longer
        //      exactly equals (float)blockPos: the two casts round to grid
        //      points that are not separated by an integer, so the combined
        //      value jitters by fractions of a block as the player moves sub-
        //      block distances. Mth.floor then flips between adjacent blocks
        //      and we sample a different lightmap cell every few frames - the
        //      "models shimmer between light and dark near a torch while the
        //      player strafes" symptom.
        //
        // Fix: keep the light sample math in BLOCK-RELATIVE space.
        //  1. Strip viewRot via RenderSystem.getInverseViewRotationMatrix(),
        //     which Mojang stores right before dispatching the level (see
        //     GameRenderer.renderLevel: RenderSystem.setInverseViewRotationMatrix).
        //     This gives   invViewRot * mat = T(offset) * perBELocal
        //     where offset = (float)(blockPos - cameraPos).
        //  2. Subtract offset from the translation column to leave pure
        //     perBELocal. We recompute offset using the EXACT same cast that
        //     LevelRenderer used ({@code (float)(blockPos.getX() - cam.x)}),
        //     so the two rounding errors cancel bit-for-bit and the
        //     reconstructed perBELocal is stable even at cameraPos > 10^6.
        //  3. Pass blockPos (int) alongside perBELocal (small floats) to
        //     LightSampleCache, which derives world sample positions as
        //     {@code blockPos.getX() + Mth.floor(perBELocal * objCorner.x)}
        //     - no absolute-world float arithmetic anywhere in the flooring
        //     step.
        if (LightSampleCache.BASE_POSE_SET.get()) {
            tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(mat);
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
            tmpLocalPose.set(tmpInvViewRot).mul(mat);
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

        instanceBuffer.put(posTmp.x).put(posTmp.y).put(posTmp.z);
        instanceBuffer.put(rotTmp.x).put(rotTmp.y).put(rotTmp.z).put(rotTmp.w);
        // InstBboxMin (vec3)
        instanceBuffer.put(objBbox[0]).put(objBbox[1]).put(objBbox[2]);
        // InstBboxSize (vec3). Guard against 0-size axes: the shader divides by
        // this and needs a safe non-zero value when a part is flat on an axis.
        float sx = objBbox[3] - objBbox[0];
        float sy = objBbox[4] - objBbox[1];
        float sz = objBbox[5] - objBbox[2];
        instanceBuffer.put(sx).put(sy).put(sz);
        // Light probes payload: either 16 floats (legacy) or 32 floats (sliced)
        instanceBuffer.put(tmpCornerUV);

        instanceCount++;
    }

    /**
     * Brightness in 0.05..1.0 derived from smoothed lightmap UV (0..240 scale),
     * matching the formula in {@link #calculateBrightness(int, float)} but
     * accepting floats so the vanilla no-shader path benefits from the same
     * sub-integer smoothing.
     */
    private float brightnessFromUV(float blockU, float skyV, float cachedSkyDarken) {
        float blockLight = blockU / 16.0f;
        float skyLight   = skyV   / 16.0f;

        float skyDarken;
        if (cachedSkyDarken >= 0f && cachedSkyDarken <= 1f) {
            skyDarken = cachedSkyDarken;
        } else {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return Math.max(0.05f, Math.max(blockLight, skyLight) / 15.0f);
            }
            skyDarken = level.getSkyDarken(1.0f);
        }

        float skyBrightness = 0.05f + (skyDarken * 0.95f);
        float effectiveSkyLight = skyLight * skyBrightness;
        float maxLight = Math.max(blockLight, effectiveSkyLight);
        return 0.05f + (maxLight / 15.0f) * 0.95f;
    }

    /**
     * Helper that primes a single instance slot at index 0 of {@code instanceBuffer}.
     */
    private void uploadSingleInstance(PoseStack poseStack, int packedLight) {
        uploadSingleInstance(poseStack, packedLight, null);
    }

    private void uploadSingleInstance(PoseStack poseStack, int packedLight,
                                      @Nullable BlockEntity blockEntity) {
        instanceBuffer.clear();
        Matrix4f mat = poseStack.last().pose();
        mat.getTranslation(posTmp);
        mat.getNormalizedRotation(rotTmp);

        // See the long comment in addInstance for the full derivation. Short
        // version: strip viewRot from `mat` (it's baked in by
        // GameRenderer.renderLevel) and then strip the (blockPos - cameraPos)
        // offset from the translation column so the resulting pose is purely
        // BE-local. LightSampleCache floors block sample positions using this
        // local pose plus the integer blockPos, keeping samples stable across
        // camera motion and at large world offsets.
        //
        // If blockEntity is null (Iris single-draw prime path without a known
        // BE), LightSampleCache early-exits to the fallback packedLight - we
        // still need a non-null localPose/blockPos to satisfy the null check,
        // but their values don't matter in that branch.
        BlockPos blockPosForSample = (blockEntity != null) ? blockEntity.getBlockPos() : BlockPos.ZERO;
        if (LightSampleCache.BASE_POSE_SET.get()) {
            tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(mat);
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
            tmpLocalPose.set(tmpInvViewRot).mul(mat);
            tmpLocalPose.m30(tmpLocalPose.m30() - (float) (blockPosForSample.getX() - cam.x));
            tmpLocalPose.m31(tmpLocalPose.m31() - (float) (blockPosForSample.getY() - cam.y));
            tmpLocalPose.m32(tmpLocalPose.m32() - (float) (blockPosForSample.getZ() - cam.z));
        }
        long partHash = System.identityHashCode(this);

        LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, blockPosForSample,
                                      tmpLocalPose, packedLight, tmpCornerUV);

        instanceBuffer.put(posTmp.x).put(posTmp.y).put(posTmp.z);
        instanceBuffer.put(rotTmp.x).put(rotTmp.y).put(rotTmp.z).put(rotTmp.w);
        instanceBuffer.put(objBbox[0]).put(objBbox[1]).put(objBbox[2]);
        instanceBuffer.put(objBbox[3] - objBbox[0])
                      .put(objBbox[4] - objBbox[1])
                      .put(objBbox[5] - objBbox[2]);
        instanceBuffer.put(tmpCornerUV);
        instanceBuffer.flip();
    }

    public void flush() {
        flush(RenderSystem.getProjectionMatrix());
    }

    //? if forge {
    /*public void flush(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        flush(event.getProjectionMatrix());
    }
    *///?}
    //? if fabric {
    public void flush(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext event) {
        flush(event.projectionMatrix());
    }
    //?}

    public void flush(Matrix4f projectionMatrix) {
        if (instanceCount == 0) return;

        if (!initialized || vaoId == -1 || eboId == -1) {
            instanceCount = 0;
            instanceBuffer.clear();
            return;
        }

        // Use the Iris batch path only when we can actually obtain a usable
        // ExtendedShader (Iris loaded + reflection resolved + shader pack active
        // + config enabled). When Iris is active but the ExtendedShader path
        // is not available (e.g. reflection failed, config disabled, or Iris
        // version unsupported), fall through to the vanilla instanced shader
        // which works correctly regardless of Iris presence. Without this
        // guard, flush would call flushBatchIris → getBlockShader falls back
        // to the vanilla block_lit_simple shader → Iris's pipeline state
        // (framebuffer binds, program context) clashes with our draw calls →
        // "GL No active program" / "Uniform must be a matrix type" errors
        // and models rendered on screen instead of in the world.
        boolean useIrisFlush = ShaderCompatibilityDetector.useNewIrisVboPath();
        if (useIrisFlush) {
            flushBatchIris(projectionMatrix);
        } else {
            flushBatchVanilla(projectionMatrix);
        }

        instanceCount = 0;
        instanceBuffer.clear();
        overflowLogged = false;
    }

    private void updateUniformCache(ShaderInstance shader) {
        // Iris/Oculus may hand out distinct ShaderInstance wrappers for the same
        // underlying GL program across frames; comparing by object identity would
        // thrash the cache and spam logs. But program ID ALONE is unsafe -
        // GL recycles IDs after glDeleteProgram, so post-rebuild a fresh
        // program can land on the same integer we had cached. We additionally
        // gate on the pipeline generation counter, which bumps whenever Iris
        // rebuilds the pipeline (destroyShaders → fresh link); if EITHER the
        // program ID or the generation disagree with the cached values, we
        // re-resolve. Also check ShaderInstance identity as a belt-and-braces
        // guard for mid-frame instance swaps.
        int programId = (shader != null) ? shader.getId() : -1;
        long currentGen = com.hbm_m.client.render.shader.IrisExtendedShaderAccess.getPipelineGeneration();
        if (this.cachedShaderProgramId == programId
                && this.cachedShader == shader
                && this.cachedPipelineGeneration == currentGen
                && this.cachedShader != null) return;

        this.cachedShader = shader;
        this.cachedShaderProgramId = programId;
        this.cachedPipelineGeneration = currentGen;
        this.uProjMat = shader.getUniform("ProjMat");
        this.uModelView = shader.getUniform("ModelViewMat");
        this.uFogStart = shader.getUniform("FogStart");
        this.uFogEnd = shader.getUniform("FogEnd");
        this.uFogColor = shader.getUniform("FogColor");
        this.uSampler0 = shader.getUniform("Sampler0");
        this.uBrightness = shader.getUniform("Brightness");
        this.uFadeAlpha = shader.getUniform("FadeAlpha");

        // Iris-extended uniform locations are looked up lazily inside flushBatchIris
        // because the program id is only valid after shader.apply(); mark them as
        // unresolved so the first flush after a shader swap re-queries.
        this.cachedLocModelViewInverse = -1;
        this.cachedLocNormalMat = -1;
        this.irisLocationsResolved = false;
    }

    private void applyCommonUniforms(ShaderInstance shader, Matrix4f projectionMatrix, Matrix4f modelView) {
        updateUniformCache(shader);

        if (uProjMat != null) uProjMat.set(projectionMatrix);
        else if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(projectionMatrix);

        if (uModelView != null) uModelView.set(modelView);
        else if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(modelView);

        if (uFogStart != null) uFogStart.set(RenderSystem.getShaderFogStart());
        if (uFogEnd != null) uFogEnd.set(RenderSystem.getShaderFogEnd());
        if (uFogColor != null) {
            float[] fogColor = RenderSystem.getShaderFogColor();
            uFogColor.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
        }
        if (uSampler0 != null) uSampler0.set(0);
        if (uFadeAlpha != null) uFadeAlpha.set(SingleMeshVboRenderer.getFadeAlpha());
    }

    private void flushBatchVanilla(Matrix4f projectionMatrix) {
        ShaderInstance shader = useSlicedLight ? ModShaders.getBlockLitInstancedSlicedShader()
                                               : ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            if (!warnedInstancedShaderNullFlush) {
                warnedInstancedShaderNullFlush = true;
                MainRegistry.LOGGER.warn(
                        "InstancedStaticPartRenderer: instanced shader is null, discarding flush of {} instances (Fabric: ClientSetup.registerFabricShaders)",
                        instanceCount);
            }
            return;
        }

        instanceBuffer.flip();

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMaskWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

        try {
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);

            for (int i = INSTANCE_ATTRIB_FIRST; i <= instanceAttribLast; i++) {
                GL20.glEnableVertexAttribArray(i);
            }

            RenderSystem.setShader(() -> shader);
            // ModelView is identity; instancing shader composes its own per-vertex transform
            // from the per-instance attributes.
            applyCommonUniforms(shader, projectionMatrix, new Matrix4f());

            // Must come BEFORE apply() — see prepareBlockLitSamplers javadoc.
            SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
            shader.apply();

            float fade = SingleMeshVboRenderer.getFadeAlpha();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            if (fade < 0.99f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            }

            glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0, instanceCount);

            if (fade < 0.99f) {
                RenderSystem.disableBlend();
            }

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced flush (vanilla)", e);
        } finally {
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            RenderSystem.depthMask(depthMaskWasEnabled);
            RenderSystem.depthFunc(previousDepthFunc);
            if (depthTestWasEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cullWasEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        }
    }

    /**
     * Iris-compatible flush: per-machine draw through the Iris {@code ExtendedShader}
     * obtained from {@code ShaderMap}. Renders both the main and shadow passes so
     * machines correctly cast shadows under shader packs.
     * <p>
     * <b>Hot loop optimisation:</b> {@link ShaderInstance#apply()} on an
     * {@code ExtendedShader} performs a {@code GlFramebuffer.bind()}, pushes all
     * Iris {@code CustomUniforms}, and uploads every dirty uniform. {@code clear()}
     * adds another {@code RenderTarget.bindWrite()}. Combined those dominate the
     * per-frame cost when there are hundreds of machines on screen (≈63% of frame
     * time per profiler trace). They are <i>batch-level</i> work - none of them
     * depend on which instance we are about to draw - so we hoist them out of the
     * per-instance loop and update only {@code ModelViewMat} between draws via a
     * direct {@link Uniform#upload()} call.
     */
    private void flushBatchIris(Matrix4f projectionMatrix) {
        IrisCompanionMesh companion = getOrBuildIrisCompanion();

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) {
            return;
        }

        instanceBuffer.flip();
        int floats = instanceCount * instanceDataSize;
        if (floats > instanceBuffer.remaining()) {
            // Sanity check: bail rather than read garbage if the buffer state was disturbed.
            return;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMaskWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

        // Pack shaders (BSL in particular) read `blockEntityId / 100` and switch
        // to special-case branches on certain ids - 155 → EMISSIVE_RECOLOR (paints
        // the surface with the warm `blocklightCol`, the infamous "solid red"
        // symptom), 252 → DrawEndPortal, etc. Iris updates this uniform from the
        // last BlockEntity it rendered; our raw GL batch would otherwise inherit
        // whatever value happened to be in there from the most recent Iris draw,
        // which is camera-frustum-dependent and explains the intermittent
        // poisoning. Force a neutral 0 here and restore in finally so Iris's
        // own subsequent BE renders are not disturbed.
        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        try (IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
            RenderSystem.setShader(() -> shader);

            // Batch-level static uniforms: identity ModelView placeholder; will be
            // overridden per-draw via direct upload below.
            applyCommonUniforms(shader, projectionMatrix, IDENTITY);
            // ExtendedShader.apply() reads RenderSystem.getShaderTexture(0..2)
            // (Mojang's tracked shader-texture slots, NOT the active GL bindings)
            // and rebinds those IDs to the IrisSamplers ALBEDO/OVERLAY/LIGHTMAP
            // texture units. If something earlier in the frame (Embeddium chunk
            // re-bake, redstone-dust particle batch, ...) leaves a non-atlas ID
            // in slot 0, the pack shader samples the wrong image - typical
            // symptom is the model going solid orange (= sampling from the
            // lightmap texture). Explicitly point the slots at the right
            // textures before apply(); also restore slot 0 in finally{} so the
            // pollution doesn't bleed forwards into other renderers.
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,
                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            net.minecraft.client.Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
            net.minecraft.client.Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            SingleMeshVboRenderer.TextureBinder.bindForModelIfNeeded(shader);

            // ONE heavy apply: framebuffer bind + Iris CustomUniforms.push() +
            // uploadIfNotNull for every uniform. Importantly this is called
            // AFTER setCurrentRenderedBlockEntity(0) above so the freshly pushed
            // `blockEntityId` uniform reflects the neutral value.
            shader.apply();

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            int targetVao = (companion != null) ? companion.getVaoId() : vaoId;
            int targetIndexCount = (companion != null) ? companion.getIndexCount() : indexCount;
            GL30.glBindVertexArray(targetVao);

            // Bind iris_Entity / mc_midTexCoord / at_tangent at their
            // linker-resolved locations on the companion VAO using pointers
            // into our VBO at the correct byte offsets. Iris's
            // MixinBufferBuilder already wrote valid per-vertex data for these
            // attributes when we built the buffer, so the shader now reads
            // stable real data - completely immune to current-value-bank
            // pollution from Embeddium chunk uploads, redstone particle batches
            // or any other immediate-mode draw, which is the root cause of the
            // intermittent broken-geometry symptom near torches and powered
            // redstone components.
            if (companion != null) {
                companion.prepareForShader(shader.getId());
            }

            // Per-instance: mutate ONLY ModelViewMat (and the lightmap UV2 per-
            // instance pointer into the companion's lightmap VBO) then redraw.
            // No apply()/clear() means no framebuffer rebind and no custom-
            // uniform repush per draw.
            //
            // <b>Per-vertex lightmap path (trilinear gradient).</b> The companion
            // mesh carries baked trilinear weights per vertex (see
            // {@code IrisCompanionMesh.perVertexCornerWeights}). Before entering
            // the draw loop we compute the per-vertex UV2 values for every
            // instance on the CPU by trilinearly combining the 8 corner samples
            // stored in the instance buffer at offsets base+13..28, pack them
            // into the companion's lightmap VBO, and point the VAO's UV2
            // attribute at that VBO. Per-draw we only update the attribute's
            // byte OFFSET to select which instance's UV2 slice to read —
            // one cheap {@code glVertexAttribIPointer} per draw. The pack
            // shader still reads vaUV2 exactly as before, but now it gets a
            // different value at every vertex, giving a smooth in-mesh
            // lighting gradient that actually responds to nearby block lights.
            //
            // When per-vertex is unavailable (companion failed, older GL
            // driver) we fall back to the legacy constant-UV2 path via
            // {@code glVertexAttribI2i} — exactly the previous behaviour.
            final Uniform modelViewUniform = uModelView;
            final int uv2Loc = (companion != null) ? companion.getUv2Location() : -1;
            final boolean perVertexLight = companion != null && companion.supportsPerVertexLightmap();

            // Pre-loop: stage per-vertex UV2 for every instance. Doing this
            // outside the draw loop lets us upload in ONE glBufferSubData
            // (minimizing driver roundtrips) rather than one per instance.
            if (perVertexLight) {
                companion.ensureLightmapCapacity(Math.max(8, instanceCount));
                for (int i = 0; i < instanceCount; i++) {
                    int cornerBase = i * instanceDataSize + LIGHT_FLOAT_OFFSET;
                    long key = 1469598103934665603L; // FNV-1a 64-bit
                    for (int k = 0; k < lightFloatCount; k++) {
                        float f = instanceBuffer.get(cornerBase + k);
                        int q = Math.round(f);
                        if (q < 0) q = 0; else if (q > 240) q = 240;
                        tmpCornerShort[k] = (short) q;
                        key ^= (q & 0xFFFF);
                        key *= 1099511628211L;
                    }
                    long alloc = companion.allocLightmapSlot(key);
                    int slot = (int) (alloc & 0xFFFF_FFFFL);
                    boolean reused = (alloc >>> 32) != 0L;
                    instanceLightmapSlot[i] = slot;
                    if (!reused) {
                        // Convert quantized shorts back to floats for the existing write path.
                        for (int k = 0; k < lightFloatCount; k++) tmpCornerUV[k] = (float) (tmpCornerShort[k] & 0xFFFF);
                        companion.writeInstanceLightmap(slot, tmpCornerUV);
                    }
                }
                companion.finishLightmapWrites();
                companion.activatePerVertexLightmap();
            }

            // ExtendedShader.apply() derives iris_ModelViewMatInverse and
            // iris_NormalMat from the MODEL_VIEW_MATRIX uniform at the moment
            // apply() is called. We call apply() ONCE with IDENTITY and then
            // mutate only ModelViewMat per instance - without re-uploading
            // these derived matrices, every instance ends up using identity
            // for the inverse / normal matrix, which makes pack shaders
            // (BSL/Complementary/RV/Solas) transform vertex normals wrong and
            // render visibly broken geometry. Photon doesn't use NormalMat the
            // same way, so the bug never appeared there. Fix: per-instance,
            // derive both matrices from the actual instance ModelViewMat and
            // upload them via the iris-prefixed uniform names directly.
            //
            // Hot-loop micro-optimisations (each profiler-driven):
            //   1. Skip Mojang's Uniform.upload() proxy stack - that path
            //      goes ShaderInstance.Uniform.upload → uploadAsMatrix →
            //      RenderSystem.glUniformMatrix4 → GlStateManager._glUniformMatrix4
            //      → GL20C.nglUniformMatrix4fv. Profiler attributed 8.67% of
            //      frame time to those layers when 11×N machines × 2 passes
            //      hit the loop. We grab the resolved uniform location once
            //      and call GL20.glUniformMatrix4fv directly (~5% saved).
            //   2. iris_NormalMat depends ONLY on the rotation portion of the
            //      instance matrix (orthonormal R: transpose(inverse(R)) = R).
            //      Most adjacent instances in our batch share orientation
            //      (every Advanced Assembler in a multiblock row faces the
            //      same direction), so we recompute & upload only when the
            //      quaternion actually changes. Saves 5.53% on the dense
            //      same-orientation case observed in the profiler.
            //   3. iris_ModelViewMatInverse changes when EITHER position OR
            //      rotation changes; we still recompute it then because there
            //      is no cheap shortcut, but we still skip the GL call when
            //      both are unchanged (rare but free check).
            //   4. glVertexAttribI2i for the lightmap UV2 is skipped when the
            //      sampled (blockU, skyV) pair matches the previous instance -
            //      adjacent machines often share lighting after smoothing.
            //      Saves 5.93% in dense fields.
            int locModelView = (modelViewUniform != null) ? modelViewUniform.getLocation() : -1;
            // Cache `iris_*` uniform locations across flushes - glGetUniformLocation
            // is a synchronous GL call (driver roundtrip on most stacks) and we
            // were paying it 2 × N partTypes per frame for nothing. The cache is
            // invalidated in updateUniformCache when the shader instance pointer
            // changes (typically only once at shader pack reload / F3+T).
            if (!irisLocationsResolved) {
                int programId = shader.getId();
                cachedLocModelViewInverse = GL20.glGetUniformLocation(programId, "iris_ModelViewMatInverse");
                cachedLocNormalMat = GL20.glGetUniformLocation(programId, "iris_NormalMat");
                irisLocationsResolved = true;
            }
            int locModelViewInverse = cachedLocModelViewInverse;
            int locNormalMat = cachedLocNormalMat;
            // Scratch buffers / joml matrices live as renderer fields so the per-frame
            // hot loop never allocates. See class field block for full rationale.
            final float[] mvFloats = irisMvFloats;
            final float[] mvInverseFloats = irisMvInverseFloats;
            final float[] normalMatFloats = irisNormalMatFloats;
            final Matrix4f mvInverseTmp = irisMvInverseTmp;
            final org.joml.Matrix3f normalTmp = irisNormalTmp;

            // Sentinel values that no real instance would ever match - using
            // NaN as the "first iteration" marker avoids a separate boolean
            // because (NaN != x) is always true.
            float lastQx = Float.NaN, lastQy = Float.NaN, lastQz = Float.NaN, lastQw = Float.NaN;
            float lastPx = Float.NaN, lastPy = Float.NaN, lastPz = Float.NaN;
            int lastBlockU = Integer.MIN_VALUE;
            int lastSkyV = Integer.MIN_VALUE;

            for (int i = 0; i < instanceCount; i++) {
                int base = i * instanceDataSize;
                float px = instanceBuffer.get(base);
                float py = instanceBuffer.get(base + 1);
                float pz = instanceBuffer.get(base + 2);
                float qx = instanceBuffer.get(base + 3);
                float qy = instanceBuffer.get(base + 4);
                float qz = instanceBuffer.get(base + 5);
                float qw = instanceBuffer.get(base + 6);

                boolean rotChanged = qx != lastQx || qy != lastQy || qz != lastQz || qw != lastQw;
                boolean posChanged = px != lastPx || py != lastPy || pz != lastPz;

                tmpInstanceMat.translationRotate(px, py, pz, irisQuatTmp.set(qx, qy, qz, qw));

                if (locModelView >= 0) {
                    tmpInstanceMat.get(mvFloats);
                    GL20.glUniformMatrix4fv(locModelView, false, mvFloats);
                }

                // Optimisation: when we're about to upload BOTH the inverse and
                // the normal matrix, derive the normal matrix from the already-
                // computed inverse instead of re-running .invert() - saves the
                // single most expensive joml call in this loop on every
                // rotation change.
                boolean haveInverseFresh = false;
                if (locModelViewInverse >= 0 && (rotChanged || posChanged)) {
                    mvInverseTmp.set(tmpInstanceMat).invert();
                    mvInverseTmp.get(mvInverseFloats);
                    GL20.glUniformMatrix4fv(locModelViewInverse, false, mvInverseFloats);
                    haveInverseFresh = true;
                }
                if (locNormalMat >= 0 && rotChanged) {
                    if (haveInverseFresh) {
                        normalTmp.set(mvInverseTmp).transpose();
                    } else {
                        normalTmp.set(tmpInstanceMat).invert().transpose();
                    }
                    normalTmp.get(normalMatFloats);
                    GL20.glUniformMatrix3fv(locNormalMat, false, normalMatFloats);
                }

                if (perVertexLight) {
                    // Per-vertex mode: repoint the VAO's UV2 attribute at this
                    // instance's slice of the lightmap VBO. One glVertexAttrib
                    // IPointer call; the pack shader then reads a different
                    // (blockU, skyV) pair per vertex giving the trilinear
                    // gradient baked by writeInstanceLightmap above.
                    companion.bindLightmapForInstance(instanceLightmapSlot[i]);
                } else if (uv2Loc != -1) {
                    // Legacy constant-UV2 fallback (companion doesn't support
                    // per-vertex lightmap, e.g. build failed or no UV2
                    // attribute in the vertex format). Pack shaders declare
                    // vaUV2 as ivec2, so we MUST update the integer "current
                    // value" bank - glVertexAttrib2f writes to a separate
                    // float bank that an ivec2 attribute never reads.
                    int uvBase = i * 2;
                    int blockUInt = Math.max(0, Math.min(240, Math.round(instanceLightUV[uvBase])));
                    int skyVInt   = Math.max(0, Math.min(240, Math.round(instanceLightUV[uvBase + 1])));
                    if (blockUInt != lastBlockU || skyVInt != lastSkyV) {
                        GL30.glVertexAttribI2i(uv2Loc, blockUInt, skyVInt);
                        lastBlockU = blockUInt;
                        lastSkyV = skyVInt;
                    }
                }

                GL11.glDrawElements(GL11.GL_TRIANGLES, targetIndexCount, GL11.GL_UNSIGNED_INT, 0);

                lastQx = qx; lastQy = qy; lastQz = qz; lastQw = qw;
                lastPx = px; lastPy = py; lastPz = pz;
            }

            // ONE clear at the end restores Iris's previous render target binding.
            shader.clear();
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced flush (Iris)", e);
        } finally {
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            RenderSystem.depthMask(depthMaskWasEnabled);
            RenderSystem.depthFunc(previousDepthFunc);
            if (depthTestWasEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cullWasEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            // Restore Mojang's shader-texture slot 0 to the block atlas so any
            // texture-binding pollution we may have inherited does not bleed
            // forwards into other renderers in the same frame.
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,
                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(previousBlockEntityId);
        }
    }

    /**
     * Single-machine draw through the Iris ExtendedShader path. Returns {@code true}
     * if the draw was performed (companion mesh available, shader resolved).
     */
    private boolean drawSingleWithIrisExtended(PoseStack poseStack, int packedLight,
                                               BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        IrisCompanionMesh companion = getOrBuildIrisCompanion();
        if (companion == null) return false;

        // Shadow pass skip: see the matching comment in
        // SingleMeshVboRenderer.renderWithIrisExtended. Sample8 here would
        // otherwise fire under Iris's shadow-camera view-rotation state and
        // poison LightSampleCache's per-frame entries, which the main pass
        // then reuses and renders with a camera-pitch-dependent drift.
        IrisRenderBatch activeBatch = IrisRenderBatch.active();
        boolean shadowPassEarly = (activeBatch != null)
                ? activeBatch.isShadowPass()
                : ShaderCompatibilityDetector.isRenderingShadowPass();

        // Sample the 8 corner lightmap UV2s in world space once; the same set
        // drives both the batched fast path and the standalone fallback
        // below. See flushBatchIris / addInstance for the full worldPose
        // reconstruction rationale (strip viewRot + subtract block-relative
        // offset so the floored block samples stay stable across camera
        // motion and large world offsets). Skipped during shadow — see above.
        boolean haveCorners = false;
        if (!shadowPassEarly) {
            sampleCornersForSingleDraw(poseStack, blockPos, blockEntity, packedLight);
            haveCorners = true;
        }

        // Fast path: a parent BlockEntityRenderer has opened an IrisRenderBatch
        // session. Skip the heavy apply()/clear() and reuse the shared shader,
        // saving most of the per-call cost. Per-vertex lightmap is staged into
        // slot 0 of the companion's lightmap VBO inside the batch helper so
        // the pack shader reads the true trilinear gradient per vertex —
        // matching the batched flushBatchIris path exactly.
        IrisRenderBatch batch = IrisRenderBatch.active();
        if (batch != null) {
            // Keep the legacy 6-face averaged sample as the fallback in case
            // the companion can't do per-vertex (degenerate mesh) - the
            // batch helper transparently falls back to constant UV2 then.
            LightSampleCache.getOrSample(blockEntity, packedLight, irisSingleUV, 0);
            int blockUInt = Math.max(0, Math.min(240, Math.round(irisSingleUV[0])));
            int skyVInt   = Math.max(0, Math.min(240, Math.round(irisSingleUV[1])));
            int packedSmoothLight = (skyVInt << 16) | blockUInt;
            if (haveCorners) {
                batch.drawCompanionWithPerVertexLight(companion, poseStack.last().pose(),
                                                      tmpCornerUV, packedSmoothLight);
            } else {
                batch.drawCompanion(companion, poseStack.last().pose(), packedSmoothLight);
            }
            return true;
        }

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) return false;

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        // Same neutral-blockEntityId reset as flushBatchIris - even though the
        // single path was less prone to BSL's "red" symptom historically, the
        // value can still leak into our draw between Iris's pre-flushes.
        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        try (IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
            RenderSystem.setShader(() -> shader);
            updateUniformCache(shader);

            // Legacy averaged sample only used for the Brightness uniform (the
            // main lighting signal is now per-vertex via tmpCornerUV).
            LightSampleCache.getOrSample(blockEntity, packedLight, irisSingleUV, 0);

            applyCommonUniforms(shader, RenderSystem.getProjectionMatrix(), poseStack.last().pose());
            if (uBrightness != null) uBrightness.set(brightnessFromUV(irisSingleUV[0], irisSingleUV[1], Float.NaN));

            SingleMeshVboRenderer.TextureBinder.bindForModelIfNeeded(shader);
            shader.apply();

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            GL30.glBindVertexArray(companion.getVaoId());

            // See flushBatchIris for the full rationale - bind real VBO data
            // at the linker-resolved locations to make the shader immune to
            // current-value-bank pollution.
            companion.prepareForShader(shader.getId());

            int uv2Loc = companion.getUv2Location();
            if (haveCorners && companion.supportsPerVertexLightmap()) {
                // Per-vertex trilinear path: stage 8-corner samples into slot
                // 0 of the companion's lightmap VBO, upload, and repoint the
                // VAO's UV2 attribute at that slot. Pack shader then reads
                // a different (blockU, skyV) per vertex → true gradient.
                // Gated on haveCorners so the shadow-pass skip above actually
                // reaches the constant-UV2 fallback below instead of reading
                // whatever stale tmpCornerUV is left from the previous frame.
                companion.ensureLightmapCapacity(1);
                companion.writeInstanceLightmap(0, tmpCornerUV);
                companion.finishLightmapWrites();
                companion.activatePerVertexLightmap();
                companion.bindLightmapForInstance(0);
            } else if (uv2Loc != -1) {
                // Constant-UV2 fallback (per-vertex unsupported for this
                // companion). Integer pipeline: pack shaders read vaUV2 as
                // ivec2; see flushBatchIris.
                companion.restoreConstantLightmap();
                int blockUInt = Math.max(0, Math.min(240, Math.round(irisSingleUV[0])));
                int skyVInt   = Math.max(0, Math.min(240, Math.round(irisSingleUV[1])));
                GL30.glVertexAttribI2i(uv2Loc, blockUInt, skyVInt);
            }

            GL11.glDrawElements(GL11.GL_TRIANGLES, companion.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
            shader.clear();
            return true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer.drawSingleWithIrisExtended failed", e);
            return false;
        } finally {
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            if (cullWasEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(previousBlockEntityId);
        }
    }

    /**
     * Populates {@link #tmpCornerUV} with the 8 world-space corner lightmap
     * UV2 samples for a single-instance draw. Mirrors the localPose
     * reconstruction done in {@link #addInstance} / {@link #uploadSingleInstance}
     * so the samples land on stable block indices regardless of camera
     * motion or distance from world origin.
     */
    private void sampleCornersForSingleDraw(PoseStack poseStack, BlockPos blockPos,
                                            @Nullable BlockEntity blockEntity, int packedLight) {
        BlockPos anchor = (blockEntity != null) ? blockEntity.getBlockPos() : blockPos;
        if (anchor == null) anchor = BlockPos.ZERO;
        if (LightSampleCache.BASE_POSE_SET.get()) {
            tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(poseStack.last().pose());
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
            tmpLocalPose.set(tmpInvViewRot).mul(poseStack.last().pose());
            tmpLocalPose.m30(tmpLocalPose.m30() - (float) (anchor.getX() - cam.x));
            tmpLocalPose.m31(tmpLocalPose.m31() - (float) (anchor.getY() - cam.y));
            tmpLocalPose.m32(tmpLocalPose.m32() - (float) (anchor.getZ() - cam.z));
        }
        long partHash = System.identityHashCode(this);
        LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, anchor,
                                      tmpLocalPose, packedLight, tmpCornerUV);
    }

    private float calculateBrightness(int packedLight) {
        return calculateBrightness(packedLight, Float.NaN);
    }

    private float calculateBrightness(int packedLight, float cachedSkyDarken) {
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        
        float skyDarken;
        if (cachedSkyDarken >= 0f && cachedSkyDarken <= 1f) {
            skyDarken = cachedSkyDarken;
        } else {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return Math.max(0.05f, Math.max(blockLight, skyLight) / 15.0f);
            }
            skyDarken = level.getSkyDarken(1.0f);
        }
        
        float skyBrightness = 0.05f + (skyDarken * 0.95f);
        float effectiveSkyLight = skyLight * skyBrightness;
        float maxLight = Math.max(blockLight, effectiveSkyLight);
        return 0.05f + (maxLight / 15.0f) * 0.95f;
    }    

    public boolean isInitialized() {
        return initialized && vaoId > 0 && vboId > 0 && eboId > 0;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    @Override
    public void cleanup() {
        super.cleanup();

        final int instanceVboToDelete = this.instanceVboId;
        final Cleaner.Cleanable bufferCleanable = this.instanceBufferCleanable;
        final IrisCompanionMesh companionToDestroy = this.irisCompanion;

        this.instanceVboId = -1;
        this.instanceBuffer = null;
        this.instanceBufferCleanable = null;
        this.irisCompanion = null;
        this.cachedShader = null;
        this.uProjMat = null;
        this.uModelView = null;
        this.uFogStart = null;
        this.uFogEnd = null;
        this.uFogColor = null;
        this.uSampler0 = null;
        this.uBrightness = null;
        this.uFadeAlpha = null;

        RenderSystem.recordRenderCall(() -> {
            try {
                if (instanceVboToDelete != -1) {
                    GL15.glDeleteBuffers(instanceVboToDelete);
                }
                if (bufferCleanable != null) {
                    bufferCleanable.clean();
                }
                if (companionToDestroy != null) {
                    companionToDestroy.destroy();
                }
            } catch (Exception e) {
                MainRegistry.LOGGER.error("InstancedStaticPartRenderer.cleanup failed", e);
            }
        });
    }
}
