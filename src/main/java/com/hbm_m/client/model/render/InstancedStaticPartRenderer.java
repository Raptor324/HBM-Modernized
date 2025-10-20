package com.hbm_m.client.model.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Instanced Renderer для статических частей (Base/Frame).
 * Рендерит все машины одного типа одним draw call.
 */
public class InstancedStaticPartRenderer extends AbstractGpuVboRenderer {
    
    private static final int MAX_INSTANCES = 1024; // Максимум машин за раз
    
    private final List<Matrix4f> instanceTransforms = new ArrayList<>();
    private final List<Integer> instanceLights = new ArrayList<>();
    private FloatBuffer transformBuffer;
    private FloatBuffer lightBuffer;
    
    public InstancedStaticPartRenderer(VboData data) {
        // Инициализируем VBO с готовыми данными
        if (data == null) return;
        
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        indexCount = data.indices != null ? data.indices.remaining() : 0;

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

        // Position (vec3) - attribute 0
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 32, 0);

        // Normal (vec3) - attribute 1
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 32, 12);

        // UV (vec2) - attribute 2
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24);

        if (data.indices != null && data.indices.remaining() > 0) {
            eboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
        }

        GL30.glBindVertexArray(0);

        MemoryUtil.memFree(data.byteBuffer);
        if (data.indices != null) MemoryUtil.memFree(data.indices);

        initialized = true;
        
        // Аллоцируем буферы для инстансов
        transformBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES * 16); // 16 floats per mat4
        lightBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES * 2); // 2 floats per light (block, sky)
    }
    
    /**
     * Добавить инстанс для батча
     */
    public void addInstance(PoseStack poseStack, int packedLight) {
        if (instanceTransforms.size() >= MAX_INSTANCES) {
            flush(); // Автофлаш при переполнении
        }
        instanceTransforms.add(new Matrix4f(poseStack.last().pose()));
        instanceLights.add(packedLight);
    }
    
    /**
     * Отрендерить все накопленные инстансы одним draw call
     */
    public void flush() {
        if (instanceTransforms.isEmpty() || !initialized || vaoId == -1) {
            instanceTransforms.clear();
            instanceLights.clear();
            return;
        }

        ShaderInstance shader = ModShaders.getBlockLitShader();
        if (shader == null) return;

        RenderSystem.setShader(() -> shader);

        // Projection matrix (одна для всех)
        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }

        // Привязка текстур (один раз для батча)
        TextureBinder.bindForAssemblerIfNeeded(shader);

        // ИСПРАВЛЕНО: Установить NEAREST фильтрацию для чётких пикселей
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        // Fog uniforms
        var fogStartUniform = shader.getUniform("FogStart");
        if (fogStartUniform != null) fogStartUniform.set(RenderSystem.getShaderFogStart());

        var fogEndUniform = shader.getUniform("FogEnd");
        if (fogEndUniform != null) fogEndUniform.set(RenderSystem.getShaderFogEnd());

        var fogColorUniform = shader.getUniform("FogColor");
        if (fogColorUniform != null) {
            float[] fogColor = RenderSystem.getShaderFogColor();
            fogColorUniform.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);

        GL30.glBindVertexArray(vaoId);

        // Рендерим каждый инстанс (пока что не настоящий instancing, но подготовка к нему)
        for (int i = 0; i < instanceTransforms.size(); i++) {
            Matrix4f transform = instanceTransforms.get(i);
            int packedLight = instanceLights.get(i);

            // Model-View matrix для этого инстанса
            if (shader.MODEL_VIEW_MATRIX != null) {
                shader.MODEL_VIEW_MATRIX.set(transform);
            }

            // Освещение для этого инстанса
            int blI = net.minecraft.client.renderer.LightTexture.block(packedLight);
            int slI = net.minecraft.client.renderer.LightTexture.sky(packedLight);
            float bl = (float) blI;
            float sl = (float) slI;

            var lightUniform = shader.getUniform("PackedLight");
            if (lightUniform != null) {
                lightUniform.set(bl, sl);
            }

            shader.apply();

            // Draw
            if (eboId != -1) {
                GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
            }
        }

        GL30.glBindVertexArray(0);

        // Очистка батча
        instanceTransforms.clear();
        instanceLights.clear();
    }
    
    @Override
    protected VboData buildVboData() {
        return null; // Не используется, данные передаются через конструктор
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        if (transformBuffer != null) {
            MemoryUtil.memFree(transformBuffer);
            transformBuffer = null;
        }
        if (lightBuffer != null) {
            MemoryUtil.memFree(lightBuffer);
            lightBuffer = null;
        }
    }
}
