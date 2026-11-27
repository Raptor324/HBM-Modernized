package com.hbm_m.platform.legacy.client;

import java.util.List;
import java.util.Optional;

import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * [26.x] Alte {@code GuiGraphics}-Aufrufe (1.21.1) auf {@code GuiGraphicsExtractor} (26.x) abgebildet.
 *
 * <p>Stonecutter schreibt {@code guiGraphics.blit(a, b)} zu {@code GuiCompat.blit(guiGraphics, a, b)} um
 * (Regeln {@code mc26_gui_calls}/{@code mc26_gui_noarg}). Die Methodennamen müssen den alten Namen
 * entsprechen, sonst greift die Rückwärts-Regel nicht.
 *
 * <p>Unterschiede zu 1.21.1: Texte brauchen in 26.x eine Alpha-Komponente (wird wie früher im Font
 * ergänzt), {@code blitOffset}/z wird ignoriert, {@code setColor} gibt es nicht mehr.
 */
@SuppressWarnings("unused")
public final class GuiCompat {

    private GuiCompat() {}

    private static final RenderPipeline TEX = RenderPipelines.GUI_TEXTURED;

    /** Wie der alte Font: Farben ohne Alpha (&lt; 4) werden deckend gezeichnet. */
    private static int argb(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    // ---------------------------------------------------------------- blit

    /** blit(tex, x, y, u, v, w, h) auf einer 256x256-Textur. */
    public static void blit(GuiGraphicsExtractor gx, Identifier tex, int x, int y, int u, int v, int w, int h) {
        gx.blit(TEX, tex, x, y, u, v, w, h, 256, 256);
    }

    public static void blit(GuiGraphicsExtractor gx, Identifier tex, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        gx.blit(TEX, tex, x, y, u, v, w, h, texW, texH);
    }

    public static void blit(GuiGraphicsExtractor gx, Identifier tex, int x, int y, int blitOffset, float u, float v, int w, int h, int texW, int texH) {
        gx.blit(TEX, tex, x, y, u, v, w, h, texW, texH);
    }

    public static void blit(GuiGraphicsExtractor gx, Identifier tex, int x, int y, int w, int h, float u, float v, int uW, int vH, int texW, int texH) {
        gx.blit(TEX, tex, x, y, u, v, w, h, uW, vH, texW, texH);
    }

    public static void blit(GuiGraphicsExtractor gx, Identifier tex, int x1, int x2, int y1, int y2, int blitOffset, int uW, int vH, float u, float v, int texW, int texH) {
        gx.blit(TEX, tex, x1, y1, u, v, x2 - x1, y2 - y1, uW, vH, texW, texH);
    }

    public static void blit(GuiGraphicsExtractor gx, int x, int y, int blitOffset, int w, int h, TextureAtlasSprite sprite) {
        gx.blitSprite(TEX, sprite, x, y, w, h);
    }

    public static void blit(GuiGraphicsExtractor gx, int x, int y, int blitOffset, int w, int h, TextureAtlasSprite sprite,
                            float r, float gr, float b, float a) {
        int color = ((int) (a * 255) & 0xFF) << 24 | ((int) (r * 255) & 0xFF) << 16 | ((int) (gr * 255) & 0xFF) << 8 | ((int) (b * 255) & 0xFF);
        gx.blitSprite(TEX, sprite, x, y, w, h, color);
    }

    public static void blitSprite(GuiGraphicsExtractor gx, Identifier sprite, int x, int y, int w, int h) {
        gx.blitSprite(TEX, sprite, x, y, w, h);
    }

    public static void blitSprite(GuiGraphicsExtractor gx, Identifier sprite, int x, int y, int blitOffset, int w, int h) {
        gx.blitSprite(TEX, sprite, x, y, w, h);
    }

    public static void blitSprite(GuiGraphicsExtractor gx, Identifier sprite, int texW, int texH, int u, int v, int x, int y, int w, int h) {
        gx.blitSprite(TEX, sprite, texW, texH, u, v, x, y, w, h);
    }

    // ---------------------------------------------------------------- Text

    public static int drawString(GuiGraphicsExtractor gx, Font font, String text, int x, int y, int color) {
        return drawString(gx, font, text, x, y, color, true);
    }

    public static int drawString(GuiGraphicsExtractor gx, Font font, String text, int x, int y, int color, boolean shadow) {
        if (text == null) return x;
        gx.text(font, text, x, y, argb(color), shadow);
        return x + font.width(text) + (shadow ? 1 : 0);
    }

    public static int drawString(GuiGraphicsExtractor gx, Font font, String text, float x, float y, int color, boolean shadow) {
        return drawString(gx, font, text, Math.round(x), Math.round(y), color, shadow);
    }

    public static int drawString(GuiGraphicsExtractor gx, Font font, Component text, int x, int y, int color) {
        return drawString(gx, font, text, x, y, color, true);
    }

    public static int drawString(GuiGraphicsExtractor gx, Font font, Component text, int x, int y, int color, boolean shadow) {
        gx.text(font, text, x, y, argb(color), shadow);
        return x + font.width(text) + (shadow ? 1 : 0);
    }

    public static int drawString(GuiGraphicsExtractor gx, Font font, FormattedCharSequence text, int x, int y, int color) {
        return drawString(gx, font, text, x, y, color, true);
    }

    public static int drawString(GuiGraphicsExtractor gx, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        gx.text(font, text, x, y, argb(color), shadow);
        return x + font.width(text) + (shadow ? 1 : 0);
    }

    public static int drawString(GuiGraphicsExtractor gx, Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow) {
        return drawString(gx, font, text, Math.round(x), Math.round(y), color, shadow);
    }

    public static void drawCenteredString(GuiGraphicsExtractor gx, Font font, String text, int x, int y, int color) {
        gx.centeredText(font, text, x, y, argb(color));
    }

    public static void drawCenteredString(GuiGraphicsExtractor gx, Font font, Component text, int x, int y, int color) {
        gx.centeredText(font, text, x, y, argb(color));
    }

    public static void drawCenteredString(GuiGraphicsExtractor gx, Font font, FormattedCharSequence text, int x, int y, int color) {
        gx.centeredText(font, text, x, y, argb(color));
    }

    public static void drawWordWrap(GuiGraphicsExtractor gx, Font font, FormattedText text, int x, int y, int width, int color) {
        gx.textWithWordWrap(font, text, x, y, width, argb(color));
    }

    // ---------------------------------------------------------------- Items

    public static void renderItem(GuiGraphicsExtractor gx, ItemStack stack, int x, int y) {
        gx.item(stack, x, y);
    }

    public static void renderItem(GuiGraphicsExtractor gx, ItemStack stack, int x, int y, int seed) {
        gx.item(stack, x, y, seed);
    }

    public static void renderItem(GuiGraphicsExtractor gx, ItemStack stack, int x, int y, int seed, int guiOffset) {
        gx.item(stack, x, y, seed);
    }

    public static void renderItem(GuiGraphicsExtractor gx, LivingEntity entity, ItemStack stack, int x, int y, int seed) {
        gx.item(entity, stack, x, y, seed);
    }

    public static void renderItem(GuiGraphicsExtractor gx, LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed) {
        gx.item(entity, stack, x, y, seed);
    }

    public static void renderFakeItem(GuiGraphicsExtractor gx, ItemStack stack, int x, int y) {
        gx.fakeItem(stack, x, y);
    }

    public static void renderFakeItem(GuiGraphicsExtractor gx, ItemStack stack, int x, int y, int seed) {
        gx.fakeItem(stack, x, y, seed);
    }

    public static void renderItemDecorations(GuiGraphicsExtractor gx, Font font, ItemStack stack, int x, int y) {
        gx.itemDecorations(font, stack, x, y);
    }

    public static void renderItemDecorations(GuiGraphicsExtractor gx, Font font, ItemStack stack, int x, int y, String text) {
        gx.itemDecorations(font, stack, x, y, text);
    }

    // ---------------------------------------------------------------- Tooltips

    public static void renderTooltip(GuiGraphicsExtractor gx, Font font, ItemStack stack, int x, int y) {
        gx.setTooltipForNextFrame(font, stack, x, y);
    }

    public static void renderTooltip(GuiGraphicsExtractor gx, Font font, Component text, int x, int y) {
        gx.setTooltipForNextFrame(font, text, x, y);
    }

    public static void renderTooltip(GuiGraphicsExtractor gx, Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
        gx.setTooltipForNextFrame(font, lines, x, y);
    }

    public static void renderTooltip(GuiGraphicsExtractor gx, Font font, List<Component> lines, Optional<TooltipComponent> image, int x, int y) {
        gx.setTooltipForNextFrame(font, lines, image, x, y);
    }

    public static void renderComponentTooltip(GuiGraphicsExtractor gx, Font font, List<Component> lines, int x, int y) {
        gx.setComponentTooltipForNextFrame(font, lines, x, y);
    }

    public static void renderComponentTooltip(GuiGraphicsExtractor gx, Font font, List<Component> lines, int x, int y, ItemStack stack) {
        gx.setComponentTooltipForNextFrame(font, lines, x, y);
    }

    // ---------------------------------------------------------------- Linien / Rahmen

    public static void hLine(GuiGraphicsExtractor gx, int minX, int maxX, int y, int color) {
        gx.horizontalLine(minX, maxX, y, color);
    }

    public static void vLine(GuiGraphicsExtractor gx, int x, int minY, int maxY, int color) {
        gx.verticalLine(x, minY, maxY, color);
    }

    public static void renderOutline(GuiGraphicsExtractor gx, int x, int y, int w, int h, int color) {
        gx.outline(x, y, w, h, color);
    }

    // ---------------------------------------------------------------- Zustand

    /** {@code RenderSystem.setShaderColor}-artige Tönung gibt es für GUI-Aufträge nicht mehr. */
    public static void setColor(GuiGraphicsExtractor gx, float r, float gr, float b, float a) {
    }

    /** 26.x sammelt GUI-Aufträge selbst, ein Flush ist unnötig. */
    public static void flush(GuiGraphicsExtractor gx) {
    }

    public static Pose pose(GuiGraphicsExtractor gx) {
        return new Pose(gx.pose());
    }

    /**
     * PoseStack-ähnliche Hülle um die 2D-Matrix ({@code Matrix3x2fStack}) von 26.x. z-Anteile von
     * translate/scale werden ignoriert; {@code mulPose} wertet nur die Drehung um z aus.
     */
    public static final class Pose {
        private final Matrix3x2fStack stack;

        Pose(Matrix3x2fStack stack) {
            this.stack = stack;
        }

        public Matrix3x2fStack matrix() {
            return stack;
        }

        public void pushPose() {
            stack.pushMatrix();
        }

        public void popPose() {
            stack.popMatrix();
        }

        public void translate(double x, double y, double z) {
            stack.translate((float) x, (float) y);
        }

        public void translate(float x, float y, float z) {
            stack.translate(x, y);
        }

        public void scale(float x, float y, float z) {
            stack.scale(x, y);
        }

        public void rotate(Quaternionf rotation) {
            Vector3f euler = rotation.getEulerAnglesZYX(new Vector3f());
            stack.rotate(euler.z);
        }

        public Pose last() {
            return this;
        }
    }
}
