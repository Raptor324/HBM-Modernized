package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.CargoElevatorBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.mojang.blaze3d.vertex.PoseStack;


import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * VBO helper for the cargo elevator. Caches and renders individual parts
 * via {@link MeshRenderCache#getOrCreateRenderer}.
 * Analog of {@link MachineAssemblerVboRenderer}.
 */

//? if < 1.21.1 {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} else {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class CargoElevatorVboRenderer {

    private final CargoElevatorBakedModel model;

    public CargoElevatorVboRenderer(CargoElevatorBakedModel model) {
        this.model = model;
    }

    /**
     * Renders a static part (Base, Guides) without transformation.
     */
    public void renderStaticPart(PoseStack poseStack, int packedLight, String partName,
                                  BlockPos blockPos, @Nullable BlockEntity blockEntity,
                                  @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(partName);
        if (part != null) {
            var r = MeshRenderCache.getOrCreateRenderer("cargo_elevator_" + partName, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    /**
     * Renders an animated part (Platform, Piston) with transformation.
     */
    public void renderAnimatedPart(PoseStack poseStack, int packedLight, String partName,
                                    double translateY, BlockPos blockPos,
                                    @Nullable BlockEntity blockEntity,
                                    @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(partName);
        if (part != null) {
            poseStack.pushPose();
            poseStack.translate(0, translateY, 0);
            var r = MeshRenderCache.getOrCreateRenderer("cargo_elevator_" + partName, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            poseStack.popPose();
        }
    }
}