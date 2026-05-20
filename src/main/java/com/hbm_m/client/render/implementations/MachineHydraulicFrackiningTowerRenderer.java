package com.hbm_m.client.render.implementations;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
import com.hbm_m.block.entity.machines.MachineFrackingTowerBlockEntity;
import com.hbm_m.block.machines.MachineFrackingTowerBlock;
import com.hbm_m.client.model.MachineHydraulicFrackiningTowerBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
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

    private static final String MAIN_PART = "Cube_Cube.001";

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

    private static synchronized void initializeInstancedRenderersSync(MachineHydraulicFrackiningTowerBakedModel model) {
        if (instancersInitialized) return;

        try {
            MainRegistry.LOGGER.info("MachineHydraulicFrackiningTowerRenderer: Initializing instanced renderers...");
            BakedModel part = model.getPart(MAIN_PART);
            if (part != null) {
                String cacheKey = "frackining_tower_" + MAIN_PART;
                PartGeometry geo = MeshRenderCache.getOrCompilePartGeometry(cacheKey, part);
                if (!geo.isEmpty()) {
                    var data = geo.toVboData(MAIN_PART);
                    if (data != null) {
                        // Tall tower: sliced light probes (2x4x2) — same as VBO fallback path.
                        InstancedStaticPartRenderer r = new InstancedStaticPartRenderer(data, geo.solidQuads(), true);
                        r.setMdiTraceTag("FrackingTower/" + MAIN_PART);
                        instancedMain = r;
                    }
                }
            }
            instancersInitialized = true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize fracking tower instanced renderers", e);
            clearCaches();
        }
    }

    private void initializeInstancedRenderers(MachineHydraulicFrackiningTowerBakedModel model) {
        if (!instancersInitialized) {
            initializeInstancedRenderersSync(model);
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
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, Minecraft.getInstance().level, renderBounds)) {
            return;
        }

        float staticFade = RenderDistanceHelper.computeStaticFade(blockPos);
        if (staticFade < 0) return;
        SingleMeshVboRenderer.setFadeAlpha(staticFade);

        boolean useVboPath = ShaderCompatibilityDetector.useVboGeometry();
        if (useVboPath) {
            initializeInstancedRenderers(model);
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineHydraulicFrackiningTowerVboRenderer(model);
        }

        boolean useBatching = useVboPath && ModClothConfig.useInstancedBatching();
        // Under an active shader pack, deferred instanced flush needs ExtendedShader;
        // otherwise vanilla instanced draw hits "No active program" (sliced path is
        // especially visible on this tall mesh).
        boolean canDeferredInstancedFlush = !ShaderCompatibilityDetector.isExternalShaderActive()
                || ShaderCompatibilityDetector.canUseIrisExtendedShader();
        boolean effectiveBatching = useBatching
                && staticFade >= 0.99f
                && canDeferredInstancedFlush;

        poseStack.pushPose();

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        // Same Iris batch contract as MachineAdvancedAssemblerRenderer.renderWithVBO.
        boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive()
                && (!effectiveBatching || shadowPass);

        if (useIrisBatch) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                renderTowerMesh(be, poseStack, packedLight, blockPos, bufferSource, effectiveBatching);
            }
        } else {
            renderTowerMesh(be, poseStack, packedLight, blockPos, bufferSource, effectiveBatching);
        }

        poseStack.popPose();
    }

    private void renderTowerMesh(MachineFrackingTowerBlockEntity be,
                                 PoseStack poseStack,
                                 int packedLight,
                                 BlockPos blockPos,
                                 MultiBufferSource bufferSource,
                                 boolean effectiveBatching) {
        if (effectiveBatching && instancedMain != null && instancedMain.isInitialized()) {
            instancedMain.addInstance(poseStack, packedLight, blockPos, be, bufferSource);
        } else {
            gpu.render(poseStack, packedLight, blockPos, be, bufferSource);
        }
    }

    @Override public boolean shouldRenderOffScreen(MachineFrackingTowerBlockEntity be) { return false; }

    @Override public int getViewDistance() { return RenderDistanceHelper.getStaticViewDistanceBlocks(); }
}
