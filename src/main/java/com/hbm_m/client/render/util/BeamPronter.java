package com.hbm_m.client.render.util;

import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.render.util.BeamPronter}: Strahl entlang {@code skeleton} aus Segmenten, die spiralfoermig oder
 * zufaellig um die Achse versetzt sind. SOLID zeichnet je Segment {@code layers} Kastenhuellen (von aussen
 * {@code outerColor} nach innen {@code innerColor}, additiv, ohne Culling), LINE eine Linienkette plus Mittellinie.
 */
public final class BeamPronter {

    public static final Random rand = new Random();

    public enum EnumWaveType { RANDOM, SPIRAL }

    public enum EnumBeamType { SOLID, LINE }

    private BeamPronter() {}

    public static void prontBeamwithDepth(PoseStack ps, MultiBufferSource buffers, Vec3 skeleton, EnumWaveType wave, EnumBeamType beam,
                                          int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness) {
        pront(ps, buffers, skeleton, wave, beam, outerColor, innerColor, start, segments, size, layers, thickness, true);
    }

    public static void prontBeam(PoseStack ps, MultiBufferSource buffers, Vec3 skeleton, EnumWaveType wave, EnumBeamType beam,
                                 int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness) {
        pront(ps, buffers, skeleton, wave, beam, outerColor, innerColor, start, segments, size, layers, thickness, false);
    }

    /** 1.7 {@code Vec3.rotateAroundY}. */
    private static double[] rotY(double[] v, float a) {
        float c = Mth.cos(a), s = Mth.sin(a);
        return new double[] { v[0] * c + v[2] * s, v[1], v[2] * c - v[0] * s };
    }

    private static void pront(PoseStack ps, MultiBufferSource buffers, Vec3 skeleton, EnumWaveType wave, EnumBeamType beam,
                              int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness, boolean depth) {
        ps.pushPose();

        float sYaw = (float) (Math.atan2(skeleton.x, skeleton.z) * 180F / Math.PI);
        float sqrt = Mth.sqrt((float) (skeleton.x * skeleton.x + skeleton.z * skeleton.z));
        float sPitch = (float) (Math.atan2(skeleton.y, sqrt) * 180F / Math.PI);

        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(sYaw));
        ps.mulPose(Axis.XP.rotationDegrees(sPitch - 90));

        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buffers.getBuffer(beam == EnumBeamType.SOLID
                ? (depth ? ClientRenderHandler.CustomRenderTypes.BEAM_SOLID_DEPTH : ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW)
                : ClientRenderHandler.CustomRenderTypes.BEAM_LINES);

        rand.setSeed(start);
        double length = skeleton.length();
        double segLength = length / segments;
        double lastX = 0, lastY = 0, lastZ = 0;

        for (int i = 0; i <= segments; i++) {
            double[] spinner = { size, 0, 0 };

            if (wave == EnumWaveType.SPIRAL) {
                spinner = rotY(spinner, (float) Math.PI * (float) start / 180F);
                spinner = rotY(spinner, (float) Math.PI * 45F / 180F * i);
            } else if (wave == EnumWaveType.RANDOM) {
                spinner = rotY(spinner, (float) Math.PI * 2 * rand.nextFloat());
                spinner = rotY(spinner, (float) Math.PI * 2 * rand.nextFloat());
            }

            double pX = spinner[0];
            double pY = segLength * i + spinner[1];
            double pZ = spinner[2];

            if (beam == EnumBeamType.LINE && i > 0) {
                line(vc, m, pX, pY, pZ, lastX, lastY, lastZ, outerColor);
            }

            if (beam == EnumBeamType.SOLID && i > 0) {
                float radius = thickness / layers;

                for (int j = 1; j <= layers; j++) {
                    float inter = (float) (j - 1) / (float) (layers - 1);

                    int r1 = (outerColor & 0xFF0000) >> 16, g1 = (outerColor & 0x00FF00) >> 8, b1 = outerColor & 0x0000FF;
                    int r2 = (innerColor & 0xFF0000) >> 16, g2 = (innerColor & 0x00FF00) >> 8, b2 = innerColor & 0x0000FF;
                    int r = (int) (r1 + (r2 - r1) * inter), g = (int) (g1 + (g2 - g1) * inter), b = (int) (b1 + (b2 - b1) * inter);

                    double rj = radius * j;
                    quad(vc, m, r, g, b, lastX + rj, lastY, lastZ + rj, lastX + rj, lastY, lastZ - rj, pX + rj, pY, pZ - rj, pX + rj, pY, pZ + rj);
                    quad(vc, m, r, g, b, lastX - rj, lastY, lastZ + rj, lastX - rj, lastY, lastZ - rj, pX - rj, pY, pZ - rj, pX - rj, pY, pZ + rj);
                    quad(vc, m, r, g, b, lastX + rj, lastY, lastZ + rj, lastX - rj, lastY, lastZ + rj, pX - rj, pY, pZ + rj, pX + rj, pY, pZ + rj);
                    quad(vc, m, r, g, b, lastX + rj, lastY, lastZ - rj, lastX - rj, lastY, lastZ - rj, pX - rj, pY, pZ - rj, pX + rj, pY, pZ - rj);
                }
            }

            lastX = pX;
            lastY = pY;
            lastZ = pZ;
        }

        if (beam == EnumBeamType.LINE) {
            line(vc, m, 0, 0, 0, 0, length, 0, innerColor);
        }

        ps.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int r, int g, int b,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4) {
        //? if < 1.21.1 {
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, 255).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, 255).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3).color(r, g, b, 255).endVertex();
        vc.vertex(m, (float) x4, (float) y4, (float) z4).color(r, g, b, 255).endVertex();
        //?} else {
        /*vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, 255);
        vc.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, 255);
        vc.addVertex(m, (float) x3, (float) y3, (float) z3).setColor(r, g, b, 255);
        vc.addVertex(m, (float) x4, (float) y4, (float) z4).setColor(r, g, b, 255);
        *///?}
    }

    private static void line(VertexConsumer vc, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        //? if < 1.21.1 {
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, 255).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, 255).endVertex();
        //?} else {
        /*vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, 255);
        vc.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, 255);
        *///?}
    }
}
