package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.client.model.PressBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
        var r = MeshRenderCache.getOrCreateRenderer("press_" + HEAD, part);
        if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        poseStack.popPose();
    }
}


