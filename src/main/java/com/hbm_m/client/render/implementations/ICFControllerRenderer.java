package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.icf.ICFControllerBlockEntity;
import com.hbm_m.client.render.util.BeamPronter;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderICFController}: der Block ist das Blockmodell, hier nur der dunkle Spiralstrahl ueber {@code laserLength}. */
public class ICFControllerRenderer implements com.hbm_m.client.render.HbmBerBounds<ICFControllerBlockEntity> {

    public ICFControllerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(ICFControllerBlockEntity controller, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (controller.getLaserLength() <= 0) return;

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ObjBerHelper.rotY(ps, 90);
        switch (ObjBerHelper.meta(controller)) {
            case 4 -> ObjBerHelper.rotY(ps, 90);
            case 3 -> ObjBerHelper.rotY(ps, 180);
            case 5 -> ObjBerHelper.rotY(ps, 270);
            case 2 -> ObjBerHelper.rotY(ps, 0);
        }
        BeamPronter.prontBeam(ps, buf, new Vec3(controller.getLaserLength(), 0, 0), BeamPronter.EnumWaveType.SPIRAL,
                BeamPronter.EnumBeamType.SOLID, 0x202020, 0x100000, 0, 1, 0F, 10, 0.125F);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(ICFControllerBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
