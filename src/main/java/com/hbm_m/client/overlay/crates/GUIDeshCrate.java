package com.hbm_m.client.overlay.crates;

import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.menu.DeshCrateMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUIDeshCrate extends BaseCrateScreen<DeshCrateMenu> {

    public GUIDeshCrate(DeshCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CrateType.DESH);
    }
}
