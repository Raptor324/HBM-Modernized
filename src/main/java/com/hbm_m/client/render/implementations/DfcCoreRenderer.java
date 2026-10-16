package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import org.joml.Matrix4f;

/**
 * 1:1-Port von {@code RenderCore} (1.7.10): der Block selbst ist ein Wuerfel, darueber sitzt die
 * Kugel - kalt eine graue mit dunklem Schleier, heiss eine in Katalysatorfarbe, die mit dem
 * Fuellstand waechst und von siebzehn pulsierenden additiven Huellen umgeben ist.
 *
 * <p><b>Abweichung:</b> das Flackern des Originals im Schmelz-Tick ({@code renderFlare}) fehlt,
 * die Kugel bleibt in dem Tick stehen.</p>
 */
public class DfcCoreRenderer implements com.hbm_m.client.render.HbmBerBounds<DFCCoreBlockEntity> {

    public DfcCoreRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(DFCCoreBlockEntity be) {
        return true;
    }

    @Override
    public void render(DFCCoreBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.getLevel() == null) return;
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        if (be.getHeat() == 0) {
            renderStandby(pose, buffer);
        } else {
            renderOrb(be, pose, buffer);
        }
        pose.popPose();
    }

    /** Original: {@code renderStandby} - graue Kugel (Radius 1/4) mit einem dunklen additiven Schleier. */
    private static void renderStandby(PoseStack pose, MultiBufferSource buffer) {
        List<float[]> sphere = ObjMachines.obj("sphere_uv").get("Sphere");
        if (sphere == null) return;
        pose.pushPose();
        pose.scale(0.25F, 0.25F, 0.25F);
        sphere(buffer.getBuffer(RenderType.debugQuads()), pose.last().pose(), sphere, 0.5F, 0.5F, 0.5F, 1F);
        pose.scale(1.25F, 1.25F, 1.25F);
        sphere(buffer.getBuffer(RenderType.lightning()), pose.last().pose(), sphere, 0.1F, 0.1F, 0.1F, 1F);
        pose.popPose();
    }

    /** Original: {@code renderOrb}. */
    private static void renderOrb(DFCCoreBlockEntity be, PoseStack pose, MultiBufferSource buffer) {
        List<float[]> sphere = ObjMachines.obj("sphere_ruv").get("Icosphere");
        if (sphere == null) return;
        int color = be.getColor();
        float mod = 0.4F;
        float r = ((color & 0xFF0000) >> 16) / 256F * mod;
        float g = ((color & 0x00FF00) >> 8) / 256F * mod;
        float b = (color & 0x0000FF) / 256F * mod;

        FluidTank[] tanks = be.getTanks();
        int tot = tanks[0].getMaxFill() + tanks[1].getMaxFill();
        int fill = tanks[0].getFill() + tanks[1].getFill();
        float scale = tot > 0 ? 4.5F * fill / tot + 0.5F : 0.5F;

        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.scale(0.25F, 0.25F, 0.25F);
        sphere(buffer.getBuffer(RenderType.debugQuads()), pose.last().pose(), sphere, r, g, b, 1F);

        double ix = (be.getLevel().getGameTime() * 0.1D) % (Math.PI * 2D);
        double t = 0.8D;
        float pulse = (float) ((1D / t) * Math.atan((t * Math.sin(ix)) / (1D - t * Math.cos(ix))));
        pulse = (pulse + 1F) / 2F;

        VertexConsumer additive = buffer.getBuffer(RenderType.lightning());
        for (int i = 0; i <= 16; i++) {
            pose.pushPose();
            float s = 1F + 0.25F * i + pulse * (20 - i) * 0.125F;
            pose.scale(s, s, s);
            sphere(additive, pose.last().pose(), sphere, r, g, b, 1F);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void sphere(VertexConsumer vc, Matrix4f m, List<float[]> tris, float r, float g, float b, float a) {
        int ir = (int) (r * 255F), ig = (int) (g * 255F), ib = (int) (b * 255F), ia = (int) (a * 255F);
        for (float[] tri : tris) {
            for (int pass = 0; pass < 4; pass++) {   // 4th repeats the 3rd: a degenerate quad
                int base = Math.min(pass, 2) * 8;
                RenderHooks.vertexColor(vc, m, tri[base], tri[base + 1], tri[base + 2], ir, ig, ib, ia);
            }
        }
    }
}
