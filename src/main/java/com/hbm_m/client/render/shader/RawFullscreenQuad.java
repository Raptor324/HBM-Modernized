package com.hbm_m.client.render.shader;

import com.hbm_m.client.render.GlVaoSafety;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Shared fullscreen quad VAO/VBO manager with safe GL state preservation.
 * Used by {@link RawDhDepthCopy} and {@link RawScreenSpaceLight}.
 */
public final class RawFullscreenQuad {
    public static final String FULLSCREEN_VSH = """
            #version 150
            in vec3 Position;
            out vec2 uv;
            void main() {
                gl_Position = vec4(Position, 1.0);
                uv = Position.xy * 0.5 + 0.5;
            }
            """;

    private static int vao = -1;
    private static int vbo = -1;

    private RawFullscreenQuad() {}

    /**
     * Ensures the fullscreen quad VAO/VBO is allocated and initialized.
     * Preserves and restores current GL_VERTEX_ARRAY_BINDING and GL_ARRAY_BUFFER_BINDING.
     *
     * @return {@code true} if the VAO is valid and ready for draw.
     */
    public static synchronized boolean ensureQuad() {
        if (vao != -1 && GL30.glIsVertexArray(vao)) {
            return true;
        }
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            GlStateManager._glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            // TRIANGLE_STRIP: BL, BR, TL, TR
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, new float[]{
                    -1f, -1f, 0f,
                     1f, -1f, 0f,
                    -1f,  1f, 0f,
                     1f,  1f, 0f}, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0);
            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn("RawFullscreenQuad: quad init failed: {}", t.toString());
            return false;
        } finally {
            GlVaoSafety.bindVertexArray(prevVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);
        }
    }

    /**
     * Binds the fullscreen quad VAO and renders a 4-vertex triangle strip.
     */
    public static void draw() {
        GlStateManager._glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
    }

    public static synchronized void destroy() {
        if (vbo != -1) {
            GL15.glDeleteBuffers(vbo);
            vbo = -1;
        }
        if (vao != -1) {
            GL30.glDeleteVertexArrays(vao);
            vao = -1;
        }
    }
}
