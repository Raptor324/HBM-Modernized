package com.hbm_m.client.render.projectile;

import com.hbm_m.entity.projectile.ClusterRocketEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ClusterRocketEntityRenderer extends EntityRenderer<ClusterRocketEntity> {

    public ClusterRocketEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ClusterRocketEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        ClusterSubmunitionMesh.applyEntityRotation(poseStack, yaw, pitch);
        ClusterSubmunitionMesh.applyModelTransform(poseStack);

        RenderType renderType = RenderType.entityCutoutNoCull(ClusterSubmunitionMesh.TEXTURE);
        var consumer = buffer.getBuffer(renderType);
        ClusterSubmunitionMesh.render(poseStack, consumer, packedLight, 0);

        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(renderType);
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ClusterRocketEntity entity) {
        return ClusterSubmunitionMesh.TEXTURE;
    }
}
