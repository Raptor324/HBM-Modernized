package com.hbm_m.client.render;

import java.nio.FloatBuffer;
import java.util.List;
import java.lang.ref.Cleaner;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Instanced Renderer для статических частей (Base/Frame).
 * Без шейдеров рендерит все машины одного типа одним {@code glDrawElementsInstanced}.
 * Под Iris/Oculus переключается на per-machine draw через {@code ExtendedShader}
 * + companion VBO с {@code IrisVertexFormats.ENTITY} layout, что даёт корректный
 * G-buffer / shadow pass / pack uniforms.
 */
@OnlyIn(Dist.CLIENT)
public class InstancedStaticPartRenderer extends AbstractGpuMesh {

    private static final int MAX_INSTANCES = 1024;
    // float per instance: 3 (Pos) + 4 (Rot) + 1 (Light) = 8 floats
    private static final int INSTANCE_DATA_SIZE = 8;

    private int instanceCount = 0;
    private float batchSkyDarken = -1f; // кэш getSkyDarken на батч
    private boolean overflowLogged = false;

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

    private final Vector3f posTmp = new Vector3f();
    private final Quaternionf rotTmp = new Quaternionf();
    /** Reusable quaternion for the Iris per-instance loop (avoid GC pressure). */
    private final Quaternionf irisQuatTmp = new Quaternionf();
    /** Reusable identity matrix for batch-level static uniform uploads. */
    private static final Matrix4f IDENTITY = new Matrix4f();
    
    private int instanceVboId = -1; // VBO для instance attributes
    private FloatBuffer instanceBuffer;

    private static final Cleaner CLEANER = Cleaner.create();
    private Cleaner.Cleanable instanceBufferCleanable;

    private ShaderInstance cachedShader = null;
    private Uniform uProjMat;
    private Uniform uModelView;
    private Uniform uFogStart;
    private Uniform uFogEnd;
    private Uniform uFogColor;
    private Uniform uSampler0;
    private Uniform uBrightness;

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
        this(data, null);
    }
    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris) {
        this.quadsForIris = quadsForIris;
        if (data == null) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer: Received NULL VboData! Cannot create renderer.");
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
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_INSTANCES * INSTANCE_DATA_SIZE * 4, GL15.GL_STREAM_DRAW);

            int stride = INSTANCE_DATA_SIZE * 4; // 32 bytes

            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, stride, 0);
            GL33.glVertexAttribDivisor(3, 1);

            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, 3 * 4);
            GL33.glVertexAttribDivisor(4, 1);

            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, stride, 7 * 4);
            GL33.glVertexAttribDivisor(5, 1);

            GL30.glBindVertexArray(0);

            instanceBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES * INSTANCE_DATA_SIZE);
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
            if (drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            // Fallback to the classic putBulkData path that defers to Iris's pipeline.
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        // --- VANILLA VBO PATH ---

        ShaderInstance shader = ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
                PoseStack.Pose pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY, false);
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
            uploadSingleInstance(poseStack, packedLight);

            applyCommonUniforms(shader, RenderSystem.getProjectionMatrix(), new Matrix4f());
            SingleMeshVboRenderer.TextureBinder.bindForModelIfNeeded(shader);
            shader.apply();

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            GL11.glDisable(GL11.GL_CULL_FACE);

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);
            for (int i = 3; i <= 5; i++) GL20.glEnableVertexAttribArray(i);

            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0, 1);
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

        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            if (drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            // Companion mesh unavailable - fall back to Iris's own pipeline via
            // a putBulkData call. This still casts a shadow because Iris reads
            // from RenderType.solid()'s buffer at the end of the shadow pass.
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

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

        // Multi-point lightmap sample around the multiblock's bounding box.
        // Routed through the per-frame cache (LightSampleCache) so all 11 part
        // renderers belonging to one machine share a single sample - without
        // the cache the profiler attributed ~17% of frame time to redundant
        // resampling on the dense Advanced Assembler / Chemical Plant case.
        int sampleBase = instanceCount * 2;
        LightSampleCache.getOrSample(blockEntity, packedLight, instanceLightUV, sampleBase);
        float blockU = instanceLightUV[sampleBase];
        float skyV   = instanceLightUV[sampleBase + 1];

        instanceBuffer.put(posTmp.x).put(posTmp.y).put(posTmp.z);
        instanceBuffer.put(rotTmp.x).put(rotTmp.y).put(rotTmp.z).put(rotTmp.w);
        // Vanilla "Brightness" path uses the same smoothed values so the no-shader
        // and Iris paths agree on per-machine brightness.
        instanceBuffer.put(brightnessFromUV(blockU, skyV, batchSkyDarken));

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
        instanceBuffer.clear();
        Matrix4f mat = poseStack.last().pose();
        mat.getTranslation(posTmp);
        mat.getNormalizedRotation(rotTmp);
        instanceBuffer.put(posTmp.x).put(posTmp.y).put(posTmp.z);
        instanceBuffer.put(rotTmp.x).put(rotTmp.y).put(rotTmp.z).put(rotTmp.w);
        instanceBuffer.put(calculateBrightness(packedLight));
        instanceBuffer.flip();
    }

    public void flush() {
        flush(RenderSystem.getProjectionMatrix());
    }

    public void flush(RenderLevelStageEvent event) {
        flush(event.getProjectionMatrix());
    }

    private void flush(Matrix4f projectionMatrix) {
        if (instanceCount == 0) return;

        if (!initialized || vaoId == -1 || eboId == -1) {
            instanceCount = 0;
            instanceBuffer.clear();
            return;
        }

        boolean shaderActive = ShaderCompatibilityDetector.isExternalShaderActive();
        if (shaderActive) {
            flushBatchIris(projectionMatrix);
        } else {
            flushBatchVanilla(projectionMatrix);
        }

        instanceCount = 0;
        instanceBuffer.clear();
        overflowLogged = false;
    }

    private void updateUniformCache(ShaderInstance shader) {
        if (this.cachedShader == shader) return;

        this.cachedShader = shader;
        this.uProjMat = shader.getUniform("ProjMat");
        this.uModelView = shader.getUniform("ModelViewMat");
        this.uFogStart = shader.getUniform("FogStart");
        this.uFogEnd = shader.getUniform("FogEnd");
        this.uFogColor = shader.getUniform("FogColor");
        this.uSampler0 = shader.getUniform("Sampler0");
        this.uBrightness = shader.getUniform("Brightness");

        // Iris-extended uniform locations are looked up lazily inside flushBatchIris
        // because the program id is only valid after shader.apply(); mark them as
        // unresolved so the first flush after a shader swap re-queries.
        this.cachedLocModelViewInverse = -1;
        this.cachedLocNormalMat = -1;
        this.irisLocationsResolved = false;

        MainRegistry.LOGGER.debug("InstancedStaticPartRenderer: Uniform cache updated for shader {}", shader.getName());
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
    }

    private void flushBatchVanilla(Matrix4f projectionMatrix) {
        ShaderInstance shader = ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
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

            for (int i = 3; i <= 5; i++) {
                GL20.glEnableVertexAttribArray(i);
            }

            RenderSystem.setShader(() -> shader);
            // ModelView is identity; instancing shader composes its own per-vertex transform
            // from the per-instance attributes.
            applyCommonUniforms(shader, projectionMatrix, new Matrix4f());

            SingleMeshVboRenderer.TextureBinder.bindForModelIfNeeded(shader);
            shader.apply();

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0, instanceCount);

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
        int floats = instanceCount * INSTANCE_DATA_SIZE;
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

            // Per-instance: mutate ONLY ModelViewMat (and the lightmap UV2 generic
            // attribute constant) then redraw. No apply()/clear() means no
            // framebuffer rebind and no custom-uniform repush per draw.
            //
            // For per-machine lighting we exploit the fact that the companion VAO
            // has its UV2 attribute array DISABLED - when an attribute array is
            // disabled, GL hands every vertex the value supplied by the most recent
            // glVertexAttrib*() call for that location. So one glVertexAttrib2f
            // per draw effectively becomes a "uniform" lightmap UV for the whole
            // mesh, which the bound pack shader reads through `vaUV2` exactly as
            // it would for any normal block. No GLSL patching, no per-vertex
            // re-upload, no companion mesh duplication needed.
            final Uniform modelViewUniform = uModelView;
            final int uv2Loc = (companion != null) ? companion.getUv2Location() : -1;

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
                int base = i * INSTANCE_DATA_SIZE;
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

                if (uv2Loc != -1) {
                    // Pack shaders declare `vaUV2` as ivec2, so we MUST update the
                    // integer "current value" bank - glVertexAttrib2f writes to a
                    // separate float bank that an ivec2 attribute never reads,
                    // which makes every machine fall back to whatever the int bank
                    // last contained (typically 0 → uniform pitch black or whatever
                    // Iris last bound). The lightmap texture's bilinear filter
                    // still gives smooth interpolation between adjacent machines
                    // because neighbouring (blockU, skyV) integer values index
                    // adjacent texels.
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

        // Fast path: a parent BlockEntityRenderer has opened an IrisRenderBatch
        // session. Skip the heavy apply()/clear() and reuse the shared shader,
        // saving most of the per-call cost. Lightmap is sampled the same way as
        // the full path so brightness stays consistent across the batch - and
        // routed through the per-frame cache so 11 part renderers for the same
        // machine share one sample.
        IrisRenderBatch batch = IrisRenderBatch.active();
        if (batch != null) {
            // Reuse the per-renderer scratch UV instead of allocating a new
            // float[2] on every call (was per-part × per-BE × per-pass GC).
            LightSampleCache.getOrSample(blockEntity, packedLight, irisSingleUV, 0);
            int blockUInt = Math.max(0, Math.min(240, Math.round(irisSingleUV[0])));
            int skyVInt   = Math.max(0, Math.min(240, Math.round(irisSingleUV[1])));
            int packedSmoothLight = (skyVInt << 16) | blockUInt;
            batch.drawCompanion(companion, poseStack.last().pose(), packedSmoothLight);
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

            // Smooth the lightmap across the multiblock bounding box, identical
            // to the batched path in flushBatchIris - and routed through the
            // per-frame cache so 11 part renderers for the same machine share
            // a single sample. Scratch reused (see field doc).
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
            if (uv2Loc != -1) {
                // Integer pipeline: pack shaders read vaUV2 as ivec2; see flushBatchIris.
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
