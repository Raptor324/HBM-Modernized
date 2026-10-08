package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.pile.PileControlBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileLoaderBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileVentBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code RenderPileControl}, {@code RenderPileLoader}, {@code RenderPileVent}. Ausrichtung wie im Original
 * {@code meta % 4} (0 Nord 90, 1 Sued 270, 2 West 180, 3 Ost 0 Grad) - FACING zeigt wie dort vom Meiler weg.
 */
public final class PileDeviceRenderers {

    private PileDeviceRenderers() {}

    static final SimpleObjModel CONTROL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/pile/pile_control.obj"));
    static final SimpleObjModel LOADER = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/pile/pile_loader.obj"));
    static final SimpleObjModel VENT = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/pile/pile_vent.obj"));
    static final ResourceLocation CONTROL_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/ported/pile_control.png");
    static final ResourceLocation LOADER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/ported/pile_loader.png");
    static final ResourceLocation VENT_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/ported/pile_vent.png");

    static void orient(PoseStack ps, BlockEntity be) {
        ps.translate(0.5, 0, 0.5);
        Direction dir = ObjBerHelper.facing(be);
        switch (dir) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }
    }

    public static class Control implements com.hbm_m.client.render.HbmBerBounds<PileControlBlockEntity> {
        public Control(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(PileControlBlockEntity control, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, control);
            double level = control.lastRenderLevel + (control.renderLevel - control.lastRenderLevel) * interp;
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(CONTROL_TEX));
            CONTROL.renderPart("Base", ps, vc, light);
            ps.translate(0, level * 0.75, 0);
            CONTROL.renderPart("Rod", ps, vc, light);
            ps.popPose();
        }
    }

    public static class Loader implements com.hbm_m.client.render.HbmBerBounds<PileLoaderBlockEntity> {
        public Loader(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(PileLoaderBlockEntity loader, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, loader);
            double position = loader.lastRenderLevel + (loader.renderLevel - loader.lastRenderLevel) * interp;
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(LOADER_TEX));
            LOADER.renderPart("Loader", ps, vc, light);

            ps.pushPose();
            ps.translate(-0.1875, 0.5, 0);
            ps.mulPose(Axis.ZP.rotationDegrees((float) (position * 90)));
            ps.translate(0.1875, -0.5, 0);
            LOADER.renderPart("Lever", ps, vc, light);
            ps.popPose();

            ps.translate(position * -0.5, 0, 0);
            LOADER.renderPart("Slider", ps, vc, light);
            if (!loader.getStack().isEmpty()) LOADER.renderPart("Rod", ps, vc, light);
            ps.popPose();
        }
    }

    public static class Vent implements com.hbm_m.client.render.HbmBerBounds<PileVentBlockEntity> {
        public Vent(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(PileVentBlockEntity vent, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, vent);
            float rot = vent.getLastFan() + (vent.getFan() - vent.getLastFan()) * interp;
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(VENT_TEX));
            VENT.renderPart("Pipe", ps, vc, light);
            ps.mulPose(Axis.YP.rotationDegrees(rot));
            VENT.renderPart("Fan", ps, vc, light);
            ps.popPose();
        }
    }
}
