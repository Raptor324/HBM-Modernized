package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.HeatingOvenMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Порт {@code GUIFirebox} с текстурой Heating Oven ({@code gui_heating_oven.png}) —
 * в оригинале тот же класс GUIFirebox, другая текстура и белый заголовок.
 */
public class GUIHeatingOven extends GUIFireboxBase<HeatingOvenMenu> {

    public GUIHeatingOven(HeatingOvenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "textures/gui/machine/gui_heating_oven.png", true);
    }
}
