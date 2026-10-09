package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.FileCabinetBlock;
import com.hbm_m.blockentity.decorations.FileCabinetBlockEntity;
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

/** 1:1 {@code RenderFileCabinet}: Korpus, untere und obere Schublade je um 0.6875 * Auszug nach vorn. */
public class FileCabinetRenderer implements com.hbm_m.client.render.HbmBerBounds<FileCabinetBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/file_cabinet.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/file_cabinet.png");
    public static final ResourceLocation TEX_STEEL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/file_cabinet_steel.png");

    public FileCabinetRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(FileCabinetBlockEntity cabinet, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState state = cabinet.getBlockState();
        if (!(state.getBlock() instanceof FileCabinetBlock block)) return;

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        // BlockDecoModel: Meta>>2 0 Nord 180, 1 Sued 0, 2 West 270, 3 Ost 90
        switch (state.getValue(FileCabinetBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        var vc = buf.getBuffer(RenderType.entityCutout(block.steel ? TEX_STEEL : TEX));
        MODEL.renderPart("Cabinet", ps, vc, light);

        ps.pushPose();
        float lower = cabinet.prevLowerExtent + (cabinet.lowerExtent - cabinet.prevLowerExtent) * interp;
        ps.translate(0F, 0F, 0.6875F * lower);
        MODEL.renderPart("LowerDrawer", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        float upper = cabinet.prevUpperExtent + (cabinet.upperExtent - cabinet.prevUpperExtent) * interp;
        ps.translate(0F, 0F, 0.6875F * upper);
        MODEL.renderPart("UpperDrawer", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }
}
