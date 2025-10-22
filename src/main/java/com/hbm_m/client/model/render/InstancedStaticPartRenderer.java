package com.hbm_m.client.model.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Instanced Renderer для статических частей (Base/Frame).
 * Рендерит все машины одного типа одним draw call через glDrawElementsInstanced.
 */
public class InstancedStaticPartRenderer extends AbstractGpuVboRenderer {
    private static final int MAX_INSTANCES = 1024;
    
    private final List<Matrix4f> instanceTransforms = new ArrayList<>();
    private final List<Integer> instanceLights = new ArrayList<>();
    
    private int instanceVboId = -1; // VBO для instance attributes
    private FloatBuffer instanceBuffer;

    public InstancedStaticPartRenderer(VboData data) {
        if (data == null) return;
        
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        indexCount = data.indices != null ? data.indices.remaining() : 0;

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

        // Per-vertex attributes (divisor = 0)
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 32, 0); // Position
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 32, 12); // Normal
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24); // UV

        if (data.indices != null && data.indices.remaining() > 0) {
            eboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
        }

        // Instance VBO для transform matrix (mat4) и lighting (vec2)
        instanceVboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
        // Резервируем пространство (18 floats: 16 для matrix + 2 для light)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_INSTANCES * 18 * 4, GL15.GL_STREAM_DRAW);

        // Matrix columns (4 vec4) - attributes 3-6, divisor = 1
        int stride = 18 * 4; // 18 floats per instance
        for (int i = 0; i < 4; i++) {
            int loc = 3 + i;
            GL20.glEnableVertexAttribArray(loc);
            GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, stride, (long) i * 16);
            GL33.glVertexAttribDivisor(loc, 1); // Instanced
        }

        // Lighting (vec2) - attribute 7, divisor = 1
        GL20.glEnableVertexAttribArray(7);
        GL20.glVertexAttribPointer(7, 2, GL11.GL_FLOAT, false, stride, 16 * 4); // Offset after matrix
        GL33.glVertexAttribDivisor(7, 1); // Instanced

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        MemoryUtil.memFree(data.byteBuffer);
        if (data.indices != null) MemoryUtil.memFree(data.indices);
        
        initialized = true;
        instanceBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES * 18);
    }

    /**
     * Добавить инстанс для батча
     */
    public void addInstance(PoseStack poseStack, int packedLight) {
        if (instanceTransforms.size() >= MAX_INSTANCES) {
            flush();
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
        if (shader == null) {
            instanceTransforms.clear();
            instanceLights.clear();
            return;
        }
    
        // Заполняем instance buffer
        instanceBuffer.clear();
        for (int i = 0; i < instanceTransforms.size(); i++) {
            Matrix4f mat = instanceTransforms.get(i);
            int light = instanceLights.get(i);
            
            mat.get(instanceBuffer);
            instanceBuffer.position(instanceBuffer.position() + 16);
            
            int blockLight = net.minecraft.client.renderer.LightTexture.block(light);
            int skyLight = net.minecraft.client.renderer.LightTexture.sky(light);
            
            // ИСПРАВЛЕНО: правильная нормализация для lightmap
            float u = (blockLight + 0.5f) / 16.0f;
            float v = (skyLight + 0.5f) / 16.0f;
            
            instanceBuffer.put(u);
            instanceBuffer.put(v);
        }
        instanceBuffer.flip();
    
        // Загружаем instance data
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    
        RenderSystem.setShader(() -> shader);
        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }
    
        // UseInstancing = 1
        var useInstancingUniform = shader.getUniform("UseInstancing");
        if (useInstancingUniform != null) {
            useInstancingUniform.set(1);
        }
    
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
    
        shader.apply();
    
        TextureBinder.bindForAssemblerIfNeeded(shader);
        var sampler0 = shader.getUniform("Sampler0");
        if (sampler0 != null) {
            sampler0.set(0); // texture unit 0
        }

        var sampler2 = shader.getUniform("Sampler2");
        if (sampler2 != null) {
            sampler2.set(2); // texture unit 2
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
    
        // КРИТИЧЕСКИ ВАЖНО: отключаем culling для двусторонних граней
        GL11.glDisable(GL11.GL_CULL_FACE);
    
        GL30.glBindVertexArray(vaoId);
        GL31.glDrawElementsInstanced(
            GL11.GL_TRIANGLES, 
            indexCount, 
            GL11.GL_UNSIGNED_INT, 
            0, 
            instanceTransforms.size()
        );
        GL30.glBindVertexArray(0);
    
        // Восстанавливаем culling
        GL11.glEnable(GL11.GL_CULL_FACE);
    
        instanceTransforms.clear();
        instanceLights.clear();
    }
    

    @Override
    protected VboData buildVboData() {
        return null; // Не используется
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
