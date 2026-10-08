package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.HeatingOvenMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Original: {@code GUIFirebox} mit {@code gui_heating_oven.png}. */
public class GUIHeatingOven extends GUIMachineFirebox<HeatingOvenMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_heating_oven.png");

    public GUIHeatingOven(HeatingOvenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }
}
