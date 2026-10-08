package com.hbm_m.inventory.gui;

import java.util.List;
import java.util.Locale;

import com.hbm_m.blockentity.machines.custom.CustomMachineBlockEntity;
import com.hbm_m.config.CustomMachineConfigJSON;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.CustomMachineMenu;
import com.hbm_m.inventory.menu.PatternSlot;
import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 1:1 {@code GUIMachineCustom}: Tanks, Strom, Fortschritt, gesperrte Plaetze nach Konfiguration, Waermezeiger und
 * Flussanzeige; Name in der Sprache des Spielers.
 */
public class GUIMachineCustom extends GuiInfoScreen<CustomMachineMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_custom.png");
    private final CustomMachineBlockEntity custom;

    public GUIMachineCustom(CustomMachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.custom = menu.custom;
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);
        if (custom.config == null) return;

        drawElectricityInfo(g, x, y, 150, 18, 16, 52, custom.getEnergyStored(), custom.config.maxPower);
        if (custom.config.maxHeat > 0) {
            drawCustomInfoStat(g, x, y, 61, 53, 18, 18, x, y,
                    Component.literal("Heat:" + String.format(Locale.US, "%,d", custom.heat) + " / " + String.format(Locale.US, "%,d", custom.config.maxHeat)));
        }

        if (this.menu.getCarried().isEmpty()) {
            for (Slot slot : this.menu.slots) {
                int tileIndex = slot.getContainerSlot();
                if (slot instanceof PatternSlot && this.isHovering(slot.x, slot.y, 16, 16, x, y)
                        && tileIndex - 10 < custom.matcher.size() && custom.matcher.getMode(tileIndex - 10) != null) {
                    g.renderComponentTooltip(font, List.of(Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                            Component.literal(ModulePatternMatcher.getLabel(custom.matcher.getMode(tileIndex - 10)))), x, y - 30);
                }
            }
        }

        for (int i = 0; i < custom.inputTanks.length; i++) {
            custom.inputTanks[i].renderTankInfo(g, font, x, y, leftPos + 8 + 18 * i, topPos + 18, 16, 34);
        }
        for (int i = 0; i < custom.outputTanks.length; i++) {
            custom.outputTanks[i].renderTankInfo(g, font, x, y, leftPos + 78 + 18 * i, topPos + 18, 16, 34);
        }

        renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int i, int j) {
        if (custom.config == null) return;
        String name = custom.config.displayName(CustomMachineConfigJSON.languageCode());
        g.drawString(font, name, 68 - font.width(name) / 2, 6, 4210752, false);
        g.drawString(font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
        if (custom.config.fluxMode) g.drawString(font, "Flux:" + custom.flux, 83, 57, 0x08FF00, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (custom.config == null) return;

        if (custom.config.fluxMode) {
            g.blit(TEXTURE, leftPos + 78, topPos + 54, 192, 122, 51, 15);
        }

        if (custom.maxHeat > 0) {
            g.blit(TEXTURE, leftPos + 61, topPos + 53, 236, 0, 18, 18);
            GuiGaugeNeedle.draw(g, leftPos + 70, topPos + 62, (double) custom.heat / (double) custom.config.maxHeat, 5, 2, 1, 0x7F0000);
        }

        int p = custom.progress * 90 / Math.max(1, custom.maxProgress);
        g.blit(TEXTURE, leftPos + 78, topPos + 119, 192, 0, Math.min(p, 44), 16);
        if (p > 44) {
            p -= 44;
            g.blit(TEXTURE, leftPos + 78 + 44, topPos + 119, 192, 16, p, 16);
        }

        int e = (int) (custom.getEnergyStored() * 52 / Math.max(1, custom.config.maxPower));
        g.blit(TEXTURE, leftPos + 150, topPos + 70 - e, 176, 52 - e, 16, e);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                if (custom.config.itemInCount <= index) {
                    g.blit(TEXTURE, leftPos + 7 + j * 18, topPos + 71 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                    g.blit(TEXTURE, leftPos + 7 + j * 18, topPos + 107 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                }
                if (custom.config.itemOutCount <= index) {
                    g.blit(TEXTURE, leftPos + 77 + j * 18, topPos + 71 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            if (custom.config.fluidInCount <= i) {
                g.blit(TEXTURE, leftPos + 7 + i * 18, topPos + 17, 192 + i * 18, 32, 18, 54);
            }
            if (custom.config.fluidOutCount <= i) {
                g.blit(TEXTURE, leftPos + 77 + i * 18, topPos + 17, 192 + i * 18, 32, 18, 36);
            }
        }

        for (int i = 0; i < custom.inputTanks.length; i++) {
            custom.inputTanks[i].renderTank(g, leftPos + 8 + 18 * i, topPos + 52 - 34, 16, 34); // Original: Unterkante 52, Port-renderTank erwartet die Oberkante
        }
        for (int i = 0; i < custom.outputTanks.length; i++) {
            custom.outputTanks[i].renderTank(g, leftPos + 78 + 18 * i, topPos + 52 - 34, 16, 34);
        }
    }
}
