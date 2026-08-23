package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.client.model.MachineRadarBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
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
import com.hbm_m.platform.RenderHooks;
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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?}

/**
 * BER радара: при VBO — основание (instanced) + вращающаяся тарелка (instanced / single VBO).
 * Порт {@code RenderRadar} / {@code RenderRadarLarge} (1.7.10).
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineRadarRenderer extends com.hbm_m.client.render.AbstractPartBasedRenderer<MachineRadarBlockEntity, BakedModel> {

    private static final RandomSource RANDOM = RandomSource.create(42L);
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    /** Смещение пивота тарелки малого радара (оригинал {@code glTranslated(-0.125, 0, 0)}). */
    private static final float SMALL_DISH_PIVOT_OFFSET_X = -0.125F;

    private final MachineRadarVboRenderer gpu = new MachineRadarVboRenderer();
    private final Matrix4f matDish = new Matrix4f();

    private static volatile InstancedStaticPartRenderer instancedSmallBase;
    private static volatile InstancedStaticPartRenderer instancedSmallDish;
    private static volatile InstancedStaticPartRenderer instancedLargeBase;
    private static volatile InstancedStaticPartRenderer instancedLargeDish;

    private static volatile boolean loggedModelResolveFailure;

    public MachineRadarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected BakedModel getModelType(BakedModel rawModel) {
        return rawModel;
    }

    @Override
    protected Direction getFacing(MachineRadarBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }

    @Override
    protected void renderParts(MachineRadarBlockEntity blockEntity, BakedModel model,
                               com.hbm_m.client.render.LegacyAnimator animator, float partialTick,
                               int packedLight, int packedOverlay, PoseStack poseStack,
                               MultiBufferSource bufferSource) {
        // Не используется: render() переопределён целиком.
        throw new UnsupportedOperationException();
    }

    @Override
    public void render(MachineRadarBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        com.hbm_m.client.render.LightSampleCache.BASE_POSE.get().set(poseStack.last().pose());
        com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(true);
        try {
            var minecraft = Minecraft.getInstance();
            BlockPos blockPos = blockEntity.getBlockPos();
            // Куллинг + fade: в контрапшене Create shouldRender() пропускает
            // frustum/ray-march кулинг.
            float staticFade = applyCullingAndStaticFade(blockEntity);
            if (staticFade < 0) {
                return;
            }

            MachineRadarBakedModel model = resolveRadarModel(minecraft.getBlockRenderer().getBlockModel(blockEntity.getBlockState()));
            if (model == null) {
                if (!loggedModelResolveFailure) {
                    loggedModelResolveFailure = true;
                    MainRegistry.LOGGER.error(
                            "MachineRadarRenderer: block model is not MachineRadarBakedModel ({}), radar invisible under VBO",
                            minecraft.getBlockRenderer().getBlockModel(blockEntity.getBlockState()).getClass().getName());
                }
                return;
            }

            boolean powered = blockEntity.isActive() || blockEntity.getEnergyStored() > 0;

            poseStack.pushPose();
            applyFacingRotation(blockEntity, poseStack);

            if (ShaderCompatibilityDetector.useVboGeometry()) {
                SingleMeshVboRenderer.setFadeAlpha(staticFade);
                int dynamicLight = LightTexture.pack(LightTexture.block(packedLight), LightTexture.sky(packedLight));
                renderRadarVbo(blockEntity, partialTick, poseStack, dynamicLight, blockPos, bufferSource, staticFade, model, powered);
            } else {
                renderLegacyDish(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, model, powered);
            }

            poseStack.popPose();
        } finally {
            com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(false);
        }
    }

    @Nullable
    private static MachineRadarBakedModel resolveRadarModel(BakedModel raw) {
        raw = AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw);
        if (raw instanceof MachineRadarBakedModel radarModel) {
            return radarModel;
        }
        return null;
    }

    private void renderRadarVbo(MachineRadarBlockEntity be, float partialTick, PoseStack poseStack,
                                int dynamicLight, BlockPos blockPos, MultiBufferSource bufferSource,
                                float staticFade, MachineRadarBakedModel model, boolean powered) {
        ensureInstancers(model);

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive();

        Runnable draw = () -> drawRadarParts(be, partialTick, poseStack, dynamicLight, blockPos, bufferSource,
                staticFade, model, powered);

        if (useIrisBatch) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                draw.run();
            }
        } else {
            draw.run();
        }
    }

    private void drawRadarParts(MachineRadarBlockEntity be, float partialTick, PoseStack poseStack,
                                int dynamicLight, BlockPos blockPos, MultiBufferSource bufferSource,
                                float staticFade, MachineRadarBakedModel model, boolean powered) {
        boolean large = model.isLargeRadar();
        InstancedStaticPartRenderer baseInstancer = large ? instancedLargeBase : instancedSmallBase;
        InstancedStaticPartRenderer dishInstancer = large ? instancedLargeDish : instancedSmallDish;

        boolean useBatching = ModClothConfig.useInstancedBatching();
        // BE-оверлоад: bypass fade/cull для контрапшенов и Sable sublevel
        // (raw BlockPos в sublevel ~40M от origin → distanceSqToCamera гигантский → fade=-1).
        float animFade = RenderDistanceHelper.computeAnimatedFade(be);
        boolean anyFading = staticFade < 0.99f || (animFade >= 0 && animFade < 0.99f);
        boolean effectiveBatching = useBatching && !anyFading;

        float baseFade = staticFade;
        SingleMeshVboRenderer.setFadeAlpha(baseFade);
        drawStaticPart(poseStack, dynamicLight, blockPos, be, bufferSource, model, baseInstancer, effectiveBatching);

        if (animFade >= 0) {
            float dishFade = Math.min(staticFade, animFade);
            SingleMeshVboRenderer.setFadeAlpha(dishFade);
            drawDishPart(be, partialTick, poseStack, dynamicLight, blockPos, bufferSource, model, dishInstancer,
                    effectiveBatching, powered, large);
        }

        SingleMeshVboRenderer.setFadeAlpha(staticFade);
    }

    private void drawStaticPart(PoseStack poseStack, int dynamicLight, BlockPos blockPos,
                                MachineRadarBlockEntity be, MultiBufferSource bufferSource,
                                MachineRadarBakedModel model, @Nullable InstancedStaticPartRenderer instancer,
                                boolean effectiveBatching) {
        if (effectiveBatching && instancer != null && instancer.isInitialized()) {
            instancer.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
        } else {
            gpu.renderStatic(poseStack, dynamicLight, blockPos, be, bufferSource, model);
        }
    }

    private void drawDishPart(MachineRadarBlockEntity be, float partialTick, PoseStack poseStack,
                              int dynamicLight, BlockPos blockPos, MultiBufferSource bufferSource,
                              MachineRadarBakedModel model, @Nullable InstancedStaticPartRenderer instancer,
                              boolean effectiveBatching, boolean powered, boolean large) {
        buildDishTransform(be, partialTick, large, powered);

        if (effectiveBatching && instancer != null && instancer.isInitialized()) {
            poseStack.pushPose();
            poseStack.last().pose().mul(matDish);
            instancer.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
            poseStack.popPose();
        } else {
            poseStack.pushPose();
            poseStack.last().pose().mul(matDish);
            gpu.renderDish(poseStack, dynamicLight, blockPos, be, bufferSource, model);
            poseStack.popPose();
        }
    }

    private void buildDishTransform(MachineRadarBlockEntity be, float partialTick, boolean large, boolean powered) {
        float angle = powered ? Mth.lerp(partialTick, be.prevRotation, be.rotation) : be.rotation;
        matDish.identity();
        matDish.translate(0.5f, 0f, 0.5f);
        matDish.rotateY(-angle * DEG_TO_RAD);
        if (!large) {
            matDish.translate(SMALL_DISH_PIVOT_OFFSET_X, 0f, 0f);
        }
        matDish.translate(-0.5f, 0f, -0.5f);
    }

    private static void renderLegacyDish(MachineRadarBlockEntity blockEntity, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, MachineRadarBakedModel model,
                                         boolean powered) {
        BakedModel dishModel = model.getPart("Dish");
        if (dishModel == null) {
            return;
        }

        float angle = powered ? Mth.lerp(partialTick, blockEntity.prevRotation, blockEntity.rotation) : blockEntity.rotation;
        boolean large = model.isLargeRadar();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YN.rotationDegrees(angle));
        if (!large) {
            poseStack.translate(SMALL_DISH_PIVOT_OFFSET_X, 0.0, 0.0);
        }
        poseStack.translate(-0.5, 0.0, -0.5);

        List<BakedQuad> quads = collectQuads(dishModel, RenderType.cutout());
        RenderType renderType = RenderType.cutout();
        if (quads.isEmpty()) {
            quads = collectQuads(dishModel, RenderType.solid());
            renderType = RenderType.solid();
        }
        if (!quads.isEmpty()) {
            VertexConsumer buffer = bufferSource.getBuffer(renderType);
            renderQuads(poseStack, buffer, quads, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void applyFacingRotation(MachineRadarBlockEntity blockEntity, PoseStack poseStack) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float rot = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 270F;
            case WEST -> 90F;
            default -> 0F;
        };

        if (rot != 0F) {
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            poseStack.translate(-0.5, 0, -0.5);
        }
    }

    private static synchronized void ensureInstancers(MachineRadarBakedModel model) {
        boolean large = model.isLargeRadar();
        if (large) {
            if (instancedLargeBase == null) {
                instancedLargeBase = createInstancer(model, model.getStaticPartName(), "Radar/LargeBase");
            }
            if (instancedLargeDish == null) {
                instancedLargeDish = createInstancer(model, "Dish", "Radar/LargeDish");
            }
        } else {
            if (instancedSmallBase == null) {
                instancedSmallBase = createInstancer(model, model.getStaticPartName(), "Radar/SmallBase");
            }
            if (instancedSmallDish == null) {
                instancedSmallDish = createInstancer(model, "Dish", "Radar/SmallDish");
            }
        }
    }

    @Nullable
    private static InstancedStaticPartRenderer createInstancer(MachineRadarBakedModel model, String partName, String traceTag) {
        BakedModel part = model.getPart(partName);
        if (part == null) {
            MainRegistry.LOGGER.warn("MachineRadarRenderer: missing part {}", partName);
            return null;
        }

        String cacheKey = MachineRadarVboRenderer.partCacheKey(model, partName);
        PartGeometry geo = MeshRenderCache.getOrCompilePartGeometry(cacheKey, part);
        if (geo.isEmpty()) {
            MainRegistry.LOGGER.warn("MachineRadarRenderer: {} geometry empty after compile", partName);
            return null;
        }

        var data = geo.toVboData(partName);
        if (data == null) {
            return null;
        }

        InstancedStaticPartRenderer instancer = new InstancedStaticPartRenderer(data, geo.solidQuads());
        instancer.setMdiTraceTag(traceTag);
        if (instancer.isInitialized()) {
            MainRegistry.LOGGER.info("MachineRadarRenderer: instancer ready for {} ({} quads)", partName, geo.solidQuads().size());
            return instancer;
        }
        return null;
    }

    public static void flushInstancedBatches(org.joml.Matrix4f projectionMatrix) {
        if (instancedSmallBase != null) {
            instancedSmallBase.flush(projectionMatrix);
        }
        if (instancedSmallDish != null) {
            instancedSmallDish.flush(projectionMatrix);
        }
        if (instancedLargeBase != null) {
            instancedLargeBase.flush(projectionMatrix);
        }
        if (instancedLargeDish != null) {
            instancedLargeDish.flush(projectionMatrix);
        }
    }

    public static void clearCaches() {
        cleanupInstancer(instancedSmallBase);
        instancedSmallBase = null;
        cleanupInstancer(instancedSmallDish);
        instancedSmallDish = null;
        cleanupInstancer(instancedLargeBase);
        instancedLargeBase = null;
        cleanupInstancer(instancedLargeDish);
        instancedLargeDish = null;
        loggedModelResolveFailure = false;
    }

    private static void cleanupInstancer(@Nullable InstancedStaticPartRenderer instancer) {
        if (instancer != null) {
            instancer.cleanup();
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
                                    int packedLight, int packedOverlay) {
        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            RenderHooks.putBulkData(buffer, pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay, true);
        }
    }
}
