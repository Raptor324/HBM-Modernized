package com.hbm_m.client.render;

import com.hbm_m.block.custom.machines.MachineHydraulicFrackiningTowerBlock;
import com.hbm_m.block.entity.custom.machines.MachineHydraulicFrackiningTowerBlockEntity;
import com.hbm_m.client.model.MachineHydraulicFrackiningTowerBakedModel;
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
public class MachineHydraulicFrackiningTowerRenderer extends AbstractPartBasedRenderer<MachineHydraulicFrackiningTowerBlockEntity, MachineHydraulicFrackiningTowerBakedModel> {

    private MachineHydraulicFrackiningTowerVboRenderer gpu;
    private MachineHydraulicFrackiningTowerBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedMain;
    private static volatile boolean instancersInitialized = false;

    public MachineHydraulicFrackiningTowerRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static void clearCaches() {
        if (instancedMain != null) {
            instancedMain.cleanup();
            instancedMain = null;
        }
        instancersInitialized = false;
    }

    // Реализуем метод flush, чтобы отрисовывать накопленные инстансы в конце кадра
    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (instancedMain != null) {
            instancedMain.flush(event);
        }
    }

    // Инициализация статического рендерера (один раз для всех вышек)
    private static synchronized void initializeInstancedRenderersSync(MachineHydraulicFrackiningTowerBakedModel model) {
        if (instancersInitialized) return;

        try {
            MainRegistry.LOGGER.info("MachineHydraulicFrackiningTowerRenderer: Initializing instanced renderers...");
            // Имя части берем из MachineHydraulicFrackiningTowerVboRenderer ("Cube_Cube.001")
            BakedModel part = model.getPart("Cube_Cube.001");
            if (part != null) {
                var data = ObjModelVboBuilder.buildSinglePart(part, "Cube_Cube.001");
                var quads = GlobalMeshCache.getOrCompile("frackining_tower_Cube_Cube.001", part);
                if (data != null) {
                    instancedMain = new InstancedStaticPartRenderer(data, quads);
                }
            }
            instancersInitialized = true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize fracking tower instanced renderers", e);
            clearCaches();
        }
    }

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

        // Ленивая инициализация батчинга
        if (!instancersInitialized) {
            initializeInstancedRenderersSync(model);
        }

        poseStack.pushPose();

        boolean isShaderActive = ShaderCompatibilityDetector.isExternalShaderActive();
        boolean useBatching = ModClothConfig.useInstancedBatching();

        if (isShaderActive) {
            RenderType renderType = RenderType.cutoutMipped();
            renderType.setupRenderState();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getRendertypeCutoutMippedShader);

            gpu.render(poseStack, packedLight, blockPos, be, bufferSource);

            renderType.clearRenderState();
        } else {
            // Шейдеров нет: проверяем конфиг на батчинг
            if (useBatching && instancedMain != null && instancedMain.isInitialized()) {
                // Добавляем вышку в список инстансов (отрисовка произойдет в flushInstancedBatches)
                instancedMain.addInstance(poseStack, packedLight, blockPos, be, bufferSource);
            } else {
                // Батчинг выключен или не инициализирован: обычный VBO рендер
                gpu.render(poseStack, packedLight, blockPos, be, bufferSource);
            }
        }

        poseStack.popPose();
    }

    @Override 
    public boolean shouldRenderOffScreen(MachineHydraulicFrackiningTowerBlockEntity be) {
        return true; 
    }
}