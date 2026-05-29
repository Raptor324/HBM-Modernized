package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.block.entity.machines.MachineCrystallizerBlockEntity;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
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
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
//? if forge {
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
//?}

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * BER Crystallizer: вращающийся спиннер (VBO + instancing) и жидкость в баке
 * (отдельная модель {@code crystallizer_fluid}, спрайт текущей жидкости).
 *
 * <p>Статическое тело — {@code crystallizer.json} в chunk mesh (Spinner/Fluid скрыты).
 * Под Iris/VBO путь совпадает с {@link MachineChemicalPlantRenderer} / {@link MachineAdvancedAssemblerRenderer}.</p>
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)
*///?}
public class MachineCrystallizerRenderer implements BlockEntityRenderer<MachineCrystallizerBlockEntity> {

    private static final RandomSource RANDOM = RandomSource.create(42L);
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final MachineCrystallizerVboRenderer gpu = new MachineCrystallizerVboRenderer();

    private static volatile InstancedStaticPartRenderer instancedSpinner;
    private static volatile boolean instancersInitialized = false;

    private final Matrix4f matSpinner = new Matrix4f();

    private record DeferredCrystallizerFluid(
        BlockPos pos,
        Matrix4f pose,
        int packedLight,
        int packedOverlay,
        TextureAtlasSprite sprite,
        float r, float g, float b
    ) {}

    private static final List<DeferredCrystallizerFluid> DEFERRED_FLUIDS = new ArrayList<>();

    public MachineCrystallizerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MachineCrystallizerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        com.hbm_m.client.render.LightSampleCache.BASE_POSE.get().set(poseStack.last().pose());
        com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(true);
        try {
            var minecraft = Minecraft.getInstance();
            BlockPos blockPos = blockEntity.getBlockPos();
            if (minecraft.level == null
                    || !OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, blockEntity.getRenderBoundingBox())) {
                return;
            }

            float staticFade = RenderDistanceHelper.computeStaticFade(blockPos);
            if (staticFade < 0) return;

            poseStack.pushPose();
            applyFacingRotation(blockEntity, poseStack);

            boolean useVboPath = ShaderCompatibilityDetector.useVboGeometry();
            if (!useVboPath) {
                renderLegacySpinnerAndFluid(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
                return;
            }

            SingleMeshVboRenderer.setFadeAlpha(staticFade);
            int blockLight = LightTexture.block(packedLight);
            int skyLight = LightTexture.sky(packedLight);
            int dynamicLight = LightTexture.pack(blockLight, skyLight);

            renderSpinnerVbo(blockEntity, partialTick, poseStack, dynamicLight, blockPos, bufferSource, staticFade);
            scheduleFluidIfPresent(blockEntity, poseStack, packedLight, packedOverlay);

            poseStack.popPose();
        } finally {
            com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(false);
        }
    }

    /** Сброс очереди жидкости в начале кадра (до BER). */
    public static void clearDeferredFluids() {
        DEFERRED_FLUIDS.clear();
    }

    static void scheduleDeferredFluid(BlockPos pos, Matrix4f poseInLocalSpace, int packedLight, int packedOverlay,
                                      TextureAtlasSprite sprite, float r, float g, float b) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }
        DEFERRED_FLUIDS.add(new DeferredCrystallizerFluid(
            pos, new Matrix4f(poseInLocalSpace), packedLight, packedOverlay, sprite, r, g, b));
    }

    /**
     * После flush instanced: depth уже содержит спиннеры, жидкость только depth-test.
     */
    public static void presentDeferredFluids() {
        if (DEFERRED_FLUIDS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            DEFERRED_FLUIDS.clear();
            return;
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack poseStack = new PoseStack();

        //? if forge {
        try (var ignored = IrisPhaseGuard.pushBlockEntities()) {
            for (DeferredCrystallizerFluid entry : DEFERRED_FLUIDS) {
                poseStack.pushPose();
                poseStack.last().pose().set(entry.pose);
                MachineCrystallizerVboRenderer.drawCrystallizerFluidBaked(
                    poseStack, buffers, entry.packedLight, entry.packedOverlay,
                    entry.sprite, entry.r, entry.g, entry.b);
                poseStack.popPose();
            }
            buffers.endBatch();
        }
        //?} else {
        /*DEFERRED_FLUIDS.clear();*///?}
        DEFERRED_FLUIDS.clear();
    }

    private void renderSpinnerVbo(MachineCrystallizerBlockEntity be, float partialTick, PoseStack poseStack,
                                  int dynamicLight, BlockPos blockPos, MultiBufferSource bufferSource,
                                  float staticFade) {
        BakedModel spinnerModel = MachineCrystallizerVboRenderer.getSpinnerModel();
        if (spinnerModel == null) return;

        if (!instancersInitialized) {
            initializeInstancedRenderersSync();
        }

        float angle = Mth.lerp(partialTick, be.prevAngle, be.angle);
        matSpinner.identity()
            .translate(0.5f, 0f, 0.5f)
            .rotateY(angle * DEG_TO_RAD)
            .translate(-0.5f, 0f, -0.5f);

        boolean useBatching = ModClothConfig.useInstancedBatching();
        float animFade = RenderDistanceHelper.computeAnimatedFade(blockPos);
        boolean anyFading = staticFade < 0.99f || (animFade >= 0 && animFade < 0.99f);
        boolean effectiveBatching = useBatching && !anyFading;

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive();

        Runnable drawSpinner = () -> drawSpinnerInternal(poseStack, dynamicLight, blockPos, be, bufferSource,
                effectiveBatching, animFade, staticFade);

        if (useIrisBatch) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                drawSpinner.run();
            }
        } else {
            drawSpinner.run();
        }
    }

    private void drawSpinnerInternal(PoseStack poseStack, int dynamicLight, BlockPos blockPos,
                                     MachineCrystallizerBlockEntity be, MultiBufferSource bufferSource,
                                     boolean effectiveBatching, float animFade, float staticFade) {
        if (animFade < 0) return;

        float fade = Math.min(staticFade, animFade >= 0 ? animFade : staticFade);
        SingleMeshVboRenderer.setFadeAlpha(fade);

        if (effectiveBatching && instancedSpinner != null && instancedSpinner.isInitialized()) {
            poseStack.pushPose();
            poseStack.last().pose().mul(matSpinner);
            instancedSpinner.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
            poseStack.popPose();
        } else {
            poseStack.pushPose();
            poseStack.last().pose().mul(matSpinner);
            gpu.renderSpinner(poseStack, dynamicLight, blockPos, be, bufferSource);
            poseStack.popPose();
        }

        SingleMeshVboRenderer.setFadeAlpha(staticFade);
    }

    private void scheduleFluidIfPresent(MachineCrystallizerBlockEntity be, PoseStack poseStack,
                                        int packedLight, int packedOverlay) {
        if (be.getTank().isEmpty()) return;
        Fluid fluid = be.getTank().getStoredFluid();
        if (fluid == null) return;

        TextureAtlasSprite sprite = MachineCrystallizerVboRenderer.getFluidSprite(be, fluid);
        if (sprite == null) return;

        int color = MachineCrystallizerVboRenderer.getFluidTint(be, fluid);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        //? if forge {
        try (var ignored = IrisPhaseGuard.pushBlockEntities()) {
            MachineCrystallizerVboRenderer.scheduleDeferredFluid(be, poseStack, packedLight, packedOverlay, sprite, r, g, b);
        }
        //?} else {
        /*MachineCrystallizerVboRenderer.scheduleDeferredFluid(be, poseStack, packedLight, packedOverlay, sprite, r, g, b);
        *///?}
    }

    private static synchronized void initializeInstancedRenderersSync() {
        if (instancersInitialized) return;
        try {
            MainRegistry.LOGGER.info("MachineCrystallizerRenderer: initializing instanced renderers...");
            List<BakedQuad> spinnerQuads = MachineCrystallizerVboRenderer.collectSpinnerQuads();
            if (spinnerQuads.isEmpty()) {
                MainRegistry.LOGGER.error("MachineCrystallizerRenderer: spinner geometry empty");
                clearCaches();
                return;
            }
            var data = PartGeometry.buildVboDataFromQuads(spinnerQuads, "crystallizer_spinner");
            if (data == null) {
                clearCaches();
                return;
            }
            instancedSpinner = new InstancedStaticPartRenderer(data, spinnerQuads);
            instancedSpinner.setMdiTraceTag("Crystallizer/Spinner");
            instancersInitialized = true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("MachineCrystallizerRenderer: failed to init instanced renderers", e);
            clearCaches();
        }
    }

    public static void flushInstancedBatches(org.joml.Matrix4f projectionMatrix) {
        if (instancedSpinner != null) {
            instancedSpinner.flush(projectionMatrix);
        }
    }

    public static void clearCaches() {
        if (instancedSpinner != null) {
            instancedSpinner.cleanup();
            instancedSpinner = null;
        }
        instancersInitialized = false;
    }

    // --- Legacy baked path (no VBO geometry / vanilla chunk) ---

    private static void renderLegacySpinnerAndFluid(MachineCrystallizerBlockEntity blockEntity, float partialTick,
                                                    PoseStack poseStack, MultiBufferSource bufferSource,
                                                    int packedLight, int packedOverlay) {
        renderLegacySpinner(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        renderLegacyFluid(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderLegacySpinner(MachineCrystallizerBlockEntity blockEntity, float partialTick,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        BakedModel spinnerModel = MachineCrystallizerVboRenderer.getSpinnerModel();
        if (spinnerModel == null) return;

        float angle = Mth.lerp(partialTick, blockEntity.prevAngle, blockEntity.angle);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, 0.0, -0.5);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());
        List<BakedQuad> quads = collectQuads(spinnerModel, RenderType.cutout());
        if (!quads.isEmpty()) {
            renderQuads(poseStack, buffer, quads, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderLegacyFluid(MachineCrystallizerBlockEntity blockEntity, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getTank().isEmpty()) return;
        Fluid fluid = blockEntity.getTank().getStoredFluid();
        if (fluid == null) return;

        TextureAtlasSprite sprite = MachineCrystallizerVboRenderer.getFluidSprite(blockEntity, fluid);
        if (sprite == null) return;

        int color = MachineCrystallizerVboRenderer.getFluidTint(blockEntity, fluid);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        BakedModel fluidModel = MachineCrystallizerVboRenderer.getFluidModel();
        if (fluidModel == null) return;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        List<BakedQuad> quads = collectQuads(fluidModel, RenderType.translucent());
        if (!quads.isEmpty()) {
            renderQuadsWithSprite(poseStack, buffer, quads, sprite, r, g, b, 1.0F, packedLight, packedOverlay);
        }
    }

    private static void applyFacingRotation(MachineCrystallizerBlockEntity blockEntity, PoseStack poseStack) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return;

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float rot = switch (facing) {
            case SOUTH -> 180F;
            case EAST  -> 270F;
            case WEST  -> 90F;
            default -> 0F;
        };

        if (rot != 0F) {
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            poseStack.translate(-0.5, 0, -0.5);
        }
    }

    private static List<BakedQuad> collectQuads(BakedModel model, RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        //? if forge {
        quads.addAll(model.getQuads(null, null, RANDOM, ModelData.EMPTY, renderType));
        for (Direction dir : Direction.values()) {
            quads.addAll(model.getQuads(null, dir, RANDOM, ModelData.EMPTY, renderType));
        }
        //?}
        //? if fabric {
        /*quads.addAll(model.getQuads(null, null, RANDOM));
        for (Direction dir : Direction.values()) {
            quads.addAll(model.getQuads(null, dir, RANDOM));
        }
        *///?}
        return quads;
    }

    private static void renderQuads(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads,
                                    float r, float g, float b, float a, int packedLight, int packedOverlay) {
        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            //? if forge {
            buffer.putBulkData(pose, quad, r, g, b, a, packedLight, packedOverlay, true);
            //?} else {
            /*buffer.putBulkData(pose, quad, r, g, b, packedLight, packedOverlay);
            *///?}
        }
    }

    private static void renderQuadsWithSprite(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads,
                                              TextureAtlasSprite sprite, float r, float g, float b, float a,
                                              int packedLight, int packedOverlay) {
        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            BakedQuad reskinned = remapQuadSprite(quad, sprite);
            //? if forge {
            buffer.putBulkData(pose, reskinned, r, g, b, a, packedLight, packedOverlay, true);
            //?} else {
            /*buffer.putBulkData(pose, reskinned, r, g, b, packedLight, packedOverlay);
            *///?}
        }
    }

    private static BakedQuad remapQuadSprite(BakedQuad source, TextureAtlasSprite newSprite) {
        int[] originalVertices = source.getVertices();
        int[] vertices = originalVertices.clone();
        TextureAtlasSprite oldSprite = source.getSprite();

        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);

            float localU = (u - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0());
            float localV = (v - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0());

            float newU = newSprite.getU0() + localU * (newSprite.getU1() - newSprite.getU0());
            float newV = newSprite.getV0() + localV * (newSprite.getV1() - newSprite.getV0());

            vertices[offset + 4] = Float.floatToRawIntBits(newU);
            vertices[offset + 5] = Float.floatToRawIntBits(newV);
        }

        return new BakedQuad(vertices, source.getTintIndex(), source.getDirection(),
                newSprite, source.isShade());
    }

    @Override
    public boolean shouldRenderOffScreen(MachineCrystallizerBlockEntity blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    @Override
    public int getViewDistance() {
        return RenderDistanceHelper.getStaticViewDistanceBlocks();
    }
}
