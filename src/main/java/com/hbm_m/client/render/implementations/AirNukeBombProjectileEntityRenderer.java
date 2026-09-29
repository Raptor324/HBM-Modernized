package com.hbm_m.client.render.implementations;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.grenades.AirNukeBombProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

@OnlyIn(Dist.CLIENT)
public class AirNukeBombProjectileEntityRenderer extends EntityRenderer<AirNukeBombProjectileEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public AirNukeBombProjectileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(AirNukeBombProjectileEntity entity,
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
        BlockState state = ModBlocks.BALEBOMB_TEST.get().defaultBlockState();

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
    public ResourceLocation getTextureLocation(AirNukeBombProjectileEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    }
}

