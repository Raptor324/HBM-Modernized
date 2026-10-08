package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineEPressBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderEPress}: Koerper, Kopf faehrt mit {@code clamp(1 - press/maxPress) * 0.875} herab, das Material
 * ({@code syncStack}) liegt flach im Rahmenmodus unter dem Stempel.
 */
public class EPressRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineEPressBlockEntity> {

    static final SimpleObjModel BODY = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/epress_body.obj"));
    static final SimpleObjModel HEAD = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/epress_head.obj"));
    static final ResourceLocation BODY_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/epress_body.png");
    static final ResourceLocation HEAD_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/epress_head.png");

    public EPressRenderer(BlockEntityRendererProvider.Context ctx) { }

    private static void orient(PoseStack ps, MachineEPressBlockEntity press) {
        ObjBerHelper.rotY(ps, 180);
        switch (ObjBerHelper.meta(press)) {
            case 2 -> ObjBerHelper.rotY(ps, 270);
            case 4 -> ObjBerHelper.rotY(ps, 0);
            case 3 -> ObjBerHelper.rotY(ps, 90);
            case 5 -> ObjBerHelper.rotY(ps, 180);
        }
    }

    @Override
    public void render(MachineEPressBlockEntity press, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        orient(ps, press);
        BODY.renderAll(ps, buf.getBuffer(RenderType.entityCutout(BODY_TEX)), light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.5D, 1D, 0.5D);
        orient(ps, press);
        double p = (press.lastPress + (press.renderPress - press.lastPress) * f) / (double) MachineEPressBlockEntity.getMaxPress();
        ps.translate(0, Mth.clamp(1D - p, 0D, 1D) * 0.875D, 0);
        HEAD.renderAll(ps, buf.getBuffer(RenderType.entityCutout(HEAD_TEX)), light);
        ps.popPose();

        if (!press.getSyncStack().isEmpty()) {
            ps.pushPose();
            ps.translate(0.5D, 1D, 0.5D);
            orient(ps, press);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.mulPose(Axis.XP.rotationDegrees(-90));
            ps.translate(1.0F, 1.0F - 0.0625F * 165 / 100, 0.0F);
            ps.translate(-1, -1.15F, 0);
            ObjBerHelper.renderDecoItemInFrame(ps, buf, press.getSyncStack(), light, press.getLevel());
            ps.popPose();
        }
    }
}
