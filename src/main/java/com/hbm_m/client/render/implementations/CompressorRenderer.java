package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineCompressorBlockEntity;
import com.hbm_m.blockentity.machines.MachineCompressorCompactBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code RenderCompressor} und {@code RenderCompressorCompact}. */
public final class CompressorRenderer {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/compressor.obj"));
    public static final SimpleObjModel CONDENSER = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/condenser.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/compressor.png");
    public static final ResourceLocation TEX_COMPACT = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/compressor_compact.png");

    private CompressorRenderer() {}

    /** Original: Metadaten 3/5/2/4 -> 270/0/90/180 Grad (bei beiden gleich). */
    private static void rotate(PoseStack ps, BlockState state) {
        Direction dir = state.getValue(DummyableMachineBlock.FACING);
        switch (dir) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }
    }

    public static class Large implements com.hbm_m.client.render.HbmBerBounds<MachineCompressorBlockEntity> {

        public Large(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineCompressorBlockEntity compressor, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            ps.translate(0.5D, 0D, 0.5D);
            rotate(ps, compressor.getBlockState());

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
            MODEL.renderPart("Compressor", ps, vc, light);

            float lift = compressor.prevPiston + (compressor.piston - compressor.prevPiston) * interp;
            float fan = compressor.prevFanSpin + (compressor.fanSpin - compressor.prevFanSpin) * interp;

            ps.pushPose();
            ps.translate(0, lift * 3 - 3, 0);
            MODEL.renderPart("Pump", ps, vc, light);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, 1.5, 0);
            ps.mulPose(Axis.XP.rotationDegrees(fan));
            ps.translate(0, -1.5, 0);
            MODEL.renderPart("Fan", ps, vc, light);
            ps.popPose();

            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(MachineCompressorBlockEntity te) { return true; }
        @Override public int getViewDistance() { return 256; }
    }

    public static class Compact implements com.hbm_m.client.render.HbmBerBounds<MachineCompressorCompactBlockEntity> {

        public Compact(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineCompressorCompactBlockEntity compressor, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            ps.translate(0.5D, 0D, 0.5D);
            rotate(ps, compressor.getBlockState());

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX_COMPACT));
            CONDENSER.renderPart("Condenser", ps, vc, light);

            float rot = compressor.prevFanSpin + (compressor.fanSpin - compressor.prevFanSpin) * f;

            ps.pushPose();
            ps.translate(0, 1.5, 0);
            ps.mulPose(Axis.XP.rotationDegrees(rot));
            ps.translate(0, -1.5, 0);
            CONDENSER.renderPart("Fan1", ps, vc, light);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, 1.5, 0);
            ps.mulPose(Axis.XN.rotationDegrees(rot));
            ps.translate(0, -1.5, 0);
            CONDENSER.renderPart("Fan2", ps, vc, light);
            ps.popPose();

            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(MachineCompressorCompactBlockEntity te) { return true; }
        @Override public int getViewDistance() { return 256; }
    }
}
