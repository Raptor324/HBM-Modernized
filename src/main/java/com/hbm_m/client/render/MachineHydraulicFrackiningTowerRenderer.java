package com.hbm_m.client.render;

import com.hbm_m.block.custom.machines.MachineHydraulicFrackiningTowerBlock;
import com.hbm_m.block.entity.custom.machines.MachineHydraulicFrackiningTowerBlockEntity;
import com.hbm_m.client.model.MachineHydraulicFrackiningTowerBakedModel;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MachineHydraulicFrackiningTowerRenderer extends AbstractPartBasedRenderer<MachineHydraulicFrackiningTowerBlockEntity, MachineHydraulicFrackiningTowerBakedModel> {

    private MachineHydraulicFrackiningTowerVboRenderer gpu;
    private MachineHydraulicFrackiningTowerBakedModel cachedModel;

    public MachineHydraulicFrackiningTowerRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static void clearCaches() {
        // Очистка при необходимости (перенесено в GlobalMeshCache для VBO)
    }

    // Для этой махины батчинг не нужен, она слишком большая.
    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {}

    @Override
    protected MachineHydraulicFrackiningTowerBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof MachineHydraulicFrackiningTowerBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(MachineHydraulicFrackiningTowerBlockEntity be) {
        return be.getBlockState().getValue(MachineHydraulicFrackiningTowerBlock.FACING);
    }

    @Override
    protected void renderParts(MachineHydraulicFrackiningTowerBlockEntity be,
                               MachineHydraulicFrackiningTowerBakedModel model,
                               LegacyAnimator animator,
                               float partialTick,
                               int packedLight,
                               int packedOverlay,
                               PoseStack poseStack,
                               MultiBufferSource bufferSource) {
        
        BlockPos blockPos = be.getBlockPos();

        // Culling с учетом гигантского размера
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, Minecraft.getInstance().level, renderBounds)) {
            return;
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineHydraulicFrackiningTowerVboRenderer(model);
        }

        poseStack.pushPose();
        Direction facing = getFacing(be);
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));

        boolean isShaderActive = ShaderCompatibilityDetector.isExternalShaderActive();

        if (isShaderActive) {
            // --- МАГИЯ СОВМЕСТИМОСТИ VBO С ШЕЙДЕРАМИ ---
            // Активируем стейт рендера блока. Iris перехватит этот вызов
            // и включит свои G-буферы (для расчета теней, нормалей и освещения).
            RenderType renderType = RenderType.cutoutMipped();
            renderType.setupRenderState();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getRendertypeCutoutMippedShader);

            // Рендерим башню через сверхбыстрый VBO. Никаких лагов от BulkData!
            gpu.render(poseStack, packedLight, blockPos, be, bufferSource);

            // Обязательно сбрасываем стейт, чтобы не сломать рендер других объектов
            renderType.clearRenderState();
        } else {
            // Обычный ванильный рендер VBO
            gpu.render(poseStack, packedLight, blockPos, be, bufferSource);
        }

        poseStack.popPose();
    }

    @Override 
    public boolean shouldRenderOffScreen(MachineHydraulicFrackiningTowerBlockEntity be) {
        return true; // Контроль видимости полностью лежит на OcclusionCullingHelper + огромном AABB
    }
}