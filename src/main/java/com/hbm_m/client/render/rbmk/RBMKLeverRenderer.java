package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;

/** 1:1 port of {@code RenderRBMKLever}: two levers that swing through 180° as they flip. */
public class RBMKLeverRenderer extends RBMKPanelRenderer<RBMKLeverBlockEntity> {

    public RBMKLeverRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RBMKLeverBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int light, int overlay) {
        Map<String, List<float[]>> obj = model("lever");
        TextureAtlasSprite tex = panelSprite("lever");

        begin(be, ps);

        for (int i = 0; i < RBMKLeverBlockEntity.UNITS; i++) {
            if (!be.isUnitActive(i)) continue;

            ps.pushPose();
            ps.translate(0.25, 0, i * -0.5 + 0.25);

            part(ps, buf, obj, "Base", tex, 1f, 1f, 1f, light, overlay);

            float progress = (float) Mth.lerp(pt, be.prevFlip[i], be.flip[i]);
            ps.pushPose();
            ps.translate(0.125, 0.5625, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-180f * progress));
            ps.translate(-0.125, -0.5625, 0);
            part(ps, buf, obj, "Lever", tex, 1f, 1f, 1f, light, overlay);
            ps.popPose();

            ps.translate(0.01, 0.0625, 0);
            label(ps, buf, be.getUnitLabel(i), 0.4f, 0x00FF00, FULLBRIGHT);

            ps.popPose();
        }

        ps.popPose();
    }
}
