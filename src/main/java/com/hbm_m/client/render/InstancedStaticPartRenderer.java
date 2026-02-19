package com.hbm_m.client.render;

import java.nio.FloatBuffer;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

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
 * Рендерит все машины одного типа одним draw call через glDrawElementsInstanced.
 */
@OnlyIn(Dist.CLIENT)
public class InstancedStaticPartRenderer extends AbstractGpuVboRenderer {

    private static final int MAX_INSTANCES = 1024;
    // float per instance: 3 (Pos) + 4 (Rot) + 1 (Light) = 8 floats
    private static final int INSTANCE_DATA_SIZE = 8;

    private int instanceCount = 0;
    private float batchSkyDarken = -1f; // кэш getSkyDarken на батч

    // Временные объекты для извлечения данных из матрицы (вместо float[16])
    private final Vector3f posTmp = new Vector3f();
    private final Quaternionf rotTmp = new Quaternionf();
    
    private int instanceVboId = -1; // VBO для instance attributes
    private FloatBuffer instanceBuffer;

    // --- Cache fields for optimization ---
    private ShaderInstance cachedShader = null;
    private Uniform uProjMat;
    private Uniform uUseInstancing;
    private Uniform uModelView;
    private Uniform uFogStart;
    private Uniform uFogEnd;
    private Uniform uFogColor;
    private Uniform uSampler0;
    private Uniform uSampler2;

    /** Квады для Iris-пути. */
    private final List<BakedQuad> quadsForIris;

    public InstancedStaticPartRenderer(VboData data) {
        this(data, null);
    }
    public InstancedStaticPartRenderer(VboData data, List<BakedQuad> quadsForIris) {
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
            // Размер буфера уменьшен: 8 floats * 4 bytes
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_INSTANCES * INSTANCE_DATA_SIZE * 4, GL15.GL_STREAM_DRAW);

            int stride = INSTANCE_DATA_SIZE * 4; // 32 bytes

            // Attribute 3: InstPos (vec3)
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, stride, 0);
            GL33.glVertexAttribDivisor(3, 1);

            // Attribute 4: InstRot (vec4 / Quaternion)
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, 3 * 4);
            GL33.glVertexAttribDivisor(4, 1);

            // Attribute 5: InstBrightness (float)
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, stride, 7 * 4);
            GL33.glVertexAttribDivisor(5, 1);

            // ОТВЯЗЫВАЕМ VAO
            GL30.glBindVertexArray(0);

            instanceBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES * INSTANCE_DATA_SIZE);

            MemoryUtil.memFree(data.byteBuffer);
            if (data.indices != null) MemoryUtil.memFree(data.indices);

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

    @Override
    protected List<BakedQuad> getQuadsForIrisPath() {
        return quadsForIris;
    }

    /**
     * Немедленный рендер одной инстанции (как было до перехода на addInstance/flush).
     * Используется для гарантированной отрисовки, когда батчинг может не сработать.
     */
    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity) {
        renderSingle(poseStack, packedLight, blockPos, blockEntity, null);
    }

    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
        @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!initialized || vaoId <= 0 || eboId <= 0 || indexCount <= 0) return;

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // Если есть квады и буфер - рисуем сразу в буфер. Это самый быстрый и безопасный путь для Iris.
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            // Если шейдер включен, но условий выше нет - выходим. VBO путь сломает рендер.
            return;
        }

        // --- VANILLA VBO PATH  ---

        ShaderInstance shader = ModShaders.getBlockLitShader();
        if (shader == null) return;

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        try {
            RenderSystem.setShader(() -> shader);
            var modelViewUniform = shader.getUniform("ModelViewMat");
            if (modelViewUniform != null) modelViewUniform.set(poseStack.last().pose());
            var projMatUniform = shader.getUniform("ProjMat");
            if (projMatUniform != null) projMatUniform.set(RenderSystem.getProjectionMatrix());
            else if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

            float brightness = calculateBrightness(packedLight);
            var brightnessUniform = shader.getUniform("Brightness");
            if (brightnessUniform != null) brightnessUniform.set(brightness);

            var useInstancingUniform = shader.getUniform("UseInstancing");
            if (useInstancingUniform != null) useInstancingUniform.set(0);

            var fogStartUniform = shader.getUniform("FogStart");
            if (fogStartUniform != null) fogStartUniform.set(RenderSystem.getShaderFogStart());
            var fogEndUniform = shader.getUniform("FogEnd");
            if (fogEndUniform != null) fogEndUniform.set(RenderSystem.getShaderFogEnd());
            var fogColorUniform = shader.getUniform("FogColor");
            if (fogColorUniform != null) {
                float[] fogColor = RenderSystem.getShaderFogColor();
                fogColorUniform.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            }
            var sampler0 = shader.getUniform("Sampler0");
            if (sampler0 != null) sampler0.set(0);

            shader.apply();
            TextureBinder.bindForModelIfNeeded(shader);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            GL11.glDisable(GL11.GL_CULL_FACE);

            GL30.glBindVertexArray(vaoId);
            GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer.renderSingle failed", e);
        } finally {
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        }
    }

    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, null);
    }

    /**
    Добавляет инстанс.
    Если шейдеры ВКЛЮЧЕНЫ: рендерит СРАЗУ в bufferSource (батчинг мира).
    Если шейдеры ВЫКЛЮЧЕНЫ: добавляет в очередь для VBO инстансинга.
    */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!initialized) return;

        // Optimizations for Iris/Shaders (immediate flush)
        if (ShaderCompatibilityDetector.isExternalShaderActive() && quadsForIris != null && !quadsForIris.isEmpty()) {
            if (ShaderCompatibilityDetector.isRenderingShadowPass()) return;
            if (bufferSource != null) {
                VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
                PoseStack.Pose pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        if (instanceCount >= MAX_INSTANCES) {
            flush(RenderSystem.getProjectionMatrix());
        }
        if (instanceCount == 0) {
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }

        // --- OPTIMIZED DATA UPLOAD ---
        // Извлекаем Pos и Rot из матрицы
        Matrix4f mat = poseStack.last().pose();
        
        mat.getTranslation(posTmp);
        mat.getNormalizedRotation(rotTmp); // Quaternion extraction
        
        // Пишем 8 floats вместо 17
        instanceBuffer.put(posTmp.x).put(posTmp.y).put(posTmp.z);       // 3 floats
        instanceBuffer.put(rotTmp.x).put(rotTmp.y).put(rotTmp.z).put(rotTmp.w); // 4 floats
        instanceBuffer.put(calculateBrightness(packedLight, batchSkyDarken));   // 1 float

        instanceCount++;
    }

    /**
     * Отрендерить все накопленные инстансы одним draw call.
     * Использует RenderSystem.getProjectionMatrix() — для вызова вне события.
     */
    public void flush() {
        flush(RenderSystem.getProjectionMatrix());
    }

    /**
     * Отрендерить все накопленные инстансы одним draw call.
     * Использует матрицы из события — корректно для AFTER_BLOCK_ENTITIES.
     */
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

        flushBatch(projectionMatrix);
        instanceCount = 0;
        instanceBuffer.clear();
    }

    private void updateUniformCache(ShaderInstance shader) {
        if (this.cachedShader == shader) return;
        
        this.cachedShader = shader;
        this.uProjMat = shader.getUniform("ProjMat");
        this.uUseInstancing = shader.getUniform("UseInstancing");
        this.uModelView = shader.getUniform("ModelViewMat");
        this.uFogStart = shader.getUniform("FogStart");
        this.uFogEnd = shader.getUniform("FogEnd");
        this.uFogColor = shader.getUniform("FogColor");
        this.uSampler0 = shader.getUniform("Sampler0");
        this.uSampler2 = shader.getUniform("Sampler2");
        
        MainRegistry.LOGGER.debug("InstancedStaticPartRenderer: Uniform cache updated for shader " + shader.getId());
    }
    
    
    private void flushBatch(Matrix4f projectionMatrix) {
        ShaderInstance shader = ModShaders.getBlockLitShader();
        if (shader == null) return;

        updateUniformCache(shader);
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

            // Включаем атрибуты 3, 4, 5 (было 3..7)
            for (int i = 3; i <= 5; i++) {
                GL20.glEnableVertexAttribArray(i);
            }

            RenderSystem.setShader(() -> shader);
            
            // Set uniforms (Proj, UseInstancing, Fog, etc.)
            if (uProjMat != null) uProjMat.set(projectionMatrix);
            else if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(projectionMatrix);

            if (uUseInstancing != null) uUseInstancing.set(1);
            
            // ModelViewMat должен быть Identity, т.к. трансформации теперь внутри вершинного шейдера
            if (uModelView != null) uModelView.set(new org.joml.Matrix4f());
            
            // Fog uniforms
            if (uFogStart != null) uFogStart.set(RenderSystem.getShaderFogStart());
            if (uFogEnd != null) uFogEnd.set(RenderSystem.getShaderFogEnd());
            if (uFogColor != null) {
                float[] fogColor = RenderSystem.getShaderFogColor();
                uFogColor.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            }
            
            // Samplers
            if (uSampler0 != null) uSampler0.set(0);
            if (uSampler2 != null) uSampler2.set(2);
            
            // Привязываем текстуру
            TextureBinder.bindForModelIfNeeded(shader);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            
            shader.apply();
            // GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            // GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            // GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            // GL11.glPolygonOffset(1.0f, 1.0f);

            RenderSystem.disableCull();
            
            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0, instanceCount);
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced flush", e);
        } finally {

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
            RenderSystem.depthMask(depthMaskWasEnabled);
            RenderSystem.depthFunc(previousDepthFunc);
            if (depthTestWasEnabled) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (cullWasEnabled) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        }
    }

    private float calculateBrightness(int packedLight) {
        return calculateBrightness(packedLight, Float.NaN);
    }

    /**
     * @param cachedSkyDarken кэш level.getSkyDarken(1.0f), если >= 0. Иначе вызывается getSkyDarken.
     */
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

    @Override
    protected VboData buildVboData() {
        return null; // Не используется
    }

    /**
     * Проверяет, инициализирован ли рендерер
     */
    public boolean isInitialized() {
        return initialized && vaoId > 0 && vboId > 0 && eboId > 0;
    }

    /** Текущее число инстансов в батче (до flush). */
    public int getInstanceCount() {
        return instanceCount;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (instanceVboId != -1) {
            GL15.glDeleteBuffers(instanceVboId);
            instanceVboId = -1;
        }
        if (instanceBuffer != null) {
            MemoryUtil.memFree(instanceBuffer);
            instanceBuffer = null;
        }
    }
}
