package com.hbm_m.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.decorations.TrinketBlock;
import com.hbm_m.block.decorations.TrinketTypes;
import com.hbm_m.block.decorations.TrinketTypes.BobbleType;
import com.hbm_m.block.decorations.TrinketTypes.SnowglobeType;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 1:1 {@code GUIScreenBobble} / {@code GUIScreenSnowglobe}: gruene Tafel 300x150 mit Titel, Namen, Beitrag (nur
 * Wackelkopf) und Gravur; beim Oeffnen der Fallout-3-Hinweiston. Der Name des Mellow-Wackelkopfs wird zum Anagramm
 * "GEORGEWILLIAMPATON" hin und her gemischt.
 */
public final class TrinketScreens {

    private TrinketScreens() {}

    public static void open(TrinketBlock.Kind kind, int type) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(HbmSoundsNT.get("block.bobble"), 1.0F));
        mc.setScreen(new Tablet(kind, type));
    }

    public static class Tablet extends Screen {

        private final TrinketBlock.Kind kind;
        private final int type;

        Tablet(TrinketBlock.Kind kind, int type) {
            super(Component.empty());
            this.kind = kind;
            this.type = type;
        }

        private void centered(GuiGraphics g, String text, double left, double sizeX, int y, int color) {
            g.drawString(font, text, (int) (left + sizeX / 2 - font.width(text) / 2), y, color, true);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);

            double sizeX = 300, sizeY = 150;
            double left = (this.width - sizeX) / 2;
            double top = (this.height - sizeY) / 2;
            g.fill((int) left, (int) top, (int) (left + sizeX), (int) (top + sizeY), 0xCC003300);

            int next = (int) top + 10;
            boolean bobble = kind == TrinketBlock.Kind.BOBBLE;

            centered(g, bobble ? "Nuclear Tech Commemorative Bobblehead" : "Nuclear Tech Commemorative Snowglobe", left, sizeX, next, 0x00ff00);
            next += 10;

            String name, contribution = null, inscription;
            if (bobble) {
                BobbleType b = TrinketTypes.safe(BobbleType.class, type);
                name = b == BobbleType.MELLOW ? anagramIt(b.title, "GEORGEWILLIAMPATON") : b.title;
                contribution = b.contribution;
                inscription = b.inscription;
            } else {
                SnowglobeType s = TrinketTypes.safe(SnowglobeType.class, type);
                name = s.label;
                inscription = s.inscription;
            }
            centered(g, name, left, sizeX, next, 0x009900);
            next += 20;

            if (contribution != null) {
                centered(g, "Has contributed", left, sizeX, next, 0x00ff00);
                next += 10;
                for (String text : contribution.split("\\$")) {
                    centered(g, text, left, sizeX, next, 0x009900);
                    next += 10;
                }
                next += 10;
            }

            if (inscription != null) {
                centered(g, "On the bottom is the following inscription:", left, sizeX, next, 0x00ff00);
                next += 10;
                if (bobble) {
                    for (String text : inscription.split("\\$")) {
                        centered(g, text, left, sizeX, next, 0x009900);
                        next += 10;
                    }
                } else {
                    // I18nUtil.autoBreakWithParagraphs(font, text, 280)
                    for (String para : inscription.split("\\$")) {
                        for (FormattedCharSequence line : font.split(Component.literal(para), 280)) {
                            g.drawString(font, line, (int) (left + sizeX / 2 - font.width(line) / 2), next, 0x009900, true);
                            next += 10;
                        }
                    }
                }
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256 || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                onClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        /** Mischt die Buchstaben von {@code from} nach {@code to} und zurueck (1.5-s-Sinus). */
        private static String anagramIt(String from, String to) {
            double t = Math.sin((double) System.currentTimeMillis() / 1500.0) * 0.75 + 0.5;
            char[] lettersFrom = from.toCharArray();
            char[] lettersTo = to.toCharArray();
            boolean[] hasPairedLetter = new boolean[lettersFrom.length];
            List<double[]> targets = new ArrayList<>();

            for (int i = 0; i < lettersFrom.length; i++) {
                char letterFrom = lettersFrom[i];
                for (int o = 0; o < lettersTo.length; o++) {
                    if (letterFrom == lettersTo[o] && !hasPairedLetter[o]) {
                        targets.add(new double[] { lerp(i, o, t), lettersFrom[i] });
                        hasPairedLetter[o] = true;
                        break;
                    }
                }
            }
            for (int i = 0; i < targets.size(); i++) {
                for (int j = i + 1; j < targets.size(); j++) {
                    if (targets.get(i)[0] > targets.get(j)[0]) {
                        double[] temp = targets.get(i);
                        targets.set(i, targets.get(j));
                        targets.set(j, temp);
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (double[] in : targets) sb.append((char) in[1]);
            return sb.toString();
        }

        private static double lerp(double a, double b, double t) {
            t = Math.max(Math.min(t, 1), 0);
            return a * (1 - t) + b * t;
        }
    }
}
