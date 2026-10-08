package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderMicrowave}: Gehaeuse und Fenster fest, der Drehteller laeuft mit {@code speed}, solange {@code time > 0}. */
public class MicrowaveRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineMicrowaveBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/microwave.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/microwave.png");

    public MicrowaveRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineMicrowaveBlockEntity mic, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, -0.785D, 0.5D);
        switch (ObjBerHelper.meta(mic)) {
            case 2 -> ObjBerHelper.rotY(ps, 0);
            case 4 -> ObjBerHelper.rotY(ps, 90);
            case 3 -> ObjBerHelper.rotY(ps, 180);
            case 5 -> ObjBerHelper.rotY(ps, 270);
        }
        ps.translate(-0.5D, 0.0D, 0.65D);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("mainbody_Cube.001", ps, vc, light);
        MODEL.renderPart("window_Cube.002", ps, vc, light);

        double rot = (System.currentTimeMillis() * mic.speed / 10D) % 360;
        if (mic.time > 0) {
            ps.translate(0.575D, 0.0D, -0.45D);
            ps.mulPose(Axis.YP.rotationDegrees((float) rot));
            ps.translate(-0.575D, 0.0D, 0.45D);
        }
        MODEL.renderPart("plate_Cylinder", ps, vc, light);

        ps.popPose();
    }
}
