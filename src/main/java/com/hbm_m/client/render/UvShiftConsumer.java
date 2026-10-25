package com.hbm_m.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Reicht alle Eckpunkte an einen Puffer weiter und verschiebt dabei die Texturkoordinaten - Ersatz fuer das
 * {@code glMatrixMode(GL_TEXTURE); glTranslated(du, dv, 0)} der Originalrenderer (laufende Baender, Wasser).
 */
public final class UvShiftConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final float du;
    private final float dv;
    private final float su;
    private final float sv;

    public UvShiftConsumer(VertexConsumer delegate, float du, float dv) {
        this(delegate, du, dv, 1F, 1F);
    }

    /** Original {@code glScalef(su, sv, 1); glTranslatef(du, dv, 0)} auf der Texturmatrix: {@code (u + du) * su}. */
    public UvShiftConsumer(VertexConsumer delegate, float du, float dv, float su, float sv) {
        this.delegate = delegate;
        this.du = du;
        this.dv = dv;
        this.su = su;
        this.sv = sv;
    }

    //? if < 1.21.1 {
    @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x, y, z); return this; }
    @Override public VertexConsumer color(int r, int g, int b, int a) { delegate.color(r, g, b, a); return this; }
    @Override public VertexConsumer uv(float u, float v) { delegate.uv((u + du) * su, (v + dv) * sv); return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }
    @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
    @Override public void endVertex() { delegate.endVertex(); }
    @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, a); }
    @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    //?} else {
    /*@Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r, g, b, a); return this; }
    @Override public VertexConsumer setUv(float u, float v) { delegate.setUv((u + du) * su, (v + dv) * sv); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    *///?}
}
