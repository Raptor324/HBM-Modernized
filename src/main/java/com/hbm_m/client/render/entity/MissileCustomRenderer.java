package com.hbm_m.client.render.entity;

import com.hbm_m.client.render.util.MissilePronter;
import com.hbm_m.entity.missile.EntityMissileCustom;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1:1 {@code RenderMissileCustom}: Gier/Nick wie die festen Raketen, dann {@link MissilePronter#prontMissile}. */
public class MissileCustomRenderer extends EntityRenderer<EntityMissileCustom> {

    public MissileCustomRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityMissileCustom entity, float entityYaw, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light) {
        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(pitch));
        ps.mulPose(Axis.YN.rotationDegrees(yaw - 90.0F));
        MissilePronter.prontMissile(entity.getStruct(), ps, buffers, light);
        ps.popPose();
        super.render(entity, entityYaw, partialTicks, ps, buffers, light);
    }

    @Override
    public boolean shouldRender(EntityMissileCustom entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMissileCustom entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
