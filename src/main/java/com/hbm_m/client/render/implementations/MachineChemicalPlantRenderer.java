package com.hbm_m.client.render.implementations;


import org.joml.Matrix4f;

import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.client.model.MachineChemicalPlantBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.shader.IrisRenderBatch;
//? if forge {
import com.hbm_m.client.render.shader.IrisPhaseGuard;
//?}
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
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
public class MachineChemicalPlantRenderer extends AbstractPartBasedRenderer<MachineChemicalPlantBlockEntity, MachineChemicalPlantBakedModel> {

    private MachineChemicalPlantVboRenderer gpu;
    private MachineChemicalPlantBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedBase;
    private static volatile InstancedStaticPartRenderer instancedFrame;
    private static volatile boolean instancersInitialized = false;

    private final Matrix4f matSlider = new Matrix4f();
    private final Matrix4f matSpinner = new Matrix4f();

    /** Degrees → radians multiplier; see {@code MachineAdvancedAssemblerRenderer.DEG_TO_RAD}. */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /**
     * Пивот вращения спиннера в пространстве уже запечённых VBO-вершин.
     * В OBJ это {@code (0.5, 0, 0.5)}; JSON root translation {@code [-0.5, 0, 0.5]} сдвигает его в {@code (0, 0, 1)} — не менять JSON, только эти константы под VBO/Batch.
     */
    private static final float CHEMPLANT_BAKE_PIVOT_X = 0.0f;
    private static final float CHEMPLANT_BAKE_PIVOT_Z = 1.0f;

    public MachineChemicalPlantRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineChemicalPlantBakedModel model) {
        if (instancersInitialized) return;
        try {
            MainRegistry.LOGGER.info("ChemicalPlantRenderer: initializing instanced renderers...");
            instancedBase = createInstancedForPart(model, "Base");
            instancedFrame = createInstancedForPart(model, "Frame");
            instancersInitialized = true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("ChemicalPlantRenderer: failed to init instanced renderers", e);
            clearCaches();
        }
    }

    private static InstancedStaticPartRenderer createInstancedForPart(MachineChemicalPlantBakedModel model, String partName) {
        BakedModel part = model.getPart(partName);
        if (part == null) return null;
        String cacheKey = "chemplant_" + partName;
        PartGeometry geo = MeshRenderCache.getOrCompilePartGeometry(cacheKey, part);
        if (geo.isEmpty()) return null;
        var data = geo.toVboData(partName);
        if (data == null) return null;
        return new InstancedStaticPartRenderer(data, geo.solidQuads());
    }

    private void initializeInstancedRenderers(MachineChemicalPlantBakedModel model) {
        if (!instancersInitialized) {
            initializeInstancedRenderersSync(model);
        }
    }

    @Override
    protected MachineChemicalPlantBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof MachineChemicalPlantBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(MachineChemicalPlantBlockEntity be) {
        return be.getBlockState().getValue(MachineChemicalPlantBlock.FACING);
    }

    @Override
    protected void setupBlockTransform(LegacyAnimator animator, MachineChemicalPlantBlockEntity be) {
        var state = be.getBlockState();
        if (state.hasProperty(MachineChemicalPlantBlock.FACING)) {
            animator.setupChemicalPlantBlockTransform(state.getValue(MachineChemicalPlantBlock.FACING));
        } else {
            animator.translate(0.5, 0.0, 0.5);
        }
    }

    @Override
    protected void renderParts(MachineChemicalPlantBlockEntity be, MachineChemicalPlantBakedModel model, LegacyAnimator animator,
                              float partialTick, int packedLight, int packedOverlay, PoseStack poseStack,
                              MultiBufferSource bufferSource) {
        var state = be.getBlockState();
        boolean renderActive = state.hasProperty(MachineChemicalPlantBlock.RENDER_ACTIVE)
            && state.getValue(MachineChemicalPlantBlock.RENDER_ACTIVE);
        boolean useVboGeometry = ShaderCompatibilityDetector.useVboGeometry();

        var minecraft = Minecraft.getInstance();
        BlockPos blockPos = be.getBlockPos();
        AABB renderBounds = be.getRenderBoundingBox();
        if (minecraft.level == null || !OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        float staticFade = RenderDistanceHelper.computeStaticFade(blockPos);
        if (staticFade < 0) return;
        SingleMeshVboRenderer.setFadeAlpha(staticFade);

        MachineChemicalPlantVboRenderer.FluidVisual visual = MachineChemicalPlantVboRenderer.getRecipeVisual(be);

        // Старый baked-путь под шейдерами + простой: статика и idle-подвижные в baked; BER только жидкость/эффекты.
        // Под новым VBO путём (useVboGeometry==true) baked пуст и нам нужно рендерить всё самим.
        if (!useVboGeometry && !renderActive) {
            if (visual != null) {
                //? if forge {
                try (var ignored = IrisPhaseGuard.pushBlockEntities()) {
                    MachineChemicalPlantVboRenderer.renderChemplantFluid(be, model, partialTick, poseStack, bufferSource, packedLight, packedOverlay, visual);
                }
                //?} else {
                /*MachineChemicalPlantVboRenderer.renderChemplantFluid(be, model, partialTick, poseStack, bufferSource, packedLight, packedOverlay, visual);
                *///?}
            }
            return;
        }

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, renderActive);

        if (visual != null) {
            //? if forge {
            try (var ignored = IrisPhaseGuard.pushBlockEntities()) {
                MachineChemicalPlantVboRenderer.renderChemplantFluid(be, model, partialTick, poseStack, bufferSource, packedLight, packedOverlay, visual);
            }
            //?} else {
            /*MachineChemicalPlantVboRenderer.renderChemplantFluid(be, model, partialTick, poseStack, bufferSource, packedLight, packedOverlay, visual);
            *///?}
        }
    }

    /** Soft peak sine (BobMathUtil.sps). */
    private static double chemicalSps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    private void renderWithVBO(MachineChemicalPlantBlockEntity be, MachineChemicalPlantBakedModel model, float partialTick,
                              PoseStack poseStack, int dynamicLight, BlockPos blockPos, MultiBufferSource bufferSource,
                              boolean renderActive) {
        boolean useVboPath = ShaderCompatibilityDetector.useVboGeometry();

        if (useVboPath && !instancersInitialized) {
            initializeInstancedRenderers(model);
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineChemicalPlantVboRenderer(model);
        }

        boolean useBatching = useVboPath && ModClothConfig.useInstancedBatching();

        // Iris batching: amortise apply()/clear() across Base + Frame + Slider + Spinner.
        // Slider/Spinner always use SingleMeshVboRenderer (never instanced); they need
        // IrisRenderBatch.active() so drawCompanion reuses the shared program + direct
        // matrix uploads. If we only open the batch when (!batching || shadow) — like
        // machines whose animated parts are fully instanced — animated parts hit the
        // standalone apply/clear path per frame and GL spams INVALID_OPERATION / No
        // active program; geometry can vanish or project wrong.
        // Instanced Base/Frame flush later calls flushBatchIris (own apply/clear);
        // ClientModEvents closes any persistent batch before those flushes so ACTIVE
        // is not left stale after shader.clear().
        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        boolean useIrisBatch = useVboPath && ShaderCompatibilityDetector.isExternalShaderActive();
        if (useIrisBatch) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                renderChemicalPlantPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath, useBatching, renderActive);
            }
        } else {
            renderChemicalPlantPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath, useBatching, renderActive);
        }
    }

    private void renderChemicalPlantPartsInternal(MachineChemicalPlantBlockEntity be,
                                                  MachineChemicalPlantBakedModel model,
                                                  float partialTick,
                                                  PoseStack poseStack,
                                                  int dynamicLight,
                                                  BlockPos blockPos,
                                                  MultiBufferSource bufferSource,
                                                  boolean useVboPath,
                                                  boolean useBatching,
                                                  boolean renderActive) {
        var blockState = be.getBlockState();

        float staticFade = SingleMeshVboRenderer.getFadeAlpha();
        float animFade = RenderDistanceHelper.computeAnimatedFade(blockPos);
        boolean anyFading = staticFade < 0.99f || (animFade >= 0 && animFade < 0.99f);
        boolean effectiveBatching = useBatching && !anyFading;

        if (useVboPath || renderActive) {
            poseStack.pushPose();
            poseStack.translate(-0.5f, 0.0f, -0.5f);

            if (useVboPath) {
                if (effectiveBatching && instancedBase != null && instancedBase.isInitialized()) {
                    poseStack.pushPose();
                    instancedBase.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
                    poseStack.popPose();
                } else {
                    gpu.renderStaticBase(poseStack, dynamicLight, blockPos, be, bufferSource);
                }

                if (blockState.hasProperty(MachineChemicalPlantBlock.FRAME) && blockState.getValue(MachineChemicalPlantBlock.FRAME)) {
                    if (effectiveBatching && instancedFrame != null && instancedFrame.isInitialized()) {
                        poseStack.pushPose();
                        instancedFrame.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
                        poseStack.popPose();
                    } else {
                        gpu.renderStaticFrame(poseStack, dynamicLight, blockPos, be, bufferSource);
                    }
                }
            }

            if (animFade < 0) {
                poseStack.popPose();
                return;
            }
            SingleMeshVboRenderer.setFadeAlpha(Math.min(staticFade, animFade));
            float anim = be.getAnim(partialTick);

            double sdx = chemicalSps(anim * 0.125) * 0.375;
            // VBO/instanced: как GL после facing — сдвиг вдоль локальной X позы (не хвост T(-0.5) из assembler).
            matSlider.identity().translate((float) sdx, 0f, 0f);

            gpu.renderAnimatedPart(poseStack, dynamicLight, "Slider", matSlider, blockPos, be, bufferSource);

            float deg = (anim * 15f) % 360f;
            if (deg < 0f) deg += 360f;
            matSpinner.identity()
                .translate(CHEMPLANT_BAKE_PIVOT_X, 0f, CHEMPLANT_BAKE_PIVOT_Z)
                .rotateY(deg * DEG_TO_RAD)
                .translate(-CHEMPLANT_BAKE_PIVOT_X, 0f, -CHEMPLANT_BAKE_PIVOT_Z);

            gpu.renderAnimatedPart(poseStack, dynamicLight, "Spinner", matSpinner, blockPos, be, bufferSource);

            SingleMeshVboRenderer.setFadeAlpha(staticFade);
            poseStack.popPose();
        }
    }


    public static void flushInstancedBatches(org.joml.Matrix4f projectionMatrix) {
        flushInstanced(projectionMatrix, instancedBase);
        flushInstanced(projectionMatrix, instancedFrame);
    }

    public static void clearCaches() {
        cleanupInstanced(instancedBase);
        instancedBase = null;
        cleanupInstanced(instancedFrame);
        instancedFrame = null;
        instancersInitialized = false;
    }

    private static void cleanupInstanced(InstancedStaticPartRenderer r) {
        if (r != null) r.cleanup();
    }

    private static void flushInstanced(org.joml.Matrix4f projectionMatrix,
                                       InstancedStaticPartRenderer r) {
        if (r != null) r.flush(projectionMatrix);
    }
    @Override public int getViewDistance() { return RenderDistanceHelper.getStaticViewDistanceBlocks(); }
}

