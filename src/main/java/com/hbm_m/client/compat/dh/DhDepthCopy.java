package com.hbm_m.client.compat.dh;

import com.hbm_m.client.render.ModShaders;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

import org.lwjgl.opengl.GL11;

/**
 * КОПИЯ DH-ГЛУБИНЫ В ГЛАВНЫЙ Z-BUFFER.
 *
 * Комозит DH переносит в главный FBO только ЦВЕТ LOD'ов — их DEPTH32F
 * остаётся в DH-текстуре, а главный z-buffer в тех пикселях равен небу
 * (1.0). Поэтому дальние меши (ракеты) нативным depth-тестом всегда
 * проходили против LOD-рельефа («ракета сквозь гору»).
 *
 * Этот фуллскрин-проход читает DEPTH32F текстуру DH шейдером
 * dh_depth_blit и пишет её в gl_FragDepth главного буфера с конвертацией
 * из клип-плоскостей проекции DH в окно нашей расширенной проекции
 * (near=0.05 / far=8e6). Небо DH не пишется (discard) — там остаются
 * честные 1.0. Ванильная геометрия ближе: GL_LESS сохраняет меньшую
 * (более близкую) глубину.
 */
public final class DhDepthCopy {

    private DhDepthCopy() {}

    public static void copyToMain(float dhNear, float dhFar) {
        int texId = DhOcclusionGpu.getDhActiveDepthTextureId();
        if (texId <= 0 || dhNear <= 0.0F || dhFar <= dhNear) {
            return;
        }
        ShaderInstance shader = ModShaders.getDhDepthBlitShader();
        if (shader == null) {
            return;
        }
        // Fabulous: AFTER_WEATHER выполняется внутри WEATHER_TARGET — переход
        // на main разорвал бы transparencyChain-композит. Копию глубины там
        // не делаем (полупрозрачный DH-проход и так поверх композита).
        if (Minecraft.useShaderTransparency()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(false);

        // Цвет не пишем — только глубина. DepthFunc LESS: если в пикселе уже
        // есть более близкая ванильная геометрия, её глубина сохраняется.
        GlStateManager._colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LESS);
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, texId);
        shader.safeGetUniform("DhNear").set(dhNear);
        shader.safeGetUniform("DhFar").set(dhFar);
        // Точные плоскости нашей расширенной проекции (для честного энкода)
        shader.safeGetUniform("OutNear").set(com.hbm_m.client.compat.dh.DhClientCompat.extendedNear());
        shader.safeGetUniform("OutFar").set(com.hbm_m.client.compat.dh.DhClientCompat.extendedFar());
        // Маска dither-fade зоны DH («Fade Nearby DH LODs») — там глубина-шум
        shader.safeGetUniform("DhFadeMaskDist").set(DhOcclusionGpu.ditherFadeMaskDistance());

        // Oculus без пака оставляет GL-программу 0 при «свежем» с точки зрения
        // ванильного кеша шейдере — без этого blit уйдёт в программу 0.
        com.hbm_m.client.render.shader.ShaderBindResync.ensureFreshBind(shader);

        try {
            //? if < 1.21.1 {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bb.vertex(-1.0F, -1.0F, 0.0F).endVertex();
            bb.vertex(1.0F, -1.0F, 0.0F).endVertex();
            bb.vertex(1.0F, 1.0F, 0.0F).endVertex();
            bb.vertex(-1.0F, 1.0F, 0.0F).endVertex();
            Tesselator.getInstance().end();
            //?} else {
            /*BufferBuilder bb = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bb.addVertex(-1.0F, -1.0F, 0.0F);
            bb.addVertex(1.0F, -1.0F, 0.0F);
            bb.addVertex(1.0F, 1.0F, 0.0F);
            bb.addVertex(-1.0F, 1.0F, 0.0F);
            BufferUploader.drawWithShader(bb.build());
            *///?}
        } finally {
            GlStateManager._colorMask(true, true, true, true);
            RenderSystem.depthFunc(org.lwjgl.opengl.GL43.GL_LEQUAL);
            RenderSystem.setShaderTexture(0, 0);
        }
    }
}
