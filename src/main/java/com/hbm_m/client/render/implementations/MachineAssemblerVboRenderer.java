package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.model.MachineAssemblerBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineAssemblerVboRenderer {

    private static final String BODY = "Body";

    private final MachineAssemblerBakedModel model;

    public MachineAssemblerVboRenderer(MachineAssemblerBakedModel model) {
        this.model = model;
    }

    public void renderStaticBody(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                 @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(BODY);
        if (part != null) {
            var r = MeshRenderCache.getOrCreateRenderer("assembler_legacy_" + BODY, part);
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
            var r = MeshRenderCache.getOrCreateRenderer("assembler_legacy_" + partName, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            poseStack.popPose();
        }
    }
}

