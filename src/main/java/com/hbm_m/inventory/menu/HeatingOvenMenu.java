package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.HeatingOvenBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;

/** Original: der Heizofen nutzt {@code ContainerFirebox} - hier nur mit eigenem MenuType. */
public class HeatingOvenMenu extends MachineFireboxMenu {

    public HeatingOvenMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.HEATING_OVEN_MENU.get(), containerId, inv, lookup(inv, extraData), new SimpleContainerData(6));
    }

    public HeatingOvenMenu(int containerId, Inventory inv, HeatingOvenBlockEntity oven) {
        super(ModMenuTypes.HEATING_OVEN_MENU.get(), containerId, inv, oven, oven.getData());
    }
}
