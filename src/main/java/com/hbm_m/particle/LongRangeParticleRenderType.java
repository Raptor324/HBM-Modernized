package com.hbm_m.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * ✅ ИСПРАВЛЕННЫЙ: Кастомный тип рендера для дальних частиц
 */
public class LongRangeParticleRenderType implements ParticleRenderType {

    public static final LongRangeParticleRenderType INSTANCE = new LongRangeParticleRenderType();

    private LongRangeParticleRenderType() {
    }

    @Override
    public String toString() {
        return "long_range_particle";
    }

    @Override
    public void begin(BufferBuilder buffer, TextureManager textureManager) {
        // ✅ Включаем прозрачность (альфа-блендинг)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ✅ ОТКЛЮЧАЕМ запись в depth buffer (depthMask = false)
        // Это позволяет прозрачным частицам правильно отображаться за объектами
        RenderSystem.depthMask(false);

        // ✅ Устанавливаем particle shader
        RenderSystem.setShader(GameRenderer::getParticleShader);

        // ✅ ОБЯЗАТЕЛЬНО привязываем текстуру частиц
        // Без этого будут фиолетовые квадраты
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

        // ✅ Включаем тест глубины (depth test)
        RenderSystem.enableDepthTest();

        // ✅ Функция глубины: 515 = GL_LEQUAL
        // Это стандартная функция для прозрачных объектов
        RenderSystem.depthFunc(515);

        // ✅ ГЛАВНОЕ: ОТКЛЮЧАЕМ FACE CULLING
        // Если не отключить, частицы будут исчезать при определенном угле обзора
        RenderSystem.disableCull();

        // ✅ Запускаем буфер вершин
        // QUADS = 4 вершины на одну частицу
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }

    @Override
    public void end(Tesselator tesselator) {
        // ✅ Отправляем все вершины на рендер
        tesselator.end();

        // ✅ ВОССТАНАВЛИВАЕМ состояние RenderSystem для остальной игры
        RenderSystem.enableCull();           // Включаем face culling обратно
        RenderSystem.depthMask(true);        // Включаем запись в depth buffer
        RenderSystem.disableBlend();         // Отключаем альфа-блендинг
        RenderSystem.depthFunc(515);         // Возвращаем стандартную функцию глубины
    }
}
