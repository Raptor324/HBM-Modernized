package com.hbm_m.client.render.implementations;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

import com.hbm_m.blockentity.decorations.DecoLootBlockEntity;
import com.hbm_m.client.render.HbmBerBounds;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
/**
 * Рендер «груды лута» (порт 1.7.10 RenderLoot, упрощённый без спец-моделей
 * мини-нюка/дробовика/брони): каждый предмет лежит плашмя на земле в своей
 * сохранённой позиции.
 */
public class DecoLootRenderer implements HbmBerBounds<DecoLootBlockEntity> {

    public DecoLootRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DecoLootBlockEntity loot, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        int seed = 0;
        for (DecoLootBlockEntity.LootEntry entry : loot.getItems()) {
            if (entry.stack().isEmpty()) continue;
            pose.pushPose();
            pose.translate(0.5D + entry.dx(), entry.dy(), 0.5D + entry.dz());
            pose.scale(0.5F, 0.5F, 0.5F);
            itemRenderer.renderStatic(entry.stack(), ItemDisplayContext.GROUND,
                    light, OverlayTexture.NO_OVERLAY, pose, buffers, loot.getLevel(), seed++);
            pose.popPose();
        }
    }
}
