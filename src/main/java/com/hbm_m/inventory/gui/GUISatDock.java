package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineSatDockMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUISatDock}: 176x186, Infofeld links neben dem Chip-Platz mit {@code desc.gui.satdock.desc}. */
public class GUISatDock extends GuiInfoScreen<MachineSatDockMenu> {

    public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_sat_dock.png");

    public GUISatDock(MachineSatDockMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        String[] lines = Component.translatable("desc.gui.satdock.desc").getString().split("\\$");
        Component[] text = new Component[lines.length];
        for (int i = 0; i < lines.length; i++) text[i] = Component.literal(lines[i]);
        this.drawCustomInfoStat(g, mouseX, mouseY, -7, 36, 16, 16, leftPos - 7, topPos + 36 + 16, text);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int i, int j) {
        Component name = this.title;
        g.drawString(this.font, name, 115 - this.font.width(name) / 2, 6, 0x404040, false);
        g.drawString(this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float f, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        this.drawInfoPanel(g, -7, 36, PanelType.LARGE_BLUE_INFO);
    }
}
