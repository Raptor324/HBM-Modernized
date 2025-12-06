package com.hbm_m.client.render;

import com.hbm_m.entity.grenades.AirBombProjectileEntity;
import com.hbm_m.block.ModBlocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

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

        // ✅ Смещение центра модели
        poseStack.translate(-0.5, 0.0, -0.5);

        // Используем блок AIRBOMB для рендера
        BlockState state = ModBlocks.AIRBOMB.get().defaultBlockState();

        // Рисуем модель авиабомбы
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
        // Не используется при рендере через blockRenderer, можно вернуть что‑нибудь дефолтное
        return new ResourceLocation("minecraft", "textures/block/iron_block.png");
    }
}