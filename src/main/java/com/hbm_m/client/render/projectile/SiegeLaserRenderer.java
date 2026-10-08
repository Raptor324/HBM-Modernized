package com.hbm_m.client.render.projectile;

import org.joml.Matrix4f;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.entity.projectile.EntitySiegeLaser;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderSiegeLaser}: untexturierter Pfeil in der Laserfarbe, additiv (SRC_ALPHA, ONE), ohne
 * Rueckseitenausblendung und ohne Tiefenschreiben. Spitze und Mittelteil sind Dreiecke (hier als entartete
 * Vierecke gezeichnet), der Schweif blendet nach hinten auf Deckkraft 0 aus.
 */
public class SiegeLaserRenderer extends EntityRenderer<EntitySiegeLaser> {

    private static final ResourceLocation UNIVERSAL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/TheGadget3_.png");

    public SiegeLaserRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntitySiegeLaser laser, float yaw, float f1, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(f1, laser.yRotO, laser.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(f1, laser.xRotO, laser.getXRot()) + 180));

        this.renderDart(laser, ps, buffers);

        ps.popPose();
        super.render(laser, yaw, f1, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySiegeLaser entity) {
        return UNIVERSAL;
    }

    private void renderDart(EntitySiegeLaser laser, PoseStack ps, MultiBufferSource buffers) {

        ps.pushPose();
        ps.scale(1F / 4F, 1F / 8F, 1F / 8F);
        ps.scale(-1, 1, 1);
        ps.scale(2, 2, 2);

        int color = laser.getColor();
        float r = ((color >> 16) & 255) / 255F;
        float g = ((color >> 8) & 255) / 255F;
        float b = (color & 255) / 255F;

        VertexConsumer vc = buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
        Matrix4f m = ps.last().pose();

        // front
        tri(vc, m, r, g, b, 6, 0, 0, 1, 3, -1, -1, 0, 3, 1, -1, 0);
        tri(vc, m, r, g, b, 3, -1, 1, 0, 6, 0, 0, 1, 3, 1, 1, 0);
        tri(vc, m, r, g, b, 3, -1, -1, 0, 6, 0, 0, 1, 3, -1, 1, 0);
        tri(vc, m, r, g, b, 6, 0, 0, 1, 3, 1, -1, 0, 3, 1, 1, 0);

        // mid
        tri(vc, m, r, g, b, 6, 0, 0, 1, 4, -0.5F, -0.5F, 1, 4, 0.5F, -0.5F, 1);
        tri(vc, m, r, g, b, 4, -0.5F, 0.5F, 1, 6, 0, 0, 1, 4, 0.5F, 0.5F, 1);
        tri(vc, m, r, g, b, 4, -0.5F, -0.5F, 1, 6, 0, 0, 1, 4, -0.5F, 0.5F, 1);
        tri(vc, m, r, g, b, 6, 0, 0, 1, 4, 0.5F, -0.5F, 1, 4, 0.5F, 0.5F, 1);

        // tail
        quad(vc, m, r, g, b, 4, 0.5F, -0.5F, 4, 0.5F, 0.5F, 0, 0.5F, 0.5F, 0, 0.5F, -0.5F);
        quad(vc, m, r, g, b, 4, -0.5F, -0.5F, 4, -0.5F, 0.5F, 0, -0.5F, 0.5F, 0, -0.5F, -0.5F);
        quad(vc, m, r, g, b, 4, -0.5F, 0.5F, 4, 0.5F, 0.5F, 0, 0.5F, 0.5F, 0, -0.5F, 0.5F);
        quad(vc, m, r, g, b, 4, -0.5F, -0.5F, 4, 0.5F, -0.5F, 0, 0.5F, -0.5F, 0, -0.5F, -0.5F);

        ps.popPose();
    }

    /** Ein Dreieck (startDrawing(4)) als Viereck mit doppeltem letztem Eckpunkt; je Ecke Position und Deckkraft. */
    private static void tri(VertexConsumer vc, Matrix4f m, float r, float g, float b,
                            float x0, float y0, float z0, float a0,
                            float x1, float y1, float z1, float a1,
                            float x2, float y2, float z2, float a2) {
        vc.vertex(m, x0, y0, z0).color(r, g, b, a0).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, a1).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, a2).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, a2).endVertex();
    }

    /** Schweif-Viereck: die ersten beiden Ecken deckend, die letzten beiden mit Deckkraft 0. */
    private static void quad(VertexConsumer vc, Matrix4f m, float r, float g, float b,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        vc.vertex(m, x0, y0, z0).color(r, g, b, 1F).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, 1F).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, 0F).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, 0F).endVertex();
    }
}
