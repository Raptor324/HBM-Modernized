package com.hbm_m.client.overlay.crates;

import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.menu.TungstenCrateMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUITungstenCrate extends BaseCrateScreen<TungstenCrateMenu> {

    public GUITungstenCrate(TungstenCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, CrateType.TUNGSTEN);
    }
}
