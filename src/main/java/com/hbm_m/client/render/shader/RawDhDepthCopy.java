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
 * КОПИЯ DH-ГЛУБИНЫ В ТЕКУЩЕ ЗАНЯТЫЙ Z-BUFFER — версия для IRIS.
 *
 * Под активным паком нельзя использовать ShaderInstance-проход (dh_depth_blit):
 * неизвестный Iris'у шейдер маскируется на apply()
 * (DepthColorStorage.disableDepthColor). Этот класс компилирует СВОЮ сырую
 * GL-программу мимо MC/Iris-пайплайна и рисует фуллскрин-квад напрямую,
 * поэтому маскирование на него не действует.
 *
 * Целевой framebuffer: вызывающий сначала применяет ExtendedShader частиц
 * (его apply() биндит правильный FB пайплайна), затем мы читаем
 * GL_DRAW_FRAMEBUFFER_BINDING и пишем глубину туда. Конвертация из клип-
 * плоскостей DH в окно расширенной проекции — как в dh_depth_blit.fsh.
 */
public final class RawDhDepthCopy {

    private static int program = -1;
    private static int uDhNear = -1;
    private static int uDhFar = -1;
    private static int uOutNear = -1;
    private static int uOutFar = -1;
    private static int uFadeMaskDist = -1;
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
            in vec2 uv;
            void main() {
                float d = texture(uDepthTex, uv).r;
                if (d >= 0.999999) { discard; }
                float ndc = d * 2.0 - 1.0;
                float denom = (uDhFar + uDhNear) - ndc * (uDhFar - uDhNear);
                float dist = (2.0 * uDhFar * uDhNear) / max(denom, 1e-6);
                // Зона dither-fade DH («Fade Nearby DH LODs»): DEPTH32F там — шум.
                if (dist < uFadeMaskDist) { discard; }
                // Точное окно нашей проекции: window = 1 - fnEff/dist,
                // fnEff = F*N/(F-N) (НЕ 0.1/dist — ошибка ×2 в дистанции окклудера).
                float fnEff = (uOutFar * uOutNear) / (uOutFar - uOutNear);
                gl_FragDepth = clamp((1.0 - fnEff / dist) + 1.0e-6, 0.0, 1.0);
            }
            """;

    /** Пишет в УЖЕ ЗАБИНДЕННЫЙ draw-framebuffer — вызвать после apply() шейдера контента. */
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

        GlStateManager._colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        // Пишем только более близкую глубину: ванильная геометрия в пикселе
        // сохраняется, небо (1.0) заменяется на LOD.
        RenderSystem.depthFunc(GL11.GL_LESS);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL20.glUseProgram(program);
        GL20.glUniform1f(uDhNear, dhNear);
        GL20.glUniform1f(uDhFar, dhFar);
        GL20.glUniform1f(uOutNear, com.hbm_m.client.compat.dh.DhClientCompat.extendedNear());
        GL20.glUniform1f(uOutFar, com.hbm_m.client.compat.dh.DhClientCompat.extendedFar());
        GL20.glUniform1f(uFadeMaskDist, com.hbm_m.client.compat.dh.DhOcclusionGpu.ditherFadeMaskDistance());
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTextureId);
        GL20.glUniform1i(uSampler, 0);

        GlStateManager._glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        // Восстановление: отвязываем DH-текстуру от юнита 0 (feedback), затем стейт.
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlStateManager._colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL43.GL_LEQUAL);
        if (cullWasEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        GlStateManager._glBindVertexArray(prevVao);
        GL20.glUseProgram(prevProgram);
        GL13.glActiveTexture(prevActiveTex);
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
        GL20.glShaderSource(sh, source);
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
