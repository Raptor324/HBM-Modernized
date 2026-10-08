package com.hbm_m.client.render.entity;

import java.util.Random;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.entity.effect.EntityModFX;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code MultiCloudRenderer}: je nach Achtel der Lebenszeit eines der acht Wolkenbilder, fuenfmal leicht
 * versetzt und unterschiedlich gross als zur Kamera gedrehte Tafel (Groesse 3,75).
 */
public class MultiCloudRenderer<T extends EntityModFX> extends EntityRenderer<T> {

    private final ResourceLocation[] frames = new ResourceLocation[8];

    public MultiCloudRenderer(EntityRendererProvider.Context ctx, String name) {
        super(ctx);
        for (int i = 0; i < 8; i++) frames[i] = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/cloud/" + name + (i + 1) + ".png");
    }

    private ResourceLocation frame(EntityModFX fx) {
        int age = fx.particleAge, max = fx.maxAge;
        ResourceLocation tex = frames[0];
        for (int k = 7; k >= 1; k--) {
            int lo = max / 8 * k, hi = k == 7 ? max : max / 8 * (k + 1);
            if ((k == 7 ? age <= hi : age < hi) && age >= lo) tex = frames[k];
        }
        if (age < max / 8 && age >= 0) tex = frames[0];
        return tex;
    }

    @Override
    public void render(T fx, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buf, int light) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(frame(fx)));
        ps.pushPose();
        ps.translate(0, 0.1F, 0); // yOffset = height / 2
        ps.scale(0.5F * 7.5F, 0.5F * 7.5F, 0.5F * 7.5F);

        Random randy = new Random(fx.getId());
        Random rand = new Random(100);
        for (int i = 0; i < 5; i++) {
            float c = (float) (1 - randy.nextInt(10) * 0.05);
            double dX = (rand.nextGaussian() - 1D) * 0.15D;
            double dY = (rand.nextGaussian() - 1D) * 0.15D;
            double dZ = (rand.nextGaussian() - 1D) * 0.15D;
            float size = (float) (rand.nextDouble() * 0.5D + 0.25D);

            ps.pushPose();
            ps.translate(dX, dY, dZ);
            ps.scale(size, size, size);
            ps.mulPose(entityRenderDispatcher.cameraOrientation());
            ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            Matrix4f m = ps.last().pose();
            Matrix3f n = ps.last().normal();
            quad(vc, m, n, -0.5F, -0.25F, 0, 1, c, light);
            quad(vc, m, n, 0.5F, -0.25F, 1, 1, c, light);
            quad(vc, m, n, 0.5F, 0.75F, 1, 0, c, light);
            quad(vc, m, n, -0.5F, 0.75F, 0, 0, c, light);
            ps.popPose();
        }
        ps.popPose();
        super.render(fx, yaw, partialTicks, ps, buf, light);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, float c, int light) {
        //? if < 1.21.1 {
        vc.vertex(m, x, y, 0).color(c, c, c, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        //?} else {
        /*com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, x, y, 0).setColor(c, c, c, 1F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light), n, 0, 1, 0);
        *///?}
    }

    @Override public ResourceLocation getTextureLocation(T fx) { return frame(fx); }
}
