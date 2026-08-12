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
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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

        //  СИНХРОНИЗАЦИЯ С САМОЛЁТОМ: поворот по Yaw
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getSynchedYaw()));

        // 🆕 ПОСТОЯННЫЙ НАКЛОН К ЗЕМЛЕ: +1° каждые 10 тиков (НАКОПИТЕЛЬНО)
        float tiltAngle = (entity.tickCount / 10.0F) * 7.0F;  // 0° → 1° → 2° → 3°...
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltAngle));  // Наклон носом вниз

        //  Смещение центра модели
        poseStack.translate(-0.5, 0.0, -0.5);

        // Используем блок AIRBOMB для рендера
        BlockState state = ModBlocks.BALEBOMB_TEST.get().defaultBlockState();

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
    public ResourceLocation getTextureLocation(AirNukeBombProjectileEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    }
}

