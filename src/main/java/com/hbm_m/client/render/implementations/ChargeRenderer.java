package com.hbm_m.client.render.implementations;

import com.hbm_m.block.bomb.BlockChargeBase;
import com.hbm_m.blockentity.bomb.ChargeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** 1:1 {@code RenderExplosiveCharge}: gruene Zeitanzeige "mm:ss" auf der Ladung. */
public class ChargeRenderer implements com.hbm_m.client.render.HbmBerBounds<ChargeBlockEntity> {

    public ChargeRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(ChargeBlockEntity charge, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0.5D, 0.5D);

        switch (charge.getBlockState().getValue(BlockChargeBase.FACING)) {
            case DOWN -> ps.mulPose(Axis.ZP.rotationDegrees(180));
            case UP -> { }
            case NORTH -> { ps.mulPose(Axis.YP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(-90)); }
            case SOUTH -> { ps.mulPose(Axis.YP.rotationDegrees(-90)); ps.mulPose(Axis.ZP.rotationDegrees(-90)); }
            case WEST -> { ps.mulPose(Axis.YP.rotationDegrees(180)); ps.mulPose(Axis.ZP.rotationDegrees(-90)); }
            case EAST -> ps.mulPose(Axis.ZP.rotationDegrees(-90));
        }

        String text = charge.getMinutes() + ":" + charge.getSeconds();
        Font font = Minecraft.getInstance().font;
        float f3 = 0.0125F;
        ps.translate(-0.05F, 0.315F - 0.5F, 0.15F);
        ps.scale(f3, -f3, f3);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.mulPose(Axis.XP.rotationDegrees(90));
        font.drawInBatch(text, 0, 0, 0x00ff00, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        ps.popPose();
    }
}
