package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.SoyuzRocketBlock;
import com.hbm_m.block.entity.decorations.SoyuzRocketBlockEntity;
import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

/**
 * BER for the decorative Soyuz rocket: renders the single ~52-block-tall
 * multi-material mesh via the VBO path (see {@link SoyuzRocketBakedModel}
 * for why baked-quad world rendering is skipped).
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class SoyuzRocketRenderer extends AbstractPartBasedRenderer<SoyuzRocketBlockEntity, SoyuzRocketBakedModel> {

    private static final String CACHE_PREFIX = "soyuz_rocket";

    public SoyuzRocketRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    protected SoyuzRocketBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof SoyuzRocketBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(SoyuzRocketBlockEntity be) {
        return be.getBlockState().getValue(SoyuzRocketBlock.FACING);
    }

    @Override
    protected void renderParts(SoyuzRocketBlockEntity be, SoyuzRocketBakedModel model, LegacyAnimator animator,
                                float partialTick, int packedLight, int packedOverlay,
                                PoseStack poseStack, MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(SoyuzRocketBakedModel.ROCKET);
        if (part == null) return;

        SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer(CACHE_PREFIX, SoyuzRocketBakedModel.ROCKET, part);
        if (renderer == null) return;

        renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
    }
}