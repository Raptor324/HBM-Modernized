package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.SoyuzCapsuleBlock;
import com.hbm_m.blockentity.machines.SoyuzCapsuleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** 1:1 {@code RenderCapsule}: gelandete Kapsel, schraeg im Boden (-25 Grad Gier, 15 Grad Neigung), rostig bei Meta 3. */
public class SoyuzCapsuleRenderer implements BlockEntityRenderer<SoyuzCapsuleBlockEntity> {

    public SoyuzCapsuleRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(SoyuzCapsuleBlockEntity te, float interp, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);

        ps.translate(0.0F, -0.25F, 0.0F);
        ps.mulPose(Axis.YP.rotationDegrees(-25));
        ps.mulPose(Axis.ZP.rotationDegrees(15));

        boolean rust = te.getBlockState().hasProperty(SoyuzCapsuleBlock.RUSTED) && te.getBlockState().getValue(SoyuzCapsuleBlock.RUSTED);
        SoyuzCapsuleEntityRenderer.SOYUZ_LANDER.renderPart("Capsule", ps,
                buffers.getBuffer(RenderType.entityCutout(rust ? SoyuzCapsuleEntityRenderer.LANDER_RUST_TEX : SoyuzCapsuleEntityRenderer.LANDER_TEX)), light);

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SoyuzCapsuleBlockEntity te) {
        return true;
    }
}
