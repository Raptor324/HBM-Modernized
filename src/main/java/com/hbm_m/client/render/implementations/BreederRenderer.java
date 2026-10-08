package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineBreederBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.RenderSparks;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderBreeder}: Gehaeuse, dazu drei gruen-weisse Funken in der Kammer, solange {@code progress > 0}. */
public class BreederRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineBreederBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/breeder.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/breeder.png");

    public BreederRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineBreederBlockEntity breeder, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ObjBerHelper.rotY(ps, 90);
        switch (ObjBerHelper.meta(breeder)) {
            case 2 -> ObjBerHelper.rotY(ps, 0);
            case 4 -> ObjBerHelper.rotY(ps, 90);
            case 3 -> ObjBerHelper.rotY(ps, 180);
            case 5 -> ObjBerHelper.rotY(ps, 270);
        }

        if (breeder.getProgress() > 0) {
            for (int i = 0; i < 3; i++) {
                ps.pushPose();
                // Original: glRotatef((float)(Math.PI * i), 0, 1, 0) - Grad, nicht Bogenmass
                ObjBerHelper.rotY(ps, (float) (Math.PI * i));
                RenderSparks.renderSpark(ps, buf, (int) ((System.currentTimeMillis() % 10000) / 100 + i), 0, 1.5625, 0, 0.15F, 3, 4, 0x00ff00, 0xffffff);
                ps.popPose();
            }
        }

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);
        ps.popPose();
    }
}
