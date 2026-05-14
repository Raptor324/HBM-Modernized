package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.GameRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

/**
 * Снимок и автоматическое восстановление стандартного набора GL state, который
 * наши рендереры регулярно трогают вручную (VAO, ARRAY_BUFFER, cull, depth
 * test/mask/func, blend + blend func, шейдер).
 * <p>
 * Используется через try-with-resources:
 * <pre>{@code
 * try (RenderStateGuard g = RenderStateGuard.snapshot()) {
 *     // GL state changes
 * } // на выходе всё восстанавливается симметрично
 * }</pre>
 * Появился, чтобы убрать асимметрию между {@code flushBatchVanilla} /
 * {@code flushBatchIris} (полное восстановление) и {@code renderSingle} /
 * {@code drawSingleWithIrisExtended} (восстановление лишь части полей)
 * <p>
 * <b>Что НЕ сохраняется намеренно:</b> текущая текстура slot 0 (мы и так
 * пересвязываем block atlas в большинстве путей), shader uniforms (это уже
 * за пределами «GL state» — управляется самими шейдерами).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class RenderStateGuard implements AutoCloseable {

    private final int previousVao;
    private final int previousArrayBuffer;
    private final boolean cullEnabled;
    private final boolean depthTestEnabled;
    private final boolean depthMaskEnabled;
    private final int depthFunc;
    private final boolean blendEnabled;
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;

    private RenderStateGuard() {
        this.previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        this.previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        this.cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        this.depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        this.depthMaskEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        this.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        this.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        this.blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        this.blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        this.blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        this.blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    }

    public static RenderStateGuard snapshot() {
        return new RenderStateGuard();
    }

    @Override
    public void close() {
        GL30.glBindVertexArray(previousVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);

        if (cullEnabled) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }
        if (depthTestEnabled) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(depthMaskEnabled);
        RenderSystem.depthFunc(depthFunc);

        if (blendEnabled) {
            RenderSystem.enableBlend();
        } else {
            RenderSystem.disableBlend();
        }
        RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);

        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
    }
}
