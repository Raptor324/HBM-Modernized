package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineIntakeBlock;
import com.hbm_m.blockentity.machines.MachineIntakeBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderIntake}: Gehaeuse und der um die Hochachse drehende Luefter ({@code fan/prevFan}). */
public class IntakeRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineIntakeBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/intake.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/intake.png");

    public IntakeRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineIntakeBlockEntity compressor, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (compressor.getBlockState().getValue(MachineIntakeBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        ps.translate(-0.5D, 0D, 0.5D);

        // GL_CULL_FACE war aus
        MODEL.renderPart("Base", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        float rot = compressor.prevFan + (compressor.fan - compressor.prevFan) * f;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-rot));
        MODEL.renderPart("Fan", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);
        ps.popPose();

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineIntakeBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
