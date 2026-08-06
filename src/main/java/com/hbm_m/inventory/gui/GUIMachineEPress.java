package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineEPressMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische Rahmen, keine geratenen Icon-
 * Koordinaten daraus) - Press-Fortschritt und Batterieladung als eigene Fuellrechteck-Overlays
 * statt der Original-Nadel-/Balken-Grafiken (siehe {@code GUIMachinePress} fuer das aufwendigere
 * Original-Pendant der Kohle-Variante).
 */
public class GUIMachineEPress extends AbstractContainerScreen<MachineEPressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_epress.png");

    public GUIMachineEPress(MachineEPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 6;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int press = menu.getPress();
        int maxPress = menu.getMaxPress();
        if (press > 0 && maxPress > 0) {
            int h = press * 17 / maxPress;
            guiGraphics.fill(x + 79, y + 35 - h, x + 98, y + 35, 0xA0FF3020);
        }

        int energy = menu.getEnergyStored();
        int maxEnergy = menu.getMaxEnergyStored();
        if (energy > 0 && maxEnergy > 0) {
            int h = energy * 34 / maxEnergy;
            guiGraphics.fill(x + 152, y + 18 + (34 - h), x + 164, y + 52, 0xFF3080FF);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(152, 18, 12, 34, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " HE"),
                    mouseX, mouseY);
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        mouseX -= guiLeft;
        mouseY -= guiTop;

        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
