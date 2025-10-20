package com.hbm_m.client.model.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public abstract class AbstractGpuVboRenderer {

    protected int vaoId = -1;
    protected int vboId = -1;
    protected int eboId = -1;
    protected int indexCount = 0;
    protected boolean initialized = false;

    protected abstract VboData buildVboData();

    protected void initVbo() {
        if (initialized) return;

        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();

        VboData data = buildVboData();
        indexCount = data.indices != null ? data.indices.remaining() : 0;

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

        if (data.indices != null && data.indices.remaining() > 0) {
            eboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
        }

        // Layout: 3f pos (0), 3f normal (12), 2f uv0 (24), stride 32
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 32, 0);

        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 32, 12);

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        MemoryUtil.memFree(data.byteBuffer);
        if (data.indices != null) MemoryUtil.memFree(data.indices);

        initialized = true;
    }

    public void render(PoseStack poseStack, int packedLight) {
        if (!initialized) initVbo();
        if (vaoId == -1) return;
    
        ShaderInstance shader = ModShaders.getBlockLitShader();
        if (shader == null) return;
    
        // Шейдер и текстуры
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        // Включаем лайтмап для Sampler2
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
    
        // Глубина
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
    
        // Применить и загрузить юниформы
        shader.apply();
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
        if (shader.PROJECTION_MATRIX != null)  shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
    
        var u = shader.getUniform("PackedLight");
        if (u != null) {
            int bl = net.minecraft.client.renderer.LightTexture.block(packedLight); // 0..15
            int sl = net.minecraft.client.renderer.LightTexture.sky(packedLight);   // 0..15
            u.set((float) bl, (float) sl);
            u.upload();
        }
    
        GL30.glBindVertexArray(vaoId);
        if (eboId != -1) {
            GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        }
        GL30.glBindVertexArray(0);
    }

    public void cleanup() {
        if (!initialized) return;
        if (vboId != -1) { GL15.glDeleteBuffers(vboId); vboId = -1; }
        if (eboId != -1) { GL15.glDeleteBuffers(eboId); eboId = -1; }
        if (vaoId != -1) { GL30.glDeleteVertexArrays(vaoId); vaoId = -1; }
        initialized = false;
    }

    public static class VboData {
        public final ByteBuffer byteBuffer;
        public final IntBuffer indices;
        public VboData(ByteBuffer byteBuffer, IntBuffer indices) {
            this.byteBuffer = byteBuffer;
            this.indices = indices;
        }
    }
}
