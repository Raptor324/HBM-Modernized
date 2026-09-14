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
 * 1:1-Port von {@code GUIElements.drawSmoothGauge} (1.7.10): ein Zeiger als zwei Dreiecke -
 * aussen eine um Faktor 1,5 vergroesserte Kontur, innen der eigentliche Zeiger. Der Ausschlag
 * laeuft ueber 270 Grad und beginnt bei -45 Grad, genau wie im Original.
 */
public final class GuiGaugeNeedle {

    private GuiGaugeNeedle() {}

    public static void draw(GuiGraphics guiGraphics, int x, int y, double progress,
                            double tipLength, double backLength, double backSide, int color) {
        draw(guiGraphics, x, y, progress, tipLength, backLength, backSide, color, 0x000000);
    }

    public static void draw(GuiGraphics guiGraphics, int x, int y, double progress,
                            double tipLength, double backLength, double backSide,
                            int color, int colorOuter) {

        progress = Mth.clamp(progress, 0D, 1D);

        float angle = (float) Math.toRadians(-progress * 270 - 45);
        float sin = Mth.sin(angle);
        float cos = Mth.cos(angle);

        // Rotation um Z wie im Original (Vec3.rotateAroundZ).
        double tipX = -tipLength * sin;
        double tipY = tipLength * cos;
        double leftX = backSide * cos - (-backLength) * sin;
        double leftY = backSide * sin + (-backLength) * cos;
        double rightX = -backSide * cos - (-backLength) * sin;
        double rightY = -backSide * sin + (-backLength) * cos;

        Matrix4f matrix = guiGraphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        double mult = 1.5D;
        addTriangle(buffer, matrix, x, y, tipX * mult, tipY * mult, leftX * mult, leftY * mult,
                rightX * mult, rightY * mult, colorOuter);
        addTriangle(buffer, matrix, x, y, tipX, tipY, leftX, leftY, rightX, rightY, color);

        tesselator.end();

        RenderSystem.disableBlend();
    }

    private static void addTriangle(BufferBuilder buffer, Matrix4f matrix, int x, int y,
                                    double tipX, double tipY, double leftX, double leftY,
                                    double rightX, double rightY, int color) {
        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        buffer.vertex(matrix, (float) (x + tipX), (float) (y + tipY), 0F).color(r, g, b, 1F).endVertex();
        buffer.vertex(matrix, (float) (x + leftX), (float) (y + leftY), 0F).color(r, g, b, 1F).endVertex();
        buffer.vertex(matrix, (float) (x + rightX), (float) (y + rightY), 0F).color(r, g, b, 1F).endVertex();
    }
}
