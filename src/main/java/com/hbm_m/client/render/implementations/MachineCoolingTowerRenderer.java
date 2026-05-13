package com.hbm_m.client.render.implementations;

import com.hbm_m.block.entity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.block.machines.MachineCoolingTowerBlock;
import com.hbm_m.client.model.MachineCoolingTowerBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.ObjModelVboBuilder;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

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
public class MachineCoolingTowerRenderer extends AbstractPartBasedRenderer<MachineCoolingTowerBlockEntity, MachineCoolingTowerBakedModel> {

    private MachineCoolingTowerVboRenderer gpu;
    private MachineCoolingTowerBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedMain;
    private static volatile boolean instancersInitialized = false;

    public MachineCoolingTowerRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static void clearCaches() {
        if (instancedMain != null) {
            instancedMain.cleanup();
            instancedMain = null;
        }
        instancersInitialized = false;
    }

    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (instancedMain != null) {
            instancedMain.flush(event);
        }
    }

    private static synchronized void initializeInstancedRenderersSync(MachineCoolingTowerBakedModel model) {
        if (instancersInitialized) return;
        try {
            MainRegistry.LOGGER.info("MachineCoolingTowerRenderer: Initializing instanced renderers...");
            BakedModel part = model.getPart("Cube_Cube.001");
            if (part != null) {
                var data = ObjModelVboBuilder.buildSinglePart(part, "Cube_Cube.001");
                var quads = GlobalMeshCache.getOrCompile("cooling_tower_Cube_Cube.001", part);
                if (data != null) {
                    instancedMain = new InstancedStaticPartRenderer(data, quads, true);
                }
            }
            instancersInitialized = true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize cooling tower instanced renderers", e);
            clearCaches();
        }
    }

    @Override
    protected MachineCoolingTowerBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof MachineCoolingTowerBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(MachineCoolingTowerBlockEntity be) {
        return be.getBlockState().getValue(MachineCoolingTowerBlock.FACING);
    }

    @Override
    protected void renderParts(MachineCoolingTowerBlockEntity be,
                               MachineCoolingTowerBakedModel model,
                               LegacyAnimator animator,
                               float partialTick,
                               int packedLight,
                               int packedOverlay,
                               PoseStack poseStack,
                               MultiBufferSource bufferSource) {

        BlockPos blockPos = be.getBlockPos();

        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, Minecraft.getInstance().level, renderBounds)) {
            return;
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineCoolingTowerVboRenderer(model);
        }

        if (!instancersInitialized) {
            initializeInstancedRenderersSync(model);
        }

        poseStack.pushPose();

        boolean isShaderActive = ShaderCompatibilityDetector.isExternalShaderActive();
        boolean useNewIrisVboPath = ShaderCompatibilityDetector.useNewIrisVboPath();
        boolean useBatching = ModClothConfig.useInstancedBatching();

        if (isShaderActive && !useNewIrisVboPath) {
            RenderType renderType = RenderType.cutoutMipped();
            renderType.setupRenderState();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getRendertypeCutoutMippedShader);
            gpu.render(poseStack, packedLight, blockPos, be, bufferSource);
            renderType.clearRenderState();
        } else {
            if (useBatching && instancedMain != null && instancedMain.isInitialized()) {
                instancedMain.addInstance(poseStack, packedLight, blockPos, be, bufferSource);
            } else {
                if (ShaderCompatibilityDetector.useNewIrisVboPath()) {
                    boolean inShadow = ShaderCompatibilityDetector.isRenderingShadowPass();
                    try (IrisRenderBatch batch = IrisRenderBatch.begin(inShadow, RenderSystem.getProjectionMatrix())) {
                        gpu.render(poseStack, packedLight, blockPos, be, bufferSource);
                    }
                } else {
                    gpu.render(poseStack, packedLight, blockPos, be, bufferSource);
                }
            }
        }

        poseStack.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineCoolingTowerBlockEntity be) { return false; }

    @Override public int getViewDistance() { return 128; }
}
