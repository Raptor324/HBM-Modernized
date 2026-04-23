package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.HeatingOvenMenu;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI screen for the Heating Oven.
 * Displays furnace-style interface with fuel indicator and cooking progress.
 */
public class GUIHeatingOven extends AbstractContainerScreen<HeatingOvenMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/gui_heating_oven.png");

    public GUIHeatingOven(HeatingOvenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Draw main GUI background
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Render the fire/flame indicator when burning
        renderFlame(guiGraphics, x, y);
        
        // Render cooking progress arrow
        renderProgressArrow(guiGraphics, x, y);
    }

    /**
     * Renders the burning flame indicator above the fuel slot.
     * Standard furnace flame is 14x14 pixels.
     */
    private void renderFlame(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isBurning()) {
            int flameHeight = menu.getScaledFuel();
            if (flameHeight > 0) {
                // Draw flame from bottom up (flame at x+56, y+36, going up)
                guiGraphics.blit(GUI_TEXTURE, 
                        x + 56, y + 36 + 14 - flameHeight,  // destination
                        176, 14 - flameHeight,               // source UV
                        14, flameHeight);                    // size
            }
        }
    }

    /**
     * Renders the cooking progress arrow.
     * Standard furnace arrow is 24x17 pixels.
     */
    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        int progressWidth = menu.getScaledProgress();
        if (progressWidth > 0) {
            // Draw progress arrow (arrow at x+79, y+34)
            guiGraphics.blit(GUI_TEXTURE,
                    x + 79, y + 34,      // destination
                    176, 14,             // source UV (below flame in texture)
                    progressWidth, 17);  // size
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Center the title
        guiGraphics.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        // Player inventory label
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                8, this.imageHeight - 96 + 2, 0x404040, false);
    }
}
