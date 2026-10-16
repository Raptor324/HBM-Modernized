package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKKeyPadBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.List;
import java.util.Map;

/** 1:1 port of {@code RenderRBMKKeyPad}: four buttons that sink in and glow while pressed. */
public class RBMKKeyPadRenderer extends RBMKPanelRenderer<RBMKKeyPadBlockEntity> {

    public RBMKKeyPadRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RBMKKeyPadBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int light, int overlay) {
        Map<String, List<float[]>> obj = model("button");
        TextureAtlasSprite tex = panelSprite("keypad");

        begin(be, ps);

        for (int i = 0; i < RBMKKeyPadBlockEntity.UNITS; i++) {
            if (!be.isUnitActive(i)) continue;

            boolean glow = be.isPressed[i];
            float mult = glow ? 1f : 0.65f;

            ps.pushPose();
            ps.translate(0.25, (i / 2) * -0.5 + 0.25, (i % 2) * -0.5 + 0.25);

            part(ps, buf, obj, "Socket", tex, 1f, 1f, 1f, light, overlay);

            ps.pushPose();
            ps.translate(glow ? -0.03125 : 0, 0, 0);
            int color = be.getUnitColor(i);
            part(ps, buf, obj, "Button", tex,
                    red(color) * mult, green(color) * mult, blue(color) * mult,
                    glow ? FULLBRIGHT : light, overlay);
            ps.popPose();

            ps.translate(0.01, 0.3125, 0);
            label(ps, buf, be.getUnitLabel(i), 0.4f, 0x00FF00, FULLBRIGHT);

            ps.popPose();
        }

        ps.popPose();
    }
}
