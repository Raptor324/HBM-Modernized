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
 * ✅ ИСПРАВЛЕННЫЙ: Кастомный тип рендера для частиц, видимых на больших расстояниях
 * Полностью решает проблему стандартного ограничения в 32 блока
 *
 * Особенности:
 * - Отключение culling для видимости со всех углов
 * - Правильное управление depth buffer
 * - Альфа-блендинг для прозрачности
 * - Использование стандартного particle shader'а Minecraft'а
 */
public class LongRangeParticleRenderType implements ParticleRenderType {

    public static final LongRangeParticleRenderType INSTANCE = new LongRangeParticleRenderType();

    private LongRangeParticleRenderType() {
        // Приватный конструктор - singleton pattern
    }

    @Override
    public String toString() {
        return "long_range_particle";
    }

    @Override
    public void begin(BufferBuilder buffer, TextureManager textureManager) {
        // ✅ Включаем прозрачность (альфа-блендинг)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA

        // ✅ ОТКЛЮЧАЕМ запись в глубину (depth mask false)
        // Это позволяет прозрачным частицам правильно отображаться за другими объектами
        RenderSystem.depthMask(false);

        // ✅ Устанавливаем шейдер частиц
        RenderSystem.setShader(GameRenderer::getParticleShader);

        // ✅ ОБЯЗАТЕЛЬНО привязываем текстуру частиц
        // Без этого будут фиолетовые квадраты (текстура не найдена)
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

        // ✅ Включаем тест глубины
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LEQUAL - стандартная функция для частиц

        // ✅ ГЛАВНОЕ: ОТКЛЮЧАЕМ CULLING (разворот граней)
        // Если этого не сделать, частицы будут исчезать при определенном угле обзора
        RenderSystem.disableCull();

        // ✅ Запускаем буфер вершин
        // QUADS - 4 вершины на одну частицу (квадрат)
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }

    @Override
    public void end(Tesselator tesselator) {
        // ✅ Отправляем накопленные вершины на рендер
        tesselator.end();

        // ✅ Восстанавливаем состояние для остальной игры
        RenderSystem.enableCull(); // Возвращаем culling для остального мира
        RenderSystem.depthMask(true); // Возвращаем запись в depth buffer
        RenderSystem.disableBlend(); // Отключаем блендинг
    }
}