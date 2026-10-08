package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;
import com.hbm_m.item.industrial.ItemMachineUpgrade;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ContainerElectrolyserMetal}: Batterie, Upgrades, Kristall (10,22), sechs Ausgaben (136/154, 18-54),
 * Spielerinventar ab (8,122).
 */
public class MachineElectrolyserMetalMenu extends AbstractContainerMenu {

    private final MachineElectrolyserBlockEntity electrolyser;

    public MachineElectrolyserMetalMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, MachineElectrolyserFluidMenu.getBlockEntity(inventory, extraData));
    }

    public MachineElectrolyserMetalMenu(int id, Inventory invPlayer, MachineElectrolyserBlockEntity tedf) {
        super(ModMenuTypes.ELECTROLYSER_METAL_MENU.get(), id);
        this.electrolyser = tedf;

        Container c = MachineElectrolyserFluidMenu.container(tedf);

        //Battery
        this.addSlot(new Slot(c, 0, 186, 109));
        //Upgrades
        this.addSlot(new Slot(c, 1, 186, 140));
        this.addSlot(new Slot(c, 2, 186, 158));
        //Input
        this.addSlot(new Slot(c, 14, 10, 22));
        //Outputs
        this.addSlot(MachineElectrolyserFluidMenu.takeOnly(c, 15, 136, 18));
        this.addSlot(MachineElectrolyserFluidMenu.takeOnly(c, 16, 154, 18));
        this.addSlot(MachineElectrolyserFluidMenu.takeOnly(c, 17, 136, 36));
        this.addSlot(MachineElectrolyserFluidMenu.takeOnly(c, 18, 154, 36));
        this.addSlot(MachineElectrolyserFluidMenu.takeOnly(c, 19, 136, 54));
        this.addSlot(MachineElectrolyserFluidMenu.takeOnly(c, 20, 154, 54));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 180));
        }
    }

    public MachineElectrolyserBlockEntity getBlockEntity() {
        return electrolyser;
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineElectrolyserFluidMenu.stillValid(electrolyser, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int par2) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(par2);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (par2 <= 9) {
                if (!this.moveItemStackTo(var5, 10, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (var3.getItem() instanceof com.hbm_m.api.item.IBatteryItem) {
                    if (!this.moveItemStackTo(var5, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (var3.getItem() instanceof ItemMachineUpgrade) {
                    if (!this.moveItemStackTo(var5, 1, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(var5, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }
}
