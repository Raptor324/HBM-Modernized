package com.hbm_m.client.render.implementations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.hbm_m.blockentity.decorations.PedestalBlockEntity;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
/**
 * Рендер постамента (порт 1.7.10 RenderPedestalTile): предмет парит над
 * постаментом с масштабом 1.5x. Блок-предметы не вращаются и висят чуть
 * выше; предметы-не-блоки медленно вращаются.
 */
public class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity> {

    public PedestalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PedestalBlockEntity pedestal, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = pedestal.getItem();
        if (stack.isEmpty()) return;

        Level level = pedestal.getLevel();
        float time = level != null ? (level.getGameTime() % 200000L) + partialTick : 0.0F;
        boolean isBlockItem = stack.getItem() instanceof BlockItem;

        pose.pushPose();
        pose.translate(0.5D, 1.15D + Mth.sin(time * 0.1F) * 0.0625D + (isBlockItem ? 0.0625D : 0.0D), 0.5D);
        if (!isBlockItem) pose.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
        pose.scale(1.5F, 1.5F, 1.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, pedestal.getLevel(), 0);
        pose.popPose();
    }
}
