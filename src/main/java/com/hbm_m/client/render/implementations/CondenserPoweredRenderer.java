package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineCondenserPoweredBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderCondenser}: Gehaeuse und zwei gegenlaeufige Luefter. */
public class CondenserPoweredRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineCondenserPoweredBlockEntity> {

    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/condenser.png");

    public CondenserPoweredRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineCondenserPoweredBlockEntity condenser, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (condenser.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        CompressorRenderer.CONDENSER.renderPart("Condenser", ps, vc, light);

        float rot = condenser.lastSpin + (condenser.spin - condenser.lastSpin) * f;

        ps.pushPose();
        ps.translate(0, 1.5, 0);
        ps.mulPose(Axis.XP.rotationDegrees(rot));
        ps.translate(0, -1.5, 0);
        CompressorRenderer.CONDENSER.renderPart("Fan1", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 1.5, 0);
        ps.mulPose(Axis.XN.rotationDegrees(rot));
        ps.translate(0, -1.5, 0);
        CompressorRenderer.CONDENSER.renderPart("Fan2", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineCondenserPoweredBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
