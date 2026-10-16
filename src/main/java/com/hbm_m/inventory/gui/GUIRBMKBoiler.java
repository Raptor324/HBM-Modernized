package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.rbmk.RBMKBoilerBlockEntity;
import com.hbm_m.inventory.menu.RBMKBoilerMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIRBMKBoiler} (1.7.10-Original, {@code ContainerRBMKGeneric}):
 * xSize/ySize (176x186) und die Original-UVs fuer den Wasser- (feed) und Dampf- (steam) Fuellstand
 * sowie die Dampfsorten-Anzeige wurden 1:1 aus {@code drawGuiContainerBackgroundLayer} uebernommen.
 * Die vorherige Platzhalter-Fassung zeichnete nur Fuellrechtecke ohne Textur und mit falscher
 * Bildhoehe (166 statt 186) - dadurch waren auch die Spieler-Inventar-Slots (siehe
 * {@link RBMKBoilerMenu}) um 20px zu hoch positioniert.
 */
public class GUIRBMKBoiler extends GuiInfoScreen<RBMKBoilerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_boiler.png");

    private static final String[] GRADE_NAMES = { "Steam", "Hot Steam", "Super-Hot Steam", "Ultra-Hot Steam" };

    // Dampfsorten-Anzeige / Klickbereich zum Umschalten der Kompressionsstufe
    // (Original: guiLeft+33..53, guiTop+21..85 -> mouseClicked; Textur bei 36,24, 14x58)
    private static final int TYPE_X = 33, TYPE_Y = 21, TYPE_W = 20, TYPE_H = 64;
    private static final int TYPE_TEX_X = 36, TYPE_TEX_Y = 24, TYPE_TEX_W = 14, TYPE_TEX_H = 58;

    // Wasser- (feed) Fuellstandsanzeige (Original: guiLeft+126, guiTop+24, 16x56 Hover-Bereich)
    private static final int WATER_X = 126, WATER_Y = 24, WATER_W = 14, WATER_H = 58;

    // Dampf- (steam) Fuellstandsanzeige (Original: guiLeft+89, guiTop+39, 8x28 Hover-Bereich)
    private static final int STEAM_X = 89, STEAM_Y = 39, STEAM_W = 8, STEAM_H = 28;

    private final RBMKBoilerBlockEntity be;

    public GUIRBMKBoiler(RBMKBoilerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        // тайл может отсутствовать в реплее Flashback
        if (be == null) return;

        // Wasser- (feed) Fuellstand: waechst von unten (guiLeft+126, guiTop+82-i, 176, 58-i, 14, i)
        int maxW = be.waterTank.getMaxFill();
        int fillW = maxW > 0 ? be.waterTank.getFill() * 58 / maxW : 0;
        if (fillW > 0) {
            g.blit(TEXTURE, x + 126, y + 82 - fillW, 176, 58 - fillW, 14, fillW);
        }

        // Dampf- (steam) Fuellstand: kleine Saeule (guiLeft+91, guiTop+65-j, 190, 24-j, 4, j)
        int maxS = be.steamTank.getMaxFill();
        int fillS = maxS > 0 ? be.steamTank.getFill() * 22 / maxS : 0;
        if (fillS > 0) fillS++;
        if (fillS > 22) fillS++;
        if (fillS > 0) {
            g.blit(TEXTURE, x + 91, y + 65 - fillS, 190, 24 - fillS, 4, fillS);
        }

        // Dampfsorten-Anzeige je nach Kompressionsstufe (194/208/222/236, 0, 14, 58)
        int grade = Math.min(Math.max(be.steamGrade, 0), 3);
        g.blit(TEXTURE, x + TYPE_TEX_X, y + TYPE_TEX_Y, 194 + grade * 14, 0, TYPE_TEX_W, TYPE_TEX_H);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        GuiCompat.renderBackground(this, g, mx, my, partial);
        super.render(g, mx, my, partial);

        // тайл может отсутствовать в реплее Flashback
        if (be != null) {
        if (isPointInRect(WATER_X, WATER_Y, WATER_W, WATER_H, mx, my)) {
            g.renderTooltip(font, Component.literal(
                    be.waterTank.getFill() + " / " + be.waterTank.getMaxFill() + " mB Water"), mx, my);
        } else if (isPointInRect(STEAM_X, STEAM_Y, STEAM_W, STEAM_H, mx, my)) {
            g.renderTooltip(font, Component.literal(
                    be.steamTank.getFill() + " / " + be.steamTank.getMaxFill() + " mB Steam"), mx, my);
        } else if (isPointInRect(TYPE_X, TYPE_Y, TYPE_W, TYPE_H, mx, my)) {
            g.renderTooltip(font, java.util.List.of(
                    Component.literal("Type: " + GRADE_NAMES[Math.min(Math.max(be.steamGrade, 0), 3)]),
                    Component.literal(String.format("Heat: %.1f°C", be.heat)),
                    Component.literal("Click to cycle steam grade")
            ), java.util.Optional.empty(), mx, my);
        }
        }

        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // тайл может отсутствовать в реплее Flashback
        if (be == null) return super.mouseClicked(mx, my, button);
        if (isPointInRect(TYPE_X, TYPE_Y, TYPE_W, TYPE_H, (int) mx, (int) my)) {
            be.cycleCompressor();
            playClickSound();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
