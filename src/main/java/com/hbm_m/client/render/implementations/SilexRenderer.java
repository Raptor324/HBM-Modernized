package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineSilexBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderSILEX}: das statische Modell der SILEX, gedreht nach Blickrichtung. */
public class SilexRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineSilexBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/silex.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/silex.png");

    public SilexRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineSilexBlockEntity te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 4/3/5/2 -> 180/270/0/90 Grad
        switch (te.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
