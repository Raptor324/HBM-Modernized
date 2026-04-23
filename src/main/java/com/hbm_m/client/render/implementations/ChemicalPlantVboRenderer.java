package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.model.ChemicalPlantBakedModel;
import com.hbm_m.client.render.GlobalMeshCache;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChemicalPlantVboRenderer {

    private static final String BASE = "Base";
    private static final String FRAME = "Frame";

    private final ChemicalPlantBakedModel model;

    public ChemicalPlantVboRenderer(ChemicalPlantBakedModel model) {
        this.model = model;
    }

    public void renderStaticBase(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(BASE);
        if (part != null) {
            var r = GlobalMeshCache.getOrCreateRenderer("chemplant_" + BASE, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    public void renderStaticFrame(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                 @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(FRAME);
        if (part != null) {
            var r = GlobalMeshCache.getOrCreateRenderer("chemplant_" + FRAME, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    public void renderAnimatedPart(PoseStack poseStack, int packedLight, String partName,
                                   Matrix4f transform, BlockPos blockPos,
                                   @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(partName);
        if (part != null) {
            poseStack.pushPose();
            if (transform != null) {
                poseStack.last().pose().mul(transform);
            }
            var r = GlobalMeshCache.getOrCreateRenderer("chemplant_" + partName, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            poseStack.popPose();
        }
    }
}
