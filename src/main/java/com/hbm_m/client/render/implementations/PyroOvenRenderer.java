package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachinePyroOvenBlock;
import com.hbm_m.blockentity.machines.MachinePyroOvenBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderPyroOven}: Ofen, hin- und herfahrender Schieber und drehender Luefter. */
public class PyroOvenRenderer implements com.hbm_m.client.render.HbmBerBounds<MachinePyroOvenBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/pyrooven.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/pyrooven.png");

    public PyroOvenRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachinePyroOvenBlockEntity pyro, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 180/270/0/90 Grad; Port-FACING entspricht dieser Richtung
        switch (pyro.getBlockState().getValue(MachinePyroOvenBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        float anim = pyro.prevAnim + (pyro.anim - pyro.prevAnim) * f;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));

        MODEL.renderPart("Oven", ps, vc, light);

        ps.pushPose();
        ps.translate(Math.sin(Math.PI / 2D * Math.cos(anim * 0.125)) / 2 - 0.5, 0, 0); // BobMathUtil.sps
        MODEL.renderPart("Slider", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(1.5, 0, 1.5);
        ps.mulPose(Axis.YP.rotationDegrees((float) (anim * 45D % 360D)));
        ps.translate(-1.5, 0, -1.5);
        MODEL.renderPart("Fan", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachinePyroOvenBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
