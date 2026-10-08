package com.hbm_m.client.render.effect;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.entity.effect.EntityCloudFleijaRainbow;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * 1:1 {@code RenderCloudRainbow}: Kugel mit Groesse = Alter, jedes Bild in Zufallsfarben, darueber fuenf additive
 * Schalen (0.6 bis 1.0).
 */
public class RenderCloudRainbow extends EntityRenderer<EntityCloudFleijaRainbow> {

    public RenderCloudRainbow(EntityRendererProvider.Context context) {
        super(context);
    }

    private static float c(RandomSource rand) {
        return rand.nextInt(0x100) / 255F;
    }

    @Override
    public void render(@NotNull EntityCloudFleijaRainbow cloud, float yaw, float partialTick, PoseStack ps, MultiBufferSource buffers, int light) {
        RandomSource rand = cloud.level().random;
        ps.pushPose();
        ps.scale(cloud.age, cloud.age, cloud.age);

        ps.pushPose();
        ps.scale(0.5F, 0.5F, 0.5F);
        FleijaSphereMesh.renderSphere(ps, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.FLEIJA_SPHERE), c(rand), c(rand), c(rand), 1.0F);
        ps.popPose();

        for (float i = 0.6F; i <= 1F; i += 0.1F) {
            ps.pushPose();
            ps.scale(i, i, i);
            FleijaSphereMesh.renderSphere(ps, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.FLEIJA_SPHERE_ADDITIVE), c(rand), c(rand), c(rand), 1.0F);
            ps.popPose();
        }

        ps.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityCloudFleijaRainbow entity) {
        return null;
    }

    @Override
    public boolean shouldRender(@NotNull EntityCloudFleijaRainbow entity, @NotNull Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }
}
