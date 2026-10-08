package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachinePumpjackMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Pumpe: Original {@code GUIMachineOilWell}, siehe {@link GUIMachineOilWell}. */
public class GUIMachinePumpjack extends GUIMachineOilWell<MachinePumpjackMenu> {

    private final MachinePumpjackBlockEntity pumpjack;

    public GUIMachinePumpjack(MachinePumpjackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.pumpjack = menu.getBlockEntity();
    }

    @Override protected FluidTank[] tanks() { return pumpjack != null ? pumpjack.tanks : null; }
    @Override protected long power() { return pumpjack != null ? pumpjack.getEnergyStored() : 0L; }
    @Override protected long maxPower() { return pumpjack != null ? pumpjack.getMaxEnergyStored() : 0L; }
    @Override protected int indicator() { return pumpjack != null ? pumpjack.indicator : 0; }
}
