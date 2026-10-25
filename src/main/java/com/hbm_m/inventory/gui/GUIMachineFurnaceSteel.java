package com.hbm_m.inventory.gui;

import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineFurnaceSteelMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIFurnaceSteel}: Hitzebalken, je Spur Fortschritt und Bonus, Flammensymbole im Betrieb. */
public class GUIMachineFurnaceSteel extends GuiInfoScreen<MachineFurnaceSteelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_furnace_steel.png");

    private final MachineFurnaceSteelBlockEntity furnace;

    public GUIMachineFurnaceSteel(MachineFurnaceSteelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.furnace = menu.blockEntity;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        if (furnace != null) {
            for (int i = 0; i < 3; i++) {
                drawCustomInfoStat(g, x, y, 53, 17 + 18 * i, 70, 7, x, y,
                        Component.literal(String.format(Locale.US, "%,d", furnace.progress[i]) + " / " + String.format(Locale.US, "%,d", MachineFurnaceSteelBlockEntity.processTime) + "TU"));
                drawCustomInfoStat(g, x, y, 53, 26 + 18 * i, 70, 7, x, y, Component.literal("Bonus: " + furnace.bonus[i] + "%"));
            }

            drawCustomInfoStat(g, x, y, 151, 18, 9, 50, x, y,
                    Component.literal(String.format(Locale.US, "%,d", furnace.heat) + " / " + String.format(Locale.US, "%,d", MachineFurnaceSteelBlockEntity.maxHeat) + "TU"));
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (furnace == null) return;

        int h = furnace.heat * 48 / MachineFurnaceSteelBlockEntity.maxHeat;
        g.blit(TEXTURE, leftPos + 152, topPos + 67 - h, 176, 76 - h, 7, h);

        for (int i = 0; i < 3; i++) {
            int p = furnace.progress[i] * 69 / MachineFurnaceSteelBlockEntity.processTime;
            g.blit(TEXTURE, leftPos + 54, topPos + 18 + 18 * i, 176, 18, p, 5);
            int b = furnace.bonus[i] * 69 / 100;
            g.blit(TEXTURE, leftPos + 54, topPos + 27 + 18 * i, 176, 23, b, 5);

            if (furnace.wasOn)
                g.blit(TEXTURE, leftPos + 16, topPos + 16 + 18 * i, 176, 0, 18, 18);
        }
    }
}
