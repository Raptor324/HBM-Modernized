package com.hbm_m.client.render.implementations;


import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;
import com.hbm_m.client.model.CargoElevatorBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * BER для CargoElevator. Рендерит OBJ-модель через VBO
 * (аналог {@link MachineAssemblerRenderer}).
 * <p>
 * Логика рендера (порт из {@code RenderCargoElevator}):
 * <ul>
 *   <li>Guides — (height+1) раз, стопка по Y</li>
 *   <li>Base — 1 раз (если renderPlatform)</li>
 *   <li>Platform — 1 раз со смещением extension (если renderPlatform)</li>
 *   <li>Piston — (extension+1) раз, стопка по Y (если renderPlatform)</li>
 * </ul>
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class CargoElevatorRenderer extends AbstractPartBasedRenderer<CargoElevatorBlockEntity, CargoElevatorBakedModel> {

    private CargoElevatorVboRenderer gpu;
    private CargoElevatorBakedModel cachedModel;

    public CargoElevatorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    protected CargoElevatorBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof CargoElevatorBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(CargoElevatorBlockEntity be) {
        return be.getBlockState().getValue(com.hbm_m.block.machines.CargoElevatorBlock.FACING);
    }

    @Override
    protected void renderParts(CargoElevatorBlockEntity be,
                               CargoElevatorBakedModel model,
                               LegacyAnimator animator,
                               float partialTick,
                               int packedLight,
                               int packedOverlay,
                               PoseStack poseStack,
                               MultiBufferSource bufferSource) {
        BlockPos blockPos = be.getBlockPos();
        var minecraft = Minecraft.getInstance();

        // Occlusion culling
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        float staticFade = RenderDistanceHelper.computeStaticFade(be);
        if (staticFade < 0) return;
        SingleMeshVboRenderer.setFadeAlpha(staticFade);

        renderWithVBO(be, model, partialTick, poseStack, packedLight, blockPos, bufferSource);
    }

    private void renderWithVBO(CargoElevatorBlockEntity be,
                               CargoElevatorBakedModel model,
                               float partialTick,
                               PoseStack poseStack,
                               int packedLight,
                               BlockPos blockPos,
                               MultiBufferSource bufferSource) {
        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new CargoElevatorVboRenderer(model);
        }

        // Оригинал: translate(x + 0.5, y, z + 0.5)
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);

        // Интерполированное extension для плавной анимации на клиенте
        double extension = be.prevExtension + (be.extension - be.prevExtension) * partialTick;

        if (be.renderPlatform) {
            // Base — статично
            gpu.renderStaticPart(poseStack, packedLight, "Base", blockPos, be, bufferSource);

            // Platform — со смещением extension
            gpu.renderAnimatedPart(poseStack, packedLight, "Platform", extension, blockPos, be, bufferSource);

            // Piston — (extension + 1) раз, стопка по Y
            int pistonCount = (int) Math.floor(extension) + 1;
            for (int i = 0; i < pistonCount; i++) {
                gpu.renderAnimatedPart(poseStack, packedLight, "Piston", extension - i, blockPos, be, bufferSource);
            }
        }

        // Guides — (height + 1) раз, стопка по Y
        int guideCount = be.height + 1;
        for (int i = 0; i < guideCount; i++) {
            gpu.renderAnimatedPart(poseStack, packedLight, "Guides", i, blockPos, be, bufferSource);
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(CargoElevatorBlockEntity be) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    @Override
    public int getViewDistance() {
        return RenderDistanceHelper.getStaticViewDistanceBlocks();
    }

    public static void clearCaches() {
        // MeshRenderCache.clearAll() уже вызывается в ClientSetup.clearClientCachesDeferred()
        // Дополнительно очищаем только если есть специфичные кэши
    }
}