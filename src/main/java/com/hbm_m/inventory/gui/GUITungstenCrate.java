package com.hbm_m.inventory.gui;

import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.inventory.menu.TungstenCrateMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUITungstenCrate extends GUICrateBase<TungstenCrateMenu> {

    public GUITungstenCrate(TungstenCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CrateType.TUNGSTEN);
    }
}
