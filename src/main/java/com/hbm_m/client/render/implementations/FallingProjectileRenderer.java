package com.hbm_m.client.render.implementations;

import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.entity.projectile.EntityBuilding;
import com.hbm_m.entity.projectile.EntityDuchessGambit;
import com.hbm_m.entity.projectile.EntityTorpedo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 1:1 {@code RenderBoxcar}, Zweige EntityDuchessGambit, EntityBuilding und EntityTorpedo (der Boxcar-Zweig liegt in
 * {@link BoxcarRenderer}). Boot 1 nach hinten versetzt, Gebaeude ohne Rueckseitenausblendung, Torpedo kippt mit
 * 3 Grad pro Tick bis 85 Grad nach vorn.
 */
public class FallingProjectileRenderer<T extends Entity> extends EntityRenderer<T> {

    public FallingProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float yaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();

        if (entity instanceof EntityDuchessGambit) {
            pose.translate(0, 0, -1.0F);
            WeaponResources.duchessgambit.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(WeaponResources.duchessgambit_tex)), light);
        }

        if (entity instanceof EntityBuilding) {
            // glDisable(GL_CULL_FACE)
            WeaponResources.building.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(WeaponResources.building_tex)), light);
        }

        if (entity instanceof EntityTorpedo) {
            float f = entity.tickCount + interp;
            pose.mulPose(Axis.XP.rotationDegrees(Math.min(85, f * 3)));
            WeaponResources.torpedo.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(WeaponResources.torpedo_tex)), light);
        }

        pose.popPose();
        super.render(entity, yaw, interp, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return BoxcarRenderer.BOXCAR_TEX;
    }
}
