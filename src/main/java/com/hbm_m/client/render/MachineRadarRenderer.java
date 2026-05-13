package com.hbm_m.client.render;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.MachineRadarBlockEntity;
import com.hbm_m.block.machines.MachineRadarBlock;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class MachineRadarRenderer implements BlockEntityRenderer<MachineRadarBlockEntity> {

    private static final RandomSource RANDOM = RandomSource.create(42L);
        private static final ResourceLocation DISH_MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/radar_dish");
        private static final ResourceLocation LARGE_DISH_MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/radar_large_dish");

    public MachineRadarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineRadarBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation dishModelLocation = blockEntity.getBlockState().is(ModBlocks.LARGE_RADAR.get())
                ? LARGE_DISH_MODEL_LOCATION
                : DISH_MODEL_LOCATION;
        BakedModel dishModel = Minecraft.getInstance().getModelManager().getModel(dishModelLocation);
        BakedModel missingModel = Minecraft.getInstance().getModelManager().getMissingModel();
        if (dishModel == null || dishModel == missingModel) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        float facingYaw = getFacingYaw(state);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw + 180.0F));

        if (blockEntity.getEnergyStored() > 0) {
            float spin = (float) (-((System.currentTimeMillis() / 10L) % 360L));
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        }

        // Legacy offset for the moving dish pivot.
        poseStack.translate(-0.125D, 0.0D, 0.0D);
        poseStack.translate(0.0D, 0.0D, 0.0D);

        renderModel(dishModel, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private float getFacingYaw(BlockState state) {
        if (!state.hasProperty(MachineRadarBlock.FACING)) {
            return 0.0F;
        }
        return switch (state.getValue(MachineRadarBlock.FACING)) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private void renderModel(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        List<BakedQuad> quads = model.getQuads(null, null, RANDOM, ModelData.EMPTY, RenderType.solid());
        putQuads(poseStack, consumer, quads, packedLight, packedOverlay);

        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            quads = model.getQuads(null, direction, RANDOM, ModelData.EMPTY, RenderType.solid());
            putQuads(poseStack, consumer, quads, packedLight, packedOverlay);
        }
    }

    private void putQuads(PoseStack poseStack, VertexConsumer consumer, List<BakedQuad> quads,
                          int packedLight, int packedOverlay) {
        if (quads.isEmpty()) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay, true);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(MachineRadarBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
