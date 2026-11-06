package com.hbm_m.client.render.implementations;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.network.RedCablePaintableBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Camo cable renderer ({@code RedCablePaintableBlock}):
 * pass 0 - the disguised block drawn with its real model, no tint; without a disguise
 * this is the base red cube ({@link ModBlocks#RED_CABLE_PAINTABLE_BASE});
 * pass 1 - the translucent red_cable_overlay veil ({@link ModBlocks#RED_CABLE_PAINTABLE_VEIL},
 * translucent model); in the original it is always drawn (meta 0 = overlay enabled).
 * IMPORTANT: renderSingleBlock skips states with RenderShape.INVISIBLE, so the paintable
 * block itself (INVISIBLE) cannot be rendered - hidden helper blocks with
 * RenderShape.MODEL are used instead.
 */
public class RedCablePaintableRenderer implements BlockEntityRenderer<RedCablePaintableBlockEntity> {

    public RedCablePaintableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RedCablePaintableBlockEntity be, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState camo = be.getCamo();
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        // Pass 0: camo block or the base red cube
        BlockState shown = camo != null ? camo : ModBlocks.RED_CABLE_PAINTABLE_BASE.get().defaultBlockState();
        blockRenderer.renderSingleBlock(shown, poseStack, buffer, light, overlay);
        // Pass 1: veil overlay
        blockRenderer.renderSingleBlock(ModBlocks.RED_CABLE_PAINTABLE_VEIL.get().defaultBlockState(), poseStack, buffer, light, overlay);
    }
}
