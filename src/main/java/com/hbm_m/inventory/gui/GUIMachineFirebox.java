package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineFireboxMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Порт {@code GUIFirebox} + текстура гритбокса ({@code gui_firebox.png}). */
public class GUIMachineFirebox extends GUIFireboxBase<MachineFireboxMenu> {

    public GUIMachineFirebox(MachineFireboxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "textures/gui/machine/gui_firebox.png", false);
    }
}
