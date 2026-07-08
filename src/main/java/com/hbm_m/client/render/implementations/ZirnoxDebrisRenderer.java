package com.hbm_m.client.render.implementations;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.projectile.ZirnoxDebrisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class ZirnoxDebrisRenderer extends EntityRenderer<ZirnoxDebrisEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public ZirnoxDebrisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(ZirnoxDebrisEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float rot = entity.lastRot + (entity.rot - entity.lastRot) * partialTicks;
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        poseStack.mulPose(Axis.XP.rotationDegrees(rot * 0.5F));

        float scale = getScale(entity.getDebrisType());
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, -0.25, -0.5);

        BlockState state = getBlockState(entity.getDebrisType());
        blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private BlockState getBlockState(ZirnoxDebrisEntity.DebrisType type) {
        return switch (type) {
            case BLANK     -> ModBlocks.ZIRNOX_DEB_BLANK.get().defaultBlockState();
            case ELEMENT   -> ModBlocks.ZIRNOX_DEB_ELEMENT.get().defaultBlockState();
            case SHRAPNEL  -> ModBlocks.ZIRNOX_DEB_SHRAPNEL.get().defaultBlockState();
            case CONCRETE  -> ModBlocks.ZIRNOX_DEB_CONCRETE.get().defaultBlockState();
            case EXCHANGER -> ModBlocks.ZIRNOX_DEB_EXCHANGER.get().defaultBlockState();
        };
    }

    private float getScale(ZirnoxDebrisEntity.DebrisType type) {
        return switch (type) {
            case EXCHANGER -> 1.2F;
            case ELEMENT   -> 1.0F;
            case CONCRETE  -> 1.1F;
            case SHRAPNEL  -> 0.9F;
            default        -> 0.9F;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(ZirnoxDebrisEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");
    }
}
