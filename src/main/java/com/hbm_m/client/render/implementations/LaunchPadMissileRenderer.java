package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderRegistry;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.item.missile.MissileItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class LaunchPadMissileRenderer implements BlockEntityRenderer<LaunchPadBaseBlockEntity> {

    public LaunchPadMissileRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LaunchPadBaseBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack missileStack = be.getMissilePreviewStack();
        if (missileStack.isEmpty() || !(missileStack.getItem() instanceof MissileItem)) {
            return;
        }

        MissileRenderData renderData = MissileRenderRegistry.get(missileStack);
        if (renderData == null) {
            return;
        }

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        //? if forge {
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                drawMissileOnPad(be, renderData, poseStack, buffer, packedLight);
            }
            return;
        }
        //?}

        drawMissileOnPad(be, renderData, poseStack, buffer, packedLight);
    }

    private static void drawMissileOnPad(LaunchPadBaseBlockEntity be, MissileRenderData renderData,
                                         PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);

        Direction facing = Direction.NORTH;
        if (be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        switch (facing) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }

        poseStack.translate(0.0F, 1.0F, 0.0F);

        renderData.render(poseStack, packedLight, be.getBlockPos(), buffer, be);
        poseStack.popPose();
    }
}
