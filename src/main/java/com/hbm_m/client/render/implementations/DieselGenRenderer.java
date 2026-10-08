package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderDieselGen}: der Motorblock ruettelt (sin der Systemzeit, 0.005), solange passender Treibstoff im Tank ist. */
public class DieselGenRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineDieselGeneratorBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/dieselgen.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/dieselgen.png");

    public DieselGenRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineDieselGeneratorBlockEntity engine, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        switch (ObjBerHelper.meta(engine)) {
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
            case 2 -> ObjBerHelper.rotY(ps, 90);
            case 4 -> ObjBerHelper.rotY(ps, 180);
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Generator", ps, vc, light);

        if (engine.hasAcceptableFuel() && engine.getTank().getFill() > 0) {
            double swingSide = Math.sin(System.currentTimeMillis() / 50D) * 0.005;
            double swingFront = Math.sin(System.currentTimeMillis() / 25D) * 0.005;
            ps.translate(swingFront, 0, swingSide);
        }
        MODEL.renderPart("Engine", ps, vc, light);

        ps.popPose();
    }
}
