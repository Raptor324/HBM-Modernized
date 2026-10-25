package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.entity.grenade.EntityGrenadeUniversal;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code RenderGrenadeUniversal}: dreht die Granate im Flug, liegt sie, kippt sie zur Seite. */
public class RenderGrenadeUniversal extends EntityRenderer<EntityGrenadeUniversal> {

    public RenderGrenadeUniversal(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityGrenadeUniversal grenade, float f0, float interp, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        GunGL.begin(ps, buffers, light);
        GunGL.enableLighting();
        GunGL.enableCull();

        double scale = 0.0625D;
        GunGL.scale(scale, scale, scale);
        double yaw = grenade.yRotO + (grenade.getYRot() - grenade.yRotO) * interp;
        GunGL.rotate(yaw, 0, 1, 0);

        double spin = grenade.prevSpin + (grenade.spin - grenade.prevSpin) * interp;
        GunGL.rotate(spin, 1, 0, 0);

        if (grenade.getBounces() > 0) {
            GunGL.rotate(-80, 0, 0, 1);
        }

        ItemStack stack = grenade.getGrenadeItem();
        ItemRenderGrenade.renderGrenade(stack, null);

        GunGL.end();
        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityGrenadeUniversal entity) {
        return WeaponResources.grenade_frag_tex;
    }
}
