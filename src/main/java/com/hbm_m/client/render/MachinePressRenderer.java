package com.hbm_m.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.block.custom.machines.MachinePressBlock;
import com.hbm_m.block.entity.custom.machines.MachinePressBlockEntity;
import com.hbm_m.client.model.PressBakedModel;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MachinePressRenderer extends AbstractPartBasedRenderer<MachinePressBlockEntity, PressBakedModel> {

    private static final String HEAD_PART = "Head";

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float HEAD_SCALE = 0.983F;
    private static final float WORKPIECE_HEIGHT = 1.125F + PIXEL;
    private static final float WORKPIECE_SCALE = 0.55F;
    private static final float STAMP_SCALE = 0.65F;

    private MachinePressVboRenderer gpuRenderer;
    private PressBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedHead;
    private static volatile boolean instancerInitialized = false;

    public MachinePressRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    protected PressBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof PressBakedModel pressModel ? pressModel : null;
    }

    @Override
    protected Direction getFacing(MachinePressBlockEntity blockEntity) {
        return blockEntity.getBlockState().getValue(MachinePressBlock.FACING);
    }

    @Override
    protected void renderParts(MachinePressBlockEntity blockEntity, PressBakedModel model,
                               LegacyAnimator animator, float partialTick, int packedLight,
                               int packedOverlay, PoseStack poseStack, MultiBufferSource bufferSource) {
        BlockPos blockPos = blockEntity.getBlockPos();
        AABB bounds = blockEntity.getRenderBoundingBox();

        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, bounds)) {
            return;
        }

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        Matrix4f headTransform = renderWithVbo(blockEntity, model, poseStack, dynamicLight, partialTick, blockPos, bufferSource);

        renderStampItem(blockEntity, poseStack, bufferSource, dynamicLight, packedOverlay, headTransform);
        renderWorkpiece(blockEntity, model, poseStack, bufferSource, dynamicLight, packedOverlay);

        if (bufferSource instanceof MultiBufferSource.BufferSource bufferSrc) {
            bufferSrc.endBatch();
        }
    }

    private Matrix4f renderWithVbo(MachinePressBlockEntity blockEntity, PressBakedModel model,
                               PoseStack poseStack, int packedLight, float partialTick, BlockPos blockPos,
                               MultiBufferSource bufferSource) {
        if (!instancerInitialized) {
            initializeInstancedHead(model);
        }

        if (cachedModel != model || gpuRenderer == null) {
            cachedModel = model;
            gpuRenderer = new MachinePressVboRenderer(model);
        }

        // Base рендерится через BlockState/BakedModel (запечён в чанк Embeddium/Sodium)

        boolean freezeAnimation = shouldSkipAnimationUpdate(blockPos);
        Matrix4f headTransform = buildHeadTransform(model, blockEntity, partialTick, freezeAnimation);
        boolean useInstancedHead = instancedHead != null && instancedHead.isInitialized();
        boolean useBatching = ModClothConfig.useInstancedBatching();
        boolean inShadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        if (useBatching && !inShadowPass && useInstancedHead) {
            poseStack.pushPose();
            poseStack.last().pose().mul(headTransform);
            instancedHead.addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            poseStack.popPose();
        } else {
            if (instancedHead != null && !instancedHead.isInitialized()) {
                MainRegistry.LOGGER.warn("MachinePressRenderer: instancedHead exists but NOT initialized, using fallback");
            }
            gpuRenderer.renderAnimatedHead(poseStack, packedLight, headTransform, blockPos, blockEntity, bufferSource);
        }
        return headTransform;
    }

    private Matrix4f buildHeadTransform(PressBakedModel model, MachinePressBlockEntity blockEntity,
                                        float partialTick, boolean freezeAnimation) {
        Vector3f rest = model.getHeadRestOffset();
        float travel = model.getHeadTravelDistance();
        float progress = freezeAnimation ? 0.0F : blockEntity.getPressAnimationProgress(partialTick);
        float effectiveTravel = Math.max(0.0F, travel - PIXEL);
        float offsetY = rest.y() - (progress * effectiveTravel);
        return new Matrix4f()
            .translate(rest.x(), offsetY, rest.z())
            .scale(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
    }

    private boolean shouldSkipAnimationUpdate(BlockPos blockPos) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();

        double dx = blockPos.getX() + 0.5 - cameraPos.x;
        double dy = blockPos.getY() + 0.5 - cameraPos.y;
        double dz = blockPos.getZ() + 0.5 - cameraPos.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        int thresholdChunks = ModClothConfig.get().modelUpdateDistance;
        double thresholdBlocks = thresholdChunks * 16.0;
        double thresholdSquared = thresholdBlocks * thresholdBlocks;

        return distanceSquared > thresholdSquared;
    }

    private void renderWorkpiece(
            MachinePressBlockEntity blockEntity,
            PressBakedModel model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack stack = blockEntity.getMaterialStack();
        if (stack.isEmpty()) {
            return;
        }

        var mc = Minecraft.getInstance();

        poseStack.pushPose();
        // Локальное (0, 0) здесь — центр блока после setupBlockTransform
        poseStack.translate(0.32F, WORKPIECE_HEIGHT, 0.32F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        poseStack.pushPose();
        poseStack.scale(WORKPIECE_SCALE, WORKPIECE_SCALE, WORKPIECE_SCALE);
        poseStack.translate(-0.5F, -0.5F, 0.32F);

        mc.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong()
        );

        poseStack.popPose();
        poseStack.popPose();
    }

    private void renderStampItem(
            MachinePressBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Matrix4f headTransform
    ) {
        ItemStack stamp = blockEntity.getStampStack();
        if (stamp.isEmpty() || headTransform == null) {
            return;
        }

        var mc = Minecraft.getInstance();

        // Достаём только трансляцию головы
        Vector3f headPos = new Vector3f();
        headTransform.getTranslation(headPos);

        poseStack.pushPose();
        // Центр блока по X/Z, высота – как у головы, плюс небольшой отступ
        poseStack.translate(0.32F, headPos.y() + 0.98F, 0.32F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        poseStack.pushPose();
        poseStack.scale(STAMP_SCALE, STAMP_SCALE, STAMP_SCALE);
        poseStack.translate(-0.5F, -0.5F, 0.0F);

        mc.getItemRenderer().renderStatic(
                stamp,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong()
        );

        poseStack.popPose();
        poseStack.popPose();
    }

    private static synchronized void initializeInstancedHead(PressBakedModel model) {
        if (instancerInitialized) {
            return;
        }
        try {
            BakedModel headModel = model.getPart(HEAD_PART);
            if (headModel != null) {
                var headData = ObjModelVboBuilder.buildSinglePart(headModel, HEAD_PART);
                if (headData != null) {
                    var headQuads = GlobalMeshCache.getOrCompile("press_head", headModel);
                    instancedHead = new InstancedStaticPartRenderer(headData, headQuads);
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize press instanced renderer", e);
            instancedHead = null;
        } finally {
            instancerInitialized = true;
        }
    }

    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (instancedHead != null) instancedHead.flush(event);
    }

    public static void clearCaches() {
        if (instancedHead != null) {
            instancedHead.cleanup();
            instancedHead = null;
        }
        instancerInitialized = false;
    }
}

