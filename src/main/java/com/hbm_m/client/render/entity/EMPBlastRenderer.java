package com.hbm_m.client.render.entity;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.effect.EntityEMPBlast;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderEMPBlast}: Ring.obj, horizontal auf {@code scale} gestreckt, unbeleuchtet, ohne Culling. */
public class EMPBlastRenderer extends EntityRenderer<EntityEMPBlast> {

    public static final SimpleObjModel RING = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/Ring.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/EMPBlast.png");

    public EMPBlastRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityEMPBlast e, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.scale(e.scale, 1F, e.scale);
        RING.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), 0xF000F0);
        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEMPBlast e) {
        return TEX;
    }
}
