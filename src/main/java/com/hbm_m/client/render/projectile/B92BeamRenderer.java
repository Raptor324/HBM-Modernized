package com.hbm_m.client.render.projectile;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.entity.projectile.EntityB92Beam;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 1:1 {@code RenderBeam5}: acht ineinanderliegende additive Vierkant-Roehren (Radius 0..0,175, Laenge 2),
 * innen weiss, aussen nach Blau auslaufend; Drehung um Gier, dann -Nick.
 */
public class B92BeamRenderer extends EntityRenderer<EntityB92Beam> {

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/PlasmaBeam.png");

    public B92BeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityB92Beam e, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buf, int light) {
        float radius = 0.175F;
        int distance = 2;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(e.getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees(-e.getXRot()));

        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
        Matrix4f m = ps.last().pose();
        boolean red = false, green = false, blue = true;

        for (float o = 0; o <= radius; o += radius / 8) {
            float color = 1f - (o * 8.333f);
            if (color < 0) color = 0;
            int r = (int) ((red ? 1 : color) * 255), g = (int) ((green ? 1 : color) * 255), b = (int) ((blue ? 1 : color) * 255);
            quad(vc, m, r, g, b, o, -o, 0, o, o, 0, o, o, distance, o, -o, distance);
            quad(vc, m, r, g, b, -o, -o, 0, o, -o, 0, o, -o, distance, -o, -o, distance);
            quad(vc, m, r, g, b, -o, o, 0, -o, -o, 0, -o, -o, distance, -o, o, distance);
            quad(vc, m, r, g, b, o, o, 0, -o, o, 0, -o, o, distance, o, o, distance);
        }
        ps.popPose();
        super.render(e, yaw, partialTicks, ps, buf, light);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int r, int g, int b,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, x0, y0, z0, r, g, b, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, x1, y1, z1, r, g, b, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, x2, y2, z2, r, g, b, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, x3, y3, z3, r, g, b, 255);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityB92Beam e) {
        return TEX;
    }
}
