package com.hbm_m.particle;

import com.hbm_m.client.render.shader.IrisBufferHelper;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Additive particle blending: {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)}, as used by every
 * {@code ParticleRBMK*} in the 1.7.10 original.
 *
 * <p>This matters because the RBMK effect textures ({@code rbmk_fire}, {@code rbmk_mush},
 * {@code rbmk_jet_steam}) are fully opaque sheets drawn on a black backdrop - they carry no
 * alpha channel at all. Under normal alpha blending that backdrop renders as a solid black
 * square around every flame. Additively, black contributes nothing and only the lit pixels
 * show, which is what the original relies on.</p>
 */
public class AdditiveParticleRenderType implements ParticleRenderType {

    public static final AdditiveParticleRenderType INSTANCE = new AdditiveParticleRenderType();

    private static final float NO_FOG_START = 1.0E8F;
    private static final float NO_FOG_END = 1.0E9F;

    private float savedFogStart;
    private float savedFogEnd;

    private AdditiveParticleRenderType() {}

    @Override
    public String toString() {
        return "additive_particle";
    }

    //? if < 1.21.1 {
    @Override
    public void begin(BufferBuilder buffer, TextureManager textureManager) {
        savedFogStart = RenderSystem.getShaderFogStart();
        savedFogEnd = RenderSystem.getShaderFogEnd();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
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
    /*/**
     * 1.21 gab {@code ParticleRenderType} eine neue Form: {@code begin} bekommt den Tesselator und
     * liefert den BufferBuilder zurueck, und ein {@code end}-Hook existiert nicht mehr. Der
     * Zustand wird daher nicht mehr selbst zurueckgesetzt - das uebernimmt der Partikel-Pass.
     *\/
    @Override
    public BufferBuilder begin(com.mojang.blaze3d.vertex.Tesselator tesselator, TextureManager textureManager) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
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
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthFunc(515);
        RenderSystem.setShaderFogStart(savedFogStart);
        RenderSystem.setShaderFogEnd(savedFogEnd);
    }
    //?}
}
