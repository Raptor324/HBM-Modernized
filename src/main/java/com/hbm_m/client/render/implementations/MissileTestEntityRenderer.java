package com.hbm_m.client.render.implementations;

import com.hbm_m.entity.missile.MissileTestEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Временный рендерер прототипа ракеты.
 *
 * Пока просто вращает entity без OBJ‑модели. Позже будет заменён
 * на полноценный OBJ‑рендер с missile_micro.obj.
 */
public class MissileTestEntityRenderer extends EntityRenderer<MissileTestEntity> {

    public MissileTestEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MissileTestEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Пока никакого сложного рендера — только ориентируем по yaw/pitch,
        // чтобы не было крашей и entity корректно отслеживалась.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MissileTestEntity entity) {
        // Текстура не используется до добавления модели
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");
    }
}

