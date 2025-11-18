package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Modernized variant of the classic GuiInfoContainer from the 1.7.10 mod.
 * Provides utility helpers for drawing info panels, energy tooltips and contextual hints.
 */
public abstract class GuiInfoScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected static final ResourceLocation GUI_UTILITY =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_utility.png");

    protected GuiInfoScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    protected void drawElectricityInfo(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                       int relX, int relY, int width, int height,
                                       long power, long maxPower) {
        if (isPointInRect(relX, relY, width, height, mouseX, mouseY)) {
            Component tooltip = Component.translatable("gui.hbm_m.energy",
                    EnergyFormatter.format(power), EnergyFormatter.format(maxPower));
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    protected void drawCustomInfoStat(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                      int relX, int relY, int width, int height,
                                      int tooltipX, int tooltipY, Component... text) {
        if (text == null || text.length == 0) {
            return;
        }
        if (isPointInRect(relX, relY, width, height, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, Arrays.asList(text), tooltipX, tooltipY);
        }
    }

    protected void drawInfoPanel(GuiGraphics guiGraphics, int relX, int relY, PanelType panelType) {
        if (panelType == null) {
            return;
        }
        int x = leftPos + relX;
        int y = topPos + relY;
        RenderSystem.enableDepthTest();
        guiGraphics.blit(GUI_UTILITY, x, y, panelType.u, panelType.v, panelType.width, panelType.height);
    }

    protected void drawItemStack(ItemStack stack, GuiGraphics guiGraphics, int relX, int relY, String label) {
        if (stack.isEmpty()) {
            return;
        }
        int x = leftPos + relX;
        int y = topPos + relY;
        guiGraphics.renderItem(stack, x, y);
        guiGraphics.renderItemDecorations(this.font, stack, x, y, label);
    }

    protected boolean isPointInRect(int relX, int relY, int width, int height, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        return x >= relX && x < relX + width && y >= relY && y < relY + height;
    }

    protected void playClickSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected enum PanelType {
        SMALL_BLUE_INFO(0, 0, 8, 8),
        SMALL_GREEN_INFO(0, 8, 8, 8),
        LARGE_BLUE_INFO(8, 0, 16, 16),
        LARGE_GREEN_INFO(24, 0, 16, 16),
        SMALL_RED_EXCLAMATION(0, 16, 8, 8),
        SMALL_YELLOW_EXCLAMATION(0, 24, 8, 8),
        LARGE_RED_EXCLAMATION(8, 16, 16, 16),
        LARGE_YELLOW_EXCLAMATION(24, 16, 16, 16),
        SMALL_BLUE_STAR(0, 32, 8, 8),
        SMALL_GRAY_STAR(0, 40, 8, 8),
        LARGE_BLUE_STAR(8, 32, 16, 16),
        LARGE_GRAY_STAR(24, 32, 16, 16);

        private final int u;
        private final int v;
        private final int width;
        private final int height;

        PanelType(int u, int v, int width, int height) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
        }
    }

}

