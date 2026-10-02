package com.hbm_m.client.render.effect;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.projectile.CogEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderCog}: der Teil "Cog" aus {@code stirling.obj}, nach der Ausrichtung gedreht; solange es fliegt
 * ({@code orientation < 6}) dreht es sich mit der Uhrzeit. Textur nach Bauart (0 normal, sonst Stahl).
 */
public class CogRenderer extends EntityRenderer<CogEntity> {

    private static final SimpleObjModel STIRLING = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/stirling.obj"));
    private static final ResourceLocation STIRLING_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/stirling.png");
    private static final ResourceLocation STIRLING_STEEL_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/stirling_steel.png");

    public CogRenderer(EntityRendererProvider.Context ctx) {
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
            ps.mulPose(Axis.ZN.rotationDegrees((float) (System.currentTimeMillis() % (360 * 3) / 3D)));
        }

        ps.translate(0, -1.375, 0);
        STIRLING.renderPart("Cog", ps, buffer.getBuffer(RenderType.entityCutout(getTextureLocation(cog))), light);
        ps.popPose();
        super.render(cog, yaw, partialTick, ps, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CogEntity cog) {
        return cog.getMeta() == 0 ? STIRLING_TEX : STIRLING_STEEL_TEX;
    }
}
