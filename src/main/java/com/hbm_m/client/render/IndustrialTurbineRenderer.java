package com.hbm_m.client.render;


import com.hbm_m.block.entity.machines.MachineIndustrialTurbineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
*///?}
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;//?}

/**
 * Renderer for the Industrial Turbine.
 * Currently a placeholder — the static model is rendered via RenderShape.MODEL.
 * TODO: Animate the Flywheel OBJ group spinning when the turbine is active.
 */
//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)//?}
public class IndustrialTurbineRenderer implements BlockEntityRenderer<MachineIndustrialTurbineBlockEntity> {

    public IndustrialTurbineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineIndustrialTurbineBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // TODO: Render animated flywheel rotation when blockEntity.isActive()
        // The flywheel rotation angle is available via blockEntity.getAnim(partialTick)
    }

    @Override
    public boolean shouldRenderOffScreen(MachineIndustrialTurbineBlockEntity blockEntity) {
        return true;
    }

}
