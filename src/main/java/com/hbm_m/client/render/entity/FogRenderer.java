package com.hbm_m.client.render.entity;

import java.util.Random;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.entity.effect.EntityFogFX;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code FogRenderer}: 25 zur Kamera gedrehte Nebeltafeln (Skalierung 7.5, fester Zufall 50) in gelbgruen
 * (0.85, 0.9, 0.5), Deckkraft {@code sin(alter * PI / 400) * 0.25}; normale Alphamischung, ohne Licht und ohne
 * Tiefenschreiben.
 */
public class FogRenderer extends EntityRenderer<EntityFogFX> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/fog.png");

    public FogRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityFogFX particle, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.scale(7.5F, 7.5F, 7.5F);

        float alpha = (float) Math.sin(particle.particleAge * Math.PI / (400F)) * 0.25F;

        // entityTranslucentEmissive: Alphamischung, kein Tiefenschreiben, ohne Lichtberechnung
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        Random rand = new Random(50);

        for (int i = 0; i < 25; i++) {

            double dX = (rand.nextGaussian() - 1D) * 0.5D;
            double dY = (rand.nextGaussian() - 1D) * 0.15D;
            double dZ = (rand.nextGaussian() - 1D) * 0.5D;
            float size = (float) (rand.nextDouble() * 0.5D + 0.25D);

            ps.pushPose();
            ps.translate(dX, dY, dZ);
            ps.mulPose(entityRenderDispatcher.cameraOrientation());
            ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            ps.scale(size, size, size);

            Matrix4f m = ps.last().pose();
            Matrix3f n = ps.last().normal();
            vertex(vc, m, n, -1, -1, 1, 0, alpha);
            vertex(vc, m, n, -1, 1, 0, 0, alpha);
            vertex(vc, m, n, 1, 1, 0, 1, alpha);
            vertex(vc, m, n, 1, -1, 1, 1, alpha);
            ps.popPose();
        }

        ps.popPose();
        super.render(particle, yaw, partialTicks, ps, buf, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, float alpha) {
        vc.vertex(m, x, y, 0).color(0.85F, 0.9F, 0.5F, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFogFX entity) {
        return TEXTURE;
    }
}
