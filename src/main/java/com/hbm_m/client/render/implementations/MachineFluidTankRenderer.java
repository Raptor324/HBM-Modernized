package com.hbm_m.client.render.implementations;

import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.block.machines.MachineFluidTankBlock;
import com.hbm_m.client.render.util.DiamondPronter;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
/**
 * Hazard diamonds on fluid tank sides (1.7.10 {@code RenderFluidTank} diamond pass).
 * Tank frame/glass uses {@link com.hbm_m.client.model.MachineFluidTankBakedModel}.
 */
public class MachineFluidTankRenderer implements BlockEntityRenderer<MachineFluidTankBlockEntity> {

    public MachineFluidTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            MachineFluidTankBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        Fluid fluid = be.getFluidTank().getTankType();
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            return;
        }

        FluidType type = FluidType.forFluid(fluid);

        BlockPos pos = be.getBlockPos();
        int light = LevelRenderer.getLightColor(be.getLevel(), pos.above(2));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);

        BlockState state = be.getBlockState();
        if (state.hasProperty(MachineFluidTankBlock.FACING)) {
            Direction facing = state.getValue(MachineFluidTankBlock.FACING);
            poseStack.mulPose(Axis.YP.rotationDegrees(MultipartFacingTransforms.vanillaChunkMeshRotationY(facing)));
        }

        RenderSystem.disableCull();

        poseStack.pushPose();
        poseStack.translate(-0.25F, 0.5F, -1.501F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(1.0F, 0.375F, 0.375F);
        DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.25F, 0.5F, 1.501F);
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.scale(1.0F, 0.375F, 0.375F);
        DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);
        poseStack.popPose();

        RenderSystem.enableCull();
        poseStack.popPose();
    }
}
