package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineDerrickBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineDerrickMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Bohrturm: Original {@code GUIMachineOilWell}, siehe {@link GUIMachineOilWell}. */
public class GUIMachineDerrick extends GUIMachineOilWell<MachineDerrickMenu> {

    private final MachineDerrickBlockEntity derrick;

    public GUIMachineDerrick(MachineDerrickMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.derrick = menu.getBlockEntity();
    }

    @Override protected FluidTank[] tanks() { return derrick != null ? derrick.tanks : null; }
    @Override protected long power() { return derrick != null ? derrick.getEnergyStored() : 0L; }
    @Override protected long maxPower() { return derrick != null ? derrick.getMaxEnergyStored() : 0L; }
    @Override protected int indicator() { return derrick != null ? derrick.indicator : 0; }
}
