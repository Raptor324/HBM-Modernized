package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineSatDockBlockEntity;
import com.hbm_m.item.satellite.ItemSatChip;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ContainerSatDock}: 15 Entnahme-Plaetze (5x3 ab 71,18), der Chip-Platz bei (26,36) nur fuer
 * Satellitenchips, Spielerinventar ab y 104.
 */
public class MachineSatDockMenu extends AbstractContainerMenu {

    private final MachineSatDockBlockEntity dock;

    public MachineSatDockMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (MachineSatDockBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public MachineSatDockMenu(int id, Inventory inv, MachineSatDockBlockEntity dock) {
        super(ModMenuTypes.SAT_DOCK_MENU.get(), id);
        this.dock = dock;

        Container container = new ModItemStackHandlerContainer(dock.getInventory(), dock::setChanged);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                this.addSlot(new Slot(container, col + row * 5, 71 + 18 * col, 18 + 18 * row) {
                    @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
                });
            }
        }

        this.addSlot(new Slot(container, MachineSatDockBlockEntity.SLOT_CHIP, 26, 36) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return stack.getItem() instanceof ItemSatChip; }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 104 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 162));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ret = stack.copy();

            if (index <= 15) {
                if (!this.moveItemStackTo(stack, 16, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 15, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return ret;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(dock.getLevel(), dock.getBlockPos()), player, ModBlocks.SAT_DOCK.get());
    }
}
