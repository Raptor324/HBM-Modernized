package com.hbm_m.inventory.gui;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Ersatz fuer die {@code GL_LINES}-Zeichnungen mit {@code glLineWidth} der Original-GUIs: eine Linie als duennes,
 * opakes Viereck senkrecht zur Strecke.
 */
public final class GuiLineHelper {

    private GuiLineHelper() {}

    public static void drawLine(GuiGraphics g, double x1, double y1, double x2, double y2, float width, int color) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len <= 0) return;
        double nx = -dy / len * width / 2D, ny = dx / len * width / 2D;

        float r = ((color >> 16) & 255) / 255F, gr = ((color >> 8) & 255) / 255F, b = (color & 255) / 255F;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder buf = com.hbm_m.platform.RenderHooks.beginTesselator(Tesselator.getInstance(), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        //? if < 1.21.1 {
        buf.vertex(m, (float) (x1 + nx), (float) (y1 + ny), 0F).color(r, gr, b, 1F).endVertex();
        buf.vertex(m, (float) (x2 + nx), (float) (y2 + ny), 0F).color(r, gr, b, 1F).endVertex();
        buf.vertex(m, (float) (x2 - nx), (float) (y2 - ny), 0F).color(r, gr, b, 1F).endVertex();
        buf.vertex(m, (float) (x1 - nx), (float) (y1 - ny), 0F).color(r, gr, b, 1F).endVertex();
        //?} else {
        /*buf.addVertex(m, (float) (x1 + nx), (float) (y1 + ny), 0F).setColor(r, gr, b, 1F);
        buf.addVertex(m, (float) (x2 + nx), (float) (y2 + ny), 0F).setColor(r, gr, b, 1F);
        buf.addVertex(m, (float) (x2 - nx), (float) (y2 - ny), 0F).setColor(r, gr, b, 1F);
        buf.addVertex(m, (float) (x1 - nx), (float) (y1 - ny), 0F).setColor(r, gr, b, 1F);
        *///?}
        com.hbm_m.platform.RenderHooks.drawWithShader(buf);
    }
}
