package com.hbm_m.client.render.implementations;


import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Renderer for the Industrial Turbine.
 * Currently a placeholder — the static model is rendered via RenderShape.MODEL.
 * TODO: Animate the Flywheel OBJ group spinning when the turbine is active.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
