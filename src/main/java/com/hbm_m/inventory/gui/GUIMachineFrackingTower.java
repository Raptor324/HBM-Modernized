package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineFrackingTowerBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineFrackingTowerMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Fracking-Turm: Original {@code GUIMachineOilWell} mit drittem (Fracksol-)Tank, siehe {@link GUIMachineOilWell}. */
public class GUIMachineFrackingTower extends GUIMachineOilWell<MachineFrackingTowerMenu> {

    private final MachineFrackingTowerBlockEntity tower;

    public GUIMachineFrackingTower(MachineFrackingTowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.tower = menu.getBlockEntity();
    }

    @Override
    protected FluidTank[] tanks() {
        return tower != null ? new FluidTank[] { tower.getOilTank(), tower.getGasTank(), tower.getFracksolTank() } : null;
    }

    @Override protected long power() { return tower != null ? tower.getEnergyStored() : 0L; }
    @Override protected long maxPower() { return tower != null ? tower.getMaxEnergyStored() : 0L; }
    @Override protected int indicator() { return tower != null ? tower.getIndicator() : 0; }
}
