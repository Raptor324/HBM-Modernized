package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachinePUREXBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderPUREX}: Sockel, optionales Geruest, drehender Luefter und pendelnder Pumpenkolben. */
public class PurexRenderer implements com.hbm_m.client.render.HbmBerBounds<MachinePUREXBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/purex.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/purex.png");

    public PurexRenderer(BlockEntityRendererProvider.Context ctx) { }

    /** {@code BobMathUtil.sps}. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public void render(MachinePUREXBlockEntity purex, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        // Original: Metadaten 2/4/3/5 -> 0/90/180/270 Grad
        switch (purex.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        float anim = purex.prevAnim + (purex.anim - purex.prevAnim) * interp;

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        MODEL.renderPart("Base", ps, vc, light);
        if (purex.frame) MODEL.renderPart("Frame", ps, vc, light);

        ps.pushPose();
        ps.translate(1.5, 1.25, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(anim * 45));
        ps.translate(-1.5, -1.25, 0);
        MODEL.renderPart("Fan", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(sps(anim * 0.25) * 0.5, 0, 0);
        MODEL.renderPart("Pump", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
