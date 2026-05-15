package com.hbm_m.client.render.implementations;

import com.hbm_m.block.entity.machines.MachineGasCentrifugeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class GasCentrifugeRenderer implements BlockEntityRenderer<MachineGasCentrifugeBlockEntity> {

    public GasCentrifugeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineGasCentrifugeBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.75, 0.5);
        poseStack.mulPose(Axis.YP.rotation(blockEntity.getAnim(partialTick)));
        poseStack.translate(-0.5, -0.75, -0.5);
        poseStack.popPose();
    }
}
