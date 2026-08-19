package com.hbm_m.client.render.implementations;


import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.blockentity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.client.model.MachineChemicalPlantBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.IrisPhaseGuard;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineChemicalPlantRenderer extends AbstractPartBasedRenderer<MachineChemicalPlantBlockEntity, MachineChemicalPlantBakedModel> {

    private MachineChemicalPlantVboRenderer gpu;
    private MachineChemicalPlantBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedBase;
    private static volatile InstancedStaticPartRenderer instancedFrame;
    private static volatile InstancedStaticPartRenderer instancedSlider;
    private static volatile InstancedStaticPartRenderer instancedSpinner;
    private static volatile boolean instancersInitialized = false;

    /**
     * Отложенный fluid-запрос: {@code renderParts} только фиксирует матрицу pose +
     * цвет/anim, сама отрисовка идёт в {@link #presentDeferredFluids()} в
     * {@code AFTER_BLOCK_ENTITIES} — после {@code IrisRenderBatch.closePersistentIfActive()}
     * и instanced-flush частей, внутри {@code IrisPhaseGuard} BLOCK_ENTITIES.
     * <p>
     * Почему deferred, а не inline в BER (как раньше):
     * <ul>
     *   <li>Inline-рисование шло пока активен persistent Iris-batch (наш BLOCK_ENTITY
     *       ExtendedShader = активная GL-программа). {@code endBatch()} translucent
     *       загружал ModelViewMat/ProjMat, но GL-программа ещё не переключилась на
     *       translucent → {@code glUniformMatrix*} по не-матричному uniform →
     *       {@code GL_INVALID_OPERATION: Uniform must be a matrix type}.</li>
     *   <li>При включённом instanced batching части идут через {@code addInstance}
     *       (запись, draw позже в {@code flushInstancedBatches}). Inline-жидкость в BER
     *       рисовалась ДО частей → opaque-части рисовали поверх жидкости («корпус всегда
     *       ближе жидкости»). Deferred рисует жидкость после частей → корректный depth.</li>
     * </ul>
     * Та же модель, что у {@link MachineCrystallizerRenderer#presentDeferredFluids()}.
     */
    private record DeferredChemplantFluid(
            MachineChemicalPlantBakedModel model,
            net.minecraft.world.level.block.state.BlockState state,
            Matrix4f pose,
            float anim,
            int packedLight,
            int packedOverlay,
            MachineChemicalPlantVboRenderer.FluidVisual visual
    ) {}

    private static final List<DeferredChemplantFluid> DEFERRED_FLUIDS = new ArrayList<>();

    private final Matrix4f matSlider = new Matrix4f();
    private final Matrix4f matSpinner = new Matrix4f();

    /** Degrees → radians multiplier; see {@code MachineAdvancedAssemblerRenderer.DEG_TO_RAD}. */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /**
     * Пивот вращения спиннера в пространстве УЖЕ ЗАПЕЧЁННЫХ VBO-вершин
     * (baked-space = OBJ + JSON root translation).
     * <p>
     * Центр геометрии Spinner в OBJ = {@code (0.5, 1.25, 0.5)} (X,Z = 0.5, 0.5).
     * JSON {@code chemical_plant.json} transform.translation = {@code [0.5, 0, 0.5]}
     * (компенсация смещения контроллера в структуре). Значит baked-центр спиннера
     * (X,Z) = {@code (0.5+0.5, 0.5+0.5)} = {@code (1.0, 1.0)}.
     * <p>
     * matSpinner применяется к baked-вершинам (после T_off(-0.5,0,-0.5) в renderParts),
     * поэтому пивот должен быть в baked-координатах. Раньше тут было (0.5, 0.5) —
     * OBJ-центр, без учёта JSON-сдвига → спиннер крутился вокруг чужой оси.
     * <p>
     * Если снова меняешь JSON translation: pivot = OBJ_CENTER + JSON_translation.
     * Вращение вокруг Y, поэтому Y пивота не важен.
     */
    private static final float CHEMPLANT_BAKE_PIVOT_X = 1.0f;
    private static final float CHEMPLANT_BAKE_PIVOT_Z = 1.0f;

    public MachineChemicalPlantRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineChemicalPlantBakedModel model) {
        if (instancersInitialized) return;
        try {
            MainRegistry.LOGGER.info("ChemicalPlantRenderer: initializing instanced renderers...");
            instancedBase = createInstancedForPart(model, "Base");
            instancedFrame = createInstancedForPart(model, "Frame");
            instancedSlider = createInstancedForPart(model, "Slider");
            instancedSpinner = createInstancedForPart(model, "Spinner");
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
        InstancedStaticPartRenderer r = new InstancedStaticPartRenderer(data, geo.solidQuads());
        r.setMdiTraceTag("ChemPlant/" + partName);
        return r;
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

        Direction facing = getFacing(be);
        var minecraft = Minecraft.getInstance();
        BlockPos blockPos = be.getBlockPos();

        AABB renderBounds;
        if (state.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController controller && controller.getStructureHelper() != null) {
            renderBounds = controller.getStructureHelper().getRenderBoundingBox(blockPos, facing, 0.0);
        } else {
            renderBounds = be.getRenderBoundingBox();
        }

        if (minecraft.level == null || !OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        float staticFade = RenderDistanceHelper.computeStaticFade(be);
        if (staticFade < 0) return;
        SingleMeshVboRenderer.setFadeAlpha(staticFade);

        MachineChemicalPlantVboRenderer.FluidVisual visual = MachineChemicalPlantVboRenderer.getRecipeVisual(be);

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource);

        if (visual != null) {
            //? if forge || neoforge {
            // Только запись в очередь; draw — в presentDeferredFluids (AFTER_BLOCK_ENTITIES),
            // см. комментарий у DeferredChemplantFluid.
            scheduleDeferredFluid(model, be.getBlockState(), poseStack, be.getAnim(partialTick),
                    packedLight, packedOverlay, visual);
            //?}
            // Fabric: deferred-fluid путь требует Forge/NeoForge getQuads(ModelData, RenderType), на fabric жидкость не рисуется.
        }
    }

    /** Soft peak sine (BobMathUtil.sps). */
    private static double chemicalSps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    private void renderWithVBO(MachineChemicalPlantBlockEntity be, MachineChemicalPlantBakedModel model, float partialTick,
                              PoseStack poseStack, int dynamicLight, BlockPos blockPos, MultiBufferSource bufferSource) {
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
        // Slider/Spinner use instanced batching when effectiveBatching; IrisRenderBatch.active()
        // still needed so drawCompanion reuses the shared program + direct
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
                renderChemicalPlantPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath, useBatching);
            }
        } else {
            renderChemicalPlantPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath, useBatching);
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
                                                  boolean useBatching) {
        var blockState = be.getBlockState();

        float staticFade = SingleMeshVboRenderer.getFadeAlpha();
        float animFade = RenderDistanceHelper.computeAnimatedFade(blockPos);
        boolean anyFading = staticFade < 0.99f || (animFade >= 0 && animFade < 0.99f);
        boolean effectiveBatching = useBatching && !anyFading;

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
        } else {
            gpu.renderStaticBase(poseStack, dynamicLight, blockPos, be, bufferSource);
            if (blockState.hasProperty(MachineChemicalPlantBlock.FRAME) && blockState.getValue(MachineChemicalPlantBlock.FRAME)) {
                gpu.renderStaticFrame(poseStack, dynamicLight, blockPos, be, bufferSource);
            }
        }

        if (animFade < 0) {
            poseStack.popPose();
            return;
        }
        SingleMeshVboRenderer.setFadeAlpha(Math.min(staticFade, animFade));
        float anim = be.getAnim(partialTick);

        double sdx = chemicalSps(anim * 0.125) * 0.375;
        matSlider.identity().translate((float) sdx, 0f, 0f);

        if (effectiveBatching && instancedSlider != null && instancedSlider.isInitialized()) {
            poseStack.pushPose();
            poseStack.last().pose().mul(matSlider);
            instancedSlider.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
            poseStack.popPose();
        } else {
            gpu.renderAnimatedPart(poseStack, dynamicLight, "Slider", matSlider, blockPos, be, bufferSource);
        }

        float deg = (anim * 15f) % 360f;
        if (deg < 0f) deg += 360f;
        matSpinner.identity()
            .translate(CHEMPLANT_BAKE_PIVOT_X, 0f, CHEMPLANT_BAKE_PIVOT_Z)
            .rotateY(deg * DEG_TO_RAD)
            .translate(-CHEMPLANT_BAKE_PIVOT_X, 0f, -CHEMPLANT_BAKE_PIVOT_Z);

        if (effectiveBatching && instancedSpinner != null && instancedSpinner.isInitialized()) {
            poseStack.pushPose();
            poseStack.last().pose().mul(matSpinner);
            instancedSpinner.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
            poseStack.popPose();
        } else {
            gpu.renderAnimatedPart(poseStack, dynamicLight, "Spinner", matSpinner, blockPos, be, bufferSource);
        }

        SingleMeshVboRenderer.setFadeAlpha(staticFade);
        poseStack.popPose();
    }


    // ==================== DEFERRED FLUID ====================

    /** Сброс очереди жидкости в начале кадра (или на early-return). */
    public static void clearDeferredFluids() {
        DEFERRED_FLUIDS.clear();
    }

    /**
     * Запись жидкости в очередь на отложенную отрисовку. Вызывается из
     * {@code renderParts} в BER-фазе; pose и anim фиксируются здесь, draw — позже.
     * В shadow-pass не планируем (жидкость translucent, в shadow не видна и
     * только дропает depth).
     */
    //? if forge || neoforge {
    static void scheduleDeferredFluid(MachineChemicalPlantBakedModel model,
                                      net.minecraft.world.level.block.state.BlockState state,
                                      PoseStack poseStack, float anim,
                                      int packedLight, int packedOverlay,
                                      MachineChemicalPlantVboRenderer.FluidVisual visual) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) return;
        DEFERRED_FLUIDS.add(new DeferredChemplantFluid(
                model, state, new Matrix4f(poseStack.last().pose()), anim, packedLight, packedOverlay, visual));
    }
    //?}

    /**
     * Отрисовка всей накопленной за кад жидкости. Вызывается из
     * {@code InstancedRenderFrame.presentAfterBlockEntities} после
     * {@code IrisRenderBatch.closePersistentIfActive()} и instanced-flush.
     * <p>
     * depthMask(false) вокруг цикла — как {@code glDepthMask(false)} в 1.7.10:
     * жидкость не пишет depth, корректно смешивается с частями за ней (depth уже
     * содержит opaque-части, отрисованные на instanced-flush). Один
     * {@code endBatch()} изолированного {@link MachineChemicalPlantVboRenderer#FLUID_BUFFER_SOURCE}
     * после цикла = один translucent glDraw (а не N), вне shared bufferSource.
     */
    public static void presentDeferredFluids() {
        if (DEFERRED_FLUIDS.isEmpty()) return;
        //? if forge || neoforge {
        try (var ignored = IrisPhaseGuard.pushBlockEntities()) {
            boolean depthMaskWas = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            PoseStack poseStack = new PoseStack();
            try {
                RenderSystem.depthMask(false);
                for (DeferredChemplantFluid e : DEFERRED_FLUIDS) {
                    poseStack.pushPose();
                    poseStack.last().pose().set(e.pose);
                    poseStack.translate(-0.5f, 0f, -0.5f);
                    MachineChemicalPlantVboRenderer.drawChemplantFluidBaked(
                            e.model, e.state, e.anim, poseStack,
                            MachineChemicalPlantVboRenderer.FLUID_BUFFER_SOURCE,
                            e.packedLight, e.packedOverlay, e.visual);
                    poseStack.popPose();
                }
                MachineChemicalPlantVboRenderer.FLUID_BUFFER_SOURCE.endBatch();
            } finally {
                RenderSystem.depthMask(depthMaskWas);
            }
        }
        //?}
        DEFERRED_FLUIDS.clear();
    }

    public static void flushInstancedBatches(org.joml.Matrix4f projectionMatrix) {
        flushInstanced(projectionMatrix, instancedBase);
        flushInstanced(projectionMatrix, instancedFrame);
        flushInstanced(projectionMatrix, instancedSlider);
        flushInstanced(projectionMatrix, instancedSpinner);
    }

    public static void clearCaches() {
        cleanupInstanced(instancedBase);
        instancedBase = null;
        cleanupInstanced(instancedFrame);
        instancedFrame = null;
        cleanupInstanced(instancedSlider);
        instancedSlider = null;
        cleanupInstanced(instancedSpinner);
        instancedSpinner = null;
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

