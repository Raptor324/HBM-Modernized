package com.hbm_m.client.render.effect;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.entity.effect.EntityCloudSolinium;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code com.hbm.render.entity.effect.RenderCloudSolinium}: volle Kugel in 0x27FFDA, Radius = Alter,
 * darueber drei additive Huellen (je x1,025, Alpha 0,125).
 */
public class RenderCloudSolinium extends EntityRenderer<EntityCloudSolinium> {

    private static final int COLOR = 0x27FFDA;

    public RenderCloudSolinium(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityCloudSolinium cloud, float entityYaw, float interp,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float r = ((COLOR >> 16) & 0xFF) / 255F;
        float g = ((COLOR >> 8) & 0xFF) / 255F;
        float b = (COLOR & 0xFF) / 255F;

        poseStack.pushPose();

        float scale = cloud.age + interp;
        poseStack.scale(scale, scale, scale);

        // Innere Kugel: undurchsichtig, ohne Licht und Culling
        FleijaSphereMesh.renderSphere(poseStack, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.BHOLE_SPHERE), r, g, b, 1.0F);

        // Huellen: GL_SRC_ALPHA / GL_ONE
        double outerScale = 1.025;
        for (int i = 0; i < 3; i++) {
            poseStack.scale((float) outerScale, (float) outerScale, (float) outerScale);
            FleijaSphereMesh.renderSphere(poseStack, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.FLEIJA_SPHERE_ADDITIVE), r, g, b, 0.125F);
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCloudSolinium entity) {
        return null;
    }

    @Override
    public boolean shouldRender(EntityCloudSolinium entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    protected int getBlockLightLevel(EntityCloudSolinium entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }
}
