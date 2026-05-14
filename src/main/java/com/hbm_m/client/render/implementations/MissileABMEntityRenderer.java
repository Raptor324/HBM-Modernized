package com.hbm_m.client.render.implementations;

import com.hbm_m.entity.missile.MissileABMEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Рендер ABM-ракеты (пока тот же упрощённый вид, что у прототипа {@link MissileTestEntityRenderer}).
 */
public class MissileABMEntityRenderer extends EntityRenderer<MissileABMEntity> {

    public MissileABMEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MissileABMEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MissileABMEntity entity) {
        //? if fabric && < 1.21.1 {
        /*return new ResourceLocation("minecraft", "textures/block/iron_block.png");
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");
        //?}
    }
}

