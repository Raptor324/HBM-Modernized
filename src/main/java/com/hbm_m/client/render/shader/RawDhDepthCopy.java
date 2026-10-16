package com.hbm_m.client.render.shader;

import com.hbm_m.client.render.GlVaoSafety;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
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
    private static int vao = -1;
    private static int vbo = -1;

    private RawDhDepthCopy() {}

    private static final String VSH = """
            #version 150
            in vec3 Position;
            out vec2 uv;
            void main() {
                gl_Position = vec4(Position, 1.0);
                uv = Position.xy * 0.5 + 0.5;
            }
            """;

    private static final String FSH = """
            #version 150
            uniform sampler2D uDepthTex;
            uniform float uDhNear;
            uniform float uDhFar;
            uniform float uOutNear;
            uniform float uOutFar;
            uniform float uFadeMaskDist;
            // DH depth convention: > 0.5 = REVERSE_Z (DH 3.3.1+ without a pack,
            // near=1/sky=0), otherwise FORWARD_Z. Detection in DhClientState.
            uniform float uReverseZ;
            in vec2 uv;
            void main() {
                float d = texture(uDepthTex, uv).r;
                bool reverseZ = uReverseZ > 0.5;
                if (reverseZ ? (d <= 1.0e-6) : (d >= 0.999999)) { discard; }
                float ndc = d * 2.0 - 1.0;
                // REVERSE_Z (DH 3.3.1+): DH's reverse matrix gives
                // ndc = (n/(f-n))*(f/dist - 1)  =>  dist = fn/(ndc*(f-n) + n).
                float dist = reverseZ
                    ? (uDhFar * uDhNear) / max(uDhNear + ndc * (uDhFar - uDhNear), 1e-6)
                    : (2.0 * uDhFar * uDhNear) / max((uDhFar + uDhNear) - ndc * (uDhFar - uDhNear), 1e-6);
                // DH dither-fade zone ("Fade Nearby DH LODs"): DEPTH32F there is noise.
                if (dist < uFadeMaskDist) { discard; }
                // Exact window of our projection: window = 1 - fnEff/dist,
                // fnEff = F*N/(F-N) (NOT 0.1/dist - a 2x error in occluder distance).
                float fnEff = (uOutFar * uOutNear) / (uOutFar - uOutNear);
                gl_FragDepth = clamp((1.0 - fnEff / dist) + 1.0e-6, 0.0, 1.0);
            }
            """;

    /** Writes into the ALREADY BOUND draw framebuffer - call after the content shader's apply(). */
    public static void copyIntoBoundFramebuffer(float dhNear, float dhFar, int depthTextureId) {
        if (depthTextureId <= 0 || dhNear <= 0.0F || dhFar <= dhNear) {
            return;
        }
        if (!ensureProgram() || !ensureQuad()) {
            return;
        }

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
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

        GL20.glUseProgram(program);
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

        GlStateManager._glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        // Restore: put back the PREVIOUS unit-0 texture (feedback), then state -
        // all through the managed API so the cache stays honest.
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(prevUnit0Tex);
        GlStateManager._colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL43.GL_LEQUAL);
        if (cullWasEnabled) {
            RenderSystem.enableCull();
        }
        GlStateManager._glBindVertexArray(prevVao);
        GL20.glUseProgram(prevProgram);
        GlStateManager._activeTexture(prevActiveTex);
    }

    private static boolean ensureProgram() {
        if (program != -1 && GL20.glIsProgram(program)) {
            return true;
        }
        try {
            int vs = compile(GL20.GL_VERTEX_SHADER, VSH);
            int fs = compile(GL20.GL_FRAGMENT_SHADER, FSH);
            if (vs == 0 || fs == 0) return false;
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vs);
            GL20.glAttachShader(program, fs);
            GL20.glBindAttribLocation(program, 0, "Position");
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                com.hbm_m.main.MainRegistry.LOGGER.warn(
                        "RawDhDepthCopy: link failed: {}", GL20.glGetProgramInfoLog(program, 4096));
                program = -1;
                return false;
            }
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            uDhNear = GL20.glGetUniformLocation(program, "uDhNear");
            uDhFar = GL20.glGetUniformLocation(program, "uDhFar");
            uOutNear = GL20.glGetUniformLocation(program, "uOutNear");
            uOutFar = GL20.glGetUniformLocation(program, "uOutFar");
            uFadeMaskDist = GL20.glGetUniformLocation(program, "uFadeMaskDist");
            uReverseZ = GL20.glGetUniformLocation(program, "uReverseZ");
            uSampler = GL20.glGetUniformLocation(program, "uDepthTex");
            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn("RawDhDepthCopy: program init failed: {}", t.toString());
            program = -1;
            return false;
        }
    }

    private static int compile(int type, String source) {
        int sh = GL20.glCreateShader(type);
        com.hbm_m.platform.RenderHooks.safeShaderSource(sh, source);
        GL20.glCompileShader(sh);
        if (GL20.glGetShaderi(sh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "RawDhDepthCopy: compile failed: {}", GL20.glGetShaderInfoLog(sh, 4096));
            return 0;
        }
        return sh;
    }

    private static boolean ensureQuad() {
        if (vao != -1 && GL30.glIsVertexArray(vao)) {
            return true;
        }
        try {
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            GlStateManager._glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            // TRIANGLE_STRIP: BL, BR, TL, TR
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, new float[]{
                    -1f, -1f, 0f,
                    1f, -1f, 0f,
                    -1f, 1f, 0f,
                    1f, 1f, 0f}, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0);
            GlVaoSafety.bindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn("RawDhDepthCopy: quad init failed: {}", t.toString());
            return false;
        }
    }
}
