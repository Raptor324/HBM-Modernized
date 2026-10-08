package com.hbm_m.client.weapon.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.util.Vec3NT;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

/**
 * Hilfen fuer die portierten Waffenrenderer (Gruppe B), die {@link GunGL} nicht abdeckt:
 * {@code GL_CLIP_PLANE0}, {@code FontRenderer.drawString} im Modellraum, {@code ColorUtil.fr/fg/fb},
 * {@code java.awt.Color} und {@code Vec3NT.rotateAroundZDeg}.
 */
public final class RenderHelperB {

    private RenderHelperB() { }

    // ─── Farben ────────────────────────────────────────────────────────────

    /** Original {@code ColorUtil.fr(color)}. */
    public static float fr(int color) { return ((color & 0xff0000) >> 16) / 255F; }
    /** Original {@code ColorUtil.fg(color)}. */
    public static float fg(int color) { return ((color & 0x00ff00) >> 8) / 255F; }
    /** Original {@code ColorUtil.fb(color)}. */
    public static float fb(int color) { return (color & 0x0000ff) / 255F; }

    /** Original {@code new Color(r, g, b).getRGB()} (Gleitkomma-Konstruktor, deckend). */
    public static int rgb(float r, float g, float b) {
        return 0xFF000000 | ((int) (r * 255 + 0.5) & 255) << 16 | ((int) (g * 255 + 0.5) & 255) << 8 | ((int) (b * 255 + 0.5) & 255);
    }

    /** Original {@code new Color(rgb).getRed()/getGreen()/getBlue()}. */
    public static int red(int rgb) { return (rgb >> 16) & 255; }
    public static int green(int rgb) { return (rgb >> 8) & 255; }
    public static int blue(int rgb) { return rgb & 255; }

    // ─── NI4NI ─────────────────────────────────────────────────────────────

    /** Original {@code ItemGunNI4NI.getColors(stack)}. */
    public static int[] ni4niGetColors(ItemStack stack) {
        return com.hbm_m.item.weapon.sedna.impl.ItemGunNI4NI.getColors(stack);
    }

    /** Original {@code ItemGunNI4NI.getCoinCount(stack)}. */
    public static int ni4niGetCoinCount(ItemStack stack) {
        return com.hbm_m.item.weapon.sedna.impl.ItemGunNI4NI.getCoinCount(stack);
    }

    // ─── Vektoren ──────────────────────────────────────────────────────────

    /** Original {@code Vec3NT.rotateAroundZDeg(alpha)} (doppelte Genauigkeit wie im Original). */
    public static Vec3NT rotateAroundZDeg(Vec3NT vec, double alpha) {
        double rad = alpha / 180D * Math.PI;
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = vec.xCoord * cos + vec.yCoord * sin;
        double y = vec.yCoord * cos - vec.xCoord * sin;
        vec.xCoord = x;
        vec.yCoord = y;
        return vec;
    }

    // ─── Schrift ───────────────────────────────────────────────────────────

    /** Original {@code fontRenderer.getStringWidth(s)}. */
    public static int getStringWidth(String s) {
        return Minecraft.getInstance().font.width(s);
    }

    /** Original {@code fontRenderer.drawString(s, x, y, color)} im aktuellen GunGL-Modellraum. */
    public static void drawString(String s, float x, float y, int color) {
        Font font = Minecraft.getInstance().font;
        font.drawInBatch(s, x, y, 0xFF000000 | color, false, GunGL.pose().last().pose(), GunGL.buffers(),
                Font.DisplayMode.NORMAL, 0, GunGL.light());
    }

    // ─── GL_CLIP_PLANE0 ────────────────────────────────────────────────────

    private static boolean clipEnabled = false;
    private static final double[] clipPlane = new double[4];
    private static final Matrix4f clipInverse = new Matrix4f();

    /** Original {@code glEnable(GL_CLIP_PLANE0)}. */
    public static void enableClipPlane0() { clipEnabled = true; }
    /** Original {@code glDisable(GL_CLIP_PLANE0)}. */
    public static void disableClipPlane0() { clipEnabled = false; }

    /**
     * Original {@code glClipPlane(GL_CLIP_PLANE0, eq)}: die Ebene gilt im Raum der aktuellen Modellmatrix
     * (wie bei OpenGL wird sie mit der Matrix zum Zeitpunkt des Aufrufs festgehalten).
     */
    public static void clipPlane0(double a, double b, double c, double d) {
        clipPlane[0] = a; clipPlane[1] = b; clipPlane[2] = c; clipPlane[3] = d;
        clipInverse.set(GunGL.pose().last().pose()).invert();
    }

    /** {@code model.renderPart(part)} unter Beachtung von {@code GL_CLIP_PLANE0}. */
    public static void renderPart(SimpleObjModel model, String part) {
        if (!clipEnabled) {
            GunGL.renderPart(model, part);
            return;
        }
        ClipConsumer clip = new ClipConsumer(GunGL.buffer());
        model.renderPartEntity(part, GunGL.pose(), clip, GunGL.light(), OverlayTexture.NO_OVERLAY,
                GunGL.red(), GunGL.green(), GunGL.blue(), GunGL.alpha());
        clip.flush();
    }

    /** Beschneidet die (entarteten) Vierecke des Modells an der gespeicherten Ebene. */
    private static final class ClipConsumer implements VertexConsumer {

        private final VertexConsumer delegate;
        private final List<float[]> quad = new ArrayList<>(4);
        // x y z r g b a u v overlayU overlayV lightU lightV nx ny nz
        private float[] cur = null;

        ClipConsumer(VertexConsumer delegate) { this.delegate = delegate; }

        private float[] start(double x, double y, double z) {
            commit();
            cur = new float[16];
            cur[0] = (float) x; cur[1] = (float) y; cur[2] = (float) z;
            cur[3] = cur[4] = cur[5] = cur[6] = 1F;
            return cur;
        }

        private void commit() {
            if (cur == null) return;
            quad.add(cur);
            cur = null;
            if (quad.size() == 4) {
                clipAndEmit();
                quad.clear();
            }
        }

        void flush() { commit(); }

        private double dist(float[] v) {
            Vector4f p = new Vector4f(v[0], v[1], v[2], 1F).mul(clipInverse);
            return clipPlane[0] * p.x + clipPlane[1] * p.y + clipPlane[2] * p.z + clipPlane[3] * p.w;
        }

        private void clipAndEmit() {
            List<float[]> poly = new ArrayList<>(6);
            int n = quad.size();
            for (int i = 0; i < n; i++) {
                float[] a = quad.get(i), b = quad.get((i + 1) % n);
                double da = dist(a), db = dist(b);
                boolean aIn = da >= 0, bIn = db >= 0;
                if (aIn) poly.add(a);
                if (aIn != bIn) {
                    float t = (float) (da / (da - db));
                    float[] m = new float[16];
                    for (int k = 0; k < 16; k++) m[k] = a[k] + (b[k] - a[k]) * t;
                    for (int k = 9; k <= 12; k++) m[k] = a[k];
                    poly.add(m);
                }
            }
            for (int k = 1; k + 1 < poly.size(); k++) {
                emit(poly.get(0));
                emit(poly.get(k));
                emit(poly.get(k + 1));
                emit(poly.get(k + 1));
            }
        }

        private void emit(float[] v) {
            //? if < 1.21.1 {
            delegate.vertex(v[0], v[1], v[2]).color((int) (v[3] * 255), (int) (v[4] * 255), (int) (v[5] * 255), (int) (v[6] * 255))
                    .uv(v[7], v[8]).overlayCoords((int) v[9], (int) v[10]).uv2((int) v[11], (int) v[12]).normal(v[13], v[14], v[15]).endVertex();
            //?} else {
            /*delegate.addVertex(v[0], v[1], v[2]).setColor((int) (v[3] * 255), (int) (v[4] * 255), (int) (v[5] * 255), (int) (v[6] * 255))
                    .setUv(v[7], v[8]).setUv1((int) v[9], (int) v[10]).setUv2((int) v[11], (int) v[12]).setNormal(v[13], v[14], v[15]);
            *///?}
        }

        //? if < 1.21.1 {
        @Override public VertexConsumer vertex(double x, double y, double z) { start(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { if (cur != null) { cur[3] = r / 255F; cur[4] = g / 255F; cur[5] = b / 255F; cur[6] = a / 255F; } return this; }
        @Override public VertexConsumer uv(float u, float v) { if (cur != null) { cur[7] = u; cur[8] = v; } return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { if (cur != null) { cur[9] = u; cur[10] = v; } return this; }
        @Override public VertexConsumer uv2(int u, int v) { if (cur != null) { cur[11] = u; cur[12] = v; } return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { if (cur != null) { cur[13] = x; cur[14] = y; cur[15] = z; } return this; }
        @Override public void endVertex() { commit(); }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
        //?} else {
        /*@Override public VertexConsumer addVertex(float x, float y, float z) { start(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { if (cur != null) { cur[3] = r / 255F; cur[4] = g / 255F; cur[5] = b / 255F; cur[6] = a / 255F; } return this; }
        @Override public VertexConsumer setUv(float u, float v) { if (cur != null) { cur[7] = u; cur[8] = v; } return this; }
        @Override public VertexConsumer setUv1(int u, int v) { if (cur != null) { cur[9] = u; cur[10] = v; } return this; }
        @Override public VertexConsumer setUv2(int u, int v) { if (cur != null) { cur[11] = u; cur[12] = v; } return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { if (cur != null) { cur[13] = x; cur[14] = y; cur[15] = z; } return this; }
        *///?}
    }
}
