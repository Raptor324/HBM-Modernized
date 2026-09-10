package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.PADipoleMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Oberflaeche des Bauteils "Dipol" (Textur {@code gui_dipole.png}). */
public class GUIPADipole extends GUIPABase<PADipoleMenu> {

    public GUIPADipole(PADipoleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_dipole.png");
    }
}
