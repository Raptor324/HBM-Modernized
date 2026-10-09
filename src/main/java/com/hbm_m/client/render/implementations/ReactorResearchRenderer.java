package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderSmallReactor}: Sockel, die mit der Stabstellung hochfahrenden Steuerstaebe und - bei einem Fluss
 * ueber 10 unter Wasser - die additive Tscherenkow-Huelle aus 17 ineinanderliegenden Wuerfeln.
 */
public class ReactorResearchRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineReactorResearchBlockEntity> {

    public static final SimpleObjModel BASE = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/reactor_small_base.obj"));
    public static final SimpleObjModel RODS = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/reactor_small_rods.obj"));
    public static final ResourceLocation BASE_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/reactor_small_base.png");
    public static final ResourceLocation RODS_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/reactor_small_rods.png");

    public ReactorResearchRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineReactorResearchBlockEntity reactor, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(180));

        BASE.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(BASE_TEX)), light);

        double level = (reactor.lastLevel + (reactor.level - reactor.lastLevel) * f);

        ps.pushPose();
        ps.translate(0.0D, level, 0.0D);
        RODS.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(RODS_TEX)), light);
        ps.popPose();

        if (reactor.totalFlux > 10 && reactor.isSubmerged()) {

            VertexConsumer vc = buf.getBuffer(com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.RBMK_CHERENKOV);
            org.joml.Matrix4f m = ps.last().pose();

            for (double dd = 0.285; dd < 0.7; dd += 0.025) {

                float d = (float) dd;
                float a = 0.025F + (float) (Math.random() * 0.015F) + (0.125F * reactor.totalFlux / 1000F);

                float top = 1.375F;
                float bottom = 1.375F;

                quad(vc, m, a, d, bottom - d, -d, d, top + d, -d, d, top + d, d, d, bottom - d, d);
                quad(vc, m, a, -d, bottom - d, -d, -d, top + d, -d, -d, top + d, d, -d, bottom - d, d);
                quad(vc, m, a, -d, bottom - d, d, -d, top + d, d, d, top + d, d, d, bottom - d, d);
                quad(vc, m, a, -d, bottom - d, -d, -d, top + d, -d, d, top + d, -d, d, bottom - d, -d);
                quad(vc, m, a, -d, top + d, -d, -d, top + d, d, d, top + d, d, d, top + d, -d);
                quad(vc, m, a, -d, bottom - d, -d, -d, bottom - d, d, d, bottom - d, d, d, bottom - d, -d);
            }
        }

        ps.popPose();
    }

    private static void quad(VertexConsumer vc, org.joml.Matrix4f m, float a,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        //? if < 1.21.1 {
        vc.vertex(m, x0, y0, z0).color(0.4F, 0.9F, 1.0F, a).endVertex();
        vc.vertex(m, x1, y1, z1).color(0.4F, 0.9F, 1.0F, a).endVertex();
        vc.vertex(m, x2, y2, z2).color(0.4F, 0.9F, 1.0F, a).endVertex();
        vc.vertex(m, x3, y3, z3).color(0.4F, 0.9F, 1.0F, a).endVertex();
        //?} else {
        /*vc.addVertex(m, x0, y0, z0).setColor(0.4F, 0.9F, 1.0F, a);
        vc.addVertex(m, x1, y1, z1).setColor(0.4F, 0.9F, 1.0F, a);
        vc.addVertex(m, x2, y2, z2).setColor(0.4F, 0.9F, 1.0F, a);
        vc.addVertex(m, x3, y3, z3).setColor(0.4F, 0.9F, 1.0F, a);
        *///?}
    }

    @Override public boolean shouldRenderOffScreen(MachineReactorResearchBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
