package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderRegistry;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.item.missile.MissileItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
public class LaunchPadMissileRenderer implements com.hbm_m.client.render.HbmBerBounds<LaunchPadBaseBlockEntity> {

    public LaunchPadMissileRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LaunchPadBaseBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack missileStack = be.getMissilePreviewStack();
        if (missileStack.isEmpty() || !(missileStack.getItem() instanceof MissileItem)) {
            return;
        }

        MissileRenderData renderData = MissileRenderRegistry.get(missileStack);
        if (renderData == null) {
            return;
        }

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        // Gate removed: the IrisRenderBatch path was Forge-only; on NeoForge the pad missile
        // always went through the slow path without batching.
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                drawMissileOnPad(be, renderData, poseStack, buffer, packedLight, partialTicks);
            }
            return;
        }

        drawMissileOnPad(be, renderData, poseStack, buffer, packedLight, partialTicks);
    }

    /**
     * Missile position in local block space, measured after FACING rotation - where
     * -Z always points in the launch pad's facing direction. Default is block center, one block high.
     */
    protected net.minecraft.world.phys.Vec3 missileOffset(LaunchPadBaseBlockEntity be, float partialTicks) {
        return new net.minecraft.world.phys.Vec3(0.0D, 1.0D, 0.0D);
    }

    /** Scale factor of the rendered missile. Launch pads with tight gantries scale down here. */
    protected float missileScale() {
        return 1.0F;
    }

    /**
     * Missile tilt around its pivot, in degrees. 0 = vertical; positive values tilt backward
     * away from facing direction, at 90 it lies horizontal. Pads with erectors report progress here.
     */
    protected float missilePitch(LaunchPadBaseBlockEntity be, float partialTicks) {
        return 0.0F;
    }

    private void drawMissileOnPad(LaunchPadBaseBlockEntity be, MissileRenderData renderData,
                                  PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                  float partialTicks) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);

        Direction facing = Direction.NORTH;
        if (be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        switch (facing) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }

        net.minecraft.world.phys.Vec3 offset = missileOffset(be, partialTicks);
        poseStack.translate(offset.x, offset.y, offset.z);
        float pitch = missilePitch(be, partialTicks);
        if (pitch != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        }
        float scale = missileScale();
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }

        renderData.render(poseStack, packedLight, be.getBlockPos(), buffer, be);
        poseStack.popPose();
    }
}
