package com.hbm_m.client.render.implementations;


//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.CargoElevatorBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.mojang.blaze3d.vertex.PoseStack;

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * VBO-хелпер для CargoElevator. Кэширует и рендерит отдельные части
 * через {@link MeshRenderCache#getOrCreateRenderer}.
 * Аналог {@link MachineAssemblerVboRenderer}.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class CargoElevatorVboRenderer {

    private final CargoElevatorBakedModel model;

    public CargoElevatorVboRenderer(CargoElevatorBakedModel model) {
        this.model = model;
    }

    /**
     * Рендерит статичную часть (Base, Guides) без трансформации.
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
     * Рендерит анимированную часть (Platform, Piston) с трансформацией.
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