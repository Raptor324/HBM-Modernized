package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKGaugeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;

/** 1:1 port of {@code RenderRBMKGauge}: up to four needle gauges on one panel face. */
public class RBMKGaugeRenderer extends RBMKPanelRenderer<RBMKGaugeBlockEntity> {

    public RBMKGaugeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RBMKGaugeBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int light, int overlay) {
        Map<String, List<float[]>> obj = model("gauge");
        TextureAtlasSprite tex = panelSprite("gauge");

        begin(be, ps);

        for (int i = 0; i < RBMKGaugeBlockEntity.UNITS; i++) {
            if (!be.isUnitActive(i)) continue;

            ps.pushPose();
            ps.translate(0.25, (i / 2) * -0.5 + 0.25, (i % 2) * -0.5 + 0.25);

            part(ps, buf, obj, "Gauge", tex, 1f, 1f, 1f, light, overlay);

            // ── Needle ────────────────────────────────────────────────────────
            int color = be.getUnitColor(i);
            double value = Mth.lerp(pt, be.lastValue[i], be.value[i]);
            double lower = Math.min(be.min[i], be.max[i]);
            double upper = Math.max(be.min[i], be.max[i]);
            if (lower == upper) upper += 1;
            double angle = (value - lower) / (upper - lower) * 50D;
            if (be.min[i] > be.max[i]) angle = 50 - angle;
            angle = Mth.clamp(angle, 0, 80);

            ps.pushPose();
            ps.translate(0, 0.4375, -0.125);
            ps.mulPose(Axis.XN.rotationDegrees((float) (angle - 85)));
            ps.translate(0, -0.4375, 0.125);
            part(ps, buf, obj, "Needle", tex,
                    red(color), green(color), blue(color), FULLBRIGHT, overlay);
            ps.popPose();

            // ── Scale end labels, printed along the dial arc ──────────────────
            String lineLower = shortNumber(be.min[i]);
            String lineUpper = shortNumber(be.max[i]);
            for (int j = 0; j < 2; j++) {
                ps.pushPose();
                ps.translate(0, 0.4375, -0.125);
                ps.mulPose(Axis.XN.rotationDegrees(10 + j * 50));
                ps.translate(0, -0.4375, 0.125);
                ps.translate(0.032, 0.4375, 0.125);
                labelRaw(ps, buf, j == 0 ? lineLower : lineUpper, 0.0025f, 0f, 0x000000, light);
                ps.popPose();
            }

            ps.translate(0.01, 0.3125, 0);
            label(ps, buf, be.getUnitLabel(i), 0.4f, 0x00FF00, FULLBRIGHT);

            ps.popPose();
        }

        ps.popPose();
    }

    /** Original: {@code Math.abs(v) <= 10_000 ? v + "" : BobMathUtil.getShortNumber(v)}. */
    private static String shortNumber(double v) {
        long l = (long) v;
        if (Math.abs(l) <= 10_000) return Long.toString(l);
        long abs = Math.abs(l);
        if (abs < 1_000_000L)             return (l / 1_000L) + "k";
        if (abs < 1_000_000_000L)         return (l / 1_000_000L) + "M";
        if (abs < 1_000_000_000_000L)     return (l / 1_000_000_000L) + "G";
        return (l / 1_000_000_000_000L) + "T";
    }
}
