package com.hbm_m.inventory.gui;

import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.inventory.menu.TemplateCrateMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUITemplateCrate extends GUICrateBase<TemplateCrateMenu> {

    public GUITemplateCrate(TemplateCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CrateType.TEMPLATE);
    }
}
