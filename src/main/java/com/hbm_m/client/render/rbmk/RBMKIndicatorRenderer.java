package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKIndicatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.List;
import java.util.Map;

/** 1:1 port of {@code RenderRBMKIndicator}: six lamps, lit or dimmed to 35%. */
public class RBMKIndicatorRenderer extends RBMKPanelRenderer<RBMKIndicatorBlockEntity> {

    public RBMKIndicatorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RBMKIndicatorBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int light, int overlay) {
        Map<String, List<float[]>> obj = model("indicator");
        TextureAtlasSprite tex = panelSprite("indicator");

        begin(be, ps);

        for (int i = 0; i < RBMKIndicatorBlockEntity.UNITS; i++) {
            if (!be.isUnitActive(i)) continue;

            ps.pushPose();
            ps.translate(0.25, (i / 2) * -0.3125 + 0.3125, (i % 2) * -0.5 + 0.25);

            part(ps, buf, obj, "Base", tex, 1f, 1f, 1f, light, overlay);

            boolean lit = be.state[i];
            float mult = lit ? 1f : 0.35f;
            int color = be.getUnitColor(i);
            part(ps, buf, obj, "Light", tex,
                    red(color) * mult, green(color) * mult, blue(color) * mult,
                    lit ? FULLBRIGHT : light, overlay);

            ps.translate(0.0725, 0.5, 0);
            label(ps, buf, be.getUnitLabel(i), 0.3f, 0x000000, light);

            ps.popPose();
        }

        ps.popPose();
    }
}
