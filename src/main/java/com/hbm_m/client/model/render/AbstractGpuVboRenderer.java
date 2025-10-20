package com.hbm_m.client.model.render;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
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

    static final class TextureBinder {
        private static boolean boundForThisAssembler = false;

        static void resetForAssembler() {
            boundForThisAssembler = false;
        }

        static void bindForAssemblerIfNeeded(ShaderInstance shader) {
            if (boundForThisAssembler) return;

            var textureManager = Minecraft.getInstance().getTextureManager();
            var blockAtlas = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
            int blockAtlasId = blockAtlas.getId();

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, blockAtlasId);

            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();

            boundForThisAssembler = true;
        }
    }

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

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 32, 0);

        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 32, 12);

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24);

        if (data.indices != null && data.indices.remaining() > 0) {
            eboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
        }

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
        if (shader == null) {
            MainRegistry.LOGGER.error("BlockLit shader is null");
            return;
        }

        RenderSystem.setShader(() -> shader);

        // Матрицы
        if (shader.MODEL_VIEW_MATRIX != null)
            shader.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
        if (shader.PROJECTION_MATRIX != null)
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

        // ИСПРАВЛЕНО: Правильное извлечение света
        int blI = net.minecraft.client.renderer.LightTexture.block(packedLight);
        int slI = net.minecraft.client.renderer.LightTexture.sky(packedLight);

        // ВАЖНО: НЕ масштабируем, используем напрямую (уже в диапазоне 0-240)
        float bl = (float) blI;
        float sl = (float) slI;

        // PackedLight uniform
        var lightUniform = shader.getUniform("PackedLight");
        if (lightUniform != null) {
            lightUniform.set(bl, sl);
        }

        // ДОБАВЛЕНО: Fog uniforms (автоматически берутся из RenderSystem)
        var fogStartUniform = shader.getUniform("FogStart");
        if (fogStartUniform != null) {
            fogStartUniform.set(RenderSystem.getShaderFogStart());
        }

        var fogEndUniform = shader.getUniform("FogEnd");
        if (fogEndUniform != null) {
            fogEndUniform.set(RenderSystem.getShaderFogEnd());
        }

        var fogColorUniform = shader.getUniform("FogColor");
        if (fogColorUniform != null) {
            float[] fogColor = RenderSystem.getShaderFogColor();
            fogColorUniform.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
        }

        shader.apply();

        TextureBinder.bindForAssemblerIfNeeded(shader);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);

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
