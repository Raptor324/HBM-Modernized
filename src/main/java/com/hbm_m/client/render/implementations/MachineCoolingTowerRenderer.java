package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineTowerLargeBlock;
import com.hbm_m.blockentity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.client.model.MachineCoolingTowerBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.ObjModelVboBuilder;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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

    //? if forge {
    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (instancedMain != null) {
            instancedMain.flush(event);
        }
    }
    //?} elif neoforge {
    /*public static void flushInstancedBatches(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (instancedMain != null) {
            // 1.21.1: нет перегрузки flush(RenderLevelStageEvent); готовая сигнатура — flush(Matrix4f).
            instancedMain.flush(event.getProjectionMatrix());
        }
    }
    *///?}

    private static synchronized void initializeInstancedRenderersSync(MachineCoolingTowerBakedModel model) {
        if (instancersInitialized) return;
        try {
            MainRegistry.LOGGER.info("MachineCoolingTowerRenderer: Initializing instanced renderers...");
            BakedModel part = model.getPart("Cube_Cube.001");
            if (part != null) {
                var data = ObjModelVboBuilder.buildSinglePart(part, "Cube_Cube.001");
                var quads = MeshRenderCache.getOrCompile("cooling_tower_Cube_Cube.001", part);
                if (data != null) {
                    instancedMain = new InstancedStaticPartRenderer(data, quads,
                            com.hbm_m.client.render.ClientRenderFlags.useSlicedLightForNewRenderer());
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
        return be.getBlockState().getValue(MachineTowerLargeBlock.FACING);
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

        // Куллинг: в контрапшене Create shouldRender() пропускает frustum/ray-march кулинг.
        if (!passesOcclusionCulling(be)) {
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

        boolean useBatching = ModClothConfig.useInstancedBatching();

        if (useBatching && instancedMain != null && instancedMain.isInitialized()) {
            instancedMain.addInstance(poseStack, packedLight, blockPos, be, bufferSource);
        } else {
            if (ShaderCompatibilityDetector.isExternalShaderActive()) {
                boolean inShadow = ShaderCompatibilityDetector.isRenderingShadowPass();
                try (IrisRenderBatch batch = IrisRenderBatch.begin(inShadow, RenderSystem.getProjectionMatrix())) {
                    gpu.render(poseStack, packedLight, blockPos, be, bufferSource);
                }
            } else {
                gpu.render(poseStack, packedLight, blockPos, be, bufferSource);
            }
        }

        poseStack.popPose();
    }

    @Override public int getViewDistance() { return 128; }
}