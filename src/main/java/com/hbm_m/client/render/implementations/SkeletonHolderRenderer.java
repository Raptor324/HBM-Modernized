package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.SkeletonHolderBlock;
import com.hbm_m.blockentity.decorations.SkeletonHolderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * {@code RenderSkeletonHolder}: das Skelett ist das statische Blockmodell, hier nur der gehaltene Gegenstand wie
 * {@code RenderItem.renderInFrame} (Rahmendarstellung), Nicht-Bloecke 1.5-fach, 0.125 angehoben.
 */
public class SkeletonHolderRenderer implements BlockEntityRenderer<SkeletonHolderBlockEntity> {

    public SkeletonHolderRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(SkeletonHolderBlockEntity te, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = te.getItem();
        if (stack.isEmpty()) return;

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        switch (te.getBlockState().getValue(SkeletonHolderBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }
        ps.mulPose(Axis.YP.rotationDegrees(90));
        if (!(stack.getItem() instanceof BlockItem)) ps.scale(1.5F, 1.5F, 1.5F);
        ps.translate(0.0D, 0.125D, 0.0D);
        // EntityItem im Rahmenmodus: halbe Groesse, auf dem Ursprung stehend
        ps.translate(0.0D, 0.25D, 0.0D);
        ps.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buffers, te.getLevel(), 0);
        ps.popPose();
    }
}
