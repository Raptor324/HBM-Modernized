package com.hbm_m.inventory.gui;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;

/**
 * 1:1-Port von {@code GUIElements.drawSmoothLinearGauge} (1.7.10): ein fuenfeckiger Schieber, der sich
 * entlang einer Geraden (Drehwinkel {@code rotation}) um {@code progress * scale} Pixel verschiebt;
 * aussen eine Kontur (Faktor 1,5, die hinteren Ecken nur in X), innen der Zeiger.
 */
public final class GuiLinearGauge {

    private GuiLinearGauge() {}

    public static void draw(GuiGraphics guiGraphics, int x, int y, double progress, double tipLength, double backLength,
                            double backSide, double scale, float rotation, int color) {
        draw(guiGraphics, x, y, progress, tipLength, backLength, backSide, scale, rotation, color, 0x000000);
    }

    public static void draw(GuiGraphics guiGraphics, int x, int y, double progress, double tipLength, double backLength,
                            double backSide, double scale, float rotation, int color, int colorOuter) {

        scale = Math.max(scale, 1);
        progress = Mth.clamp(progress, 0D, 1D) * scale;

        float angle = (float) Math.toRadians(-rotation);
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);

        // Original-Vektoren, dann rotateAroundZ(angle)
        double[] tip = rot(0, -tipLength, cos, sin);
        double[] right = rot(-backSide, 0, cos, sin);
        double[] bRight = rot(-backSide, backLength, cos, sin);
        double[] bLeft = rot(backSide, backLength, cos, sin);
        double[] left = rot(backSide, 0, cos, sin);

        double deltaX = progress * cos;
        double deltaY = progress * sin;
        double cx = x + deltaX;
        double cy = y + deltaY;

        Matrix4f matrix = guiGraphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = com.hbm_m.platform.RenderHooks.beginTesselator(tesselator, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        double mult = 1.5;
        // Kontur: wie im Original werden die hinteren Ecken nur in X vergroessert
        pentagon(buffer, matrix, colorOuter,
                cx + tip[0] * mult, cy + tip[1] * mult,
                cx + right[0] * mult, cy + right[1] * mult,
                cx + bRight[0] * mult, cy + bRight[1],
                cx + bLeft[0] * mult, cy + bLeft[1],
                cx + left[0] * mult, cy + left[1] * mult);

        pentagon(buffer, matrix, color,
                cx + tip[0], cy + tip[1],
                cx + right[0], cy + right[1],
                cx + bRight[0], cy + bRight[1],
                cx + bLeft[0], cy + bLeft[1],
                cx + left[0], cy + left[1]);

        com.hbm_m.platform.RenderHooks.drawWithShader(buffer);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Vec3.rotateAroundZ des Originals. */
    private static double[] rot(double vx, double vy, float cos, float sin) {
        return new double[] { vx * cos + vy * sin, vy * cos - vx * sin };
    }

    /** GL_POLYGON des Originals als Faecher aus drei Dreiecken. */
    private static void pentagon(BufferBuilder buffer, Matrix4f matrix, int color,
                                 double x0, double y0, double x1, double y1, double x2, double y2,
                                 double x3, double y3, double x4, double y4) {
        tri(buffer, matrix, color, x0, y0, x1, y1, x2, y2);
        tri(buffer, matrix, color, x0, y0, x2, y2, x3, y3);
        tri(buffer, matrix, color, x0, y0, x3, y3, x4, y4);
    }

    private static void tri(BufferBuilder buffer, Matrix4f matrix, int color,
                            double ax, double ay, double bx, double by, double cx, double cy) {
        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;
        //? if < 1.21.1 {
        buffer.vertex(matrix, (float) ax, (float) ay, 0F).color(r, g, b, 1F).endVertex();
        buffer.vertex(matrix, (float) bx, (float) by, 0F).color(r, g, b, 1F).endVertex();
        buffer.vertex(matrix, (float) cx, (float) cy, 0F).color(r, g, b, 1F).endVertex();
        //?} else {
        /*buffer.addVertex(matrix, (float) ax, (float) ay, 0F).setColor(r, g, b, 1F);
        buffer.addVertex(matrix, (float) bx, (float) by, 0F).setColor(r, g, b, 1F);
        buffer.addVertex(matrix, (float) cx, (float) cy, 0F).setColor(r, g, b, 1F);
        *///?}
    }
}
