package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
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

@OnlyIn(Dist.CLIENT)
public final class GlVaoSafety {

    private static int dummyVao = 0;

    private GlVaoSafety() {}

    public static int getDummyVao() {
        if (dummyVao == 0) {
            dummyVao = GL30.glGenVertexArrays();
        }
        return dummyVao;
    }

    /**
     * Exact VAO bind, including {@code 0} (unbind).
     * <p>
     * Binding {@code 0} also nulls vanilla's {@code BufferUploader.lastImmediateBuffer}
     * (verified cost: one static-null store) so the next vanilla/TACZ immediate draw
     * honestly rebinds its VAO instead of trusting the bind cache while the physical
     * binding has just become foreign. Cannot break our own draws: our draw paths use
     * unconditional explicit VAO binds, and vanilla's own {@code VertexBuffer.bind()}
     * calls {@code invalidate()} itself.
     */
    public static void bindVertexArray(int vao) {
        if (vao == 0) {
            BufferUploader.invalidate();
        }
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
            // Route through bindVertexArray so the restore-to-0 case (usedDummy)
            // also invalidates the vanilla immediate-draw bind cache.
            bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
    }
}
