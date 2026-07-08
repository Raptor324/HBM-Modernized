package com.hbm_m.client.render.effect;

import com.hbm_m.entity.projectile.RubbleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code com.hbm.render.entity.projectile.RenderRubble} — текстура из блока-источника.
 */
public class RubbleEntityRenderer extends EntityRenderer<RubbleEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public RubbleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(RubbleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        float rot = ((entity.tickCount + partialTick) % 360.0F) * 10.0F;
        poseStack.mulPose(Axis.XP.rotationDegrees(rot));
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        BlockState state = entity.getBlockState();
        blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RubbleEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/stone.png");
    }
}
