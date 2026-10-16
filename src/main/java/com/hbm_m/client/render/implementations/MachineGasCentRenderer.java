package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineGasCentBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

//? if < 1.21.1 {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} else {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineGasCentRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineGasCentBlockEntity> {

    public MachineGasCentRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineGasCentBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.75, 0.5);
        poseStack.mulPose(Axis.YP.rotation(blockEntity.getAnim(partialTick)));
        poseStack.translate(-0.5, -0.75, -0.5);
        poseStack.popPose();
    }
}
