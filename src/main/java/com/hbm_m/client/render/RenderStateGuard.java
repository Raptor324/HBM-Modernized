package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

/**
 * Snapshot and automatic restoration of the standard set of GL state that our
 * renderers routinely touch manually (VAO, ARRAY_BUFFER, cull, depth
 * test/mask/func, blend + blend func).
 * <p>
 * Used via try-with-resources:
 * <pre>{@code
 * try (RenderStateGuard g = RenderStateGuard.snapshot()) {
 *     // GL state changes
 * } // everything is restored to the CAPTURED state on exit
 * }</pre>
 * {@link #snapshot()} captures the ACTUAL current state (one raw GL query per
 * captured field, once per scope - the guards wrap one draw each, per renderer,
 * not per vertex) into a fresh per-instance record; {@link #close()} restores
 * exactly that state through the managed {@code RenderSystem} entry points
 * (so the GlStateManager cache stays in sync). A static singleton with
 * hardcoded "canonical defaults" on close was tried before and was wrong:
 * it silently re-enabled cull mid-Iris-shadow-pass after
 * {@code drawShadowInstances} disabled it, instead of restoring what the
 * shadow pass had before the guard.
 * <p>
 * <b>Intentionally NOT saved:</b> the current texture slot 0 (we rebind the
 * block atlas in most paths anyway), the shader program and shader uniforms
 * (outside "GL state" - managed by the shaders themselves; callers restore
 * {@code RenderSystem.setShader} in their own finally blocks).
 */

@OnlyIn(Dist.CLIENT)
public final class RenderStateGuard implements AutoCloseable {

    private final int vaoBinding;
    private final int arrayBufferBinding;
    private final boolean cullFace;
    private final boolean depthTest;
    private final int depthFunc;
    private final boolean depthMask;
    private final boolean blend;
    private final int blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha;

    private RenderStateGuard() {
        this.vaoBinding = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        this.arrayBufferBinding = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        this.cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        this.depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        this.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        this.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        this.blend = GL11.glIsEnabled(GL11.GL_BLEND);
        this.blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        this.blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        this.blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        this.blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    }

    /** Captures the current GL state into a fresh guard; restore happens in {@link #close()}. */
    public static RenderStateGuard snapshot() {
        return new RenderStateGuard();
    }

    @Override
    public void close() {
        // Managed (RenderSystem) restore where the cache exists - raw restores would
        // desynchronize GlStateManager and turn later managed calls into no-ops.
        if (this.cullFace) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }
        if (this.depthTest) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthFunc(this.depthFunc);
        RenderSystem.depthMask(this.depthMask);
        if (this.blend) {
            RenderSystem.enableBlend();
        } else {
            RenderSystem.disableBlend();
        }
        RenderSystem.blendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);

        // Restore the exact previous VAO (0 included - GlVaoSafety.bindVertexArray
        // nulls vanilla's immediate-draw bind cache when unbinding) and ARRAY_BUFFER.
        GlVaoSafety.bindVertexArray(this.vaoBinding);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.arrayBufferBinding);
    }
}
