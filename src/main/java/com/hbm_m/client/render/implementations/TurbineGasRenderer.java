package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderTurbineGas}: das statische Modell der Gasturbine, gedreht nach Blickrichtung. */
public class TurbineGasRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineTurbineGasBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/turbinegas.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/turbinegas.png");

    public TurbineGasRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineTurbineGasBlockEntity te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (te.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
