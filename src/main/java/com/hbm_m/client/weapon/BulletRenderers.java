package com.hbm_m.client.weapon;

import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1:1 {@code RenderBulletMK4} und {@code RenderBeam}: Ausrichtung nach Flugrichtung, dann das Lambda der BulletConfig. */
public final class BulletRenderers {

    private BulletRenderers() { }

    public static class MK4 extends EntityRenderer<EntityBulletBaseMK4> {

        public MK4(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public void render(EntityBulletBaseMK4 bullet, float yaw, float interp, PoseStack ps, MultiBufferSource buffers, int light) {
            if (bullet.config == null) bullet.config = bullet.getBulletConfig();
            if (bullet.config == null) return;

            ps.pushPose();
            if (bullet.config.renderRotations) {
                ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(interp, bullet.yRotO, bullet.getYRot()) - 90.0F));
                ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(interp, bullet.xRotO, bullet.getXRot()) + 180));
            }
            if (bullet.config.renderer != null) {
                GunGL.begin(ps, buffers, light);
                bullet.config.renderer.accept(bullet, interp);
                GunGL.end();
            }
            ps.popPose();
        }

        @Override public boolean shouldRender(EntityBulletBaseMK4 e, Frustum f, double x, double y, double z) { return true; }
        @Override public ResourceLocation getTextureLocation(EntityBulletBaseMK4 e) { return TextureAtlas.LOCATION_BLOCKS; }
    }

    public static class Beam extends EntityRenderer<EntityBulletBeamBase> {

        public Beam(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public void render(EntityBulletBeamBase bullet, float yaw, float interp, PoseStack ps, MultiBufferSource buffers, int light) {
            if (bullet.config == null) bullet.config = bullet.getBulletConfig();
            if (bullet.config == null) return;

            ps.pushPose();
            if (bullet.config.renderRotations) {
                ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(interp, bullet.yRotO, bullet.getYRot()) - 90.0F));
                ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(interp, bullet.xRotO, bullet.getXRot()) + 180));
            }
            if (bullet.config.rendererBeam != null) {
                GunGL.begin(ps, buffers, light);
                bullet.config.rendererBeam.accept(bullet, interp);
                GunGL.end();
            }
            ps.popPose();
        }

        @Override public boolean shouldRender(EntityBulletBeamBase e, Frustum f, double x, double y, double z) { return true; }
        @Override public ResourceLocation getTextureLocation(EntityBulletBeamBase e) { return TextureAtlas.LOCATION_BLOCKS; }
    }
}
