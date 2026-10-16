package com.hbm_m.client.render.implementations;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.network.RedCablePaintableBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Рендер кабеля-камуфляжа (RedCablePaintableBlock):
 * пасс 0 — замаскированный блок его настоящей моделью, без окраски — базовый красный куб
 * ({@link ModBlocks#RED_CABLE_PAINTABLE_BASE});
 * пасс 1 — полупрозрачная вуаль red_cable_overlay ({@link ModBlocks#RED_CABLE_PAINTABLE_VEIL},
 * модель translucent), в оригинале рисуется всегда (мета 0 = overlay включён).
 * ВАЖНО: renderSingleBlock пропускает состояния с RenderShape.INVISIBLE, поэтому сам paintable
 * (INVISIBLE) рендерить нельзя — используются скрытые хелпер-блоки с RenderShape.MODEL.
 */
public class RedCablePaintableRenderer implements BlockEntityRenderer<RedCablePaintableBlockEntity> {

    public RedCablePaintableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RedCablePaintableBlockEntity be, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState camo = be.getCamo();
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        // Пасс 0: камуфляж или базовый красный куб
        BlockState shown = camo != null ? camo : ModBlocks.RED_CABLE_PAINTABLE_BASE.get().defaultBlockState();
        blockRenderer.renderSingleBlock(shown, poseStack, buffer, light, overlay);
        // Пасс 1: вуаль-оверлей
        blockRenderer.renderSingleBlock(ModBlocks.RED_CABLE_PAINTABLE_VEIL.get().defaultBlockState(), poseStack, buffer, light, overlay);
    }
}
