package com.hbm_m.client.render;

import org.joml.Matrix4f;

import com.hbm_m.client.model.PressBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MachinePressVboRenderer {

    private static final String HEAD = "Head";

    private final PressBakedModel model;

    public MachinePressVboRenderer(PressBakedModel model) {
        this.model = model;
    }

    public void renderAnimatedHead(PoseStack poseStack, int packedLight, Matrix4f transform,
                                   BlockPos blockPos, BlockEntity blockEntity) {
        renderAnimatedHead(poseStack, packedLight, transform, blockPos, blockEntity, null);
    }

    public void renderAnimatedHead(PoseStack poseStack, int packedLight, Matrix4f transform,
                                   BlockPos blockPos, BlockEntity blockEntity,
                                   @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(HEAD);
        if (part == null) return;
        poseStack.pushPose();
        if (transform != null) {
            poseStack.last().pose().mul(transform);
        }
        var r = GlobalMeshCache.getOrCreateRenderer("press_" + HEAD, part);
        if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        poseStack.popPose();
    }
}

