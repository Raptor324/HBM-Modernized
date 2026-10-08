package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderArcFurnace}: Ofen, Schmelze (heiss vollhell, gehoben nach Fuellstand) oder kalte Charge, Deckel mit
 * Ringen, Elektroden je Zustand (frisch, gluehend, abgebrannt) und schwingende Kabel waehrend des Betriebs.
 */
public class ArcFurnaceRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineArcFurnaceBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/arc_furnace.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/arc_furnace.png");
    private static final int FULL = 0xF000F0;

    public ArcFurnaceRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineArcFurnaceBlockEntity arc, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        switch (arc.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        float lift = arc.prevLid + (arc.lid - arc.prevLid) * interp;
        long time = arc.getLevel() != null ? arc.getLevel().getGameTime() : 0;

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        VertexConsumer nocull = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        MODEL.renderPart("Furnace", ps, vc, light);

        if (!arc.liquids.isEmpty()) {
            ps.pushPose();
            ps.translate(0, -1.75 + MachineArcFurnaceBlockEntity.getStackAmount(arc.liquids) * 1.75 / MachineArcFurnaceBlockEntity.maxLiquid, 0);
            MODEL.renderPart("ContentsHot", ps, nocull, FULL);
            ps.popPose();
        } else if (arc.hasMaterial) {
            MODEL.renderPart("ContentsCold", ps, vc, light);
        }

        ps.translate(0, 2 * lift, 0);
        if (arc.isProgressing) ps.translate(0, 0, Math.sin(time + interp) * 0.005);
        MODEL.renderPart("Lid", ps, vc, light);
        for (int i = 0; i < 3; i++) {
            byte e = arc.electrodes[i];
            if (e != MachineArcFurnaceBlockEntity.ELECTRODE_NONE) MODEL.renderPart("Ring" + (i + 1), ps, vc, light);
            if (e == MachineArcFurnaceBlockEntity.ELECTRODE_FRESH) MODEL.renderPart("Electrode" + (i + 1), ps, vc, light);
            if (e == MachineArcFurnaceBlockEntity.ELECTRODE_USED) MODEL.renderPart("Electrode" + (i + 1) + "Hot", ps, nocull, FULL);
            if (e == MachineArcFurnaceBlockEntity.ELECTRODE_DEPLETED) MODEL.renderPart("Electrode" + (i + 1) + "Short", ps, nocull, FULL);
        }

        double[] offsets = { 0.5, 0, -0.5 };
        for (int i = 0; i < 3; i++) {
            if (arc.electrodes[i] == MachineArcFurnaceBlockEntity.ELECTRODE_NONE) continue;
            ps.pushPose();
            ps.translate(0, 5.5, offsets[i]);
            if (arc.isProgressing) ps.mulPose(Axis.XP.rotationDegrees((float) (Math.sin((time + interp) / 2) * 30)));
            ps.translate(0, -5.5, -offsets[i]);
            MODEL.renderPart("Cable" + (i + 1), ps, vc, light);
            ps.popPose();
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineArcFurnaceBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
