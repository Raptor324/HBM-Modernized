package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineRockMillBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderRockMill}: Sockel, optionales Geruest und das drehende Mahlrad. */
public class RockMillRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineRockMillBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/rockmill.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/rockmill.png");

    public RockMillRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineRockMillBlockEntity mill, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        // Original: Metadaten 2/4/3/5 -> 0/90/180/270 Grad
        switch (mill.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        MODEL.renderPart("Base", ps, vc, light);
        if (mill.frame) MODEL.renderPart("Frame", ps, vc, light);

        float rot = mill.prevRotation + (mill.rotation - mill.prevRotation) * interp;
        ps.mulPose(Axis.YN.rotationDegrees(rot));
        MODEL.renderPart("Wheel", ps, vc, light);

        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
