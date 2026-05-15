package com.hbm_m.client.render.implementations;

import com.hbm_m.block.entity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.item.missile.MissileItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class LaunchPadMissileRenderer implements BlockEntityRenderer<LaunchPadBaseBlockEntity> {

    private static final double PAD_OFFSET_X = 0.5D;
    private static final double PAD_OFFSET_Y = 0.98D;
    private static final double PAD_OFFSET_Z = 0.5D;
    private static final float PAD_SCALE = 1.0F;

    public LaunchPadMissileRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LaunchPadBaseBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack missileStack = be.getInventory().getStackInSlot(LaunchPadBaseBlockEntity.SLOT_MISSILE);
        if (missileStack.isEmpty() || !(missileStack.getItem() instanceof MissileItem)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(PAD_OFFSET_X, PAD_OFFSET_Y, PAD_OFFSET_Z);

        if (be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            float yaw = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        }

        poseStack.scale(PAD_SCALE, PAD_SCALE, PAD_SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                missileStack,
            ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                be.getLevel(),
                (int) be.getBlockPos().asLong()
        );

        poseStack.popPose();
    }
}