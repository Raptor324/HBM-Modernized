package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.MachineCoolingTowerBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MachineCoolingTowerVboRenderer {
    private static final String MAIN_PART = "Cube_Cube.001";

    private final MachineCoolingTowerBakedModel model;

    public MachineCoolingTowerVboRenderer(MachineCoolingTowerBakedModel model) {
        this.model = model;
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos,
                       @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(MAIN_PART);
        if (part != null) {
            var r = MeshRenderCache.getOrCreateRenderer("cooling_tower_" + MAIN_PART, part);
            if (r != null) {
                r.setUseSlicedLight(true);
                r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            }
        }
    }
}

