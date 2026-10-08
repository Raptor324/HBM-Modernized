package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineArcWelderBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolderingStationBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code RenderArcWelder} und {@code RenderSolderingStation}: Tisch, dazu das Rezeptergebnis ({@code display})
 * flach auf der Arbeitsflaeche, 1.5-fach im Rahmenmodus.
 */
public final class WorkbenchMachineRenderers {

    private WorkbenchMachineRenderers() {}

    static final SimpleObjModel ARC_WELDER = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/arc_welder.obj"));
    static final ResourceLocation ARC_WELDER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/arc_welder.png");
    static final SimpleObjModel SOLDERING = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/soldering_station.obj"));
    static final ResourceLocation SOLDERING_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/soldering_station.png");

    static void orient(PoseStack ps, BlockEntity be) {
        ps.translate(0.5, 0, 0.5);
        switch (ObjBerHelper.meta(be)) {
            case 2 -> ObjBerHelper.rotY(ps, 90);
            case 4 -> ObjBerHelper.rotY(ps, 180);
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
        }
    }

    static void renderDisplay(PoseStack ps, MultiBufferSource buf, ItemStack display, int light, BlockEntity be) {
        if (display.isEmpty()) return;
        ps.pushPose();
        ps.translate(0.0625D * 2.5D, 1.125D, 0D);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        ps.scale(1.5F, 1.5F, 1.5F);
        ObjBerHelper.renderDecoItemInFrame(ps, buf, display, light, be.getLevel());
        ps.popPose();
    }

    public static class ArcWelder implements com.hbm_m.client.render.HbmBerBounds<MachineArcWelderBlockEntity> {
        public ArcWelder(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineArcWelderBlockEntity welder, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, welder);
            ps.translate(-0.5, 0, 0);
            ARC_WELDER.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ARC_WELDER_TEX)), light);
            renderDisplay(ps, buf, welder.display, light, welder);
            ps.popPose();
        }
    }

    public static class SolderingStation implements com.hbm_m.client.render.HbmBerBounds<MachineSolderingStationBlockEntity> {
        public SolderingStation(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineSolderingStationBlockEntity solderer, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, solderer);
            ps.translate(-0.5, 0, 0.5);
            SOLDERING.renderAll(ps, buf.getBuffer(RenderType.entityCutout(SOLDERING_TEX)), light);
            renderDisplay(ps, buf, solderer.display, light, solderer);
            ps.popPose();
        }
    }
}
