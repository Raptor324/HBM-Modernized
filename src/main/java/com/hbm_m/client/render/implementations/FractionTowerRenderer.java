package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineFractionTowerBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderFractionTower}: das statische Turmsegment, ohne Drehung. */
public class FractionTowerRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFractionTowerBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/fraction_tower.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/fraction_tower.png");

    public FractionTowerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineFractionTowerBlockEntity te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineFractionTowerBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
