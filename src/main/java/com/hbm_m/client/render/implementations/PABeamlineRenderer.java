package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.albion.PABeamlineBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderPABeamline}: ohne Fenster das geschlossene Rohr, mit Fenster Rahmen plus Glas, das beim Durchflug
 * aufleuchtet ({@code 0.9/0.9/1.0 * flash}, ohne Textur, vollhell).
 */
public class PABeamlineRenderer implements com.hbm_m.client.render.HbmBerBounds<PABeamlineBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/beamline.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/beamline.png");

    public PABeamlineRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(PABeamlineBlockEntity beamline, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        switch (ObjBerHelper.meta(beamline)) {
            case 4 -> ObjBerHelper.rotY(ps, 180);
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
            case 2 -> ObjBerHelper.rotY(ps, 90);
        }

        if (!beamline.hasWindow()) {
            MODEL.renderPart("Beamline", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
        } else {
            MODEL.renderPart("BeamlineWindow", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
            float flash = beamline.prevLight + (beamline.light - beamline.prevLight) * f;
            MODEL.renderPartColor("BeamlineGlass", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                    Math.min(1F, 0.9F * flash), Math.min(1F, 0.9F * flash), Math.min(1F, 1.0F * flash), 1F);
        }

        ps.popPose();
    }
}
