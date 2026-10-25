package com.hbm_m.client.render.implementations;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.missile.EntityMinerRocket;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderMinerRocket} (Zweig EntityMinerRocket): {@code minerRocket.obj} ohne Rueckseitenausblendung. */
public class MinerRocketRenderer extends EntityRenderer<EntityMinerRocket> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/miner_rocket.obj"));
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/miner_rocket.png");

    public MinerRocketRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityMinerRocket entity, float yaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light);
        super.render(entity, yaw, interp, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMinerRocket entity) {
        return TEXTURE;
    }
}
