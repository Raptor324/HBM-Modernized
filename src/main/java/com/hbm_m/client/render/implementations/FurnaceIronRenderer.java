package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineFurnaceIronBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderFurnaceIron}: Gehaeuse, dazu {@code On} vollhell ohne Culling, solange {@code wasOn}, sonst {@code Off}. */
public class FurnaceIronRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFurnaceIronBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/furnace_iron.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/furnace_iron.png");

    public FurnaceIronRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineFurnaceIronBlockEntity furnace, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        switch (ObjBerHelper.meta(furnace)) {
            case 3 -> ObjBerHelper.rotY(ps, 0);
            case 5 -> ObjBerHelper.rotY(ps, 90);
            case 2 -> ObjBerHelper.rotY(ps, 180);
            case 4 -> ObjBerHelper.rotY(ps, 270);
        }
        ps.translate(-0.5D, 0, -0.5D);

        MODEL.renderPart("Main", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        if (furnace.wasOn) {
            MODEL.renderPart("On", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), LightTexture.FULL_BRIGHT);
        } else {
            MODEL.renderPart("Off", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineFurnaceIronBlockEntity be) { return true; }
}
