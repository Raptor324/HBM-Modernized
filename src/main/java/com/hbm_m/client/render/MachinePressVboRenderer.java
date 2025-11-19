package com.hbm_m.client.render;

import com.hbm_m.client.model.PressBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MachinePressVboRenderer {

    private static final String BASE = "Base";
    private static final String HEAD = "Head";

    private final PressBakedModel model;

    public MachinePressVboRenderer(PressBakedModel model) {
        this.model = model;
    }

    public void renderStaticBase(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                 BlockEntity blockEntity) {
        BakedModel part = model.getPart(BASE);
        if (part == null) {
            return;
        }
        GlobalMeshCache.getOrCreateRenderer("press_" + BASE, part)
            .render(poseStack, packedLight, blockPos, blockEntity);
    }

    public void renderAnimatedHead(PoseStack poseStack, int packedLight, Matrix4f transform,
                                   BlockPos blockPos, BlockEntity blockEntity) {
        BakedModel part = model.getPart(HEAD);
        if (part == null) {
            return;
        }
        poseStack.pushPose();
        if (transform != null) {
            poseStack.last().pose().mul(transform);
        }
        GlobalMeshCache.getOrCreateRenderer("press_" + HEAD, part)
            .render(poseStack, packedLight, blockPos, blockEntity);
        poseStack.popPose();
    }
}

