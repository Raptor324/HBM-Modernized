package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Safe {@code POSITION_TEX_COLOR} writing for the Embeddium {@code SodiumBufferBuilder}:
 * int color, {@code uv} before {@code color}, or the full {@code vertex(...)} via a MethodHandle.
 *
 * <p>Attribution / Credits:
 * Adapted from Embeddium (https://github.com/embeddedt/embeddium, LGPL-3.0) and
 * Sodium (https://github.com/CaffeineMC/sodium-fabric, LGPL-3.0).
 * Authors: jellysquid3, embeddedt, CaffeineMC.</p>
 *
 * <p>Hot path (mushroom-cloud billboards): the Sodium-buffer check is hoisted out of the
 * hot loop once per batch ({@link #isSodiumBuffer} plus boolean overloads) to avoid running
 * getClass().getName() for every quad corner; matrix transformation goes through a
 * reused scratch vector - zero allocations per vertex.
 */

@OnlyIn(Dist.CLIENT)
public final class ImmediateVertexWriter {

    private static final String[] SODIUM_BUILDER_CLASSES = {
            "me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder",
            "net.caffeinemc.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder"
    };

    private static final String[] EXTENDED_BUILDER_CLASSES = {
            "me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder",
            "net.caffeinemc.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder"
    };

    private static volatile boolean sodiumResolved = false;
    private static Class<?> sodiumBufferBuilderClass;
    private static Class<?> extendedBufferBuilderClass;
    private static java.lang.invoke.MethodHandle sodiumFullVertex;
    private static java.lang.invoke.MethodHandle getDelegateHandle;

    /** Scratch vector for matrix transformation - rendering is single-threaded, so reuse it. */
    private static final Vector4f SCRATCH_POS = new Vector4f();

    private ImmediateVertexWriter() {}

    /**
     * Fast Embeddium buffer check, hoisted out of the hot loop.
     * Returns true only when a fast-path 14-parameter MethodHandle is available.
     */
    public static boolean isSodiumBuffer(VertexConsumer consumer) {
        if (consumer == null) return false;
        resolveSodium();
        if (sodiumFullVertex == null) return false;
        Class<?> clazz = consumer.getClass();
        if (clazz == sodiumBufferBuilderClass) return true;
        return extendedBufferBuilderClass != null && extendedBufferBuilderClass.isInstance(consumer);
    }

    private static void resolveSodium() {
        if (sodiumResolved) return;
        synchronized (ImmediateVertexWriter.class) {
            if (sodiumResolved) return;
            for (String name : SODIUM_BUILDER_CLASSES) {
                try {
                    Class<?> cls = Class.forName(name);
                    sodiumFullVertex = java.lang.invoke.MethodHandles.lookup().findVirtual(cls, "vertex",
                            java.lang.invoke.MethodType.methodType(void.class,
                                    float.class, float.class, float.class,
                                    float.class, float.class, float.class, float.class,
                                    float.class, float.class,
                                    int.class, int.class,
                                    float.class, float.class, float.class));
                    sodiumBufferBuilderClass = cls;
                    break;
                } catch (Throwable ignored) {
                }
            }

            for (String name : EXTENDED_BUILDER_CLASSES) {
                try {
                    Class<?> cls = Class.forName(name);
                    extendedBufferBuilderClass = cls;
                    if (sodiumBufferBuilderClass != null) {
                        getDelegateHandle = java.lang.invoke.MethodHandles.lookup().findVirtual(cls, "sodium$getDelegate",
                                java.lang.invoke.MethodType.methodType(sodiumBufferBuilderClass));
                    }
                    break;
                } catch (Throwable ignored) {
                }
            }
            sodiumResolved = true;
        }
    }

    /** Camera-facing quad (billboard) in effect-local coordinates. */
    public static void billboardQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            float cx, float cy, float cz,
            Vector3f left, Vector3f up,
            float r, float g, float b, float a,
            float u0, float v0, float u1, float v1) {
        billboardQuad(consumer, isSodiumBuffer(consumer), matrix, cx, cy, cz, left, up, r, g, b, a, u0, v0, u1, v1);
    }

    /** Hot variant: the sodium flag is resolved by the caller once per batch. */
    public static void billboardQuad(
            VertexConsumer consumer,
            boolean sodium,
            Matrix4f matrix,
            float cx, float cy, float cz,
            Vector3f left, Vector3f up,
            float r, float g, float b, float a,
            float u0, float v0, float u1, float v1) {
        int ri = toColorByte(r);
        int gi = toColorByte(g);
        int bi = toColorByte(b);
        int ai = toColorByte(a);

        putCorner(consumer, sodium, matrix, cx - left.x - up.x, cy - left.y - up.y, cz - left.z - up.z, ri, gi, bi, ai, u1, v1);
        putCorner(consumer, sodium, matrix, cx - left.x + up.x, cy - left.y + up.y, cz - left.z + up.z, ri, gi, bi, ai, u1, v0);
        putCorner(consumer, sodium, matrix, cx + left.x + up.x, cy + left.y + up.y, cz + left.z + up.z, ri, gi, bi, ai, u0, v0);
        putCorner(consumer, sodium, matrix, cx + left.x - up.x, cy + left.y - up.y, cz + left.z - up.z, ri, gi, bi, ai, u0, v1);
    }

    public static void texColor(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x, float y, float z,
            float r, float g, float b, float a,
            float u, float v) {
        putCorner(consumer, isSodiumBuffer(consumer), matrix, x, y, z, toColorByte(r), toColorByte(g), toColorByte(b), toColorByte(a), u, v);
    }

    private static void putCorner(
            VertexConsumer consumer,
            boolean sodium,
            Matrix4f matrix,
            float x, float y, float z,
            int r, int g, int b, int a,
            float u, float v) {
        if (sodium && trySodiumFullVertex(consumer, matrix, x, y, z, r, g, b, a, u, v)) {
            return;
        }
        if (matrix != null) {
            com.hbm_m.platform.RenderHooks.vertexTexColor(consumer, matrix, x, y, z, u, v, r, g, b, a);
        } else {
            com.hbm_m.platform.RenderHooks.vertexTexColor(consumer, x, y, z, u, v, r, g, b, a);
        }
    }

    /** Quad in world/camera-relative coordinates (no Matrix4f). */
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
        boolean sodium = isSodiumBuffer(consumer);
        putCorner(consumer, sodium, null, x0, y0, z0, ri, gi, bi, ai, u1, v1);
        putCorner(consumer, sodium, null, x1, y1, z1, ri, gi, bi, ai, u1, v0);
        putCorner(consumer, sodium, null, x2, y2, z2, ri, gi, bi, ai, u0, v0);
        putCorner(consumer, sodium, null, x3, y3, z3, ri, gi, bi, ai, u0, v1);
    }

    private static boolean trySodiumFullVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x, float y, float z,
            int r, int g, int b, int a,
            float u, float v) {
        resolveSodium();
        java.lang.invoke.MethodHandle mh = sodiumFullVertex;
        if (mh == null || consumer == null) {
            return false;
        }
        try {
            Object target = consumer;
            if (extendedBufferBuilderClass != null && extendedBufferBuilderClass.isInstance(consumer)) {
                if (getDelegateHandle != null) {
                    target = getDelegateHandle.invoke(consumer);
                    if (target == null) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            SCRATCH_POS.set(x, y, z, 1.0F);
            if (matrix != null) {
                matrix.transform(SCRATCH_POS);
            }
            mh.invoke(target,
                    SCRATCH_POS.x(), SCRATCH_POS.y(), SCRATCH_POS.z(),
                    r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F,
                    u, v,
                    0, 0,
                    0.0F, 0.0F, 1.0F);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int toColorByte(float component) {
        return Mth.clamp((int) (component * 255.0F), 0, 255);
    }
}
