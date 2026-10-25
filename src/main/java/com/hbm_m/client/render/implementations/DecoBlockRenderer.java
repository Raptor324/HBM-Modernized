package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.DecoModelTEBlock;
import com.hbm_m.blockentity.decorations.DecoBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code RenderDecoBlock} fuer {@code boxcar} und {@code boat} (gleiche Matrizenfolge wie das Original). */
public class DecoBlockRenderer implements com.hbm_m.client.render.HbmBerBounds<DecoBlockEntity> {

    public static final SimpleObjModel BOXCAR = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/boxcar.obj"));
    public static final ResourceLocation BOXCAR_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/boxcar.png");
    public static final SimpleObjModel DUCHESS = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/duchessgambit.obj"));
    public static final ResourceLocation DUCHESS_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/duchessgambit.png");

    public DecoBlockRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(DecoBlockEntity tile, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof DecoModelTEBlock block)) return;

        ps.pushPose();
        ps.translate(0.5D, 1.5D, 0.5D);
        ps.mulPose(Axis.ZP.rotationDegrees(180));

        if (block.kind == DecoModelTEBlock.Kind.BOXCAR) {
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.translate(0, -1.5F, 0);
            switch (state.getValue(DecoModelTEBlock.FACING)) {
                case WEST -> { }
                case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
                case EAST -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
                default -> {
                    ps.mulPose(Axis.ZP.rotationDegrees(180));
                    ps.mulPose(Axis.XP.rotationDegrees(90));
                    ps.translate(0, -1.5F, 0);
                }
            }
            BOXCAR.renderAll(ps, buffers.getBuffer(RenderType.entityCutout(BOXCAR_TEX)), light);
        } else {
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.translate(0, 0, -1.5F);
            ps.translate(0, 0.5F, 0);
            DUCHESS.renderAll(ps, buffers.getBuffer(RenderType.entityCutout(DUCHESS_TEX)), light);
        }

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(DecoBlockEntity tile) {
        return true;
    }
}
