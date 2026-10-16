package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKGraphBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderRBMKGraph}: two rolling line charts sharing the numitron's tube body.
 *
 * <p>Each chart plots its 30-sample history left to right, auto-scaling to the data unless the
 * unit pins a bound, and prints the resulting lower/upper bound at the axis ends.</p>
 */
public class RBMKGraphRenderer extends RBMKPanelRenderer<RBMKGraphBlockEntity> {

    private static final int GREEN = 0x00FF00;

    public RBMKGraphRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RBMKGraphBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int light, int overlay) {
        Map<String, List<float[]>> obj = model("numitron");
        TextureAtlasSprite body = panelSprite("numitron");

        begin(be, ps);

        for (int i = 0; i < RBMKGraphBlockEntity.UNITS; i++) {
            if (!be.isUnitActive(i)) continue;

            ps.pushPose();
            ps.translate(0.25, i * -0.5 + 0.25, 0);

            all(ps, buf, obj, body, 1f, 1f, 1f, light, overlay);

            long[] values = be.history[i];
            long lowest  = be.minBound[i] ? be.graphMin[i] : min(values);
            long highest = be.maxBound[i] ? be.graphMax[i] : max(values);
            long range = Math.max(highest - lowest, 1);

            // The original emits GL_LINES vertex pairs; each pair is one segment here.
            for (int v = 0; v < values.length - 1; v++) {
                float[] a = point(values, v,     lowest, highest, range);
                float[] b = point(values, v + 1, lowest, highest, range);
                line(ps, buf, a[0], a[1], a[2], b[0], b[1], b[2],
                        red(GREEN), green(GREEN), blue(GREEN));
            }

            // Axis bound labels, drawn at the low then high end of the plot area.
            ps.pushPose();
            ps.translate(0.032, 0.5 - 0.03125 * 1.5, -0.375 + 0.03125);
            String lower = Long.toString(lowest);
            String upper = Long.toString(highest);
            labelRaw(ps, buf, lower, 0.0025f, -font().width(lower), GREEN, FULLBRIGHT);
            ps.translate(0, -0.03125 * 7, 0);
            labelRaw(ps, buf, upper, 0.0025f, -font().width(upper), GREEN, FULLBRIGHT);
            ps.popPose();

            ps.translate(0.01, 0.3125, 0);
            label(ps, buf, be.getUnitLabel(i), 0.75f, GREEN, FULLBRIGHT);

            ps.popPose();
        }

        ps.popPose();
    }

    private static float[] point(long[] values, int k, long lowest, long highest, long range) {
        long flux = Math.max(lowest, Math.min(highest, values[k]));
        float dx = 0.03225f;
        float dy = (float) (0.5 - 0.03125 + (flux - lowest) * 0.1875D / range);
        float dz = (float) (0.375 - k * 0.75 / (values.length - 1));
        return new float[]{dx, dy, dz};
    }

    private static long min(long[] v) {
        long m = Long.MAX_VALUE;
        for (long l : v) m = Math.min(m, l);
        return m == Long.MAX_VALUE ? 0 : m;
    }

    private static long max(long[] v) {
        long m = Long.MIN_VALUE;
        for (long l : v) m = Math.max(m, l);
        return m == Long.MIN_VALUE ? 0 : m;
    }
}
