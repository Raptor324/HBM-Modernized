package com.hbm_m.particle;

import com.hbm_m.client.render.shader.IrisBufferHelper;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Самосветящиеся частицы взрыва — {@link GameRenderer#getPositionTexColorShader()} без lightmap.
 * Как {@link com.hbm.render.entity.effect.RenderTorex} 1.7.10: {@code RenderHelper.disableStandardItemLighting()}.
 */
public class FullBrightParticleRenderType implements ParticleRenderType {

    public static final FullBrightParticleRenderType INSTANCE = new FullBrightParticleRenderType();

    private static final float NO_FOG_START = 1.0E8F;
    private static final float NO_FOG_END = 1.0E9F;

    private float savedFogStart;
    private float savedFogEnd;

    private FullBrightParticleRenderType() {}

    @Override
    public String toString() {
        return "full_bright_particle";
    }

    //? if < 1.21.1 {
    @Override
    public void begin(BufferBuilder buffer, TextureManager textureManager) {
        savedFogStart = RenderSystem.getShaderFogStart();
        savedFogEnd = RenderSystem.getShaderFogEnd();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        disableParticleFog();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.disableCull();

        IrisBufferHelper.beginWithoutExtending(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    }
    //?} else {
    /*// 1.21.1: ParticleRenderType.begin(Tesselator, TextureManager) возвращает BufferBuilder.
    @Override
    public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
        savedFogStart = RenderSystem.getShaderFogStart();
        savedFogEnd = RenderSystem.getShaderFogEnd();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        disableParticleFog();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.disableCull();

        return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    }
    *///?}

    private static void disableParticleFog() {
        RenderSystem.setShaderFogStart(NO_FOG_START);
        RenderSystem.setShaderFogEnd(NO_FOG_END);

        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            return;
        }
        Uniform fogStart = shader.getUniform("FogStart");
        if (fogStart != null) {
            fogStart.set(NO_FOG_START);
        }
        Uniform fogEnd = shader.getUniform("FogEnd");
        if (fogEnd != null) {
            fogEnd.set(NO_FOG_END);
        }
    }

    //? if < 1.21.1 {
    @Override
    public void end(Tesselator tesselator) {
        tesselator.end();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.depthFunc(515);
        RenderSystem.setShaderFogStart(savedFogStart);
        RenderSystem.setShaderFogEnd(savedFogEnd);
    }
    //?}
    // 1.21.1: ParticleRenderType.end(Tesselator) удалён из интерфейса — сброс RenderSystem-стейта
    // выполняет движок после flush. Паритет с LongRangeParticleRenderType (else-ветки для end() нет).
}
