package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.albion.PAState;
import com.hbm_m.inventory.menu.PASourceMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Oberflaeche der Teilchenquelle. Sie zeigt zusaetzlich den Zustand des Strahls in der Farbe des
 * Originals ({@link PAState#color}), den zuletzt gemessenen Impuls und die Streuung.
 */
public class GUIPASource extends GUIPABase<PASourceMenu> {

    public GUIPASource(PASourceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_source.png");
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        PAState[] states = PAState.values();
        int ordinal = menu.getStateOrdinal();
        PAState state = ordinal >= 0 && ordinal < states.length ? states[ordinal] : PAState.IDLE;

        drawStatus(guiGraphics, 8, 30,
                Component.translatable("gui.hbm_m.pa.state." + state.name().toLowerCase(java.util.Locale.ROOT)),
                state.color);

        guiGraphics.drawString(this.font,
                Component.translatable("gui.hbm_m.pa.momentum", menu.getLastSpeed()),
                8, 44, 0x404040, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.hbm_m.pa.defocus", menu.getDefocus()),
                8, 56, 0x404040, false);
    }
}
