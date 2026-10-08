package com.hbm_m.client.overlay;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
//? if forge {
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
//?}

/**
 * 1:1 {@code RenderScreenOverlay.renderDashBar} und {@code renderShieldBar} (1.7.10) samt der
 * Aufrufe aus {@code ModEventHandlerClient}: Ausdauerbalken der Sprint-Ausweichschritte (Wismut-
 * Ruestung, Wolke in der Flasche) links unten, Schildbalken ueber der Lebensanzeige.
 */
public final class OverlayDashShield {

    private OverlayDashShield() { }

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation MISC = new ResourceLocation(RefStrings.MODID, "textures/misc/overlay_misc.png");
    *///?} else {
    private static final ResourceLocation MISC = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/misc/overlay_misc.png");
    //?}

    private static float fadeOut = 0F;

    /** Original {@code renderDashBar} ("like a fella once said, aint that a kick in the head"). */
    public static void renderDashBar(GuiGraphics gui, int screenHeight, HbmPlayerProps props) {

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int width = 30;
        int posX = 16;
        int posY = screenHeight - 40 - 2;

        gui.blit(MISC, posX - 10, posY, 107, 18, 7, 10);

        int stamina = props.getStamina();
        int dashes = props.getDashCount();
        int rows = dashes / 3;
        int finalColumns = dashes % 3;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < 3; x++) {
                if (y == rows && x > finalColumns)
                    break;
                gui.blit(MISC, posX + (width + 2) * x, posY - 12 * y, 76, 48, width, 10);
                int staminaDiv = stamina / 30;
                int staminaMod = stamina % 30;
                int barID = (3 * y) + x;
                int barStatus = 1; //0 = red, 1 = normal, 2 = greyed, 3 = dashed, 4 = ascended
                int barSize = width;
                if (staminaDiv < barID) {
                    barStatus = 3;
                } else if (staminaDiv == barID) {
                    barStatus = 2;
                    barSize = (int) ((float) (stamina % 30) * (width / 30F));
                    if (barID == 0) barStatus = 0;
                }
                gui.blit(MISC, posX + (width + 2) * x, posY - 12 * y, 76, 18 + (10 * barStatus), barSize, 10);

                if (staminaDiv == barID && staminaMod >= 27) {
                    fadeOut = 1F;
                }
                if (fadeOut > 0 && staminaDiv - 1 == barID) {
                    gui.setColor(1F, 1F, 1F, fadeOut);
                    int bar = barID;
                    if (stamina % 30 >= 25) bar++;
                    if (bar / 3 != y) y++;

                    bar = bar % 3;
                    gui.blit(MISC, posX + (width + 2) * bar, posY - 12 * y, 76, 58, width, 10);
                    fadeOut -= 0.04F;
                    gui.setColor(1F, 1F, 1F, 1F);
                }
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    /**
     * Original {@code renderShieldBar}: zeichnet in Hoehe {@code left_height} und schiebt die
     * folgenden Leisten (Ruestung, Luft) um 10 Pixel nach oben.
     * @return die Zeilenhoehe, um die {@code leftHeight} wachsen muss
     */
    public static int renderShieldBar(GuiGraphics gui, int screenWidth, int screenHeight, int leftHeight, HbmPlayerProps props) {
        Font font = Minecraft.getInstance().font;

        int left = screenWidth / 2 - 91;
        int top = screenHeight - leftHeight;

        gui.blit(MISC, left, top, 146, 0, 81, 9);
        int i = (int) Math.ceil(props.shield * 79 / props.getEffectiveMaxShield());
        gui.blit(MISC, left + 1, top, 147, 9, i, 9);

        String label = "" + ((int) (props.shield * 10F)) / 10D;
        gui.drawString(font, label, left + 41 - font.width(label) / 2, top + 1, 0x0000, false);
        gui.drawString(font, label, left + 39 - font.width(label) / 2, top + 1, 0x0000, false);
        gui.drawString(font, label, left + 40 - font.width(label) / 2, top, 0x0000, false);
        gui.drawString(font, label, left + 40 - font.width(label) / 2, top + 2, 0x0000, false);
        gui.drawString(font, label, left + 40 - font.width(label) / 2, top + 1, 0xFFFF80, false);
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        return 10;
    }

    //? if forge {
    /** Original: {@code onOverlayRender} (HOTBAR) - nur wenn der Spieler Ausweichschritte hat. */
    public static final IGuiOverlay DASH_OVERLAY = (ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.hideGui) return;
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        if (props.getDashCount() > 0) renderDashBar(g, h, props);
    };

    /** Original: {@code onHUDRenderShield} (ARMOR, vor der Ruestungsanzeige). */
    public static final IGuiOverlay SHIELD_OVERLAY = (ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.hideGui || !gui.shouldDrawSurvivalElements()) return;
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        if (props.getEffectiveMaxShield() > 0) {
            gui.setupOverlayRenderState(true, false);
            gui.leftHeight += renderShieldBar(g, w, h, gui.leftHeight, props);
        }
    };
    //?}
}
