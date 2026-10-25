package com.hbm_m.inventory.gui;

import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.inventory.menu.SafeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUISafe}. */
public class GUISafe extends GUICrateBase<SafeMenu> {

    public GUISafe(SafeMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CrateType.SAFE);
    }
}
