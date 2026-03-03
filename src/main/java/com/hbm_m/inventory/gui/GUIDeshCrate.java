package com.hbm_m.inventory.gui;

import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.inventory.menu.DeshCrateMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUIDeshCrate extends GUICrateBase<DeshCrateMenu> {

    public GUIDeshCrate(DeshCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CrateType.DESH);
    }
}
