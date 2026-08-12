package com.hbm_m.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Безопасная запись {@code POSITION_TEX_COLOR} для Embeddium {@code SodiumBufferBuilder}:
 * int-цвет, {@code uv} перед {@code color}, либо полный {@code vertex(...)} через reflection.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class ImmediateVertexWriter {

    private static final String SODIUM_BUILDER =
            "me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder";

    private static java.lang.reflect.Method sodiumFullVertex;

    private ImmediateVertexWriter() {}

    /** Camera-facing quad (billboard) в локальных координатах эффекта. */
    public static void billboardQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            float cx, float cy, float cz,
            Vector3f left, Vector3f up,
            float r, float g, float b, float a,
            float u0, float v0, float u1, float v1) {
        int ri = toColorByte(r);
        int gi = toColorByte(g);
        int bi = toColorByte(b);
        int ai = toColorByte(a);

        putCorner(consumer, matrix, cx - left.x - up.x, cy - left.y - up.y, cz - left.z - up.z, ri, gi, bi, ai, u1, v1);
        putCorner(consumer, matrix, cx - left.x + up.x, cy - left.y + up.y, cz - left.z + up.z, ri, gi, bi, ai, u1, v0);
        putCorner(consumer, matrix, cx + left.x + up.x, cy + left.y + up.y, cz + left.z + up.z, ri, gi, bi, ai, u0, v0);
        putCorner(consumer, matrix, cx + left.x - up.x, cy + left.y - up.y, cz + left.z - up.z, ri, gi, bi, ai, u0, v1);
    }

    public static void texColor(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x, float y, float z,
            float r, float g, float b, float a,
            float u, float v) {
        putCorner(consumer, matrix, x, y, z, toColorByte(r), toColorByte(g), toColorByte(b), toColorByte(a), u, v);
    }

    private static void putCorner(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x, float y, float z,
            int r, int g, int b, int a,
            float u, float v) {
        if (trySodiumFullVertex(consumer, matrix, x, y, z, r, g, b, a, u, v)) {
            return;
        }
        if (matrix != null) {
            //? if < 1.21.1 {
            consumer.vertex(matrix, x, y, z).uv(u, v).color(r, g, b, a).endVertex();
            //?} else {
            /*consumer.addVertex(matrix, x, y, z).setUv(u, v).setColor(r, g, b, a);
            *///?}
        } else {
            //? if < 1.21.1 {
            consumer.vertex(x, y, z).uv(u, v).color(r, g, b, a).endVertex();
            //?} else {
            /*consumer.addVertex(x, y, z).setUv(u, v).setColor(r, g, b, a);
            *///?}
        }
    }

    /** Quad в мировых/камера-relative координатах (без Matrix4f). */
    public static void worldQuad(
            VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r, float g, float b, float a,
            float u0, float v0, float u1, float v1) {
        int ri = toColorByte(r);
        int gi = toColorByte(g);
        int bi = toColorByte(b);
        int ai = toColorByte(a);
        putCorner(consumer, null, x0, y0, z0, ri, gi, bi, ai, u1, v1);
        putCorner(consumer, null, x1, y1, z1, ri, gi, bi, ai, u1, v0);
        putCorner(consumer, null, x2, y2, z2, ri, gi, bi, ai, u0, v0);
        putCorner(consumer, null, x3, y3, z3, ri, gi, bi, ai, u0, v1);
    }

    private static boolean trySodiumFullVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x, float y, float z,
            int r, int g, int b, int a,
            float u, float v) {
        if (!SODIUM_BUILDER.equals(consumer.getClass().getName())) {
            return false;
        }
        try {
            if (sodiumFullVertex == null) {
                sodiumFullVertex = consumer.getClass().getMethod(
                        "vertex",
                        float.class, float.class, float.class,
                        float.class, float.class, float.class, float.class,
                        float.class, float.class,
                        int.class, int.class,
                        float.class, float.class, float.class);
            }
            Vector4f pos = matrix != null
                    ? matrix.transform(new Vector4f(x, y, z, 1.0F))
                    : new Vector4f(x, y, z, 1.0F);
            sodiumFullVertex.invoke(
                    consumer,
                    pos.x(), pos.y(), pos.z(),
                    r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F,
                    u, v,
                    0, 0,
                    0.0F, 0.0F, 1.0F);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static int toColorByte(float component) {
        return Mth.clamp((int) (component * 255.0F), 0, 255);
    }
}
