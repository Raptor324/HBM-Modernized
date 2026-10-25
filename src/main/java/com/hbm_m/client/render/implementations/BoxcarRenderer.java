package com.hbm_m.client.render.implementations;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.projectile.EntityBoxcar;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderBoxcar} (Zweig EntityBoxcar): {@code boxcar.obj} aufrecht stehend, 1.5 nach hinten versetzt. */
public class BoxcarRenderer extends EntityRenderer<EntityBoxcar> {

    public static final SimpleObjModel BOXCAR = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/boxcar.obj"));
    public static final ResourceLocation BOXCAR_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/boxcar.png");

    public BoxcarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityBoxcar entity, float yaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 0, -1.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        BOXCAR.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(BOXCAR_TEX)), light);
        pose.popPose();
        super.render(entity, yaw, interp, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBoxcar entity) {
        return BOXCAR_TEX;
    }
}
