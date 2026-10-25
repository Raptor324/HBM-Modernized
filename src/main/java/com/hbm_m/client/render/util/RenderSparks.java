package com.hbm_m.client.render.util;

import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.render.util.RenderSparks}: Zickzack-Linie aus {@code min + rand(max)} Segmenten in zwei Farben
 * (Original GL_LINE_STRIP Breite 5 und 2, hier als Linien ohne Textur und Licht).
 */
public final class RenderSparks {

    private RenderSparks() {}

    public static void renderSpark(PoseStack ps, MultiBufferSource buf, int seed, double x, double y, double z,
                                   float length, int min, int max, int color1, int color2) {
        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.BEAM_LINES);
        Matrix4f m = ps.last().pose();
        Random rand = new Random(seed);
        Vec3 vec = new Vec3(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5).normalize();

        for (int i = 0; i < min + rand.nextInt(max); i++) {
            double prevX = x, prevY = y, prevZ = z;
            Vec3 dir = vec.normalize();
            double dx = dir.x * length * rand.nextFloat();
            double dy = dir.y * length * rand.nextFloat();
            double dz = dir.z * length * rand.nextFloat();
            x = prevX + dx;
            y = prevY + dy;
            z = prevZ + dz;
            for (int c : new int[] { color1, color2 }) {
                int cr = c >> 16 & 255, cg = c >> 8 & 255, cb = c & 255;
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, (float) prevX, (float) prevY, (float) prevZ, cr, cg, cb, 255);
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, (float) x, (float) y, (float) z, cr, cg, cb, 255);
            }
        }
    }
}
