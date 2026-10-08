package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.DemonLampBlock;
import com.hbm_m.blockentity.machines.DemonLampBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderDemonLamp}: Lampe plus zwei Kraenze aus 16 blauen, additiven Lichtfaechern bis 15 Bloecke. */
public class DemonLampRenderer implements com.hbm_m.client.render.HbmBerBounds<DemonLampBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/blocks/demon_lamp.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/demon_lamp.png");

    public DemonLampRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(DemonLampBlockEntity te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0.5D, 0.5D);
        switch (te.getBlockState().getValue(DemonLampBlock.FACING)) {
            case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(180));
            case UP -> { }
            case NORTH -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(180)); }
            case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(90)); }
            case EAST -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(270)); }
        }
        ps.translate(0, -0.5F, 0);

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
        Matrix4f m = ps.last().pose();
        double near = 0.375D, far = 15D;
        for (int j = 0; j < 2; j++) {
            double h = 0.5;
            double height = j == 0 ? -h : h;
            double vx = 1, vz = 0;
            for (int i = 0; i < 16; i++) {
                double y0 = 0.5D + j * 0.125D, y1 = y0 + height;
                float x0 = (float) (vx * near), z0 = (float) (vz * near), x1 = (float) (vx * far), z1 = (float) (vz * far);
                double a = Math.PI * 2D / 16D, c = Math.cos(a), s = Math.sin(a);
                double nvx = vx * c + vz * s, nvz = vz * c - vx * s;
                //? if < 1.21.1 {
                vc.vertex(m, x0, (float) y0, z0).color(0F, 0.75F, 1F, 0.25F).endVertex();
                vc.vertex(m, x1, (float) y1, z1).color(0F, 0.75F, 1F, 0F).endVertex();
                vc.vertex(m, (float) (nvx * far), (float) y1, (float) (nvz * far)).color(0F, 0.75F, 1F, 0F).endVertex();
                vc.vertex(m, (float) (nvx * near), (float) y0, (float) (nvz * near)).color(0F, 0.75F, 1F, 0.25F).endVertex();
                //?} else {
                /*vc.addVertex(m, x0, (float) y0, z0).setColor(0F, 0.75F, 1F, 0.25F);
                vc.addVertex(m, x1, (float) y1, z1).setColor(0F, 0.75F, 1F, 0F);
                vc.addVertex(m, (float) (nvx * far), (float) y1, (float) (nvz * far)).setColor(0F, 0.75F, 1F, 0F);
                vc.addVertex(m, (float) (nvx * near), (float) y0, (float) (nvz * near)).setColor(0F, 0.75F, 1F, 0.25F);
                *///?}
                vx = nvx;
                vz = nvz;
            }
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(DemonLampBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
