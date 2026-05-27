package com.hbm_m.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Helpers for core-profile VAO rules.
 * <p>
 * Binding a shared empty "dummy" VAO instead of VAO {@code 0} breaks vanilla and
 * Iris draws ({@code drawElements} can crash the GL driver) because the dummy has
 * no vertex buffers or element array. Use {@link #bindVertexArray(int)} for normal
 * restore/unbind; reserve {@link #withAttribEditVao(Runnable)} only when code must
 * call {@code glVertexAttribPointer} / {@code glEnableVertexAttribArray} and the
 * current binding is {@code 0}.
 */
public final class GlVaoSafety {

    private static int dummyVao = 0;

    private GlVaoSafety() {}

    public static int getDummyVao() {
        if (dummyVao == 0) {
            dummyVao = GL30.glGenVertexArrays();
        }
        return dummyVao;
    }

    /** Exact VAO bind, including {@code 0} (unbind). */
    public static void bindVertexArray(int vao) {
        GlStateManager._glBindVertexArray(vao);
    }

    public static int currentBinding() {
        return GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    }

    /**
     * Runs {@code work} with a non-zero VAO bound so attrib pointer setup is legal.
     * Restores the previous VAO and ARRAY_BUFFER binding afterward.
     */
    public static void withAttribEditVao(Runnable work) {
        int previousVao = currentBinding();
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean usedDummy = previousVao == 0;
        try {
            if (usedDummy) {
                GlStateManager._glBindVertexArray(getDummyVao());
            }
            work.run();
        } finally {
            GlStateManager._glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
    }
}
