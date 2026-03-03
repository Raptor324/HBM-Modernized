package com.hbm_m.inventory.gui;

import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.inventory.menu.IronCrateMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUIIronCrate extends GUICrateBase<IronCrateMenu> {

    public GUIIronCrate(IronCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CrateType.IRON);
    }
}
