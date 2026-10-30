package com.hbm_m.client.render.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.ModelHelper;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Zero-allocation CPU-клиппер дверных партов (Sutherland-Hodgman по BakedQuad)
 * под архитектуру Nucleus.
 * <p>
 * При STRADDLING геометрия отсекается плоскостями на CPU и стримится напрямую
 * в {@link VertexConsumer} через {@link RenderHooks#putBulkData} без создания
 * промежуточных объектов в куче (0 байт аллокаций на горячем пути).
 */
public final class DoorPartClipper {

    /** BLOCK vertex format: 8 int на вершину (pos x3, color, uv x2, light, normal). */
    private static final int VERTEX_SIZE = 8;
    /** Ёмкость скратч-полигона (4 входные вершины; 3 плоскости максимум у дверных партов). */
    private static final int CAPACITY = 16;

    // Переиспользуемые скратч-буферы (рендер однопоточный на RenderThread).
    private static final int[][] polyA = new int[CAPACITY][VERTEX_SIZE];
    private static final int[][] polyB = new int[CAPACITY][VERTEX_SIZE];
    private static final float[] dist = new float[CAPACITY];
    private static final int[] PACKED_SCRATCH = new int[4 * VERTEX_SIZE];

    private static final class MutableScratchQuad extends BakedQuad {
        private net.minecraft.core.Direction dir = net.minecraft.core.Direction.UP;
        private net.minecraft.client.renderer.texture.TextureAtlasSprite spr = null;
        private int tint = -1;
        private boolean shadeFlag = true;

        MutableScratchQuad(int[] vertices) {
            super(vertices, -1, net.minecraft.core.Direction.UP, null, true);
        }

        MutableScratchQuad prepare(BakedQuad src) {
            if (src != null) {
                this.dir = src.getDirection() != null ? src.getDirection() : net.minecraft.core.Direction.UP;
                this.spr = src.getSprite();
                this.tint = src.getTintIndex();
                this.shadeFlag = src.isShade();
            }
            return this;
        }

        @Override public net.minecraft.core.Direction getDirection() { return this.dir; }
        @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getSprite() { return this.spr; }
        @Override public int getTintIndex() { return this.tint; }
        @Override public boolean isShade() { return this.shadeFlag; }
    }

    private static final MutableScratchQuad SCRATCH_QUAD = new MutableScratchQuad(PACKED_SCRATCH);

    private DoorPartClipper() {}

    /**
     * Потоковый рендеринг подрезанных квадов напрямую в consumer.
     */
    static void renderClipped(List<BakedQuad> quads, float[][] planes, @Nullable float[] uvRect,
                              VertexConsumer consumer, PoseStack.Pose pose,
                              float tr, float tg, float tb, float alpha, int packedLight) {
        if (quads.isEmpty() || planes == null || planes.length == 0) return;

        boolean remap = uvRect != null && !ModelHelper.isIdentityRect(uvRect);
        final float u0 = remap ? uvRect[0] : 0f;
        final float v0 = remap ? uvRect[1] : 0f;
        final float du = remap ? uvRect[2] : 1f;
        final float dv = remap ? uvRect[3] : 1f;

        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            int[] v = quad.getVertices();
            if (v.length != 4 * VERTEX_SIZE) {
                // Нестандартный формат — отдаём как есть
                float shade = RenderHooks.quadShade(quad.getDirection());
                RenderHooks.putBulkData(consumer, pose, quad,
                        shade * tr, shade * tg, shade * tb, alpha, packedLight,
                        OverlayTexture.NO_OVERLAY, false);
                continue;
            }

            if (isFullyInside(v, planes)) {
                if (!remap) {
                    float shade = RenderHooks.quadShade(quad.getDirection());
                    RenderHooks.putBulkData(consumer, pose, quad,
                            shade * tr, shade * tg, shade * tb, alpha, packedLight,
                            OverlayTexture.NO_OVERLAY, false);
                } else {
                    System.arraycopy(v, 0, PACKED_SCRATCH, 0, 4 * VERTEX_SIZE);
                    remapUvInPlace(PACKED_SCRATCH, u0, v0, du, dv);
                    float shade = RenderHooks.quadShade(quad.getDirection());
                    RenderHooks.putBulkData(consumer, pose, SCRATCH_QUAD.prepare(quad),
                            shade * tr, shade * tg, shade * tb, alpha, packedLight,
                            OverlayTexture.NO_OVERLAY, false);
                }
                continue;
            }

            // Квад пересекает плоскости или частично/полностью снаружи
            clipAndEmit(v, planes, remap, u0, v0, du, dv, consumer, pose, quad,
                    tr, tg, tb, alpha, packedLight);
        }
    }

    private static boolean isFullyInside(int[] v, float[][] planes) {
        for (float[] plane : planes) {
            final float a = plane[0], b = plane[1], c = plane[2], d = plane[3];
            for (int i = 0; i < 4; i++) {
                int o = i * VERTEX_SIZE;
                if (a * Float.intBitsToFloat(v[o])
                        + b * Float.intBitsToFloat(v[o + 1])
                        + c * Float.intBitsToFloat(v[o + 2]) + d < 0f) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void clipAndEmit(int[] data, float[][] planes, boolean remap,
                                   float u0, float v0, float du, float dv,
                                   VertexConsumer consumer, PoseStack.Pose pose, BakedQuad srcQuad,
                                   float tr, float tg, float tb, float alpha, int packedLight) {
        int[][] src = polyA;
        int[][] dst = polyB;
        int count = 4;
        for (int i = 0; i < 4; i++) {
            System.arraycopy(data, i * VERTEX_SIZE, src[i], 0, VERTEX_SIZE);
        }

        for (float[] plane : planes) {
            count = clipPlane(src, count, plane, dst);
            if (count == 0) {
                return; // Полностью отсечён
            }
            int[][] tmp = src;
            src = dst;
            dst = tmp;
        }

        emitFan(src, count, remap, u0, v0, du, dv, consumer, pose, srcQuad,
                tr, tg, tb, alpha, packedLight);
    }

    private static int clipPlane(int[][] src, int count, float[] plane, int[][] dst) {
        final float a = plane[0], b = plane[1], c = plane[2], d = plane[3];
        int out = 0;
        for (int i = 0; i < count; i++) {
            int[] v = src[i];
            dist[i] = a * Float.intBitsToFloat(v[0])
                    + b * Float.intBitsToFloat(v[1])
                    + c * Float.intBitsToFloat(v[2]) + d;
        }
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            boolean inI = dist[i] >= 0f;
            boolean inJ = dist[j] >= 0f;
            if (inI) {
                System.arraycopy(src[i], 0, dst[out], 0, VERTEX_SIZE);
                out++;
            }
            if (inI != inJ) {
                float t = dist[i] / (dist[i] - dist[j]);
                lerpVertex(src[i], src[j], t, dst[out]);
                out++;
            }
        }
        return out;
    }

    private static void lerpVertex(int[] a, int[] b, float t, int[] out) {
        out[0] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[0]), Float.intBitsToFloat(b[0]), t));
        out[1] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[1]), Float.intBitsToFloat(b[1]), t));
        out[2] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[2]), Float.intBitsToFloat(b[2]), t));
        out[3] = lerpColor(a[3], b[3], t);
        out[4] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[4]), Float.intBitsToFloat(b[4]), t));
        out[5] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[5]), Float.intBitsToFloat(b[5]), t));
        out[6] = a[6]; // light
        out[7] = a[7]; // normal
    }

    private static float lerp(float p, float q, float t) {
        return p + (q - p) * t;
    }

    private static int lerpColor(int p, int q, float t) {
        int r = Math.round(lerp(p & 0xFF, q & 0xFF, t));
        int g = Math.round(lerp((p >> 8) & 0xFF, q & 0xFF, t));
        int b = Math.round(lerp((p >> 16) & 0xFF, q & 0xFF, t));
        int a = Math.round(lerp((p >> 24) & 0xFF, q & 0xFF, t));
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static void emitFan(int[][] poly, int n, boolean remap, float u0, float v0, float du, float dv,
                                VertexConsumer consumer, PoseStack.Pose pose, BakedQuad srcQuad,
                                float tr, float tg, float tb, float alpha, int packedLight) {
        if (n < 3) return;

        SCRATCH_QUAD.prepare(srcQuad);
        float shade = RenderHooks.quadShade(srcQuad.getDirection());

        // Первый квад (или треугольник, дополненный 4-й вершиной)
        if (n == 3) {
            System.arraycopy(poly[0], 0, PACKED_SCRATCH, 0, VERTEX_SIZE);
            System.arraycopy(poly[1], 0, PACKED_SCRATCH, VERTEX_SIZE, VERTEX_SIZE);
            System.arraycopy(poly[2], 0, PACKED_SCRATCH, 2 * VERTEX_SIZE, VERTEX_SIZE);
            System.arraycopy(poly[2], 0, PACKED_SCRATCH, 3 * VERTEX_SIZE, VERTEX_SIZE);
            emitPacked(PACKED_SCRATCH, remap, u0, v0, du, dv, consumer, pose,
                    shade, tr, tg, tb, alpha, packedLight);
            return;
        }

        // n == 4
        for (int i = 0; i < 4; i++) {
            System.arraycopy(poly[i], 0, PACKED_SCRATCH, i * VERTEX_SIZE, VERTEX_SIZE);
        }
        emitPacked(PACKED_SCRATCH, remap, u0, v0, du, dv, consumer, pose,
                shade, tr, tg, tb, alpha, packedLight);

        // n >= 5: веер треугольников poly[0], poly[k], poly[k+1], poly[k+1]
        for (int k = 3; k + 1 < n; k++) {
            System.arraycopy(poly[0], 0, PACKED_SCRATCH, 0, VERTEX_SIZE);
            System.arraycopy(poly[k], 0, PACKED_SCRATCH, VERTEX_SIZE, VERTEX_SIZE);
            System.arraycopy(poly[k + 1], 0, PACKED_SCRATCH, 2 * VERTEX_SIZE, VERTEX_SIZE);
            System.arraycopy(poly[k + 1], 0, PACKED_SCRATCH, 3 * VERTEX_SIZE, VERTEX_SIZE);
            emitPacked(PACKED_SCRATCH, remap, u0, v0, du, dv, consumer, pose,
                    shade, tr, tg, tb, alpha, packedLight);
        }
    }

    private static void emitPacked(int[] packed, boolean remap, float u0, float v0, float du, float dv,
                                   VertexConsumer consumer, PoseStack.Pose pose,
                                   float shade, float tr, float tg, float tb, float alpha, int packedLight) {
        if (remap) {
            remapUvInPlace(packed, u0, v0, du, dv);
        }
        RenderHooks.putBulkData(consumer, pose, SCRATCH_QUAD,
                shade * tr, shade * tg, shade * tb, alpha, packedLight,
                OverlayTexture.NO_OVERLAY, false);
    }

    /**
     * Статическое запекание (bake-time) для рам дверей: однократная обрезка списка BakedQuad
     * перед компиляцией VBO (вызывается из resolveDoorPartQuads).
     */
    public static List<BakedQuad> clipQuads(List<BakedQuad> quads, float[][] planes, @Nullable float[] uvRect) {
        if (quads.isEmpty() || planes == null || planes.length == 0) return quads;
        boolean remap = uvRect != null && !ModelHelper.isIdentityRect(uvRect);
        final float u0 = remap ? uvRect[0] : 0f;
        final float v0 = remap ? uvRect[1] : 0f;
        final float du = remap ? uvRect[2] : 1f;
        final float dv = remap ? uvRect[3] : 1f;

        List<BakedQuad> out = new java.util.ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] v = quad.getVertices();
            if (v.length != 4 * VERTEX_SIZE) {
                out.add(quad);
                continue;
            }
            if (isFullyInside(v, planes)) {
                if (!remap) {
                    out.add(quad);
                } else {
                    int[] packed = new int[4 * VERTEX_SIZE];
                    System.arraycopy(v, 0, packed, 0, 4 * VERTEX_SIZE);
                    remapUvInPlace(packed, u0, v0, du, dv);
                    out.add(new BakedQuad(packed, quad.getTintIndex(), quad.getDirection(),
                            quad.getSprite(), quad.isShade()));
                }
                continue;
            }

            int[][] src = polyA;
            int[][] dst = polyB;
            int count = 4;
            for (int i = 0; i < 4; i++) {
                System.arraycopy(v, i * VERTEX_SIZE, src[i], 0, VERTEX_SIZE);
            }
            for (float[] plane : planes) {
                count = clipPlane(src, count, plane, dst);
                if (count == 0) break;
                int[][] tmp = src;
                src = dst;
                dst = tmp;
            }
            if (count < 3) continue;

            if (count == 3) {
                int[] packed = new int[4 * VERTEX_SIZE];
                System.arraycopy(src[0], 0, packed, 0, VERTEX_SIZE);
                System.arraycopy(src[1], 0, packed, VERTEX_SIZE, VERTEX_SIZE);
                System.arraycopy(src[2], 0, packed, 2 * VERTEX_SIZE, VERTEX_SIZE);
                System.arraycopy(src[2], 0, packed, 3 * VERTEX_SIZE, VERTEX_SIZE);
                if (remap) remapUvInPlace(packed, u0, v0, du, dv);
                out.add(new BakedQuad(packed, quad.getTintIndex(), quad.getDirection(),
                        quad.getSprite(), quad.isShade()));
            } else {
                int[] packed = new int[4 * VERTEX_SIZE];
                for (int i = 0; i < 4; i++) {
                    System.arraycopy(src[i], 0, packed, i * VERTEX_SIZE, VERTEX_SIZE);
                }
                if (remap) remapUvInPlace(packed, u0, v0, du, dv);
                out.add(new BakedQuad(packed, quad.getTintIndex(), quad.getDirection(),
                        quad.getSprite(), quad.isShade()));
                for (int k = 3; k + 1 < count; k++) {
                    int[] pTri = new int[4 * VERTEX_SIZE];
                    System.arraycopy(src[0], 0, pTri, 0, VERTEX_SIZE);
                    System.arraycopy(src[k], 0, pTri, VERTEX_SIZE, VERTEX_SIZE);
                    System.arraycopy(src[k + 1], 0, pTri, 2 * VERTEX_SIZE, VERTEX_SIZE);
                    System.arraycopy(src[k + 1], 0, pTri, 3 * VERTEX_SIZE, VERTEX_SIZE);
                    if (remap) remapUvInPlace(pTri, u0, v0, du, dv);
                    out.add(new BakedQuad(pTri, quad.getTintIndex(), quad.getDirection(),
                            quad.getSprite(), quad.isShade()));
                }
            }
        }
        return out;
    }

    private static void remapUvInPlace(int[] packed, float u0, float v0, float du, float dv) {
        for (int v = 0; v < 4; v++) {
            int i = v * VERTEX_SIZE;
            packed[i + 4] = Float.floatToRawIntBits(u0 + Float.intBitsToFloat(packed[i + 4]) * du);
            packed[i + 5] = Float.floatToRawIntBits(v0 + Float.intBitsToFloat(packed[i + 5]) * dv);
        }
    }
}
