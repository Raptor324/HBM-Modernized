package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKTerminalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderRBMKTerminal}: an 18-line text console on the panel face.
 *
 * <p>Line 0 is the line currently being typed, lines 1-17 are the scrollback. Each line is
 * prefixed with {@code "> "} and clipped to the screen width character by character, exactly like
 * the original. Text is green normally and amber while the terminal is repeating a broadcast.</p>
 */
public class RBMKTerminalRenderer extends RBMKPanelRenderer<RBMKTerminalBlockEntity> {

    private static final int LINES = 18;
    private static final int MAX_WIDTH = 172;
    private static final float SCALE = 1f / 250f;
    private static final String PREFIX = "> ";

    public RBMKTerminalRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RBMKTerminalBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int light, int overlay) {
        Map<String, List<float[]>> obj = model("terminal");
        TextureAtlasSprite tex = panelSprite("terminal");

        begin(be, ps);

        ps.translate(0.25, 0, 0);
        all(ps, buf, obj, tex, 1f, 1f, 1f, light, overlay);

        ps.translate(0.0635, 0.125, 0.0625 * 5.5);

        Font font = font();
        int prefixWidth = font.width(PREFIX);
        int color = be.doesRepeat ? 0xFF8000 : 0x00FF00;

        for (int i = 0; i < LINES; i++) {
            String source = i == 0 ? "" : be.history[i - 1];
            if (source == null) source = "";

            StringBuilder line = new StringBuilder(40);
            if (i == 0 || !source.isEmpty()) line.append(PREFIX);

            int width = prefixWidth;
            for (int j = 0; j < source.length(); j++) {
                char c = source.charAt(j);
                width += font.width(String.valueOf(c));
                if (width > MAX_WIDTH) break;
                line.append(c);
            }

            ps.translate(0, 10 * SCALE, 0);

            ps.pushPose();
            ps.scale(SCALE, -SCALE, SCALE);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            font.drawInBatch(line.toString(), 0, -font.lineHeight / 2f, color, false,
                    ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, FULLBRIGHT);
            ps.popPose();
        }

        ps.popPose();
    }
}
