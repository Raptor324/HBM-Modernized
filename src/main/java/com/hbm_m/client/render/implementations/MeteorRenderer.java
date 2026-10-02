package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.entity.projectile.EntityMeteor;
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
 * 1:1 {@code RenderMeteor}: ein 5x5x5 grosser Wuerfel mit der Textur des geschmolzenen Meteorgesteins (das Original
 * bindet die Datei direkt, also den ganzen Animationsstreifen), um die Achse (1,1,1) rotierend, voll beleuchtet.
 */
public class MeteorRenderer extends EntityRenderer<EntityMeteor> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/ported/block_meteor_molten.png");

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityMeteor meteor, float yaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(new org.joml.Quaternionf().rotateAxis((float) Math.toRadians((meteor.tickCount % 360 + interp) * 10), 0.57735026F, 0.57735026F, 0.57735026F));
        pose.scale(5.0F, 5.0F, 5.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(180));

        VertexConsumer t = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f m = pose.last().pose();
        // Wie im Original die ganze Datei (Animationsstreifen aus 3 Bildern) auf jede Seite
        float vMax = 1F;
        int l = 0xF000F0;

        v(t, m, -0.5F, -0.5F, -0.5F, 1, 0, l); v(t, m, +0.5F, -0.5F, -0.5F, 0, 0, l); v(t, m, +0.5F, +0.5F, -0.5F, 0, vMax, l); v(t, m, -0.5F, +0.5F, -0.5F, 1, vMax, l);
        v(t, m, -0.5F, -0.5F, +0.5F, 1, 0, l); v(t, m, -0.5F, -0.5F, -0.5F, 0, 0, l); v(t, m, -0.5F, +0.5F, -0.5F, 0, vMax, l); v(t, m, -0.5F, +0.5F, +0.5F, 1, vMax, l);
        v(t, m, +0.5F, -0.5F, +0.5F, 1, 0, l); v(t, m, -0.5F, -0.5F, +0.5F, 0, 0, l); v(t, m, -0.5F, +0.5F, +0.5F, 0, vMax, l); v(t, m, +0.5F, +0.5F, +0.5F, 1, vMax, l);
        v(t, m, +0.5F, -0.5F, -0.5F, 1, 0, l); v(t, m, +0.5F, -0.5F, +0.5F, 0, 0, l); v(t, m, +0.5F, +0.5F, +0.5F, 0, vMax, l); v(t, m, +0.5F, +0.5F, -0.5F, 1, vMax, l);
        v(t, m, -0.5F, -0.5F, +0.5F, 1, 0, l); v(t, m, +0.5F, -0.5F, +0.5F, 0, 0, l); v(t, m, +0.5F, -0.5F, -0.5F, 0, vMax, l); v(t, m, -0.5F, -0.5F, -0.5F, 1, vMax, l);
        v(t, m, +0.5F, +0.5F, +0.5F, 1, 0, l); v(t, m, -0.5F, +0.5F, +0.5F, 0, 0, l); v(t, m, -0.5F, +0.5F, -0.5F, 0, vMax, l); v(t, m, +0.5F, +0.5F, -0.5F, 1, vMax, l);

        pose.popPose();
        super.render(meteor, yaw, interp, pose, buffers, light);
    }

    private static void v(VertexConsumer t, Matrix4f m, float x, float y, float z, float u, float v, int light) {
        t.vertex(m, x, y, z).color(1F, 1F, 1F, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMeteor meteor) {
        return TEXTURE;
    }
}
