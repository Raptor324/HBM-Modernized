package com.hbm_m.client.render.projectile;

import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.entity.projectile.EntityArtilleryShell;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1:1 {@code RenderArtilleryShell}: der "Grenade"-Teil aus projectiles.obj, 2,5/5/2,5-fach skaliert. */
public class RenderArtilleryShell extends EntityRenderer<EntityArtilleryShell> {

    public RenderArtilleryShell(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityArtilleryShell shell, float entityYaw, float f1, PoseStack ps, MultiBufferSource buffer, int light) {

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(f1, shell.yRotO, shell.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(f1, shell.xRotO, shell.getXRot()) - 90.0F));

        float scale = 5F;
        ps.scale(scale * 0.5F, scale, scale * 0.5F);

        WeaponResources.projectiles.renderPart("Grenade", ps, buffer.getBuffer(RenderType.entityCutout(WeaponResources.grenade_tex)), light);

        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityArtilleryShell entity) {
        return WeaponResources.grenade_tex;
    }
}
