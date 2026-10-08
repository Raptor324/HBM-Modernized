package com.hbm_m.client.render.effect;

import com.hbm_m.client.render.implementations.SawmillRenderer;
import com.hbm_m.entity.projectile.CogEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderSawblade}: der Teil "Blade" aus {@code sawmill.obj}, nach der Ausrichtung gedreht; solange es
 * fliegt dreht es sich mit der Uhrzeit.
 */
public class SawbladeRenderer extends EntityRenderer<CogEntity> {

    public SawbladeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(CogEntity cog, float yaw, float partialTick, PoseStack ps, MultiBufferSource buffer, int light) {
        ps.pushPose();

        int orientation = cog.getOrientation();
        switch (orientation % 6) {
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(0));
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 2 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        ps.translate(0, 0, -1);

        if (orientation < 6) {
            ps.mulPose(Axis.ZN.rotationDegrees((float) (System.currentTimeMillis() % (360 * 5) / 3D)));
        }

        ps.translate(0, -1.375, 0);
        SawmillRenderer.MODEL.renderPart("Blade", ps, buffer.getBuffer(RenderType.entityCutout(getTextureLocation(cog))), light);
        ps.popPose();
        super.render(cog, yaw, partialTick, ps, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CogEntity cog) {
        return SawmillRenderer.TEX;
    }
}
