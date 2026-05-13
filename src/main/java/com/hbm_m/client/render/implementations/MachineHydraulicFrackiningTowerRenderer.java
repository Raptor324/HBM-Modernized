package com.hbm_m.client.render.implementations;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
import com.hbm_m.block.entity.machines.MachineFrackingTowerBlockEntity;
import com.hbm_m.block.machines.MachineFrackingTowerBlock;
import com.hbm_m.client.model.MachineHydraulicFrackiningTowerBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.ObjModelVboBuilder;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class MachineHydraulicFrackiningTowerRenderer extends AbstractPartBasedRenderer<MachineFrackingTowerBlockEntity, MachineHydraulicFrackiningTowerBakedModel> {

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

    public static void flushInstancedBatches(org.joml.Matrix4f projectionMatrix) {
        if (instancedMain != null) {
            instancedMain.flush(projectionMatrix);
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
                    // Tall tower: use sliced light probes (2x4x2) so mid-height side lights affect the mesh.
                    instancedMain = new InstancedStaticPartRenderer(data, quads, true);
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
    protected Direction getFacing(MachineFrackingTowerBlockEntity be) {
        return be.getBlockState().getValue(MachineFrackingTowerBlock.FACING);
    }

    @Override
    protected void renderParts(MachineFrackingTowerBlockEntity be,
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

        float staticFade = RenderDistanceHelper.computeStaticFade(blockPos);
        if (staticFade < 0) return;
        SingleMeshVboRenderer.setFadeAlpha(staticFade);

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
        boolean useNewIrisVboPath = ShaderCompatibilityDetector.useNewIrisVboPath();
        boolean useBatching = ModClothConfig.useInstancedBatching();

        if (isShaderActive && !useNewIrisVboPath) {
            // Старый путь под шейдерами: один draw через cutoutMipped, без инстансинга и без Iris ExtendedShader.
            RenderType renderType = RenderType.cutoutMipped();
            renderType.setupRenderState();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getRendertypeCutoutMippedShader);

            gpu.render(poseStack, packedLight, blockPos, be, bufferSource);

            renderType.clearRenderState();
        } else {
            // Шейдеров нет ИЛИ включён useIrisExtendedShaderPath: используем нашу VBO/инстанс-систему.
            // SingleMeshVboRenderer и InstancedStaticPartRenderer сами выберут Iris ExtendedShader,
            // если шейдер-пак активен (см. ShaderCompatibilityDetector.canUseIrisExtendedShader).
            if (useBatching && staticFade >= 0.99f && instancedMain != null && instancedMain.isInitialized()) {
                instancedMain.addInstance(poseStack, packedLight, blockPos, be, bufferSource);
            } else {
                // Ветка всегда без instancing (один part → SingleMeshVboRenderer). Под Iris
                // открываем тот же persistent IrisRenderBatch, что Assembler/Chemical Plant:
                // один shader.apply/пакет за проход вместо apply+clear на каждой вышке — иначе
                // GL_INVALID_OPERATION / No active program и пропадание по углу камеры.
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

    @Override public boolean shouldRenderOffScreen(MachineFrackingTowerBlockEntity be) { return false; }

    @Override public int getViewDistance() { return RenderDistanceHelper.getStaticViewDistanceBlocks(); }
}