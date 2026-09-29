package com.hbm_m.client.render.implementations;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.grenades.AirBombProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

//? if < 1.21.1 {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} else {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class AirBombProjectileEntityRenderer extends EntityRenderer<AirBombProjectileEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public AirBombProjectileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(AirBombProjectileEntity entity,
                       float entityYaw,
                       float partialTicks,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight) {

        poseStack.pushPose();

        // Sync with the plane: rotate by yaw
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getSynchedYaw()));

        // Constant tilt toward the ground: +1 degree every 10 ticks (cumulative)
        float tiltAngle = (entity.tickCount / 10.0F) * 7.0F;  // 0 -> 1 -> 2 -> 3 degrees...
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltAngle));  // nose-down tilt

        // Center the model
        poseStack.translate(-0.5, 0.0, -0.5);

        // Render using the AIRBOMB block
        BlockState state = ModBlocks.AIRBOMB.get().defaultBlockState();

        // Draw the bomb model
        blockRenderer.renderSingleBlock(
                state,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AirBombProjectileEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    }
}

