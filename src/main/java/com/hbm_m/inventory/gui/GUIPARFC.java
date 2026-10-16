package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.PARFCMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Oberflaeche des Bauteils "Beschleunigerzelle" (Textur {@code gui_rfc.png}). */
public class GUIPARFC extends GUIPABase<PARFCMenu> {

    public GUIPARFC(PARFCMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_rfc.png");
    }
}
