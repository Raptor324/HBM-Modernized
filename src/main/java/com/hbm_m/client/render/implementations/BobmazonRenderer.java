package com.hbm_m.client.render.implementations;

import com.hbm_m.entity.missile.EntityBobmazon;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderMinerRocket} (Zweig EntityBobmazon): {@code minerRocket.obj} mit Bobmazon-Textur, kopfueber. */
public class BobmazonRenderer extends EntityRenderer<EntityBobmazon> {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/bobmazon.png");

    public BobmazonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityBobmazon entity, float yaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180));
        MinerRocketRenderer.MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light);
        pose.popPose();
        super.render(entity, yaw, interp, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBobmazon entity) {
        return TEXTURE;
    }
}
