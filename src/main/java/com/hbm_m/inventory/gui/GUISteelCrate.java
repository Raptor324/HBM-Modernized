package com.hbm_m.inventory.gui;

import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.inventory.menu.SteelCrateMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUISteelCrate extends GUICrateBase<SteelCrateMenu> {

    public GUISteelCrate(SteelCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CrateType.STEEL);
    }
}
