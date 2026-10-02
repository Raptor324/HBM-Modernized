package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineLargeTurbineBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderBigTurbine}: Gehaeuse und der drehende Laeufer. */
public class LargeTurbineRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineLargeTurbineBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/turbine.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/turbine.png");
    public static final ResourceLocation BLADES = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/turbofan_blades.png");

    public LargeTurbineRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineLargeTurbineBlockEntity turbine, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (turbine.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        ps.translate(0, 0, -1);

        MODEL.renderPart("Body", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        ps.translate(0, 1, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(turbine.lastRotor + (turbine.rotor - turbine.lastRotor) * f));
        ps.translate(0, -1, 0);

        MODEL.renderPart("Blades", ps, buf.getBuffer(RenderType.entityCutoutNoCull(BLADES)), light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineLargeTurbineBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
