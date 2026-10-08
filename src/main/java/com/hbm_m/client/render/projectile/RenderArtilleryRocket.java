package com.hbm_m.client.render.projectile;

import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.entity.projectile.EntityArtilleryRocket;
import com.hbm_m.item.weapon.ItemAmmoHIMARS.HIMARSRocket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1:1 {@code RenderArtilleryRocket}: "RocketStandard"/"RocketSingle" aus turret_himars.obj mit der Typ-Textur. */
public class RenderArtilleryRocket extends EntityRenderer<EntityArtilleryRocket> {

    public RenderArtilleryRocket(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityArtilleryRocket rocket, float entityYaw, float f1, PoseStack ps, MultiBufferSource buffer, int light) {

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(f1, rocket.yRotO, rocket.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(f1, rocket.xRotO, rocket.getXRot()) - 90.0F));
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.mulPose(Axis.XP.rotationDegrees(90));

        HIMARSRocket type = rocket.getRocketType();
        var vc = buffer.getBuffer(RenderType.entityCutout(getTextureLocation(rocket)));
        if (type.modelType == 0) WeaponResources.turret_himars.renderPart("RocketStandard", ps, vc, light);
        if (type.modelType == 1) WeaponResources.turret_himars.renderPart("RocketSingle", ps, vc, light);

        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityArtilleryRocket rocket) {
        return rocket.getRocketType().texture;
    }
}
