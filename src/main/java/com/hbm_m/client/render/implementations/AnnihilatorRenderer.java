package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineAnnihilatorBlock;
import com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderAnnihilator}: Gehaeuse, drehende Walze und das laufende Band (Texturverschiebung). */
public class AnnihilatorRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineAnnihilatorBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/annihilator.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/annihilator.png");
    public static final ResourceLocation BELT_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/annihilator_belt.png");

    public AnnihilatorRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineAnnihilatorBlockEntity te, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        // Original: Metadaten 2/4/3/5 -> 0/90/180/270 Grad; Port-FACING entspricht dieser Richtung
        switch (te.getBlockState().getValue(MachineAnnihilatorBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderPart("Annihilator", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        ps.pushPose();
        ps.translate(0, 1.75, 0);
        ps.mulPose(Axis.ZN.rotationDegrees((float) (System.currentTimeMillis() * 0.15 % 360)));
        ps.translate(0, -1.75, 0);
        MODEL.renderPart("Roller", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
        ps.popPose();

        float shift = (float) (-System.currentTimeMillis() / 3000D % 1D);
        MODEL.renderPart("Belt", ps, new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(BELT_TEX)), shift, 0F), light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineAnnihilatorBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
