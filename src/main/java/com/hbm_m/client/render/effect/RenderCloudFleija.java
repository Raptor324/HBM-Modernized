package com.hbm_m.client.render.effect;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.entity.effect.EntityCloudFleija;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
/**
 * Порт {@code com.hbm.render.entity.effect.RenderCloudFleija} — сфера без текстуры, additive blend.
 */
public class RenderCloudFleija extends EntityRenderer<EntityCloudFleija> {

    public RenderCloudFleija(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityCloudFleija cloud, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        double baseScale = (cloud.age + partialTick) * 2.0;
        int maxAge = Math.max(1, cloud.getMaxAge());
        double ageScale = baseScale / maxAge;

        VertexConsumer innerConsumer = bufferSource.getBuffer(ClientRenderHandler.CustomRenderTypes.FLEIJA_SPHERE);
        VertexConsumer additiveConsumer = bufferSource.getBuffer(ClientRenderHandler.CustomRenderTypes.FLEIJA_SPHERE_ADDITIVE);

        poseStack.pushPose();

        // Внутренняя сфера (0, 1, 1)
        poseStack.pushPose();
        double scale = ageScale * 1.2;
        if (scale > 1.0) {
            scale = Math.max(1.0 - (scale - 1.0) * 5.0, 0.0);
        }
        scale *= 2.0 * baseScale;
        poseStack.scale((float) scale, (float) scale, (float) scale);
        FleijaSphereMesh.renderSphere(poseStack, innerConsumer, 0.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Внешние слои (0, 0.125, 0.125), additive
        poseStack.pushPose();
        scale = ageScale * 1.2;
        if (scale > 1.0) {
            scale = Math.max(1.0 - (scale - 1.0) * 5.0, 0.0);
        }
        scale *= 2.0 * baseScale;
        poseStack.scale((float) scale, (float) scale, (float) scale);
        for (int i = 0; i < 3; i++) {
            poseStack.scale(1.05F, 1.05F, 1.05F);
            FleijaSphereMesh.renderSphere(poseStack, additiveConsumer, 0.0F, 0.125F, 0.125F, 1.0F);
        }
        poseStack.popPose();

        // Ударная волна
        poseStack.pushPose();
        float shockwave = (float) (5.0 * baseScale);
        poseStack.scale(shockwave, shockwave, shockwave);
        float shockTint = (1.0F - (float) ageScale) * 0.75F;
        FleijaSphereMesh.renderSphere(poseStack, additiveConsumer, shockTint, shockTint, shockTint, 1.0F);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCloudFleija entity) {
        return null;
    }

    @Override
    public boolean shouldRender(EntityCloudFleija entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    protected int getBlockLightLevel(EntityCloudFleija entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }
}
