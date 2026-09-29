package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.GameRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

/**
 * Snapshot and automatic restoration of the standard set of GL state that our
 * renderers routinely touch manually (VAO, ARRAY_BUFFER, cull, depth
 * test/mask/func, blend + blend func, shader).
 * <p>
 * Used via try-with-resources:
 * <pre>{@code
 * try (RenderStateGuard g = RenderStateGuard.snapshot()) {
 *     // GL state changes
 * } // everything is restored symmetrically on exit
 * }</pre>
 * Introduced to remove the asymmetry between {@code flushBatchVanilla} /
 * {@code flushBatchIris} (full restoration) and {@code renderSingle} /
 * {@code drawSingleWithIrisExtended} (only some fields restored).
 * <p>
 * <b>Intentionally NOT saved:</b> the current texture slot 0 (we rebind the
 * block atlas in most paths anyway) and shader uniforms (outside "GL state" -
 * managed by the shaders themselves).
 */

@OnlyIn(Dist.CLIENT)
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

        GlVaoSafety.bindVertexArray(previousVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
    }
}