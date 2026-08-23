package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineFluidTankBlock;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.client.model.MachineFluidTankBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.client.render.util.DiamondPronter;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * Fluid Tank BlockEntityRenderer на VBO (аналог {@link MachineAssemblerRenderer}).
 * <p>
 * Геометрия: Frame (статический VBO) + Tank (VBO с ретекстурой под жидкость) —
 * вся геометрия в BER/VBO, chunk mesh пуст ({@link MachineFluidTankBakedModel#getQuads} = List.of()).
 * <p>
 * Алмазы опасности (NFPA): 1.7.10 {@code RenderFluidTank} diamond pass —
 * рендерятся через {@link DiamondPronter#pront} на двух боковых гранях бака.
 * <p>
 * BAT9000 переиспользует этот рендерер (см. {@link com.hbm_m.client.ClientSetup} —
 * {@code BlockEntityRenderers.register(BAT9000_BE, MachineFluidTankRenderer::new)}).
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineFluidTankRenderer extends AbstractPartBasedRenderer<MachineFluidTankBlockEntity, MachineFluidTankBakedModel> {

    private MachineFluidTankVboRenderer gpu;
    private MachineFluidTankBakedModel cachedModel;

    public MachineFluidTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    protected MachineFluidTankBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof MachineFluidTankBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(MachineFluidTankBlockEntity be) {
        return be.getBlockState().getValue(MachineFluidTankBlock.FACING);
    }

    @Override
    protected void renderParts(MachineFluidTankBlockEntity be, MachineFluidTankBakedModel model,
                              LegacyAnimator animator, float partialTick,
                              int packedLight, int packedOverlay, PoseStack poseStack,
                              MultiBufferSource bufferSource) {
        BlockPos blockPos = be.getBlockPos();

        // Куллинг + fade: AABB бака inflate(3) как в BE.getRenderBoundingBox();
        // в контрапшене Create shouldRender() пропускает frustum/ray-march кулинг.
        if (applyCullingAndStaticFade(be) < 0) {
            return;
        }

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        renderWithVBO(be, model, poseStack, dynamicLight, blockPos, bufferSource);

        // Алмазы опасности рисуются после VBO-частей, в той же PoseStack-ветке
        // (после setupBlockTransform → translate(0.5,0,0.5) + rotateY).
        renderHazardDiamonds(be, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void renderWithVBO(MachineFluidTankBlockEntity be, MachineFluidTankBakedModel model,
                               PoseStack poseStack, int dynamicLight, BlockPos blockPos,
                               MultiBufferSource bufferSource) {
        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineFluidTankVboRenderer(model);
        }

        // Iris batching: один apply()/clear() на Frame + Tank.
        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive();
        if (useIrisBatch) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                renderTankPartsInternal(be, model, poseStack, dynamicLight, blockPos, bufferSource);
            }
        } else {
            renderTankPartsInternal(be, model, poseStack, dynamicLight, blockPos, bufferSource);
        }
    }

    private void renderTankPartsInternal(MachineFluidTankBlockEntity be, MachineFluidTankBakedModel model,
                                         PoseStack poseStack, int dynamicLight, BlockPos blockPos,
                                         MultiBufferSource bufferSource) {
        // setupBlockTransform уже применил translate(0.5,0,0.5)+rotateY (90° + legacy facing).
        // VBO-меши запечены в OBJ-координатах (0..1 блок). Компенсируем pivot: translate(-0.5,0,-0.5).
        poseStack.pushPose();
        poseStack.translate(-0.5f, 0.0f, -0.5f);

        // Frame — статический VBO.
        gpu.renderFrame(poseStack, dynamicLight, blockPos, be, bufferSource);

        // Tank — VBO с ретекстурой под текущую жидкость.
        ResourceLocation fluidTex = be.getTankTextureLocation();
        gpu.renderTankRetextured(poseStack, dynamicLight, blockPos, fluidTex, be, bufferSource);

        poseStack.popPose();
    }

    /**
     * Алмазы опасности (NFPA) на двух боковых гранях бака.
     * 1.7.10 {@code RenderFluidTank}: translate(-0.25, 0.5, -1.501) / (0.25, 0.5, 1.501),
     * rotateY ±90°, scale(1, 0.375, 0.375).
     * <p>
     * PoseStack здесь уже после {@code setupBlockTransform} (translate(0.5,0,0.5)+rotateY),
     * поэтому смещения -1.501/+1.501 в локальных координатахモデルа.
     */
    private void renderHazardDiamonds(MachineFluidTankBlockEntity be, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Fluid fluid = be.getFluidTank().getTankType();
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            return;
        }

        FluidType type = FluidType.forFluid(fluid);

        BlockPos pos = be.getBlockPos();
        int light = LevelRenderer.getLightColor(be.getLevel(), pos.above(2));

        RenderSystem.disableCull();

        poseStack.pushPose();
        poseStack.translate(-0.25F, 0.5F, -0.501F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(1.0F, 0.375F, 0.375F);
        DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.25F, 0.5F, 2.501F);
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.scale(1.0F, 0.375F, 0.375F);
        DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);
        poseStack.popPose();

        RenderSystem.enableCull();
    }

    // ==================== CLEANUP ====================

    public static void clearCaches() {
        MachineFluidTankVboRenderer.clearTankTextureCache();
    }
}
