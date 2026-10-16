package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.PAQuadrupoleMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Oberflaeche des Bauteils "Quadrupol" (Textur {@code gui_quadrupole.png}). */
public class GUIPAQuadrupole extends GUIPABase<PAQuadrupoleMenu> {

    public GUIPAQuadrupole(PAQuadrupoleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_quadrupole.png");
    }
}
