package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.Vec3NT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderPumpjack}: Kurbel (Rotor) dreht mit {@code rot - 90}, Pferdekopf wippt mit
 * {@code sin(rot) * 0.25} rad, Gestaengewagen hebt sich um {@code -sin(rot)}; Pleuel, Seile und Polierstange
 * als untexturierte Flaechen (Grau 0.5 bzw. 0.2), ohne Licht und ohne Culling.
 */
public class PumpjackRenderer implements com.hbm_m.client.render.HbmBerBounds<MachinePumpjackBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/pumpjack.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/pumpjack.png");

    public PumpjackRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachinePumpjackBlockEntity pj, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        switch (ObjBerHelper.meta(pj)) {
            case 2 -> ObjBerHelper.rotY(ps, 90);
            case 4 -> ObjBerHelper.rotY(ps, 180);
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
        }

        float rotation = pj.prevRot + (pj.rot - pj.prevRot) * f;

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Base", ps, vc, light);

        ps.pushPose();
        ps.translate(0, 1.5, -5.5);
        ps.mulPose(Axis.XP.rotationDegrees(rotation - 90));
        ps.translate(0, -1.5, 5.5);
        MODEL.renderPart("Rotor", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 3.5, -3.5);
        ps.mulPose(Axis.XP.rotationDegrees((float) (Math.toDegrees(Math.sin(Math.toRadians(rotation))) * 0.25)));
        ps.translate(0, -3.5, 3.5);
        MODEL.renderPart("Head", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, -Math.sin(Math.toRadians(rotation)), 0);
        MODEL.renderPart("Carriage", ps, vc, light);
        ps.popPose();

        Vec3NT backPos = Vec3NT.createVectorHelper(0, 0, -2);
        backPos.rotateAroundX(-(float) Math.sin(Math.toRadians(rotation)) * 0.25F);
        Vec3NT rot = Vec3NT.createVectorHelper(0, 0.5, 0);
        rot.rotateAroundX(-(float) Math.toRadians(rotation - 90));

        VertexConsumer lines = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL);
        Matrix4f m = ps.last().pose();

        float c = 0.5F;
        for (int i = -1; i <= 1; i += 2) {
            quad(lines, m, c, c, c,
                    0.53125 * i, 1.5 + rot.yCoord, -5.5 + rot.zCoord - 0.0625D,
                    0.53125 * i, 1.5 + rot.yCoord, -5.5 + rot.zCoord + 0.0625D,
                    0.53125 * i, 3.5 + backPos.yCoord, -3.5 + backPos.zCoord + 0.0625D,
                    0.53125 * i, 3.5 + backPos.yCoord, -3.5 + backPos.zCoord - 0.0625D);
        }

        c = 0.2F;
        double pd = 0.03125D;
        double width = 0.25D;
        double height = -Math.sin(Math.toRadians(rotation));

        for (int i = -1; i <= 1; i += 2) {
            float pRot = -(float) (Math.sin(Math.toRadians(rotation)) * 0.25);
            Vec3NT frontPos = Vec3NT.createVectorHelper(0, 0, 1);
            frontPos.rotateAroundX(pRot);
            double dist = 0.03125D;
            Vec3NT frontRad = Vec3NT.createVectorHelper(0, 0, 2.5 + dist);
            double cutlet = 360D / 32D;
            frontRad.rotateAroundX(pRot);
            frontRad.rotateAroundX(-(float) Math.toRadians(cutlet * -3));

            for (int j = 0; j < 4; j++) {
                double sumY = frontPos.yCoord + frontRad.yCoord;
                double sumZ = frontPos.zCoord + frontRad.zCoord;
                if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
                double ax = (width - pd) * i, ay = 3.5 + sumY, az = -3.5 + sumZ;
                double bx = (width + pd) * i;
                frontRad.rotateAroundX(-(float) Math.toRadians(cutlet));
                sumY = frontPos.yCoord + frontRad.yCoord;
                sumZ = frontPos.zCoord + frontRad.zCoord;
                if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
                quad(lines, m, c, c, c,
                        ax, ay, az,
                        bx, ay, az,
                        (width + pd) * i, 3.5 + sumY, -3.5 + sumZ,
                        (width - pd) * i, 3.5 + sumY, -3.5 + sumZ);
            }

            double sumY = frontPos.yCoord + frontRad.yCoord;
            double sumZ = frontPos.zCoord + frontRad.zCoord;
            if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
            quad(lines, m, c, c, c,
                    (width + pd) * i, 3.5 + sumY, -3.5 + sumZ,
                    (width - pd) * i, 3.5 + sumY, -3.5 + sumZ,
                    (width - pd) * i, 2 + height, 0,
                    (width + pd) * i, 2 + height, 0);
        }

        double p = 0.03125D;
        quad(lines, m, c, c, c,
                p, height + 1.5, p,
                -p, height + 1.5, -p,
                -p, 0.75, -p,
                p, 0.75, p);
        quad(lines, m, c, c, c,
                -p, height + 1.5, p,
                p, height + 1.5, -p,
                p, 0.75, -p,
                -p, 0.75, p);

        ps.popPose();
    }

    /** Tessellator-Quad als zwei Dreiecke (POSITION_COLOR). */
    static void quad(VertexConsumer vc, Matrix4f m, float r, float g, float b,
                     double x1, double y1, double z1, double x2, double y2, double z2,
                     double x3, double y3, double z3, double x4, double y4, double z4) {
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, 1F).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, 1F).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3).color(r, g, b, 1F).endVertex();
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, 1F).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3).color(r, g, b, 1F).endVertex();
        vc.vertex(m, (float) x4, (float) y4, (float) z4).color(r, g, b, 1F).endVertex();
    }

    @Override public boolean shouldRenderOffScreen(MachinePumpjackBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
