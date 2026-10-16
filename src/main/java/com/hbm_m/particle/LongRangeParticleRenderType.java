package com.hbm_m.particle;

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
 *  ИСПРАВЛЕННЫЙ: Кастомный тип рендера для дальних частиц
 */
public class LongRangeParticleRenderType implements ParticleRenderType {

    public static final LongRangeParticleRenderType INSTANCE = new LongRangeParticleRenderType();

    /** Push fog past any virtualized contrail depth (see MissileTrackWorldRender / SingleMeshVboRenderer). */
    private static final float NO_FOG_START = 1.0E8F;
    private static final float NO_FOG_END = 1.0E9F;

    private float savedFogStart;
    private float savedFogEnd;

    private LongRangeParticleRenderType() {
    }

    @Override
    public String toString() {
        return "long_range_particle";
    }

    //? if < 1.21.1 {
    @Override
    public void begin(BufferBuilder buffer, TextureManager textureManager) {
        savedFogStart = RenderSystem.getShaderFogStart();
        savedFogEnd = RenderSystem.getShaderFogEnd();

        //  Включаем прозрачность (альфа-блендинг)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        //  ОТКЛЮЧАЕМ запись в depth buffer (depthMask = false)
        // Это позволяет прозрачным частицам правильно отображаться за объектами
        RenderSystem.depthMask(false);

        //  Устанавливаем particle shader
        RenderSystem.setShader(GameRenderer::getParticleShader);

        //  ОБЯЗАТЕЛЬНО привязываем текстуру частиц
        // Без этого будут фиолетовые квадраты
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

        // Long-range contrails are drawn at virtualized camera-relative depth (~render distance).
        // Vanilla particle fog still blends toward sky color at that depth — white wash on trails.
        // Missile mesh renderers disable/extend fog the same way (MissileTrackWorldRender).
        disableParticleFog();

        //  Включаем тест глубины (depth test)
        RenderSystem.enableDepthTest();

        //  Функция глубины: 515 = GL_LEQUAL
        // Это стандартная функция для прозрачных объектов
        RenderSystem.depthFunc(515);

        //  ГЛАВНОЕ: ОТКЛЮЧАЕМ FACE CULLING
        // Если не отключить, частицы будут исчезать при определенном угле обзора
        RenderSystem.disableCull();

        //  Запускаем буфер вершин
        // QUADS = 4 вершины на одну частицу
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }

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
    //?} else {
    /*@Override
    public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
        savedFogStart = RenderSystem.getShaderFogStart();
        savedFogEnd = RenderSystem.getShaderFogEnd();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        disableParticleFog();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.disableCull();

        return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
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
}
