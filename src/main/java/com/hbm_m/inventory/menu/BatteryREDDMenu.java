package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.machines.BatteryREDDBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ContainerBatteryREDD}: Ladeplatz (26,53), Entladeplatz (80,53), Spielerinventar ab Y 99. */
public class BatteryREDDMenu extends AbstractContainerMenu {

    public final BatteryREDDBlockEntity battery;

    public BatteryREDDMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (BatteryREDDBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public BatteryREDDMenu(int id, Inventory inv, BatteryREDDBlockEntity be) {
        super(ModMenuTypes.BATTERY_REDD_MENU.get(), id);
        this.battery = be;

        var container = new com.hbm_m.inventory.ModItemStackHandlerContainer(be.getInventory(), be::setChanged);
        this.addSlot(new Slot(container, 0, 26, 53));
        this.addSlot(new Slot(container, 1, 80, 53));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 99 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 157));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();

            if (index <= 1) {
                if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // w16b: Original 128 vom Kern; der REDD ist 9x10x5 gross -> zusaetzlich Huelle der Maschine
        return MultiblockMenuReach.stillValidCore(battery, player, 128.0D);
    }
}
