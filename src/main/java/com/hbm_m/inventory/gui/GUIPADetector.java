package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.PADetectorMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Oberflaeche des Bauteils "Detektor" (Textur {@code gui_detector.png}). */
public class GUIPADetector extends GUIPABase<PADetectorMenu> {

    public GUIPADetector(PADetectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_detector.png");
    }
}
