package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKNumitronBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderRBMKNumitron}: two seven-character numitron tubes.
 *
 * <p>Each character is one quad sampling a 0.1 x 0.5 cell of {@code numitron_lights}: digits 0-9
 * sit on the top row, and the SI suffixes plus '-' and '.' on the bottom row, at exactly the
 * offsets the original hard-codes.</p>
 */
public class RBMKNumitronRenderer extends RBMKPanelRenderer<RBMKNumitronBlockEntity> {

    private static final int DIGITS = 7;
    private static final double SCALE = 200D;
    private static final float W = (float) (8D / SCALE);
    private static final float H = (float) (13D / SCALE);
    private static final float Y_OFFSET = 0.5625f;
    private static final float PLANE_X = 0.03135f;

    public RBMKNumitronRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RBMKNumitronBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int light, int overlay) {
        Map<String, List<float[]>> obj = model("numitron");
        TextureAtlasSprite body   = panelSprite("numitron");
        TextureAtlasSprite lights = panelSprite("numitron_lights");

        begin(be, ps);

        for (int i = 0; i < RBMKNumitronBlockEntity.UNITS; i++) {
            if (!be.isUnitActive(i)) continue;

            ps.pushPose();
            ps.translate(0.25, i * -0.5 + 0.25, 0);

            all(ps, buf, obj, body, 1f, 1f, 1f, light, overlay);

            String value = format(be.value[i], be.shortenNumber[i], be.leadingZeroes[i]);

            for (int j = 0; j < DIGITS; j++) {
                // 0x40 is the leftmost digit's bit, matching the original's mask walk.
                if ((be.activeDigits[i] & (0x40L >> j)) == 0) continue;

                char c = value.charAt(j);
                if (c == ' ') continue;

                float[] uv = cell(c);
                float zOffset = (float) ((j - 3) * 0.1D);
                uvQuad(ps, buf, lights, PLANE_X,
                        -H + Y_OFFSET, H + Y_OFFSET,
                        W - zOffset, -W - zOffset,
                        uv[0], uv[0] + 0.1f, uv[1], uv[1] + 0.5f,
                        1f, 1f, 1f, FULLBRIGHT, overlay);
            }

            ps.translate(0.01, 0.3125, 0);
            label(ps, buf, be.getUnitLabel(i), 0.75f, 0x00FF00, FULLBRIGHT);

            ps.popPose();
        }

        ps.popPose();
    }

    /** The original's character-to-UV table; anything unknown falls back to '-'. */
    private static float[] cell(char c) {
        if (c >= '0' && c <= '9') return new float[]{0.1f * (c - '0'), 0.0f};
        return switch (c) {
            case '.' -> new float[]{0.9f, 0.5f};
            case 'k' -> new float[]{0.0f, 0.5f};
            case 'M' -> new float[]{0.1f, 0.5f};
            case 'G' -> new float[]{0.2f, 0.5f};
            case 'T' -> new float[]{0.3f, 0.5f};
            case 'P' -> new float[]{0.4f, 0.5f};
            case 'E' -> new float[]{0.5f, 0.5f};
            default  -> new float[]{0.8f, 0.5f}; // '-'
        };
    }

    /**
     * Original: clamp to the tube's range, then pad to seven characters - with the padding zeroes
     * inserted *after* a leading minus sign rather than before it.
     */
    private static String format(double raw, boolean shorten, boolean leadingZeroes) {
        long v = (long) raw;
        String value;
        if (shorten) {
            value = shortNumber(v);
        } else if (v > 9999999L) {
            value = "9999999";
        } else if (v < -999999L) {
            value = "-999999";
        } else {
            value = Long.toString(v);
        }

        if (value.length() < DIGITS && !value.isEmpty() && value.charAt(0) == '-' && leadingZeroes) {
            String body = value.substring(1);
            while (body.length() < DIGITS - 1) body = "0" + body;
            value = "-" + body;
        } else {
            String fill = leadingZeroes ? "0" : " ";
            while (value.length() < DIGITS) value = fill + value;
        }
        return value.length() > DIGITS ? value.substring(0, DIGITS) : value;
    }

    private static String shortNumber(long l) {
        long abs = Math.abs(l);
        if (abs < 1_000L)                 return Long.toString(l);
        if (abs < 1_000_000L)             return (l / 1_000L) + "k";
        if (abs < 1_000_000_000L)         return (l / 1_000_000L) + "M";
        if (abs < 1_000_000_000_000L)     return (l / 1_000_000_000L) + "G";
        if (abs < 1_000_000_000_000_000L) return (l / 1_000_000_000_000L) + "T";
        return (l / 1_000_000_000_000_000L) + "P";
    }
}
