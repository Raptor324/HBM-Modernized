package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.PARFCMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIPARFC}: Energie bei 53, Tanks bei 89/107, Temperatur bei 91 - ohne Titel. */
public class GUIPARFC extends GUIPABase<PARFCMenu> {

    public GUIPARFC(PARFCMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_rfc.png", 53, 89, 91);
    }
}
