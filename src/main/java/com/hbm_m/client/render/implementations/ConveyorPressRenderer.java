package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineConveyorPressBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderConveyorPress}: Gestell, Kolben nur mit eingesetztem Stempel ({@code syncStack}), Band mit
 * laufender Textur ({@code (worldTime % 16 - 2) / 16} auf V).
 */
public class ConveyorPressRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineConveyorPressBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/conveyor_press.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/conveyor_press.png");
    static final ResourceLocation BELT_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/conveyor_press_belt.png");

    public ConveyorPressRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineConveyorPressBlockEntity press, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        switch (ObjBerHelper.meta(press)) {
            case 2 -> ObjBerHelper.rotY(ps, 90);
            case 4 -> ObjBerHelper.rotY(ps, 180);
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
        }

        MODEL.renderPart("Press", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        if (!press.getStampStack().isEmpty()) {
            ps.pushPose();
            // Original: lastPress + (renderPress - lastPress) * interp
            double piston = press.getRenderPress(interp);
            ps.translate(0, -piston * 0.75, 0);
            MODEL.renderPart("Piston", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
            ps.popPose();
        }

        long time = press.getLevel() != null ? press.getLevel().getGameTime() : 0L;
        int ticks = (int) (time % 16) - 2;
        MODEL.renderPart("Belt", ps, new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(BELT_TEX)), 0F, ticks / 16F), light);

        ps.popPose();
    }
}
