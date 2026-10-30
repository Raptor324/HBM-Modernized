package com.hbm_m.client.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

/**
 * COPY OF THE DH DEPTH INTO THE CURRENTLY BOUND Z-BUFFER - the IRIS variant.
 *
 * <p>Under an active pack, a ShaderInstance pass (dh_depth_blit) cannot be
 * used: a shader unknown to Iris gets masked on apply()
 * (DepthColorStorage.disableDepthColor). This class compiles its own raw GL
 * program outside the MC/Iris pipeline and draws a fullscreen quad directly,
 * so the masking does not affect it.</p>
 *
 * <p>Target framebuffer: the caller first applies the particle ExtendedShader
 * (its apply() binds the correct pipeline FB), then we read
 * GL_DRAW_FRAMEBUFFER_BINDING and write the depth there. Conversion from DH
 * clip planes to the extended projection window matches dh_depth_blit.fsh.</p>
 */
public final class RawDhDepthCopy {

    private static int program = -1;
    private static int uDhNear = -1;
    private static int uDhFar = -1;
    private static int uOutNear = -1;
    private static int uOutFar = -1;
    private static int uFadeMaskDist = -1;
    private static int uReverseZ = -1;
    private static int uSampler = -1;

    // One-way session latch for program construction failures (pattern:
    // IrisInstancedShaders.constructionFailed). Deliberately NEVER reset - this
    // class has no pipeline-generation concept. A build that fails here (e.g. a
    // driver rejecting the #version 150 pair) fails permanently; without the
    // latch ensureProgram would re-run the full compile+link on EVERY frame and
    // leak the freshly created shader/program objects each time. Only the Iris
    // path reaches this class, so the latch only disables the DH depth copy for
    // the session.
    private static boolean constructionFailed = false;

    private RawDhDepthCopy() {}

    /**
     * The Iris raw path compiles the SAME dh_depth_blit.fsh the vanilla path
     * uses via its ShaderInstance - one source of truth for the clip-plane
     * math. Only the uniform upload differs (glUniform* vs safeGetUniform);
     * the uniform names below are the ones declared in the .fsh file.
     */
    private static final String FSH_RESOURCE =
            "/assets/hbm_m/shaders/core/dh_depth_blit.fsh";

    /** Writes into the ALREADY BOUND draw framebuffer - call after the content shader's apply(). */
    public static void copyIntoBoundFramebuffer(float dhNear, float dhFar, int depthTextureId) {
        if (depthTextureId <= 0 || dhNear <= 0.0F || dhFar <= dhNear) {
            return;
        }
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            if (!ensureProgram() || !RawFullscreenQuad.ensureQuad()) {
                return;
            }

            int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            int prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            // Previous unit-0 texture: restore THAT one, not raw zero. Previously,
            // after this pass TU0 was left with an empty texture while the
            // GlStateManager cache was live, so subsequent vanilla _bindTexture
            // calls were no-ops and later draws (particles) sampled "nothing"
            // (black squares).
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            int prevUnit0Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

            GlStateManager._colorMask(false, false, false, false);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            // Write only closer depth: vanilla geometry in the pixel is kept,
            // sky (1.0) is replaced with the LOD.
            RenderSystem.depthFunc(GL11.GL_LESS);
            RenderSystem.disableCull();

            GlStateManager._glUseProgram(program);
            GL20.glUniform1f(uDhNear, dhNear);
            GL20.glUniform1f(uDhFar, dhFar);
            GL20.glUniform1f(uOutNear, com.hbm_m.client.compat.dh.DhClientCompat.extendedNear());
            GL20.glUniform1f(uOutFar, com.hbm_m.client.compat.dh.DhClientCompat.extendedFar());
            GL20.glUniform1f(uFadeMaskDist, com.hbm_m.client.compat.dh.DhOcclusionGpu.ditherFadeMaskDistance());
            GL20.glUniform1f(uReverseZ,
                    com.hbm_m.client.compat.dh.DhClientState.dhReverseZ() ? 1.0F : 0.0F);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(depthTextureId);
            GL20.glUniform1i(uSampler, 0);

            RawFullscreenQuad.draw();

            // Restore: put back the PREVIOUS unit-0 texture (feedback), then state -
            // all through the managed API so the cache stays honest.
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(prevUnit0Tex);
            GlStateManager._colorMask(true, true, true, true);
            RenderSystem.depthFunc(GL43.GL_LEQUAL);
            if (cullWasEnabled) {
                RenderSystem.enableCull();
            }
            GlStateManager._glUseProgram(prevProgram);
            GlStateManager._activeTexture(prevActiveTex);
        } finally {
            GlStateManager._glBindVertexArray(prevVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevVbo);
        }
    }

    private static boolean ensureProgram() {
        if (constructionFailed) {
            return false; // latched - never retry the compile/link per frame
        }
        if (program != -1 && GL20.glIsProgram(program)) {
            return true;
        }
        String fsh = GlShaderUtil.loadResource(RawDhDepthCopy.class, FSH_RESOURCE);
        int newProgram = GlShaderUtil.createRasterProgram(RawFullscreenQuad.FULLSCREEN_VSH, fsh, java.util.Map.of(0, "Position"), "RawDhDepthCopy");
        if (newProgram == 0) {
            constructionFailed = true;
            return false;
        }
        uDhNear = GL20.glGetUniformLocation(newProgram, "DhNear");
        uDhFar = GL20.glGetUniformLocation(newProgram, "DhFar");
        uOutNear = GL20.glGetUniformLocation(newProgram, "OutNear");
        uOutFar = GL20.glGetUniformLocation(newProgram, "OutFar");
        uFadeMaskDist = GL20.glGetUniformLocation(newProgram, "DhFadeMaskDist");
        uReverseZ = GL20.glGetUniformLocation(newProgram, "DhReverseZ");
        uSampler = GL20.glGetUniformLocation(newProgram, "Sampler0");
        program = newProgram;
        return true;
    }
}
