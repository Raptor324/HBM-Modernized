package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.MachineRadarBakedModel;
import com.hbm_m.client.render.MeshRenderCache;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * VBO single-draw fallback для радара (основание и тарелка).
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineRadarVboRenderer {

    public static final String SMALL_BASE_CACHE_KEY = "radar:base";
    public static final String SMALL_DISH_CACHE_KEY = "radar:dish";
    public static final String LARGE_BASE_CACHE_KEY = "radar:large_base";
    public static final String LARGE_DISH_CACHE_KEY = "radar:large_dish";

    public static String partCacheKey(MachineRadarBakedModel model, String partName) {
        String prefix = model.isLargeRadar() ? "radar_large" : "radar";
        return prefix + ":" + partName;
    }

    public void renderStatic(PoseStack poseStack, int packedLight, BlockPos blockPos,
                             @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                             MachineRadarBakedModel model) {
        renderPart(poseStack, packedLight, blockPos, blockEntity, bufferSource, model, model.getStaticPartName());
    }

    public void renderDish(PoseStack poseStack, int packedLight, BlockPos blockPos,
                           @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                           MachineRadarBakedModel model) {
        renderPart(poseStack, packedLight, blockPos, blockEntity, bufferSource, model, "Dish");
    }

    private void renderPart(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            MachineRadarBakedModel model, String partName) {
        BakedModel part = model.getPart(partName);
        if (part == null) {
            return;
        }
        var geo = MeshRenderCache.getOrCompilePartGeometry(partCacheKey(model, partName), part);
        if (geo.isEmpty()) {
            return;
        }
        var renderer = MeshRenderCache.getOrCreateRendererFromQuadList(partCacheKey(model, partName), geo.solidQuads());
        if (renderer != null) {
            renderer.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }
}
